package com.peoplemanager.domain

import java.time.Instant

data class OneOnOneEntry(
    val id: OneOnOneEntryId,
    val userId: UserId,
    val personId: PersonId,
    val meetingDate: Instant,
    val agendaItems: List<AgendaItem> = emptyList(),
    val notesMarkdown: String? = null,
    val outcomesMarkdown: String? = null,
    val sensitive: Boolean = false,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    init {
        agendaItems.forEach { item ->
            require(item.text.isNotBlank()) { "Agenda item text must not be blank" }
        }
    }

    fun updateNotes(notes: String?): OneOnOneEntry =
        copy(notesMarkdown = notes, updatedAt = Instant.now())

    fun updateOutcomes(outcomes: String?): OneOnOneEntry =
        copy(outcomesMarkdown = outcomes, updatedAt = Instant.now())

    fun toggleSensitive(): OneOnOneEntry =
        copy(sensitive = !sensitive, updatedAt = Instant.now())

    fun updateAgendaItems(items: List<AgendaItem>): OneOnOneEntry =
        copy(agendaItems = items, updatedAt = Instant.now())
}
