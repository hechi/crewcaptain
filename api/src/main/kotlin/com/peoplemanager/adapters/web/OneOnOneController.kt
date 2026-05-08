package com.peoplemanager.adapters.web

import com.peoplemanager.adapters.auth.AuthenticatedUser
import com.peoplemanager.adapters.web.dto.CreateOneOnOneEntryRequest
import com.peoplemanager.adapters.web.dto.OneOnOneEntryResponse
import com.peoplemanager.adapters.web.dto.OneOnOneSeriesResponse
import com.peoplemanager.adapters.web.dto.PaginatedOneOnOneEntryResponse
import com.peoplemanager.adapters.web.dto.UpdateOneOnOneEntryRequest
import com.peoplemanager.adapters.web.dto.UpsertSeriesRequest
import com.peoplemanager.application.commands.AgendaItemInput
import com.peoplemanager.application.commands.CreateOneOnOneEntryCommand
import com.peoplemanager.application.commands.DeleteOneOnOneEntryCommand
import com.peoplemanager.application.commands.UpdateOneOnOneEntryCommand
import com.peoplemanager.application.commands.UpsertOneOnOneSeriesCommand
import com.peoplemanager.application.ports.OneOnOneCommandPort
import com.peoplemanager.application.ports.OneOnOneQueryPort
import com.peoplemanager.application.queries.GetOneOnOneEntryQuery
import com.peoplemanager.application.queries.GetOneOnOneSeriesQuery
import com.peoplemanager.application.queries.ListOneOnOneEntriesQuery
import com.peoplemanager.domain.OneOnOneEntryId
import com.peoplemanager.domain.PersonId
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/persons/{personId}")
class OneOnOneController(
    private val oneOnOneCommandPort: OneOnOneCommandPort,
    private val oneOnOneQueryPort: OneOnOneQueryPort
) {

    @PutMapping("/one-on-one-series")
    fun upsertSeries(
        @PathVariable personId: UUID,
        @Valid @RequestBody request: UpsertSeriesRequest
    ): ResponseEntity<OneOnOneSeriesResponse> {
        val userId = AuthenticatedUser.getUserId()
        val command = UpsertOneOnOneSeriesCommand(
            userId = userId,
            personId = PersonId(personId),
            cadenceType = request.cadenceType!!,
            customIntervalDays = request.customIntervalDays,
            templateMarkdown = request.templateMarkdown
        )
        val series = oneOnOneCommandPort.upsertSeries(command)
        return ResponseEntity.ok(OneOnOneSeriesResponse.from(series))
    }

    @GetMapping("/one-on-one-series")
    fun getSeries(@PathVariable personId: UUID): ResponseEntity<OneOnOneSeriesResponse> {
        val userId = AuthenticatedUser.getUserId()
        val query = GetOneOnOneSeriesQuery(userId = userId, personId = PersonId(personId))
        val series = oneOnOneQueryPort.getSeries(query)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(OneOnOneSeriesResponse.from(series))
    }

    @PostMapping("/one-on-one-entries")
    fun createEntry(
        @PathVariable personId: UUID,
        @Valid @RequestBody request: CreateOneOnOneEntryRequest
    ): ResponseEntity<OneOnOneEntryResponse> {
        val userId = AuthenticatedUser.getUserId()
        val command = CreateOneOnOneEntryCommand(
            userId = userId,
            personId = PersonId(personId),
            meetingDate = request.meetingDate!!,
            agendaItems = request.agendaItems?.map { AgendaItemInput(text = it.text, checked = it.checked) }
                ?: emptyList(),
            notesMarkdown = request.notesMarkdown,
            outcomesMarkdown = request.outcomesMarkdown,
            sensitive = request.sensitive ?: false
        )
        val entry = oneOnOneCommandPort.createEntry(command)
        return ResponseEntity.status(HttpStatus.CREATED).body(OneOnOneEntryResponse.from(entry))
    }

    @GetMapping("/one-on-one-entries/{entryId}")
    fun getEntry(
        @PathVariable personId: UUID,
        @PathVariable entryId: UUID
    ): ResponseEntity<OneOnOneEntryResponse> {
        val userId = AuthenticatedUser.getUserId()
        val query = GetOneOnOneEntryQuery(
            userId = userId,
            personId = PersonId(personId),
            entryId = OneOnOneEntryId(entryId)
        )
        val entry = oneOnOneQueryPort.getEntry(query)
        return ResponseEntity.ok(OneOnOneEntryResponse.from(entry))
    }

    @PutMapping("/one-on-one-entries/{entryId}")
    fun updateEntry(
        @PathVariable personId: UUID,
        @PathVariable entryId: UUID,
        @Valid @RequestBody request: UpdateOneOnOneEntryRequest
    ): ResponseEntity<OneOnOneEntryResponse> {
        val userId = AuthenticatedUser.getUserId()
        val command = UpdateOneOnOneEntryCommand(
            userId = userId,
            personId = PersonId(personId),
            entryId = OneOnOneEntryId(entryId),
            meetingDate = request.meetingDate,
            agendaItems = request.agendaItems?.map { AgendaItemInput(text = it.text, checked = it.checked) },
            notesMarkdown = request.notesMarkdown,
            outcomesMarkdown = request.outcomesMarkdown,
            sensitive = request.sensitive
        )
        val entry = oneOnOneCommandPort.updateEntry(command)
        return ResponseEntity.ok(OneOnOneEntryResponse.from(entry))
    }

    @DeleteMapping("/one-on-one-entries/{entryId}")
    fun deleteEntry(
        @PathVariable personId: UUID,
        @PathVariable entryId: UUID
    ): ResponseEntity<Void> {
        val userId = AuthenticatedUser.getUserId()
        val command = DeleteOneOnOneEntryCommand(
            userId = userId,
            personId = PersonId(personId),
            entryId = OneOnOneEntryId(entryId)
        )
        oneOnOneCommandPort.deleteEntry(command)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/one-on-one-entries")
    fun listEntries(
        @PathVariable personId: UUID,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PaginatedOneOnOneEntryResponse> {
        val userId = AuthenticatedUser.getUserId()
        val query = ListOneOnOneEntriesQuery(
            userId = userId,
            personId = PersonId(personId),
            page = page,
            size = size
        )
        val result = oneOnOneQueryPort.listEntries(query)
        return ResponseEntity.ok(PaginatedOneOnOneEntryResponse.from(result))
    }
}
