package com.peoplemanager.application

import com.peoplemanager.application.ports.UserSettingsRepository
import com.peoplemanager.domain.Theme
import com.peoplemanager.domain.UserId
import com.peoplemanager.domain.UserSettings
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class UserSettingsService(
    private val userSettingsRepository: UserSettingsRepository
) {

    @Transactional(readOnly = true)
    fun getSettings(userId: UserId): UserSettings {
        return userSettingsRepository.findByUserId(userId)
            ?: UserSettings.createDefault(userId)
    }

    fun updateSettings(userId: UserId, command: UpdateUserSettingsCommand): UserSettings {
        val existing = userSettingsRepository.findByUserId(userId)
            ?: UserSettings.createDefault(userId)

        val updated = existing.copy(
            dueSoonDays = command.dueSoonDays,
            staleOneOnOneDays = command.staleOneOnOneDays,
            anniversaryLookaheadDays = command.anniversaryLookaheadDays,
            theme = command.theme,
            showAchievements = command.showAchievements,
            notifyActionItemOverdue = command.notifyActionItemOverdue,
            notifyActionItemDueSoon = command.notifyActionItemDueSoon,
            notifyStaleOneOnOne = command.notifyStaleOneOnOne,
            notifyUpcomingAnniversary = command.notifyUpcomingAnniversary,
            updatedAt = java.time.Instant.now()
        )

        return userSettingsRepository.save(updated)
    }
}

data class UpdateUserSettingsCommand(
    val dueSoonDays: Int,
    val staleOneOnOneDays: Int,
    val anniversaryLookaheadDays: Int,
    val theme: Theme,
    val showAchievements: Boolean,
    val notifyActionItemOverdue: Boolean,
    val notifyActionItemDueSoon: Boolean,
    val notifyStaleOneOnOne: Boolean,
    val notifyUpcomingAnniversary: Boolean
)
