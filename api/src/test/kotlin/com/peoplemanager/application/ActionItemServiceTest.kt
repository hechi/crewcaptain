package com.peoplemanager.application

import com.peoplemanager.application.commands.CancelActionItemCommand
import com.peoplemanager.application.commands.CompleteActionItemCommand
import com.peoplemanager.application.commands.CreateActionItemCommand
import com.peoplemanager.application.commands.DeleteActionItemCommand
import com.peoplemanager.application.commands.UpdateActionItemCommand
import com.peoplemanager.application.port.output.ActionItemRepository
import com.peoplemanager.application.port.output.PersonRepository
import com.peoplemanager.application.queries.CountOpenActionItemsQuery
import com.peoplemanager.application.queries.GetActionItemQuery
import com.peoplemanager.application.queries.ListActionItemsByPersonQuery
import com.peoplemanager.application.queries.ListAllActionItemsQuery
import com.peoplemanager.domain.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.time.LocalDate

class ActionItemServiceTest {

    private val personRepository = mockk<PersonRepository>()
    private val actionItemRepository = mockk<ActionItemRepository>()
    private val auditLogService = mockk<AuditLogService>(relaxed = true)

    private val service = ActionItemService(personRepository, actionItemRepository, auditLogService)

    private val userId = UserId.generate()
    private val personId = PersonId.generate()
    private val person = Person(
        id = personId,
        userId = userId,
        name = "Test Person"
    )

    @BeforeEach
    fun setup() {
        clearAllMocks()
    }

    @Nested
    inner class CreateActionItemTests {

        @Test
        fun `should create action item with all fields`() {
            every { personRepository.findByIdAndUserId(personId, userId) } returns person
            every { actionItemRepository.save(any()) } answers { firstArg() }

            val command = CreateActionItemCommand(
                userId = userId,
                personId = personId,
                title = "Follow up on project",
                description = "Check progress next week",
                ownerType = ActionItemOwnerType.PERSON,
                dueDate = LocalDate.of(2026, 5, 20)
            )

            val result = service.createActionItem(command)

            result.title shouldBe "Follow up on project"
            result.description shouldBe "Check progress next week"
            result.ownerType shouldBe ActionItemOwnerType.PERSON
            result.dueDate shouldBe LocalDate.of(2026, 5, 20)
            result.status shouldBe ActionItemStatus.OPEN
            result.userId shouldBe userId
            result.personId shouldBe personId
            verify { actionItemRepository.save(any()) }
        }

        @Test
        fun `should create action item with originating entry id`() {
            val entryId = OneOnOneEntryId.generate()
            every { personRepository.findByIdAndUserId(personId, userId) } returns person
            every { actionItemRepository.save(any()) } answers { firstArg() }

            val command = CreateActionItemCommand(
                userId = userId,
                personId = personId,
                title = "From 1:1 meeting",
                originatingEntryId = entryId
            )

            val result = service.createActionItem(command)

            result.originatingEntryId shouldBe entryId
        }

        @Test
        fun `should throw PersonNotFoundException when person not found`() {
            every { personRepository.findByIdAndUserId(personId, userId) } returns null

            val command = CreateActionItemCommand(
                userId = userId,
                personId = personId,
                title = "Task"
            )

            shouldThrow<PersonNotFoundException> {
                service.createActionItem(command)
            }
        }

        @Test
        fun `should create a person-less action item without checking person ownership`() {
            every { actionItemRepository.save(any()) } answers { firstArg() }

            val command = CreateActionItemCommand(
                userId = userId,
                personId = null,
                title = "My personal todo"
            )

            val result = service.createActionItem(command)

            result.personId shouldBe null
            result.title shouldBe "My personal todo"
            verify(exactly = 0) { personRepository.findByIdAndUserId(any(), any()) }
            verify { actionItemRepository.save(any()) }
        }
    }

