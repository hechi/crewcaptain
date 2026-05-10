package com.peoplemanager.adapters.persistence

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "audit_log")
class AuditLogEntryEntity(
    @Id
    val id: UUID,

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "action", nullable = false, length = 20)
    val action: String,

    @Column(name = "entity_type", nullable = false, length = 50)
    val entityType: String,

    @Column(name = "entity_id", nullable = false, length = 255)
    val entityId: String,

    @Column(name = "person_id")
    val personId: UUID? = null,

    @Column(name = "summary", nullable = false, length = 500)
    val summary: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)
