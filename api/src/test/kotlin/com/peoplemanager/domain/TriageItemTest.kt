package com.peoplemanager.domain

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class TriageItemTest {

    private val personId = PersonId.generate()

    @Nested
    inner class TriageItemCreationTests {

        @Test
        fun `should create overdue action item triage item`() {
            val item = TriageItem(
                id = "ai-123",
                type = TriageItemType.ACTION_ITEM_OVERDUE,
                criticality = TriageCriticality.OVERDUE,
                title = "Follow up on project",
                personId = personId,
                personName = "Alice",
                dueDate = LocalDate.of(2026, 5, 1),
                daysOverdue = 5,
                ownerType = ActionItemOwnerType.MANAGER,
                sourceActionItemId = ActionItemId.generate()
            )

            item.type shouldBe TriageItemType.ACTION_ITEM_OVERDUE
            item.criticality shouldBe TriageCriticality.OVERDUE
            item.daysOverdue shouldBe 5
            item.personName shouldBe "Alice"
        }

        @Test
        fun `should create due soon action item triage item`() {
            val item = TriageItem(
                id = "ai-ds-456",
                type = TriageItemType.ACTION_ITEM_DUE_SOON,
                criticality = TriageCriticality.DUE_SOON,
                title = "Submit report",
                personId = personId,
                personName = "Bob",
                dueDate = LocalDate.of(2026, 6, 10),
                daysUntilDue = 3
            )

            item.type shouldBe TriageItemType.ACTION_ITEM_DUE_SOON
            item.criticality shouldBe TriageCriticality.DUE_SOON
            item.daysUntilDue shouldBe 3
        }

        @Test
        fun `should create stale one on one triage item`() {
            val item = TriageItem(
                id = "stale-789",
                type = TriageItemType.STALE_ONE_ON_ONE,
                criticality = TriageCriticality.STALE,
                title = "1:1 overdue by 5d",
                personId = personId,
                personName = "Charlie",
                daysOverdue = 5
            )

            item.type shouldBe TriageItemType.STALE_ONE_ON_ONE
            item.criticality shouldBe TriageCriticality.STALE
        }

        @Test
        fun `should create upcoming anniversary triage item`() {
            val item = TriageItem(
                id = "anniv-101",
                type = TriageItemType.UPCOMING_ANNIVERSARY,
                criticality = TriageCriticality.INFORMATIONAL,
                title = "3-year anniversary in 7d",
                personId = personId,
                personName = "Dana",
                daysUntilDue = 7
            )

            item.type shouldBe TriageItemType.UPCOMING_ANNIVERSARY
            item.criticality shouldBe TriageCriticality.INFORMATIONAL
        }
    }

    @Nested
    inner class SnoozedTests {

        @Test
        fun `should be snoozed when snoozedUntil is in the future`() {
            val item = TriageItem(
                id = "ai-123",
                type = TriageItemType.ACTION_ITEM_OVERDUE,
                criticality = TriageCriticality.OVERDUE,
                title = "Test",
                personId = personId,
                personName = "Alice",
                snoozedUntil = Instant.now().plusSeconds(3600)
            )

            item.isSnoozed shouldBe true
        }

        @Test
        fun `should not be snoozed when snoozedUntil is in the past`() {
            val item = TriageItem(
                id = "ai-123",
                type = TriageItemType.ACTION_ITEM_OVERDUE,
                criticality = TriageCriticality.OVERDUE,
                title = "Test",
                personId = personId,
                personName = "Alice",
                snoozedUntil = Instant.now().minusSeconds(3600)
            )

            item.isSnoozed shouldBe false
        }

        @Test
        fun `should not be snoozed when snoozedUntil is null`() {
            val item = TriageItem(
                id = "ai-123",
                type = TriageItemType.ACTION_ITEM_OVERDUE,
                criticality = TriageCriticality.OVERDUE,
                title = "Test",
                personId = personId,
                personName = "Alice",
                snoozedUntil = null
            )

            item.isSnoozed shouldBe false
        }
    }

    @Nested
    inner class CriticalitySortOrderTests {

        @Test
        fun `OVERDUE has lowest sort order`() {
            TriageCriticality.OVERDUE.sortOrder shouldBe 0
        }

        @Test
        fun `DUE_SOON is after OVERDUE`() {
            TriageCriticality.DUE_SOON.sortOrder shouldBe 1
        }

        @Test
        fun `STALE is after DUE_SOON`() {
            TriageCriticality.STALE.sortOrder shouldBe 2
        }

        @Test
        fun `INFORMATIONAL has highest sort order`() {
            TriageCriticality.INFORMATIONAL.sortOrder shouldBe 3
        }
    }

    @Nested
    inner class ActionItemSnoozeTests {

        private val userId = UserId.generate()

        @Test
        fun `should snooze an open action item`() {
            val item = ActionItem(
                id = ActionItemId.generate(),
                userId = userId,
                personId = personId,
                title = "Test task",
                status = ActionItemStatus.OPEN
            )

            val snoozedUntil = Instant.now().plusSeconds(86400)
            val snoozed = item.snooze(snoozedUntil)

            snoozed.snoozedUntil shouldBe snoozedUntil
            snoozed.isSnoozed shouldBe true
        }

        @Test
        fun `should not snooze a done action item`() {
            val item = ActionItem(
                id = ActionItemId.generate(),
                userId = userId,
                personId = personId,
                title = "Done task",
                status = ActionItemStatus.DONE
            )

            val ex = org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
                item.snooze(Instant.now().plusSeconds(86400))
            }
            ex.message shouldBe "Can only snooze an action item with status OPEN, current status is DONE"
        }

        @Test
        fun `should not snooze a canceled action item`() {
            val item = ActionItem(
                id = ActionItemId.generate(),
                userId = userId,
                personId = personId,
                title = "Canceled task",
                status = ActionItemStatus.CANCELED
            )

            val ex = org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
                item.snooze(Instant.now().plusSeconds(86400))
            }
            ex.message shouldBe "Can only snooze an action item with status OPEN, current status is CANCELED"
        }

        @Test
        fun `isSnoozed returns false when snoozedUntil is in the past`() {
            val item = ActionItem(
                id = ActionItemId.generate(),
                userId = userId,
                personId = personId,
                title = "Test task",
                status = ActionItemStatus.OPEN,
                snoozedUntil = Instant.now().minusSeconds(3600)
            )

            item.isSnoozed shouldBe false
        }
    }
}