    @Nested
    inner class PersonLessActionItemTests {

        private val actionItemId = ActionItemId.generate()
        private val personLessItem = ActionItem(
            id = actionItemId,
            userId = userId,
            personId = null,
            title = "My personal todo",
            status = ActionItemStatus.OPEN
        )

        @Test
        fun `should get a person-less action item by id and user`() {
            every { actionItemRepository.findByIdAndUserId(actionItemId, userId) } returns personLessItem

            val query = GetActionItemQuery(userId = userId, personId = null, actionItemId = actionItemId)
            val result = service.getActionItem(query)

            result shouldBe personLessItem
        }

        @Test
        fun `should throw when person-less action item not found`() {
            every { actionItemRepository.findByIdAndUserId(actionItemId, userId) } returns null

            val query = GetActionItemQuery(userId = userId, personId = null, actionItemId = actionItemId)

            shouldThrow<ActionItemNotFoundException> {
                service.getActionItem(query)
            }
        }

        @Test
        fun `should complete a person-less action item`() {
            every { actionItemRepository.findByIdAndUserId(actionItemId, userId) } returns personLessItem
            every { actionItemRepository.save(any()) } answers { firstArg() }

            val command = CompleteActionItemCommand(userId = userId, personId = null, actionItemId = actionItemId)
            val result = service.completeActionItem(command)

            result.status shouldBe ActionItemStatus.DONE
        }

        @Test
        fun `should cancel a person-less action item`() {
            every { actionItemRepository.findByIdAndUserId(actionItemId, userId) } returns personLessItem
            every { actionItemRepository.save(any()) } answers { firstArg() }

            val command = CancelActionItemCommand(userId = userId, personId = null, actionItemId = actionItemId)
            val result = service.cancelActionItem(command)

            result.status shouldBe ActionItemStatus.CANCELED
        }

        @Test
        fun `should delete a person-less action item`() {
            every { actionItemRepository.findByIdAndUserId(actionItemId, userId) } returns personLessItem
            every { actionItemRepository.deleteByIdAndUserId(actionItemId, userId) } returns true

            val command = DeleteActionItemCommand(userId = userId, personId = null, actionItemId = actionItemId)
            service.deleteActionItem(command)

            verify { actionItemRepository.deleteByIdAndUserId(actionItemId, userId) }
        }
    }

    @Nested
    inner class UpdateActionItemTests {

        private val actionItemId = ActionItemId.generate()
        private val existingItem = ActionItem(
            id = actionItemId,
            userId = userId,
            personId = personId,
            title = "Original title",
            description = "Original description",
            ownerType = ActionItemOwnerType.MANAGER,
            dueDate = LocalDate.of(2026, 5, 15)
        )

        @Test
        fun `should update action item fields`() {
            every { actionItemRepository.findByIdAndUserIdAndPersonId(actionItemId, userId, personId) } returns existingItem
            every { actionItemRepository.save(any()) } answers { firstArg() }

            val command = UpdateActionItemCommand(
                userId = userId,
                personId = personId,
                actionItemId = actionItemId,
                title = "Updated title",
                ownerType = ActionItemOwnerType.PERSON
            )

            val result = service.updateActionItem(command)

            result.title shouldBe "Updated title"
            result.ownerType shouldBe ActionItemOwnerType.PERSON
            result.description shouldBe "Original description" // unchanged
        }

        @Test
        fun `should throw ActionItemNotFoundException when not found`() {
            every { actionItemRepository.findByIdAndUserIdAndPersonId(actionItemId, userId, personId) } returns null

            val command = UpdateActionItemCommand(
                userId = userId,
                personId = personId,
                actionItemId = actionItemId,
                title = "Updated"
            )

            shouldThrow<ActionItemNotFoundException> {
                service.updateActionItem(command)
            }
        }
    }

