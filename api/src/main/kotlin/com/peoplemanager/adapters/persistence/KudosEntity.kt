package com.peoplemanager.adapters.persistence

import jakarta.persistence.*
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "kudos")
class KudosEntity(
    @Id
    @Column(name = "id")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    val userId: UUID = UUID.randomUUID(),

    @Column(name = "person_id", nullable = false)
    val personId: UUID = UUID.randomUUID(),

    @Column(name = "date", nullable = false)
    val date: LocalDate = LocalDate.now(),

    @Column(name = "text", nullable = false, columnDefinition = "TEXT")
    val text: String = "",

    @Column(name = "tags", columnDefinition = "TEXT[]")
    val tags: Array<String> = emptyArray(),

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now()
)
