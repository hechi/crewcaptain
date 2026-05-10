package com.peoplemanager.application

import com.peoplemanager.application.commands.*
import com.peoplemanager.application.ports.OneOnOneEntryRepository
import com.peoplemanager.application.ports.PersonRepository
import com.peoplemanager.application.ports.QuickNoteRepository
import com.peoplemanager.application.queries.GetQuickNoteQuery
import com.peoplemanager.application.queries.ListQuickNotesQuery
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

class QuickNoteServiceTest {

    private val quickNoteRepository = mockk<QuickNoteRepository>()
    private val personRepository = mockk<PersonRepository>()
    private val oneOnOneEntryRepository = mockk<OneOnOneEntryRepository>()

    private val service = QuickNoteService(quickNoteRepository, personRepository, oneOnOneEntryRepository)

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
    inner class CreateQuickNoteTests {

        @Test
        fun `should create quick note without person`() {
            every { quickNoteRepository.save(any()) } answers { firstArg() }

            val command = CreateQuickNoteCommand(
                userId = userId,
                text = "Remember to follow up"
            )

            val result = service.createQuickNote(command)

            result.text shouldBe "Remember to follow up"
            result.userId shouldBe userId
            result.personId shouldBe null
            result.status shouldBe QuickNoteStatus.INBOX
            result.sensitive shouldBe false

            verify { quickNoteRepository.save(any()) }
        }

        @Test
        fun `should create quick note with person`() {
            every { personRepository.findByIdAndUserId(personId, userId) } returns person
            every { quickNoteRepository.save(any()) } answers { firstArg() }

            val command = CreateQuickNoteCommand(
                userId = userId,
                personId = personId,
                text = "Discuss promotion timeline"
            )

            val result = service.createQuickNote(command)

            result.personId shouldBe personId
            verify { personRepository.findByIdAndUserId(personId, userId) }
        }

        @Test
        fun `should create sensitive quick note`() {
            every { quickNoteRepository.save(any()) } answers { firstArg() }

            val command = CreateQuickNoteCommand(
                userId = userId,
                text = "Personal health situation",
                sensitive = true
            )

            val result = service.createQuickNote(command)

            result.sensitive shouldBe true
        }

        @Test
        fun `should throw when person not found`() {
            every { personRepository.findByIdAndUserId(personId, userId) } returns null

            val command = CreateQuickNoteCommand(
                userId = userId,
                personId = personId,
                text = "Some note"
            )

            shouldThrow<PersonNotFoundException> {
                service.createQuickNote(command)
            }
        }
    }

    @Nested
    inner class UpdateQuickNoteTests {

        private val quickNoteId = QuickNoteId.generate()
        private val existingNote = QuickNote(
            id = quickNoteId,
            userId = userId,
            text = "Original text"
        )

        @Test
        fun `should update text`() {
            every { quickNoteRepository.findByIdAndUserId(quickNoteId, userId) } returns existingNote
            every { quickNoteRepository.save(any()) } answers { firstArg() }

            val command = UpdateQuickNoteCommand(
                userId = userId,
                quickNoteId = quickNoteId,
                text = "Updated text"
            )

            val result = service.updateQuickNote(command)

            result.text shouldBe "Updated text"
        }

        @Test
        fun `should update sensitive flag`() {
            every { quickNoteRepository.findByIdAndUserId(quickNoteId, userId) } returns existingNote
            every { quickNoteRepository.save(any()) } answers { firstArg() }

            val command = UpdateQuickNoteCommand(
                userId = userId,
                quickNoteId = quickNoteId,
                sensitive = true
            )

            val result = service.updateQuickNote(command)

            result.sensitive shouldBe true
        }

        @Test
        fun `should throw when quick note not found`() {
            every { quickNoteRepository.findByIdAndUserId(quickNoteId, userId) } returns null

            val command = UpdateQuickNoteCommand(
                userId = userId,
                quickNoteId = quickNoteId,
                text = "Updated"
            )

            shouldThrow<QuickNoteNotFoundException> {
                service.updateQuickNote(command)
            }
        }
    }

