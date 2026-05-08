package com.peoplemanager.adapters.web.dto

import com.peoplemanager.domain.CadenceType
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.time.Instant

data class UpsertSeriesRequest(
    @field:NotNull(message = "Cadence type must not be null")
    val cadenceType: CadenceType?,
    @field:Positive(message = "Custom interval days must be positive")
    val customIntervalDays: Int? = null,
    val templateMarkdown: String? = null
)

data class CreateOneOnOneEntryRequest(
    @field:NotNull(message = "Meeting date must not be null")
    val meetingDate: Instant?,
    @field:Valid
    val agendaItems: List<AgendaItemRequest>? = null,
    val notesMarkdown: String? = null,
    val outcomesMarkdown: String? = null,
    val sensitive: Boolean? = null
)

data class UpdateOneOnOneEntryRequest(
    val meetingDate: Instant? = null,
    @field:Valid
    val agendaItems: List<AgendaItemRequest>? = null,
    val notesMarkdown: String? = null,
    val outcomesMarkdown: String? = null,
    val sensitive: Boolean? = null
)

data class AgendaItemRequest(
    @field:NotBlank(message = "Agenda item text must not be blank")
    val text: String,
    val checked: Boolean = false
)
