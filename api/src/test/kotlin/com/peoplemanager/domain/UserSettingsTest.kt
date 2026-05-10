package com.peoplemanager.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import org.junit.jupiter.api.Assertions.*

class UserSettingsTest {

    private val userId = UserId(UUID.randomUUID())

    @Test
    fun `should create default settings with expected values`() {
        val settings = UserSettings.createDefault(userId)

        assertEquals(userId, settings.userId)
        assertEquals(3, settings.dueSoonDays)
        assertEquals(14, settings.staleOneOnOneDays)
        assertEquals(30, settings.anniversaryLookaheadDays)
        assertEquals(Theme.DARK, settings.theme)
        assertTrue(settings.showAchievements)
        assertTrue(settings.notifyActionItemOverdue)
        assertTrue(settings.notifyActionItemDueSoon)
        assertTrue(settings.notifyStaleOneOnOne)
        assertTrue(settings.notifyUpcomingAnniversary)
    }

    @Test
    fun `should reject dueSoonDays less than 1`() {
        assertThrows<IllegalArgumentException> {
            UserSettings(userId = userId, dueSoonDays = 0)
        }
    }

    @Test
    fun `should reject dueSoonDays greater than 30`() {
        assertThrows<IllegalArgumentException> {
            UserSettings(userId = userId, dueSoonDays = 31)
        }
    }

    @Test
    fun `should reject staleOneOnOneDays less than 1`() {
        assertThrows<IllegalArgumentException> {
            UserSettings(userId = userId, staleOneOnOneDays = 0)
        }
    }

    @Test
    fun `should reject staleOneOnOneDays greater than 90`() {
        assertThrows<IllegalArgumentException> {
            UserSettings(userId = userId, staleOneOnOneDays = 91)
        }
    }

    @Test
    fun `should reject anniversaryLookaheadDays less than 1`() {
        assertThrows<IllegalArgumentException> {
            UserSettings(userId = userId, anniversaryLookaheadDays = 0)
        }
    }

    @Test
    fun `should reject anniversaryLookaheadDays greater than 90`() {
        assertThrows<IllegalArgumentException> {
            UserSettings(userId = userId, anniversaryLookaheadDays = 91)
        }
    }

    @Test
    fun `should accept boundary values for dueSoonDays`() {
        val settingsMin = UserSettings(userId = userId, dueSoonDays = 1)
        val settingsMax = UserSettings(userId = userId, dueSoonDays = 30)
        assertEquals(1, settingsMin.dueSoonDays)
        assertEquals(30, settingsMax.dueSoonDays)
    }

    @Test
    fun `should accept boundary values for staleOneOnOneDays`() {
        val settingsMin = UserSettings(userId = userId, staleOneOnOneDays = 1)
        val settingsMax = UserSettings(userId = userId, staleOneOnOneDays = 90)
        assertEquals(1, settingsMin.staleOneOnOneDays)
        assertEquals(90, settingsMax.staleOneOnOneDays)
    }

    @Test
    fun `should accept boundary values for anniversaryLookaheadDays`() {
        val settingsMin = UserSettings(userId = userId, anniversaryLookaheadDays = 1)
        val settingsMax = UserSettings(userId = userId, anniversaryLookaheadDays = 90)
        assertEquals(1, settingsMin.anniversaryLookaheadDays)
        assertEquals(90, settingsMax.anniversaryLookaheadDays)
    }

    @Test
    fun `should update thresholds`() {
        val settings = UserSettings.createDefault(userId)
        val updated = settings.updateThresholds(
            dueSoonDays = 7,
            staleOneOnOneDays = 21,
            anniversaryLookaheadDays = 60
        )

        assertEquals(7, updated.dueSoonDays)
        assertEquals(21, updated.staleOneOnOneDays)
        assertEquals(60, updated.anniversaryLookaheadDays)
        assertTrue(updated.updatedAt >= settings.updatedAt)
    }

    @Test
    fun `should update theme`() {
        val settings = UserSettings.createDefault(userId)
        val updated = settings.updateTheme(Theme.LIGHT)

        assertEquals(Theme.LIGHT, updated.theme)
        assertTrue(updated.updatedAt >= settings.updatedAt)
    }

    @Test
    fun `should update showAchievements`() {
        val settings = UserSettings.createDefault(userId)
        val updated = settings.updateShowAchievements(false)

        assertFalse(updated.showAchievements)
        assertTrue(updated.updatedAt >= settings.updatedAt)
    }

    @Test
    fun `should update notification preferences`() {
        val settings = UserSettings.createDefault(userId)
        val updated = settings.updateNotificationPreferences(
            actionItemOverdue = false,
            actionItemDueSoon = true,
            staleOneOnOne = false,
            upcomingAnniversary = true
        )

        assertFalse(updated.notifyActionItemOverdue)
        assertTrue(updated.notifyActionItemDueSoon)
        assertFalse(updated.notifyStaleOneOnOne)
        assertTrue(updated.notifyUpcomingAnniversary)
        assertTrue(updated.updatedAt >= settings.updatedAt)
    }

    @Test
    fun `should preserve userId when updating`() {
        val settings = UserSettings.createDefault(userId)
        val updated = settings.updateTheme(Theme.LIGHT)

        assertEquals(userId, updated.userId)
    }
}
