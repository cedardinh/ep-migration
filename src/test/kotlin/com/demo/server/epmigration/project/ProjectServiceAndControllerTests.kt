package com.demo.server.epmigration.project

import com.demo.server.epmigration.chain.gateway.TopazLifecycleGateway
import com.demo.server.epmigration.chain.tx.ContractTransactionSender
import com.demo.server.epmigration.chain.tx.ResilientNonceManager
import com.demo.server.epmigration.config.EpChainProperties
import com.demo.server.epmigration.observability.ChainCallReporter
import com.demo.server.epmigration.project.dto.ApproverRequest
import com.demo.server.epmigration.project.dto.CreateProjectRequest
import com.demo.server.epmigration.project.dto.CreateProjectResponse
import com.demo.server.epmigration.project.dto.ParticipantRequest
import com.demo.server.epmigration.project.persistence.NoopProjectSummaryPersistence
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j

class ProjectServiceAndControllerTests {
    @Test
    fun `service validates then delegates create project`() {
        val validator = RecordingValidator()
        val gateway = RecordingGateway()
        val service = ProjectService(validator, gateway)
        val request = sampleRequest()

        val response = service.createProject(request)

        assertSame(request, validator.validatedRequest)
        assertSame(request, gateway.request)
        assertEquals("0xsubmitted", response.transactionHash)
    }

    @Test
    fun `service does not submit to gateway when validation fails`() {
        val validator = RecordingValidator(BadProjectRequestException("bad request"))
        val gateway = RecordingGateway()
        val service = ProjectService(validator, gateway)
        val request = sampleRequest()

        val ex = assertThrows(BadProjectRequestException::class.java) {
            service.createProject(request)
        }

        assertEquals("bad request", ex.message)
        assertEquals(null, gateway.request)
    }

    @Test
    fun `controller delegates create project to service`() {
        val service = RecordingService()
        val controller = ProjectController(service)
        val request = sampleRequest()

        val response = controller.createProject(request)

        assertSame(request, service.request)
        assertEquals("project-1", response.externalProjectId)
    }

    private class RecordingValidator(
        private val failure: BadProjectRequestException? = null
    ) : ProjectRequestValidator() {
        var validatedRequest: CreateProjectRequest? = null

        override fun validateCreateProject(request: CreateProjectRequest) {
            validatedRequest = request
            if (failure != null) {
                throw failure
            }
        }
    }

    private class RecordingGateway : TopazLifecycleGateway(
        validProperties(),
        dummySender(),
        Mockito.mock(Web3j::class.java),
        Credentials.create(PRIVATE_KEY),
        NoopProjectSummaryPersistence
    ) {
        var request: CreateProjectRequest? = null

        override fun createProject(input: CreateProjectRequest): CreateProjectResponse {
            request = input
            return response(input.externalProjectId)
        }
    }

    private class RecordingService : ProjectService(ProjectRequestValidator(), RecordingGateway()) {
        var request: CreateProjectRequest? = null

        override fun createProject(request: CreateProjectRequest): CreateProjectResponse {
            this.request = request
            return response(request.externalProjectId)
        }
    }

    companion object {
        private const val CONTRACT_ADDRESS = "0x0000000000000000000000000000000000000001"
        private const val PRIVATE_KEY = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
        private const val WALLET = "0x628d684197485c054cda7d3def46e8be6b3d174c"

        private fun sampleRequest(): CreateProjectRequest {
            return CreateProjectRequest(
                externalProjectId = "project-1",
                name = "Project 1",
                developer = ParticipantRequest(wallet = WALLET, legalName = "Developer", externalRef = "developer"),
                mainContractors = listOf(
                    ParticipantRequest(wallet = WALLET, legalName = "Contractor", externalRef = "contractor")
                ),
                claimApprovers = emptyList(),
                paymentApprovers = listOf(
                    ApproverRequest(wallet = WALLET, userHash = bytes32(1), roleName = "Payment", externalRef = "payment")
                ),
                bankAccountRefs = listOf("bank-1")
            )
        }

        private fun response(externalProjectId: String): CreateProjectResponse {
            return CreateProjectResponse(
                transactionHash = "0xsubmitted",
                externalProjectId = externalProjectId,
                from = "0xfrom",
                to = CONTRACT_ADDRESS,
                nonce = "1"
            )
        }

        private fun validProperties(): EpChainProperties {
            return EpChainProperties().apply {
                lifecycleContractAddress = CONTRACT_ADDRESS
                persistProjectSummary = false
            }
        }

        private fun dummySender(): ContractTransactionSender {
            val credentials = Credentials.create(PRIVATE_KEY)
            return ContractTransactionSender(
                properties = EpChainProperties(),
                credentials = credentials,
                nonceManager = ResilientNonceManager(Mockito.mock(Web3j::class.java), credentials),
                reporter = ChainCallReporter(),
                web3j = Mockito.mock(Web3j::class.java)
            )
        }

        private fun bytes32(value: Int): String {
            return "0x" + value.toString(16).padStart(64, '0')
        }
    }
}
