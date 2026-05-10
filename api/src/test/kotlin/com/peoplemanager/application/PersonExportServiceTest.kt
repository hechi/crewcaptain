package com.peoplemanager.application

import com.peoplemanager.application.ports.ActionItemRepository
import com.peoplemanager.application.ports.KudosRepository
import com.peoplemanager.application.ports.OneOnOneEntryRepository
import com.peoplemanager.application.ports.PdpGoalRepository
import com.peoplemanager.application.ports.PdpUpdateRepository
import com.peoplemanager.application.ports.PersonRepository
import com.peoplemanager.application.queries.ExportPersonDataQuery
import com.peoplemanager.domain.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import java.time.Instant
import java.time.LocalDate

class PersonExportServiceTest {

    private lateinit var personRepository: PersonRepository
    private lateinit var oneOnOneEntryRepository: OneOnOneEntryRepository
    private lateinit var actionItemRepository: ActionItemRepository
    private lateinit var pdpGoalRepository: PdpGoalRepository
    private lateinit var pdpUpdateRepository: PdpUpdateRepository
    private lateinit var kudosRepository: KudosRepository
    private lateinit var service: PersonExportService

    private val userId = UserId.generate()
    private val personId = PersonId.generate()

    @BeforeEach
    fun setUp() {
        personRepository = mockk()
        oneOnOneEntryRepository = mockk()
        actionItemRepository = mockk()
        pdpGoalRepository = mockk()
        pdpUpdateRepository = mockk()
        kudosRepository = mockk()
        service = PersonExportService(
            personRepository, oneOnOneEntryRepository, actionItemRepository,
            pdpGoalRepository, pdpUpdateRepository, kudosRepository
        )
    }

    private fun createTestPerson() = Person(
        id = personId,
        userId = userId,
        name = "Jane Smith",
        preferredName = "Jane",
        roleTitle = "Engineer",
        moraleStatus = MoraleStatus.GREEN
    )

