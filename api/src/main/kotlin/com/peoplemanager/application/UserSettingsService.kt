package com.peoplemanager.application

import com.peoplemanager.application.ports.UserSettingsRepository
import com.peoplemanager.domain.AiWritingStyle
import com.peoplemanager.domain.AuditLogEntry
import com.peoplemanager.domain.Theme
import com.peoplemanager.domain.UserId
import com.peoplemanager.domain.UserSettings
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class UserSettingsService(
    private val userSettingsRepository: UserSettingsRepository,
    private val auditLogService: AuditLogService
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
            aiEnabled = command.aiEnabled,
            aiApiBaseUrl = command.aiApiBaseUrl,
            aiApiKey = command.aiApiKey,
            aiModelName = command.aiModelName,
            aiPrivacyMode = command.aiPrivacyMode,
            aiWritingStyle = command.aiWritingStyle,
            kudosRefinementPrompt = command.kudosRefinementPrompt,
            pdpOptimizationPrompt = command.pdpOptimizationPrompt,
            agendaPrepPrompt = command.agendaPrepPrompt,
            narrativePrompt = command.narrativePrompt,
            outcomeExtractorPrompt = command.outcomeExtractorPrompt,
            trendRadarPrompt = command.trendRadarPrompt,
            linkSuggestionsPrompt = command.linkSuggestionsPrompt,
            updatedAt = java.time.Instant.now()
        )

        val saved = userSettingsRepository.save(updated)
        auditLogService.record(AuditLogEntry.userSettingsUpdated(userId))
        return saved
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
    val notifyUpcomingAnniversary: Boolean,
    val aiEnabled: Boolean = false,
    val aiApiBaseUrl: String? = null,
    val aiApiKey: String? = null,
    val aiModelName: String? = null,
    val aiPrivacyMode: Boolean = true,
    val aiWritingStyle: AiWritingStyle = AiWritingStyle.NARRATIVE,
    val kudosRefinementPrompt: String? = null,
    val pdpOptimizationPrompt: String? = null,
    val agendaPrepPrompt: String? = null,
    val narrativePrompt: String? = null,
    val outcomeExtractorPrompt: String? = null,
    val trendRadarPrompt: String? = null,
    val linkSuggestionsPrompt: String? = null
)
