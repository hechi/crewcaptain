package com.peoplemanager.adapters.persistence

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "feedback_responses")
class FeedbackResponseEntity(
    @Id
    @Column(name = "id")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    val userId: UUID = UUID.randomUUID(),

    @Column(name = "person_id", nullable = false)
    val personId: UUID = UUID.randomUUID(),

    @Column(name = "link_id", nullable = false)
    val linkId: UUID = UUID.randomUUID(),

    @Column(name = "submitter_name", length = 200)
    val submitterName: String? = null,

    @Column(name = "submitter_email", length = 320)
    val submitterEmail: String? = null,

    @Column(name = "anonymous", nullable = false)
    val anonymous: Boolean = true,

    /** JSON list of answers; encrypted at rest (contains free-text). */
    @Column(name = "answers", nullable = false, columnDefinition = "TEXT")
    val answers: String = "[]",

    /** Encrypted at rest. */
    @Column(name = "additional_comments", columnDefinition = "TEXT")
    val additionalComments: String? = null,

    @Column(name = "status", nullable = false, length = 20)
    val status: String = "PENDING",

    @Column(name = "flagged", nullable = false)
    val flagged: Boolean = false,

    @Column(name = "pinned", nullable = false)
    val pinned: Boolean = false,

    @Column(name = "converted_to_type", length = 20)
    val convertedToType: String? = null,

    @Column(name = "converted_to_id", length = 255)
    val convertedToId: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now()
)
