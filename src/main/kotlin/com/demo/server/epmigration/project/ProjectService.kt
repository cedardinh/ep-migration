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
    fun createProject(request: CreateProjectRequest): CreateProjectResponse {
        validator.validateCreateProject(request)
        return gateway.createProject(request)
    }
}
