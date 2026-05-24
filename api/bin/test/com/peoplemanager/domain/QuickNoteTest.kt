package com.peoplemanager.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant

class QuickNoteTest {

    private val userId = UserId.generate()
    private val personId = PersonId.generate()

    private fun createQuickNote(
        text: String = "Remember to discuss project timeline",
        personId: PersonId? = null,
        sensitive: Boolean = false,
        status: QuickNoteStatus = QuickNoteStatus.INBOX
    ) = QuickNote(
        id = QuickNoteId.generate(),
        userId = userId,
        personId = personId,
        text = text,
        sensitive = sensitive,
        status = status
    )

    @Nested
    inner class CreationTests {

        @Test
        fun `should create quick note with minimal fields`() {
            val note = createQuickNote()

            note.text shouldBe "Remember to discuss project timeline"
            note.userId shouldBe userId
            note.personId shouldBe null
            note.sensitive shouldBe false
            note.status shouldBe QuickNoteStatus.INBOX
            note.id shouldNotBe null
            note.createdAt shouldNotBe null
            note.updatedAt shouldNotBe null
        }

        @Test
        fun `should create quick note with person assigned`() {
            val note = createQuickNote(personId = personId)

            note.personId shouldBe personId
        }

        @Test
        fun `should create quick note with sensitive flag`() {
            val note = createQuickNote(sensitive = true)

            note.sensitive shouldBe true
        }

        @Test
        fun `should reject blank text`() {
            shouldThrow<IllegalArgumentException> {
                createQuickNote(text = "")
            }.message shouldBe "Quick note text must not be blank"
        }

        @Test
        fun `should reject whitespace-only text`() {
            shouldThrow<IllegalArgumentException> {
                createQuickNote(text = "   ")
            }.message shouldBe "Quick note text must not be blank"
        }
    }

    @Nested
    inner class AssignToPersonTests {

        @Test
        fun `should assign quick note to a person`() {
            val note = createQuickNote()
            val assigned = note.assignToPerson(personId)

            assigned.personId shouldBe personId
            assigned.updatedAt shouldNotBe note.updatedAt
        }

        @Test
        fun `should reassign quick note to a different person`() {
            val otherPersonId = PersonId.generate()
            val note = createQuickNote(personId = personId)
            val reassigned = note.assignToPerson(otherPersonId)

            reassigned.personId shouldBe otherPersonId
        }
    }

    @Nested
    inner class StatusTransitionTests {

        private val entryId = OneOnOneEntryId.generate()

        @Test
        fun `should mark as attached from INBOX`() {
            val note = createQuickNote()
            val attached = note.markAttached(entryId)

            attached.status shouldBe QuickNoteStatus.ATTACHED
            attached.attachedEntryId shouldBe entryId
            attached.updatedAt shouldNotBe note.updatedAt
        }

        @Test
        fun `should reject attach from non-INBOX status`() {
            val note = createQuickNote(status = QuickNoteStatus.ARCHIVED)

            shouldThrow<IllegalArgumentException> {
                note.markAttached(entryId)
            }.message shouldBe "Can only attach a quick note with status INBOX, current status is ARCHIVED"
        }

        @Test
        fun `should mark as converted from INBOX`() {
            val note = createQuickNote()
            val converted = note.markConverted()

            converted.status shouldBe QuickNoteStatus.CONVERTED
            converted.updatedAt shouldNotBe note.updatedAt
        }

        @Test
        fun `should reject convert from non-INBOX status`() {
            val note = createQuickNote(status = QuickNoteStatus.ATTACHED)

            shouldThrow<IllegalArgumentException> {
                note.markConverted()
            }.message shouldBe "Can only convert a quick note with status INBOX, current status is ATTACHED"
        }

        @Test
        fun `should archive from INBOX`() {
            val note = createQuickNote()
            val archived = note.archive()

            archived.status shouldBe QuickNoteStatus.ARCHIVED
            archived.updatedAt shouldNotBe note.updatedAt
        }

        @Test
        fun `should reject archive from non-INBOX status`() {
            val note = createQuickNote(status = QuickNoteStatus.CONVERTED)

            shouldThrow<IllegalArgumentException> {
                note.archive()
            }.message shouldBe "Can only archive a quick note with status INBOX, current status is CONVERTED"
        }
    }

    @Nested
    inner class UpdateTests {

        @Test
        fun `should update text`() {
            val note = createQuickNote()
            val updated = note.updateText("New text content")

            updated.text shouldBe "New text content"
            updated.updatedAt shouldNotBe note.updatedAt
        }

        @Test
        fun `should reject blank text on update`() {
            val note = createQuickNote()

            shouldThrow<IllegalArgumentException> {
                note.updateText("")
            }.message shouldBe "Quick note text must not be blank"
        }

        @Test
        fun `should toggle sensitive flag`() {
            val note = createQuickNote(sensitive = false)
            val toggled = note.toggleSensitive()

            toggled.sensitive shouldBe true
        }

        @Test
        fun `should toggle sensitive flag back`() {
            val note = createQuickNote(sensitive = true)
            val toggled = note.toggleSensitive()

            toggled.sensitive shouldBe false
        }
    }

    @Nested
    inner class SelfAssignedTests {

        @Test
        fun `should create self-assigned quick note`() {
            val note = QuickNote(
                id = QuickNoteId.generate(),
                userId = userId,
                text = "My personal reminder",
                selfAssigned = true
            )

            note.selfAssigned shouldBe true
            note.personId shouldBe null
        }

        @Test
        fun `should reject self-assigned note with personId set`() {
            shouldThrow<IllegalArgumentException> {
                QuickNote(
                    id = QuickNoteId.generate(),
                    userId = userId,
                    personId = personId,
                    text = "Invalid note",
                    selfAssigned = true
                )
            }.message shouldBe "A quick note cannot be both self-assigned and assigned to a person"
        }

        @Test
        fun `should mark note as self-assigned`() {
            val note = createQuickNote()
            val selfAssigned = note.markSelfAssigned()

            selfAssigned.selfAssigned shouldBe true
            selfAssigned.personId shouldBe null
            selfAssigned.updatedAt shouldNotBe note.updatedAt
        }

        @Test
        fun `should clear personId when marking as self-assigned`() {
            val note = createQuickNote(personId = personId)
            val selfAssigned = note.markSelfAssigned()

            selfAssigned.selfAssigned shouldBe true
            selfAssigned.personId shouldBe null
        }

        @Test
        fun `should clear selfAssigned when assigning to person`() {
            val note = QuickNote(
                id = QuickNoteId.generate(),
                userId = userId,
                text = "My personal note",
                selfAssigned = true
            )
            val assigned = note.assignToPerson(personId)

            assigned.selfAssigned shouldBe false
            assigned.personId shouldBe personId
        }
    }
}
