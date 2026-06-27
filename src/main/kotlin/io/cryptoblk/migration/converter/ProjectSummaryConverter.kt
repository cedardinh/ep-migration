package io.cryptoblk.migration.converter

import io.cryptoblk.migration.dto.entity.ProjectApprover
import io.cryptoblk.migration.dto.entity.ProjectEntity
import io.cryptoblk.migration.dto.entity.ProjectParticipant
import io.cryptoblk.migration.web3j.generated.TopazLifecycle
import io.cryptoblk.migration.web3j.generated.TopazTypes
import org.springframework.stereotype.Component
import java.math.BigInteger
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Component
class ProjectSummaryConverter {

    fun toProjectEntity(summary: TopazLifecycle.ProjectSummary): ProjectEntity = ProjectEntity(
        externalProjectId = summary.externalProjectId,
        name = summary.name,
        bankAccountRefs = summary.bankAccountRefs.toTypedArray(),
        createdAt = toUtcOffsetDateTime(summary.createdAt),
        updatedAt = toUtcOffsetDateTime(summary.updatedAt)
    )

    fun toParticipants(summary: TopazLifecycle.ProjectSummary, project: ProjectEntity): List<ProjectParticipant> {
        val createdAt = toUtcOffsetDateTime(summary.createdAt)
        val participants = mutableListOf<ProjectParticipant>()
        participants += toParticipant(summary.developer, project, PARTICIPANT_DEVELOPER, createdAt)
        summary.mainContractors.forEach {
            participants += toParticipant(it, project, PARTICIPANT_MAIN_CONTRACTOR, createdAt)
        }
        return participants
    }

    fun toApprovers(summary: TopazLifecycle.ProjectSummary, project: ProjectEntity): List<ProjectApprover> {
        val createdAt = toUtcOffsetDateTime(summary.createdAt)
        val approvers = mutableListOf<ProjectApprover>()
        summary.claimApprovers.forEach {
            approvers += toApprover(it, project, APPROVER_CLAIM, createdAt)
        }
        summary.paymentApprovers.forEach {
            approvers += toApprover(it, project, APPROVER_PAYMENT, createdAt)
        }
        return approvers
    }

    private fun toParticipant(
        source: TopazTypes.Participant,
        project: ProjectEntity,
        participantType: String,
        createdAt: OffsetDateTime
    ): ProjectParticipant = ProjectParticipant(
        project = project,
        participantType = participantType,
        wallet = source.wallet,
        legalName = source.legalName,
        addressLine1 = source.addressLine1,
        addressLine2 = source.addressLine2,
        bic = source.bic,
        lei = source.lei,
        externalRef = source.externalRef,
        createdAt = createdAt
    )

    private fun toApprover(
        source: TopazTypes.ApproverConfig,
        project: ProjectEntity,
        approverType: String,
        createdAt: OffsetDateTime
    ): ProjectApprover = ProjectApprover(
        project = project,
        approverType = approverType,
        wallet = source.wallet,
        userHash = source.userHash,
        roleName = source.roleName,
        externalRef = source.externalRef,
        createdAt = createdAt
    )

    private fun toUtcOffsetDateTime(epochSeconds: BigInteger): OffsetDateTime =
        OffsetDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds.longValueExact()), ZoneOffset.UTC)

    companion object {
        private const val PARTICIPANT_DEVELOPER = "DEVELOPER"
        private const val PARTICIPANT_MAIN_CONTRACTOR = "MAIN_CONTRACTOR"
        private const val APPROVER_CLAIM = "CLAIM_APPROVER"
        private const val APPROVER_PAYMENT = "PAYMENT_APPROVER"
    }
}
