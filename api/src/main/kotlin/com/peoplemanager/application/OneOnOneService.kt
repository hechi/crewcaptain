package com.peoplemanager.application

import com.peoplemanager.application.commands.CreateOneOnOneEntryCommand
import com.peoplemanager.application.commands.DeleteOneOnOneEntryCommand
import com.peoplemanager.application.commands.UpdateOneOnOneEntryCommand
import com.peoplemanager.application.commands.UpsertOneOnOneSeriesCommand
import com.peoplemanager.application.port.input.OneOnOneCommandPort
import com.peoplemanager.application.port.output.OneOnOneEntryRepository
import com.peoplemanager.application.port.input.OneOnOneQueryPort
import com.peoplemanager.application.port.output.OneOnOneSeriesRepository
import com.peoplemanager.application.port.output.PersonRepository
import com.peoplemanager.application.queries.GetLastOneOnOneDateQuery
import com.peoplemanager.application.queries.GetOneOnOneEntryQuery
import com.peoplemanager.application.queries.GetOneOnOneSeriesQuery
import com.peoplemanager.application.queries.ListOneOnOneEntriesQuery
import com.peoplemanager.domain.AgendaItem
import com.peoplemanager.domain.AgendaItemId
import com.peoplemanager.domain.AuditLogEntry
import com.peoplemanager.domain.OneOnOneEntry
import com.peoplemanager.domain.OneOnOneEntryId
import com.peoplemanager.domain.OneOnOneSeries
import com.peoplemanager.domain.OneOnOneSeriesId
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
@Transactional
class OneOnOneService(
    private val personRepository: PersonRepository,
    private val seriesRepository: OneOnOneSeriesRepository,
    private val entryRepository: OneOnOneEntryRepository,
    private val auditLogService: AuditLogService
) : OneOnOneCommandPort, OneOnOneQueryPort {

    override fun upsertSeries(command: UpsertOneOnOneSeriesCommand): OneOnOneSeries {
        // Verify person belongs to user
        personRepository.findByIdAndUserId(command.personId, command.userId)
            ?: throw PersonNotFoundException(command.personId)

        val existing = seriesRepository.findByUserIdAndPersonId(command.userId, command.personId)

        val series = if (existing != null) {
            existing.copy(
                cadenceType = command.cadenceType,
                customIntervalDays = command.customIntervalDays,
                templateMarkdown = command.templateMarkdown,
                updatedAt = Instant.now()
            )
        } else {
            OneOnOneSeries(
                id = OneOnOneSeriesId.generate(),
                userId = command.userId,
                personId = command.personId,
                cadenceType = command.cadenceType,
                customIntervalDays = command.customIntervalDays,
                templateMarkdown = command.templateMarkdown
            )
        }

        return seriesRepository.save(series)
    }

    override fun createEntry(command: CreateOneOnOneEntryCommand): OneOnOneEntry {
        // Verify person belongs to user
        val person = personRepository.findByIdAndUserId(command.personId, command.userId)
            ?: throw PersonNotFoundException(command.personId)

        // Apply template if notes not provided
        val notes = if (command.notesMarkdown == null) {
            val series = seriesRepository.findByUserIdAndPersonId(command.userId, command.personId)
            series?.templateMarkdown
        } else {
            command.notesMarkdown
        }

        val agendaItems = command.agendaItems.mapIndexed { index, input ->
            AgendaItem(
                id = AgendaItemId.generate(),
                text = input.text,
                checked = input.checked,
                displayOrder = index
            )
        }

        val entry = OneOnOneEntry(
            id = OneOnOneEntryId.generate(),
            userId = command.userId,
            personId = command.personId,
            meetingDate = command.meetingDate,
            agendaItems = agendaItems,
            notesMarkdown = notes,
            outcomesMarkdown = command.outcomesMarkdown,
            sensitive = command.sensitive
        )

        val saved = entryRepository.save(entry)
        auditLogService.record(AuditLogEntry.oneOnOneEntryCreated(command.userId, saved.id, command.personId, person.name))
        return saved
    }

    override fun updateEntry(command: UpdateOneOnOneEntryCommand): OneOnOneEntry {
        val existing = entryRepository.findByIdAndUserIdAndPersonId(
            command.entryId, command.userId, command.personId
        ) ?: throw OneOnOneEntryNotFoundException(command.entryId)

        val person = personRepository.findByIdAndUserId(command.personId, command.userId)
            ?: throw PersonNotFoundException(command.personId)

        val updatedAgendaItems = command.agendaItems?.mapIndexed { index, input ->
            AgendaItem(
                id = AgendaItemId.generate(),
                text = input.text,
                checked = input.checked,
                displayOrder = index
            )
        }

        val updated = existing.copy(
            meetingDate = command.meetingDate ?: existing.meetingDate,
            agendaItems = updatedAgendaItems ?: existing.agendaItems,
            notesMarkdown = command.notesMarkdown ?: existing.notesMarkdown,
            outcomesMarkdown = command.outcomesMarkdown ?: existing.outcomesMarkdown,
            sensitive = command.sensitive ?: existing.sensitive,
            updatedAt = Instant.now()
        )

        val saved = entryRepository.save(updated)
        auditLogService.record(AuditLogEntry.oneOnOneEntryUpdated(command.userId, saved.id, command.personId, person.name))
        return saved
    }

    override fun deleteEntry(command: DeleteOneOnOneEntryCommand) {
        val person = personRepository.findByIdAndUserId(command.personId, command.userId)
            ?: throw PersonNotFoundException(command.personId)
        val deleted = entryRepository.deleteByIdAndUserIdAndPersonId(
            command.entryId, command.userId, command.personId
        )
        if (!deleted) throw OneOnOneEntryNotFoundException(command.entryId)
        auditLogService.record(AuditLogEntry.oneOnOneEntryDeleted(command.userId, command.entryId, command.personId, person.name))
    }

    override fun getSeries(query: GetOneOnOneSeriesQuery): OneOnOneSeries? {
        // Verify person belongs to user
        personRepository.findByIdAndUserId(query.personId, query.userId)
            ?: throw PersonNotFoundException(query.personId)

        return seriesRepository.findByUserIdAndPersonId(query.userId, query.personId)
    }

    override fun getEntry(query: GetOneOnOneEntryQuery): OneOnOneEntry {
        return entryRepository.findByIdAndUserIdAndPersonId(
            query.entryId, query.userId, query.personId
        ) ?: throw OneOnOneEntryNotFoundException(query.entryId)
    }

    override fun listEntries(query: ListOneOnOneEntriesQuery): Page<OneOnOneEntry> {
        // Verify person belongs to user
        personRepository.findByIdAndUserId(query.personId, query.userId)
            ?: throw PersonNotFoundException(query.personId)

        val pageable = PageRequest.of(query.page, query.size, Sort.by(Sort.Direction.DESC, "meetingDate"))
        return entryRepository.findAllByUserIdAndPersonId(query.userId, query.personId, pageable)
    }

    override fun getLastOneOnOneDate(query: GetLastOneOnOneDateQuery): Instant? {
        return entryRepository.findLatestMeetingDate(query.userId, query.personId)
    }
}
