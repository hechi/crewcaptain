package com.peoplemanager.application.commands

import com.peoplemanager.domain.OneOnOneEntryId
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.QuickNoteId
import com.peoplemanager.domain.UserId

data class CreateQuickNoteCommand(
    val userId: UserId,
    val personId: PersonId? = null,
    val text: String,
    val sensitive: Boolean = false,
    val selfAssigned: Boolean = false,
    val tag: String? = null
)

data class UpdateQuickNoteCommand(
    val userId: UserId,
    val quickNoteId: QuickNoteId,
    val text: String? = null,
    val personId: PersonId? = null,
    val sensitive: Boolean? = null,
    val tag: String? = null,
    val updateTag: Boolean = false
)

data class AssignQuickNoteToPersonCommand(
    val userId: UserId,
    val quickNoteId: QuickNoteId,
    val personId: PersonId
)

data class AssignQuickNoteToSelfCommand(
    val userId: UserId,
    val quickNoteId: QuickNoteId
)

data class AttachQuickNoteCommand(
    val userId: UserId,
    val quickNoteId: QuickNoteId,
    val entryId: OneOnOneEntryId
)

data class ConvertQuickNoteCommand(
    val userId: UserId,
    val quickNoteId: QuickNoteId,
    val personId: PersonId? = null,
    val dueDate: java.time.LocalDate? = null
)

data class ArchiveQuickNoteCommand(
    val userId: UserId,
    val quickNoteId: QuickNoteId
)

data class DeleteQuickNoteCommand(
    val userId: UserId,
    val quickNoteId: QuickNoteId
)

enum class MoveDirection { UP, DOWN }

/**
 * Moves a personal (self-assigned) note one position up or down in the user's
 * manual ordering by swapping its display order with the adjacent note.
 */
data class MoveQuickNoteCommand(
    val userId: UserId,
    val quickNoteId: QuickNoteId,
    val direction: MoveDirection
)

/**
 * Moves a personal (self-assigned) note to an absolute position in the user's manual
 * ordering (0-based index among their self-assigned INBOX notes). Used by drag-and-drop,
 * where a single drop can span multiple positions. The target index is clamped into range.
 */
data class ReorderQuickNoteCommand(
    val userId: UserId,
    val quickNoteId: QuickNoteId,
    val targetIndex: Int
)
