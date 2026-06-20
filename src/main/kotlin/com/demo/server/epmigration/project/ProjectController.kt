package com.demo.server.epmigration.project

import com.demo.server.epmigration.project.dto.CreateProjectRequest
import com.demo.server.epmigration.project.dto.CreateProjectResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/projects")
class ProjectController(
    private val projectService: ProjectService
) {
    @PostMapping
    fun createProject(
        @RequestBody request: CreateProjectRequest,
        @RequestHeader("X-Request-Id", required = false) requestId: String?
    ): CreateProjectResponse {
        val correlationId = requestId ?: UUID.randomUUID().toString()
        return projectService.createProject(request, correlationId)
    }
}