    @Nested
    inner class AssignToPersonTests {

        private val quickNoteId = QuickNoteId.generate()
        private val existingNote = QuickNote(
            id = quickNoteId,
            userId = userId,
            text = "Unassigned note"
        )

        @Test
        fun `should assign quick note to person`() {
            every { quickNoteRepository.findByIdAndUserId(quickNoteId, userId) } returns existingNote
            every { personRepository.findByIdAndUserId(personId, userId) } returns person
            every { quickNoteRepository.save(any()) } answers { firstArg() }

            val command = AssignQuickNoteToPersonCommand(
                userId = userId,
                quickNoteId = quickNoteId,
                personId = personId
            )

            val result = service.assignToPerson(command)

            result.personId shouldBe personId
        }

        @Test
        fun `should throw when person not found on assign`() {
            every { quickNoteRepository.findByIdAndUserId(quickNoteId, userId) } returns existingNote
            every { personRepository.findByIdAndUserId(personId, userId) } returns null

            val command = AssignQuickNoteToPersonCommand(
                userId = userId,
                quickNoteId = quickNoteId,
                personId = personId
            )

            shouldThrow<PersonNotFoundException> {
                service.assignToPerson(command)
            }
        }
    }

    @Nested
    inner class StatusTransitionTests {

        private val quickNoteId = QuickNoteId.generate()
        private val entryId = OneOnOneEntryId.generate()
        private val inboxNote = QuickNote(
            id = quickNoteId,
            userId = userId,
            text = "Inbox note"
        )

        @Test
        fun `should attach quick note to entry`() {
            val mockEntry = mockk<OneOnOneEntry>()
            every { quickNoteRepository.findByIdAndUserId(quickNoteId, userId) } returns inboxNote
            every { oneOnOneEntryRepository.findByIdAndUserId(entryId, userId) } returns mockEntry
            every { quickNoteRepository.save(any()) } answers { firstArg() }

            val command = AttachQuickNoteCommand(userId = userId, quickNoteId = quickNoteId, entryId = entryId)
            val result = service.attachQuickNote(command)

            result.status shouldBe QuickNoteStatus.ATTACHED
            result.attachedEntryId shouldBe entryId
        }

        @Test
        fun `should throw when entry not found on attach`() {
            every { quickNoteRepository.findByIdAndUserId(quickNoteId, userId) } returns inboxNote
            every { oneOnOneEntryRepository.findByIdAndUserId(entryId, userId) } returns null

            val command = AttachQuickNoteCommand(userId = userId, quickNoteId = quickNoteId, entryId = entryId)

            shouldThrow<OneOnOneEntryNotFoundException> {
                service.attachQuickNote(command)
            }
        }

        @Test
        fun `should convert quick note`() {
            every { quickNoteRepository.findByIdAndUserId(quickNoteId, userId) } returns inboxNote
            every { quickNoteRepository.save(any()) } answers { firstArg() }

            val command = ConvertQuickNoteCommand(userId = userId, quickNoteId = quickNoteId)
            val result = service.convertQuickNote(command)

            result.status shouldBe QuickNoteStatus.CONVERTED
        }

        @Test
        fun `should archive quick note`() {
            every { quickNoteRepository.findByIdAndUserId(quickNoteId, userId) } returns inboxNote
            every { quickNoteRepository.save(any()) } answers { firstArg() }

            val command = ArchiveQuickNoteCommand(userId = userId, quickNoteId = quickNoteId)
            val result = service.archiveQuickNote(command)

            result.status shouldBe QuickNoteStatus.ARCHIVED
        }

        @Test
        fun `should throw when attaching non-INBOX note`() {
            val attachedNote = inboxNote.copy(status = QuickNoteStatus.ATTACHED)
            every { quickNoteRepository.findByIdAndUserId(quickNoteId, userId) } returns attachedNote
            every { oneOnOneEntryRepository.findByIdAndUserId(entryId, userId) } returns mockk()

            val command = AttachQuickNoteCommand(userId = userId, quickNoteId = quickNoteId, entryId = entryId)

            shouldThrow<IllegalArgumentException> {
                service.attachQuickNote(command)
            }
        }
    }

