package com.peoplemanager.application

import com.peoplemanager.application.commands.*
import com.peoplemanager.application.ports.QuickNoteCommandPort
import com.peoplemanager.application.ports.QuickNoteQueryPort
import com.peoplemanager.application.ports.QuickNoteRepository
import com.peoplemanager.application.ports.PersonRepository
import com.peoplemanager.application.ports.OneOnOneEntryRepository
import com.peoplemanager.application.queries.GetQuickNoteQuery
import com.peoplemanager.application.queries.ListQuickNotesQuery
import com.peoplemanager.domain.QuickNote
import com.peoplemanager.domain.QuickNoteId
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
    private val oneOnOneEntryRepository: OneOnOneEntryRepository
) : QuickNoteCommandPort, QuickNoteQueryPort {

    override fun createQuickNote(command: CreateQuickNoteCommand): QuickNote {
        // If personId is provided, verify it belongs to the user
        if (command.personId != null) {
            personRepository.findByIdAndUserId(command.personId, command.userId)
                ?: throw PersonNotFoundException(command.personId)
        }

        val quickNote = QuickNote(
            id = QuickNoteId.generate(),
            userId = command.userId,
            personId = command.personId,
            text = command.text,
            sensitive = command.sensitive
        )

        return quickNoteRepository.save(quickNote)
    }

    override fun updateQuickNote(command: UpdateQuickNoteCommand): QuickNote {
        val existing = quickNoteRepository.findByIdAndUserId(command.quickNoteId, command.userId)
            ?: throw QuickNoteNotFoundException(command.quickNoteId)

        // If assigning to a new person, verify ownership
        if (command.personId != null) {
            personRepository.findByIdAndUserId(command.personId, command.userId)
                ?: throw PersonNotFoundException(command.personId)
        }

        val updated = existing.copy(
            text = command.text ?: existing.text,
            personId = command.personId ?: existing.personId,
            sensitive = command.sensitive ?: existing.sensitive,
            updatedAt = java.time.Instant.now()
        )

        // Validate text is not blank if provided
        if (command.text != null) {
            require(command.text.isNotBlank()) { "Quick note text must not be blank" }
        }

        return quickNoteRepository.save(updated)
    }

    override fun assignToPerson(command: AssignQuickNoteToPersonCommand): QuickNote {
        val existing = quickNoteRepository.findByIdAndUserId(command.quickNoteId, command.userId)
            ?: throw QuickNoteNotFoundException(command.quickNoteId)

        personRepository.findByIdAndUserId(command.personId, command.userId)
            ?: throw PersonNotFoundException(command.personId)

        val updated = existing.assignToPerson(command.personId)
        return quickNoteRepository.save(updated)
    }

    override fun attachQuickNote(command: AttachQuickNoteCommand): QuickNote {
        val existing = quickNoteRepository.findByIdAndUserId(command.quickNoteId, command.userId)
            ?: throw QuickNoteNotFoundException(command.quickNoteId)

        // Validate the 1:1 entry exists and belongs to the user
        oneOnOneEntryRepository.findByIdAndUserId(command.entryId, command.userId)
            ?: throw OneOnOneEntryNotFoundException(command.entryId)

        val updated = existing.markAttached(command.entryId)
        return quickNoteRepository.save(updated)
    }

    override fun convertQuickNote(command: ConvertQuickNoteCommand): QuickNote {
        val existing = quickNoteRepository.findByIdAndUserId(command.quickNoteId, command.userId)
            ?: throw QuickNoteNotFoundException(command.quickNoteId)

        val updated = existing.markConverted()
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
    }

    override fun getQuickNote(query: GetQuickNoteQuery): QuickNote {
        return quickNoteRepository.findByIdAndUserId(query.quickNoteId, query.userId)
            ?: throw QuickNoteNotFoundException(query.quickNoteId)
    }

    override fun listQuickNotes(query: ListQuickNotesQuery): Page<QuickNote> {
        val pageable = PageRequest.of(query.page, query.size, Sort.by(Sort.Direction.DESC, "createdAt"))

        return when {
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
