package com.demo.server.epmigration.chain.tx

import com.demo.server.epmigration.config.EpChainProperties
import org.springframework.stereotype.Component

data class ChainCallContext(
    val correlationId: String?,
    val op: String,
    val externalProjectId: String?,
    val chainId: Long,
    val from: String,
    val to: String,
    val phase: String,
    val rpcCode: Int?,
    val rpcMessage: String?,
    val transactionHash: String?,
    val nonce: String?,
    val httpStatus: Int
)

data class ContractWriteCall(
    val contractName: String,
    val functionName: String,
    val to: String,
    val data: String
)

data class SubmittedTransaction(
    val transactionHash: String,
    val nonce: String
)

interface ContractTransactionSubmitter {
    fun submit(call: ContractWriteCall, context: ChainCallContext): SubmittedTransaction
}

@Component
class Web3jContractTransactionSubmitter(
    private val properties: EpChainProperties,
    private val nonceManager: ResilientNonceManager
) : ContractTransactionSubmitter {
    override fun submit(call: ContractWriteCall, context: ChainCallContext): SubmittedTransaction {
        return nonceManager.sendRawTransaction(
            to = call.to,
            data = call.data,
            gasLimit = properties.gasLimit,
            chainId = properties.chainId,
            context = context.copy(
                op = call.functionName,
                to = call.to
            )
        )
    }
}
