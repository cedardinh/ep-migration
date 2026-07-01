package com.demo.server.epmigration.chain.tx

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.demo.server.epmigration.chain.generated.TopazLifecycle
import com.demo.server.epmigration.config.EpChainProperties
import com.demo.server.epmigration.observability.ChainCallReporter
import com.demo.server.epmigration.project.dto.ApproverRequest
import com.demo.server.epmigration.project.dto.CreateProjectRequest
import com.demo.server.epmigration.project.dto.ParticipantRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.tx.gas.StaticGasProvider
import java.io.File
import java.math.BigInteger

class ContractTransactionSenderTests {
    private val mapper = jacksonObjectMapper()

    @Test
    fun `send generated transaction uses web3j wrapper calldata`() {
        val request = sampleRequest()
        val properties = EpChainProperties().apply {
            chainId = 31337L
            gasPrice = BigInteger.valueOf(9L)
            gasLimit = BigInteger.valueOf(123_456L)
        }
        val credentials = Credentials.create(PRIVATE_KEY)
        val nonceManager = CapturingNonceManager(credentials)
        val sender = ContractTransactionSender(
            properties = properties,
            credentials = credentials,
            nonceManager = nonceManager,
            reporter = ChainCallReporter(),
            web3j = Mockito.mock(Web3j::class.java)
        )
        val lifecycle = generatedLifecycle(sender, properties)

        val submitted = sender.sendGeneratedTransaction(
            functionName = TopazLifecycle.FUNC_CREATEPROJECT,
            externalProjectId = request.externalProjectId,
            call = lifecycle.createProject(request)
        ).submitted

        assertEquals(CONTRACT_ADDRESS, nonceManager.to)
        assertEquals(ethersEncode(sampleRequestJson()), nonceManager.data)
        assertEquals(properties.gasPrice, nonceManager.gasPrice)
        assertEquals(properties.gasLimit, nonceManager.gasLimit)
        assertEquals(properties.chainId, nonceManager.chainId)
        assertEquals(TopazLifecycle.FUNC_CREATEPROJECT, nonceManager.context.op)
        assertEquals(credentials.address, nonceManager.context.from)
        assertEquals(CONTRACT_ADDRESS, nonceManager.context.to)
        assertEquals(request.externalProjectId, submitted.externalProjectId)
    }

    @Test
    fun `send generated transaction defaults external project id to null`() {
        val request = sampleRequest()
        val properties = EpChainProperties().apply {
            chainId = 31337L
            gasPrice = BigInteger.valueOf(9L)
            gasLimit = BigInteger.valueOf(123_456L)
        }
        val credentials = Credentials.create(PRIVATE_KEY)
        val nonceManager = CapturingNonceManager(credentials)
        val sender = ContractTransactionSender(
            properties = properties,
            credentials = credentials,
            nonceManager = nonceManager,
            reporter = ChainCallReporter(),
            web3j = Mockito.mock(Web3j::class.java)
        )
        val lifecycle = generatedLifecycle(sender, properties)

        val submitted = sender.sendGeneratedTransaction(
            functionName = TopazLifecycle.FUNC_CREATEPROJECT,
            call = lifecycle.createProject(request)
        ).submitted

        assertNull(nonceManager.context.externalProjectId)
        assertNull(submitted.externalProjectId)
    }

    @Test
    fun `send generated transaction rejects invalid transaction configuration before nonce manager`() {
        val request = sampleRequest()
        val cases = listOf(
            InvalidConfig(
                "chain id",
                EpChainProperties().apply { chainId = 0L },
                "ep.chain.chain-id must be positive"
            ),
            InvalidConfig(
                "gas price",
                EpChainProperties().apply { gasPrice = BigInteger.valueOf(-1L) },
                "ep.chain.gas-price must be greater than or equal to 0"
            ),
            InvalidConfig(
                "gas limit",
                EpChainProperties().apply { gasLimit = BigInteger.ZERO },
                "ep.chain.gas-limit must be positive"
            )
        )

        cases.forEach { case ->
            val credentials = Credentials.create(PRIVATE_KEY)
            val nonceManager = CapturingNonceManager(credentials)
            val sender = ContractTransactionSender(
                properties = case.properties,
                credentials = credentials,
                nonceManager = nonceManager,
                reporter = ChainCallReporter(),
                web3j = Mockito.mock(Web3j::class.java)
            )
            val lifecycle = generatedLifecycle(sender, case.properties)

            val ex = assertThrows(IllegalStateException::class.java, {
                sender.sendGeneratedTransaction(
                    functionName = TopazLifecycle.FUNC_CREATEPROJECT,
                    externalProjectId = request.externalProjectId,
                    call = lifecycle.createProject(request)
                )
            }, case.label)

            assertEquals(case.expectedMessage, ex.message, case.label)
            assertEquals(false, nonceManager.called, case.label)
        }
    }

