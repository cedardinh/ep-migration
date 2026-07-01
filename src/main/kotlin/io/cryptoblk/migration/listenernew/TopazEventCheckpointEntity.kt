package io.cryptoblk.migration.listenernew

import java.math.BigInteger
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

data class TopazEventCheckpointIdentity(
    /** Listener identifier：listener_name identifies an independent event-listener cursor so multiple listeners can
    use the same checkpoint table without overwriting each other's progress.. */
    val listenerName: String,

    /** Chain id for the current RPC environment. */
    val chainId: Long,

    /** Lifecycle contract address for this deployment. */
    val lifecycleContractAddress: String,

    /** Payment contract address for this deployment. */
    val paymentContractAddress: String,

    /** Contacts contract address for this deployment. */
    val contactsContractAddress: String
)

@Entity
@Table(name = "topaz_event_checkpoint")
/** Persistent listener checkpoint. */
class TopazEventCheckpointEntity(
    /** Listener identifier. */
    @Id
    @Column(name = "listener_name", nullable = false)
    var listenerName: String = "",

    /** Last processed block. */
    @Column(name = "processed_block", nullable = false)
    var processedBlock: BigInteger = BigInteger.ZERO,

    /** Last processed transaction. */
    @Column(name = "processed_tx_hash")
    var processedTransactionHash: String? = null,

    /** Last processed log index. */
    @Column(name = "processed_log_index")
    var processedLogIndex: BigInteger? = null,

    /** Chain id for the deployment that owns this checkpoint. */
    @Column(name = "chain_id")
    var chainId: Long = 0L,

    /** Lifecycle contract address for the deployment that owns this checkpoint. */
    @Column(name = "lifecycle_contract_address")
    var lifecycleContractAddress: String = "",

    /** Payment contract address for the deployment that owns this checkpoint. */
    @Column(name = "payment_contract_address")
    var paymentContractAddress: String = "",

    /** Contacts contract address for the deployment that owns this checkpoint. */
    @Column(name = "contacts_contract_address")
    var contactsContractAddress: String = ""
) {
    fun identity(): TopazEventCheckpointIdentity {
        return TopazEventCheckpointIdentity(
            listenerName = listenerName,
            chainId = chainId,
            lifecycleContractAddress = lifecycleContractAddress,
            paymentContractAddress = paymentContractAddress,
            contactsContractAddress = contactsContractAddress
        )
    }
}
