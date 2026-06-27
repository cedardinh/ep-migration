package io.cryptoblk.migration.repository

import io.cryptoblk.migration.dto.entity.ProjectEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ProjectRepository : JpaRepository<ProjectEntity, Long>
