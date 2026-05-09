package com.peoplemanager.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ActionItemTest {

    private val userId = UserId.generate()
    private val personId = PersonId.generate()

    private fun createActionItem(
        title: String = "Follow up on project plan",
        description: String? = "Discuss timeline with team",
        ownerType: ActionItemOwnerType = ActionItemOwnerType.MANAGER,
        dueDate: LocalDate? = LocalDate.of(2026, 5, 15),
        status: ActionItemStatus = ActionItemStatus.OPEN
    ) = ActionItem(
        id = ActionItemId.generate(),
        userId = userId,
        personId = personId,
        title = title,
        description = description,
        ownerType = ownerType,
        dueDate = dueDate,
        status = status
    )

    @Nested
    inner class CreationTests {

        @Test
        fun `should create action item with valid fields`() {
            val item = createActionItem()

            item.title shouldBe "Follow up on project plan"
            item.description shouldBe "Discuss timeline with team"
            item.ownerType shouldBe ActionItemOwnerType.MANAGER
            item.dueDate shouldBe LocalDate.of(2026, 5, 15)
            item.status shouldBe ActionItemStatus.OPEN
            item.userId shouldBe userId
            item.personId shouldBe personId
        }

        @Test
        fun `should create action item with minimal fields`() {
            val item = ActionItem(
                id = ActionItemId.generate(),
                userId = userId,
                personId = personId,
                title = "Simple task"
            )

            item.title shouldBe "Simple task"
            item.description shouldBe null
            item.ownerType shouldBe ActionItemOwnerType.MANAGER
            item.dueDate shouldBe null
            item.status shouldBe ActionItemStatus.OPEN
            item.originatingEntryId shouldBe null
        }

        @Test
        fun `should create action item with originating entry id`() {
            val entryId = OneOnOneEntryId.generate()
            val item = ActionItem(
                id = ActionItemId.generate(),
                userId = userId,
                personId = personId,
                title = "From 1:1",
                originatingEntryId = entryId
            )

            item.originatingEntryId shouldBe entryId
        }

        @Test
        fun `should reject blank title`() {
            shouldThrow<IllegalArgumentException> {
                createActionItem(title = "")
            }.message shouldBe "Action item title must not be blank"
        }

        @Test
        fun `should reject whitespace-only title`() {
            shouldThrow<IllegalArgumentException> {
                createActionItem(title = "   ")
            }.message shouldBe "Action item title must not be blank"
        }
    }

    @Nested
    inner class StatusTransitionTests {

        @Test
        fun `should complete an open action item`() {
            val item = createActionItem(status = ActionItemStatus.OPEN)

            val completed = item.complete()

            completed.status shouldBe ActionItemStatus.DONE
        }

        @Test
        fun `should cancel an open action item`() {
            val item = createActionItem(status = ActionItemStatus.OPEN)

            val canceled = item.cancel()

            canceled.status shouldBe ActionItemStatus.CANCELED
        }

        @Test
        fun `should not complete a done action item`() {
            val item = createActionItem(status = ActionItemStatus.DONE)

            shouldThrow<IllegalArgumentException> {
                item.complete()
            }.message shouldBe "Can only complete an action item with status OPEN, current status is DONE"
        }

        @Test
        fun `should not complete a canceled action item`() {
            val item = createActionItem(status = ActionItemStatus.CANCELED)

            shouldThrow<IllegalArgumentException> {
                item.complete()
            }.message shouldBe "Can only complete an action item with status OPEN, current status is CANCELED"
        }

        @Test
        fun `should not cancel a done action item`() {
            val item = createActionItem(status = ActionItemStatus.DONE)

            shouldThrow<IllegalArgumentException> {
                item.cancel()
            }.message shouldBe "Can only cancel an action item with status OPEN, current status is DONE"
        }

        @Test
        fun `should not cancel a canceled action item`() {
            val item = createActionItem(status = ActionItemStatus.CANCELED)

            shouldThrow<IllegalArgumentException> {
                item.cancel()
            }.message shouldBe "Can only cancel an action item with status OPEN, current status is CANCELED"
        }
    }

    @Nested
    inner class UpdateDetailsTests {

        @Test
        fun `should update title`() {
            val item = createActionItem()

            val updated = item.updateDetails(title = "New title")

            updated.title shouldBe "New title"
            updated.description shouldBe item.description
        }

        @Test
        fun `should update description`() {
            val item = createActionItem()

            val updated = item.updateDetails(description = "New description")

            updated.description shouldBe "New description"
            updated.title shouldBe item.title
        }

        @Test
        fun `should update owner type`() {
            val item = createActionItem(ownerType = ActionItemOwnerType.MANAGER)

            val updated = item.updateDetails(ownerType = ActionItemOwnerType.PERSON)

            updated.ownerType shouldBe ActionItemOwnerType.PERSON
        }

        @Test
        fun `should update due date`() {
            val item = createActionItem()
            val newDate = LocalDate.of(2026, 6, 1)

            val updated = item.updateDetails(dueDate = newDate)

            updated.dueDate shouldBe newDate
        }

        @Test
        fun `should reject blank title on update`() {
            val item = createActionItem()

            shouldThrow<IllegalArgumentException> {
                item.updateDetails(title = "  ")
            }.message shouldBe "Action item title must not be blank"
        }

        @Test
        fun `should update updatedAt timestamp`() {
            val item = createActionItem()

            val updated = item.updateDetails(title = "Updated")

            updated.updatedAt.isAfter(item.createdAt) shouldBe true
        }
    }

    @Nested
    inner class OverdueTests {

        @Test
        fun `should be overdue when due date is in the past and status is OPEN`() {
            val item = createActionItem(
                dueDate = LocalDate.of(2026, 5, 1),
                status = ActionItemStatus.OPEN
            )

            item.isOverdue(LocalDate.of(2026, 5, 10)) shouldBe true
        }

        @Test
        fun `should not be overdue when due date is today`() {
            val today = LocalDate.of(2026, 5, 10)
            val item = createActionItem(
                dueDate = today,
                status = ActionItemStatus.OPEN
            )

            item.isOverdue(today) shouldBe false
        }

        @Test
        fun `should not be overdue when due date is in the future`() {
            val item = createActionItem(
                dueDate = LocalDate.of(2026, 5, 20),
                status = ActionItemStatus.OPEN
            )

            item.isOverdue(LocalDate.of(2026, 5, 10)) shouldBe false
        }

        @Test
        fun `should not be overdue when status is DONE`() {
            val item = createActionItem(
                dueDate = LocalDate.of(2026, 5, 1),
                status = ActionItemStatus.DONE
            )

            item.isOverdue(LocalDate.of(2026, 5, 10)) shouldBe false
        }

        @Test
        fun `should not be overdue when status is CANCELED`() {
            val item = createActionItem(
                dueDate = LocalDate.of(2026, 5, 1),
                status = ActionItemStatus.CANCELED
            )

            item.isOverdue(LocalDate.of(2026, 5, 10)) shouldBe false
        }

        @Test
        fun `should not be overdue when no due date`() {
            val item = createActionItem(
                dueDate = null,
                status = ActionItemStatus.OPEN
            )

            item.isOverdue(LocalDate.of(2026, 5, 10)) shouldBe false
        }
    }
}
