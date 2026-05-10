package com.peoplemanager.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class NotificationTest {

    private val userId = UserId.generate()
    private val personId = PersonId.generate()
    private val actionItemId = ActionItemId.generate()

    @Nested
    inner class CreationTests {

        @Test
        fun `should create notification with required fields`() {
            val notification = Notification(
                userId = userId,
                type = NotificationType.ACTION_ITEM_OVERDUE,
                title = "Action item overdue",
                message = "Task X is overdue"
            )

            notification.userId shouldBe userId
            notification.type shouldBe NotificationType.ACTION_ITEM_OVERDUE
            notification.title shouldBe "Action item overdue"
            notification.message shouldBe "Task X is overdue"
            notification.readAt shouldBe null
            notification.isRead shouldBe false
            notification.referenceId shouldBe null
            notification.personId shouldBe null
            notification.id shouldNotBe null
            notification.createdAt shouldNotBe null
        }

        @Test
        fun `should create notification with optional fields`() {
            val notification = Notification(
                userId = userId,
                type = NotificationType.ACTION_ITEM_DUE_SOON,
                title = "Due soon",
                message = "Task Y is due soon",
                referenceId = actionItemId.value.toString(),
                personId = personId
            )

            notification.referenceId shouldBe actionItemId.value.toString()
            notification.personId shouldBe personId
        }

        @Test
        fun `should reject blank title`() {
            shouldThrow<IllegalArgumentException> {
                Notification(
                    userId = userId,
                    type = NotificationType.ACTION_ITEM_OVERDUE,
                    title = "   ",
                    message = "Some message"
                )
            }.message shouldContain "title"
        }

        @Test
        fun `should reject blank message`() {
            shouldThrow<IllegalArgumentException> {
                Notification(
                    userId = userId,
                    type = NotificationType.ACTION_ITEM_OVERDUE,
                    title = "Some title",
                    message = ""
                )
            }.message shouldContain "message"
        }
    }

    @Nested
    inner class MarkAsReadTests {

        @Test
        fun `should mark notification as read`() {
            val notification = Notification(
                userId = userId,
                type = NotificationType.STALE_ONE_ON_ONE,
                title = "1:1 overdue",
                message = "You haven't met with Alice"
            )

            val readNotification = notification.markAsRead()

            readNotification.isRead shouldBe true
            readNotification.readAt shouldNotBe null
        }

        @Test
        fun `should mark notification as read with specific timestamp`() {
            val notification = Notification(
                userId = userId,
                type = NotificationType.STALE_ONE_ON_ONE,
                title = "1:1 overdue",
                message = "You haven't met with Alice"
            )
            val readTime = Instant.parse("2026-05-10T12:00:00Z")

            val readNotification = notification.markAsRead(readTime)

            readNotification.readAt shouldBe readTime
        }

        @Test
        fun `should preserve other fields when marking as read`() {
            val notification = Notification(
                userId = userId,
                type = NotificationType.UPCOMING_ANNIVERSARY,
                title = "Anniversary",
                message = "Alice's 2-year anniversary",
                referenceId = personId.value.toString(),
                personId = personId
            )

            val readNotification = notification.markAsRead()

            readNotification.id shouldBe notification.id
            readNotification.userId shouldBe notification.userId
            readNotification.type shouldBe notification.type
            readNotification.title shouldBe notification.title
            readNotification.message shouldBe notification.message
            readNotification.referenceId shouldBe notification.referenceId
            readNotification.personId shouldBe notification.personId
            readNotification.createdAt shouldBe notification.createdAt
        }
    }

    @Nested
    inner class FactoryMethodTests {

        @Test
        fun `should create action item overdue notification`() {
            val notification = Notification.actionItemOverdue(
                userId = userId,
                personId = personId,
                personName = "Alice Smith",
                actionItemId = actionItemId,
                actionItemTitle = "Review PR",
                dueDate = LocalDate.of(2026, 5, 8)
            )

            notification.userId shouldBe userId
            notification.type shouldBe NotificationType.ACTION_ITEM_OVERDUE
            notification.title shouldBe "Action item overdue"
            notification.message shouldContain "Review PR"
            notification.message shouldContain "Alice Smith"
            notification.message shouldContain "2026-05-08"
            notification.referenceId shouldBe actionItemId.value.toString()
            notification.personId shouldBe personId
            notification.isRead shouldBe false
        }

        @Test
        fun `should create action item due soon notification`() {
            val notification = Notification.actionItemDueSoon(
                userId = userId,
                personId = personId,
                personName = "Bob Jones",
                actionItemId = actionItemId,
                actionItemTitle = "Submit report",
                dueDate = LocalDate.of(2026, 5, 12)
            )

            notification.userId shouldBe userId
            notification.type shouldBe NotificationType.ACTION_ITEM_DUE_SOON
            notification.title shouldBe "Action item due soon"
            notification.message shouldContain "Submit report"
            notification.message shouldContain "Bob Jones"
            notification.message shouldContain "2026-05-12"
            notification.referenceId shouldBe actionItemId.value.toString()
            notification.personId shouldBe personId
        }

        @Test
        fun `should create stale one-on-one notification`() {
            val notification = Notification.staleOneOnOne(
                userId = userId,
                personId = personId,
                personName = "Charlie Brown",
                daysSinceLastMeeting = 14
            )

            notification.userId shouldBe userId
            notification.type shouldBe NotificationType.STALE_ONE_ON_ONE
            notification.title shouldBe "1:1 overdue"
            notification.message shouldContain "Charlie Brown"
            notification.message shouldContain "14 days"
            notification.referenceId shouldBe personId.value.toString()
            notification.personId shouldBe personId
        }

        @Test
        fun `should create upcoming anniversary notification`() {
            val notification = Notification.upcomingAnniversary(
                userId = userId,
                personId = personId,
                personName = "Diana Prince",
                yearsCompleted = 3,
                daysUntil = 5
            )

            notification.userId shouldBe userId
            notification.type shouldBe NotificationType.UPCOMING_ANNIVERSARY
            notification.title shouldBe "Upcoming work anniversary"
            notification.message shouldContain "Diana Prince"
            notification.message shouldContain "3-year"
            notification.message shouldContain "5 days"
            notification.referenceId shouldBe personId.value.toString()
            notification.personId shouldBe personId
        }
    }

    @Nested
    inner class NotificationTypeTests {

        @Test
        fun `should have all expected notification types`() {
            val types = NotificationType.entries
            types.size shouldBe 4
            types shouldBe listOf(
                NotificationType.ACTION_ITEM_OVERDUE,
                NotificationType.ACTION_ITEM_DUE_SOON,
                NotificationType.STALE_ONE_ON_ONE,
                NotificationType.UPCOMING_ANNIVERSARY
            )
        }
    }
}
