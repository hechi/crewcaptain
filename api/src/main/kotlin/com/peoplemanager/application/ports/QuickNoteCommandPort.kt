package com.peoplemanager.application.ports

import com.peoplemanager.application.commands.*
import com.peoplemanager.domain.QuickNote

interface QuickNoteCommandPort {
    fun createQuickNote(command: CreateQuickNoteCommand): QuickNote
    fun updateQuickNote(command: UpdateQuickNoteCommand): QuickNote
    fun assignToPerson(command: AssignQuickNoteToPersonCommand): QuickNote
    fun attachQuickNote(command: AttachQuickNoteCommand): QuickNote
    fun convertQuickNote(command: ConvertQuickNoteCommand): QuickNote
    fun archiveQuickNote(command: ArchiveQuickNoteCommand): QuickNote
    fun deleteQuickNote(command: DeleteQuickNoteCommand)
}
