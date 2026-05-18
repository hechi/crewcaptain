package com.peoplemanager.adapters.web.dto

import com.peoplemanager.domain.UserSettings

data class UserSettingsResponse(
    val dueSoonDays: Int,
    val staleOneOnOneDays: Int,
    val anniversaryLookaheadDays: Int,
    val theme: String,
    val showAchievements: Boolean,
    val notifyActionItemOverdue: Boolean,
    val notifyActionItemDueSoon: Boolean,
    val notifyStaleOneOnOne: Boolean,
    val notifyUpcomingAnniversary: Boolean,
    val aiEnabled: Boolean,
    val aiApiBaseUrl: String?,
    val aiModelName: String?,
    val aiPrivacyMode: Boolean,
    val aiWritingStyle: String,
    val kudosRefinementPrompt: String?,
    val pdpOptimizationPrompt: String?,
    val agendaPrepPrompt: String?,
    val narrativePrompt: String?,
    val outcomeExtractorPrompt: String?
) {
    companion object {
        fun from(settings: UserSettings): UserSettingsResponse = UserSettingsResponse(
            dueSoonDays = settings.dueSoonDays,
            staleOneOnOneDays = settings.staleOneOnOneDays,
            anniversaryLookaheadDays = settings.anniversaryLookaheadDays,
            theme = settings.theme.name,
            showAchievements = settings.showAchievements,
            notifyActionItemOverdue = settings.notifyActionItemOverdue,
            notifyActionItemDueSoon = settings.notifyActionItemDueSoon,
            notifyStaleOneOnOne = settings.notifyStaleOneOnOne,
            notifyUpcomingAnniversary = settings.notifyUpcomingAnniversary,
            aiEnabled = settings.aiEnabled,
            aiApiBaseUrl = settings.aiApiBaseUrl,
            aiModelName = settings.aiModelName,
            aiPrivacyMode = settings.aiPrivacyMode,
            aiWritingStyle = settings.aiWritingStyle.name,
            kudosRefinementPrompt = settings.kudosRefinementPrompt,
            pdpOptimizationPrompt = settings.pdpOptimizationPrompt,
            agendaPrepPrompt = settings.agendaPrepPrompt,
            narrativePrompt = settings.narrativePrompt,
            outcomeExtractorPrompt = settings.outcomeExtractorPrompt
        )
    }
}
