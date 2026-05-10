package com.peoplemanager.domain

import java.time.Instant
import java.time.LocalDate

data class Kudos(
    val id: KudosId,
    val userId: UserId,
    val personId: PersonId,
    val date: LocalDate,
    val text: String,
    val tags: List<String> = emptyList(),
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    init {
        require(text.isNotBlank()) { "Kudos text must not be blank" }
    }
}
