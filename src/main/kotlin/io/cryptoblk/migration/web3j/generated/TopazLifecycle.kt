package io.cryptoblk.migration.web3j.generated

import java.math.BigInteger

class TopazLifecycle {
    data class ProjectSummary(
        val externalProjectId: String,
        val name: String,
        val status: BigInteger,
        val developer: TopazTypes.Participant,
        val mainContractors: List<TopazTypes.Participant>,
        val claimApprovers: List<TopazTypes.ApproverConfig>,
        val paymentApprovers: List<TopazTypes.ApproverConfig>,
        val bankAccountRefs: List<String>,
        val createdAt: BigInteger,
        val updatedAt: BigInteger,
        val claimCount: BigInteger
    )
}
