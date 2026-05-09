package com.peoplemanager.domain

import java.time.Instant
import java.time.LocalDate

data class ActionItem(
    val id: ActionItemId,
    val userId: UserId,
    val personId: PersonId,
    val title: String,
    val description: String? = null,
    val ownerType: ActionItemOwnerType = ActionItemOwnerType.MANAGER,
    val dueDate: LocalDate? = null,
    val status: ActionItemStatus = ActionItemStatus.OPEN,
    val originatingEntryId: OneOnOneEntryId? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    init {
        require(title.isNotBlank()) { "Action item title must not be blank" }
    }

    fun complete(): ActionItem {
        require(status == ActionItemStatus.OPEN) {
            "Can only complete an action item with status OPEN, current status is $status"
        }
        return copy(status = ActionItemStatus.DONE, updatedAt = Instant.now())
    }

    fun cancel(): ActionItem {
        require(status == ActionItemStatus.OPEN) {
            "Can only cancel an action item with status OPEN, current status is $status"
        }
        return copy(status = ActionItemStatus.CANCELED, updatedAt = Instant.now())
    }

    fun updateDetails(
        title: String? = null,
        description: String? = null,
        ownerType: ActionItemOwnerType? = null,
        dueDate: LocalDate? = null
    ): ActionItem {
        val newTitle = title ?: this.title
        require(newTitle.isNotBlank()) { "Action item title must not be blank" }
        return copy(
            title = newTitle,
            description = description ?: this.description,
            ownerType = ownerType ?: this.ownerType,
            dueDate = dueDate ?: this.dueDate,
            updatedAt = Instant.now()
        )
    }

    fun isOverdue(referenceDate: LocalDate = LocalDate.now()): Boolean {
        return status == ActionItemStatus.OPEN && dueDate != null && dueDate.isBefore(referenceDate)
    }
}
