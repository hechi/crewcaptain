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
    val workspaceId: WorkspaceId? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val deletedAt: Instant? = null
) {
    init {
        require(name.isNotBlank()) { "Person name must not be blank" }
    }

    val isDeleted: Boolean get() = deletedAt != null

    fun softDelete(): Person = copy(deletedAt = Instant.now(), updatedAt = Instant.now())

    fun restore(): Person {
        require(isDeleted) { "Cannot restore a person that is not deleted" }
        return copy(deletedAt = null, updatedAt = Instant.now())
    }

    fun updateMorale(status: MoraleStatus, note: String?): Person =
        copy(moraleStatus = status, moraleNote = note, updatedAt = Instant.now())

    fun addRememberItem(
        text: String,
        color: StickyNoteColor = StickyNoteColor.CYAN,
        tag: String? = null,
        sensitive: Boolean = false
    ): Person {
        val newItem = PinnedRememberItem(
            id = RememberItemId.generate(),
            text = text,
            color = color,
            tag = tag,
            sensitive = sensitive,
            displayOrder = pinnedRememberItems.size,
            createdAt = Instant.now()
        )
        return copy(
            pinnedRememberItems = pinnedRememberItems + newItem,
            updatedAt = Instant.now()
        )
    }

    fun updateRememberItem(
        itemId: RememberItemId,
        text: String,
        color: StickyNoteColor,
        tag: String?,
        sensitive: Boolean
    ): Person {
        val updatedItems = pinnedRememberItems.map { item ->
            if (item.id == itemId) {
                item.copy(text = text, color = color, tag = tag, sensitive = sensitive)
            } else {
                item
            }
        }
        require(updatedItems.any { it.id == itemId }) {
            "Remember item with id $itemId not found"
        }
        return copy(
            pinnedRememberItems = updatedItems,
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

    fun assignToWorkspace(workspaceId: WorkspaceId?): Person =
        copy(workspaceId = workspaceId, updatedAt = Instant.now())
}
