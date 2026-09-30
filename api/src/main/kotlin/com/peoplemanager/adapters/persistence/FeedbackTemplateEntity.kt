package com.peoplemanager.adapters.persistence

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "feedback_templates")
class FeedbackTemplateEntity(
    @Id
    @Column(name = "id")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    val userId: UUID = UUID.randomUUID(),

    @Column(name = "title", nullable = false, length = 200)
    val title: String = "",

    @Column(name = "description", columnDefinition = "TEXT")
    val description: String? = null,

    /** JSON-serialized list of questions. */
    @Column(name = "questions", nullable = false, columnDefinition = "TEXT")
    val questions: String = "[]",

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now()
)
