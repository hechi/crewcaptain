package com.peoplemanager.adapters.persistence

import jakarta.persistence.*
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "pdp_goals")
class PdpGoalEntity(
    @Id
    @Column(name = "id")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    val userId: UUID = UUID.randomUUID(),

    @Column(name = "person_id", nullable = false)
    val personId: UUID = UUID.randomUUID(),

    @Column(name = "title", nullable = false)
    var title: String = "",

    @Column(name = "description")
    var description: String? = null,

    @Column(name = "target_date")
    var targetDate: LocalDate? = null,

    @Column(name = "status", nullable = false)
    var status: String = "ACTIVE",

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)
