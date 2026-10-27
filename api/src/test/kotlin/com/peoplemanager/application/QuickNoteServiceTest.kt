package com.peoplemanager.application

import com.peoplemanager.application.commands.*
import com.peoplemanager.application.port.output.ActionItemRepository
import com.peoplemanager.application.port.output.OneOnOneEntryRepository
import com.peoplemanager.application.port.output.PersonRepository
import com.peoplemanager.application.port.output.QuickNoteRepository
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
    private val actionItemRepository = mockk<ActionItemRepository>()
    private val auditLogService = mockk<AuditLogService>(relaxed = true)

    private val service = QuickNoteService(quickNoteRepository, personRepository, oneOnOneEntryRepository, actionItemRepository, auditLogService)

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
        fun `should attach quick note to entry and add agenda item`() {
            val entry = OneOnOneEntry(
                id = OneOnOneEntryId(entryId.value),
                userId = userId,
                personId = personId,
                meetingDate = java.time.Instant.now(),
                agendaItems = emptyList()
            )
            every { quickNoteRepository.findByIdAndUserId(quickNoteId, userId) } returns inboxNote
            every { oneOnOneEntryRepository.findByIdAndUserId(entryId, userId) } returns entry
            every { oneOnOneEntryRepository.save(any()) } answers { firstArg() }
            every { quickNoteRepository.save(any()) } answers { firstArg() }

            val command = AttachQuickNoteCommand(userId = userId, quickNoteId = quickNoteId, entryId = entryId)
            val result = service.attachQuickNote(command)

            result.status shouldBe QuickNoteStatus.ATTACHED
            result.attachedEntryId shouldBe entryId
            result.personId shouldBe personId
            verify { oneOnOneEntryRepository.save(match { it.agendaItems.size == 1 && it.agendaItems[0].text == "Inbox note" }) }
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
        fun `should convert quick note to action item`() {
            every { quickNoteRepository.findByIdAndUserId(quickNoteId, userId) } returns inboxNote
            every { personRepository.findByIdAndUserId(personId, userId) } returns person
            every { actionItemRepository.save(any()) } answers { firstArg() }
            every { quickNoteRepository.save(any()) } answers { firstArg() }

            val command = ConvertQuickNoteCommand(userId = userId, quickNoteId = quickNoteId, personId = personId)
            val result = service.convertQuickNote(command)

            result.status shouldBe QuickNoteStatus.CONVERTED
            result.personId shouldBe personId
            verify {
                actionItemRepository.save(
                    match { it.title == "Inbox note" && it.personId == personId && it.originatingQuickNoteId == quickNoteId }
                )
            }
        }

        @Test
        fun `should convert a self-assigned note to a person-less action item`() {
            val selfNote = QuickNote(
                id = quickNoteId,
                userId = userId,
                text = "My personal todo",
                selfAssigned = true
            )
            every { quickNoteRepository.findByIdAndUserId(quickNoteId, userId) } returns selfNote
            every { actionItemRepository.save(any()) } answers { firstArg() }
            every { quickNoteRepository.save(any()) } answers { firstArg() }

            val command = ConvertQuickNoteCommand(userId = userId, quickNoteId = quickNoteId, personId = null)
            val result = service.convertQuickNote(command)

            result.status shouldBe QuickNoteStatus.CONVERTED
            result.personId shouldBe null
            result.selfAssigned shouldBe true
            verify {
                actionItemRepository.save(
                    match { it.title == "My personal todo" && it.personId == null && it.originatingQuickNoteId == quickNoteId }
                )
            }
            verify(exactly = 0) { personRepository.findByIdAndUserId(any(), any()) }
        }

        @Test
        fun `should convert with a due date`() {
            val dueDate = java.time.LocalDate.of(2026, 7, 1)
            every { quickNoteRepository.findByIdAndUserId(quickNoteId, userId) } returns inboxNote
            every { personRepository.findByIdAndUserId(personId, userId) } returns person
            every { actionItemRepository.save(any()) } answers { firstArg() }
            every { quickNoteRepository.save(any()) } answers { firstArg() }

            val command = ConvertQuickNoteCommand(
                userId = userId,
                quickNoteId = quickNoteId,
                personId = personId,
                dueDate = dueDate
            )
            service.convertQuickNote(command)

            verify { actionItemRepository.save(match { it.dueDate == dueDate }) }
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

        @Test
        fun `should list self-assigned quick notes ordered by display order`() {
            val selfAssignedPageable = PageRequest.of(
                0, 20, Sort.by(Sort.Order.asc("displayOrder"), Sort.Order.desc("createdAt"))
            )
            val selfAssignedNote = existingNote.copy(selfAssigned = true)
            val page = PageImpl(listOf(selfAssignedNote), selfAssignedPageable, 1)
            every { quickNoteRepository.findAllByUserIdAndSelfAssigned(userId, true, selfAssignedPageable) } returns page

            val query = ListQuickNotesQuery(userId = userId, selfAssigned = true)
            val result = service.listQuickNotes(query)

            result.content.size shouldBe 1
            result.content[0].selfAssigned shouldBe true
        }

        @Test
        fun `should list self-assigned quick notes filtered by status ordered by display order`() {
            val selfAssignedPageable = PageRequest.of(
                0, 20, Sort.by(Sort.Order.asc("displayOrder"), Sort.Order.desc("createdAt"))
            )
            val selfAssignedNote = existingNote.copy(selfAssigned = true)
            val page = PageImpl(listOf(selfAssignedNote), selfAssignedPageable, 1)
            every {
                quickNoteRepository.findAllByUserIdAndSelfAssignedAndStatus(userId, true, QuickNoteStatus.INBOX, selfAssignedPageable)
            } returns page

            val query = ListQuickNotesQuery(userId = userId, selfAssigned = true, status = QuickNoteStatus.INBOX)
            val result = service.listQuickNotes(query)

            result.content.size shouldBe 1
        }
    }

    @Nested
    inner class SelfAssignedTests {

        private val quickNoteId = QuickNoteId.generate()

        @Test
        fun `should create self-assigned quick note`() {
            every { quickNoteRepository.findMinSelfAssignedDisplayOrder(userId) } returns null
            every { quickNoteRepository.save(any()) } answers { firstArg() }

            val command = CreateQuickNoteCommand(
                userId = userId,
                text = "My personal reminder",
                selfAssigned = true
            )

            val result = service.createQuickNote(command)

            result.selfAssigned shouldBe true
            result.personId shouldBe null
        }

        @Test
        fun `should clear selfAssigned when assigning to person`() {
            val selfAssignedNote = QuickNote(
                id = quickNoteId,
                userId = userId,
                text = "My personal note",
                selfAssigned = true
            )
            every { quickNoteRepository.findByIdAndUserId(quickNoteId, userId) } returns selfAssignedNote
            every { personRepository.findByIdAndUserId(personId, userId) } returns person
            every { quickNoteRepository.save(any()) } answers { firstArg() }

            val command = AssignQuickNoteToPersonCommand(
                userId = userId,
                quickNoteId = quickNoteId,
                personId = personId
            )

            val result = service.assignToPerson(command)

            result.selfAssigned shouldBe false
            result.personId shouldBe personId
        }

        @Test
        fun `should assign quick note to self`() {
            val unassignedNote = QuickNote(
                id = quickNoteId,
                userId = userId,
                text = "Unassigned note"
            )
            every { quickNoteRepository.findByIdAndUserId(quickNoteId, userId) } returns unassignedNote
            every { quickNoteRepository.save(any()) } answers { firstArg() }

            val command = AssignQuickNoteToSelfCommand(
                userId = userId,
                quickNoteId = quickNoteId
            )

            val result = service.assignToSelf(command)

            result.selfAssigned shouldBe true
            result.personId shouldBe null
        }

        @Test
        fun `should clear personId when assigning to self`() {
            val personAssignedNote = QuickNote(
                id = quickNoteId,
                userId = userId,
                personId = personId,
                text = "Person-assigned note"
            )
            every { quickNoteRepository.findByIdAndUserId(quickNoteId, userId) } returns personAssignedNote
            every { quickNoteRepository.save(any()) } answers { firstArg() }

            val command = AssignQuickNoteToSelfCommand(
                userId = userId,
                quickNoteId = quickNoteId
            )

            val result = service.assignToSelf(command)

            result.selfAssigned shouldBe true
            result.personId shouldBe null
        }

        @Test
        fun `should throw when quick note not found on assign to self`() {
            every { quickNoteRepository.findByIdAndUserId(quickNoteId, userId) } returns null

            val command = AssignQuickNoteToSelfCommand(
                userId = userId,
                quickNoteId = quickNoteId
            )

            shouldThrow<QuickNoteNotFoundException> {
                service.assignToSelf(command)
            }
        }
    }

    @Nested
    inner class DisplayOrderOnCreateTests {

        @Test
        fun `should place a new self-assigned note above all existing ones`() {
            // Existing top note has order 3; the new note must get a strictly lower order.
            every { quickNoteRepository.findMinSelfAssignedDisplayOrder(userId) } returns 3
            every { quickNoteRepository.save(any()) } answers { firstArg() }

            val command = CreateQuickNoteCommand(
                userId = userId,
                text = "Newest personal note",
                selfAssigned = true
            )

            val result = service.createQuickNote(command)

            (result.displayOrder < 3) shouldBe true
            verify { quickNoteRepository.findMinSelfAssignedDisplayOrder(userId) }
        }

        @Test
        fun `should use order 0 for the first self-assigned note`() {
            every { quickNoteRepository.findMinSelfAssignedDisplayOrder(userId) } returns null
            every { quickNoteRepository.save(any()) } answers { firstArg() }

            val command = CreateQuickNoteCommand(
                userId = userId,
                text = "First personal note",
                selfAssigned = true
            )

            val result = service.createQuickNote(command)

            result.displayOrder shouldBe 0
        }

        @Test
        fun `should not compute display order for non self-assigned notes`() {
            every { quickNoteRepository.save(any()) } answers { firstArg() }

            val command = CreateQuickNoteCommand(
                userId = userId,
                text = "A plain inbox note"
            )

            service.createQuickNote(command)

            verify(exactly = 0) { quickNoteRepository.findMinSelfAssignedDisplayOrder(any()) }
        }
    }

    @Nested
    inner class MoveQuickNoteTests {

        private fun selfNote(order: Int, text: String) = QuickNote(
            id = QuickNoteId.generate(),
            userId = userId,
            text = text,
            selfAssigned = true,
            displayOrder = order
        )

        @Test
        fun `should move a note up by swapping display order with the note above`() {
            val top = selfNote(0, "top")
            val middle = selfNote(1, "middle")
            val bottom = selfNote(2, "bottom")
            every { quickNoteRepository.findByIdAndUserId(middle.id, userId) } returns middle
            every { quickNoteRepository.findSelfAssignedOrdered(userId, QuickNoteStatus.INBOX) } returns listOf(top, middle, bottom)
            val saved = mutableListOf<QuickNote>()
            every { quickNoteRepository.save(capture(saved)) } answers { firstArg() }

            val command = MoveQuickNoteCommand(userId = userId, quickNoteId = middle.id, direction = MoveDirection.UP)
            val result = service.moveQuickNote(command)

            // middle takes top's order, top takes middle's order
            result.id shouldBe middle.id
            result.displayOrder shouldBe 0
            saved.first { it.id == top.id }.displayOrder shouldBe 1
            saved.first { it.id == middle.id }.displayOrder shouldBe 0
        }

        @Test
        fun `should move a note down by swapping display order with the note below`() {
            val top = selfNote(0, "top")
            val middle = selfNote(1, "middle")
            val bottom = selfNote(2, "bottom")
            every { quickNoteRepository.findByIdAndUserId(middle.id, userId) } returns middle
            every { quickNoteRepository.findSelfAssignedOrdered(userId, QuickNoteStatus.INBOX) } returns listOf(top, middle, bottom)
            val saved = mutableListOf<QuickNote>()
            every { quickNoteRepository.save(capture(saved)) } answers { firstArg() }

            val command = MoveQuickNoteCommand(userId = userId, quickNoteId = middle.id, direction = MoveDirection.DOWN)
            val result = service.moveQuickNote(command)

            result.displayOrder shouldBe 2
            saved.first { it.id == bottom.id }.displayOrder shouldBe 1
            saved.first { it.id == middle.id }.displayOrder shouldBe 2
        }

        @Test
        fun `should be a no-op when moving the top note up`() {
            val top = selfNote(0, "top")
            val bottom = selfNote(1, "bottom")
            every { quickNoteRepository.findByIdAndUserId(top.id, userId) } returns top
            every { quickNoteRepository.findSelfAssignedOrdered(userId, QuickNoteStatus.INBOX) } returns listOf(top, bottom)

            val command = MoveQuickNoteCommand(userId = userId, quickNoteId = top.id, direction = MoveDirection.UP)
            val result = service.moveQuickNote(command)

            result.id shouldBe top.id
            result.displayOrder shouldBe 0
            verify(exactly = 0) { quickNoteRepository.save(any()) }
        }

        @Test
        fun `should be a no-op when moving the bottom note down`() {
            val top = selfNote(0, "top")
            val bottom = selfNote(1, "bottom")
            every { quickNoteRepository.findByIdAndUserId(bottom.id, userId) } returns bottom
            every { quickNoteRepository.findSelfAssignedOrdered(userId, QuickNoteStatus.INBOX) } returns listOf(top, bottom)

            val command = MoveQuickNoteCommand(userId = userId, quickNoteId = bottom.id, direction = MoveDirection.DOWN)
            val result = service.moveQuickNote(command)

            result.id shouldBe bottom.id
            verify(exactly = 0) { quickNoteRepository.save(any()) }
        }

        @Test
        fun `should throw when the note to move is not found for this user`() {
            val someId = QuickNoteId.generate()
            every { quickNoteRepository.findByIdAndUserId(someId, userId) } returns null

            val command = MoveQuickNoteCommand(userId = userId, quickNoteId = someId, direction = MoveDirection.UP)

            shouldThrow<QuickNoteNotFoundException> {
                service.moveQuickNote(command)
            }
        }
    }
}
