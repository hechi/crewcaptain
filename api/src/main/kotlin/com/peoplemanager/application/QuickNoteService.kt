package com.peoplemanager.application

import com.peoplemanager.application.commands.*
import com.peoplemanager.application.ports.QuickNoteCommandPort
import com.peoplemanager.application.ports.QuickNoteQueryPort
import com.peoplemanager.application.ports.QuickNoteRepository
import com.peoplemanager.application.ports.PersonRepository
import com.peoplemanager.application.ports.OneOnOneEntryRepository
import com.peoplemanager.application.ports.ActionItemRepository
import com.peoplemanager.application.queries.GetQuickNoteQuery
import com.peoplemanager.application.queries.ListQuickNotesQuery
import com.peoplemanager.domain.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class QuickNoteService(
    private val quickNoteRepository: QuickNoteRepository,
    private val personRepository: PersonRepository,
    private val oneOnOneEntryRepository: OneOnOneEntryRepository,
    private val actionItemRepository: ActionItemRepository,
    private val auditLogService: AuditLogService
) : QuickNoteCommandPort, QuickNoteQueryPort {

    override fun createQuickNote(command: CreateQuickNoteCommand): QuickNote {
        if (command.personId != null) {
            personRepository.findByIdAndUserId(command.personId, command.userId)
                ?: throw PersonNotFoundException(command.personId)
        }

        val quickNote = QuickNote(
            id = QuickNoteId.generate(),
            userId = command.userId,
            personId = command.personId,
            text = command.text,
            sensitive = command.sensitive,
            selfAssigned = command.selfAssigned
        )

        val saved = quickNoteRepository.save(quickNote)
        auditLogService.record(AuditLogEntry.quickNoteCreated(command.userId, saved.id))
        return saved
    }

    override fun updateQuickNote(command: UpdateQuickNoteCommand): QuickNote {
        val existing = quickNoteRepository.findByIdAndUserId(command.quickNoteId, command.userId)
            ?: throw QuickNoteNotFoundException(command.quickNoteId)

        if (command.personId != null) {
            personRepository.findByIdAndUserId(command.personId, command.userId)
                ?: throw PersonNotFoundException(command.personId)
        }

        if (command.text != null) {
            require(command.text.isNotBlank()) { "Quick note text must not be blank" }
        }

        val updated = existing.copy(
            text = command.text ?: existing.text,
            personId = command.personId ?: existing.personId,
            sensitive = command.sensitive ?: existing.sensitive,
            updatedAt = java.time.Instant.now()
        )

        return quickNoteRepository.save(updated)
    }

    override fun assignToPerson(command: AssignQuickNoteToPersonCommand): QuickNote {
        val existing = quickNoteRepository.findByIdAndUserId(command.quickNoteId, command.userId)
            ?: throw QuickNoteNotFoundException(command.quickNoteId)

        personRepository.findByIdAndUserId(command.personId, command.userId)
            ?: throw PersonNotFoundException(command.personId)

        // assignToPerson on the domain model clears selfAssigned automatically
        val updated = existing.assignToPerson(command.personId)
        return quickNoteRepository.save(updated)
    }

    override fun assignToSelf(command: AssignQuickNoteToSelfCommand): QuickNote {
        val existing = quickNoteRepository.findByIdAndUserId(command.quickNoteId, command.userId)
            ?: throw QuickNoteNotFoundException(command.quickNoteId)

        val updated = existing.markSelfAssigned()
        return quickNoteRepository.save(updated)
    }

    override fun attachQuickNote(command: AttachQuickNoteCommand): QuickNote {
        val existing = quickNoteRepository.findByIdAndUserId(command.quickNoteId, command.userId)
            ?: throw QuickNoteNotFoundException(command.quickNoteId)

        // Validate status first — fail fast before doing any work
        require(existing.status == QuickNoteStatus.INBOX) {
            "Can only attach a quick note with status INBOX, current status is ${existing.status}"
        }

        // Validate the 1:1 entry exists and belongs to the user
        val entry = oneOnOneEntryRepository.findByIdAndUserId(command.entryId, command.userId)
            ?: throw OneOnOneEntryNotFoundException(command.entryId)

        // Add the quick note text as an agenda item to the 1:1 entry
        val newAgendaItem = AgendaItem(
            id = AgendaItemId.generate(),
            text = existing.text,
            checked = false,
            displayOrder = entry.agendaItems.size
        )
        val updatedEntry = entry.updateAgendaItems(entry.agendaItems + newAgendaItem)
        oneOnOneEntryRepository.save(updatedEntry)

        // Also assign the person from the entry if not already assigned
        val noteWithPerson = if (existing.personId == null) {
            existing.assignToPerson(entry.personId)
        } else {
            existing
        }

        val updated = noteWithPerson.markAttached(command.entryId)
        return quickNoteRepository.save(updated)
    }

    override fun convertQuickNote(command: ConvertQuickNoteCommand): QuickNote {
        val existing = quickNoteRepository.findByIdAndUserId(command.quickNoteId, command.userId)
            ?: throw QuickNoteNotFoundException(command.quickNoteId)

        // Validate the person belongs to the user
        personRepository.findByIdAndUserId(command.personId, command.userId)
            ?: throw PersonNotFoundException(command.personId)

        // Create an action item from the quick note text
        val actionItem = ActionItem(
            id = ActionItemId.generate(),
            userId = command.userId,
            personId = command.personId,
            title = existing.text.take(500), // Action item title max 500 chars
            description = if (existing.text.length > 500) existing.text else null,
            ownerType = ActionItemOwnerType.MANAGER
        )
        actionItemRepository.save(actionItem)

        // Assign person if not already assigned, then mark as converted
        val noteWithPerson = if (existing.personId == null) {
            existing.assignToPerson(command.personId)
        } else {
            existing
        }

        val updated = noteWithPerson.markConverted()
        return quickNoteRepository.save(updated)
    }

    override fun archiveQuickNote(command: ArchiveQuickNoteCommand): QuickNote {
        val existing = quickNoteRepository.findByIdAndUserId(command.quickNoteId, command.userId)
            ?: throw QuickNoteNotFoundException(command.quickNoteId)

        val updated = existing.archive()
        return quickNoteRepository.save(updated)
    }

    override fun deleteQuickNote(command: DeleteQuickNoteCommand) {
        val deleted = quickNoteRepository.deleteByIdAndUserId(command.quickNoteId, command.userId)
        if (!deleted) throw QuickNoteNotFoundException(command.quickNoteId)
        auditLogService.record(AuditLogEntry.quickNoteDeleted(command.userId, command.quickNoteId))
    }

    override fun getQuickNote(query: GetQuickNoteQuery): QuickNote {
        return quickNoteRepository.findByIdAndUserId(query.quickNoteId, query.userId)
            ?: throw QuickNoteNotFoundException(query.quickNoteId)
    }

    override fun listQuickNotes(query: ListQuickNotesQuery): Page<QuickNote> {
        val pageable = PageRequest.of(query.page, query.size, Sort.by(Sort.Direction.DESC, "createdAt"))

        return when {
            query.selfAssigned != null && query.status != null ->
                quickNoteRepository.findAllByUserIdAndSelfAssignedAndStatus(
                    query.userId, query.selfAssigned, query.status, pageable
                )
            query.selfAssigned != null ->
                quickNoteRepository.findAllByUserIdAndSelfAssigned(query.userId, query.selfAssigned, pageable)
            query.status != null && query.personId != null ->
                quickNoteRepository.findAllByUserIdAndStatusAndPersonId(
                    query.userId, query.status, query.personId, pageable
                )
            query.status != null ->
                quickNoteRepository.findAllByUserIdAndStatus(query.userId, query.status, pageable)
            query.personId != null ->
                quickNoteRepository.findAllByUserIdAndPersonId(query.userId, query.personId, pageable)
            else ->
                quickNoteRepository.findAllByUserId(query.userId, pageable)
        }
    }
}
