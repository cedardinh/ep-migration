package io.cryptoblk.migration.dto.entity

import java.time.OffsetDateTime
import javax.persistence.*

@Entity
@Table(name = "project_approver")
class ProjectApprover(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    var project: ProjectEntity,

    @Column(name = "approver_type", nullable = false)
    var approverType: String,

    @Column(name = "wallet", nullable = false)
    var wallet: String,

    @Column(name = "user_hash", nullable = false)
    var userHash: ByteArray,

    @Column(name = "role_name", nullable = false)
    var roleName: String,

    @Column(name = "external_ref", nullable = false)
    var externalRef: String = "",

    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime? = null
)
