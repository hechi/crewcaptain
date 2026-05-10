package com.peoplemanager.application.commands

import com.peoplemanager.domain.OneOnOneEntryId
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.QuickNoteId
import com.peoplemanager.domain.UserId

data class CreateQuickNoteCommand(
    val userId: UserId,
    val personId: PersonId? = null,
    val text: String,
    val sensitive: Boolean = false
)

data class UpdateQuickNoteCommand(
    val userId: UserId,
    val quickNoteId: QuickNoteId,
    val text: String? = null,
    val personId: PersonId? = null,
    val sensitive: Boolean? = null
)

data class AssignQuickNoteToPersonCommand(
    val userId: UserId,
    val quickNoteId: QuickNoteId,
    val personId: PersonId
)

data class AttachQuickNoteCommand(
    val userId: UserId,
    val quickNoteId: QuickNoteId,
    val entryId: OneOnOneEntryId
)

data class ConvertQuickNoteCommand(
    val userId: UserId,
    val quickNoteId: QuickNoteId,
    val personId: PersonId
)

data class ArchiveQuickNoteCommand(
    val userId: UserId,
    val quickNoteId: QuickNoteId
)

data class DeleteQuickNoteCommand(
    val userId: UserId,
    val quickNoteId: QuickNoteId
)
