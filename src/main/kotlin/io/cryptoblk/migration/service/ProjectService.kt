package io.cryptoblk.migration.service

import io.cryptoblk.migration.converter.ProjectSummaryConverter
import io.cryptoblk.migration.converter.TopazProjectSummary
import io.cryptoblk.migration.dto.entity.ProjectEntity
import io.cryptoblk.migration.repository.ProjectApproverRepository
import io.cryptoblk.migration.repository.ProjectParticipantRepository
import io.cryptoblk.migration.repository.ProjectRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProjectService(
    private val projectRepository: ProjectRepository,
    private val projectParticipantRepository: ProjectParticipantRepository,
    private val projectApproverRepository: ProjectApproverRepository,
    private val projectSummaryConverter: ProjectSummaryConverter
) {

    @Transactional
    fun addProjectSummaryDetail(summary: TopazProjectSummary): ProjectEntity {
        val project = projectRepository.save(projectSummaryConverter.toProjectEntity(summary))
        projectParticipantRepository.saveAll(projectSummaryConverter.toParticipants(summary, project))
        projectApproverRepository.saveAll(projectSummaryConverter.toApprovers(summary, project))
        return project
    }
}
