package com.peoplemanager.adapters.persistence

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "feedback_links")
class FeedbackLinkEntity(
    @Id
    @Column(name = "id")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    val userId: UUID = UUID.randomUUID(),

    @Column(name = "person_id", nullable = false)
    val personId: UUID = UUID.randomUUID(),

    @Column(name = "token", nullable = false, length = 64)
    val token: String = "",

    @Column(name = "title", nullable = false, length = 200)
    val title: String = "",

    @Column(name = "description", columnDefinition = "TEXT")
    val description: String? = null,

    @Column(name = "questions", nullable = false, columnDefinition = "TEXT")
    val questions: String = "[]",

    @Column(name = "label", length = 200)
    val label: String? = null,

    @Column(name = "request_submitter_info", nullable = false)
    val requestSubmitterInfo: Boolean = true,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant = Instant.now(),

    @Column(name = "revoked_at")
    val revokedAt: Instant? = null,

    @Column(name = "source_template_id")
    val sourceTemplateId: UUID? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now()
)
