package com.peoplemanager.application.commands

import com.peoplemanager.domain.CadenceType
import com.peoplemanager.domain.OneOnOneEntryId
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.UserId
import java.time.Instant

data class UpsertOneOnOneSeriesCommand(
    val userId: UserId,
    val personId: PersonId,
    val cadenceType: CadenceType,
    val customIntervalDays: Int? = null,
    val templateMarkdown: String? = null
)

data class CreateOneOnOneEntryCommand(
    val userId: UserId,
    val personId: PersonId,
    val meetingDate: Instant,
    val agendaItems: List<AgendaItemInput> = emptyList(),
    val notesMarkdown: String? = null,
    val outcomesMarkdown: String? = null,
    val sensitive: Boolean = false
)

data class UpdateOneOnOneEntryCommand(
    val userId: UserId,
    val personId: PersonId,
    val entryId: OneOnOneEntryId,
    val meetingDate: Instant? = null,
    val agendaItems: List<AgendaItemInput>? = null,
    val notesMarkdown: String? = null,
    val outcomesMarkdown: String? = null,
    val sensitive: Boolean? = null
)

data class DeleteOneOnOneEntryCommand(
    val userId: UserId,
    val personId: PersonId,
    val entryId: OneOnOneEntryId
)

data class AgendaItemInput(
    val text: String,
    val checked: Boolean = false
)
