package io.cryptoblk.migration.listenernew

import java.math.BigInteger
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "topaz_event_checkpoint")
class TopazEventCheckpointEntity(
    @Id
    @Column(name = "listener_name", nullable = false)
    var listenerName: String = "",

    @Column(name = "processed_block", nullable = false)
    var processedBlock: BigInteger = BigInteger.ZERO
)
