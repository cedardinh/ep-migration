package com.demo.server.epmigration.chain.contract

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.demo.server.epmigration.config.EpChainProperties
import com.demo.server.epmigration.project.dto.ApproverRequest
import com.demo.server.epmigration.project.dto.CreateProjectRequest
import com.demo.server.epmigration.project.dto.ParticipantRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class TopazLifecycleContractTests {
    private val mapper = jacksonObjectMapper()
    private val lifecycle = TopazLifecycleContract(
        EpChainProperties().apply {
            lifecycleContractAddress = "0x0000000000000000000000000000000000000001"
        }
    )

    @Test
    fun `createProject returns contract call metadata`() {
        val call = lifecycle.createProject(sampleRequest())

        assertEquals("createProject", call.functionName)
        assertEquals("0x0000000000000000000000000000000000000001", call.to)
    }

    @Test
    fun `encode createProject matches ethers calldata`() {
        val request = sampleRequest()

        assertEquals(
            ethersEncode(sampleRequestJson()),
            lifecycle.createProject(request).data
        )
    }

    @Test
    fun `encode createProject supports empty claim approvers`() {
        val request = sampleRequest(claimApprovers = emptyList())

        assertEquals(
            ethersEncode(sampleRequestJson(claimApproversJson = "[]")),
            lifecycle.createProject(request).data
        )
    }

    @Test
    fun `controller json request is already a contract input`() {
        val request = mapper.readValue<CreateProjectRequest>(sampleRequestJson())

        assertEquals(
            ethersEncode(sampleRequestJson()),
            lifecycle.createProject(request).data
        )
    }

    private fun sampleRequest(
        claimApprovers: List<ApproverRequest> = listOf(
            ApproverRequest(
                wallet = WALLET,
                userHash = "0x61533c4c2e198353cde1c7df7a23852535a93a5d1f2ee39863bb3cf118855a53",
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
                    roleName = "1",
                    externalRef = "Approver Entity"
                )
            ),
            bankAccountRefs = listOf("dev-bank")
        )
    }

    private fun sampleRequestJson(
        claimApproversJson: String =
            """[{"wallet":"$WALLET","userHash":"0x61533c4c2e198353cde1c7df7a23852535a93a5d1f2ee39863bb3cf118855a53","roleName":"1","externalRef":"Approver Entity"}]"""
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

    companion object {
        private const val WALLET = "0x628d684197485c054cda7d3def46e8be6b3d174c"
    }
}
