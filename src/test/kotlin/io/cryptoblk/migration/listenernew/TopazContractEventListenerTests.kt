package io.cryptoblk.migration.listenernew

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.Request
import org.web3j.protocol.core.methods.request.EthFilter
import org.web3j.protocol.core.methods.response.EthBlockNumber
import org.web3j.protocol.core.methods.response.EthLog
import java.math.BigInteger
import java.util.Optional

class TopazContractEventListenerTests {

    @Test
    fun `fresh compensation initializes checkpoint at latest block`() {
        val web3j = Mockito.mock(Web3j::class.java)
        val checkpointRepository = Mockito.mock(TopazEventCheckpointRepository::class.java)
        val listener = listener(web3j, checkpointRepository)
        mockBlockNumber(web3j, BigInteger.TEN)
        Mockito.`when`(checkpointRepository.findById(LISTENER_NAME)).thenReturn(Optional.empty<TopazEventCheckpointEntity>())

        val scanned = listener.compensateMissingEvents()

        assertEquals(0, scanned)
        val checkpoint = captureSavedCheckpoint(checkpointRepository)
        assertEquals(LISTENER_NAME, checkpoint.listenerName)
        assertEquals(BigInteger.TEN, checkpoint.processedBlock)
        Mockito.verify(web3j, Mockito.never()).ethGetLogs(Mockito.any(EthFilter::class.java))
    }

    @Test
    fun `compensation scans from persisted checkpoint plus one`() {
        val web3j = Mockito.mock(Web3j::class.java)
        val checkpointRepository = Mockito.mock(TopazEventCheckpointRepository::class.java)
        val listener = listener(web3j, checkpointRepository)
        mockBlockNumber(web3j, BigInteger.TEN)
        mockLogs(web3j)
        Mockito.`when`(checkpointRepository.findById(LISTENER_NAME)).thenReturn(
            Optional.of(TopazEventCheckpointEntity(LISTENER_NAME, BigInteger.valueOf(8L)))
        )

        val scanned = listener.compensateMissingEvents()

        assertEquals(0, scanned)
        val filter = captureLogFilter(web3j)
        assertEquals("0x9", filter.fromBlock.value)
        assertEquals("0xa", filter.toBlock.value)
        val checkpoint = captureSavedCheckpoint(checkpointRepository)
        assertEquals(LISTENER_NAME, checkpoint.listenerName)
        assertEquals(BigInteger.TEN, checkpoint.processedBlock)
    }

    @Test
    fun `empty reversed scan range does not call chain`() {
        val web3j = Mockito.mock(Web3j::class.java)
        val checkpointRepository = Mockito.mock(TopazEventCheckpointRepository::class.java)
        val listener = listener(web3j, checkpointRepository)

        val scanned = listener.scanEvents(BigInteger.TEN, BigInteger.ONE)

        assertEquals(0, scanned)
        Mockito.verify(web3j, Mockito.never()).ethGetLogs(Mockito.any(EthFilter::class.java))
        Mockito.verifyNoMoreInteractions(checkpointRepository)
    }

    private fun listener(
        web3j: Web3j,
        checkpointRepository: TopazEventCheckpointRepository
    ): TopazContractEventListener {
        return TopazContractEventListener(
            web3j = web3j,
            lifecycleContractAddress = LIFECYCLE_ADDRESS,
            paymentContractAddress = PAYMENT_ADDRESS,
            contactsContractAddress = CONTACTS_ADDRESS,
            workflow = TopazWorkflowService(),
            checkpointRepository = checkpointRepository
        )
    }

    private fun captureSavedCheckpoint(
        checkpointRepository: TopazEventCheckpointRepository
    ): TopazEventCheckpointEntity {
        val captor = ArgumentCaptor.forClass(TopazEventCheckpointEntity::class.java)
        Mockito.verify(checkpointRepository).save(captor.capture())
        return captor.value
    }

    @Suppress("UNCHECKED_CAST")
    private fun mockBlockNumber(web3j: Web3j, blockNumber: BigInteger) {
        val request = Mockito.mock(Request::class.java) as Request<Any, EthBlockNumber>
        val response = EthBlockNumber().apply {
            result = "0x${blockNumber.toString(16)}"
        }
        Mockito.`when`(web3j.ethBlockNumber()).thenReturn(request)
        Mockito.`when`(request.send()).thenReturn(response)
    }

    @Suppress("UNCHECKED_CAST")
    private fun mockLogs(web3j: Web3j) {
        val request = Mockito.mock(Request::class.java) as Request<Any, EthLog>
        val response = EthLog().apply {
            setResult(emptyList())
        }
        Mockito.`when`(web3j.ethGetLogs(Mockito.any(EthFilter::class.java))).thenReturn(request)
        Mockito.`when`(request.send()).thenReturn(response)
    }

    private fun captureLogFilter(web3j: Web3j): EthFilter {
        val captor = ArgumentCaptor.forClass(EthFilter::class.java)
        Mockito.verify(web3j).ethGetLogs(captor.capture())
        return captor.value
    }

    private companion object {
        private const val LIFECYCLE_ADDRESS = "0x0000000000000000000000000000000000000001"
        private const val PAYMENT_ADDRESS = "0x0000000000000000000000000000000000000002"
        private const val CONTACTS_ADDRESS = "0x0000000000000000000000000000000000000003"
        private const val LISTENER_NAME = "topaz-contract-event-listener"
    }
}
