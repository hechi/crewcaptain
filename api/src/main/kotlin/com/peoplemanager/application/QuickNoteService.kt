package com.peoplemanager.application

import com.peoplemanager.application.commands.*
import com.peoplemanager.application.port.input.QuickNoteCommandPort
import com.peoplemanager.application.port.input.QuickNoteQueryPort
import com.peoplemanager.application.port.output.QuickNoteRepository
import com.peoplemanager.application.port.output.PersonRepository
import com.peoplemanager.application.port.output.OneOnOneEntryRepository
import com.peoplemanager.application.port.output.ActionItemRepository
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

        // Personal (self-assigned) notes keep newest-on-top: a new note is placed strictly
        // above the current top note so it sorts first by displayOrder ascending.
        val displayOrder = if (command.selfAssigned) {
            val currentMin = quickNoteRepository.findMinSelfAssignedDisplayOrder(command.userId)
            if (currentMin == null) 0 else currentMin - 1
        } else {
            0
        }

        val quickNote = QuickNote(
            id = QuickNoteId.generate(),
            userId = command.userId,
            personId = command.personId,
            text = command.text,
            sensitive = command.sensitive,
            selfAssigned = command.selfAssigned,
            tag = command.tag,
            displayOrder = displayOrder
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

        if (command.updateTag) {
            require(command.tag == null || command.tag.length <= 30) { "Tag must be 30 characters or fewer" }
        }

        val updated = existing.copy(
            text = command.text ?: existing.text,
            personId = command.personId ?: existing.personId,
            sensitive = command.sensitive ?: existing.sensitive,
            tag = if (command.updateTag) command.tag else existing.tag,
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

        // Resolve the target person for the action item. A null personId produces a
        // person-less (manager's own) TODO, which is how self-assigned notes convert.
        // When no person is given on the command, fall back to the note's own person (if any).
        val targetPersonId = command.personId ?: existing.personId

        // Validate the person belongs to the user when one is involved.
        if (targetPersonId != null) {
            personRepository.findByIdAndUserId(targetPersonId, command.userId)
                ?: throw PersonNotFoundException(targetPersonId)
        }

        // Create an action item from the quick note text.
        val actionItem = ActionItem(
            id = ActionItemId.generate(),
            userId = command.userId,
            personId = targetPersonId,
            title = existing.text.take(500), // Action item title max 500 chars
            description = if (existing.text.length > 500) existing.text else null,
            ownerType = ActionItemOwnerType.MANAGER,
            dueDate = command.dueDate,
            originatingQuickNoteId = existing.id
        )
        actionItemRepository.save(actionItem)

        // Assign the note to the person if one was explicitly provided and the note is
        // not self-assigned; otherwise leave the note's assignment untouched so the
        // self-assigned invariant (selfAssigned implies no personId) is preserved.
        val noteToConvert = if (command.personId != null && !existing.selfAssigned && existing.personId == null) {
            existing.assignToPerson(command.personId)
        } else {
            existing
        }

        val updated = noteToConvert.markConverted()
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

    override fun moveQuickNote(command: MoveQuickNoteCommand): QuickNote {
        // Load (and user-scope) the target note first so a foreign note surfaces as 404.
        val target = quickNoteRepository.findByIdAndUserId(command.quickNoteId, command.userId)
            ?: throw QuickNoteNotFoundException(command.quickNoteId)

        // Reordering only applies to the manual personal-notes list (self-assigned, INBOX).
        val ordered = quickNoteRepository.findSelfAssignedOrdered(command.userId, QuickNoteStatus.INBOX)
        val index = ordered.indexOfFirst { it.id == target.id }

        // If the note isn't part of the ordered personal list, there's nothing to reorder.
        if (index < 0) return target

        val neighborIndex = when (command.direction) {
            MoveDirection.UP -> index - 1
            MoveDirection.DOWN -> index + 1
        }

        // At the edge already — no-op (keeps move buttons harmless at the ends of the list).
        if (neighborIndex < 0 || neighborIndex >= ordered.size) return target

        val neighbor = ordered[neighborIndex]

        // Swap the two notes' display orders. Both saves are user-scoped by construction
        // (both notes were loaded from this user's ordered list / findByIdAndUserId).
        val movedTarget = target.withDisplayOrder(neighbor.displayOrder)
        val movedNeighbor = neighbor.withDisplayOrder(target.displayOrder)
        quickNoteRepository.save(movedNeighbor)
        return quickNoteRepository.save(movedTarget)
    }

    override fun getQuickNote(query: GetQuickNoteQuery): QuickNote {
        return quickNoteRepository.findByIdAndUserId(query.quickNoteId, query.userId)
            ?: throw QuickNoteNotFoundException(query.quickNoteId)
    }

    override fun listQuickNotes(query: ListQuickNotesQuery): Page<QuickNote> {
        val pageable = PageRequest.of(query.page, query.size, Sort.by(Sort.Direction.DESC, "createdAt"))
        // Personal (self-assigned) notes honor the user's manual ordering: lower displayOrder
        // sorts higher, with newest-first as the tie-breaker so new notes land on top.
        val selfAssignedPageable = PageRequest.of(
            query.page,
            query.size,
            Sort.by(Sort.Order.asc("displayOrder"), Sort.Order.desc("createdAt"))
        )

        return when {
            query.selfAssigned != null && query.tag != null ->
                quickNoteRepository.findAllByUserIdAndSelfAssignedAndTag(
                    query.userId, query.selfAssigned, query.tag, selfAssignedPageable
                )
            query.selfAssigned != null && query.status != null ->
                quickNoteRepository.findAllByUserIdAndSelfAssignedAndStatus(
                    query.userId, query.selfAssigned, query.status, selfAssignedPageable
                )
            query.selfAssigned != null ->
                quickNoteRepository.findAllByUserIdAndSelfAssigned(query.userId, query.selfAssigned, selfAssignedPageable)
            query.tag != null ->
                quickNoteRepository.findAllByUserIdAndTag(query.userId, query.tag, pageable)
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
