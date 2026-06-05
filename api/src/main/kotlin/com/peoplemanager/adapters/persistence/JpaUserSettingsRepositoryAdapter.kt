package com.peoplemanager.adapters.persistence

import com.peoplemanager.application.port.output.UserSettingsRepository
import com.peoplemanager.domain.AiWritingStyle
import com.peoplemanager.domain.Theme
import com.peoplemanager.domain.UserId
import com.peoplemanager.domain.UserSettings
import org.springframework.stereotype.Repository

@Repository
class JpaUserSettingsRepositoryAdapter(
    private val springDataUserSettingsRepository: SpringDataUserSettingsRepository
) : UserSettingsRepository {

    override fun findByUserId(userId: UserId): UserSettings? {
        return springDataUserSettingsRepository.findById(userId.value)
            .map { it.toDomain() }
            .orElse(null)
    }

    override fun save(settings: UserSettings): UserSettings {
        val entity = settings.toEntity()
        return springDataUserSettingsRepository.save(entity).toDomain()
    }

    private fun UserSettingsEntity.toDomain(): UserSettings = UserSettings(
        userId = UserId(this.userId),
        dueSoonDays = this.dueSoonDays,
        staleOneOnOneDays = this.staleOneOnOneDays,
        anniversaryLookaheadDays = this.anniversaryLookaheadDays,
        theme = Theme.valueOf(this.theme),
        showAchievements = this.showAchievements,
        notifyActionItemOverdue = this.notifyActionItemOverdue,
        notifyActionItemDueSoon = this.notifyActionItemDueSoon,
        notifyStaleOneOnOne = this.notifyStaleOneOnOne,
        notifyUpcomingAnniversary = this.notifyUpcomingAnniversary,
        aiEnabled = this.aiEnabled,
        aiApiBaseUrl = this.aiApiBaseUrl,
        aiApiKey = this.aiApiKey,
        aiModelName = this.aiModelName,
        aiPrivacyMode = this.aiPrivacyMode,
        aiWritingStyle = AiWritingStyle.valueOf(this.aiWritingStyle),
        kudosRefinementPrompt = this.kudosRefinementPrompt,
        pdpOptimizationPrompt = this.pdpOptimizationPrompt,
        agendaPrepPrompt = this.agendaPrepPrompt,
        narrativePrompt = this.narrativePrompt,
        outcomeExtractorPrompt = this.outcomeExtractorPrompt,
        trendRadarPrompt = this.trendRadarPrompt,
        linkSuggestionsPrompt = this.linkSuggestionsPrompt,
        strategyOptimizationPrompt = this.strategyOptimizationPrompt,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )

    private fun UserSettings.toEntity(): UserSettingsEntity = UserSettingsEntity(
        userId = this.userId.value,
        dueSoonDays = this.dueSoonDays,
        staleOneOnOneDays = this.staleOneOnOneDays,
        anniversaryLookaheadDays = this.anniversaryLookaheadDays,
        theme = this.theme.name,
        showAchievements = this.showAchievements,
        notifyActionItemOverdue = this.notifyActionItemOverdue,
        notifyActionItemDueSoon = this.notifyActionItemDueSoon,
        notifyStaleOneOnOne = this.notifyStaleOneOnOne,
        notifyUpcomingAnniversary = this.notifyUpcomingAnniversary,
        aiEnabled = this.aiEnabled,
        aiApiBaseUrl = this.aiApiBaseUrl,
        aiApiKey = this.aiApiKey,
        aiModelName = this.aiModelName,
        aiPrivacyMode = this.aiPrivacyMode,
        aiWritingStyle = this.aiWritingStyle.name,
        kudosRefinementPrompt = this.kudosRefinementPrompt,
        pdpOptimizationPrompt = this.pdpOptimizationPrompt,
        agendaPrepPrompt = this.agendaPrepPrompt,
        narrativePrompt = this.narrativePrompt,
        outcomeExtractorPrompt = this.outcomeExtractorPrompt,
        trendRadarPrompt = this.trendRadarPrompt,
        linkSuggestionsPrompt = this.linkSuggestionsPrompt,
        strategyOptimizationPrompt = this.strategyOptimizationPrompt,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}
