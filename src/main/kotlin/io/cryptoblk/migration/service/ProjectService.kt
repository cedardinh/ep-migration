package io.cryptoblk.migration.service

import io.cryptoblk.migration.dto.entity.ProjectApprover
import io.cryptoblk.migration.dto.entity.ProjectEntity
import io.cryptoblk.migration.dto.entity.ProjectParticipant
import io.cryptoblk.migration.repository.ProjectApproverRepository
import io.cryptoblk.migration.repository.ProjectParticipantRepository
import io.cryptoblk.migration.repository.ProjectRepository
import io.cryptoblk.migration.web3j.generated.TopazLifecycle
import io.cryptoblk.migration.web3j.generated.TopazTypes
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigInteger
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Service
class ProjectService(
    private val projectRepository: ProjectRepository,
    private val projectParticipantRepository: ProjectParticipantRepository,
    private val projectApproverRepository: ProjectApproverRepository
) {

    @Transactional
    fun addProjectSummaryDetail(summary: TopazLifecycle.ProjectSummary): ProjectEntity {
        val createdAt = summary.createdAt.toUtcOffsetDateTime()
        val updatedAt = summary.updatedAt.toUtcOffsetDateTime()

        val project = projectRepository.save(
            ProjectEntity(
                externalProjectId = summary.externalProjectId,
                name = summary.name,
                bankAccountRefs = summary.bankAccountRefs.toTypedArray(),
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        )

        val participants = mutableListOf<ProjectParticipant>()
        participants.add(summary.developer.toEntity(project, PARTICIPANT_DEVELOPER, createdAt))
        summary.mainContractors.forEach { participants.add(it.toEntity(project, PARTICIPANT_MAIN_CONTRACTOR, createdAt)) }
        projectParticipantRepository.saveAll(participants)

        val approvers = mutableListOf<ProjectApprover>()
        summary.claimApprovers.forEach { approvers.add(it.toEntity(project, APPROVER_CLAIM, createdAt)) }
        summary.paymentApprovers.forEach { approvers.add(it.toEntity(project, APPROVER_PAYMENT, createdAt)) }
        projectApproverRepository.saveAll(approvers)

        return project
    }

    private fun TopazTypes.Participant.toEntity(
        project: ProjectEntity,
        participantType: String,
        createdAt: OffsetDateTime
    ) = ProjectParticipant(
        project = project,
        participantType = participantType,
        wallet = wallet,
        legalName = legalName,
        addressLine1 = addressLine1,
        addressLine2 = addressLine2,
        bic = bic,
        lei = lei,
        externalRef = externalRef,
        createdAt = createdAt
    )

    private fun TopazTypes.ApproverConfig.toEntity(
        project: ProjectEntity,
        approverType: String,
        createdAt: OffsetDateTime
    ) = ProjectApprover(
        project = project,
        approverType = approverType,
        wallet = wallet,
        userHash = userHash,
        roleName = roleName,
        externalRef = externalRef,
        createdAt = createdAt
    )

    companion object {
        private const val PARTICIPANT_DEVELOPER = "DEVELOPER"
        private const val PARTICIPANT_MAIN_CONTRACTOR = "MAIN_CONTRACTOR"
        private const val APPROVER_CLAIM = "CLAIM_APPROVER"
        private const val APPROVER_PAYMENT = "PAYMENT_APPROVER"
    }
}

internal fun BigInteger.toUtcOffsetDateTime(): OffsetDateTime =
    OffsetDateTime.ofInstant(Instant.ofEpochSecond(this.longValueExact()), ZoneOffset.UTC)
