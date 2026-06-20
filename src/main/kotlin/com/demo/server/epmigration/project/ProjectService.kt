package com.demo.server.epmigration.project

import com.demo.server.epmigration.chain.gateway.TopazLifecycleGateway
import com.demo.server.epmigration.project.dto.CreateProjectRequest
import com.demo.server.epmigration.project.dto.CreateProjectResponse
import org.springframework.stereotype.Service

@Service
class ProjectService(
    private val validator: ProjectRequestValidator,
    private val gateway: TopazLifecycleGateway
) {
    fun createProject(request: CreateProjectRequest, correlationId: String): CreateProjectResponse {
        validator.validateCreateProject(request, correlationId)
        val result = gateway.createProject(request, correlationId)
        return CreateProjectResponse(
            transactionHash = result.transactionHash,
            externalProjectId = result.externalProjectId,
            from = result.from,
            to = result.to,
            nonce = result.nonce
        )
    }
}
