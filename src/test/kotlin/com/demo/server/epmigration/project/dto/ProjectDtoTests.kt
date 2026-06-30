package com.demo.server.epmigration.project.dto

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.web3j.utils.Numeric

class ProjectDtoTests {
    @Test
    fun `participant request keeps valid wallet and maps invalid wallet to zero address`() {
        assertEquals(WALLET, ParticipantRequest(wallet = WALLET).wallet)
        assertEquals(ZERO_ADDRESS, ParticipantRequest(wallet = "bad").wallet)
    }

    @Test
    fun `approver request keeps valid wallet and user hash`() {
        val request = ApproverRequest(
            wallet = WALLET,
            userHash = USER_HASH,
            email = "approver@example.com",
            firstName = "Ada",
            lastName = "Lovelace",
            userProfileName = "ada.lovelace"
        )

        assertEquals(WALLET, request.wallet)
        assertArrayEquals(Numeric.hexStringToByteArray(USER_HASH), request.userHash)
        assertEquals("approver@example.com", request.email)
        assertEquals("Ada", request.firstName)
        assertEquals("Lovelace", request.lastName)
        assertEquals("ada.lovelace", request.userProfileName)
    }

    @Test
    fun `approver request maps invalid wallet or user hash to zero ABI values`() {
        val badWallet = ApproverRequest(wallet = "bad", userHash = USER_HASH)
        val badHash = ApproverRequest(wallet = WALLET, userHash = "0x1234")

        assertEquals(ZERO_ADDRESS, badWallet.wallet)
        assertArrayEquals(ByteArray(32), badHash.userHash)
    }

    @Test
    fun `participant request defaults map to zero address and empty fields`() {
        val request = ParticipantRequest()

        assertEquals(ZERO_ADDRESS, request.wallet)
        assertEquals("", request.legalName)
    }

    @Test
    fun `approver request defaults map to zero ABI values and empty fields`() {
        val request = ApproverRequest()

        assertEquals(ZERO_ADDRESS, request.wallet)
        assertArrayEquals(ByteArray(32), request.userHash)
        assertEquals("", request.email)
        assertEquals("", request.firstName)
        assertEquals("", request.lastName)
        assertEquals("", request.userProfileName)
        assertEquals("", request.roleName)
    }

    @Test
    fun `create project request defaults expose empty participant collections`() {
        val request = CreateProjectRequest()

        assertEquals("", request.externalProjectId)
        assertEquals("", request.name)
        assertEquals(ZERO_ADDRESS, request.developerRequest.wallet)
        assertEquals(emptyList<ParticipantRequest>(), request.mainContractorRequests)
        assertEquals(emptyList<ApproverRequest>(), request.claimApprovers)
        assertEquals(emptyList<ApproverRequest>(), request.paymentApprovers)
        assertEquals(emptyList<String>(), request.bankAccountRefs)
    }

    @Test
    fun `create project request exposes typed developer and contractor requests`() {
        val developer = ParticipantRequest(wallet = WALLET, legalName = "Dev")
        val contractor = ParticipantRequest(wallet = WALLET, legalName = "Con")
        val request = CreateProjectRequest(
            externalProjectId = "p-1",
            name = "Project",
            developer = developer,
            mainContractors = listOf(contractor)
        )

        assertEquals("Dev", request.developerRequest.legalName)
        assertEquals(listOf(contractor), request.mainContractorRequests)
    }

    companion object {
        private const val WALLET = "0x628d684197485c054cda7d3def46e8be6b3d174c"
        private const val ZERO_ADDRESS = "0x0000000000000000000000000000000000000000"
        private const val USER_HASH = "0x61533c4c2e198353cde1c7df7a23852535a93a5d1f2ee39863bb3cf118855a53"
    }
}
