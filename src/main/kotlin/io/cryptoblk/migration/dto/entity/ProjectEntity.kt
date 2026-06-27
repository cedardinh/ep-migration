package io.cryptoblk.migration.dto.entity

import org.hibernate.annotations.Type
import java.time.OffsetDateTime
import javax.persistence.*

@Entity
@Table(name = "project")
class ProjectEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "external_project_id", nullable = false, unique = true)
    var externalProjectId: String,

    @Column(name = "name", nullable = false)
    var name: String,

    @Type(type = "com.vladmihalcea.hibernate.type.array.StringArrayType")
    @Column(name = "bank_account_refs", columnDefinition = "text[]")
    var bankAccountRefs: Array<String>? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime? = null,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime? = null
)
