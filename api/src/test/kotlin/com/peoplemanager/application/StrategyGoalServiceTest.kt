package com.peoplemanager.application

import com.peoplemanager.application.commands.*
import com.peoplemanager.application.port.output.StrategyGoalRepository
import com.peoplemanager.application.queries.GetStrategyGoalQuery
import com.peoplemanager.domain.*
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.util.*
import org.junit.jupiter.api.Assertions.*

class StrategyGoalServiceTest {

    private lateinit var strategyGoalRepository: StrategyGoalRepository
    private lateinit var auditLogService: AuditLogService
    private lateinit var service: StrategyGoalService

    private val testUserId = UserId(UUID.randomUUID())

    @BeforeEach
    fun setUp() {
        strategyGoalRepository = mockk()
        auditLogService = mockk(relaxed = true)
        service = StrategyGoalService(strategyGoalRepository, auditLogService)
    }

    @Test
    fun `should create strategy goal with audit log`() {
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

        every { strategyGoalRepository.save(any()) } returns savedGoal

        val result = service.createStrategyGoal(command)

        assertEquals(command.title, result.title)
        assertEquals(command.description, result.description)
        assertEquals(StrategyGoalStatus.ACTIVE, result.status)
        assertFalse(result.sensitive)
        verify { auditLogService.record(any()) }
    }

    @Test
    fun `should create sensitive strategy goal`() {
        val command = CreateStrategyGoalCommand(
            userId = testUserId,
            title = "Sensitive Goal",
            description = "Secret Description",
            targetDate = null,
            sensitive = true
        )

        val savedGoal = createTestStrategyGoal(
            title = command.title,
            description = command.description,
            sensitive = true
        )

        every { strategyGoalRepository.save(any()) } returns savedGoal

        val result = service.createStrategyGoal(command)

        assertTrue(result.sensitive)
        verify { strategyGoalRepository.save(any()) }
    }

    @Test
    fun `should update strategy goal with audit log`() {
        val existingGoal = createTestStrategyGoal()
        val command = UpdateStrategyGoalCommand(
            userId = testUserId,
            strategyGoalId = existingGoal.id,
            title = "Updated Title",
            description = "Updated Description",
            targetDate = LocalDate.now().plusMonths(6)
        )

        every { strategyGoalRepository.findByIdAndUserId(any(), any()) } returns existingGoal
        every { strategyGoalRepository.save(any()) } returns existingGoal.copy(
            title = command.title!!,
            description = command.description
        )

        val result = service.updateStrategyGoal(command)

        assertEquals(command.title, result.title)
        assertEquals(command.description, result.description)
        verify { auditLogService.record(any()) }
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

        every { strategyGoalRepository.findByIdAndUserId(any(), any()) } returns null

        assertThrows(StrategyGoalNotFoundException::class.java) {
            service.updateStrategyGoal(command)
        }
    }

    @Test
    fun `should achieve active strategy goal`() {
        val goal = createTestStrategyGoal(status = StrategyGoalStatus.ACTIVE)
        val command = AchieveStrategyGoalCommand(testUserId, goal.id)

        every { strategyGoalRepository.findByIdAndUserId(any(), any()) } returns goal
        every { strategyGoalRepository.save(any()) } returns goal.copy(status = StrategyGoalStatus.ACHIEVED)

        val result = service.achieveStrategyGoal(command)

        assertEquals(StrategyGoalStatus.ACHIEVED, result.status)
        verify { auditLogService.record(any()) }
    }

    @Test
    fun `should drop active strategy goal`() {
        val goal = createTestStrategyGoal(status = StrategyGoalStatus.ACTIVE)
        val command = DropStrategyGoalCommand(testUserId, goal.id)

        every { strategyGoalRepository.findByIdAndUserId(any(), any()) } returns goal
        every { strategyGoalRepository.save(any()) } returns goal.copy(status = StrategyGoalStatus.DROPPED)

        val result = service.dropStrategyGoal(command)

        assertEquals(StrategyGoalStatus.DROPPED, result.status)
        verify { auditLogService.record(any()) }
    }

    @Test
    fun `should delete strategy goal with audit log`() {
        val goal = createTestStrategyGoal()
        val command = DeleteStrategyGoalCommand(testUserId, goal.id)

        every { strategyGoalRepository.findByIdAndUserId(any(), any()) } returns goal
        every { strategyGoalRepository.deleteByIdAndUserId(any(), any()) } returns true

        service.deleteStrategyGoal(command)

        verify { strategyGoalRepository.deleteByIdAndUserId(goal.id, testUserId) }
        verify { auditLogService.record(any()) }
    }

    @Test
    fun `should throw exception when deleting non-existent goal`() {
        val command = DeleteStrategyGoalCommand(testUserId, StrategyGoalId.generate())

        every { strategyGoalRepository.findByIdAndUserId(any(), any()) } returns null

        assertThrows(StrategyGoalNotFoundException::class.java) {
            service.deleteStrategyGoal(command)
        }
    }

    @Test
    fun `should get strategy goal by id`() {
        val goal = createTestStrategyGoal()

        every { strategyGoalRepository.findByIdAndUserId(any(), any()) } returns goal

        val result = service.getStrategyGoal(GetStrategyGoalQuery(testUserId, goal.id))

        assertEquals(goal.id, result.id)
        assertEquals(goal.title, result.title)
    }

    @Test
    fun `should throw exception when getting non-existent goal`() {
        val goalId = StrategyGoalId.generate()

        every { strategyGoalRepository.findByIdAndUserId(any(), any()) } returns null

        assertThrows(StrategyGoalNotFoundException::class.java) {
            service.getStrategyGoal(GetStrategyGoalQuery(testUserId, goalId))
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
