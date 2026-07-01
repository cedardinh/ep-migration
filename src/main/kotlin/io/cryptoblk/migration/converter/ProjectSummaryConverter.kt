package io.cryptoblk.migration.converter

import com.demo.server.epmigration.chain.generated.TopazLifecycle
import io.cryptoblk.migration.dto.entity.ProjectApprover
import io.cryptoblk.migration.dto.entity.ProjectEntity
import io.cryptoblk.migration.dto.entity.ProjectParticipant
import org.springframework.stereotype.Component
import org.web3j.tuples.generated.Tuple11
import java.math.BigInteger
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

typealias TopazProjectSummary = Tuple11<
    String,
    String,
    BigInteger,
    TopazLifecycle.Participant,
    List<TopazLifecycle.Participant>,
    List<TopazLifecycle.ApproverConfig>,
    List<TopazLifecycle.ApproverConfig>,
    List<String>,
    BigInteger,
    BigInteger,
    BigInteger
>

@Component
class ProjectSummaryConverter {

    fun toProjectEntity(summary: TopazProjectSummary): ProjectEntity = ProjectEntity(
        externalProjectId = summary.value1,
        name = summary.value2,
        bankAccountRefs = summary.value8.toTypedArray(),
        createdAt = toUtcOffsetDateTime(summary.value9),
        updatedAt = toUtcOffsetDateTime(summary.value10)
    )

    fun toParticipants(summary: TopazProjectSummary, project: ProjectEntity): List<ProjectParticipant> {
        val createdAt = toUtcOffsetDateTime(summary.value9)
        val participants = mutableListOf<ProjectParticipant>()
        participants += toParticipant(summary.value4, project, PARTICIPANT_DEVELOPER, createdAt)
        summary.value5.forEach {
            participants += toParticipant(it, project, PARTICIPANT_MAIN_CONTRACTOR, createdAt)
        }
        return participants
    }

    fun toApprovers(summary: TopazProjectSummary, project: ProjectEntity): List<ProjectApprover> {
        val createdAt = toUtcOffsetDateTime(summary.value9)
        val approvers = mutableListOf<ProjectApprover>()
        summary.value6.forEach {
            approvers += toApprover(it, project, APPROVER_CLAIM, createdAt)
        }
        summary.value7.forEach {
            approvers += toApprover(it, project, APPROVER_PAYMENT, createdAt)
        }
        return approvers
    }

    private fun toParticipant(
        source: TopazLifecycle.Participant,
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
        source: TopazLifecycle.ApproverConfig,
        project: ProjectEntity,
        approverType: String,
        createdAt: OffsetDateTime
    ): ProjectApprover = ProjectApprover(
        project = project,
        approverType = approverType,
        wallet = source.wallet,
        userHash = source.userHash,
        email = source.email,
        firstName = source.firstName,
        lastName = source.lastName,
        userProfileName = source.userProfileName,
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
