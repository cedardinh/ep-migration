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
import org.web3j.tx.gas.StaticGasProvider

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
        val lifecycle = TopazLifecycle.load(
            properties.lifecycleContractAddress.trim(),
            web3j,
            transactionSender,
            StaticGasProvider(properties.gasPrice, properties.gasLimit)
        )
        val result = transactionSender.sendGeneratedTransaction(
            functionName = TopazLifecycle.FUNC_CREATEPROJECT,
            externalProjectId = input.externalProjectId,
            call = lifecycle.createProject(input)
        )
        if (properties.persistProjectSummary) {
            persistProjectSummary(result.submitted.transactionHash)
        }
        return CreateProjectResponse(
            transactionHash = result.submitted.transactionHash,
            externalProjectId = input.externalProjectId,
            from = result.submitted.from,
            to = result.submitted.to,
            nonce = result.submitted.nonce
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
            StaticGasProvider(properties.gasPrice, properties.gasLimit)
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
