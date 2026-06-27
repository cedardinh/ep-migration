package com.demo.server.epmigration.chain.gateway

import com.demo.server.epmigration.chain.generated.TopazLifecycle
import com.demo.server.epmigration.chain.tx.ChainCallContext
import com.demo.server.epmigration.chain.tx.ContractTransactionSender
import com.demo.server.epmigration.chain.tx.ResilientNonceManager
import com.demo.server.epmigration.chain.tx.SubmittedTransaction
import com.demo.server.epmigration.config.EpChainProperties
import com.demo.server.epmigration.observability.ChainCallReporter
import com.demo.server.epmigration.project.dto.ApproverRequest
import com.demo.server.epmigration.project.dto.CreateProjectRequest
import com.demo.server.epmigration.project.dto.ParticipantRequest
import com.demo.server.epmigration.project.persistence.NoopProjectSummaryPersistence
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.web3j.abi.datatypes.Type
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j

class TopazLifecycleGatewayTests {
    @Test
    fun `gateway rejects malformed lifecycle contract address at startup`() {
        listOf("", " ", "0x1234", "not-an-address").forEach { address ->
            val properties = EpChainProperties().apply { lifecycleContractAddress = address }

            val ex = assertThrows(IllegalStateException::class.java, {
                TopazLifecycleGateway(properties, CapturingSender(), Mockito.mock(Web3j::class.java), Credentials.create(PRIVATE_KEY), NoopProjectSummaryPersistence)
            }, address)

            assertEquals("ep.chain.lifecycle-contract-address must be a 20-byte hex address", ex.message)
        }
    }

    @Test
    fun `create project sends lifecycle function and maps submitted transaction`() {
        val sender = CapturingSender()
        val properties = EpChainProperties().apply {
            lifecycleContractAddress = "  $CONTRACT_ADDRESS  "
            persistProjectSummary = false
        }
        val gateway = TopazLifecycleGateway(
            properties,
            sender,
            Mockito.mock(Web3j::class.java),
            Credentials.create(PRIVATE_KEY),
            NoopProjectSummaryPersistence
        )
        val request = sampleRequest()

        val response = gateway.createProject(request)

        assertEquals(CONTRACT_ADDRESS, sender.contractAddress)
        assertEquals(TopazLifecycle.FUNC_CREATEPROJECT, sender.functionName)
        assertEquals(request, sender.inputParameters.single())
        assertEquals(request.externalProjectId, sender.externalProjectId)
        assertEquals("0xsubmitted", response.transactionHash)
        assertEquals(request.externalProjectId, response.externalProjectId)
        assertEquals("0xfrom", response.from)
        assertEquals(CONTRACT_ADDRESS, response.to)
        assertEquals("9", response.nonce)
    }

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

    private class CapturingSender : ContractTransactionSender(
        properties = EpChainProperties(),
        credentials = Credentials.create(PRIVATE_KEY),
        nonceManager = ResilientNonceManager(Mockito.mock(Web3j::class.java), Credentials.create(PRIVATE_KEY)),
        reporter = ChainCallReporter()
    ) {
        lateinit var contractAddress: String
        lateinit var functionName: String
        lateinit var inputParameters: List<Type<*>>
        var externalProjectId: String? = null

        override fun sendWriteFunction(
            contractAddress: String,
            functionName: String,
            inputParameters: List<Type<*>>,
            externalProjectId: String?
        ): SubmittedTransaction {
            this.contractAddress = contractAddress
            this.functionName = functionName
            this.inputParameters = inputParameters
            this.externalProjectId = externalProjectId
            return SubmittedTransaction(
                transactionHash = "0xsubmitted",
                nonce = "9",
                from = "0xfrom",
                to = contractAddress,
                functionName = functionName,
                externalProjectId = externalProjectId
            )
        }
    }

    companion object {
        private const val CONTRACT_ADDRESS = "0x0000000000000000000000000000000000000001"
        private const val PRIVATE_KEY = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
        private const val WALLET = "0x628d684197485c054cda7d3def46e8be6b3d174c"

        private fun bytes32(value: Int): String {
            return "0x" + value.toString(16).padStart(64, '0')
        }
    }
}
