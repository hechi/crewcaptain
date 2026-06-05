package com.peoplemanager.application

import com.peoplemanager.application.port.output.UserSettingsRepository
import com.peoplemanager.domain.Theme
import com.peoplemanager.domain.UserId
import com.peoplemanager.domain.UserSettings
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class UserSettingsServiceTest {

    private val userSettingsRepository: UserSettingsRepository = mockk()
    private val auditLogService: AuditLogService = mockk(relaxed = true)
    private lateinit var service: UserSettingsService

    private val userId = UserId(UUID.randomUUID())

    @BeforeEach
    fun setUp() {
        service = UserSettingsService(userSettingsRepository, auditLogService)
    }

    @Test
    fun `should return existing settings for user`() {
        val existing = UserSettings(
            userId = userId,
            dueSoonDays = 5,
            staleOneOnOneDays = 21,
            anniversaryLookaheadDays = 45,
            theme = Theme.LIGHT,
            showAchievements = false,
            notifyActionItemOverdue = false,
            notifyActionItemDueSoon = true,
            notifyStaleOneOnOne = false,
            notifyUpcomingAnniversary = true
        )
        every { userSettingsRepository.findByUserId(userId) } returns existing

        val result = service.getSettings(userId)

        assertEquals(existing, result)
        verify { userSettingsRepository.findByUserId(userId) }
    }

    @Test
    fun `should return default settings when none exist`() {
        every { userSettingsRepository.findByUserId(userId) } returns null

        val result = service.getSettings(userId)

        assertEquals(userId, result.userId)
        assertEquals(3, result.dueSoonDays)
        assertEquals(14, result.staleOneOnOneDays)
        assertEquals(30, result.anniversaryLookaheadDays)
        assertEquals(Theme.DARK, result.theme)
        assertTrue(result.showAchievements)
        assertTrue(result.notifyActionItemOverdue)
        assertTrue(result.notifyActionItemDueSoon)
        assertTrue(result.notifyStaleOneOnOne)
        assertTrue(result.notifyUpcomingAnniversary)
    }

    @Test
    fun `should update existing settings`() {
        val existing = UserSettings.createDefault(userId)
        every { userSettingsRepository.findByUserId(userId) } returns existing
        every { userSettingsRepository.save(any()) } answers { firstArg() }

        val command = UpdateUserSettingsCommand(
            dueSoonDays = 7,
            staleOneOnOneDays = 21,
            anniversaryLookaheadDays = 60,
            theme = Theme.LIGHT,
            showAchievements = false,
            notifyActionItemOverdue = false,
            notifyActionItemDueSoon = true,
            notifyStaleOneOnOne = false,
            notifyUpcomingAnniversary = true
        )

        val result = service.updateSettings(userId, command)

        assertEquals(7, result.dueSoonDays)
        assertEquals(21, result.staleOneOnOneDays)
        assertEquals(60, result.anniversaryLookaheadDays)
        assertEquals(Theme.LIGHT, result.theme)
        assertFalse(result.showAchievements)
        assertFalse(result.notifyActionItemOverdue)
        assertTrue(result.notifyActionItemDueSoon)
        assertFalse(result.notifyStaleOneOnOne)
        assertTrue(result.notifyUpcomingAnniversary)

        verify { userSettingsRepository.save(any()) }
    }

    @Test
    fun `should create new settings when updating and none exist`() {
        every { userSettingsRepository.findByUserId(userId) } returns null
        every { userSettingsRepository.save(any()) } answers { firstArg() }

        val command = UpdateUserSettingsCommand(
            dueSoonDays = 5,
            staleOneOnOneDays = 10,
            anniversaryLookaheadDays = 45,
            theme = Theme.DARK,
            showAchievements = true,
            notifyActionItemOverdue = true,
            notifyActionItemDueSoon = true,
            notifyStaleOneOnOne = true,
            notifyUpcomingAnniversary = true
        )

        val result = service.updateSettings(userId, command)

        assertEquals(userId, result.userId)
        assertEquals(5, result.dueSoonDays)
        assertEquals(10, result.staleOneOnOneDays)
        assertEquals(45, result.anniversaryLookaheadDays)

        verify { userSettingsRepository.save(any()) }
    }

    @Test
    fun `should preserve userId when updating settings`() {
        every { userSettingsRepository.findByUserId(userId) } returns null
        every { userSettingsRepository.save(any()) } answers { firstArg() }

        val command = UpdateUserSettingsCommand(
            dueSoonDays = 3,
            staleOneOnOneDays = 14,
            anniversaryLookaheadDays = 30,
            theme = Theme.DARK,
            showAchievements = true,
            notifyActionItemOverdue = true,
            notifyActionItemDueSoon = true,
            notifyStaleOneOnOne = true,
            notifyUpcomingAnniversary = true
        )

        val result = service.updateSettings(userId, command)

        assertEquals(userId, result.userId)
        verify {
            userSettingsRepository.save(match { it.userId == userId })
        }
    }

    @Test
    fun `should not call save when getting settings`() {
        every { userSettingsRepository.findByUserId(userId) } returns null

        service.getSettings(userId)

        verify(exactly = 0) { userSettingsRepository.save(any()) }
    }

    @Test
    fun `should scope settings retrieval by userId`() {
        val otherUserId = UserId(UUID.randomUUID())
        every { userSettingsRepository.findByUserId(userId) } returns UserSettings(userId = userId, theme = Theme.LIGHT)
        every { userSettingsRepository.findByUserId(otherUserId) } returns null

        val result1 = service.getSettings(userId)
        val result2 = service.getSettings(otherUserId)

        assertEquals(Theme.LIGHT, result1.theme)
        assertEquals(Theme.DARK, result2.theme) // default
    }
}
