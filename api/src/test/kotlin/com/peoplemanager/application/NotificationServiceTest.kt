package com.peoplemanager.application

import com.peoplemanager.application.commands.MarkAllNotificationsReadCommand
import com.peoplemanager.application.commands.MarkNotificationReadCommand
import com.peoplemanager.application.port.output.NotificationRepository
import com.peoplemanager.application.queries.GetNotificationsQuery
import com.peoplemanager.application.queries.GetUnreadCountQuery
import com.peoplemanager.domain.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.Instant

class NotificationServiceTest {

    private val notificationRepository = mockk<NotificationRepository>()
    private val service = NotificationService(notificationRepository)

    private val userId = UserId.generate()
    private val notificationId = NotificationId.generate()

    @BeforeEach
    fun setup() {
        clearAllMocks()
    }

    @Nested
    inner class GetNotificationsTests {

        @Test
        fun `should return paginated notifications for user`() {
            val notification = Notification(
                id = notificationId,
                userId = userId,
                type = NotificationType.ACTION_ITEM_OVERDUE,
                title = "Overdue",
                message = "Task is overdue"
            )
            val pageable = PageRequest.of(0, 20)
            every { notificationRepository.findAllByUserId(userId, false, pageable) } returns PageImpl(listOf(notification))

            val query = GetNotificationsQuery(userId = userId, unreadOnly = false, pageable = pageable)
            val result = service.getNotifications(query)

            result.content.size shouldBe 1
            result.content[0].id shouldBe notificationId
        }

        @Test
        fun `should return only unread notifications when requested`() {
            val pageable = PageRequest.of(0, 20)
            every { notificationRepository.findAllByUserId(userId, true, pageable) } returns PageImpl(emptyList())

            val query = GetNotificationsQuery(userId = userId, unreadOnly = true, pageable = pageable)
            val result = service.getNotifications(query)

            result.content.size shouldBe 0
            verify { notificationRepository.findAllByUserId(userId, true, pageable) }
        }
    }

    @Nested
    inner class GetUnreadCountTests {

        @Test
        fun `should return unread count for user`() {
            every { notificationRepository.countUnreadByUserId(userId) } returns 5L

            val result = service.getUnreadCount(GetUnreadCountQuery(userId))

            result shouldBe 5L
        }

        @Test
        fun `should return zero when no unread notifications`() {
            every { notificationRepository.countUnreadByUserId(userId) } returns 0L

            val result = service.getUnreadCount(GetUnreadCountQuery(userId))

            result shouldBe 0L
        }
    }

    @Nested
    inner class MarkAsReadTests {

        @Test
        fun `should mark notification as read`() {
            val notification = Notification(
                id = notificationId,
                userId = userId,
                type = NotificationType.ACTION_ITEM_OVERDUE,
                title = "Overdue",
                message = "Task is overdue"
            )
            every { notificationRepository.findByIdAndUserId(notificationId, userId) } returns notification
            every { notificationRepository.save(any()) } answers { firstArg() }

            val command = MarkNotificationReadCommand(userId = userId, notificationId = notificationId)
            val result = service.markAsRead(command)

            result.isRead shouldBe true
            result.readAt shouldNotBe null
            verify { notificationRepository.save(match { it.isRead }) }
        }

        @Test
        fun `should return already-read notification without saving again`() {
            val notification = Notification(
                id = notificationId,
                userId = userId,
                type = NotificationType.ACTION_ITEM_OVERDUE,
                title = "Overdue",
                message = "Task is overdue",
                readAt = Instant.now()
            )
            every { notificationRepository.findByIdAndUserId(notificationId, userId) } returns notification

            val command = MarkNotificationReadCommand(userId = userId, notificationId = notificationId)
            val result = service.markAsRead(command)

            result.isRead shouldBe true
            verify(exactly = 0) { notificationRepository.save(any()) }
        }

        @Test
        fun `should throw NotificationNotFoundException when notification does not exist`() {
            every { notificationRepository.findByIdAndUserId(notificationId, userId) } returns null

            val command = MarkNotificationReadCommand(userId = userId, notificationId = notificationId)

            shouldThrow<NotificationNotFoundException> {
                service.markAsRead(command)
            }
        }

        @Test
        fun `should not find notification belonging to another user`() {
            val otherUserId = UserId.generate()
            every { notificationRepository.findByIdAndUserId(notificationId, otherUserId) } returns null

            val command = MarkNotificationReadCommand(userId = otherUserId, notificationId = notificationId)

            shouldThrow<NotificationNotFoundException> {
                service.markAsRead(command)
            }
        }
    }

    @Nested
    inner class MarkAllAsReadTests {

        @Test
        fun `should mark all notifications as read for user`() {
            every { notificationRepository.markAllAsReadByUserId(userId, any()) } returns 3

            val command = MarkAllNotificationsReadCommand(userId)
            val result = service.markAllAsRead(command)

            result shouldBe 3
            verify { notificationRepository.markAllAsReadByUserId(userId, any()) }
        }

        @Test
        fun `should return zero when no unread notifications exist`() {
            every { notificationRepository.markAllAsReadByUserId(userId, any()) } returns 0

            val command = MarkAllNotificationsReadCommand(userId)
            val result = service.markAllAsRead(command)

            result shouldBe 0
        }
    }
}

private infix fun Any?.shouldNotBe(other: Any?) {
    if (this == other) throw AssertionError("Expected value to not be $other")
}
