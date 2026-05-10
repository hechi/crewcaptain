package com.peoplemanager.application

import com.peoplemanager.application.commands.AgendaItemInput
import com.peoplemanager.application.commands.CreateOneOnOneEntryCommand
import com.peoplemanager.application.commands.DeleteOneOnOneEntryCommand
import com.peoplemanager.application.commands.UpdateOneOnOneEntryCommand
import com.peoplemanager.application.commands.UpsertOneOnOneSeriesCommand
import com.peoplemanager.application.ports.OneOnOneEntryRepository
import com.peoplemanager.application.ports.OneOnOneSeriesRepository
import com.peoplemanager.application.ports.PersonRepository
import com.peoplemanager.application.queries.GetLastOneOnOneDateQuery
import com.peoplemanager.application.queries.GetOneOnOneEntryQuery
import com.peoplemanager.application.queries.GetOneOnOneSeriesQuery
import com.peoplemanager.application.queries.ListOneOnOneEntriesQuery
import com.peoplemanager.domain.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.time.Instant

class OneOnOneServiceTest {

    private val personRepository = mockk<PersonRepository>()
    private val seriesRepository = mockk<OneOnOneSeriesRepository>()
    private val entryRepository = mockk<OneOnOneEntryRepository>()
    private val auditLogService = mockk<AuditLogService>(relaxed = true)

    private val service = OneOnOneService(personRepository, seriesRepository, entryRepository, auditLogService)

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
    inner class UpsertSeriesTests {

        @Test
        fun `should create new series when none exists`() {
            every { personRepository.findByIdAndUserId(personId, userId) } returns person
            every { seriesRepository.findByUserIdAndPersonId(userId, personId) } returns null
            every { seriesRepository.save(any()) } answers { firstArg() }

            val command = UpsertOneOnOneSeriesCommand(
                userId = userId,
                personId = personId,
                cadenceType = CadenceType.WEEKLY,
                templateMarkdown = "## Notes"
            )

            val result = service.upsertSeries(command)

            result.cadenceType shouldBe CadenceType.WEEKLY
            result.templateMarkdown shouldBe "## Notes"
            result.userId shouldBe userId
            result.personId shouldBe personId
            verify { seriesRepository.save(any()) }
        }

        @Test
        fun `should update existing series`() {
            val existingSeries = OneOnOneSeries(
                id = OneOnOneSeriesId.generate(),
                userId = userId,
                personId = personId,
                cadenceType = CadenceType.WEEKLY
            )
            every { personRepository.findByIdAndUserId(personId, userId) } returns person
            every { seriesRepository.findByUserIdAndPersonId(userId, personId) } returns existingSeries
            every { seriesRepository.save(any()) } answers { firstArg() }

            val command = UpsertOneOnOneSeriesCommand(
                userId = userId,
                personId = personId,
                cadenceType = CadenceType.BIWEEKLY,
                templateMarkdown = "Updated template"
            )

            val result = service.upsertSeries(command)

            result.id shouldBe existingSeries.id
            result.cadenceType shouldBe CadenceType.BIWEEKLY
            result.templateMarkdown shouldBe "Updated template"
        }

        @Test
        fun `should throw PersonNotFoundException when person not found`() {
            every { personRepository.findByIdAndUserId(personId, userId) } returns null

            val command = UpsertOneOnOneSeriesCommand(
                userId = userId,
                personId = personId,
                cadenceType = CadenceType.WEEKLY
            )

            shouldThrow<PersonNotFoundException> {
                service.upsertSeries(command)
            }
        }
    }

