package com.demo.server.epmigration.project

import com.demo.server.epmigration.project.dto.CreateProjectRequest
import org.springframework.stereotype.Component

@Component
class ProjectRequestValidator {
    fun validateCreateProject(request: CreateProjectRequest) {
        if (request.externalProjectId.isBlank()) {
            throw BadProjectRequestException("externalProjectId must not be blank")
        }
        if (request.name.isBlank()) {
            throw BadProjectRequestException("name must not be blank")
        }
        if (request.developerRequest.legalName.isBlank()) {
            throw BadProjectRequestException("developer.legalName must not be blank")
        }
        if (request.mainContractors.isEmpty()) {
            throw BadProjectRequestException("mainContractors must not be empty")
        }
        request.mainContractorRequests.forEachIndexed { index, participant ->
            if (participant.legalName.isBlank()) {
                throw BadProjectRequestException("mainContractors[$index].legalName must not be blank")
            }
        }
        if (request.paymentApprovers.isEmpty()) {
            throw BadProjectRequestException("paymentApprovers must not be empty")
        }
    }
}
