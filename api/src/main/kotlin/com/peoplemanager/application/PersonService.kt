package com.peoplemanager.application

import com.peoplemanager.application.commands.AddRememberItemCommand
import com.peoplemanager.application.commands.CreatePersonCommand
import com.peoplemanager.application.commands.DeletePersonCommand
import com.peoplemanager.application.commands.RemoveRememberItemCommand
import com.peoplemanager.application.commands.ReorderRememberItemsCommand
import com.peoplemanager.application.commands.RestorePersonCommand
import com.peoplemanager.application.commands.SetMoraleCommand
import com.peoplemanager.application.commands.UpdatePersonCommand
import com.peoplemanager.domain.AuditLogEntry
import com.peoplemanager.application.ports.PersonCommandPort
import com.peoplemanager.application.ports.PersonQueryPort
import com.peoplemanager.application.ports.PersonRepository
import com.peoplemanager.application.queries.GetPersonQuery
import com.peoplemanager.application.queries.ListDeletedPersonsQuery
import com.peoplemanager.application.queries.ListPersonsQuery
import com.peoplemanager.domain.MoraleStatus
import com.peoplemanager.domain.Person
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.PinnedRememberItem
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class PersonService(
    private val personRepository: PersonRepository,
    private val auditLogService: AuditLogService
) : PersonCommandPort, PersonQueryPort {

    override fun createPerson(command: CreatePersonCommand): Person {
        val person = Person(
            id = PersonId.generate(),
            userId = command.userId,
            name = command.name,
            preferredName = command.preferredName,
            roleTitle = command.roleTitle,
            timezone = command.timezone,
            startDate = command.startDate,
            email = command.email,
            tags = command.tags,
            moraleStatus = MoraleStatus.UNKNOWN,
            moraleNote = null,
            pinnedRememberItems = emptyList()
        )
        val saved = personRepository.save(person)
        auditLogService.record(AuditLogEntry.personCreated(command.userId, saved.id, saved.name))
        return saved
    }

    override fun updatePerson(command: UpdatePersonCommand): Person {
        val existing = personRepository.findByIdAndUserId(command.personId, command.userId)
            ?: throw PersonNotFoundException(command.personId)

        val updated = existing.copy(
            name = command.name,
            preferredName = command.preferredName,
            roleTitle = command.roleTitle,
            timezone = command.timezone,
            startDate = command.startDate,
            email = command.email,
            tags = command.tags,
            updatedAt = Instant.now()
        )
        val saved = personRepository.save(updated)
        auditLogService.record(AuditLogEntry.personUpdated(command.userId, saved.id, saved.name))
        return saved
    }

    override fun deletePerson(command: DeletePersonCommand) {
        val person = personRepository.findByIdAndUserId(command.personId, command.userId)
            ?: throw PersonNotFoundException(command.personId)
        val deleted = personRepository.softDeleteByIdAndUserId(command.personId, command.userId)
        if (!deleted) throw PersonNotFoundException(command.personId)
        auditLogService.record(AuditLogEntry.personDeleted(command.userId, command.personId, person.name))
    }

    override fun restorePerson(command: RestorePersonCommand): Person {
        val restored = personRepository.restoreByIdAndUserId(command.personId, command.userId)
        if (!restored) throw PersonNotFoundException(command.personId)
        val person = personRepository.findByIdAndUserId(command.personId, command.userId)
            ?: throw PersonNotFoundException(command.personId)
        auditLogService.record(AuditLogEntry.personRestored(command.userId, person.id, person.name))
        return person
    }

    override fun setMorale(command: SetMoraleCommand): Person {
        val person = personRepository.findByIdAndUserId(command.personId, command.userId)
            ?: throw PersonNotFoundException(command.personId)

        val updated = person.updateMorale(command.status, command.note)
        return personRepository.save(updated)
    }

    override fun addRememberItem(command: AddRememberItemCommand): List<PinnedRememberItem> {
        val person = personRepository.findByIdAndUserId(command.personId, command.userId)
            ?: throw PersonNotFoundException(command.personId)

        val updated = person.addRememberItem(command.text)
        val saved = personRepository.save(updated)
        return saved.pinnedRememberItems
    }

    override fun removeRememberItem(command: RemoveRememberItemCommand): List<PinnedRememberItem> {
        val person = personRepository.findByIdAndUserId(command.personId, command.userId)
            ?: throw PersonNotFoundException(command.personId)

        val updated = person.removeRememberItem(command.itemId)
        val saved = personRepository.save(updated)
        return saved.pinnedRememberItems
    }

    override fun reorderRememberItems(command: ReorderRememberItemsCommand): List<PinnedRememberItem> {
        val person = personRepository.findByIdAndUserId(command.personId, command.userId)
            ?: throw PersonNotFoundException(command.personId)

        val updated = person.reorderRememberItems(command.orderedIds)
        val saved = personRepository.save(updated)
        return saved.pinnedRememberItems
    }

    override fun getPerson(query: GetPersonQuery): Person {
        return personRepository.findByIdAndUserId(query.personId, query.userId)
            ?: throw PersonNotFoundException(query.personId)
    }

    override fun listPersons(query: ListPersonsQuery): Page<Person> {
        val pageable = PageRequest.of(query.page, query.size, Sort.by(Sort.Direction.ASC, "name"))
        return personRepository.findAllByUserId(query.userId, pageable, query.tagFilter, query.moraleFilter)
    }

    override fun listDeletedPersons(query: ListDeletedPersonsQuery): Page<Person> {
        val pageable = PageRequest.of(query.page, query.size)
        return personRepository.findAllDeletedByUserId(query.userId, pageable)
    }
}
