package com.peoplemanager.application

import com.peoplemanager.application.commands.GenerateFeedbackSummaryCommand
import com.peoplemanager.application.port.output.AiClientPort
import com.peoplemanager.application.port.output.AiCompletionResult
import com.peoplemanager.application.port.output.FeedbackResponseRepository
import com.peoplemanager.application.port.output.PersonRepository
import com.peoplemanager.application.port.output.UserSettingsRepository
import com.peoplemanager.domain.FeedbackResponse
import com.peoplemanager.domain.UserSettings
import com.peoplemanager.domain.service.AnalyticsBucket
import com.peoplemanager.domain.service.FeedbackAnalyticsCalculator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZoneOffset

/**
 * Generates an on-demand AI narrative summarizing a person's approved peer feedback.
 * The result is returned to the manager to edit and (optionally) save; it is not persisted here.
 */
@Service
@Transactional(readOnly = true)
class FeedbackSummaryAiService(
    private val userSettingsRepository: UserSettingsRepository,
    private val personRepository: PersonRepository,
    private val feedbackResponseRepository: FeedbackResponseRepository,
    private val aiClientPort: AiClientPort,
    private val aiConfigResolver: AiConfigResolver
) {

    fun generate(command: GenerateFeedbackSummaryCommand): FeedbackSummaryResult {
        val person = personRepository.findByIdAndUserId(command.personId, command.userId)
            ?: throw PersonNotFoundException(command.personId)

        val settings = userSettingsRepository.findByUserId(command.userId)
            ?: UserSettings.createDefault(command.userId)

        val config = aiConfigResolver.resolve(settings)
            ?: return FeedbackSummaryResult.Error(
                "AI Assistant is not configured. Please configure it in Settings or ask your admin to set team defaults."
            )

        val responses = usableResponses(command)
        if (responses.isEmpty()) {
            return FeedbackSummaryResult.Error("There is no approved feedback to summarize yet.")
        }

        val analytics = FeedbackAnalyticsCalculator.compute(responses, AnalyticsBucket.QUARTER)
        val userMessage = buildString {
            append("Person: ${person.name}")
            if (!person.roleTitle.isNullOrBlank()) append(" (${person.roleTitle})")
            append("\n\nResponses: ${responses.size}")
            analytics.overallAverageRating?.let { append("\nAverage rating: ${"%.2f".format(it)}/5") }
            if (analytics.topThemes.isNotEmpty()) {
                append("\nTop themes: ${analytics.topThemes.joinToString(", ") { "${it.theme} (${it.count})" }}")
            }
            append("\n\nFree-text feedback:")
            responses.flatMap { it.freeTextParts() }.take(40).forEach { append("\n- $it") }
        }

        val result = aiClientPort.chatCompletion(
            baseUrl = config.baseUrl,
            apiKey = config.apiKey,
            model = config.model,
            systemPrompt = settings.effectiveFeedbackSummaryPrompt(),
            userMessage = userMessage
        )

        return when (result) {
            is AiCompletionResult.Success -> FeedbackSummaryResult.Success(result.content.trim(), responses.size)
            is AiCompletionResult.Error -> FeedbackSummaryResult.Error(result.message)
        }
    }

    private fun usableResponses(command: GenerateFeedbackSummaryCommand): List<FeedbackResponse> {
        return feedbackResponseRepository.findAllByUserIdAndPersonId(command.userId, command.personId)
            .filter { it.isUsable }
            .filter { r ->
                val d = r.createdAt.atZone(ZoneOffset.UTC).toLocalDate()
                (command.from == null || !d.isBefore(command.from)) &&
                    (command.to == null || !d.isAfter(command.to))
            }
    }
}

sealed class FeedbackSummaryResult {
    data class Success(val content: String, val responseCount: Int) : FeedbackSummaryResult()
    data class Error(val message: String) : FeedbackSummaryResult()
}
