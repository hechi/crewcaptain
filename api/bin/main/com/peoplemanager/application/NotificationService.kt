package com.peoplemanager.application

import com.peoplemanager.application.commands.MarkAllNotificationsReadCommand
import com.peoplemanager.application.commands.MarkNotificationReadCommand
import com.peoplemanager.application.ports.NotificationCommandPort
import com.peoplemanager.application.ports.NotificationQueryPort
import com.peoplemanager.application.ports.NotificationRepository
import com.peoplemanager.application.queries.GetNotificationsQuery
import com.peoplemanager.application.queries.GetUnreadCountQuery
import com.peoplemanager.domain.Notification
import org.springframework.data.domain.Page
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
@Transactional
class NotificationService(
    private val notificationRepository: NotificationRepository
) : NotificationCommandPort, NotificationQueryPort {

    @Transactional(readOnly = true)
    override fun getNotifications(query: GetNotificationsQuery): Page<Notification> {
        return notificationRepository.findAllByUserId(
            userId = query.userId,
            unreadOnly = query.unreadOnly,
            pageable = query.pageable
        )
    }

    @Transactional(readOnly = true)
    override fun getUnreadCount(query: GetUnreadCountQuery): Long {
        return notificationRepository.countUnreadByUserId(query.userId)
    }

    override fun markAsRead(command: MarkNotificationReadCommand): Notification {
        val notification = notificationRepository.findByIdAndUserId(command.notificationId, command.userId)
            ?: throw NotificationNotFoundException(command.notificationId)

        if (notification.isRead) {
            return notification
        }

        val readNotification = notification.markAsRead(Instant.now())
        return notificationRepository.save(readNotification)
    }

    override fun markAllAsRead(command: MarkAllNotificationsReadCommand): Int {
        return notificationRepository.markAllAsReadByUserId(command.userId, Instant.now())
    }
}
