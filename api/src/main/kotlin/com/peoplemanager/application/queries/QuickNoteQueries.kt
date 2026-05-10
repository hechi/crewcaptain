package com.peoplemanager.application.queries

import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.QuickNoteId
import com.peoplemanager.domain.QuickNoteStatus
import com.peoplemanager.domain.UserId

data class GetQuickNoteQuery(
    val userId: UserId,
    val quickNoteId: QuickNoteId
)

data class ListQuickNotesQuery(
    val userId: UserId,
    val status: QuickNoteStatus? = null,
    val personId: PersonId? = null,
    val page: Int = 0,
    val size: Int = 20
)
