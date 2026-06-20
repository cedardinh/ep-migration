package com.demo.server.epmigration.chain.error

import com.demo.server.epmigration.chain.tx.ChainCallContext

open class ChainException(
    val errorCode: String,
    val publicMessage: String,
    val context: ChainCallContext,
    cause: Throwable? = null
) : RuntimeException(publicMessage, cause)

class ChainRpcUnavailableException(
    context: ChainCallContext,
    cause: Throwable? = null
) : ChainException("CHAIN_RPC_UNAVAILABLE", "Blockchain RPC is unavailable", context, cause)

class NonceUnavailableException(
    context: ChainCallContext,
    cause: Throwable? = null
) : ChainException("NONCE_UNAVAILABLE", "Blockchain nonce is unavailable", context, cause)

class TransactionSubmissionFailedException(
    context: ChainCallContext,
    cause: Throwable? = null
) : ChainException("TRANSACTION_SUBMISSION_FAILED", "Blockchain transaction submission failed", context, cause)
