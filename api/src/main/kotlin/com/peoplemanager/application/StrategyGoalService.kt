package com.peoplemanager.application

import com.peoplemanager.application.commands.*
import com.peoplemanager.application.port.output.StrategyGoalRepository
import com.peoplemanager.application.queries.*
import com.peoplemanager.domain.AuditLogEntry
import com.peoplemanager.domain.StrategyGoal
import com.peoplemanager.domain.StrategyGoalId
import com.peoplemanager.domain.StrategyGoalStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class StrategyGoalService(
    private val strategyGoalRepository: StrategyGoalRepository,
    private val auditLogService: AuditLogService
) {

    fun createStrategyGoal(command: CreateStrategyGoalCommand): StrategyGoal {
        val goal = StrategyGoal(
            id = StrategyGoalId.generate(),
            userId = command.userId,
            title = command.title,
            description = command.description,
            targetDate = command.targetDate,
            sensitive = command.sensitive
        )

        val saved = strategyGoalRepository.save(goal)
        auditLogService.record(AuditLogEntry.strategyGoalCreated(command.userId, saved.id, saved.title))
        return saved
    }

    fun updateStrategyGoal(command: UpdateStrategyGoalCommand): StrategyGoal {
        val existing = strategyGoalRepository.findByIdAndUserId(command.strategyGoalId, command.userId)
            ?: throw StrategyGoalNotFoundException(command.strategyGoalId)

        val updated = existing.updateDetails(
            title = command.title,
            description = command.description,
            targetDate = command.targetDate
        )

        val saved = strategyGoalRepository.save(updated)
        auditLogService.record(AuditLogEntry.strategyGoalUpdated(command.userId, saved.id, saved.title))
        return saved
    }

    fun achieveStrategyGoal(command: AchieveStrategyGoalCommand): StrategyGoal {
        val existing = strategyGoalRepository.findByIdAndUserId(command.strategyGoalId, command.userId)
            ?: throw StrategyGoalNotFoundException(command.strategyGoalId)

        val achieved = existing.achieve()
        val saved = strategyGoalRepository.save(achieved)
        auditLogService.record(AuditLogEntry.strategyGoalAchieved(command.userId, saved.id, saved.title))
        return saved
    }

    fun dropStrategyGoal(command: DropStrategyGoalCommand): StrategyGoal {
        val existing = strategyGoalRepository.findByIdAndUserId(command.strategyGoalId, command.userId)
            ?: throw StrategyGoalNotFoundException(command.strategyGoalId)

        val dropped = existing.drop()
        val saved = strategyGoalRepository.save(dropped)
        auditLogService.record(AuditLogEntry.strategyGoalDropped(command.userId, saved.id, saved.title))
        return saved
    }

    fun deleteStrategyGoal(command: DeleteStrategyGoalCommand) {
        val existing = strategyGoalRepository.findByIdAndUserId(command.strategyGoalId, command.userId)
            ?: throw StrategyGoalNotFoundException(command.strategyGoalId)

        val deleted = strategyGoalRepository.deleteByIdAndUserId(command.strategyGoalId, command.userId)
        if (!deleted) throw StrategyGoalNotFoundException(command.strategyGoalId)

        auditLogService.record(AuditLogEntry.strategyGoalDeleted(command.userId, command.strategyGoalId, existing.title))
    }

    fun getStrategyGoal(query: GetStrategyGoalQuery): StrategyGoal {
        return strategyGoalRepository.findByIdAndUserId(query.strategyGoalId, query.userId)
            ?: throw StrategyGoalNotFoundException(query.strategyGoalId)
    }

    fun listStrategyGoals(query: ListStrategyGoalsQuery): Page<StrategyGoal> {
        val pageable = PageRequest.of(query.page, query.size, Sort.by(Sort.Direction.DESC, "createdAt"))
        return strategyGoalRepository.findAllByUserId(query.userId, query.status, pageable)
    }

    fun countActiveStrategyGoals(userId: com.peoplemanager.domain.UserId): Long {
        return strategyGoalRepository.countActiveByUserId(userId)
    }

    fun countByStatus(userId: com.peoplemanager.domain.UserId, status: StrategyGoalStatus): Long {
        return strategyGoalRepository.countByUserIdAndStatus(userId, status)
    }
}
