package com.peoplemanager.application.port.input

import com.peoplemanager.application.queries.GetQuickNoteQuery
import com.peoplemanager.application.queries.ListQuickNotesQuery
import com.peoplemanager.domain.QuickNote
import org.springframework.data.domain.Page

interface QuickNoteQueryPort {
    fun getQuickNote(query: GetQuickNoteQuery): QuickNote
    fun listQuickNotes(query: ListQuickNotesQuery): Page<QuickNote>
}
