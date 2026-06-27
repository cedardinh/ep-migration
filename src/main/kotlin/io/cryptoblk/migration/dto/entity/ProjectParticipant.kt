package io.cryptoblk.migration.dto.entity

import java.time.OffsetDateTime
import javax.persistence.*

@Entity
@Table(name = "project_participant")
class ProjectParticipant(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    var project: ProjectEntity,

    @Column(name = "participant_type", nullable = false)
    var participantType: String,

    @Column(name = "wallet", nullable = false)
    var wallet: String,

    @Column(name = "legal_name", nullable = false)
    var legalName: String,

    @Column(name = "address_line1", nullable = false)
    var addressLine1: String = "",

    @Column(name = "address_line2", nullable = false)
    var addressLine2: String = "",

    @Column(name = "bic", nullable = false)
    var bic: String = "",

    @Column(name = "lei", nullable = false)
    var lei: String = "",

    @Column(name = "external_ref", nullable = false)
    var externalRef: String = "",

    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime? = null
)
