package io.cryptoblk.migration.listenernew

import org.springframework.data.jpa.repository.JpaRepository

interface TopazEventCheckpointRepository : JpaRepository<TopazEventCheckpointEntity, String>
