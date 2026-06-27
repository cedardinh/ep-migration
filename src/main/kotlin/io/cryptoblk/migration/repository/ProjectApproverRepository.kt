package io.cryptoblk.migration.repository

import io.cryptoblk.migration.dto.entity.ProjectApprover
import org.springframework.data.jpa.repository.JpaRepository

interface ProjectApproverRepository : JpaRepository<ProjectApprover, Long>
