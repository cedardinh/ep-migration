package com.demo.server.epmigration.chain.tx

data class ChainCallContext(
    val op: String,
    val externalProjectId: String?,
    val from: String,
    val to: String,
    val phase: String = "prepare",
    val rpcCode: Int? = null,
    val rpcMessage: String? = null,
    val nonce: String? = null,
    val httpStatus: Int = 200
)

data class ContractWriteCall(
    val functionName: String,
    val to: String,
    val data: String
)

data class SubmittedTransaction(
    val transactionHash: String,
    val nonce: String
)