    @Test
    fun `with hex prefix preserves prefixed values and prefixes bare values`() {
        val credentials = Credentials.create(PRIVATE_KEY)
        val sender = ContractTransactionSender(
            properties = EpChainProperties(),
            credentials = credentials,
            nonceManager = CapturingNonceManager(credentials),
            reporter = ChainCallReporter(),
            web3j = Mockito.mock(Web3j::class.java)
        )

        assertEquals("0xabc", withHexPrefix(sender, "0xabc"))
        assertEquals("0xabc", withHexPrefix(sender, "abc"))
    }

    @Test
    fun `encode createProject matches ethers calldata`() {
        val request = sampleRequest()

        assertEquals(
            ethersEncode(sampleRequestJson()),
            createProjectCalldata(request)
        )
    }

    @Test
    fun `encode createProject supports empty claim approvers`() {
        val request = sampleRequest(claimApprovers = emptyList())

        assertEquals(
            ethersEncode(sampleRequestJson(claimApproversJson = "[]")),
            createProjectCalldata(request)
        )
    }

    @Test
    fun `controller json request is already a contract input`() {
        val request = mapper.readValue<CreateProjectRequest>(sampleRequestJson())

        assertEquals(
            ethersEncode(sampleRequestJson()),
            createProjectCalldata(request)
        )
    }

    private fun createProjectCalldata(request: CreateProjectRequest): String {
        val lifecycle = TopazLifecycle.load(
            CONTRACT_ADDRESS,
            Mockito.mock(Web3j::class.java),
            Credentials.create(PRIVATE_KEY),
            BigInteger.ZERO,
            BigInteger.ONE
        )
        return lifecycle.createProject(request).encodeFunctionCall()
    }

    private fun generatedLifecycle(
        sender: ContractTransactionSender,
        properties: EpChainProperties
    ): TopazLifecycle {
        return TopazLifecycle.load(
            CONTRACT_ADDRESS,
            Mockito.mock(Web3j::class.java),
            sender,
            StaticGasProvider(properties.gasPrice, properties.gasLimit)
        )
    }

    private fun sampleRequest(
        claimApprovers: List<ApproverRequest> = listOf(
            ApproverRequest(
                wallet = WALLET,
                userHash = "0x61533c4c2e198353cde1c7df7a23852535a93a5d1f2ee39863bb3cf118855a53",
                email = "claim@example.com",
                firstName = "Claim",
                lastName = "Approver",
                userProfileName = "claim-approver",
                roleName = "1",
                externalRef = "Approver Entity"
            )
        )
    ): CreateProjectRequest {
        return CreateProjectRequest(
            externalProjectId = "1",
            name = "1",
            developer = ParticipantRequest(
                wallet = WALLET,
                legalName = "DEVELOPERACCOUNTA",
                addressLine1 = "",
                addressLine2 = "",
                bic = "",
                lei = "",
                externalRef = ""
            ),
            mainContractors = listOf(
                ParticipantRequest(
                    wallet = WALLET,
                    legalName = "A1CordaAccVick1Con",
                    addressLine1 = "",
                    addressLine2 = "",
                    bic = "",
                    lei = "",
                    externalRef = "A1CordaAccVick1Con"
                )
            ),
            claimApprovers = claimApprovers,
            paymentApprovers = listOf(
                ApproverRequest(
                    wallet = WALLET,
                    userHash = "0x06a649d9b77f6b7a90a57443026f693d362b91ab6d64aac3557edef254d5efeb",
                    email = "payment@example.com",
                    firstName = "Payment",
                    lastName = "Approver",
                    userProfileName = "payment-approver",
                    roleName = "1",
                    externalRef = "Approver Entity"
                )
            ),
            bankAccountRefs = listOf("dev-bank")
        )
    }

