package com.peoplemanager.application

import com.peoplemanager.application.commands.CreateKudosCommand
import com.peoplemanager.application.commands.DeleteKudosCommand
import com.peoplemanager.application.port.input.KudosCommandPort
import com.peoplemanager.application.port.input.KudosQueryPort
import com.peoplemanager.application.port.output.KudosRepository
import com.peoplemanager.application.port.output.PersonRepository
import com.peoplemanager.application.queries.GetKudosQuery
import com.peoplemanager.application.queries.ListAllKudosQuery
import com.peoplemanager.application.queries.ListKudosByPersonQuery
import com.peoplemanager.domain.Kudos
import com.peoplemanager.domain.KudosId
import com.peoplemanager.domain.AuditLogEntry
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class KudosService(
    private val personRepository: PersonRepository,
    private val kudosRepository: KudosRepository,
    private val auditLogService: AuditLogService
) : KudosCommandPort, KudosQueryPort {

    override fun createKudos(command: CreateKudosCommand): Kudos {
        // Verify person belongs to user
        val person = personRepository.findByIdAndUserId(command.personId, command.userId)
            ?: throw PersonNotFoundException(command.personId)

        val kudos = Kudos(
            id = KudosId.generate(),
            userId = command.userId,
            personId = command.personId,
            date = command.date,
            text = command.text,
            tags = command.tags
        )

        val saved = kudosRepository.save(kudos)
        auditLogService.record(AuditLogEntry.kudosCreated(command.userId, saved.id, command.personId, person.name))
        return saved
    }

    override fun deleteKudos(command: DeleteKudosCommand) {
        val person = personRepository.findByIdAndUserId(command.personId, command.userId)
            ?: throw PersonNotFoundException(command.personId)
        val deleted = kudosRepository.deleteByIdAndUserIdAndPersonId(
            command.kudosId, command.userId, command.personId
        )
        if (!deleted) throw KudosNotFoundException(command.kudosId)
        auditLogService.record(AuditLogEntry.kudosDeleted(command.userId, command.kudosId, command.personId, person.name))
    }

    override fun getKudos(query: GetKudosQuery): Kudos {
        return kudosRepository.findByIdAndUserIdAndPersonId(
            query.kudosId, query.userId, query.personId
        ) ?: throw KudosNotFoundException(query.kudosId)
    }

    override fun listKudosByPerson(query: ListKudosByPersonQuery): Page<Kudos> {
        // Verify person belongs to user
        personRepository.findByIdAndUserId(query.personId, query.userId)
            ?: throw PersonNotFoundException(query.personId)

        val pageable = PageRequest.of(query.page, query.size, Sort.by(Sort.Direction.DESC, "date"))
        return kudosRepository.findAllByUserIdAndPersonId(query.userId, query.personId, pageable)
    }

    override fun listAllKudos(query: ListAllKudosQuery): Page<Kudos> {
        val pageable = PageRequest.of(query.page, query.size, Sort.by(Sort.Direction.DESC, "date"))
        return kudosRepository.findAllByUserId(query.userId, pageable)
    }
}
