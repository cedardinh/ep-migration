package com.demo.server.epmigration.ledger

import com.demo.server.epmigration.chain.generated.TopazLifecycle
import com.demo.server.epmigration.chain.tx.ContractRevertDecoder
import com.demo.server.epmigration.config.EpChainProperties
import com.demo.server.epmigration.project.dto.CreateProjectRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.RemoteCall
import org.web3j.protocol.core.methods.response.TransactionReceipt
import org.web3j.tx.RawTransactionManager
import org.web3j.tx.gas.StaticGasProvider
import java.math.BigInteger

@Component
class Web3jLedgerClient(
    private val properties: EpChainProperties,
    web3j: Web3j,
    credentials: Credentials
) {
    private val log = LoggerFactory.getLogger(Web3jLedgerClient::class.java)
    private val lifecycleAddress = properties.lifecycleContractAddress.trim()
    private val transactionManager = RawTransactionManager(web3j, credentials, properties.chainId)
    private val gasProvider = StaticGasProvider(properties.gasPrice, properties.gasLimit)
    private val lifecycle: TopazLifecycle = TopazLifecycle.load(lifecycleAddress, web3j, transactionManager, gasProvider)

    fun createProject(payload: CreateProjectRequest): WorkflowResultPayload {
        val input = payload
        logCreateProjectInput(input)
        val receipt = sendTransaction(stage = TopazLifecycle.FUNC_CREATEPROJECT, call = lifecycle.createProject(input))
        val event = TopazLifecycle.getProjectCreatedEvents(receipt).firstOrNull()
            ?: error("ProjectCreated event not found in transaction ${receipt.transactionHash}")
        return workflowResult(stage = TopazLifecycle.FUNC_CREATEPROJECT, receipt = receipt, stateId = event.projectId)
    }

    private fun sendTransaction(stage: String, call: RemoteCall<TransactionReceipt>): TransactionReceipt {
        try {
            val receipt = call.send().requireSuccess(stage)
            log.info(
                "Web3j transaction accepted: stage={} hash={} block={} gasUsed={}",
                stage,
                receipt.transactionHash,
                receipt.blockNumber,
                receipt.gasUsed
            )
            return receipt
        } catch (ex: Exception) {
            val message = ex.message
            val decodedRevert = decodeRevertReason(ex.message, ex.cause?.message)
            if (message?.startsWith("ledger.Web3jLedgerClient.") == true) {
                log.error(message)
            } else if (decodedRevert == null) {
                log.error("Web3j transaction failed before acceptance: stage={}", stage, ex)
            } else {
                log.error("ledger.Web3jLedgerClient.{} - revert reason:{}", stage, decodedRevert)
            }
            if (ex.message?.contains("cannot remove contractor with existing claims", ignoreCase = true) == true) {
                throw GenericBadRequestException(
                    UserMessage(
                        key = "Project.Update.ImmutableContractor",
                        enMessage = "immutable contractor"
                    )
                )
            }
            throw ex
        }
    }

    private fun workflowResult(stage: String, receipt: TransactionReceipt, stateId: BigInteger): WorkflowResultPayload =
        WorkflowResultPayload(
            stage = stage,
            status = WorkflowStatus.RECEIVED,
            reason = "Web3j transaction accepted",
            stateId = stateId,
            transactionHash = receipt.transactionHash,
            blockNumber = receipt.blockNumber,
            gasUsed = receipt.gasUsed
        )

    private fun logCreateProjectInput(input: CreateProjectRequest) {
        log.info(
            "Web3j createProject input: externalProjectId={} name={} mainContractors={} claimApprovers={} paymentApprovers={} bankAccountRefs={}",
            input.externalProjectId,
            input.name,
            input.mainContractors.size,
            input.claimApprovers.size,
            input.paymentApprovers.size,
            input.bankAccountRefs.size
        )
    }

    private fun TransactionReceipt.requireSuccess(stage: String): TransactionReceipt {
        if (!isStatusOK) {
            val decodedRevert = decodeRevertReason(revertReason)
            val reason = decodedRevert ?: revertReason ?: "unknown"
            throw IllegalStateException(
                "ledger.Web3jLedgerClient.$stage - revert reason:$reason hash=$transactionHash status=$status"
            )
        }
        return this
    }

    private fun decodeRevertReason(vararg candidates: String?): String? {
        val raw = ContractRevertDecoder.extractHex(*candidates) ?: return null
        return ContractRevertDecoder.decode(raw)?.toLogMessage()
    }
}
