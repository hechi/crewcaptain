package com.peoplemanager.application

import com.peoplemanager.application.port.output.AiClientPort
import com.peoplemanager.application.port.output.AiCompletionResult
import com.peoplemanager.application.port.output.PersonRepository
import com.peoplemanager.application.port.output.UserSettingsRepository
import com.peoplemanager.domain.UserId
import com.peoplemanager.domain.UserSettings
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * AI Command Terminal service.
 * Accepts natural language input, sends it to the configured LLM with a structured
 * JSON system prompt and person directory context, then parses the response into
 * a typed command result.
 */
@Service
class AiCommandTerminalService(
    private val aiClientPort: AiClientPort,
    private val userSettingsRepository: UserSettingsRepository,
    private val personRepository: PersonRepository,
    private val objectMapper: ObjectMapper,
    private val aiConfigResolver: AiConfigResolver
) {

    private val logger = LoggerFactory.getLogger(AiCommandTerminalService::class.java)

    data class PersonDirectoryEntry(
        val id: String,
        val preferredName: String
    )

    data class CommandParseResult(
        val intent: String?,
        val targetPersonId: String?,
        val content: String?,
        val dueDate: String?,
        val meetingDate: String?,
        val tags: List<String>,
        val sensitive: Boolean,
        val error: String?
    ) {
        companion object {
            fun error(message: String) = CommandParseResult(
                intent = null,
                targetPersonId = null,
                content = null,
                dueDate = null,
                meetingDate = null,
                tags = emptyList(),
                sensitive = false,
                error = message
            )
        }
    }

    /**
     * Parse a natural language command using the configured AI.
     * Returns a structured result with the parsed intent, or an error message.
     */
    fun parseCommand(userId: UserId, userInput: String): CommandParseResult {
        if (userInput.isBlank()) {
            return CommandParseResult.error("Command input cannot be empty.")
        }

        val settings = userSettingsRepository.findByUserId(userId)
            ?: UserSettings.createDefault(userId)

        val config = aiConfigResolver.resolve(settings)
            ?: return CommandParseResult.error("AI Assistant is not configured. Please configure it in Settings or ask your admin to set team defaults.")

        // Build the person directory context
        val personDirectory = personRepository.findAllByUserIdUnpaged(userId)
            .map { PersonDirectoryEntry(id = it.id.value.toString(), preferredName = it.preferredName ?: it.name) }

        val directoryJson = objectMapper.writeValueAsString(personDirectory)

        val systemPrompt = settings.effectiveCommandTerminalPrompt()

        val now = java.time.LocalDate.now()
        val nowTime = java.time.LocalTime.now()
        val dayOfWeek = now.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }

        // Provide upcoming week dates so LLM doesn't need to compute date math
        val weekDates = (0..6).map { offset ->
            val date = now.plusDays(offset.toLong())
            val day = date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
            "$day = $date"
        }

        val userMessage = buildString {
            appendLine("Current Date: $now ($dayOfWeek)")
            appendLine("Current Time: ${nowTime.hour.toString().padStart(2, '0')}:${nowTime.minute.toString().padStart(2, '0')}")
            appendLine("This week's dates: ${weekDates.joinToString(", ")}")
            appendLine()
            appendLine("Person Directory:")
            appendLine(directoryJson)
            appendLine()
            appendLine("User Command:")
            appendLine(userInput)
        }

        return when (val result = aiClientPort.chatCompletion(
            baseUrl = config.baseUrl,
            apiKey = config.apiKey,
            model = config.model,
            systemPrompt = systemPrompt,
            userMessage = userMessage
        )) {
            is AiCompletionResult.Success -> parseAiResponse(result.content)
            is AiCompletionResult.Error -> CommandParseResult.error(result.message)
        }
    }

    /**
     * Get the person directory (lightweight list of id + preferred name)
     * for frontend micro-context injection.
     */
    fun getPersonDirectory(userId: UserId): List<PersonDirectoryEntry> {
        return personRepository.findAllByUserIdUnpaged(userId)
            .map { PersonDirectoryEntry(id = it.id.value.toString(), preferredName = it.preferredName ?: it.name) }
    }

    private fun parseAiResponse(content: String): CommandParseResult {
        // Try to extract JSON from the response (strip any accidental markdown fences)
        val jsonContent = content.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        return try {
            val parsed = objectMapper.readValue<Map<String, Any?>>(jsonContent)

            val intent = parsed["intent"] as? String
            val targetPersonId = parsed["target_person_id"] as? String
            val parsedContent = parsed["content"] as? String
            val dueDate = parsed["due_date"] as? String
            val meetingDate = parsed["meeting_date"] as? String
            val tags = (parsed["tags"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            val sensitive = parsed["sensitive"] as? Boolean ?: false

            // Validate intent
            val validIntents = setOf("create_action_item", "create_kudo", "create_quick_note", "create_one_on_one_entry")
            if (intent == null || intent !in validIntents) {
                return CommandParseResult.error("Could not determine the action. Please try rephrasing your command.")
            }

            if (parsedContent.isNullOrBlank()) {
                return CommandParseResult.error("Could not extract content from your command. Please try rephrasing.")
            }

            // For 1:1 entries, enforce today's date if LLM returned null or a clearly wrong date
            val effectiveMeetingDate = if (intent == "create_one_on_one_entry") {
                val today = java.time.LocalDate.now()
                when {
                    meetingDate == null -> today.toString()
                    else -> {
                        // Validate date is parseable and not absurdly far from today
                        try {
                            val parsed = java.time.LocalDate.parse(meetingDate)
                            val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(parsed, today)
                            // If date is more than 30 days in the past or future, likely hallucinated — use today
                            if (daysDiff > 30 || daysDiff < -30) {
                                logger.warn("AI returned suspicious meeting_date $meetingDate (${daysDiff}d from today). Overriding with today.")
                                today.toString()
                            } else {
                                meetingDate
                            }
                        } catch (e: Exception) {
                            today.toString()
                        }
                    }
                }
            } else meetingDate

            CommandParseResult(
                intent = intent,
                targetPersonId = targetPersonId,
                content = parsedContent,
                dueDate = dueDate,
                meetingDate = effectiveMeetingDate,
                tags = tags,
                sensitive = sensitive,
                error = null
            )
        } catch (e: Exception) {
            logger.warn("Failed to parse AI command response: ${e.message}. Raw content: ${content.take(200)}")
            CommandParseResult.error("Failed to parse AI response. Please try rephrasing your command.")
        }
    }
}