    @Nested
    inner class CompleteActionItemTests {

        private val actionItemId = ActionItemId.generate()
        private val openItem = ActionItem(
            id = actionItemId,
            userId = userId,
            personId = personId,
            title = "Task to complete",
            status = ActionItemStatus.OPEN
        )

        @Test
        fun `should complete an open action item`() {
            every { actionItemRepository.findByIdAndUserIdAndPersonId(actionItemId, userId, personId) } returns openItem
            every { actionItemRepository.save(any()) } answers { firstArg() }

            val command = CompleteActionItemCommand(userId, personId, actionItemId)

            val result = service.completeActionItem(command)

            result.status shouldBe ActionItemStatus.DONE
        }

        @Test
        fun `should throw ActionItemNotFoundException when not found`() {
            every { actionItemRepository.findByIdAndUserIdAndPersonId(actionItemId, userId, personId) } returns null

            shouldThrow<ActionItemNotFoundException> {
                service.completeActionItem(CompleteActionItemCommand(userId, personId, actionItemId))
            }
        }

        @Test
        fun `should throw IllegalArgumentException when already done`() {
            val doneItem = openItem.copy(status = ActionItemStatus.DONE)
            every { actionItemRepository.findByIdAndUserIdAndPersonId(actionItemId, userId, personId) } returns doneItem

            shouldThrow<IllegalArgumentException> {
                service.completeActionItem(CompleteActionItemCommand(userId, personId, actionItemId))
            }
        }
    }

    @Nested
    inner class CancelActionItemTests {

        private val actionItemId = ActionItemId.generate()
        private val openItem = ActionItem(
            id = actionItemId,
            userId = userId,
            personId = personId,
            title = "Task to cancel",
            status = ActionItemStatus.OPEN
        )

        @Test
        fun `should cancel an open action item`() {
            every { actionItemRepository.findByIdAndUserIdAndPersonId(actionItemId, userId, personId) } returns openItem
            every { actionItemRepository.save(any()) } answers { firstArg() }

            val command = CancelActionItemCommand(userId, personId, actionItemId)

            val result = service.cancelActionItem(command)

            result.status shouldBe ActionItemStatus.CANCELED
        }

        @Test
        fun `should throw ActionItemNotFoundException when not found`() {
            every { actionItemRepository.findByIdAndUserIdAndPersonId(actionItemId, userId, personId) } returns null

            shouldThrow<ActionItemNotFoundException> {
                service.cancelActionItem(CancelActionItemCommand(userId, personId, actionItemId))
            }
        }
    }

    @Nested
    inner class DeleteActionItemTests {

        private val actionItemId = ActionItemId.generate()

        @Test
        fun `should delete action item successfully`() {
            val existing = ActionItem(
                id = actionItemId,
                userId = userId,
                personId = personId,
                title = "Test Item"
            )
            every { actionItemRepository.findByIdAndUserIdAndPersonId(actionItemId, userId, personId) } returns existing
            every { actionItemRepository.deleteByIdAndUserIdAndPersonId(actionItemId, userId, personId) } returns true

            service.deleteActionItem(DeleteActionItemCommand(userId, personId, actionItemId))

            verify { actionItemRepository.deleteByIdAndUserIdAndPersonId(actionItemId, userId, personId) }
        }

        @Test
        fun `should throw ActionItemNotFoundException when not found`() {
            every { actionItemRepository.findByIdAndUserIdAndPersonId(actionItemId, userId, personId) } returns null

            shouldThrow<ActionItemNotFoundException> {
                service.deleteActionItem(DeleteActionItemCommand(userId, personId, actionItemId))
            }
        }
    }

    @Nested
    inner class GetActionItemTests {

        private val actionItemId = ActionItemId.generate()

        @Test
        fun `should return action item when found`() {
            val item = ActionItem(
                id = actionItemId,
                userId = userId,
                personId = personId,
                title = "Test item"
            )
            every { actionItemRepository.findByIdAndUserIdAndPersonId(actionItemId, userId, personId) } returns item

            val result = service.getActionItem(GetActionItemQuery(userId, personId, actionItemId))

            result.id shouldBe actionItemId
            result.title shouldBe "Test item"
        }

        @Test
        fun `should throw ActionItemNotFoundException when not found`() {
            every { actionItemRepository.findByIdAndUserIdAndPersonId(actionItemId, userId, personId) } returns null

            shouldThrow<ActionItemNotFoundException> {
                service.getActionItem(GetActionItemQuery(userId, personId, actionItemId))
            }
        }
    }

