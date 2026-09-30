package com.peoplemanager.application

import com.peoplemanager.application.commands.GenerateFeedbackTemplateCommand
import com.peoplemanager.application.port.output.AiClientPort
import com.peoplemanager.application.port.output.AiCompletionResult
import com.peoplemanager.application.port.output.UserSettingsRepository
import com.peoplemanager.domain.FeedbackQuestion
import com.peoplemanager.domain.FeedbackQuestionType
import com.peoplemanager.domain.FeedbackTemplate
import com.peoplemanager.domain.FeedbackTemplateId
import com.peoplemanager.domain.ShowIfOperator
import com.peoplemanager.domain.ShowIfRule
import com.peoplemanager.domain.UserId
import com.peoplemanager.domain.UserSettings
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import tools.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Generates a draft feedback template from a manager's short brief using the configured LLM.
 *
 * The AI's JSON output is parsed and then validated by constructing a real [FeedbackTemplate]
 * (which enforces all domain invariants). Invalid AI output surfaces as an error rather than
 * a broken template. The result is NOT persisted; the manager edits and saves it.
 */
@Service
@Transactional(readOnly = true)
class FeedbackTemplateAiService(
    private val userSettingsRepository: UserSettingsRepository,
    private val aiClientPort: AiClientPort,
    private val aiConfigResolver: AiConfigResolver,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(FeedbackTemplateAiService::class.java)

    fun generate(command: GenerateFeedbackTemplateCommand): FeedbackTemplateGenerationResult {
        if (command.brief.isBlank()) {
            return FeedbackTemplateGenerationResult.Error("Please provide a short brief to generate a template.")
        }

        val settings = userSettingsRepository.findByUserId(command.userId)
            ?: UserSettings.createDefault(command.userId)

        val config = aiConfigResolver.resolve(settings)
            ?: return FeedbackTemplateGenerationResult.Error(
                "AI Assistant is not configured. Please configure it in Settings or ask your admin to set team defaults."
            )

        val result = aiClientPort.chatCompletion(
            baseUrl = config.baseUrl,
            apiKey = config.apiKey,
            model = config.model,
            systemPrompt = settings.effectiveFeedbackTemplatePrompt(),
            userMessage = "Brief: ${command.brief}"
        )

        return when (result) {
            is AiCompletionResult.Success -> parseAndValidate(command.userId, result.content)
            is AiCompletionResult.Error -> FeedbackTemplateGenerationResult.Error(result.message)
        }
    }

    internal fun parseAndValidate(userId: UserId, content: String): FeedbackTemplateGenerationResult {
        return try {
            val cleaned = content
                .replace(Regex("^```json\\s*", RegexOption.MULTILINE), "")
                .replace(Regex("^```\\s*", RegexOption.MULTILINE), "")
                .trim()

            val dto = objectMapper.readValue(cleaned, AiTemplateDto::class.java)

            val questions = (dto.questions ?: emptyList()).mapIndexedNotNull { index, q ->
                val id = q.id?.takeIf { it.isNotBlank() } ?: "q${index + 1}"
                val text = q.text?.takeIf { it.isNotBlank() } ?: return@mapIndexedNotNull null
                val type = parseType(q.type) ?: return@mapIndexedNotNull null
                FeedbackQuestion(
                    id = id,
                    type = type,
                    text = text.take(500),
                    required = q.required ?: false,
                    lowLabel = q.lowLabel?.takeIf { it.isNotBlank() && type == FeedbackQuestionType.RATING },
                    highLabel = q.highLabel?.takeIf { it.isNotBlank() && type == FeedbackQuestionType.RATING },
                    showIf = parseShowIf(q.showIf)
                )
            }

            if (questions.isEmpty()) {
                return FeedbackTemplateGenerationResult.Error(
                    "The AI did not return any usable questions. Try rephrasing your brief."
                )
            }

            // Constructing the template enforces all domain invariants (unique ids,
            // single-level branching, question limit, etc). Any violation throws.
            val template = FeedbackTemplate(
                id = FeedbackTemplateId.generate(),
                userId = userId,
                title = (dto.title?.takeIf { it.isNotBlank() } ?: "Feedback template").take(200),
                description = dto.description?.takeIf { it.isNotBlank() },
                questions = questions
            )

            FeedbackTemplateGenerationResult.Success(template)
        } catch (e: Exception) {
            logger.warn("Failed to parse AI feedback template: ${e.javaClass.simpleName}: ${e.message}")
            FeedbackTemplateGenerationResult.Error(
                "The AI response could not be turned into a valid template. Please try again or build one manually."
            )
        }
    }

    private fun parseType(raw: String?): FeedbackQuestionType? =
        when (raw?.trim()?.uppercase()) {
            "RATING" -> FeedbackQuestionType.RATING
            "LIKERT" -> FeedbackQuestionType.LIKERT
            "TEXT" -> FeedbackQuestionType.TEXT
            else -> null
        }

    private fun parseShowIf(dto: AiShowIfDto?): ShowIfRule? {
        if (dto?.questionId.isNullOrBlank() || dto?.value == null) return null
        val op = when (dto.operator?.trim()?.uppercase()) {
            "LTE" -> ShowIfOperator.LTE
            "GTE" -> ShowIfOperator.GTE
            "EQ" -> ShowIfOperator.EQ
            else -> return null
        }
        val v = dto.value.coerceIn(1, 5)
        return ShowIfRule(dto.questionId, op, v)
    }
}

sealed class FeedbackTemplateGenerationResult {
    data class Success(val template: FeedbackTemplate) : FeedbackTemplateGenerationResult()
    data class Error(val message: String) : FeedbackTemplateGenerationResult()
}

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class AiTemplateDto(
    val title: String? = null,
    val description: String? = null,
    val questions: List<AiQuestionDto>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class AiQuestionDto(
    val id: String? = null,
    val type: String? = null,
    val text: String? = null,
    val required: Boolean? = null,
    val lowLabel: String? = null,
    val highLabel: String? = null,
    val showIf: AiShowIfDto? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class AiShowIfDto(
    val questionId: String? = null,
    val operator: String? = null,
    val value: Int? = null
)
