package com.peoplemanager.adapters.persistence

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "one_on_one_entries")
class OneOnOneEntryEntity(
    @Id
    @Column(name = "id")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    val userId: UUID = UUID.randomUUID(),

    @Column(name = "person_id", nullable = false)
    val personId: UUID = UUID.randomUUID(),

    @Column(name = "meeting_date", nullable = false)
    var meetingDate: Instant = Instant.now(),

    @Column(name = "notes_markdown")
    var notesMarkdown: String? = null,

    @Column(name = "outcomes_markdown")
    var outcomesMarkdown: String? = null,

    @Column(name = "sensitive", nullable = false)
    var sensitive: Boolean = false,

    @OneToMany(mappedBy = "entry", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("displayOrder ASC")
    var agendaItems: MutableList<AgendaItemEntity> = mutableListOf(),

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)
