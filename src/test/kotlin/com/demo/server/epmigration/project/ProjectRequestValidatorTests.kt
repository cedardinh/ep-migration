package com.demo.server.epmigration.project

import com.demo.server.epmigration.project.dto.ApproverRequest
import com.demo.server.epmigration.project.dto.CreateProjectRequest
import com.demo.server.epmigration.project.dto.ParticipantRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ProjectRequestValidatorTests {
    private val validator = ProjectRequestValidator()

    @Test
    fun `valid create project request passes validation`() {
        validator.validateCreateProject(validRequest())
    }

    @Test
    fun `valid create project request supports empty claim approvers`() {
        validator.validateCreateProject(validRequest(claimApprovers = emptyList()))
    }

    @Test
    fun `invalid create project requests are rejected before chain submission`() {
        val cases = listOf(
            InvalidCase(
                "blank externalProjectId",
                validRequest(externalProjectId = " "),
                "externalProjectId must not be blank"
            ),
            InvalidCase(
                "blank name",
                validRequest(name = "\t"),
                "name must not be blank"
            ),
            InvalidCase(
                "invalid developer wallet",
                validRequest(developer = participant(wallet = "not-an-address")),
                "developer.wallet must be a non-zero 20-byte hex address"
            ),
            InvalidCase(
                "mixed-case zero developer wallet",
                validRequest(developer = participant(wallet = "0X0000000000000000000000000000000000000000")),
                "developer.wallet must be a non-zero 20-byte hex address"
            ),
            InvalidCase(
                "blank developer legalName",
                validRequest(developer = participant(legalName = "")),
                "developer.legalName must not be blank"
            ),
            InvalidCase(
                "empty main contractors",
                validRequest(mainContractors = emptyList()),
                "mainContractors must not be empty"
            ),
            InvalidCase(
                "invalid main contractor wallet",
                validRequest(mainContractors = listOf(participant(wallet = "bad"))),
                "mainContractors[0].wallet must be a non-zero 20-byte hex address"
            ),
            InvalidCase(
                "blank main contractor legalName",
                validRequest(mainContractors = listOf(participant(legalName = " "))),
                "mainContractors[0].legalName must not be blank"
            ),
            InvalidCase(
                "invalid claim approver wallet",
                validRequest(claimApprovers = listOf(approver(bytes32(1), "Claim Approver", wallet = "bad"))),
                "claimApprovers[0].wallet must be a non-zero 20-byte hex address"
            ),
            InvalidCase(
                "zero claim approver user hash",
                validRequest(claimApprovers = listOf(approver(ZERO_BYTES32, "Claim Approver"))),
                "claimApprovers[0].userHash must be a non-zero bytes32 hex value"
            ),
            InvalidCase(
                "empty payment approvers",
                validRequest(paymentApprovers = emptyList()),
                "paymentApprovers must not be empty"
            ),
            InvalidCase(
                "invalid payment approver wallet",
                validRequest(paymentApprovers = listOf(approver(bytes32(2), "Payment Approver", wallet = "bad"))),
                "paymentApprovers[0].wallet must be a non-zero 20-byte hex address"
            ),
            InvalidCase(
                "zero payment approver user hash",
                validRequest(paymentApprovers = listOf(approver(ZERO_BYTES32, "Payment Approver"))),
                "paymentApprovers[0].userHash must be a non-zero bytes32 hex value"
            )
        )

        cases.forEach { case ->
            val ex = assertThrows(BadProjectRequestException::class.java, {
                validator.validateCreateProject(case.request)
            }, case.label)
            assertEquals(case.expectedMessage, ex.message, case.label)
        }
    }

    private fun validRequest(
        externalProjectId: String = "project-1",
        name: String = "Project 1",
        developer: ParticipantRequest = ParticipantRequest(
            wallet = WALLET,
            legalName = "Developer",
            externalRef = "developer"
        ),
        mainContractors: List<ParticipantRequest> = listOf(contractor("Contractor", "contractor")),
        claimApprovers: List<ApproverRequest> = listOf(approver(bytes32(1), "Claim Approver")),
        paymentApprovers: List<ApproverRequest> = listOf(approver(bytes32(2), "Payment Approver"))
    ): CreateProjectRequest {
        return CreateProjectRequest(
            externalProjectId = externalProjectId,
            name = name,
            developer = developer,
            mainContractors = mainContractors,
            claimApprovers = claimApprovers,
            paymentApprovers = paymentApprovers,
            bankAccountRefs = listOf("dev-bank")
        )
    }

    private fun participant(
        wallet: String = WALLET,
        legalName: String = "Participant",
        externalRef: String = "participant"
    ): ParticipantRequest {
        return ParticipantRequest(
            wallet = wallet,
            legalName = legalName,
            externalRef = externalRef
        )
    }

    private fun contractor(legalName: String, externalRef: String): ParticipantRequest {
        return participant(legalName = legalName, externalRef = externalRef)
    }

    private fun approver(
        userHash: String,
        roleName: String,
        wallet: String = WALLET
    ): ApproverRequest {
        return ApproverRequest(
            wallet = wallet,
            userHash = userHash,
            roleName = roleName,
            externalRef = roleName
        )
    }

    private fun bytes32(value: Int): String {
        return "0x" + value.toString(16).padStart(64, '0')
    }

    private data class InvalidCase(
        val label: String,
        val request: CreateProjectRequest,
        val expectedMessage: String
    )

    companion object {
        private const val WALLET = "0x628d684197485c054cda7d3def46e8be6b3d174c"
        private const val ZERO_BYTES32 = "0x0000000000000000000000000000000000000000000000000000000000000000"
    }
}
