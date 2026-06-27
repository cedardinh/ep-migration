package com.demo.server.epmigration.integration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.demo.server.epmigration.chain.generated.TopazLifecycle
import com.demo.server.epmigration.chain.gateway.TopazLifecycleGateway
import com.demo.server.epmigration.chain.tx.ContractRevertDecoder
import com.demo.server.epmigration.chain.tx.ContractTransactionSender
import com.demo.server.epmigration.chain.tx.ResilientNonceManager
import com.demo.server.epmigration.config.EpChainProperties
import com.demo.server.epmigration.observability.ChainCallReporter
import com.demo.server.epmigration.project.ProjectRequestValidator
import com.demo.server.epmigration.project.ProjectService
import com.demo.server.epmigration.project.dto.ApproverRequest
import com.demo.server.epmigration.project.dto.CreateProjectRequest
import com.demo.server.epmigration.project.dto.ParticipantRequest
import com.demo.server.epmigration.project.persistence.NoopProjectSummaryPersistence
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Function
import org.web3j.crypto.Credentials
import org.web3j.crypto.Hash
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.core.methods.response.TransactionReceipt
import org.web3j.protocol.http.HttpService
import org.web3j.utils.Numeric
import java.io.File
import java.math.BigInteger
import java.util.Locale

@Tag("integration")
class DockerBesuCreateProjectIntegrationTests {
    private val mapper = jacksonObjectMapper()
    private var web3j: Web3j? = null

    @AfterEach
    fun tearDown() {
        web3j?.shutdown()
    }

    @Test
    fun `createProject submits to Docker Besu and generated wrapper reads the stored project`() {
        val deployment = mapper.readTree(File("contracts/deployments/docker-besu-1337.json"))
        val config = mapper.readTree(File("contracts/config/docker-besu.local.json"))
        val rpcUrl = config.path("network").path("rpcUrl").asText("http://127.0.0.1:8546")
        val chainId = config.path("network").path("chainId").asLong(1337L)
        val contractAddress = deployment.path("contracts").path("topazLifecycle").asText()
        val privateKey = config.path("deployment").path("deployerPrivateKey").asText()

        require(contractAddress.matches(Regex("^0x[0-9a-fA-F]{40}$"))) {
            "contracts/deployments/docker-besu-1337.json must contain contracts.topazLifecycle"
        }
        require(privateKey.matches(Regex("^(0x)?[0-9a-fA-F]{64}$"))) {
            "contracts/config/docker-besu.local.json must contain deployment.deployerPrivateKey"
        }

        val localWeb3j = Web3j.build(HttpService(rpcUrl))
        web3j = localWeb3j
        val credentials = Credentials.create(Numeric.cleanHexPrefix(privateKey))
        val properties = EpChainProperties().apply {
            this.rpcUrl = rpcUrl
            this.chainId = chainId
            lifecycleContractAddress = contractAddress
            signerPrivateKey = privateKey
            gasPrice = BigInteger.ZERO
            gasLimit = BigInteger.valueOf(5_000_000L)
            persistProjectSummary = false
        }
        val service = ProjectService(
            ProjectRequestValidator(),
            TopazLifecycleGateway(
                properties,
                ContractTransactionSender(
                    properties,
                    credentials,
                    ResilientNonceManager(localWeb3j, credentials),
                    ChainCallReporter()
                ),
                localWeb3j,
                credentials,
                NoopProjectSummaryPersistence
            )
        )
        val request = sampleRequest("INT-${System.currentTimeMillis()}")

        val response = service.createProject(request)
        val receipt = waitForReceipt(localWeb3j, response.transactionHash)

        assertEquals("0x1", receipt.status)
        assertEquals(contractAddress.toLowerCase(Locale.US), response.to.toLowerCase(Locale.US))

        val projectCreated = TopazLifecycle.getProjectCreatedEvents(receipt).single()
        assertEquals(request.externalProjectId, projectCreated.externalProjectId)
        assertEquals(request.developer.wallet.toLowerCase(Locale.US), projectCreated.developerWallet.toLowerCase(Locale.US))

        val lifecycle = TopazLifecycle.load(contractAddress, localWeb3j, credentials, BigInteger.ZERO, properties.gasLimit)
        val summary = lifecycle.getProjectSummary(projectCreated.projectId).send()

        assertEquals(request.externalProjectId, summary.value1)
        assertEquals(request.name, summary.value2)
        assertEquals(BigInteger.ONE, summary.value3)
        assertEquals(request.developer.wallet.toLowerCase(Locale.US), summary.value4.wallet.toLowerCase(Locale.US))
        assertEquals(request.mainContractors.size, summary.value5.size)
        assertEquals(request.claimApprovers.size, summary.value6.size)
        assertEquals(request.paymentApprovers.size, summary.value7.size)
        assertEquals(request.bankAccountRefs, summary.value8)
        assertEquals(BigInteger.ZERO, summary.value11)

        val duplicateCall = localWeb3j.ethCall(
            Transaction.createEthCallTransaction(
                credentials.address,
                contractAddress,
                createProjectCalldata(request)
            ),
            DefaultBlockParameterName.LATEST
        ).send()
        assertTrue(duplicateCall.hasError() || duplicateCall.isReverted || duplicateCall.reverts())

        val decoded = ContractRevertDecoder.decode(
            ContractRevertDecoder.extractHex(
                duplicateCall.error?.data,
                duplicateCall.error?.message,
                duplicateCall.revertReason,
                duplicateCall.value
            )
        )
        assertNotNull(decoded, "Duplicate createProject eth_call should expose decodable revert data")
        assertEquals("CustomError", decoded!!.kind)
        assertEquals(selector("DuplicateProjectId(string)"), decoded.selector)
    }

