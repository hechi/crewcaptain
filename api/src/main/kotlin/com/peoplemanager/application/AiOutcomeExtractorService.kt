package com.peoplemanager.application

import com.peoplemanager.application.ports.AiClientPort
import com.peoplemanager.application.ports.AiCompletionResult
import com.peoplemanager.application.ports.ActionItemRepository
import com.peoplemanager.application.ports.OneOnOneEntryRepository
import com.peoplemanager.application.ports.PersonRepository
import com.peoplemanager.application.ports.UserSettingsRepository
import com.peoplemanager.domain.ActionItemOwnerType
import com.peoplemanager.domain.OneOnOneEntryId
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.UserId
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class AiOutcomeExtractorService(
    private val userSettingsRepository: UserSettingsRepository,
    private val personRepository: PersonRepository,
    private val entryRepository: OneOnOneEntryRepository,
    private val actionItemRepository: ActionItemRepository,
    private val aiClientPort: AiClientPort,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(AiOutcomeExtractorService::class.java)

    /**
     * Extracts action items and decisions from a 1:1 entry's notes using AI.
     * Respects privacy mode: refuses to process sensitive entries when ai_privacy_mode is ON.
     */
    fun extractOutcomes(userId: UserId, personId: PersonId, entryId: OneOnOneEntryId): AiExtractionResult {
        // Verify person belongs to user
        val person = personRepository.findByIdAndUserId(personId, userId)
            ?: throw PersonNotFoundException(personId)

        // Load entry
        val entry = entryRepository.findByIdAndUserIdAndPersonId(entryId, userId, personId)
            ?: throw OneOnOneEntryNotFoundException(entryId)

        // Load user settings
        val settings = userSettingsRepository.findByUserId(userId)
            ?: return AiExtractionResult.Error("AI Assistant is not configured. Please configure it in Settings.")

        if (!settings.isAiConfigured()) {
            return AiExtractionResult.Error("AI Assistant is not enabled or not fully configured. Please check Settings.")
        }

        // Privacy check: refuse to process sensitive entries when privacy mode is ON
        if (entry.sensitive && settings.aiPrivacyMode) {
            return AiExtractionResult.Error(
                "Cannot extract outcomes from a sensitive entry while AI Privacy Mode is enabled. " +
                "Disable AI Privacy Mode in Settings to use this feature on sensitive entries."
            )
        }

        // Check notes are not empty
        if (entry.notesMarkdown.isNullOrBlank()) {
            return AiExtractionResult.Error("Cannot extract outcomes: the notes field is empty.")
        }

        // Build user message with context
        val userMessage = buildString {
            append("Person: ${person.name}")
            if (!person.roleTitle.isNullOrBlank()) {
                append(" (${person.roleTitle})")
            }
            append("\n\nMeeting Notes:\n${entry.notesMarkdown}")
        }

        // Call the LLM
        val result = aiClientPort.chatCompletion(
            baseUrl = settings.aiApiBaseUrl!!,
            apiKey = settings.aiApiKey,
            model = settings.aiModelName!!,
            systemPrompt = settings.effectiveOutcomeExtractorPrompt(),
            userMessage = userMessage
        )

        return when (result) {
            is AiCompletionResult.Success -> parseExtractionResult(result.content)
            is AiCompletionResult.Error -> AiExtractionResult.Error(result.message)
        }
    }

    /**
     * Applies extracted outcomes: bulk-creates action items and appends decisions to the entry's outcomes field.
     */
    @Transactional
    fun applyOutcomes(userId: UserId, personId: PersonId, entryId: OneOnOneEntryId, command: ApplyOutcomesCommand): ApplyOutcomesResult {
        // Verify person belongs to user
        personRepository.findByIdAndUserId(personId, userId)
            ?: throw PersonNotFoundException(personId)

        // Load entry
        val entry = entryRepository.findByIdAndUserIdAndPersonId(entryId, userId, personId)
            ?: throw OneOnOneEntryNotFoundException(entryId)

        // Create action items
        val createdItems = command.actionItems.map { item ->
            val ownerType = try {
                ActionItemOwnerType.valueOf(item.ownerType)
            } catch (e: IllegalArgumentException) {
                ActionItemOwnerType.MANAGER
            }

            val dueDate = item.suggestedDaysToDue?.let { days ->
                LocalDate.now().plusDays(days.toLong())
            }

            val actionItem = com.peoplemanager.domain.ActionItem(
                id = com.peoplemanager.domain.ActionItemId.generate(),
                userId = userId,
                personId = personId,
                title = item.title,
                ownerType = ownerType,
                dueDate = dueDate,
                originatingEntryId = entryId
            )
            actionItemRepository.save(actionItem)
        }

        // Append decisions to outcomes
        if (command.decisions.isNotEmpty()) {
            val decisionsText = command.decisions.joinToString("\n") { "- $it" }
            val newOutcomes = if (!entry.outcomesMarkdown.isNullOrBlank()) {
                "${entry.outcomesMarkdown}\n\n### Extracted Decisions\n$decisionsText"
            } else {
                "### Extracted Decisions\n$decisionsText"
            }
            val updatedEntry = entry.updateOutcomes(newOutcomes)
            entryRepository.save(updatedEntry)
        }

        return ApplyOutcomesResult(
            actionItemsCreated = createdItems.size,
            decisionsAppended = command.decisions.size
        )
    }

    internal fun parseExtractionResult(content: String): AiExtractionResult {
        return try {
            // Strip markdown code fences if present
            val cleanedContent = content
                .replace(Regex("^```json\\s*", RegexOption.MULTILINE), "")
                .replace(Regex("^```\\s*", RegexOption.MULTILINE), "")
                .trim()

            val parsed = objectMapper.readValue(cleanedContent, AiExtractionResponse::class.java)

            AiExtractionResult.Success(
                actionItems = parsed.action_items?.map { item ->
                    ExtractedActionItem(
                        title = item.title ?: "",
                        ownerType = item.owner_type?.uppercase() ?: "MANAGER",
                        suggestedDaysToDue = item.suggested_days_to_due
                    )
                }?.filter { it.title.isNotBlank() } ?: emptyList(),
                decisions = parsed.decisions?.filter { it.isNotBlank() } ?: emptyList()
            )
        } catch (e: Exception) {
            logger.warn("Failed to parse AI extraction response: ${e.message}")
            AiExtractionResult.Error("Failed to parse AI response. The model may not have returned valid JSON.")
        }
    }
}

// --- Result types ---

sealed class AiExtractionResult {
    data class Success(
        val actionItems: List<ExtractedActionItem>,
        val decisions: List<String>
    ) : AiExtractionResult()

    data class Error(val message: String) : AiExtractionResult()
}

data class ExtractedActionItem(
    val title: String,
    val ownerType: String, // "MANAGER" or "PERSON"
    val suggestedDaysToDue: Int?
)

data class ApplyOutcomesCommand(
    val actionItems: List<ApplyActionItem>,
    val decisions: List<String>
)

data class ApplyActionItem(
    val title: String,
    val ownerType: String,
    val suggestedDaysToDue: Int?
)

data class ApplyOutcomesResult(
    val actionItemsCreated: Int,
    val decisionsAppended: Int
)

// --- Internal DTO for parsing AI JSON response ---

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class AiExtractionResponse(
    val action_items: List<AiActionItemDto>? = null,
    val decisions: List<String>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class AiActionItemDto(
    val title: String? = null,
    val owner_type: String? = null,
    val suggested_days_to_due: Int? = null
)
