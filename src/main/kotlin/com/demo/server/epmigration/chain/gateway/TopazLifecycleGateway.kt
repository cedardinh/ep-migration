package com.demo.server.epmigration.chain.gateway

import com.demo.server.epmigration.chain.generated.TopazLifecycle
import com.demo.server.epmigration.chain.tx.ContractTransactionSender
import com.demo.server.epmigration.config.EpChainProperties
import com.demo.server.epmigration.project.dto.CreateProjectRequest
import com.demo.server.epmigration.project.dto.CreateProjectResponse
import org.springframework.stereotype.Component

@Component
class TopazLifecycleGateway(
    private val properties: EpChainProperties,
    private val transactionSender: ContractTransactionSender
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
        return CreateProjectResponse(
            transactionHash = submitted.transactionHash,
            externalProjectId = input.externalProjectId,
            from = submitted.from,
            to = submitted.to,
            nonce = submitted.nonce
        )
    }

    companion object {
        private val addressPattern = Regex("^0x[0-9a-fA-F]{40}$")
    }
}
