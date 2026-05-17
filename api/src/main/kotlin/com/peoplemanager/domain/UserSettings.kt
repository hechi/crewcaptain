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
    val aiWritingStyle: AiWritingStyle = AiWritingStyle.NARRATIVE,
    val kudosRefinementPrompt: String? = null,
    val pdpOptimizationPrompt: String? = null,
    val agendaPrepPrompt: String? = null,
    val narrativePrompt: String? = null,
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
        aiPrivacyMode: Boolean,
        aiWritingStyle: AiWritingStyle = AiWritingStyle.NARRATIVE
    ): UserSettings = copy(
        aiEnabled = aiEnabled,
        aiApiBaseUrl = aiApiBaseUrl,
        aiApiKey = aiApiKey,
        aiModelName = aiModelName,
        aiPrivacyMode = aiPrivacyMode,
        aiWritingStyle = aiWritingStyle,
        updatedAt = Instant.now()
    )

    fun isAiConfigured(): Boolean = aiEnabled && !aiApiBaseUrl.isNullOrBlank() && !aiModelName.isNullOrBlank()

    /**
     * Returns the effective kudos refinement prompt (custom or default).
     */
    fun effectiveKudosRefinementPrompt(): String =
        kudosRefinementPrompt?.takeIf { it.isNotBlank() } ?: DEFAULT_KUDOS_REFINEMENT_PROMPT

    /**
     * Returns the effective PDP goal optimization prompt (custom or default).
     */
    fun effectivePdpOptimizationPrompt(): String =
        pdpOptimizationPrompt?.takeIf { it.isNotBlank() } ?: DEFAULT_PDP_OPTIMIZATION_PROMPT

    /**
     * Returns the effective agenda prep prompt (custom or default).
     */
    fun effectiveAgendaPrepPrompt(): String =
        agendaPrepPrompt?.takeIf { it.isNotBlank() } ?: DEFAULT_AGENDA_PREP_PROMPT

    /**
     * Returns the effective narrative prompt (custom or default).
     */
    fun effectiveNarrativePrompt(): String =
        narrativePrompt?.takeIf { it.isNotBlank() } ?: DEFAULT_NARRATIVE_PROMPT

    companion object {
        const val DEFAULT_DUE_SOON_DAYS = 3
        const val DEFAULT_STALE_ONE_ON_ONE_DAYS = 14
        const val DEFAULT_ANNIVERSARY_LOOKAHEAD_DAYS = 30

        const val DEFAULT_KUDOS_REFINEMENT_PROMPT =
            "You are a leadership coach. Refine the following kudos draft using the Situation-Behavior-Impact (SBI) framework. Keep it professional, specific, and concise. Output ONLY the refined kudos text, no preamble or explanation."

        const val DEFAULT_PDP_OPTIMIZATION_PROMPT =
            "You are a career development expert. Evaluate the following goal and ensure it meets SMART (Specific, Measurable, Achievable, Relevant, Time-bound) criteria. Provide an improved version of the goal title and description, and briefly explain why it is better. Format your response as:\nTitle: <improved title>\nDescription: <improved description>\nExplanation: <why it is better>"

        const val DEFAULT_AGENDA_PREP_PROMPT =
            "You are a leadership coach. Based on the provided context, " +
            "suggest 3-5 high-impact agenda items for the next 1:1 meeting. " +
            "RULES: " +
            "- Output ONLY the agenda items, one per line. " +
            "- Do NOT include any introduction, preamble, explanation, or closing text. " +
            "- Do NOT use markdown formatting (no bold, italic, headers, or links). " +
            "- Start each line with a dash followed by a space. " +
            "- Keep each item short and actionable (one sentence). " +
            "- Focus on follow-ups, blockers, growth opportunities, and recognition."

        const val DEFAULT_NARRATIVE_PROMPT =
            "You are an expert Leadership Coach and People Manager. " +
            "Your goal is to draft a professional, objective, and supportive performance review narrative " +
            "based on raw meeting notes, kudos, and goal progress. " +
            "Maintain a professional tone: concise, impactful, and forward-looking. " +
            "Do NOT include any preamble, introduction, or meta-commentary. " +
            "Output ONLY the narrative content."

        fun createDefault(userId: UserId): UserSettings = UserSettings(userId = userId)
    }
}

enum class Theme {
    DARK,
    LIGHT
}

enum class AiWritingStyle {
    NARRATIVE,
    BULLET_POINTS,
    CONCISE
}
