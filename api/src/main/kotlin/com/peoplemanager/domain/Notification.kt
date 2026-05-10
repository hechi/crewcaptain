package com.peoplemanager.domain

import java.time.Instant
import java.util.UUID

@JvmInline
value class NotificationId(val value: UUID) {
    companion object {
        fun generate(): NotificationId = NotificationId(UUID.randomUUID())
    }
}

enum class NotificationType {
    ACTION_ITEM_OVERDUE,
    ACTION_ITEM_DUE_SOON,
    STALE_ONE_ON_ONE,
    UPCOMING_ANNIVERSARY
}

/**
 * In-app notification for a manager.
 * Always scoped to a single user (manager). Never shared across users.
 */
data class Notification(
    val id: NotificationId = NotificationId.generate(),
    val userId: UserId,
    val type: NotificationType,
    val title: String,
    val message: String,
    val referenceId: String? = null,
    val personId: PersonId? = null,
    val readAt: Instant? = null,
    val createdAt: Instant = Instant.now()
) {
    init {
        require(title.isNotBlank()) { "Notification title must not be blank" }
        require(message.isNotBlank()) { "Notification message must not be blank" }
    }

    val isRead: Boolean get() = readAt != null

    fun markAsRead(at: Instant = Instant.now()): Notification {
        return copy(readAt = at)
    }

    companion object {
        fun actionItemOverdue(
            userId: UserId,
            personId: PersonId,
            personName: String,
            actionItemId: ActionItemId,
            actionItemTitle: String,
            dueDate: java.time.LocalDate
        ): Notification = Notification(
            userId = userId,
            type = NotificationType.ACTION_ITEM_OVERDUE,
            title = "Action item overdue",
            message = "\"$actionItemTitle\" for $personName was due on $dueDate",
            referenceId = actionItemId.value.toString(),
            personId = personId
        )

        fun actionItemDueSoon(
            userId: UserId,
            personId: PersonId,
            personName: String,
            actionItemId: ActionItemId,
            actionItemTitle: String,
            dueDate: java.time.LocalDate
        ): Notification = Notification(
            userId = userId,
            type = NotificationType.ACTION_ITEM_DUE_SOON,
            title = "Action item due soon",
            message = "\"$actionItemTitle\" for $personName is due on $dueDate",
            referenceId = actionItemId.value.toString(),
            personId = personId
        )

        fun staleOneOnOne(
            userId: UserId,
            personId: PersonId,
            personName: String,
            daysSinceLastMeeting: Long
        ): Notification = Notification(
            userId = userId,
            type = NotificationType.STALE_ONE_ON_ONE,
            title = "1:1 overdue",
            message = "You haven't had a 1:1 with $personName in $daysSinceLastMeeting days",
            referenceId = personId.value.toString(),
            personId = personId
        )

        fun upcomingAnniversary(
            userId: UserId,
            personId: PersonId,
            personName: String,
            yearsCompleted: Int,
            daysUntil: Long
        ): Notification = Notification(
            userId = userId,
            type = NotificationType.UPCOMING_ANNIVERSARY,
            title = "Upcoming work anniversary",
            message = "$personName's ${yearsCompleted}-year work anniversary is in $daysUntil days",
            referenceId = personId.value.toString(),
            personId = personId
        )
    }
}
