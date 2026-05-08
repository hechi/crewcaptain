package com.peoplemanager.adapters.persistence

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "persons")
class PersonEntity(
    @Id
    @Column(name = "id")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    val userId: UUID = UUID.randomUUID(),

    @Column(name = "name", nullable = false)
    var name: String = "",

    @Column(name = "preferred_name")
    var preferredName: String? = null,

    @Column(name = "role_title")
    var roleTitle: String? = null,

    @Column(name = "timezone")
    var timezone: String? = null,

    @Column(name = "start_date")
    var startDate: LocalDate? = null,

    @Column(name = "email")
    var email: String? = null,

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "tags", columnDefinition = "text[]")
    var tags: Array<String> = emptyArray(),

    @Column(name = "morale_status", nullable = false)
    var moraleStatus: String = "UNKNOWN",

    @Column(name = "morale_note")
    var moraleNote: String? = null,

    @OneToMany(mappedBy = "person", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("displayOrder ASC")
    var pinnedRememberItems: MutableList<PinnedRememberItemEntity> = mutableListOf(),

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)
