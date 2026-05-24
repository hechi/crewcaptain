package com.peoplemanager.domain

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.util.UUID

class UserSettingsPromptsTest {

    private val userId = UserId(UUID.randomUUID())

    @Test
    fun `effectiveKudosRefinementPrompt should return default when no custom prompt is set`() {
        val settings = UserSettings.createDefault(userId)

        settings.effectiveKudosRefinementPrompt() shouldBe UserSettings.DEFAULT_KUDOS_REFINEMENT_PROMPT
    }

    @Test
    fun `effectiveKudosRefinementPrompt should return custom prompt when set`() {
        val customPrompt = "Be a radical candor coach."
        val settings = UserSettings.createDefault(userId).copy(kudosRefinementPrompt = customPrompt)

        settings.effectiveKudosRefinementPrompt() shouldBe customPrompt
    }

    @Test
    fun `effectiveKudosRefinementPrompt should return default when custom prompt is blank`() {
        val settings = UserSettings.createDefault(userId).copy(kudosRefinementPrompt = "   ")

        settings.effectiveKudosRefinementPrompt() shouldBe UserSettings.DEFAULT_KUDOS_REFINEMENT_PROMPT
    }

    @Test
    fun `effectivePdpOptimizationPrompt should return default when no custom prompt is set`() {
        val settings = UserSettings.createDefault(userId)

        settings.effectivePdpOptimizationPrompt() shouldBe UserSettings.DEFAULT_PDP_OPTIMIZATION_PROMPT
    }

    @Test
    fun `effectivePdpOptimizationPrompt should return custom prompt when set`() {
        val customPrompt = "Rewrite for executive review."
        val settings = UserSettings.createDefault(userId).copy(pdpOptimizationPrompt = customPrompt)

        settings.effectivePdpOptimizationPrompt() shouldBe customPrompt
    }

    @Test
    fun `effectivePdpOptimizationPrompt should return default when custom prompt is blank`() {
        val settings = UserSettings.createDefault(userId).copy(pdpOptimizationPrompt = "")

        settings.effectivePdpOptimizationPrompt() shouldBe UserSettings.DEFAULT_PDP_OPTIMIZATION_PROMPT
    }

    @Test
    fun `effectiveAgendaPrepPrompt should return default when no custom prompt is set`() {
        val settings = UserSettings.createDefault(userId)

        settings.effectiveAgendaPrepPrompt() shouldBe UserSettings.DEFAULT_AGENDA_PREP_PROMPT
    }

    @Test
    fun `effectiveAgendaPrepPrompt should return custom prompt when set`() {
        val customPrompt = "Focus on career growth topics only."
        val settings = UserSettings.createDefault(userId).copy(agendaPrepPrompt = customPrompt)

        settings.effectiveAgendaPrepPrompt() shouldBe customPrompt
    }

    @Test
    fun `effectiveNarrativePrompt should return default when no custom prompt is set`() {
        val settings = UserSettings.createDefault(userId)

        settings.effectiveNarrativePrompt() shouldBe UserSettings.DEFAULT_NARRATIVE_PROMPT
    }

    @Test
    fun `effectiveNarrativePrompt should return custom prompt when set`() {
        val customPrompt = "Write in a concise executive style."
        val settings = UserSettings.createDefault(userId).copy(narrativePrompt = customPrompt)

        settings.effectiveNarrativePrompt() shouldBe customPrompt
    }
}
