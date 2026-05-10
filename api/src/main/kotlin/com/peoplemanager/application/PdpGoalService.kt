package com.peoplemanager.application

import com.peoplemanager.application.commands.*
import com.peoplemanager.application.ports.PdpGoalCommandPort
import com.peoplemanager.application.ports.PdpGoalQueryPort
import com.peoplemanager.application.ports.PdpGoalRepository
import com.peoplemanager.application.ports.PdpUpdateRepository
import com.peoplemanager.application.ports.PersonRepository
import com.peoplemanager.application.queries.CountActivePdpGoalsQuery
import com.peoplemanager.application.queries.GetPdpGoalQuery
import com.peoplemanager.application.queries.ListPdpGoalsByPersonQuery
import com.peoplemanager.application.queries.ListPdpUpdatesByGoalQuery
import com.peoplemanager.domain.PdpGoal
import com.peoplemanager.domain.PdpGoalId
import com.peoplemanager.domain.PdpUpdate
import com.peoplemanager.domain.PdpUpdateId
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class PdpGoalService(
    private val personRepository: PersonRepository,
    private val pdpGoalRepository: PdpGoalRepository,
    private val pdpUpdateRepository: PdpUpdateRepository
) : PdpGoalCommandPort, PdpGoalQueryPort {

    override fun createPdpGoal(command: CreatePdpGoalCommand): PdpGoal {
        personRepository.findByIdAndUserId(command.personId, command.userId)
            ?: throw PersonNotFoundException(command.personId)

        val goal = PdpGoal(
            id = PdpGoalId.generate(),
            userId = command.userId,
            personId = command.personId,
            title = command.title,
            description = command.description,
            targetDate = command.targetDate
        )

        return pdpGoalRepository.save(goal)
    }

    override fun updatePdpGoal(command: UpdatePdpGoalCommand): PdpGoal {
        val existing = pdpGoalRepository.findByIdAndUserIdAndPersonId(
            command.goalId, command.userId, command.personId
        ) ?: throw PdpGoalNotFoundException(command.goalId)

        val updated = existing.updateDetails(
            title = command.title,
            description = command.description,
            targetDate = command.targetDate
        )

        return pdpGoalRepository.save(updated)
    }

    override fun achievePdpGoal(command: AchievePdpGoalCommand): PdpGoal {
        val existing = pdpGoalRepository.findByIdAndUserIdAndPersonId(
            command.goalId, command.userId, command.personId
        ) ?: throw PdpGoalNotFoundException(command.goalId)

        val achieved = existing.achieve()
        return pdpGoalRepository.save(achieved)
    }

    override fun pausePdpGoal(command: PausePdpGoalCommand): PdpGoal {
        val existing = pdpGoalRepository.findByIdAndUserIdAndPersonId(
            command.goalId, command.userId, command.personId
        ) ?: throw PdpGoalNotFoundException(command.goalId)

        val paused = existing.pause()
        return pdpGoalRepository.save(paused)
    }

    override fun dropPdpGoal(command: DropPdpGoalCommand): PdpGoal {
        val existing = pdpGoalRepository.findByIdAndUserIdAndPersonId(
            command.goalId, command.userId, command.personId
        ) ?: throw PdpGoalNotFoundException(command.goalId)

        val dropped = existing.drop()
        return pdpGoalRepository.save(dropped)
    }

    override fun resumePdpGoal(command: ResumePdpGoalCommand): PdpGoal {
        val existing = pdpGoalRepository.findByIdAndUserIdAndPersonId(
            command.goalId, command.userId, command.personId
        ) ?: throw PdpGoalNotFoundException(command.goalId)

        val resumed = existing.resume()
        return pdpGoalRepository.save(resumed)
    }

    override fun deletePdpGoal(command: DeletePdpGoalCommand) {
        val deleted = pdpGoalRepository.deleteByIdAndUserIdAndPersonId(
            command.goalId, command.userId, command.personId
        )
        if (!deleted) throw PdpGoalNotFoundException(command.goalId)
    }

    override fun addPdpUpdate(command: AddPdpUpdateCommand): PdpUpdate {
        // Verify goal belongs to user and person
        pdpGoalRepository.findByIdAndUserIdAndPersonId(
            command.goalId, command.userId, command.personId
        ) ?: throw PdpGoalNotFoundException(command.goalId)

        val update = PdpUpdate(
            id = PdpUpdateId.generate(),
            goalId = command.goalId,
            userId = command.userId,
            textMarkdown = command.textMarkdown,
            sensitive = command.sensitive
        )

        return pdpUpdateRepository.save(update)
    }

    override fun deletePdpUpdate(command: DeletePdpUpdateCommand) {
        // Verify goal belongs to user and person
        pdpGoalRepository.findByIdAndUserIdAndPersonId(
            command.goalId, command.userId, command.personId
        ) ?: throw PdpGoalNotFoundException(command.goalId)

        val deleted = pdpUpdateRepository.deleteByIdAndGoalIdAndUserId(
            command.updateId, command.goalId, command.userId
        )
        if (!deleted) throw PdpUpdateNotFoundException(command.updateId)
    }

    override fun getPdpGoal(query: GetPdpGoalQuery): PdpGoal {
        return pdpGoalRepository.findByIdAndUserIdAndPersonId(
            query.goalId, query.userId, query.personId
        ) ?: throw PdpGoalNotFoundException(query.goalId)
    }

    override fun listPdpGoalsByPerson(query: ListPdpGoalsByPersonQuery): Page<PdpGoal> {
        personRepository.findByIdAndUserId(query.personId, query.userId)
            ?: throw PersonNotFoundException(query.personId)

        val pageable = PageRequest.of(query.page, query.size, Sort.by(Sort.Direction.DESC, "createdAt"))
        return pdpGoalRepository.findAllByUserIdAndPersonId(
            query.userId, query.personId, query.status, pageable
        )
    }

    override fun listPdpUpdatesByGoal(query: ListPdpUpdatesByGoalQuery): Page<PdpUpdate> {
        // Verify goal belongs to user and person
        pdpGoalRepository.findByIdAndUserIdAndPersonId(
            query.goalId, query.userId, query.personId
        ) ?: throw PdpGoalNotFoundException(query.goalId)

        val pageable = PageRequest.of(query.page, query.size, Sort.by(Sort.Direction.DESC, "createdAt"))
        return pdpUpdateRepository.findAllByGoalIdAndUserId(query.goalId, query.userId, pageable)
    }

    override fun countActivePdpGoals(query: CountActivePdpGoalsQuery): Long {
        return pdpGoalRepository.countActiveByUserIdAndPersonId(query.userId, query.personId)
    }
}
