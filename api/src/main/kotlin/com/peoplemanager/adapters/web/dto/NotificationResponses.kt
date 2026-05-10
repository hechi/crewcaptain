package com.peoplemanager.adapters.web.dto

import com.peoplemanager.domain.Notification
import com.peoplemanager.domain.NotificationType
import org.springframework.data.domain.Page
import java.time.Instant

data class NotificationResponse(
    val id: String,
    val type: NotificationType,
    val title: String,
    val message: String,
    val referenceId: String?,
    val personId: String?,
    val isRead: Boolean,
    val readAt: Instant?,
    val createdAt: Instant
) {
    companion object {
        fun from(notification: Notification): NotificationResponse = NotificationResponse(
            id = notification.id.value.toString(),
            type = notification.type,
            title = notification.title,
            message = notification.message,
            referenceId = notification.referenceId,
            personId = notification.personId?.value?.toString(),
            isRead = notification.isRead,
            readAt = notification.readAt,
            createdAt = notification.createdAt
        )
    }
}

data class PaginatedNotificationResponse(
    val content: List<NotificationResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
) {
    companion object {
        fun from(page: Page<Notification>): PaginatedNotificationResponse = PaginatedNotificationResponse(
            content = page.content.map { NotificationResponse.from(it) },
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages
        )
    }
}

data class UnreadCountResponse(
    val count: Long
)

data class MarkAllReadResponse(
    val markedCount: Int
)
