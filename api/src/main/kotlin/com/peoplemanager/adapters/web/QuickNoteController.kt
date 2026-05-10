package com.peoplemanager.adapters.web

import com.peoplemanager.adapters.auth.AuthenticatedUser
import com.peoplemanager.adapters.web.dto.*
import com.peoplemanager.application.commands.*
import com.peoplemanager.application.ports.QuickNoteCommandPort
import com.peoplemanager.application.ports.QuickNoteQueryPort
import com.peoplemanager.application.queries.GetQuickNoteQuery
import com.peoplemanager.application.queries.ListQuickNotesQuery
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.QuickNoteId
import com.peoplemanager.domain.QuickNoteStatus
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class QuickNoteController(
    private val quickNoteCommandPort: QuickNoteCommandPort,
    private val quickNoteQueryPort: QuickNoteQueryPort
) {

    @PostMapping("/quick-notes")
    fun createQuickNote(
        @Valid @RequestBody request: CreateQuickNoteRequest
    ): ResponseEntity<QuickNoteResponse> {
        val userId = AuthenticatedUser.getUserId()
        val command = CreateQuickNoteCommand(
            userId = userId,
            personId = request.personId?.let { PersonId(it) },
            text = request.text!!,
            sensitive = request.sensitive ?: false
        )
        val quickNote = quickNoteCommandPort.createQuickNote(command)
        return ResponseEntity.status(HttpStatus.CREATED).body(QuickNoteResponse.from(quickNote))
    }

    @GetMapping("/quick-notes")
    fun listQuickNotes(
        @RequestParam(required = false) status: QuickNoteStatus?,
        @RequestParam(required = false) personId: UUID?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PaginatedQuickNoteResponse> {
        val userId = AuthenticatedUser.getUserId()
        val query = ListQuickNotesQuery(
            userId = userId,
            status = status,
            personId = personId?.let { PersonId(it) },
            page = page,
            size = size
        )
        val result = quickNoteQueryPort.listQuickNotes(query)
        return ResponseEntity.ok(PaginatedQuickNoteResponse.from(result))
    }

    @GetMapping("/quick-notes/{quickNoteId}")
    fun getQuickNote(
        @PathVariable quickNoteId: UUID
    ): ResponseEntity<QuickNoteResponse> {
        val userId = AuthenticatedUser.getUserId()
        val query = GetQuickNoteQuery(userId = userId, quickNoteId = QuickNoteId(quickNoteId))
        val quickNote = quickNoteQueryPort.getQuickNote(query)
        return ResponseEntity.ok(QuickNoteResponse.from(quickNote))
    }

    @PutMapping("/quick-notes/{quickNoteId}")
    fun updateQuickNote(
        @PathVariable quickNoteId: UUID,
        @RequestBody request: UpdateQuickNoteRequest
    ): ResponseEntity<QuickNoteResponse> {
        val userId = AuthenticatedUser.getUserId()
        val command = UpdateQuickNoteCommand(
            userId = userId,
            quickNoteId = QuickNoteId(quickNoteId),
            text = request.text,
            personId = request.personId?.let { PersonId(it) },
            sensitive = request.sensitive
        )
        val quickNote = quickNoteCommandPort.updateQuickNote(command)
        return ResponseEntity.ok(QuickNoteResponse.from(quickNote))
    }

    @PostMapping("/quick-notes/{quickNoteId}/assign")
    fun assignToPerson(
        @PathVariable quickNoteId: UUID,
        @Valid @RequestBody request: AssignQuickNoteToPersonRequest
    ): ResponseEntity<QuickNoteResponse> {
        val userId = AuthenticatedUser.getUserId()
        val command = AssignQuickNoteToPersonCommand(
            userId = userId,
            quickNoteId = QuickNoteId(quickNoteId),
            personId = PersonId(request.personId)
        )
        val quickNote = quickNoteCommandPort.assignToPerson(command)
        return ResponseEntity.ok(QuickNoteResponse.from(quickNote))
    }

    @PostMapping("/quick-notes/{quickNoteId}/attach")
    fun attachQuickNote(
        @PathVariable quickNoteId: UUID,
        @Valid @RequestBody request: AttachQuickNoteToEntryRequest
    ): ResponseEntity<QuickNoteResponse> {
        val userId = AuthenticatedUser.getUserId()
        val command = AttachQuickNoteCommand(
            userId = userId,
            quickNoteId = QuickNoteId(quickNoteId),
            entryId = com.peoplemanager.domain.OneOnOneEntryId(request.entryId)
        )
        val quickNote = quickNoteCommandPort.attachQuickNote(command)
        return ResponseEntity.ok(QuickNoteResponse.from(quickNote))
    }

    @PostMapping("/quick-notes/{quickNoteId}/convert")
    fun convertQuickNote(
        @PathVariable quickNoteId: UUID,
        @Valid @RequestBody request: ConvertQuickNoteRequest
    ): ResponseEntity<QuickNoteResponse> {
        val userId = AuthenticatedUser.getUserId()
        val command = ConvertQuickNoteCommand(
            userId = userId,
            quickNoteId = QuickNoteId(quickNoteId),
            personId = PersonId(request.personId)
        )
        val quickNote = quickNoteCommandPort.convertQuickNote(command)
        return ResponseEntity.ok(QuickNoteResponse.from(quickNote))
    }

    @PostMapping("/quick-notes/{quickNoteId}/archive")
    fun archiveQuickNote(
        @PathVariable quickNoteId: UUID
    ): ResponseEntity<QuickNoteResponse> {
        val userId = AuthenticatedUser.getUserId()
        val command = ArchiveQuickNoteCommand(userId = userId, quickNoteId = QuickNoteId(quickNoteId))
        val quickNote = quickNoteCommandPort.archiveQuickNote(command)
        return ResponseEntity.ok(QuickNoteResponse.from(quickNote))
    }

    @DeleteMapping("/quick-notes/{quickNoteId}")
    fun deleteQuickNote(
        @PathVariable quickNoteId: UUID
    ): ResponseEntity<Void> {
        val userId = AuthenticatedUser.getUserId()
        val command = DeleteQuickNoteCommand(userId = userId, quickNoteId = QuickNoteId(quickNoteId))
        quickNoteCommandPort.deleteQuickNote(command)
        return ResponseEntity.noContent().build()
    }
}
