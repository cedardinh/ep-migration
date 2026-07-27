package com.demo.server.epmigration.nonce_management

import com.demo.server.epmigration.chain.generated.TopazLifecycle
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.web3j.utils.Numeric
import java.math.BigInteger

@RestController
@RequestMapping("/api/nonce-management/projects")
class CreateProjectController(
    private val lifecycleClient: TopazLifecycleClient
) {
    @PostMapping
    fun createProject(
        @RequestBody request: CreateProjectRequest
    ): CreateProjectResponse {
        return lifecycleClient.createProject(request)
    }
}

data class CreateProjectRequest(
    val externalProjectId: String = "",
    val name: String = "",
    val developer: ParticipantRequest = ParticipantRequest(),
    val mainContractors: List<ParticipantRequest> = emptyList(),
    val claimApprovers: List<ApproverRequest> = emptyList(),
    val paymentApprovers: List<ApproverRequest> = emptyList(),
    val bankAccountRefs: List<String> = emptyList()
) {
    fun toContractInput(): TopazLifecycle.CreateProjectInput {
        return TopazLifecycle.CreateProjectInput(
            externalProjectId,
            name,
            developer.toContractType(),
            mainContractors.map { it.toContractType() },
            claimApprovers.map { it.toContractType() },
            paymentApprovers.map { it.toContractType() },
            bankAccountRefs
        )
    }
}

data class ParticipantRequest(
    val wallet: String = "",
    val legalName: String = "",
    val addressLine1: String = "",
    val addressLine2: String = "",
    val bic: String = "",
    val lei: String = "",
    val externalRef: String = ""
) {
    fun toContractType(): TopazLifecycle.Participant {
        return TopazLifecycle.Participant(
            wallet,
            legalName,
            addressLine1,
            addressLine2,
            bic,
            lei,
            externalRef
        )
    }
}

data class ApproverRequest(
    val wallet: String = "",
    val userHash: String = "",
    val email: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val userProfileName: String = "",
    val roleName: String = "",
    val externalRef: String = ""
) {
    fun toContractType(): TopazLifecycle.ApproverConfig {
        val userHashBytes = Numeric.hexStringToByteArray(userHash)
        require(userHashBytes.size == 32) {
            "userHash must be a 32-byte hex value"
        }
        return TopazLifecycle.ApproverConfig(
            wallet,
            userHashBytes,
            email,
            firstName,
            lastName,
            userProfileName,
            roleName,
            externalRef
        )
    }
}

data class CreateProjectResponse(
    val projectId: BigInteger,
    val transactionHash: String,
    val blockNumber: BigInteger,
    val gasUsed: BigInteger
)
