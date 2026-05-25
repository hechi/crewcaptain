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
    val outcomeExtractorPrompt: String? = null,
    val trendRadarPrompt: String? = null,
    val linkSuggestionsPrompt: String? = null,
    val strategyOptimizationPrompt: String? = null,
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

    /**
     * Returns the effective outcome extractor prompt (custom or default).
     */
    fun effectiveOutcomeExtractorPrompt(): String =
        outcomeExtractorPrompt?.takeIf { it.isNotBlank() } ?: DEFAULT_OUTCOME_EXTRACTOR_PROMPT

    /**
     * Returns the effective trend radar prompt (custom or default).
     */
    fun effectiveTrendRadarPrompt(): String =
        trendRadarPrompt?.takeIf { it.isNotBlank() } ?: DEFAULT_TREND_RADAR_PROMPT

    /**
     * Returns the effective link suggestions prompt (custom or default).
     */
    fun effectiveLinkSuggestionsPrompt(): String =
        linkSuggestionsPrompt?.takeIf { it.isNotBlank() } ?: DEFAULT_LINK_SUGGESTIONS_PROMPT

    /**
     * Returns the effective strategy goal optimization prompt (custom or default).
     */
    fun effectiveStrategyOptimizationPrompt(): String =
        strategyOptimizationPrompt?.takeIf { it.isNotBlank() } ?: DEFAULT_STRATEGY_OPTIMIZATION_PROMPT

    companion object {
        const val DEFAULT_DUE_SOON_DAYS = 3
        const val DEFAULT_STALE_ONE_ON_ONE_DAYS = 14
        const val DEFAULT_ANNIVERSARY_LOOKAHEAD_DAYS = 30

        const val DEFAULT_KUDOS_REFINEMENT_PROMPT =
            "You are a leadership coach. Refine the following kudos draft using the Situation-Behavior-Impact (SBI) framework. " +
            "Keep it professional, specific, and concise. " +
            "RULES: " +
            "- Output ONLY the refined kudos text. " +
            "- Do NOT include any introduction, preamble, or explanation such as 'Here is the refined draft:' or 'Sure, here you go:'. " +
            "- Do NOT use markdown formatting (no bold, italic, headers, or bullet points). " +
            "- Start directly with the refined text."

        const val DEFAULT_PDP_OPTIMIZATION_PROMPT =
            "You are a career development expert. Evaluate the following goal and ensure it meets SMART (Specific, Measurable, Achievable, Relevant, Time-bound) criteria. " +
            "Provide an improved version of the goal title and description, and briefly explain why it is better. " +
            "RULES: " +
            "- Do NOT use markdown formatting (no bold, italic, headers, or bullet points). " +
            "- Do NOT include any preamble or introduction. " +
            "- Format your response EXACTLY as three plain text lines:\n" +
            "Title: <improved title>\n" +
            "Description: <improved description>\n" +
            "Explanation: <why it is better>"

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

        const val DEFAULT_OUTCOME_EXTRACTOR_PROMPT =
            "You are an executive assistant for a manager. Analyze the following 1:1 meeting notes. " +
            "Extract a JSON object containing: " +
            "1. 'action_items': A list of objects with 'title' (string), 'owner_type' (either 'MANAGER' or 'PERSON'), " +
            "and 'suggested_days_to_due' (integer, number of days from today). " +
            "2. 'decisions': A list of strings summarizing key agreements or conclusions. " +
            "RULES: " +
            "- Be concise. Only extract items explicitly mentioned or strongly implied as tasks. " +
            "- Output ONLY valid JSON. No markdown code fences, no preamble, no explanation. " +
            "- If no action items or decisions are found, return empty arrays. " +
            "- suggested_days_to_due should be a reasonable estimate (7 for 'next week', 14 for 'in two weeks', etc.)."

        const val DEFAULT_TREND_RADAR_PROMPT =
            "You are a strategic people analytics advisor for a manager. " +
            "Analyze the provided manager-report metadata and identify 3 potential trends or patterns. " +
            "For each trend, assess the data density and assign a confidence_score (0-100). " +
            "RULES: " +
            "- Output ONLY valid JSON. No markdown code fences, no preamble, no explanation. " +
            "- Return a JSON object with a single key 'insights' containing an array of objects. " +
            "- Each object must have: 'title' (string, short label like 'Burnout Risk' or 'High Impact'), " +
            "'description' (string, 2-3 sentences explaining the pattern), " +
            "'dimension' (string, one of: 'MORALE', 'WORK_GROWTH_BALANCE', 'RECOGNITION', 'MEETING_EFFICACY'), " +
            "'confidence_score' (integer 0-100 based on data volume and recency). " +
            "- confidence_score < 40 means insufficient data, 40-75 means moderate signal, > 75 means strong signal. " +
            "- Be objective and data-driven. Do not speculate beyond what the data supports. " +
            "- If data is very thin, reflect that honestly in low confidence scores."

        const val DEFAULT_LINK_SUGGESTIONS_PROMPT =
            "You are a strategic alignment advisor for a manager. " +
            "Your task is to analyze strategy goals and team members' personal development goals (PDP goals), " +
            "then suggest meaningful connections between them. " +
            "RULES: " +
            "- Output ONLY valid JSON. No markdown code fences, no preamble, no explanation. " +
            "- Return a JSON object with a single key 'suggestions' containing an array of objects. " +
            "- Each suggestion object must have these fields: " +
            "  'strategyGoalId' (UUID string from the Strategy Goals list), " +
            "  'pdpGoalId' (UUID string from the PDP Goals list), " +
            "  'personId' (UUID string from the PDP Goals list - NOT the person's name), " +
            "  'personName' (the person's name as shown in the PDP Goals list), " +
            "  'strategyGoalTitle' (the strategy goal title), " +
            "  'pdpGoalTitle' (the PDP goal title), " +
            "  'matchScore' (integer 0-100 indicating strength of alignment), " +
            "  'reasoning' (string explaining the connection). " +
            "CRITICAL: The UUID fields (strategyGoalId, pdpGoalId, personId) must contain the EXACT UUID strings " +
            "provided in the input lists. Do NOT invent UUIDs. Copy them exactly from the bracketed IDs in the input. " +
            "Example: if input shows '- [550e8400-e29b-41d4-a716-446655440000] Reduce Cloud Costs', " +
            "you must use '550e8400-e29b-41d4-a716-446655440000' as the strategyGoalId. " +
            "- Only suggest links that have genuine strategic relevance (matchScore >= 40). " +
            "- Limit to the top 5-10 most relevant suggestions. " +
            "- Do NOT suggest links for sensitive strategy goals. " +
            "- Be specific in reasoning: explain WHY this PDP goal contributes to the strategy goal."

        const val DEFAULT_STRATEGY_OPTIMIZATION_PROMPT =
            "You are a strategic planning expert. Evaluate the following strategy goal and ensure it meets SMART (Specific, Measurable, Achievable, Relevant, Time-bound) criteria. " +
            "Provide an improved version of the goal title and description, and briefly explain why it is better. " +
            "RULES: " +
            "- Do NOT use markdown formatting (no bold, italic, headers, or bullet points). " +
            "- Do NOT include any preamble or introduction. " +
            "- Format your response EXACTLY as three plain text lines:\n" +
            "Title: <improved title>\n" +
            "Description: <improved description>\n" +
            "Explanation: <why it is better>"

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
