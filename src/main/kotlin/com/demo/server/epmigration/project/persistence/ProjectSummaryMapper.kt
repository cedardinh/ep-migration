package com.demo.server.epmigration.project.persistence

import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import java.time.OffsetDateTime

@Mapper
interface ProjectSummaryMapper {
    fun createProjectTable()

    fun ensureProjectSummaryColumns()

    fun createProjectParticipantTable()

    fun createProjectApproverTable()

    fun ensureProjectApproverColumns()

    fun upsertProject(project: PersistedProject)

    fun deleteParticipantsByProjectId(@Param("projectId") projectId: Long)

    fun insertParticipant(participant: PersistedProjectParticipant)

    fun deleteApproversByProjectId(@Param("projectId") projectId: Long)

    fun insertApprover(approver: PersistedProjectApprover)
}

data class PersistedProject(
    val id: Long,
    val externalProjectId: String,
    val name: String,
    val status: Int,
    val bankAccountRefs: List<String>,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
    val claimCount: Long
)

data class PersistedProjectParticipant(
    val projectId: Long,
    val participantType: String,
    val wallet: String,
    val legalName: String,
    val addressLine1: String,
    val addressLine2: String,
    val bic: String,
    val lei: String,
    val externalRef: String,
    val createdAt: OffsetDateTime
)

data class PersistedProjectApprover(
    val projectId: Long,
    val approverType: String,
    val wallet: String,
    val userHash: ByteArray,
    val email: String,
    val firstName: String,
    val lastName: String,
    val userProfileName: String,
    val roleName: String,
    val externalRef: String,
    val createdAt: OffsetDateTime
)