    @Nested
    inner class ListActionItemsByPersonTests {

        @Test
        fun `should return paginated action items for person`() {
            val items = listOf(
                ActionItem(
                    id = ActionItemId.generate(),
                    userId = userId,
                    personId = personId,
                    title = "Item 1"
                )
            )
            val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "dueDate"))
            every { personRepository.findByIdAndUserId(personId, userId) } returns person
            every { actionItemRepository.findAllByUserIdAndPersonId(userId, personId, null, pageable) } returns
                PageImpl(items, pageable, 1)

            val result = service.listActionItemsByPerson(
                ListActionItemsByPersonQuery(userId, personId)
            )

            result.totalElements shouldBe 1
            result.content.size shouldBe 1
        }

        @Test
        fun `should filter by status`() {
            val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "dueDate"))
            every { personRepository.findByIdAndUserId(personId, userId) } returns person
            every { actionItemRepository.findAllByUserIdAndPersonId(userId, personId, ActionItemStatus.OPEN, pageable) } returns
                PageImpl(emptyList(), pageable, 0)

            val result = service.listActionItemsByPerson(
                ListActionItemsByPersonQuery(userId, personId, status = ActionItemStatus.OPEN)
            )

            result.totalElements shouldBe 0
            verify { actionItemRepository.findAllByUserIdAndPersonId(userId, personId, ActionItemStatus.OPEN, pageable) }
        }

        @Test
        fun `should throw PersonNotFoundException when person not found`() {
            every { personRepository.findByIdAndUserId(personId, userId) } returns null

            shouldThrow<PersonNotFoundException> {
                service.listActionItemsByPerson(ListActionItemsByPersonQuery(userId, personId))
            }
        }

        @Test
        fun `should filter by originatingEntryId`() {
            val entryId = OneOnOneEntryId.generate()
            val items = listOf(
                ActionItem(
                    id = ActionItemId.generate(),
                    userId = userId,
                    personId = personId,
                    title = "From 1:1",
                    originatingEntryId = entryId
                )
            )
            val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "dueDate"))
            every { personRepository.findByIdAndUserId(personId, userId) } returns person
            every { actionItemRepository.findAllByUserIdAndPersonIdAndOriginatingEntryId(userId, personId, entryId, pageable) } returns
                PageImpl(items, pageable, 1)

            val result = service.listActionItemsByPerson(
                ListActionItemsByPersonQuery(userId, personId, originatingEntryId = entryId)
            )

            result.totalElements shouldBe 1
            result.content[0].originatingEntryId shouldBe entryId
            verify { actionItemRepository.findAllByUserIdAndPersonIdAndOriginatingEntryId(userId, personId, entryId, pageable) }
        }
    }

    @Nested
    inner class ListAllActionItemsTests {

        @Test
        fun `should return all action items for user`() {
            val items = listOf(
                ActionItem(
                    id = ActionItemId.generate(),
                    userId = userId,
                    personId = personId,
                    title = "Item 1"
                )
            )
            val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "dueDate"))
            every { actionItemRepository.findAllByUserId(userId, null, pageable) } returns
                PageImpl(items, pageable, 1)

            val result = service.listAllActionItems(ListAllActionItemsQuery(userId))

            result.totalElements shouldBe 1
        }

        @Test
        fun `should return overdue items when overdueOnly is true`() {
            val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "dueDate"))
            every { actionItemRepository.findOverdueByUserId(userId, any(), pageable) } returns
                PageImpl(emptyList(), pageable, 0)

            val result = service.listAllActionItems(
                ListAllActionItemsQuery(userId, overdueOnly = true)
            )

            result.totalElements shouldBe 0
            verify { actionItemRepository.findOverdueByUserId(userId, any(), pageable) }
        }
    }

    @Nested
    inner class CountOpenActionItemsTests {

        @Test
        fun `should return count of open action items`() {
            every { actionItemRepository.countOpenByUserIdAndPersonId(userId, personId) } returns 5

            val result = service.countOpenActionItems(CountOpenActionItemsQuery(userId, personId))

            result shouldBe 5
        }
    }
}
