package com.peoplemanager.adapters.persistence

import jakarta.persistence.*
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "feedback_summaries")
class FeedbackSummaryEntity(
    @Id
    @Column(name = "id")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    val userId: UUID = UUID.randomUUID(),

    @Column(name = "person_id", nullable = false)
    val personId: UUID = UUID.randomUUID(),

    @Column(name = "period_from", nullable = false)
    val periodFrom: LocalDate = LocalDate.now(),

    @Column(name = "period_to", nullable = false)
    val periodTo: LocalDate = LocalDate.now(),

    /** Encrypted at rest. */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    val content: String = "",

    @Column(name = "response_count", nullable = false)
    val responseCount: Int = 0,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now()
)
