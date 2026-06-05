package com.peoplemanager.adapters.web

import com.peoplemanager.adapters.auth.AuthenticatedUser
import com.peoplemanager.adapters.web.dto.MarkAllReadResponse
import com.peoplemanager.adapters.web.dto.NotificationResponse
import com.peoplemanager.adapters.web.dto.PaginatedNotificationResponse
import com.peoplemanager.adapters.web.dto.UnreadCountResponse
import com.peoplemanager.application.commands.MarkAllNotificationsReadCommand
import com.peoplemanager.application.commands.MarkNotificationReadCommand
import com.peoplemanager.application.port.input.NotificationCommandPort
import com.peoplemanager.application.port.input.NotificationQueryPort
import com.peoplemanager.application.queries.GetNotificationsQuery
import com.peoplemanager.application.queries.GetUnreadCountQuery
import com.peoplemanager.domain.NotificationId
import org.springframework.data.domain.PageRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/notifications")
class NotificationController(
    private val notificationQueryPort: NotificationQueryPort,
    private val notificationCommandPort: NotificationCommandPort
) {

    @GetMapping
    fun listNotifications(
        @RequestParam(defaultValue = "false") unreadOnly: Boolean,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PaginatedNotificationResponse> {
        val userId = AuthenticatedUser.getUserId()
        val query = GetNotificationsQuery(
            userId = userId,
            unreadOnly = unreadOnly,
            pageable = PageRequest.of(page, size)
        )
        val notifications = notificationQueryPort.getNotifications(query)
        return ResponseEntity.ok(PaginatedNotificationResponse.from(notifications))
    }

    @GetMapping("/unread-count")
    fun getUnreadCount(): ResponseEntity<UnreadCountResponse> {
        val userId = AuthenticatedUser.getUserId()
        val count = notificationQueryPort.getUnreadCount(GetUnreadCountQuery(userId))
        return ResponseEntity.ok(UnreadCountResponse(count))
    }

    @PostMapping("/{notificationId}/read")
    fun markAsRead(@PathVariable notificationId: UUID): ResponseEntity<NotificationResponse> {
        val userId = AuthenticatedUser.getUserId()
        val command = MarkNotificationReadCommand(
            userId = userId,
            notificationId = NotificationId(notificationId)
        )
        val notification = notificationCommandPort.markAsRead(command)
        return ResponseEntity.ok(NotificationResponse.from(notification))
    }

    @PostMapping("/read-all")
    fun markAllAsRead(): ResponseEntity<MarkAllReadResponse> {
        val userId = AuthenticatedUser.getUserId()
        val command = MarkAllNotificationsReadCommand(userId)
        val count = notificationCommandPort.markAllAsRead(command)
        return ResponseEntity.ok(MarkAllReadResponse(count))
    }
}
