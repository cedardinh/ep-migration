package com.demo.server.epmigration.nonce_management.configuration

import com.demo.server.epmigration.config.EpChainProperties
import com.demo.server.epmigration.nonce_management.allocation.ChainNonceReader
import com.demo.server.epmigration.nonce_management.allocation.NonceAllocator
import com.demo.server.epmigration.nonce_management.allocation.NonceCursorStore
import com.demo.server.epmigration.nonce_management.persistence.NonceSchemaMigrator
import com.demo.server.epmigration.nonce_management.persistence.PostgresNonceCursorStore
import com.demo.server.epmigration.nonce_management.web3j.ManagedNonceTransactionManager
import com.demo.server.epmigration.nonce_management.web3j.Web3jChainNonceReader
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import org.springframework.context.annotation.Primary
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.tx.TransactionManager
import javax.sql.DataSource

@Configuration
internal class NonceManagementConfiguration {
    @Bean(initMethod = "migrate")
    fun nonceSchemaMigrator(
        dataSource: DataSource
    ): NonceSchemaMigrator {
        return NonceSchemaMigrator(dataSource)
    }

    @Bean
    @DependsOn("nonceSchemaMigrator")
    fun nonceCursorStore(dataSource: DataSource): NonceCursorStore {
        return PostgresNonceCursorStore(dataSource)
    }

    @Bean
    fun chainNonceReader(
        web3j: Web3j,
        credentials: Credentials,
        chainProperties: EpChainProperties
    ): ChainNonceReader {
        return Web3jChainNonceReader(
            web3j,
            credentials,
            chainProperties
        )
    }

    @Bean
    fun nonceAllocator(
        chain: ChainNonceReader,
        cursorStore: NonceCursorStore
    ): NonceAllocator {
        return NonceAllocator(chain, cursorStore)
    }

    @Bean(MANAGED_TRANSACTION_MANAGER)
    @Primary
    fun managedTransactionManager(
        web3j: Web3j,
        credentials: Credentials,
        chainProperties: EpChainProperties,
        nonceAllocator: NonceAllocator
    ): TransactionManager {
        return ManagedNonceTransactionManager(
            web3j,
            credentials,
            chainProperties.chainId,
            nonceAllocator
        )
    }

    companion object {
        const val MANAGED_TRANSACTION_MANAGER =
            "managedTransactionManager"
    }
}