    private fun waitForReceipt(web3j: Web3j, transactionHash: String): TransactionReceipt {
        val deadline = System.currentTimeMillis() + 15_000L
        while (System.currentTimeMillis() < deadline) {
            val response = web3j.ethGetTransactionReceipt(transactionHash).send()
            if (response.hasError()) {
                throw AssertionError("eth_getTransactionReceipt failed: ${response.error.message}")
            }
            val receipt = response.transactionReceipt
            if (receipt.isPresent) {
                return receipt.get()
            }
            Thread.sleep(250L)
        }
        throw AssertionError("Timed out waiting for transaction receipt: $transactionHash")
    }

    private fun createProjectCalldata(request: CreateProjectRequest): String {
        val function = Function(
            TopazLifecycle.FUNC_CREATEPROJECT,
            listOf(request),
            emptyList<TypeReference<*>>()
        )
        return FunctionEncoder.encode(function)
    }

    private fun selector(signature: String): String {
        return Hash.sha3String(signature).substring(0, 10)
    }

    private fun sampleRequest(externalProjectId: String): CreateProjectRequest {
        return CreateProjectRequest(
            externalProjectId = externalProjectId,
            name = "Docker Besu Integration Project",
            developer = ParticipantRequest(
                wallet = WALLET,
                legalName = "Developer Ltd",
                addressLine1 = "",
                addressLine2 = "",
                bic = "",
                lei = "",
                externalRef = "developer"
            ),
            mainContractors = listOf(
                ParticipantRequest(
                    wallet = WALLET,
                    legalName = "Contractor Ltd",
                    addressLine1 = "",
                    addressLine2 = "",
                    bic = "",
                    lei = "",
                    externalRef = "contractor"
                )
            ),
            claimApprovers = listOf(
                ApproverRequest(
                    wallet = WALLET,
                    userHash = "0xf09b66dfb6bd1bb5e7d2be0b15a80542e02b79b94ea63cd7e918ac65b1164a9a",
                    roleName = "Claim Approver",
                    externalRef = "claim-approver"
                )
            ),
            paymentApprovers = listOf(
                ApproverRequest(
                    wallet = WALLET,
                    userHash = "0x06a649d9b77f6b7a90a57443026f693d362b91ab6d64aac3557edef254d5efeb",
                    roleName = "Payment Approver",
                    externalRef = "payment-approver"
                )
            ),
            bankAccountRefs = listOf("dev-bank")
        )
    }

    companion object {
        private const val WALLET = "0x628d684197485c054cda7d3def46e8be6b3d174c"
    }
}