    @Nested
    inner class CreateEntryTests {

        @Test
        fun `should create entry with provided notes`() {
            every { personRepository.findByIdAndUserId(personId, userId) } returns person
            every { entryRepository.save(any()) } answers { firstArg() }

            val command = CreateOneOnOneEntryCommand(
                userId = userId,
                personId = personId,
                meetingDate = Instant.parse("2025-05-08T14:00:00Z"),
                notesMarkdown = "Meeting notes",
                outcomesMarkdown = "Outcomes"
            )

            val result = service.createEntry(command)

            result.notesMarkdown shouldBe "Meeting notes"
            result.outcomesMarkdown shouldBe "Outcomes"
            result.userId shouldBe userId
            result.personId shouldBe personId
        }

        @Test
        fun `should prefill notes from template when notes not provided`() {
            val series = OneOnOneSeries(
                id = OneOnOneSeriesId.generate(),
                userId = userId,
                personId = personId,
                cadenceType = CadenceType.WEEKLY,
                templateMarkdown = "## Template Content"
            )
            every { personRepository.findByIdAndUserId(personId, userId) } returns person
            every { seriesRepository.findByUserIdAndPersonId(userId, personId) } returns series
            every { entryRepository.save(any()) } answers { firstArg() }

            val command = CreateOneOnOneEntryCommand(
                userId = userId,
                personId = personId,
                meetingDate = Instant.parse("2025-05-08T14:00:00Z"),
                notesMarkdown = null
            )

            val result = service.createEntry(command)

            result.notesMarkdown shouldBe "## Template Content"
        }

        @Test
        fun `should NOT apply template when notes explicitly provided`() {
            val series = OneOnOneSeries(
                id = OneOnOneSeriesId.generate(),
                userId = userId,
                personId = personId,
                cadenceType = CadenceType.WEEKLY,
                templateMarkdown = "## Template Content"
            )
            every { personRepository.findByIdAndUserId(personId, userId) } returns person
            every { entryRepository.save(any()) } answers { firstArg() }

            val command = CreateOneOnOneEntryCommand(
                userId = userId,
                personId = personId,
                meetingDate = Instant.parse("2025-05-08T14:00:00Z"),
                notesMarkdown = "My own notes"
            )

            val result = service.createEntry(command)

            result.notesMarkdown shouldBe "My own notes"
            // Should not even query for series when notes are provided
            verify(exactly = 0) { seriesRepository.findByUserIdAndPersonId(any(), any()) }
        }

        @Test
        fun `should create entry with agenda items`() {
            every { personRepository.findByIdAndUserId(personId, userId) } returns person
            every { entryRepository.save(any()) } answers { firstArg() }

            val command = CreateOneOnOneEntryCommand(
                userId = userId,
                personId = personId,
                meetingDate = Instant.parse("2025-05-08T14:00:00Z"),
                agendaItems = listOf(
                    AgendaItemInput("Item 1", false),
                    AgendaItemInput("Item 2", true)
                ),
                notesMarkdown = "Notes"
            )

            val result = service.createEntry(command)

            result.agendaItems.size shouldBe 2
            result.agendaItems[0].text shouldBe "Item 1"
            result.agendaItems[0].checked shouldBe false
            result.agendaItems[0].displayOrder shouldBe 0
            result.agendaItems[1].text shouldBe "Item 2"
            result.agendaItems[1].checked shouldBe true
            result.agendaItems[1].displayOrder shouldBe 1
        }

        @Test
        fun `should create entry with sensitive flag`() {
            every { personRepository.findByIdAndUserId(personId, userId) } returns person
            every { entryRepository.save(any()) } answers { firstArg() }

            val command = CreateOneOnOneEntryCommand(
                userId = userId,
                personId = personId,
                meetingDate = Instant.parse("2025-05-08T14:00:00Z"),
                notesMarkdown = "Sensitive content",
                sensitive = true
            )

            val result = service.createEntry(command)

            result.sensitive shouldBe true
        }

        @Test
        fun `should throw PersonNotFoundException when person not found`() {
            every { personRepository.findByIdAndUserId(personId, userId) } returns null

            val command = CreateOneOnOneEntryCommand(
                userId = userId,
                personId = personId,
                meetingDate = Instant.now(),
                notesMarkdown = "Notes"
            )

            shouldThrow<PersonNotFoundException> {
                service.createEntry(command)
            }
        }
    }

    @Nested
    inner class UpdateEntryTests {

        private val entryId = OneOnOneEntryId.generate()
        private val existingEntry = OneOnOneEntry(
            id = entryId,
            userId = userId,
            personId = personId,
            meetingDate = Instant.parse("2025-05-08T14:00:00Z"),
            notesMarkdown = "Original notes",
            outcomesMarkdown = "Original outcomes",
            sensitive = false
        )

        @Test
        fun `should update entry fields`() {
            every { entryRepository.findByIdAndUserIdAndPersonId(entryId, userId, personId) } returns existingEntry
            every { personRepository.findByIdAndUserId(personId, userId) } returns person
            every { entryRepository.save(any()) } answers { firstArg() }

            val command = UpdateOneOnOneEntryCommand(
                userId = userId,
                personId = personId,
                entryId = entryId,
                notesMarkdown = "Updated notes",
                sensitive = true
            )

            val result = service.updateEntry(command)

            result.notesMarkdown shouldBe "Updated notes"
            result.sensitive shouldBe true
            result.outcomesMarkdown shouldBe "Original outcomes" // unchanged
        }

        @Test
        fun `should throw OneOnOneEntryNotFoundException when entry not found`() {
            every { entryRepository.findByIdAndUserIdAndPersonId(entryId, userId, personId) } returns null

            val command = UpdateOneOnOneEntryCommand(
                userId = userId,
                personId = personId,
                entryId = entryId,
                notesMarkdown = "Updated"
            )

            shouldThrow<OneOnOneEntryNotFoundException> {
                service.updateEntry(command)
            }
        }

        @Test
        fun `should update agenda items`() {
            every { entryRepository.findByIdAndUserIdAndPersonId(entryId, userId, personId) } returns existingEntry
            every { personRepository.findByIdAndUserId(personId, userId) } returns person
            every { entryRepository.save(any()) } answers { firstArg() }

            val command = UpdateOneOnOneEntryCommand(
                userId = userId,
                personId = personId,
                entryId = entryId,
                agendaItems = listOf(AgendaItemInput("New item", true))
            )

            val result = service.updateEntry(command)

            result.agendaItems.size shouldBe 1
            result.agendaItems[0].text shouldBe "New item"
            result.agendaItems[0].checked shouldBe true
        }
    }

