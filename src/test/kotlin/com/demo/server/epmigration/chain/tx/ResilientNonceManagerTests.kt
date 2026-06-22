package com.demo.server.epmigration.chain.tx

import com.demo.server.epmigration.chain.error.ChainRpcUnavailableException
import com.demo.server.epmigration.chain.error.NonceUnavailableException
import com.demo.server.epmigration.chain.error.TransactionSubmissionFailedException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.web3j.crypto.Credentials
import org.web3j.crypto.Hash
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.Request
import org.web3j.protocol.core.Response
import org.web3j.protocol.core.methods.response.EthGetTransactionCount
import org.web3j.protocol.core.methods.response.EthSendTransaction
import java.io.IOException
import java.math.BigInteger
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ResilientNonceManagerTests {
    @Test
    fun `successful submissions cache and increment pending nonce`() {
        val credentials = Credentials.create(PRIVATE_KEY)
        val web3j = Mockito.mock(Web3j::class.java)
        val nonceManager = ResilientNonceManager(web3j, credentials)
        val nonceRequest = request(nonceResponse(7))
        val firstSend = request(sendSuccess("0xfirst"))
        val secondSend = request(sendSuccess("0xsecond"))

        Mockito.`when`(web3j.ethGetTransactionCount(credentials.address, DefaultBlockParameterName.PENDING))
            .thenReturn(nonceRequest)
        Mockito.`when`(web3j.ethSendRawTransaction(Mockito.anyString()))
            .thenReturn(firstSend, secondSend)

        val first = nonceManager.sendRawTransaction(CONTRACT_ADDRESS, "0x", GAS_PRICE, GAS_LIMIT, CHAIN_ID, context())
        val second = nonceManager.sendRawTransaction(CONTRACT_ADDRESS, "0x", GAS_PRICE, GAS_LIMIT, CHAIN_ID, context())

        assertEquals("7", first.nonce)
        assertEquals("8", second.nonce)
        assertEquals("0xfirst", first.transactionHash)
        assertEquals("0xsecond", second.transactionHash)
        Mockito.verify(web3j, Mockito.times(1)).ethGetTransactionCount(credentials.address, DefaultBlockParameterName.PENDING)
        Mockito.verify(web3j, Mockito.times(2)).ethSendRawTransaction(Mockito.anyString())
    }

    @Test
    fun `concurrent submissions serialize nonce allocation and refresh pending nonce once`() {
        val credentials = Credentials.create(PRIVATE_KEY)
        val web3j = Mockito.mock(Web3j::class.java)
        val nonceManager = ResilientNonceManager(web3j, credentials)
        val nonceRequest = request(nonceResponse(7))
        val sendRequest = request(sendSuccess("0xsubmitted"))
        val threadCount = 8
        val executor = Executors.newFixedThreadPool(threadCount)
        val ready = CountDownLatch(threadCount)
        val start = CountDownLatch(1)
        val submitted = Collections.synchronizedList(mutableListOf<SubmittedTransaction>())

        Mockito.`when`(web3j.ethGetTransactionCount(credentials.address, DefaultBlockParameterName.PENDING))
            .thenReturn(nonceRequest)
        Mockito.`when`(web3j.ethSendRawTransaction(Mockito.anyString()))
            .thenReturn(sendRequest)

        val futures = (1..threadCount).map {
            executor.submit {
                ready.countDown()
                start.await(5, TimeUnit.SECONDS)
                submitted.add(
                    nonceManager.sendRawTransaction(
                        CONTRACT_ADDRESS,
                        "0x",
                        GAS_PRICE,
                        GAS_LIMIT,
                        CHAIN_ID,
                        context()
                    )
                )
            }
        }

        ready.await(5, TimeUnit.SECONDS)
        start.countDown()
        futures.forEach { it.get(5, TimeUnit.SECONDS) }
        executor.shutdown()

        assertEquals((7 until 7 + threadCount).map { it.toString() }, submitted.map { it.nonce }.sortedBy { it.toInt() })
        Mockito.verify(web3j, Mockito.times(1)).ethGetTransactionCount(credentials.address, DefaultBlockParameterName.PENDING)
        Mockito.verify(web3j, Mockito.times(threadCount)).ethSendRawTransaction(Mockito.anyString())
    }

    @Test
    fun `nonce too low fails without retrying`() {
        val credentials = Credentials.create(PRIVATE_KEY)
        val web3j = Mockito.mock(Web3j::class.java)
        val nonceManager = ResilientNonceManager(web3j, credentials)
        val nonceRequest = request(nonceResponse(7))
        val sendRequest = request(sendError("Nonce too low"))

        Mockito.`when`(web3j.ethGetTransactionCount(credentials.address, DefaultBlockParameterName.PENDING))
            .thenReturn(nonceRequest)
        Mockito.`when`(web3j.ethSendRawTransaction(Mockito.anyString()))
            .thenReturn(sendRequest)

        assertThrows(NonceUnavailableException::class.java) {
            nonceManager.sendRawTransaction(CONTRACT_ADDRESS, "0x", GAS_PRICE, GAS_LIMIT, CHAIN_ID, context())
        }
        Mockito.verify(web3j, Mockito.times(1)).ethGetTransactionCount(credentials.address, DefaultBlockParameterName.PENDING)
        Mockito.verify(web3j, Mockito.times(1)).ethSendRawTransaction(Mockito.anyString())
    }

    @Test
    fun `future nonce fails once and refreshes pending nonce on the next submission`() {
        val credentials = Credentials.create(PRIVATE_KEY)
        val web3j = Mockito.mock(Web3j::class.java)
        val nonceManager = ResilientNonceManager(web3j, credentials)
        val nonce10Request = request(nonceResponse(10))
        val nonce7Request = request(nonceResponse(7))
        val nonceTooDistantRequest = request(sendError("Transaction nonce is too distant from current sender nonce"))
        val successRequest = request(sendSuccess("0xnext"))

        Mockito.`when`(web3j.ethGetTransactionCount(credentials.address, DefaultBlockParameterName.PENDING))
            .thenReturn(nonce10Request, nonce7Request)
        Mockito.`when`(web3j.ethSendRawTransaction(Mockito.anyString()))
            .thenReturn(nonceTooDistantRequest, successRequest)

        val ex = assertThrows(NonceUnavailableException::class.java) {
            nonceManager.sendRawTransaction(CONTRACT_ADDRESS, "0x", GAS_PRICE, GAS_LIMIT, CHAIN_ID, context())
        }
        assertEquals("10", ex.context.nonce)
        Mockito.verify(web3j, Mockito.times(1)).ethGetTransactionCount(credentials.address, DefaultBlockParameterName.PENDING)
        Mockito.verify(web3j, Mockito.times(1)).ethSendRawTransaction(Mockito.anyString())

        val submitted = nonceManager.sendRawTransaction(CONTRACT_ADDRESS, "0x", GAS_PRICE, GAS_LIMIT, CHAIN_ID, context())

        assertEquals("7", submitted.nonce)
        assertEquals("0xnext", submitted.transactionHash)
        Mockito.verify(web3j, Mockito.times(2)).ethGetTransactionCount(credentials.address, DefaultBlockParameterName.PENDING)
        Mockito.verify(web3j, Mockito.times(2)).ethSendRawTransaction(Mockito.anyString())
    }

    @Test
    fun `future nonce is treated as nonce unavailable`() {
        val credentials = Credentials.create(PRIVATE_KEY)
        val web3j = Mockito.mock(Web3j::class.java)
        val nonceManager = ResilientNonceManager(web3j, credentials)
        val nonceRequest = request(nonceResponse(10))
        val sendRequest = request(sendError("Nonce too high"))

        Mockito.`when`(web3j.ethGetTransactionCount(credentials.address, DefaultBlockParameterName.PENDING))
            .thenReturn(nonceRequest)
        Mockito.`when`(web3j.ethSendRawTransaction(Mockito.anyString()))
            .thenReturn(sendRequest)

        val ex = assertThrows(NonceUnavailableException::class.java) {
            nonceManager.sendRawTransaction(CONTRACT_ADDRESS, "0x", GAS_PRICE, GAS_LIMIT, CHAIN_ID, context())
        }

        assertEquals(503, ex.context.httpStatus)
        assertEquals("10", ex.context.nonce)
        Mockito.verify(web3j, Mockito.times(1)).ethGetTransactionCount(credentials.address, DefaultBlockParameterName.PENDING)
        Mockito.verify(web3j, Mockito.times(1)).ethSendRawTransaction(Mockito.anyString())
    }

    @Test
    fun `initial sync is treated as rpc unavailable`() {
        val credentials = Credentials.create(PRIVATE_KEY)
        val web3j = Mockito.mock(Web3j::class.java)
        val nonceManager = ResilientNonceManager(web3j, credentials)
        val nonceRequest = request(nonceResponse(7))
        val sendRequest = request(sendError("Initial sync is still in progress"))

        Mockito.`when`(web3j.ethGetTransactionCount(credentials.address, DefaultBlockParameterName.PENDING))
            .thenReturn(nonceRequest)
        Mockito.`when`(web3j.ethSendRawTransaction(Mockito.anyString()))
            .thenReturn(sendRequest)

        assertThrows(ChainRpcUnavailableException::class.java) {
            nonceManager.sendRawTransaction(CONTRACT_ADDRESS, "0x", GAS_PRICE, GAS_LIMIT, CHAIN_ID, context())
        }
    }

    @Test
    fun `invalid signature is treated as transaction submission failure`() {
        val credentials = Credentials.create(PRIVATE_KEY)
        val web3j = Mockito.mock(Web3j::class.java)
        val nonceManager = ResilientNonceManager(web3j, credentials)
        val nonceRequest = request(nonceResponse(7))
        val sendRequest = request(sendError("Invalid signature"))

        Mockito.`when`(web3j.ethGetTransactionCount(credentials.address, DefaultBlockParameterName.PENDING))
            .thenReturn(nonceRequest)
        Mockito.`when`(web3j.ethSendRawTransaction(Mockito.anyString()))
            .thenReturn(sendRequest)

        val ex = assertThrows(TransactionSubmissionFailedException::class.java) {
            nonceManager.sendRawTransaction(CONTRACT_ADDRESS, "0x", GAS_PRICE, GAS_LIMIT, CHAIN_ID, context())
        }
        assertEquals(502, ex.context.httpStatus)
    }

    @Test
    fun `unknown send error is treated as transaction submission failure`() {
        val credentials = Credentials.create(PRIVATE_KEY)
        val web3j = Mockito.mock(Web3j::class.java)
        val nonceManager = ResilientNonceManager(web3j, credentials)
        val nonceRequest = request(nonceResponse(7))
        val sendRequest = request(sendError("Unexpected Besu error"))

        Mockito.`when`(web3j.ethGetTransactionCount(credentials.address, DefaultBlockParameterName.PENDING))
            .thenReturn(nonceRequest)
        Mockito.`when`(web3j.ethSendRawTransaction(Mockito.anyString()))
            .thenReturn(sendRequest)

        val ex = assertThrows(TransactionSubmissionFailedException::class.java) {
            nonceManager.sendRawTransaction(CONTRACT_ADDRESS, "0x", GAS_PRICE, GAS_LIMIT, CHAIN_ID, context())
        }
        assertEquals(502, ex.context.httpStatus)
        assertEquals("Unexpected Besu error", ex.context.rpcMessage)
    }

    @Test
    fun `empty transaction hash is treated as transaction submission failure`() {
        val credentials = Credentials.create(PRIVATE_KEY)
        val web3j = Mockito.mock(Web3j::class.java)
        val nonceManager = ResilientNonceManager(web3j, credentials)
        val nonceRequest = request(nonceResponse(7))
        val sendRequest = request(sendSuccess(""))

        Mockito.`when`(web3j.ethGetTransactionCount(credentials.address, DefaultBlockParameterName.PENDING))
            .thenReturn(nonceRequest)
        Mockito.`when`(web3j.ethSendRawTransaction(Mockito.anyString()))
            .thenReturn(sendRequest)

        val ex = assertThrows(TransactionSubmissionFailedException::class.java) {
            nonceManager.sendRawTransaction(CONTRACT_ADDRESS, "0x", GAS_PRICE, GAS_LIMIT, CHAIN_ID, context())
        }
        assertEquals(502, ex.context.httpStatus)
        assertEquals("eth_sendRawTransaction returned an empty transaction hash", ex.context.rpcMessage)
    }

    @Test
    fun `null transaction hash is treated as transaction submission failure`() {
        val credentials = Credentials.create(PRIVATE_KEY)
        val web3j = Mockito.mock(Web3j::class.java)
        val nonceManager = ResilientNonceManager(web3j, credentials)
        val nonceRequest = request(nonceResponse(7))
        val sendRequest = request(EthSendTransaction())

        Mockito.`when`(web3j.ethGetTransactionCount(credentials.address, DefaultBlockParameterName.PENDING))
            .thenReturn(nonceRequest)
        Mockito.`when`(web3j.ethSendRawTransaction(Mockito.anyString()))
            .thenReturn(sendRequest)

        val ex = assertThrows(TransactionSubmissionFailedException::class.java) {
            nonceManager.sendRawTransaction(CONTRACT_ADDRESS, "0x", GAS_PRICE, GAS_LIMIT, CHAIN_ID, context())
        }
        assertEquals(502, ex.context.httpStatus)
        assertEquals("eth_sendRawTransaction returned an empty transaction hash", ex.context.rpcMessage)
    }

    @Test
    fun `nonce rpc error is treated as nonce unavailable`() {
        val credentials = Credentials.create(PRIVATE_KEY)
        val web3j = Mockito.mock(Web3j::class.java)
        val nonceManager = ResilientNonceManager(web3j, credentials)
        val nonceRequest = request(nonceError(-32000, "nonce rpc failed"))

        Mockito.`when`(web3j.ethGetTransactionCount(credentials.address, DefaultBlockParameterName.PENDING))
            .thenReturn(nonceRequest)

        val ex = assertThrows(NonceUnavailableException::class.java) {
            nonceManager.sendRawTransaction(CONTRACT_ADDRESS, "0x", GAS_PRICE, GAS_LIMIT, CHAIN_ID, context())
        }

        assertEquals(503, ex.context.httpStatus)
        assertEquals(-32000, ex.context.rpcCode)
        assertEquals("nonce rpc failed", ex.context.rpcMessage)
        Mockito.verify(web3j, Mockito.never()).ethSendRawTransaction(Mockito.anyString())
    }

    @Test
    fun `nonce io failure is treated as rpc unavailable`() {
        val credentials = Credentials.create(PRIVATE_KEY)
        val web3j = Mockito.mock(Web3j::class.java)
        val nonceManager = ResilientNonceManager(web3j, credentials)
        val failingNonceRequest = failingRequest<EthGetTransactionCount>(IOException("nonce socket closed"))

        Mockito.`when`(web3j.ethGetTransactionCount(credentials.address, DefaultBlockParameterName.PENDING))
            .thenReturn(failingNonceRequest)

        val ex = assertThrows(ChainRpcUnavailableException::class.java) {
            nonceManager.sendRawTransaction(CONTRACT_ADDRESS, "0x", GAS_PRICE, GAS_LIMIT, CHAIN_ID, context())
        }

        assertEquals(503, ex.context.httpStatus)
        assertEquals("nonce socket closed", ex.context.rpcMessage)
        Mockito.verify(web3j, Mockito.never()).ethSendRawTransaction(Mockito.anyString())
    }

    @Test
    fun `send io failure is treated as rpc unavailable`() {
        val credentials = Credentials.create(PRIVATE_KEY)
        val web3j = Mockito.mock(Web3j::class.java)
        val nonceManager = ResilientNonceManager(web3j, credentials)
        val nonceRequest = request(nonceResponse(7))
        val failingSendRequest = failingRequest<EthSendTransaction>(IOException("send socket closed"))

        Mockito.`when`(web3j.ethGetTransactionCount(credentials.address, DefaultBlockParameterName.PENDING))
            .thenReturn(nonceRequest)
        Mockito.`when`(web3j.ethSendRawTransaction(Mockito.anyString()))
            .thenReturn(failingSendRequest)

        val ex = assertThrows(ChainRpcUnavailableException::class.java) {
            nonceManager.sendRawTransaction(CONTRACT_ADDRESS, "0x", GAS_PRICE, GAS_LIMIT, CHAIN_ID, context())
        }

        assertEquals(503, ex.context.httpStatus)
        assertEquals("send socket closed", ex.context.rpcMessage)
        assertEquals("7", ex.context.nonce)
    }

    @Test
    fun `known transaction returns the signed transaction hash without retrying`() {
        val credentials = Credentials.create(PRIVATE_KEY)
        val web3j = Mockito.mock(Web3j::class.java)
        val nonceManager = ResilientNonceManager(web3j, credentials)
        val signedTransaction = ArgumentCaptor.forClass(String::class.java)
        val nonceRequest = request(nonceResponse(7))
        val sendRequest = request(sendError("Known transaction"))

        Mockito.`when`(web3j.ethGetTransactionCount(credentials.address, DefaultBlockParameterName.PENDING))
            .thenReturn(nonceRequest)
        Mockito.`when`(web3j.ethSendRawTransaction(signedTransaction.capture()))
            .thenReturn(sendRequest)

        val submitted = nonceManager.sendRawTransaction(CONTRACT_ADDRESS, "0x", GAS_PRICE, GAS_LIMIT, CHAIN_ID, context())

        assertEquals("7", submitted.nonce)
        assertEquals(Hash.sha3(signedTransaction.value), submitted.transactionHash)
        Mockito.verify(web3j, Mockito.times(1)).ethSendRawTransaction(Mockito.anyString())
    }

    private fun context(): ChainCallContext =
        ChainCallContext(op = "createProject", externalProjectId = "project-1", from = WALLET, to = CONTRACT_ADDRESS)

    private fun nonceResponse(nonce: Long): EthGetTransactionCount =
        EthGetTransactionCount().apply { result = "0x${nonce.toString(16)}" }

    private fun nonceError(code: Int, message: String): EthGetTransactionCount =
        EthGetTransactionCount().apply { error = Response.Error(code, message) }

    private fun sendError(message: String): EthSendTransaction =
        EthSendTransaction().apply { error = Response.Error(-32000, message) }

    private fun sendSuccess(transactionHash: String): EthSendTransaction =
        EthSendTransaction().apply { result = transactionHash }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Response<*>> request(response: T): Request<Any, T> {
        val request = Mockito.mock(Request::class.java) as Request<Any, T>
        Mockito.`when`(request.send()).thenReturn(response)
        return request
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Response<*>> failingRequest(ex: IOException): Request<Any, T> {
        val request = Mockito.mock(Request::class.java) as Request<Any, T>
        Mockito.`when`(request.send()).thenThrow(ex)
        return request
    }

    companion object {
        private const val CHAIN_ID = 31337L
        private const val CONTRACT_ADDRESS = "0x0000000000000000000000000000000000000001"
        private const val PRIVATE_KEY = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
        private const val WALLET = "0x628d684197485c054cda7d3def46e8be6b3d174c"
        private val GAS_PRICE = BigInteger.ZERO
        private val GAS_LIMIT = BigInteger.valueOf(123_456L)
    }
}
