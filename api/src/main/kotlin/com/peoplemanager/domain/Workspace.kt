package com.peoplemanager.domain

import java.time.Instant

data class Workspace(
    val id: WorkspaceId,
    val userId: UserId,
    val name: String,
    val description: String? = null,
    val displayOrder: Int = 0,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    init {
        require(name.isNotBlank()) { "Workspace name must not be blank" }
        require(name.length <= 100) { "Workspace name must not exceed 100 characters" }
        description?.let {
            require(it.length <= 500) { "Workspace description must not exceed 500 characters" }
        }
    }

    fun updateDetails(name: String? = null, description: String? = null): Workspace {
        val newName = name ?: this.name
        require(newName.isNotBlank()) { "Workspace name must not be blank" }
        require(newName.length <= 100) { "Workspace name must not exceed 100 characters" }
        val newDescription = description ?: this.description
        newDescription?.let {
            require(it.length <= 500) { "Workspace description must not exceed 500 characters" }
        }
        return copy(
            name = newName,
            description = newDescription,
            updatedAt = Instant.now()
        )
    }

    fun reorder(newDisplayOrder: Int): Workspace {
        require(newDisplayOrder >= 0) { "Display order must not be negative" }
        return copy(displayOrder = newDisplayOrder, updatedAt = Instant.now())
    }
}
