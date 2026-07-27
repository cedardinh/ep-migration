package com.demo.server.epmigration.nonce_management.persistence

import org.flywaydb.core.Flyway
import javax.sql.DataSource

internal class NonceSchemaMigrator(
    private val dataSource: DataSource
) {
    fun migrate() {
        Flyway.configure()
            .dataSource(dataSource)
            .schemas(SCHEMA)
            .defaultSchema(SCHEMA)
            .table("schema_history")
            .locations("classpath:db/nonce-management")
            .load()
            .migrate()
    }

    companion object {
        const val SCHEMA = "ep_nonce_management"
    }
}
