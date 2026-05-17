package com.peoplemanager.domain

import java.time.Instant

/**
 * User-level settings for dashboard thresholds, notification preferences,
 * theme, and UI visibility toggles.
 *
 * Each user has exactly one settings record. If none exists, defaults apply.
 */
data class UserSettings(
    val userId: UserId,
    val dueSoonDays: Int = DEFAULT_DUE_SOON_DAYS,
    val staleOneOnOneDays: Int = DEFAULT_STALE_ONE_ON_ONE_DAYS,
    val anniversaryLookaheadDays: Int = DEFAULT_ANNIVERSARY_LOOKAHEAD_DAYS,
    val theme: Theme = Theme.DARK,
    val showAchievements: Boolean = true,
    val notifyActionItemOverdue: Boolean = true,
    val notifyActionItemDueSoon: Boolean = true,
    val notifyStaleOneOnOne: Boolean = true,
    val notifyUpcomingAnniversary: Boolean = true,
    val aiEnabled: Boolean = false,
    val aiApiBaseUrl: String? = null,
    val aiApiKey: String? = null,
    val aiModelName: String? = null,
    val aiPrivacyMode: Boolean = true,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    init {
        require(dueSoonDays in 1..30) { "dueSoonDays must be between 1 and 30" }
        require(staleOneOnOneDays in 1..90) { "staleOneOnOneDays must be between 1 and 90" }
        require(anniversaryLookaheadDays in 1..90) { "anniversaryLookaheadDays must be between 1 and 90" }
        if (aiEnabled) {
            require(!aiApiBaseUrl.isNullOrBlank()) { "AI API base URL is required when AI is enabled" }
            require(!aiModelName.isNullOrBlank()) { "AI model name is required when AI is enabled" }
        }
    }

    fun updateThresholds(
        dueSoonDays: Int,
        staleOneOnOneDays: Int,
        anniversaryLookaheadDays: Int
    ): UserSettings = copy(
        dueSoonDays = dueSoonDays,
        staleOneOnOneDays = staleOneOnOneDays,
        anniversaryLookaheadDays = anniversaryLookaheadDays,
        updatedAt = Instant.now()
    )

    fun updateTheme(theme: Theme): UserSettings = copy(
        theme = theme,
        updatedAt = Instant.now()
    )

    fun updateShowAchievements(show: Boolean): UserSettings = copy(
        showAchievements = show,
        updatedAt = Instant.now()
    )

    fun updateNotificationPreferences(
        actionItemOverdue: Boolean,
        actionItemDueSoon: Boolean,
        staleOneOnOne: Boolean,
        upcomingAnniversary: Boolean
    ): UserSettings = copy(
        notifyActionItemOverdue = actionItemOverdue,
        notifyActionItemDueSoon = actionItemDueSoon,
        notifyStaleOneOnOne = staleOneOnOne,
        notifyUpcomingAnniversary = upcomingAnniversary,
        updatedAt = Instant.now()
    )

    fun updateAiSettings(
        aiEnabled: Boolean,
        aiApiBaseUrl: String?,
        aiApiKey: String?,
        aiModelName: String?,
        aiPrivacyMode: Boolean
    ): UserSettings = copy(
        aiEnabled = aiEnabled,
        aiApiBaseUrl = aiApiBaseUrl,
        aiApiKey = aiApiKey,
        aiModelName = aiModelName,
        aiPrivacyMode = aiPrivacyMode,
        updatedAt = Instant.now()
    )

    fun isAiConfigured(): Boolean = aiEnabled && !aiApiBaseUrl.isNullOrBlank() && !aiModelName.isNullOrBlank()

    companion object {
        const val DEFAULT_DUE_SOON_DAYS = 3
        const val DEFAULT_STALE_ONE_ON_ONE_DAYS = 14
        const val DEFAULT_ANNIVERSARY_LOOKAHEAD_DAYS = 30

        fun createDefault(userId: UserId): UserSettings = UserSettings(userId = userId)
    }
}

enum class Theme {
    DARK,
    LIGHT
}
