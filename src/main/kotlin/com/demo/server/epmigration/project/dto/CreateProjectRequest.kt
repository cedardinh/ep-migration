package com.demo.server.epmigration.project.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.demo.server.epmigration.chain.generated.TopazLifecycle

@Suppress("UNCHECKED_CAST")
class CreateProjectRequest @JsonCreator constructor(
    @JsonProperty("externalProjectId") externalProjectId: String = "",
    @JsonProperty("name") name: String = "",
    @JsonProperty("developer") developer: ParticipantRequest = ParticipantRequest(),
    @JsonProperty("mainContractors") mainContractors: List<ParticipantRequest> = emptyList(),
    @JsonProperty("claimApprovers") claimApprovers: List<ApproverRequest> = emptyList(),
    @JsonProperty("paymentApprovers") paymentApprovers: List<ApproverRequest> = emptyList(),
    @JsonProperty("bankAccountRefs") bankAccountRefs: List<String> = emptyList()
) : TopazLifecycle.CreateProjectInput(
    externalProjectId,
    name,
    developer,
    mainContractors as List<TopazLifecycle.Participant>,
    claimApprovers as List<TopazLifecycle.ApproverConfig>,
    paymentApprovers as List<TopazLifecycle.ApproverConfig>,
    bankAccountRefs
) {
    val developerRequest: ParticipantRequest
        get() = developer as ParticipantRequest

    val mainContractorRequests: List<ParticipantRequest>
        get() = mainContractors as List<ParticipantRequest>
}
