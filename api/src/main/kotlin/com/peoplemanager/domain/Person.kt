package com.peoplemanager.domain

import java.time.Instant
import java.time.LocalDate

data class Person(
    val id: PersonId,
    val userId: UserId,
    val name: String,
    val preferredName: String? = null,
    val roleTitle: String? = null,
    val timezone: String? = null,
    val startDate: LocalDate? = null,
    val email: String? = null,
    val tags: List<String> = emptyList(),
    val moraleStatus: MoraleStatus = MoraleStatus.UNKNOWN,
    val moraleNote: String? = null,
    val pinnedRememberItems: List<PinnedRememberItem> = emptyList(),
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    init {
        require(name.isNotBlank()) { "Person name must not be blank" }
    }

    fun updateMorale(status: MoraleStatus, note: String?): Person =
        copy(moraleStatus = status, moraleNote = note, updatedAt = Instant.now())

    fun addRememberItem(text: String): Person {
        val newItem = PinnedRememberItem(
            id = RememberItemId.generate(),
            text = text,
            displayOrder = pinnedRememberItems.size,
            createdAt = Instant.now()
        )
        return copy(
            pinnedRememberItems = pinnedRememberItems + newItem,
            updatedAt = Instant.now()
        )
    }

    fun removeRememberItem(itemId: RememberItemId): Person {
        val updatedItems = pinnedRememberItems
            .filter { it.id != itemId }
            .mapIndexed { index, item -> item.copy(displayOrder = index) }
        return copy(
            pinnedRememberItems = updatedItems,
            updatedAt = Instant.now()
        )
    }

    fun reorderRememberItems(orderedIds: List<RememberItemId>): Person {
        val itemMap = pinnedRememberItems.associateBy { it.id }
        val reordered = orderedIds.mapIndexed { index, id ->
            itemMap[id]?.copy(displayOrder = index)
                ?: throw IllegalArgumentException("Remember item with id $id not found")
        }
        return copy(
            pinnedRememberItems = reordered,
            updatedAt = Instant.now()
        )
    }
}
