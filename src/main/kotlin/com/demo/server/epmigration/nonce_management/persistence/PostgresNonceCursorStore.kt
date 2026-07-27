package com.demo.server.epmigration.nonce_management.persistence

import com.demo.server.epmigration.nonce_management.allocation.ChainNonceIdentity
import com.demo.server.epmigration.nonce_management.allocation.NonceCursorStore
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal
import java.math.BigInteger
import javax.sql.DataSource

internal class PostgresNonceCursorStore(
    dataSource: DataSource
) : NonceCursorStore {
    private val jdbc = JdbcTemplate(dataSource)
    private val transaction = TransactionTemplate(
        DataSourceTransactionManager(dataSource)
    ).apply {
        propagationBehavior =
            TransactionDefinition.PROPAGATION_REQUIRES_NEW
        isolationLevel =
            TransactionDefinition.ISOLATION_READ_COMMITTED
        timeout = TRANSACTION_TIMEOUT_SECONDS
    }

    override fun allocate(
        identity: ChainNonceIdentity,
        pendingNonce: BigInteger
    ): BigInteger {
        require(pendingNonce.signum() >= 0) {
            "pending nonce must not be negative"
        }

        return transaction.execute {
            bindChain(identity)
            jdbc.update(
                """
                    INSERT INTO $SCHEMA.nonce_cursor (
                        signer_address,
                        next_nonce
                    ) VALUES (?, ?)
                    ON CONFLICT (signer_address) DO NOTHING
                """.trimIndent(),
                identity.signerAddress,
                pendingNonce
            )
            val localNext = jdbc.queryForObject(
                """
                    SELECT next_nonce
                    FROM $SCHEMA.nonce_cursor
                    WHERE signer_address = ?
                    FOR UPDATE
                """.trimIndent(),
                arrayOf(identity.signerAddress),
                BigDecimal::class.java
            )!!.toBigIntegerExact()
            val allocated = maxOf(localNext, pendingNonce)
            jdbc.update(
                """
                    UPDATE $SCHEMA.nonce_cursor
                    SET next_nonce = ?,
                        updated_at = CURRENT_TIMESTAMP
                    WHERE signer_address = ?
                """.trimIndent(),
                allocated.add(BigInteger.ONE),
                identity.signerAddress
            )
            allocated
        } ?: error("nonce allocation transaction returned no result")
    }

    private fun bindChain(identity: ChainNonceIdentity) {
        jdbc.update(
            """
                INSERT INTO $SCHEMA.chain_identity (
                    identity_key,
                    chain_id,
                    genesis_hash
                ) VALUES (1, ?, ?)
                ON CONFLICT (identity_key) DO NOTHING
            """.trimIndent(),
            identity.chainId,
            identity.genesisHash
        )
        val stored = jdbc.queryForMap(
            """
                SELECT chain_id, genesis_hash
                FROM $SCHEMA.chain_identity
                WHERE identity_key = 1
                FOR UPDATE
            """.trimIndent()
        )
        val storedChainId =
            (stored["chain_id"] as BigDecimal).toBigIntegerExact()
        val storedGenesis = stored["genesis_hash"].toString().trim()
        check(
            storedChainId == identity.chainId &&
                storedGenesis == identity.genesisHash
        ) {
            "nonce database is bound to another chain"
        }
    }

    companion object {
        private const val SCHEMA = NonceSchemaMigrator.SCHEMA
        private const val TRANSACTION_TIMEOUT_SECONDS = 5
    }
}
