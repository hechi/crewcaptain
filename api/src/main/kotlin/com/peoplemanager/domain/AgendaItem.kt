package com.peoplemanager.domain

import java.time.Instant

data class AgendaItem(
    val id: AgendaItemId,
    val text: String,
    val checked: Boolean = false,
    val displayOrder: Int,
    val createdAt: Instant = Instant.now()
) {
    init {
        require(text.isNotBlank()) { "Agenda item text must not be blank" }
    }
}
