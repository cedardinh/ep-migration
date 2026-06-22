package com.demo.server.epmigration.ledger

import java.math.BigInteger

data class WorkflowResultPayload(
    val stage: String,
    val status: WorkflowStatus,
    val reason: String,
    val stateId: BigInteger,
    val transactionHash: String,
    val blockNumber: BigInteger?,
    val gasUsed: BigInteger?
)

enum class WorkflowStatus {
    RECEIVED
}

data class UserMessage(
    val key: String,
    val enMessage: String
)

class GenericBadRequestException(
    val userMessage: UserMessage
) : RuntimeException(userMessage.key)
