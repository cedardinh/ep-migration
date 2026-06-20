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
    fun createProject(input: CreateProjectRequest): CreateProjectResponse {
        val submitted = transactionSender.sendWriteFunction(
            contractAddress = properties.lifecycleContractAddress,
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
}
