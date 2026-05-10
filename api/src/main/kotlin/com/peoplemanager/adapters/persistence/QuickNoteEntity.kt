package com.peoplemanager.adapters.persistence

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "quick_notes")
class QuickNoteEntity(
    @Id
    @Column(name = "id")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    val userId: UUID = UUID.randomUUID(),

    @Column(name = "person_id")
    val personId: UUID? = null,

    @Column(name = "text", nullable = false, columnDefinition = "TEXT")
    val text: String = "",

    @Column(name = "sensitive", nullable = false)
    val sensitive: Boolean = false,

    @Column(name = "status", nullable = false)
    val status: String = "INBOX",

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now()
)
