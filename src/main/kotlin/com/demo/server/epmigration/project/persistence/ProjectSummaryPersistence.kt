package com.demo.server.epmigration.project.persistence

import com.demo.server.epmigration.chain.generated.TopazLifecycle
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.web3j.tuples.generated.Tuple11
import java.math.BigInteger
import java.time.OffsetDateTime
import java.time.ZoneOffset

interface ProjectSummaryPersistence {
    fun save(
        projectId: BigInteger,
        summary: Tuple11<
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
    )
}

object NoopProjectSummaryPersistence : ProjectSummaryPersistence {
    override fun save(
        projectId: BigInteger,
        summary: Tuple11<
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
    ) {
    }
}

@Service
class MyBatisProjectSummaryPersistence(
    private val mapper: ProjectSummaryMapper
) : ProjectSummaryPersistence {

    @Transactional
    override fun save(
        projectId: BigInteger,
        summary: Tuple11<
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
    ) {
        mapper.createProjectTable()
        mapper.ensureProjectSummaryColumns()
        mapper.createProjectParticipantTable()
        mapper.createProjectApproverTable()

        val projectIdLong = projectId.longValueExact()
        val createdAt = epochSecond(summary.value9)
        val updatedAt = epochSecond(summary.value10)

        mapper.upsertProject(
            PersistedProject(
                id = projectIdLong,
                externalProjectId = summary.value1,
                name = summary.value2,
                status = summary.value3.intValueExact(),
                bankAccountRefs = summary.value8,
                createdAt = createdAt,
                updatedAt = updatedAt,
                claimCount = summary.value11.longValueExact()
            )
        )

        mapper.deleteParticipantsByProjectId(projectIdLong)
        mapper.insertParticipant(summary.value4.toPersistedParticipant(projectIdLong, "DEVELOPER", createdAt))
        summary.value5.forEach {
            mapper.insertParticipant(it.toPersistedParticipant(projectIdLong, "MAIN_CONTRACTOR", createdAt))
        }

        mapper.deleteApproversByProjectId(projectIdLong)
        summary.value6.forEach {
            mapper.insertApprover(it.toPersistedApprover(projectIdLong, "CLAIM_APPROVER", createdAt))
        }
        summary.value7.forEach {
            mapper.insertApprover(it.toPersistedApprover(projectIdLong, "PAYMENT_APPROVER", createdAt))
        }
    }

    private fun epochSecond(value: BigInteger): OffsetDateTime {
        return OffsetDateTime.ofInstant(java.time.Instant.ofEpochSecond(value.longValueExact()), ZoneOffset.UTC)
    }

    private fun TopazLifecycle.Participant.toPersistedParticipant(
        projectId: Long,
        participantType: String,
        createdAt: OffsetDateTime
    ): PersistedProjectParticipant {
        return PersistedProjectParticipant(
            projectId = projectId,
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
    }

    private fun TopazLifecycle.ApproverConfig.toPersistedApprover(
        projectId: Long,
        approverType: String,
        createdAt: OffsetDateTime
    ): PersistedProjectApprover {
        return PersistedProjectApprover(
            projectId = projectId,
            approverType = approverType,
            wallet = wallet,
            userHash = userHash,
            roleName = roleName,
            externalRef = externalRef,
            createdAt = createdAt
        )
    }
}