    private fun setupEmptyRepositories() {
        every { oneOnOneEntryRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns
            PageImpl(emptyList())
        every { actionItemRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns
            PageImpl(emptyList())
        every { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns
            PageImpl(emptyList())
        every { kudosRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns
            PageImpl(emptyList())
    }

    @Nested
    inner class ExportPersonMarkdownTests {

        @Test
        fun `should throw PersonNotFoundException when person does not exist`() {
            every { personRepository.findByIdAndUserId(personId, userId) } returns null

            val query = ExportPersonDataQuery(userId = userId, personId = personId)

            shouldThrow<PersonNotFoundException> {
                service.exportPersonMarkdown(query)
            }
        }

        @Test
        fun `should return markdown with person profile`() {
            every { personRepository.findByIdAndUserId(personId, userId) } returns createTestPerson()
            setupEmptyRepositories()

            val query = ExportPersonDataQuery(userId = userId, personId = personId)
            val result = service.exportPersonMarkdown(query)

            result shouldContain "# Jane Smith"
            result shouldContain "## Profile"
            result shouldContain "| Name | Jane Smith |"
        }

        @Test
        fun `should include 1-1 entries in export`() {
            every { personRepository.findByIdAndUserId(personId, userId) } returns createTestPerson()
            val entry = OneOnOneEntry(
                id = OneOnOneEntryId.generate(), userId = userId, personId = personId,
                meetingDate = Instant.parse("2024-06-01T10:00:00Z"),
                notesMarkdown = "Discussed project"
            )
            every { oneOnOneEntryRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns
                PageImpl(listOf(entry))
            every { actionItemRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns
                PageImpl(emptyList())
            every { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns
                PageImpl(emptyList())
            every { kudosRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns
                PageImpl(emptyList())

            val query = ExportPersonDataQuery(userId = userId, personId = personId)
            val result = service.exportPersonMarkdown(query)

            result shouldContain "## 1:1 History"
            result shouldContain "Discussed project"
        }

        @Test
        fun `should include action items in export`() {
            every { personRepository.findByIdAndUserId(personId, userId) } returns createTestPerson()
            val item = ActionItem(
                id = ActionItemId.generate(), userId = userId, personId = personId,
                title = "Follow up on review", ownerType = ActionItemOwnerType.MANAGER,
                status = ActionItemStatus.OPEN
            )
            every { oneOnOneEntryRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns
                PageImpl(emptyList())
            every { actionItemRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns
                PageImpl(listOf(item))
            every { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns
                PageImpl(emptyList())
            every { kudosRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns
                PageImpl(emptyList())

            val query = ExportPersonDataQuery(userId = userId, personId = personId)
            val result = service.exportPersonMarkdown(query)

            result shouldContain "## Action Items"
            result shouldContain "Follow up on review"
        }

        @Test
        fun `should include PDP goals with updates in export`() {
            every { personRepository.findByIdAndUserId(personId, userId) } returns createTestPerson()
            val goalId = PdpGoalId.generate()
            val goal = PdpGoal(
                id = goalId, userId = userId, personId = personId,
                title = "Learn Rust", status = PdpGoalStatus.ACTIVE
            )
            val update = PdpUpdate(
                id = PdpUpdateId.generate(), goalId = goalId, userId = userId,
                textMarkdown = "Completed chapter 1"
            )
            every { oneOnOneEntryRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns
                PageImpl(emptyList())
            every { actionItemRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns
                PageImpl(emptyList())
            every { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns
                PageImpl(listOf(goal))
            every { pdpUpdateRepository.findAllByGoalIdAndUserId(goalId, userId, any()) } returns
                PageImpl(listOf(update))
            every { kudosRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns
                PageImpl(emptyList())

            val query = ExportPersonDataQuery(userId = userId, personId = personId)
            val result = service.exportPersonMarkdown(query)

            result shouldContain "## PDP Goals"
            result shouldContain "Learn Rust"
            result shouldContain "Completed chapter 1"
        }

        @Test
        fun `should include kudos in export`() {
            every { personRepository.findByIdAndUserId(personId, userId) } returns createTestPerson()
            val kudos = Kudos(
                id = KudosId.generate(), userId = userId, personId = personId,
                date = LocalDate.of(2024, 5, 1), text = "Excellent teamwork!"
            )
            every { oneOnOneEntryRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns
                PageImpl(emptyList())
            every { actionItemRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns
                PageImpl(emptyList())
            every { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns
                PageImpl(emptyList())
            every { kudosRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns
                PageImpl(listOf(kudos))

            val query = ExportPersonDataQuery(userId = userId, personId = personId)
            val result = service.exportPersonMarkdown(query)

            result shouldContain "## Kudos"
            result shouldContain "Excellent teamwork!"
        }

        @Test
        fun `should enforce userId scoping on all repository calls`() {
            every { personRepository.findByIdAndUserId(personId, userId) } returns createTestPerson()
            setupEmptyRepositories()

            val query = ExportPersonDataQuery(userId = userId, personId = personId)
            service.exportPersonMarkdown(query)

            verify { personRepository.findByIdAndUserId(personId, userId) }
            verify { oneOnOneEntryRepository.findAllByUserIdAndPersonId(userId, personId, any()) }
            verify { actionItemRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) }
            verify { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) }
            verify { kudosRepository.findAllByUserIdAndPersonId(userId, personId, any()) }
        }
    }

    @Nested
    inner class DateRangeFilterTests {

        @Test
        fun `should filter 1-1 entries by date range`() {
            every { personRepository.findByIdAndUserId(personId, userId) } returns createTestPerson()
            val entryInRange = OneOnOneEntry(
                id = OneOnOneEntryId.generate(), userId = userId, personId = personId,
                meetingDate = Instant.parse("2024-03-15T10:00:00Z"),
                notesMarkdown = "In range"
            )
            val entryOutOfRange = OneOnOneEntry(
                id = OneOnOneEntryId.generate(), userId = userId, personId = personId,
                meetingDate = Instant.parse("2024-01-01T10:00:00Z"),
                notesMarkdown = "Out of range"
            )
            every { oneOnOneEntryRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns
                PageImpl(listOf(entryInRange, entryOutOfRange))
            every { actionItemRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns
                PageImpl(emptyList())
            every { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns
                PageImpl(emptyList())
            every { kudosRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns
                PageImpl(emptyList())

            val query = ExportPersonDataQuery(
                userId = userId, personId = personId,
                dateFrom = LocalDate.of(2024, 3, 1),
                dateTo = LocalDate.of(2024, 3, 31)
            )
            val result = service.exportPersonMarkdown(query)

            result shouldContain "In range"
            result shouldNotContain "Out of range"
        }

        @Test
        fun `should filter action items by date range`() {
            every { personRepository.findByIdAndUserId(personId, userId) } returns createTestPerson()
            val itemInRange = ActionItem(
                id = ActionItemId.generate(), userId = userId, personId = personId,
                title = "In range item", ownerType = ActionItemOwnerType.MANAGER,
                status = ActionItemStatus.OPEN,
                createdAt = Instant.parse("2024-03-15T10:00:00Z")
            )
            val itemOutOfRange = ActionItem(
                id = ActionItemId.generate(), userId = userId, personId = personId,
                title = "Out of range item", ownerType = ActionItemOwnerType.MANAGER,
                status = ActionItemStatus.OPEN,
                createdAt = Instant.parse("2024-01-01T10:00:00Z")
            )
            every { oneOnOneEntryRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns
                PageImpl(emptyList())
            every { actionItemRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns
                PageImpl(listOf(itemInRange, itemOutOfRange))
            every { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns
                PageImpl(emptyList())
            every { kudosRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns
                PageImpl(emptyList())

            val query = ExportPersonDataQuery(
                userId = userId, personId = personId,
                dateFrom = LocalDate.of(2024, 3, 1),
                dateTo = LocalDate.of(2024, 3, 31)
            )
            val result = service.exportPersonMarkdown(query)

            result shouldContain "In range item"
            result shouldNotContain "Out of range item"
        }

        @Test
        fun `should filter kudos by date range`() {
            every { personRepository.findByIdAndUserId(personId, userId) } returns createTestPerson()
            val kudosInRange = Kudos(
                id = KudosId.generate(), userId = userId, personId = personId,
                date = LocalDate.of(2024, 3, 15), text = "In range kudos"
            )
            val kudosOutOfRange = Kudos(
                id = KudosId.generate(), userId = userId, personId = personId,
                date = LocalDate.of(2024, 1, 1), text = "Out of range kudos"
            )
            every { oneOnOneEntryRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns
                PageImpl(emptyList())
            every { actionItemRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns
                PageImpl(emptyList())
            every { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns
                PageImpl(emptyList())
            every { kudosRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns
                PageImpl(listOf(kudosInRange, kudosOutOfRange))

            val query = ExportPersonDataQuery(
                userId = userId, personId = personId,
                dateFrom = LocalDate.of(2024, 3, 1),
                dateTo = LocalDate.of(2024, 3, 31)
            )
            val result = service.exportPersonMarkdown(query)

            result shouldContain "In range kudos"
            result shouldNotContain "Out of range kudos"
        }

        @Test
        fun `should include all data when no date range specified`() {
            every { personRepository.findByIdAndUserId(personId, userId) } returns createTestPerson()
            val entry = OneOnOneEntry(
                id = OneOnOneEntryId.generate(), userId = userId, personId = personId,
                meetingDate = Instant.parse("2020-01-01T10:00:00Z"),
                notesMarkdown = "Old entry"
            )
            every { oneOnOneEntryRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns
                PageImpl(listOf(entry))
            every { actionItemRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns
                PageImpl(emptyList())
            every { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns
                PageImpl(emptyList())
            every { kudosRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns
                PageImpl(emptyList())

            val query = ExportPersonDataQuery(userId = userId, personId = personId)
            val result = service.exportPersonMarkdown(query)

            result shouldContain "Old entry"
        }
    }
}