    private fun sampleRequestJson(
        claimApproversJson: String =
            """[{"wallet":"$WALLET","userHash":"0x61533c4c2e198353cde1c7df7a23852535a93a5d1f2ee39863bb3cf118855a53","email":"claim@example.com","firstName":"Claim","lastName":"Approver","userProfileName":"claim-approver","roleName":"1","externalRef":"Approver Entity"}]"""
    ): String {
        return """
            {
              "externalProjectId": "1",
              "name": "1",
              "developer": {
                "wallet": "$WALLET",
                "legalName": "DEVELOPERACCOUNTA",
                "addressLine1": "",
                "addressLine2": "",
                "bic": "",
                "lei": "",
                "externalRef": ""
              },
              "mainContractors": [
                {
                  "wallet": "$WALLET",
                  "legalName": "A1CordaAccVick1Con",
                  "addressLine1": "",
                  "addressLine2": "",
                  "bic": "",
                  "lei": "",
                  "externalRef": "A1CordaAccVick1Con"
                }
              ],
              "claimApprovers": $claimApproversJson,
              "paymentApprovers": [
                {
                  "wallet": "$WALLET",
                  "userHash": "0x06a649d9b77f6b7a90a57443026f693d362b91ab6d64aac3557edef254d5efeb",
                  "email": "payment@example.com",
                  "firstName": "Payment",
                  "lastName": "Approver",
                  "userProfileName": "payment-approver",
                  "roleName": "1",
                  "externalRef": "Approver Entity"
                }
              ],
              "bankAccountRefs": ["dev-bank"]
            }
        """.trimIndent()
    }

    private fun ethersEncode(inputJson: String): String {
        val script = """
            const { ethers } = require("ethers");
            const artifact = require("./artifacts/contracts/TopazLifecycle.sol/TopazLifecycle.json");
            const iface = new ethers.Interface(artifact.abi);
            const input = JSON.parse(process.argv[1]);
            process.stdout.write(iface.encodeFunctionData("createProject", [input]));
        """.trimIndent()
        val process = ProcessBuilder(nodeExecutable(), "-e", script, inputJson)
            .directory(File("contracts"))
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText().trim()
        val exit = process.waitFor()
        assertEquals(0, exit, output)
        return output
    }

    private fun nodeExecutable(): String {
        val candidates = listOf(
            "/opt/homebrew/bin/node",
            "/usr/local/bin/node",
            "/usr/bin/node"
        )
        return candidates.firstOrNull { File(it).canExecute() } ?: "node"
    }

    private class CapturingNonceManager(credentials: Credentials) :
        ResilientNonceManager(Mockito.mock(Web3j::class.java), credentials) {
        var called: Boolean = false
        lateinit var to: String
        lateinit var data: String
        lateinit var gasPrice: BigInteger
        lateinit var gasLimit: BigInteger
        lateinit var context: ChainCallContext
        var chainId: Long = -1L

        override fun sendRawTransaction(
            to: String,
            data: String,
            gasPrice: BigInteger,
            gasLimit: BigInteger,
            chainId: Long,
            context: ChainCallContext
        ): SubmittedTransaction {
            called = true
            this.to = to
            this.data = data
            this.gasPrice = gasPrice
            this.gasLimit = gasLimit
            this.chainId = chainId
            this.context = context
            return SubmittedTransaction(
                transactionHash = "0xsubmitted",
                nonce = "7",
                from = context.from,
                to = to,
                functionName = context.op,
                externalProjectId = context.externalProjectId
            )
        }
    }

    private fun withHexPrefix(sender: ContractTransactionSender, value: String): String {
        val method = ContractTransactionSender::class.java.getDeclaredMethod("withHexPrefix", String::class.java)
        method.isAccessible = true
        return method.invoke(sender, value) as String
    }

    private data class InvalidConfig(
        val label: String,
        val properties: EpChainProperties,
        val expectedMessage: String
    )

    companion object {
        private const val CONTRACT_ADDRESS = "0x0000000000000000000000000000000000000001"
        private const val PRIVATE_KEY = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
        private const val WALLET = "0x628d684197485c054cda7d3def46e8be6b3d174c"
    }
}
