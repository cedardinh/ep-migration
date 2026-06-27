package com.demo.server.epmigration.chain.gateway

import com.demo.server.epmigration.chain.generated.TopazLifecycle
import com.demo.server.epmigration.chain.tx.ContractTransactionSender
import com.demo.server.epmigration.config.EpChainProperties
import com.demo.server.epmigration.project.persistence.ProjectSummaryPersistence
import com.demo.server.epmigration.project.dto.CreateProjectRequest
import com.demo.server.epmigration.project.dto.CreateProjectResponse
import org.springframework.stereotype.Component
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.methods.response.TransactionReceipt
import java.math.BigInteger

@Component
class TopazLifecycleGateway(
    private val properties: EpChainProperties,
    private val transactionSender: ContractTransactionSender,
    private val web3j: Web3j,
    private val credentials: Credentials,
    private val projectSummaryPersistence: ProjectSummaryPersistence
) {
    init {
        if (!addressPattern.matches(properties.lifecycleContractAddress.trim())) {
            throw IllegalStateException("ep.chain.lifecycle-contract-address must be a 20-byte hex address")
        }
    }

    fun createProject(input: CreateProjectRequest): CreateProjectResponse {
        val submitted = transactionSender.sendWriteFunction(
            contractAddress = properties.lifecycleContractAddress.trim(),
            functionName = TopazLifecycle.FUNC_CREATEPROJECT,
            inputParameters = listOf(input),
            externalProjectId = input.externalProjectId
        )
        if (properties.persistProjectSummary) {
            persistProjectSummary(submitted.transactionHash)
        }
        return CreateProjectResponse(
            transactionHash = submitted.transactionHash,
            externalProjectId = input.externalProjectId,
            from = submitted.from,
            to = submitted.to,
            nonce = submitted.nonce
        )
    }

    private fun persistProjectSummary(transactionHash: String) {
        val receipt = waitForReceipt(transactionHash)
        if (!receipt.isStatusOK) {
            throw IllegalStateException("createProject transaction failed: $transactionHash status=${receipt.status}")
        }

        val projectCreated = TopazLifecycle.getProjectCreatedEvents(receipt).singleOrNull()
            ?: throw IllegalStateException("ProjectCreated event not found in transaction $transactionHash")
        val lifecycle = TopazLifecycle.load(
            properties.lifecycleContractAddress.trim(),
            web3j,
            credentials,
            properties.gasPrice,
            properties.gasLimit
        )
        val summary = lifecycle.getProjectSummary(projectCreated.projectId).send()
        projectSummaryPersistence.save(projectCreated.projectId, summary)
    }

    private fun waitForReceipt(transactionHash: String): TransactionReceipt {
        val deadline = System.currentTimeMillis() + RECEIPT_TIMEOUT_MILLIS
        while (System.currentTimeMillis() < deadline) {
            val response = web3j.ethGetTransactionReceipt(transactionHash).send()
            if (response.hasError()) {
                throw IllegalStateException("eth_getTransactionReceipt failed: ${response.error.message}")
            }
            val receipt = response.transactionReceipt
            if (receipt.isPresent) {
                return receipt.get()
            }
            Thread.sleep(RECEIPT_POLL_INTERVAL_MILLIS)
        }
        throw IllegalStateException("Timed out waiting for transaction receipt: $transactionHash")
    }

    companion object {
        private val addressPattern = Regex("^0x[0-9a-fA-F]{40}$")
        private const val RECEIPT_TIMEOUT_MILLIS = 30_000L
        private const val RECEIPT_POLL_INTERVAL_MILLIS = 250L
    }
}
