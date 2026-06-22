package com.demo.server.epmigration.chain.tx

import com.demo.server.epmigration.chain.error.BesuJsonRpcErrors
import com.demo.server.epmigration.chain.error.ChainErrorType
import com.demo.server.epmigration.chain.error.ChainException
import org.web3j.crypto.Credentials
import org.web3j.crypto.Hash
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.utils.Numeric
import org.springframework.stereotype.Component
import java.io.IOException
import java.math.BigInteger

@Component
class ResilientNonceManager(private val web3j: Web3j, private val credentials: Credentials) {
    private val lock = Object()
    private var nextNonce: BigInteger? = null

    fun sendRawTransaction(
        to: String,
        data: String,
        gasPrice: BigInteger,
        gasLimit: BigInteger,
        chainId: Long,
        context: ChainCallContext
    ): SubmittedTransaction =
        synchronized(lock) { sendOnce(to, data, gasPrice, gasLimit, chainId, context) }

    private fun sendOnce(
        to: String,
        data: String,
        gasPrice: BigInteger,
        gasLimit: BigInteger,
        chainId: Long,
        context: ChainCallContext
    ): SubmittedTransaction {
        val nonce = nextNonce ?: refreshNonce(context)
        val signed = signTransaction(nonce, to, data, gasPrice, gasLimit, chainId)

        try {
            val response = web3j.ethSendRawTransaction(signed).send()
            if (!response.hasError()) {
                return submittedTransaction(context, nonce, response.transactionHash)
            }

            if (BesuJsonRpcErrors.isKnownTransaction(response.error)) {
                return submittedTransaction(context, nonce, Hash.sha3(signed))
            }

            val ex = BesuJsonRpcErrors.sendError(context, nonce, response.error)
            invalidateNonceOnNonceError(ex)
            throw ex
        } catch (ex: IOException) {
            throw BesuJsonRpcErrors.sendIo(context, nonce, ex)
        }
    }

    private fun invalidateNonceOnNonceError(ex: ChainException) {
        if (ex.type == ChainErrorType.NONCE_UNAVAILABLE) {
            nextNonce = null
        }
    }

    private fun submittedTransaction(context: ChainCallContext, nonce: BigInteger, transactionHash: String?): SubmittedTransaction {
        if (transactionHash.isNullOrBlank()) {
            throw BesuJsonRpcErrors.emptyTransactionHash(context, nonce)
        }

        nextNonce = nonce.add(BigInteger.ONE)
        return SubmittedTransaction(
            transactionHash = transactionHash,
            nonce = nonce.toString(),
            from = context.from,
            to = context.to,
            functionName = context.op,
            externalProjectId = context.externalProjectId
        )
    }

    private fun refreshNonce(context: ChainCallContext): BigInteger {
        try {
            val response = web3j.ethGetTransactionCount(credentials.address, DefaultBlockParameterName.PENDING).send()

            if (response.hasError()) {
                val error = response.error
                throw BesuJsonRpcErrors.nonceError(context, error)
            }

            nextNonce = response.transactionCount
            return response.transactionCount
        } catch (ex: IOException) {
            throw BesuJsonRpcErrors.nonceIo(context, ex)
        }
    }

    private fun signTransaction(
        nonce: BigInteger,
        to: String,
        data: String,
        gasPrice: BigInteger,
        gasLimit: BigInteger,
        chainId: Long
    ): String = Numeric.toHexString(
        TransactionEncoder.signMessage(
            RawTransaction.createTransaction(nonce, gasPrice, gasLimit, to, BigInteger.ZERO, data),
            chainId,
            credentials
        )
    )

}
