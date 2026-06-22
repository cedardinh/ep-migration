package com.demo.server.epmigration.chain.error

import com.demo.server.epmigration.chain.tx.ChainCallContext
import org.web3j.protocol.core.Response
import java.io.IOException
import java.math.BigInteger
import java.util.Locale

enum class ChainErrorType(val errorCode: String, val publicMessage: String) {
    RPC_UNAVAILABLE("CHAIN_RPC_UNAVAILABLE", "Blockchain RPC is unavailable"),
    NONCE_UNAVAILABLE("NONCE_UNAVAILABLE", "Blockchain nonce is unavailable"),
    TRANSACTION_SUBMISSION_FAILED("TRANSACTION_SUBMISSION_FAILED", "Blockchain transaction submission failed");

    fun exception(context: ChainCallContext, cause: Throwable? = null): ChainException =
        when (this) {
            RPC_UNAVAILABLE -> ChainRpcUnavailableException(context, cause)
            NONCE_UNAVAILABLE -> NonceUnavailableException(context, cause)
            TRANSACTION_SUBMISSION_FAILED -> TransactionSubmissionFailedException(context, cause)
        }
}

open class ChainException(
    val type: ChainErrorType,
    val context: ChainCallContext,
    cause: Throwable? = null
) : RuntimeException(type.publicMessage, cause) {
    val errorCode: String = type.errorCode
    val publicMessage: String = type.publicMessage
}

class ChainRpcUnavailableException(
    context: ChainCallContext,
    cause: Throwable? = null
) : ChainException(ChainErrorType.RPC_UNAVAILABLE, context, cause)

class NonceUnavailableException(
    context: ChainCallContext,
    cause: Throwable? = null
) : ChainException(ChainErrorType.NONCE_UNAVAILABLE, context, cause)

class TransactionSubmissionFailedException(
    context: ChainCallContext,
    cause: Throwable? = null
) : ChainException(ChainErrorType.TRANSACTION_SUBMISSION_FAILED, context, cause)

internal object BesuJsonRpcErrors {
    private const val SEND_PHASE = "send(eth_sendRawTransaction)"
    private const val NONCE_PHASE = "nonce(eth_getTransactionCount)"
    private const val EMPTY_HASH_MESSAGE = "eth_sendRawTransaction returned an empty transaction hash"

    fun isKnownTransaction(error: Response.Error): Boolean =
        BesuJsonRpcError.from(error.message) == BesuJsonRpcError.KNOWN_TRANSACTION

    fun sendError(
        context: ChainCallContext,
        nonce: BigInteger,
        error: Response.Error
    ): ChainException {
        val classification = BesuJsonRpcError.from(error.message)
        val errorContext = context.withRpcFailure(
            phase = SEND_PHASE,
            message = error.message,
            httpStatus = classification.httpStatus,
            nonce = nonce,
            rpcCode = error.code
        )
        return classification.toException(errorContext)
    }

    fun emptyTransactionHash(context: ChainCallContext, nonce: BigInteger): TransactionSubmissionFailedException =
        TransactionSubmissionFailedException(
            context.withRpcFailure(SEND_PHASE, EMPTY_HASH_MESSAGE, 502, nonce)
        )

    fun sendIo(context: ChainCallContext, nonce: BigInteger, ex: IOException): ChainRpcUnavailableException =
        ChainRpcUnavailableException(
            context.withRpcFailure(SEND_PHASE, ex.message, 503, nonce),
            ex
        )

    fun nonceError(context: ChainCallContext, error: Response.Error): NonceUnavailableException =
        NonceUnavailableException(
            context.withRpcFailure(
                phase = NONCE_PHASE,
                message = error.message,
                httpStatus = 503,
                rpcCode = error.code
            )
        )

    fun nonceIo(context: ChainCallContext, ex: IOException): ChainRpcUnavailableException =
        ChainRpcUnavailableException(
            context.withRpcFailure(NONCE_PHASE, ex.message, 503),
            ex
        )

    private fun ChainCallContext.withRpcFailure(
        phase: String,
        message: String?,
        httpStatus: Int,
        nonce: BigInteger? = null,
        rpcCode: Int? = this.rpcCode
    ): ChainCallContext = copy(
        phase = phase,
        rpcCode = rpcCode,
        rpcMessage = message,
        nonce = if (nonce != null) nonce.toString() else this.nonce,
        httpStatus = httpStatus
    )
}

internal enum class BesuJsonRpcError(
    val httpStatus: Int,
    val chainError: ChainErrorType,
    vararg messages: String
) {
    // Besu 24.9.1 JsonRpcErrorConverter + RpcErrorType messages for eth_sendRawTransaction.
    // https://github.com/hyperledger/besu/blob/24.9.1/ethereum/api/src/main/java/org/hyperledger/besu/ethereum/api/jsonrpc/JsonRpcErrorConverter.java
    KNOWN_TRANSACTION(
        200,
        ChainErrorType.TRANSACTION_SUBMISSION_FAILED,
        "Known transaction"
    ),
    FUTURE_NONCE(
        503,
        ChainErrorType.NONCE_UNAVAILABLE,
        "Nonce too high",
        "Transaction nonce is too distant from current sender nonce"
    ),
    NONCE_CONFLICT(
        503,
        ChainErrorType.NONCE_UNAVAILABLE,
        "Nonce too low",
        "Replacement transaction underpriced",
        "An invalid transaction with a lower nonce exists"
    ),
    RPC_UNAVAILABLE(
        503,
        ChainErrorType.RPC_UNAVAILABLE,
        "Internal error",
        "Timeout expired",
        "Initial sync is still in progress",
        "Transaction pool not enabled. (Either txpool explicitly disabled, or node not yet in sync).",
        "World state unavailable",
        "Block not found",
        "Transaction processing could not be completed due to an exception"
    ),
    SUBMISSION_REJECTED(
        502,
        ChainErrorType.TRANSACTION_SUBMISSION_FAILED,
        "Invalid params",
        "Invalid block, unable to parse RLP",
        "Invalid input",
        "Invalid transaction params (missing or incorrect)",
        "Invalid signature",
        "Invalid transaction type",
        "Intrinsic gas exceeds gas limit",
        "Upfront cost exceeds account balance",
        "Transaction gas limit exceeds block gas limit",
        "Sender account not authorized to send transactions",
        "Gas price below configured minimum gas price",
        "Gas price below current base fee",
        "blob gas price below current blob base fee",
        "Wrong chainId",
        "ChainId not supported",
        "ChainId is required",
        "Transaction fee cap exceeded",
        "Max priority fee per gas exceeds max fee per gas",
        "Unsupported private transaction type",
        "Offchain Privacy group does not exist.",
        "Private transaction invalid",
        "Private transaction failed",
        "Total blob gas too high",
        "Plugin has marked the transaction as invalid",
        "blobs failed kzg validation"
    ),
    UNKNOWN(502, ChainErrorType.TRANSACTION_SUBMISSION_FAILED);

    private val normalizedMessages = messages.map { it.normalizedBesuMessage() }.toSet()

    fun toException(context: ChainCallContext): ChainException =
        chainError.exception(context)

    fun matches(message: String): Boolean =
        message.normalizedBesuMessage() in normalizedMessages

    companion object {
        fun from(message: String?): BesuJsonRpcError {
            if (message == null) {
                return UNKNOWN
            }
            return values().firstOrNull { it.matches(message) } ?: UNKNOWN
        }
    }
}

private fun String.normalizedBesuMessage(): String =
    trim().toLowerCase(Locale.US)
