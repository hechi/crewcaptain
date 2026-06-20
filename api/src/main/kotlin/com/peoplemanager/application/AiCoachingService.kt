package com.peoplemanager.application

import com.peoplemanager.application.port.output.AiClientPort
import com.peoplemanager.application.port.output.AiCompletionResult
import com.peoplemanager.application.port.output.UserSettingsRepository
import com.peoplemanager.domain.UserId
import com.peoplemanager.domain.UserSettings
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for AI-powered coaching features:
 * - Kudos refinement using SBI framework
 * - PDP Goal optimization using SMART criteria
 *
 * Respects user-customizable prompts stored in UserSettings.
 */
@Service
@Transactional(readOnly = true)
class AiCoachingService(
    private val userSettingsRepository: UserSettingsRepository,
    private val aiClientPort: AiClientPort,
    private val aiConfigResolver: AiConfigResolver
) {

    /**
     * Refines a kudos draft using the SBI framework (or user's custom prompt).
     * The prompt must contain {{draft}} placeholder which will be replaced with the user's text.
     */
    fun refineKudos(userId: UserId, draft: String): AiCoachingResult {
        val settings = loadAndValidateSettings(userId) ?: return AiCoachingResult.Error(
            "AI Assistant is not configured. Please configure it in Settings or ask your admin to set team defaults."
        )

        if (draft.isBlank()) {
            return AiCoachingResult.Error("Kudos draft cannot be empty.")
        }

        val systemPrompt = settings.effectiveKudosRefinementPrompt()
        val userMessage = "Draft: $draft"

        return callAi(settings, systemPrompt, userMessage)
    }

    /**
     * Optimizes a PDP goal using SMART criteria (or user's custom prompt).
     * The prompt must contain {{title}} and {{description}} placeholders.
     */
    fun optimizePdpGoal(userId: UserId, title: String, description: String?): AiCoachingResult {
        val settings = loadAndValidateSettings(userId) ?: return AiCoachingResult.Error(
            "AI Assistant is not configured. Please configure it in Settings or ask your admin to set team defaults."
        )

        if (title.isBlank()) {
            return AiCoachingResult.Error("Goal title cannot be empty.")
        }

        val systemPrompt = settings.effectivePdpOptimizationPrompt()
        val userMessage = buildString {
            append("Title: $title")
            if (!description.isNullOrBlank()) {
                append("\nDescription: $description")
            }
        }

        return callAi(settings, systemPrompt, userMessage)
    }

    /**
     * Optimizes a strategy goal using SMART criteria (or user's custom prompt).
     * The prompt must contain {{title}} and {{description}} placeholders.
     */
    fun optimizeStrategyGoal(userId: UserId, title: String, description: String?): AiCoachingResult {
        val settings = loadAndValidateSettings(userId) ?: return AiCoachingResult.Error(
            "AI Assistant is not configured. Please configure it in Settings or ask your admin to set team defaults."
        )

        if (title.isBlank()) {
            return AiCoachingResult.Error("Strategy goal title cannot be empty.")
        }

        val systemPrompt = settings.effectiveStrategyOptimizationPrompt()
        val userMessage = buildString {
            append("Title: $title")
            if (!description.isNullOrBlank()) {
                append("\nDescription: $description")
            }
        }

        return callAi(settings, systemPrompt, userMessage)
    }

    private fun loadAndValidateSettings(userId: UserId): UserSettings? {
        val settings = userSettingsRepository.findByUserId(userId)
            ?: UserSettings.createDefault(userId)

        if (aiConfigResolver.resolve(settings) == null) {
            return null
        }

        return settings
    }

    private fun callAi(settings: UserSettings, systemPrompt: String, userMessage: String): AiCoachingResult {
        val config = aiConfigResolver.resolve(settings)!!
        val result = aiClientPort.chatCompletion(
            baseUrl = config.baseUrl,
            apiKey = config.apiKey,
            model = config.model,
            systemPrompt = systemPrompt,
            userMessage = userMessage
        )

        return when (result) {
            is AiCompletionResult.Success -> AiCoachingResult.Success(result.content)
            is AiCompletionResult.Error -> AiCoachingResult.Error(result.message)
        }
    }
}

sealed class AiCoachingResult {
    data class Success(val content: String) : AiCoachingResult()
    data class Error(val message: String) : AiCoachingResult()
}