    @Nested
    inner class DeleteQuickNoteTests {

        private val quickNoteId = QuickNoteId.generate()

        @Test
        fun `should delete quick note`() {
            every { quickNoteRepository.deleteByIdAndUserId(quickNoteId, userId) } returns true

            val command = DeleteQuickNoteCommand(userId = userId, quickNoteId = quickNoteId)
            service.deleteQuickNote(command)

            verify { quickNoteRepository.deleteByIdAndUserId(quickNoteId, userId) }
        }

        @Test
        fun `should throw when quick note not found on delete`() {
            every { quickNoteRepository.deleteByIdAndUserId(quickNoteId, userId) } returns false

            val command = DeleteQuickNoteCommand(userId = userId, quickNoteId = quickNoteId)

            shouldThrow<QuickNoteNotFoundException> {
                service.deleteQuickNote(command)
            }
        }
    }

    @Nested
    inner class QueryTests {

        private val quickNoteId = QuickNoteId.generate()
        private val existingNote = QuickNote(
            id = quickNoteId,
            userId = userId,
            text = "Test note"
        )

        @Test
        fun `should get quick note by id`() {
            every { quickNoteRepository.findByIdAndUserId(quickNoteId, userId) } returns existingNote

            val query = GetQuickNoteQuery(userId = userId, quickNoteId = quickNoteId)
            val result = service.getQuickNote(query)

            result shouldBe existingNote
        }

        @Test
        fun `should throw when quick note not found on get`() {
            every { quickNoteRepository.findByIdAndUserId(quickNoteId, userId) } returns null

            val query = GetQuickNoteQuery(userId = userId, quickNoteId = quickNoteId)

            shouldThrow<QuickNoteNotFoundException> {
                service.getQuickNote(query)
            }
        }

        @Test
        fun `should list all quick notes for user`() {
            val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))
            val page = PageImpl(listOf(existingNote), pageable, 1)
            every { quickNoteRepository.findAllByUserId(userId, pageable) } returns page

            val query = ListQuickNotesQuery(userId = userId)
            val result = service.listQuickNotes(query)

            result.content.size shouldBe 1
            result.content[0] shouldBe existingNote
        }

        @Test
        fun `should list quick notes filtered by status`() {
            val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))
            val page = PageImpl(listOf(existingNote), pageable, 1)
            every { quickNoteRepository.findAllByUserIdAndStatus(userId, QuickNoteStatus.INBOX, pageable) } returns page

            val query = ListQuickNotesQuery(userId = userId, status = QuickNoteStatus.INBOX)
            val result = service.listQuickNotes(query)

            result.content.size shouldBe 1
        }

        @Test
        fun `should list quick notes filtered by person`() {
            val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))
            val noteWithPerson = existingNote.copy(personId = personId)
            val page = PageImpl(listOf(noteWithPerson), pageable, 1)
            every { quickNoteRepository.findAllByUserIdAndPersonId(userId, personId, pageable) } returns page

            val query = ListQuickNotesQuery(userId = userId, personId = personId)
            val result = service.listQuickNotes(query)

            result.content.size shouldBe 1
            result.content[0].personId shouldBe personId
        }

        @Test
        fun `should list quick notes filtered by status and person`() {
            val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))
            val noteWithPerson = existingNote.copy(personId = personId)
            val page = PageImpl(listOf(noteWithPerson), pageable, 1)
            every {
                quickNoteRepository.findAllByUserIdAndStatusAndPersonId(userId, QuickNoteStatus.INBOX, personId, pageable)
            } returns page

            val query = ListQuickNotesQuery(userId = userId, status = QuickNoteStatus.INBOX, personId = personId)
            val result = service.listQuickNotes(query)

            result.content.size shouldBe 1
        }
    }
}
