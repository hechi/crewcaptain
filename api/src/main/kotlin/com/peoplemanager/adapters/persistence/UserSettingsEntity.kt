package com.peoplemanager.adapters.persistence

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "user_settings")
class UserSettingsEntity(
    @Id
    @Column(name = "user_id")
    val userId: UUID = UUID.randomUUID(),

    @Column(name = "due_soon_days", nullable = false)
    val dueSoonDays: Int = 3,

    @Column(name = "stale_one_on_one_days", nullable = false)
    val staleOneOnOneDays: Int = 14,

    @Column(name = "anniversary_lookahead_days", nullable = false)
    val anniversaryLookaheadDays: Int = 30,

    @Column(name = "theme", nullable = false)
    val theme: String = "DARK",

    @Column(name = "show_achievements", nullable = false)
    val showAchievements: Boolean = true,

    @Column(name = "notify_action_item_overdue", nullable = false)
    val notifyActionItemOverdue: Boolean = true,

    @Column(name = "notify_action_item_due_soon", nullable = false)
    val notifyActionItemDueSoon: Boolean = true,

    @Column(name = "notify_stale_one_on_one", nullable = false)
    val notifyStaleOneOnOne: Boolean = true,

    @Column(name = "notify_upcoming_anniversary", nullable = false)
    val notifyUpcomingAnniversary: Boolean = true,

    @Column(name = "ai_enabled", nullable = false)
    val aiEnabled: Boolean = false,

    @Column(name = "ai_api_base_url")
    val aiApiBaseUrl: String? = null,

    @Column(name = "ai_api_key")
    val aiApiKey: String? = null,

    @Column(name = "ai_model_name")
    val aiModelName: String? = null,

    @Column(name = "ai_privacy_mode", nullable = false)
    val aiPrivacyMode: Boolean = true,

    @Column(name = "ai_writing_style", nullable = false)
    val aiWritingStyle: String = "NARRATIVE",

    @Column(name = "kudos_refinement_prompt", columnDefinition = "TEXT")
    val kudosRefinementPrompt: String? = null,

    @Column(name = "pdp_optimization_prompt", columnDefinition = "TEXT")
    val pdpOptimizationPrompt: String? = null,

    @Column(name = "agenda_prep_prompt", columnDefinition = "TEXT")
    val agendaPrepPrompt: String? = null,

    @Column(name = "narrative_prompt", columnDefinition = "TEXT")
    val narrativePrompt: String? = null,

    @Column(name = "outcome_extractor_prompt", columnDefinition = "TEXT")
    val outcomeExtractorPrompt: String? = null,

    @Column(name = "trend_radar_prompt", columnDefinition = "TEXT")
    val trendRadarPrompt: String? = null,

    @Column(name = "link_suggestions_prompt", columnDefinition = "TEXT")
    val linkSuggestionsPrompt: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now()
)
