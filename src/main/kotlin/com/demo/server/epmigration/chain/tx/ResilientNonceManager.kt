package com.demo.server.epmigration.chain.tx

import com.demo.server.epmigration.chain.error.ChainRpcUnavailableException
import com.demo.server.epmigration.chain.error.NonceUnavailableException
import com.demo.server.epmigration.chain.error.TransactionSubmissionFailedException
import org.web3j.crypto.Credentials
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.utils.Numeric
import org.springframework.stereotype.Component
import java.io.IOException
import java.math.BigInteger
import java.util.Locale

@Component
class ResilientNonceManager(
    private val web3j: Web3j,
    private val credentials: Credentials
) {
    private val lock = Object()
    private var nextNonce: BigInteger? = null

    fun sendRawTransaction(
        to: String,
        data: String,
        gasLimit: BigInteger,
        chainId: Long,
        context: ChainCallContext
    ): SubmittedTransaction {
        synchronized(lock) {
            return sendWithRetry(to, data, gasLimit, chainId, context, true)
        }
    }

    private fun sendWithRetry(
        to: String,
        data: String,
        gasLimit: BigInteger,
        chainId: Long,
        context: ChainCallContext,
        allowRetry: Boolean
    ): SubmittedTransaction {
        val nonce = nextNonce ?: refreshNonce(context)
        val rawTransaction = RawTransaction.createTransaction(
            nonce,
            BigInteger.ZERO,
            gasLimit,
            to,
            BigInteger.ZERO,
            data
        )
        val signed = Numeric.toHexString(TransactionEncoder.signMessage(rawTransaction, chainId, credentials))

        try {
            val response = web3j.ethSendRawTransaction(signed).send()
            if (response.hasError()) {
                val error = response.error
                val errorContext = context.copy(
                    phase = "send(eth_sendRawTransaction)",
                    rpcCode = error.code,
                    rpcMessage = error.message,
                    nonce = nonce.toString(),
                    httpStatus = if (isRetryableNonceError(error.message)) 503 else 502
                )

                if (allowRetry && isRetryableNonceError(error.message)) {
                    refreshNonce(errorContext)
                    return sendWithRetry(to, data, gasLimit, chainId, context, false)
                }

                if (isNonceError(error.message)) {
                    throw NonceUnavailableException(errorContext)
                }
                throw TransactionSubmissionFailedException(errorContext)
            }

            val transactionHash = response.transactionHash
            if (transactionHash.isNullOrBlank()) {
                val errorContext = context.copy(
                    phase = "send(eth_sendRawTransaction)",
                    rpcMessage = "eth_sendRawTransaction returned an empty transaction hash",
                    nonce = nonce.toString(),
                    httpStatus = 502
                )
                throw TransactionSubmissionFailedException(errorContext)
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
        } catch (ex: IOException) {
            val errorContext = context.copy(
                phase = "send(eth_sendRawTransaction)",
                rpcMessage = ex.message,
                nonce = nonce.toString(),
                httpStatus = 503
            )
            throw ChainRpcUnavailableException(errorContext, ex)
        }
    }

    private fun refreshNonce(context: ChainCallContext): BigInteger {
        try {
            val response = web3j.ethGetTransactionCount(
                credentials.address,
                DefaultBlockParameterName.PENDING
            ).send()

            if (response.hasError()) {
                val error = response.error
                val errorContext = context.copy(
                    phase = "nonce(eth_getTransactionCount)",
                    rpcCode = error.code,
                    rpcMessage = error.message,
                    httpStatus = 503
                )
                throw NonceUnavailableException(errorContext)
            }

            nextNonce = response.transactionCount
            return response.transactionCount
        } catch (ex: IOException) {
            val errorContext = context.copy(
                phase = "nonce(eth_getTransactionCount)",
                rpcMessage = ex.message,
                httpStatus = 503
            )
            throw ChainRpcUnavailableException(errorContext, ex)
        }
    }

    private fun isRetryableNonceError(message: String?): Boolean {
        val normalized = (message ?: "").toLowerCase(Locale.US)
        return normalized.contains("nonce too low") ||
            normalized.contains("nonce too high") ||
            normalized.contains("nonce has already been used") ||
            normalized.contains("replacement transaction underpriced")
    }

    private fun isNonceError(message: String?): Boolean {
        val normalized = (message ?: "").toLowerCase(Locale.US)
        return isRetryableNonceError(normalized) ||
            normalized.contains("already known") ||
            normalized.contains("known transaction")
    }
}
