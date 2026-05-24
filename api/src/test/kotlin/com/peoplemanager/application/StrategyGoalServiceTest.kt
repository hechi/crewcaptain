package com.peoplemanager.application

import com.peoplemanager.application.commands.*
import com.peoplemanager.application.ports.StrategyGoalRepository
import com.peoplemanager.domain.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.kotlin.*
import java.time.LocalDate
import java.util.*

class StrategyGoalServiceTest {

    private val strategyGoalRepository: StrategyGoalRepository = mock()
    private val auditLogService: AuditLogService = mock()
    private val service = StrategyGoalService(strategyGoalRepository, auditLogService)

    private val testUserId = UserId(UUID.randomUUID())

    @Test
    fun `should create strategy goal`() {
        val command = CreateStrategyGoalCommand(
            userId = testUserId,
            title = "Test Goal",
            description = "Test Description",
            targetDate = LocalDate.now().plusMonths(3),
            sensitive = false
        )

        val savedGoal = createTestStrategyGoal(
            title = command.title,
            description = command.description,
            targetDate = command.targetDate,
            sensitive = command.sensitive
        )

        whenever(strategyGoalRepository.save(any())) doReturn savedGoal

        val result = service.createStrategyGoal(command)

        assertEquals(command.title, result.title)
        assertEquals(command.description, result.description)
        assertEquals(StrategyGoalStatus.ACTIVE, result.status)
        verify(auditLogService).record(any())
    }

    @Test
    fun `should update strategy goal`() {
        val existingGoal = createTestStrategyGoal()
        val command = UpdateStrategyGoalCommand(
            userId = testUserId,
            strategyGoalId = existingGoal.id,
            title = "Updated Title",
            description = "Updated Description",
            targetDate = LocalDate.now().plusMonths(6)
        )

        whenever(strategyGoalRepository.findByIdAndUserId(any(), any())) doReturn existingGoal
        whenever(strategyGoalRepository.save(any())) doReturn existingGoal.copy(
            title = command.title!!,
            description = command.description
        )

        val result = service.updateStrategyGoal(command)

        assertEquals(command.title, result.title)
        verify(auditLogService).record(any())
    }

    @Test
    fun `should throw exception when updating non-existent goal`() {
        val command = UpdateStrategyGoalCommand(
            userId = testUserId,
            strategyGoalId = StrategyGoalId.generate(),
            title = "Updated Title",
            description = null,
            targetDate = null
        )

        whenever(strategyGoalRepository.findByIdAndUserId(any(), any())) doReturn null

        assertThrows(StrategyGoalNotFoundException::class.java) {
            service.updateStrategyGoal(command)
        }
    }

    @Test
    fun `should achieve strategy goal`() {
        val goal = createTestStrategyGoal(status = StrategyGoalStatus.ACTIVE)
        val command = AchieveStrategyGoalCommand(testUserId, goal.id)

        whenever(strategyGoalRepository.findByIdAndUserId(any(), any())) doReturn goal
        whenever(strategyGoalRepository.save(any())) doReturn goal.copy(status = StrategyGoalStatus.ACHIEVED)

        val result = service.achieveStrategyGoal(command)

        assertEquals(StrategyGoalStatus.ACHIEVED, result.status)
        verify(auditLogService).record(any())
    }

    @Test
    fun `should drop strategy goal`() {
        val goal = createTestStrategyGoal(status = StrategyGoalStatus.ACTIVE)
        val command = DropStrategyGoalCommand(testUserId, goal.id)

        whenever(strategyGoalRepository.findByIdAndUserId(any(), any())) doReturn goal
        whenever(strategyGoalRepository.save(any())) doReturn goal.copy(status = StrategyGoalStatus.DROPPED)

        val result = service.dropStrategyGoal(command)

        assertEquals(StrategyGoalStatus.DROPPED, result.status)
        verify(auditLogService).record(any())
    }

    @Test
    fun `should delete strategy goal`() {
        val goal = createTestStrategyGoal()
        val command = DeleteStrategyGoalCommand(testUserId, goal.id)

        whenever(strategyGoalRepository.findByIdAndUserId(any(), any())) doReturn goal
        whenever(strategyGoalRepository.deleteByIdAndUserId(any(), any())) doReturn true

        service.deleteStrategyGoal(command)

        verify(strategyGoalRepository).deleteByIdAndUserId(goal.id, testUserId)
        verify(auditLogService).record(any())
    }

    @Test
    fun `should throw exception when deleting non-existent goal`() {
        val command = DeleteStrategyGoalCommand(testUserId, StrategyGoalId.generate())

        whenever(strategyGoalRepository.findByIdAndUserId(any(), any())) doReturn null

        assertThrows(StrategyGoalNotFoundException::class.java) {
            service.deleteStrategyGoal(command)
        }
    }

    private fun createTestStrategyGoal(
        id: StrategyGoalId = StrategyGoalId.generate(),
        title: String = "Test Goal",
        description: String? = "Test Description",
        targetDate: LocalDate? = LocalDate.now().plusMonths(3),
        sensitive: Boolean = false,
        status: StrategyGoalStatus = StrategyGoalStatus.ACTIVE
    ): StrategyGoal {
        return StrategyGoal(
            id = id,
            userId = testUserId,
            title = title,
            description = description,
            targetDate = targetDate,
            sensitive = sensitive,
            status = status
        )
    }
}
