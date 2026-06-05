package com.peoplemanager.application.port.output

import com.peoplemanager.domain.Notification
import com.peoplemanager.domain.NotificationId
import com.peoplemanager.domain.NotificationType
import com.peoplemanager.domain.UserId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.Instant
import java.time.LocalDate

interface NotificationRepository {
    fun save(notification: Notification): Notification
    fun saveAll(notifications: List<Notification>): List<Notification>
    fun findByIdAndUserId(notificationId: NotificationId, userId: UserId): Notification?
    fun findAllByUserId(userId: UserId, unreadOnly: Boolean, pageable: Pageable): Page<Notification>
    fun countUnreadByUserId(userId: UserId): Long
    fun markAllAsReadByUserId(userId: UserId, readAt: Instant): Int

    /**
     * Check if a notification of the given type already exists for this user
     * with the given referenceId, created on or after the given date.
     * Used to prevent duplicate notifications within the same scheduling window.
     */
    fun existsByUserIdAndTypeAndReferenceIdAndCreatedAfter(
        userId: UserId,
        type: NotificationType,
        referenceId: String,
        createdAfter: Instant
    ): Boolean
}
