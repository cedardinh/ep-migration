package io.cryptoblk.migration.repository

import io.cryptoblk.migration.dto.entity.ProjectParticipant
import org.springframework.data.jpa.repository.JpaRepository

interface ProjectParticipantRepository : JpaRepository<ProjectParticipant, Long>
