package com.peoplemanager.adapters.web.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

data class UpdateUserSettingsRequest(
    @field:Min(1) @field:Max(30)
    val dueSoonDays: Int,

    @field:Min(1) @field:Max(90)
    val staleOneOnOneDays: Int,

    @field:Min(1) @field:Max(90)
    val anniversaryLookaheadDays: Int,

    val theme: String,

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
    val aiWritingStyle: String = "NARRATIVE",
    val kudosRefinementPrompt: String? = null,
    val pdpOptimizationPrompt: String? = null,
    val agendaPrepPrompt: String? = null,
    val narrativePrompt: String? = null
)
