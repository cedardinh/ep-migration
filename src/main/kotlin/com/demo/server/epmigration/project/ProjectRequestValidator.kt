package com.demo.server.epmigration.project

import com.demo.server.epmigration.chain.generated.TopazLifecycle
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
        validateParticipant("developer", request.developer)
        if (request.developerRequest.legalName.isBlank()) {
            throw BadProjectRequestException("developer.legalName must not be blank")
        }
        if (request.mainContractors.isEmpty()) {
            throw BadProjectRequestException("mainContractors must not be empty")
        }
        request.mainContractorRequests.forEachIndexed { index, participant ->
            validateParticipant("mainContractors[$index]", participant)
            if (participant.legalName.isBlank()) {
                throw BadProjectRequestException("mainContractors[$index].legalName must not be blank")
            }
        }
        validateApprovers("claimApprovers", request.claimApprovers)
        if (request.paymentApprovers.isEmpty()) {
            throw BadProjectRequestException("paymentApprovers must not be empty")
        }
        validateApprovers("paymentApprovers", request.paymentApprovers)
    }

    private fun validateParticipant(label: String, participant: TopazLifecycle.Participant) {
        if (isZeroAddress(participant.wallet)) {
            throw BadProjectRequestException("$label.wallet must be a non-zero 20-byte hex address")
        }
    }

    private fun validateApprovers(
        label: String,
        approvers: List<TopazLifecycle.ApproverConfig>
    ) {
        approvers.forEachIndexed { index, approver ->
            if (isZeroAddress(approver.wallet)) {
                throw BadProjectRequestException("$label[$index].wallet must be a non-zero 20-byte hex address")
            }
            if (isZeroBytes32(approver.userHash)) {
                throw BadProjectRequestException("$label[$index].userHash must be a non-zero bytes32 hex value")
            }
        }
    }

    private fun isZeroAddress(value: String): Boolean {
        return value.equals(ZERO_ADDRESS, ignoreCase = true)
    }

    private fun isZeroBytes32(value: ByteArray): Boolean {
        return value.all { it.toInt() == 0 }
    }

    companion object {
        private const val ZERO_ADDRESS = "0x0000000000000000000000000000000000000000"
    }
}
