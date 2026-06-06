package com.peoplemanager.application

import com.peoplemanager.application.commands.SnoozeActionItemCommand
import com.peoplemanager.application.port.output.ActionItemRepository
import com.peoplemanager.application.port.output.OneOnOneEntryRepository
import com.peoplemanager.application.port.output.OneOnOneSeriesRepository
import com.peoplemanager.application.port.output.PersonRepository
import com.peoplemanager.application.queries.GetTriageQueueQuery
import com.peoplemanager.application.queries.OwnerScope
import com.peoplemanager.domain.*
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class TriageQueueServiceTest {

    private val personRepository: PersonRepository = mockk()
    private val actionItemRepository: ActionItemRepository = mockk()
    private val oneOnOneSeriesRepository: OneOnOneSeriesRepository = mockk()
    private val oneOnOneEntryRepository: OneOnOneEntryRepository = mockk()

    private val service = TriageQueueService(
        personRepository, actionItemRepository, oneOnOneSeriesRepository, oneOnOneEntryRepository
    )

    private val userId = UserId.generate()
    private val personId = PersonId.generate()
    private val workspaceId = WorkspaceId.generate()

    private val person = Person(
        id = personId,
        userId = userId,
        name = "Alice Smith",
        workspaceId = workspaceId,
        startDate = LocalDate.now(ZoneOffset.UTC).minusYears(2).plusDays(10)
    )

    @BeforeEach
    fun setup() {
        every { personRepository.findAllByUserIdUnpaged(userId) } returns listOf(person)
    }

    @Nested
    inner class OverdueActionItemsTests {

        @Test
        fun `should return overdue action items`() {
            val overdueItem = ActionItem(
                id = ActionItemId.generate(),
                userId = userId,
                personId = personId,
                title = "Overdue task",
                dueDate = LocalDate.now(ZoneOffset.UTC).minusDays(3),
                status = ActionItemStatus.OPEN
            )
            every { actionItemRepository.findOverdueByUserId(userId, any(), any()) } returns
                PageImpl(listOf(overdueItem))
            every { actionItemRepository.findDueSoonByUserId(userId, any(), any()) } returns emptyList()
            every { oneOnOneSeriesRepository.findAllByUserId(userId) } returns emptyList()

            val query = GetTriageQueueQuery(userId = userId)
            val result = service.getTriageQueue(query)

            result shouldHaveSize 2 // 1 overdue + 1 anniversary
            val overdueResult = result.first { it.type == TriageItemType.ACTION_ITEM_OVERDUE }
            overdueResult.title shouldBe "Overdue task"
            overdueResult.criticality shouldBe TriageCriticality.OVERDUE
            overdueResult.personName shouldBe "Alice Smith"
            overdueResult.daysOverdue shouldNotBe null
        }

        @Test
        fun `should filter snoozed items from overdue results`() {
            val snoozedItem = ActionItem(
                id = ActionItemId.generate(),
                userId = userId,
                personId = personId,
                title = "Snoozed task",
                dueDate = LocalDate.now(ZoneOffset.UTC).minusDays(3),
                status = ActionItemStatus.OPEN,
                snoozedUntil = Instant.now().plus(1, ChronoUnit.DAYS)
            )
            every { actionItemRepository.findOverdueByUserId(userId, any(), any()) } returns
                PageImpl(listOf(snoozedItem))
            every { actionItemRepository.findDueSoonByUserId(userId, any(), any()) } returns emptyList()
            every { oneOnOneSeriesRepository.findAllByUserId(userId) } returns emptyList()

            val query = GetTriageQueueQuery(userId = userId)
            val result = service.getTriageQueue(query)

            result.none { it.type == TriageItemType.ACTION_ITEM_OVERDUE } shouldBe true
        }

        @Test
        fun `should filter by MINE scope returning only MANAGER owned items`() {
            val managerItem = ActionItem(
                id = ActionItemId.generate(), userId = userId, personId = personId,
                title = "Manager task", dueDate = LocalDate.now(ZoneOffset.UTC).minusDays(1),
                status = ActionItemStatus.OPEN, ownerType = ActionItemOwnerType.MANAGER
            )
            val personItem = ActionItem(
                id = ActionItemId.generate(), userId = userId, personId = personId,
                title = "Person task", dueDate = LocalDate.now(ZoneOffset.UTC).minusDays(1),
                status = ActionItemStatus.OPEN, ownerType = ActionItemOwnerType.PERSON
            )
            every { actionItemRepository.findOverdueByUserId(userId, any(), any()) } returns
                PageImpl(listOf(managerItem, personItem))
            every { actionItemRepository.findDueSoonByUserId(userId, any(), any()) } returns emptyList()
            every { oneOnOneSeriesRepository.findAllByUserId(userId) } returns emptyList()

            val query = GetTriageQueueQuery(userId = userId, ownerScope = OwnerScope.MINE)
            val result = service.getTriageQueue(query)

            val overdueItems = result.filter { it.type == TriageItemType.ACTION_ITEM_OVERDUE }
            overdueItems shouldHaveSize 1
            overdueItems[0].title shouldBe "Manager task"
        }
    }

    @Nested
    inner class DueSoonTests {

        @Test
        fun `should return due soon action items`() {
            val dueSoonItem = ActionItem(
                id = ActionItemId.generate(), userId = userId, personId = personId,
                title = "Due soon task",
                dueDate = LocalDate.now(ZoneOffset.UTC).plusDays(2),
                status = ActionItemStatus.OPEN
            )
            every { actionItemRepository.findOverdueByUserId(userId, any(), any()) } returns
                PageImpl(emptyList())
            every { actionItemRepository.findDueSoonByUserId(userId, any(), any()) } returns
                listOf(dueSoonItem)
            every { oneOnOneSeriesRepository.findAllByUserId(userId) } returns emptyList()

            val query = GetTriageQueueQuery(userId = userId)
            val result = service.getTriageQueue(query)

            val dueSoonResults = result.filter { it.type == TriageItemType.ACTION_ITEM_DUE_SOON }
            dueSoonResults shouldHaveSize 1
            dueSoonResults[0].title shouldBe "Due soon task"
            dueSoonResults[0].criticality shouldBe TriageCriticality.DUE_SOON
        }
    }

    @Nested
    inner class StaleOneOnOneTests {

        @Test
        fun `should return stale 1-1 reminders`() {
            val series = OneOnOneSeries(
                id = OneOnOneSeriesId.generate(),
                userId = userId,
                personId = personId,
                cadenceType = CadenceType.WEEKLY
            )
            every { actionItemRepository.findOverdueByUserId(userId, any(), any()) } returns
                PageImpl(emptyList())
            every { actionItemRepository.findDueSoonByUserId(userId, any(), any()) } returns emptyList()
            every { oneOnOneSeriesRepository.findAllByUserId(userId) } returns listOf(series)
            every { oneOnOneEntryRepository.findLatestMeetingDate(userId, personId) } returns
                Instant.now().minus(14, ChronoUnit.DAYS)

            val query = GetTriageQueueQuery(userId = userId)
            val result = service.getTriageQueue(query)

            val staleItems = result.filter { it.type == TriageItemType.STALE_ONE_ON_ONE }
            staleItems shouldHaveSize 1
            staleItems[0].criticality shouldBe TriageCriticality.STALE
            staleItems[0].personName shouldBe "Alice Smith"
        }

        @Test
        fun `should not return stale reminder if on schedule`() {
            val series = OneOnOneSeries(
                id = OneOnOneSeriesId.generate(),
                userId = userId,
                personId = personId,
                cadenceType = CadenceType.WEEKLY
            )
            every { actionItemRepository.findOverdueByUserId(userId, any(), any()) } returns
                PageImpl(emptyList())
            every { actionItemRepository.findDueSoonByUserId(userId, any(), any()) } returns emptyList()
            every { oneOnOneSeriesRepository.findAllByUserId(userId) } returns listOf(series)
            every { oneOnOneEntryRepository.findLatestMeetingDate(userId, personId) } returns
                Instant.now().minus(3, ChronoUnit.DAYS)

            val query = GetTriageQueueQuery(userId = userId)
            val result = service.getTriageQueue(query)

            val staleItems = result.filter { it.type == TriageItemType.STALE_ONE_ON_ONE }
            staleItems.shouldBeEmpty()
        }
    }

    @Nested
    inner class AnniversaryTests {

        @Test
        fun `should return upcoming anniversary`() {
            every { actionItemRepository.findOverdueByUserId(userId, any(), any()) } returns
                PageImpl(emptyList())
            every { actionItemRepository.findDueSoonByUserId(userId, any(), any()) } returns emptyList()
            every { oneOnOneSeriesRepository.findAllByUserId(userId) } returns emptyList()

            val query = GetTriageQueueQuery(userId = userId)
            val result = service.getTriageQueue(query)

            val anniversaryItems = result.filter { it.type == TriageItemType.UPCOMING_ANNIVERSARY }
            anniversaryItems shouldHaveSize 1
            anniversaryItems[0].criticality shouldBe TriageCriticality.INFORMATIONAL
            anniversaryItems[0].personName shouldBe "Alice Smith"
        }
    }

    @Nested
    inner class FilteringTests {

        @Test
        fun `should filter by item type`() {
            every { actionItemRepository.findOverdueByUserId(userId, any(), any()) } returns
                PageImpl(emptyList())

            val query = GetTriageQueueQuery(
                userId = userId,
                itemType = TriageItemType.ACTION_ITEM_OVERDUE
            )
            val result = service.getTriageQueue(query)

            result.all { it.type == TriageItemType.ACTION_ITEM_OVERDUE } shouldBe true
        }

        @Test
        fun `should filter by workspace`() {
            val otherWorkspaceId = WorkspaceId.generate()
            val otherPerson = Person(
                id = PersonId.generate(), userId = userId,
                name = "Other Person", workspaceId = otherWorkspaceId
            )
            every { personRepository.findAllByUserIdUnpaged(userId) } returns listOf(person, otherPerson)
            every { actionItemRepository.findOverdueByUserId(userId, any(), any()) } returns
                PageImpl(emptyList())
            every { actionItemRepository.findDueSoonByUserId(userId, any(), any()) } returns emptyList()
            every { oneOnOneSeriesRepository.findAllByUserId(userId) } returns emptyList()

            val query = GetTriageQueueQuery(
                userId = userId,
                workspaceIds = listOf(workspaceId)
            )
            val result = service.getTriageQueue(query)

            result.all { it.workspaceId == workspaceId } shouldBe true
        }

        @Test
        fun `should filter by person`() {
            val otherPerson = Person(
                id = PersonId.generate(), userId = userId, name = "Other Person"
            )
            every { personRepository.findAllByUserIdUnpaged(userId) } returns listOf(person, otherPerson)
            every { actionItemRepository.findOverdueByUserId(userId, any(), any()) } returns
                PageImpl(emptyList())
            every { actionItemRepository.findDueSoonByUserId(userId, any(), any()) } returns emptyList()
            every { oneOnOneSeriesRepository.findAllByUserId(userId) } returns emptyList()

            val query = GetTriageQueueQuery(
                userId = userId,
                personId = personId
            )
            val result = service.getTriageQueue(query)

            result.all { it.personId == personId } shouldBe true
        }
    }

    @Nested
    inner class SortingTests {

        @Test
        fun `should sort by criticality then due date`() {
            val overdueItem = ActionItem(
                id = ActionItemId.generate(), userId = userId, personId = personId,
                title = "Overdue", dueDate = LocalDate.now(ZoneOffset.UTC).minusDays(5),
                status = ActionItemStatus.OPEN
            )
            val dueSoonItem = ActionItem(
                id = ActionItemId.generate(), userId = userId, personId = personId,
                title = "Due soon", dueDate = LocalDate.now(ZoneOffset.UTC).plusDays(2),
                status = ActionItemStatus.OPEN
            )
            every { actionItemRepository.findOverdueByUserId(userId, any(), any()) } returns
                PageImpl(listOf(overdueItem))
            every { actionItemRepository.findDueSoonByUserId(userId, any(), any()) } returns
                listOf(dueSoonItem)
            every { oneOnOneSeriesRepository.findAllByUserId(userId) } returns emptyList()

            val query = GetTriageQueueQuery(userId = userId)
            val result = service.getTriageQueue(query)

            // Overdue should come before due soon
            val overdueIdx = result.indexOfFirst { it.type == TriageItemType.ACTION_ITEM_OVERDUE }
            val dueSoonIdx = result.indexOfFirst { it.type == TriageItemType.ACTION_ITEM_DUE_SOON }
            val anniversaryIdx = result.indexOfFirst { it.type == TriageItemType.UPCOMING_ANNIVERSARY }
            (overdueIdx < dueSoonIdx) shouldBe true
            (dueSoonIdx < anniversaryIdx) shouldBe true
        }
    }

    @Nested
    inner class SnoozeTests {

        @Test
        fun `should snooze an action item`() {
            val item = ActionItem(
                id = ActionItemId.generate(), userId = userId, personId = personId,
                title = "To snooze", status = ActionItemStatus.OPEN
            )
            every { actionItemRepository.findByIdAndUserIdAndPersonId(item.id, userId, personId) } returns item
            val savedSlot = slot<ActionItem>()
            every { actionItemRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

            val snoozedUntil = Instant.now().plus(3, ChronoUnit.DAYS)
            val command = SnoozeActionItemCommand(userId, personId, item.id, snoozedUntil)
            val result = service.snoozeActionItem(command)

            result.snoozedUntil shouldBe snoozedUntil
        }

        @Test
        fun `should throw when action item not found for snooze`() {
            val fakeId = ActionItemId.generate()
            every { actionItemRepository.findByIdAndUserIdAndPersonId(fakeId, userId, personId) } returns null

            val command = SnoozeActionItemCommand(userId, personId, fakeId, Instant.now())

            assertThrows<ActionItemNotFoundException> {
                service.snoozeActionItem(command)
            }
        }
    }
}