    @Nested
    inner class DeleteEntryTests {

        private val entryId = OneOnOneEntryId.generate()

        @Test
        fun `should delete entry successfully`() {
            every { personRepository.findByIdAndUserId(personId, userId) } returns person
            every { entryRepository.deleteByIdAndUserIdAndPersonId(entryId, userId, personId) } returns true

            service.deleteEntry(DeleteOneOnOneEntryCommand(userId, personId, entryId))

            verify { entryRepository.deleteByIdAndUserIdAndPersonId(entryId, userId, personId) }
        }

        @Test
        fun `should throw OneOnOneEntryNotFoundException when entry not found`() {
            every { personRepository.findByIdAndUserId(personId, userId) } returns person
            every { entryRepository.deleteByIdAndUserIdAndPersonId(entryId, userId, personId) } returns false

            shouldThrow<OneOnOneEntryNotFoundException> {
                service.deleteEntry(DeleteOneOnOneEntryCommand(userId, personId, entryId))
            }
        }
    }

    @Nested
    inner class GetSeriesTests {

        @Test
        fun `should return series when exists`() {
            val series = OneOnOneSeries(
                id = OneOnOneSeriesId.generate(),
                userId = userId,
                personId = personId,
                cadenceType = CadenceType.MONTHLY
            )
            every { personRepository.findByIdAndUserId(personId, userId) } returns person
            every { seriesRepository.findByUserIdAndPersonId(userId, personId) } returns series

            val result = service.getSeries(GetOneOnOneSeriesQuery(userId, personId))

            result shouldNotBe null
            result!!.cadenceType shouldBe CadenceType.MONTHLY
        }

        @Test
        fun `should return null when no series exists`() {
            every { personRepository.findByIdAndUserId(personId, userId) } returns person
            every { seriesRepository.findByUserIdAndPersonId(userId, personId) } returns null

            val result = service.getSeries(GetOneOnOneSeriesQuery(userId, personId))

            result shouldBe null
        }

        @Test
        fun `should throw PersonNotFoundException when person not found`() {
            every { personRepository.findByIdAndUserId(personId, userId) } returns null

            shouldThrow<PersonNotFoundException> {
                service.getSeries(GetOneOnOneSeriesQuery(userId, personId))
            }
        }
    }

    @Nested
    inner class GetEntryTests {

        private val entryId = OneOnOneEntryId.generate()

        @Test
        fun `should return entry when found`() {
            val entry = OneOnOneEntry(
                id = entryId,
                userId = userId,
                personId = personId,
                meetingDate = Instant.now(),
                notesMarkdown = "Notes"
            )
            every { entryRepository.findByIdAndUserIdAndPersonId(entryId, userId, personId) } returns entry

            val result = service.getEntry(GetOneOnOneEntryQuery(userId, personId, entryId))

            result.id shouldBe entryId
            result.notesMarkdown shouldBe "Notes"
        }

        @Test
        fun `should throw OneOnOneEntryNotFoundException when not found`() {
            every { entryRepository.findByIdAndUserIdAndPersonId(entryId, userId, personId) } returns null

            shouldThrow<OneOnOneEntryNotFoundException> {
                service.getEntry(GetOneOnOneEntryQuery(userId, personId, entryId))
            }
        }
    }

    @Nested
    inner class ListEntriesTests {

        @Test
        fun `should return paginated entries`() {
            val entries = listOf(
                OneOnOneEntry(
                    id = OneOnOneEntryId.generate(),
                    userId = userId,
                    personId = personId,
                    meetingDate = Instant.now()
                )
            )
            val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "meetingDate"))
            every { personRepository.findByIdAndUserId(personId, userId) } returns person
            every { entryRepository.findAllByUserIdAndPersonId(userId, personId, pageable) } returns
                PageImpl(entries, pageable, 1)

            val result = service.listEntries(ListOneOnOneEntriesQuery(userId, personId, 0, 20))

            result.totalElements shouldBe 1
            result.content.size shouldBe 1
        }

        @Test
        fun `should throw PersonNotFoundException when person not found`() {
            every { personRepository.findByIdAndUserId(personId, userId) } returns null

            shouldThrow<PersonNotFoundException> {
                service.listEntries(ListOneOnOneEntriesQuery(userId, personId))
            }
        }
    }

    @Nested
    inner class GetLastOneOnOneDateTests {

        @Test
        fun `should return latest meeting date`() {
            val date = Instant.parse("2025-05-08T14:00:00Z")
            every { entryRepository.findLatestMeetingDate(userId, personId) } returns date

            val result = service.getLastOneOnOneDate(GetLastOneOnOneDateQuery(userId, personId))

            result shouldBe date
        }

        @Test
        fun `should return null when no entries exist`() {
            every { entryRepository.findLatestMeetingDate(userId, personId) } returns null

            val result = service.getLastOneOnOneDate(GetLastOneOnOneDateQuery(userId, personId))

            result shouldBe null
        }
    }
}
