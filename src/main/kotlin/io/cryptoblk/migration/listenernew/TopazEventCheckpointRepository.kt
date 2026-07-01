package io.cryptoblk.migration.listenernew

import org.springframework.beans.factory.InitializingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.math.BigInteger
import java.util.Optional

interface TopazEventCheckpointRepository {
    fun findByIdentity(identity: TopazEventCheckpointIdentity): Optional<TopazEventCheckpointEntity>

    fun save(entity: TopazEventCheckpointEntity): TopazEventCheckpointEntity
}

@Repository
@ConditionalOnProperty(prefix = "ep.chain", name = ["listener-enabled"], havingValue = "true", matchIfMissing = true)
class JdbcTopazEventCheckpointRepository(
    private val jdbcTemplate: JdbcTemplate
) : TopazEventCheckpointRepository, InitializingBean {

    override fun afterPropertiesSet() {
        jdbcTemplate.execute(
            """
            create table if not exists topaz_event_checkpoint (
                listener_name varchar(128) not null,
                chain_id numeric(78, 0),
                lifecycle_contract_address varchar(42),
                payment_contract_address varchar(42),
                contacts_contract_address varchar(42),
                processed_block numeric(78, 0) not null,
                processed_tx_hash varchar(66),
                processed_log_index numeric(78, 0)
            )
            """.trimIndent()
        )
        jdbcTemplate.execute("alter table topaz_event_checkpoint add column if not exists chain_id numeric(78, 0)")
        jdbcTemplate.execute("alter table topaz_event_checkpoint add column if not exists lifecycle_contract_address varchar(42)")
        jdbcTemplate.execute("alter table topaz_event_checkpoint add column if not exists payment_contract_address varchar(42)")
        jdbcTemplate.execute("alter table topaz_event_checkpoint add column if not exists contacts_contract_address varchar(42)")
        jdbcTemplate.execute("alter table topaz_event_checkpoint add column if not exists processed_tx_hash varchar(66)")
        jdbcTemplate.execute("alter table topaz_event_checkpoint add column if not exists processed_log_index numeric(78, 0)")
        dropExistingPrimaryKey()
        jdbcTemplate.execute(
            """
            create unique index if not exists topaz_event_checkpoint_identity_uidx
            on topaz_event_checkpoint (
                listener_name,
                chain_id,
                lifecycle_contract_address,
                payment_contract_address,
                contacts_contract_address
            )
            where chain_id is not null
              and lifecycle_contract_address is not null
              and payment_contract_address is not null
              and contacts_contract_address is not null
            """.trimIndent()
        )
    }

    override fun findByIdentity(identity: TopazEventCheckpointIdentity): Optional<TopazEventCheckpointEntity> {
        val rows = jdbcTemplate.query(
            """
            select listener_name,
                   chain_id,
                   lifecycle_contract_address,
                   payment_contract_address,
                   contacts_contract_address,
                   processed_block,
                   processed_tx_hash,
                   processed_log_index
            from topaz_event_checkpoint
            where listener_name = ?
              and chain_id = ?
              and lifecycle_contract_address = ?
              and payment_contract_address = ?
              and contacts_contract_address = ?
            """.trimIndent(),
            arrayOf(
                identity.listenerName,
                BigDecimal.valueOf(identity.chainId),
                identity.lifecycleContractAddress,
                identity.paymentContractAddress,
                identity.contactsContractAddress
            )
        ) { rs, _ ->
            val processedLogIndex = rs.getBigDecimal("processed_log_index")
            TopazEventCheckpointEntity(
                listenerName = rs.getString("listener_name"),
                processedBlock = BigInteger(rs.getBigDecimal("processed_block").toPlainString()),
                processedTransactionHash = rs.getString("processed_tx_hash"),
                processedLogIndex = processedLogIndex?.toPlainString()?.let(::BigInteger),
                chainId = BigInteger(rs.getBigDecimal("chain_id").toPlainString()).toLong(),
                lifecycleContractAddress = rs.getString("lifecycle_contract_address"),
                paymentContractAddress = rs.getString("payment_contract_address"),
                contactsContractAddress = rs.getString("contacts_contract_address")
            )
        }
        return rows.firstOrNull()?.let { Optional.of(it) } ?: Optional.empty()
    }

    override fun save(entity: TopazEventCheckpointEntity): TopazEventCheckpointEntity {
        require(entity.lifecycleContractAddress.isNotBlank()) {
            "lifecycleContractAddress must be set before saving Topaz event checkpoint"
        }
        require(entity.paymentContractAddress.isNotBlank()) {
            "paymentContractAddress must be set before saving Topaz event checkpoint"
        }
        require(entity.contactsContractAddress.isNotBlank()) {
            "contactsContractAddress must be set before saving Topaz event checkpoint"
        }
        val updated = jdbcTemplate.update(
            """
            update topaz_event_checkpoint
            set processed_block = ?, processed_tx_hash = ?, processed_log_index = ?
            where listener_name = ?
              and chain_id = ?
              and lifecycle_contract_address = ?
              and payment_contract_address = ?
              and contacts_contract_address = ?
            """.trimIndent(),
            BigDecimal(entity.processedBlock),
            entity.processedTransactionHash,
            entity.processedLogIndex?.let(::BigDecimal),
            entity.listenerName,
            BigDecimal.valueOf(entity.chainId),
            entity.lifecycleContractAddress,
            entity.paymentContractAddress,
            entity.contactsContractAddress
        )
        if (updated == 0) {
            insert(entity)
        }
        return entity
    }

    private fun insert(entity: TopazEventCheckpointEntity) {
        try {
            jdbcTemplate.update(
                """
                insert into topaz_event_checkpoint
                    (
                        listener_name,
                        chain_id,
                        lifecycle_contract_address,
                        payment_contract_address,
                        contacts_contract_address,
                        processed_block,
                        processed_tx_hash,
                        processed_log_index
                    )
                values (?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                entity.listenerName,
                BigDecimal.valueOf(entity.chainId),
                entity.lifecycleContractAddress,
                entity.paymentContractAddress,
                entity.contactsContractAddress,
                BigDecimal(entity.processedBlock),
                entity.processedTransactionHash,
                entity.processedLogIndex?.let(::BigDecimal)
            )
        } catch (ex: DuplicateKeyException) {
            jdbcTemplate.update(
                """
                update topaz_event_checkpoint
                set processed_block = ?, processed_tx_hash = ?, processed_log_index = ?
                where listener_name = ?
                  and chain_id = ?
                  and lifecycle_contract_address = ?
                  and payment_contract_address = ?
                  and contacts_contract_address = ?
                """.trimIndent(),
                BigDecimal(entity.processedBlock),
                entity.processedTransactionHash,
                entity.processedLogIndex?.let(::BigDecimal),
                entity.listenerName,
                BigDecimal.valueOf(entity.chainId),
                entity.lifecycleContractAddress,
                entity.paymentContractAddress,
                entity.contactsContractAddress
            )
        }
    }

    private fun dropExistingPrimaryKey() {
        val primaryKeys = jdbcTemplate.queryForList(
            """
            select constraint_name
            from information_schema.table_constraints
            where table_schema = current_schema()
              and table_name = 'topaz_event_checkpoint'
              and constraint_type = 'PRIMARY KEY'
            """.trimIndent(),
            String::class.java
        )
        primaryKeys.forEach { constraint ->
            jdbcTemplate.execute(
                "alter table topaz_event_checkpoint drop constraint if exists ${quoteIdentifier(constraint)}"
            )
        }
    }

    private fun quoteIdentifier(identifier: String): String {
        return "\"${identifier.replace("\"", "\"\"")}\""
    }
}
