package com.peoplemanager.application

import com.peoplemanager.application.port.output.AiClientPort
import com.peoplemanager.application.port.output.AiCompletionResult
import com.peoplemanager.application.port.output.PdpGoalRepository
import com.peoplemanager.application.port.output.PersonRepository
import com.peoplemanager.application.port.output.StrategyGoalRepository
import com.peoplemanager.application.port.output.UserSettingsRepository
import com.peoplemanager.domain.PdpGoalId
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.StrategyGoalId
import com.peoplemanager.domain.StrategyGoalStatus
import com.peoplemanager.domain.UserId
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AiLinkDiscoveryService(
    private val strategyGoalRepository: StrategyGoalRepository,
    private val pdpGoalRepository: PdpGoalRepository,
    private val personRepository: PersonRepository,
    private val linkService: StrategyGoalLinkService,
    private val userSettingsRepository: UserSettingsRepository,
    private val aiClientPort: AiClientPort,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(AiLinkDiscoveryService::class.java)

    data class LinkSuggestion(
        val strategyGoalId: StrategyGoalId,
        val strategyGoalTitle: String,
        val pdpGoalId: PdpGoalId,
        val personId: PersonId,
        val pdpGoalTitle: String,
        val personName: String,
        val matchScore: Int,
        val reasoning: String
    )

    fun generateLinkSuggestions(userId: UserId): AiLinkSuggestionsResult {
        logger.info("Generating AI link suggestions for user: ${userId.value}")

        // Load user settings to check AI configuration
        val settings = userSettingsRepository.findByUserId(userId)
            ?: run {
                logger.warn("No settings found for user: ${userId.value}")
                return AiLinkSuggestionsResult.Error("AI Assistant is not configured. Please configure it in Settings.")
            }

        if (!settings.isAiConfigured()) {
            logger.warn("AI not configured for user: ${userId.value} - enabled: ${settings.aiEnabled}, baseUrl: ${settings.aiApiBaseUrl}, model: ${settings.aiModelName}")
            return AiLinkSuggestionsResult.Error("AI Assistant is not enabled or not fully configured. Please check Settings.")
        }

        logger.info("AI configured with baseUrl: ${settings.aiApiBaseUrl}, model: ${settings.aiModelName}")

        // Get all active strategy goals (excluding sensitive ones)
        val pageable = PageRequest.of(0, 1000)
        val strategyGoals = strategyGoalRepository.findAllByUserId(
            userId, StrategyGoalStatus.ACTIVE, pageable
        ).content.filter { !it.sensitive }

        logger.info("Found ${strategyGoals.size} non-sensitive strategy goals for user: ${userId.value}")

        if (strategyGoals.isEmpty()) {
            return AiLinkSuggestionsResult.Error("No active strategy goals found. Create some strategy goals first.")
        }

        // Get all active PDP goals across all persons
        val persons = personRepository.findAllByUserIdUnpaged(userId)
        val allPdpGoals = persons.flatMap { person ->
            pdpGoalRepository.findAllByUserIdAndPersonId(
                userId, person.id, com.peoplemanager.domain.PdpGoalStatus.ACTIVE, pageable
            ).content.map { goal ->
                Triple(goal, person.id, person.preferredName ?: person.name)
            }
        }

        logger.info("Found ${allPdpGoals.size} active PDP goals for user: ${userId.value}")

        if (allPdpGoals.isEmpty()) {
            return AiLinkSuggestionsResult.Error("No active PDP goals found. Create some PDP goals first.")
        }

        // Get existing links to avoid suggesting already linked goals
        val existingLinks = linkService.getAllAlignmentScores(userId)
            .flatMap { score ->
                linkService.getLinkedPdpGoals(score.strategyGoalId, userId)
                    .map { it.pdpGoalId to score.strategyGoalId }
            }
            .toSet()

        logger.info("Found ${existingLinks.size} existing links for user: ${userId.value}")

        // Filter out already linked goals
        val unlinkedPdpGoals = allPdpGoals.filter { (goal, _, _) ->
            !existingLinks.any { (linkedPdpId, _) -> linkedPdpId == goal.id }
        }

        logger.info("Found ${unlinkedPdpGoals.size} unlinked PDP goals for user: ${userId.value}")

        if (unlinkedPdpGoals.isEmpty()) {
            return AiLinkSuggestionsResult.Error("All PDP goals are already linked to strategy goals.")
        }

        // Build the prompt with all goals
        val userPrompt = buildUserPrompt(strategyGoals, unlinkedPdpGoals)
        logger.debug("Built user prompt with ${strategyGoals.size} strategy goals and ${unlinkedPdpGoals.size} PDP goals")

        // Call the LLM
        logger.info("Calling AI API at ${settings.aiApiBaseUrl} with model ${settings.aiModelName}")
        val result = aiClientPort.chatCompletion(
            baseUrl = settings.aiApiBaseUrl!!,
            apiKey = settings.aiApiKey,
            model = settings.aiModelName!!,
            systemPrompt = settings.effectiveLinkSuggestionsPrompt(),
            userMessage = userPrompt
        )

        return when (result) {
            is AiCompletionResult.Success -> {
                logger.info("AI API call successful, received ${result.content.length} characters")
                parseSuggestions(result.content, strategyGoals, unlinkedPdpGoals)
            }
            is AiCompletionResult.Error -> {
                logger.error("AI API call failed: ${result.message}")
                AiLinkSuggestionsResult.Error(result.message)
            }
        }
    }

    private fun buildUserPrompt(
        strategyGoals: List<com.peoplemanager.domain.StrategyGoal>,
        pdpGoals: List<Triple<com.peoplemanager.domain.PdpGoal, PersonId, String>>
    ): String {
        val sections = mutableListOf<String>()

        // Strategy Goals section
        val strategySection = strategyGoals.joinToString("\n") { goal ->
            val desc = goal.description?.let { " - $it" } ?: ""
            "- [${goal.id.value}] ${goal.title}$desc"
        }
        sections.add("## Strategy Goals (Active)\n$strategySection")

        // PDP Goals section
        val pdpSection = pdpGoals.joinToString("\n") { (goal, personId, personName) ->
            val desc = goal.description?.let { " - $it" } ?: ""
            val target = goal.targetDate?.let { " (Target: $it)" } ?: ""
            "- PDP Goal ID: [${goal.id.value}] | Person ID: [${personId.value}] | Person: $personName | Title: ${goal.title}$desc$target"
        }
        sections.add("## PDP Goals (Active, Unlinked)\n$pdpSection")

        return sections.joinToString("\n\n")
    }

    internal fun parseSuggestions(
        content: String,
        strategyGoals: List<com.peoplemanager.domain.StrategyGoal>,
        pdpGoals: List<Triple<com.peoplemanager.domain.PdpGoal, PersonId, String>>
    ): AiLinkSuggestionsResult {
        return try {
            // Try to parse JSON response
            val cleanContent = content
                .replace(Regex("^```json\\s*", RegexOption.MULTILINE), "")
                .replace(Regex("^```\\s*$", RegexOption.MULTILINE), "")
                .trim()

            logger.debug("Parsing AI response: ${cleanContent.take(500)}")

            val rawSuggestions: List<LinkSuggestionResponse>? = parseSuggestionsJson(cleanContent, objectMapper)

            if (rawSuggestions.isNullOrEmpty()) {
                return AiLinkSuggestionsResult.Error("AI returned no suggestions. Try creating more strategy goals or PDP goals.")
            }

            logger.info("Parsing ${rawSuggestions.size} suggestions from AI response")
            logger.debug("Available strategy goal IDs: ${strategyGoals.map { it.id.value }}")
            logger.debug("Available PDP goal IDs: ${pdpGoals.map { it.first.id.value }}")

            val suggestions = rawSuggestions.mapNotNull { suggestion ->
                try {
                    logger.debug("Processing suggestion: strategyGoalId=${suggestion.strategyGoalId}, pdpGoalId=${suggestion.pdpGoalId}")
                    
                    val strategyGoalId = StrategyGoalId(java.util.UUID.fromString(suggestion.strategyGoalId))
                    val pdpGoalId = PdpGoalId(java.util.UUID.fromString(suggestion.pdpGoalId))
                    val personId = PersonId(java.util.UUID.fromString(suggestion.personId))

                    val strategyGoal = strategyGoals.find { it.id == strategyGoalId }
                    val pdpGoal = pdpGoals.find { it.first.id == pdpGoalId }

                    if (strategyGoal == null) {
                        logger.warn("Strategy goal not found: $strategyGoalId")
                        null
                    } else if (pdpGoal == null) {
                        logger.warn("PDP goal not found: $pdpGoalId")
                        null
                    } else {
                        logger.info("Valid suggestion found: ${suggestion.strategyGoalTitle} -> ${suggestion.pdpGoalTitle}")
                        LinkSuggestion(
                            strategyGoalId = strategyGoalId,
                            strategyGoalTitle = strategyGoal.title,
                            pdpGoalId = pdpGoalId,
                            personId = personId,
                            pdpGoalTitle = pdpGoal.first.title,
                            personName = pdpGoal.third,
                            matchScore = (suggestion.matchScore ?: 50).coerceIn(0, 100),
                            reasoning = suggestion.reasoning ?: "Suggested by AI analysis"
                        )
                    }
                } catch (e: Exception) {
                    logger.warn("Failed to parse suggestion: ${e.message}")
                    null
                }
            }.sortedByDescending { it.matchScore }

            logger.info("Parsed ${suggestions.size} valid suggestions from ${rawSuggestions.size} total")

            if (suggestions.isEmpty()) {
                AiLinkSuggestionsResult.Error("No valid suggestions could be parsed from AI response. The AI may have returned incorrect goal IDs.")
            } else {
                AiLinkSuggestionsResult.Success(suggestions)
            }
        } catch (e: Exception) {
            logger.error("Failed to parse AI response: ${e.message}", e)
            AiLinkSuggestionsResult.Error("Failed to parse AI response: ${e.message}. Please try again.")
        }
    }
}

private fun parseSuggestionsJson(content: String, objectMapper: ObjectMapper): List<LinkSuggestionResponse>? {
    return try {
        val response = objectMapper.readValue(content, LinkSuggestionsAiResponse::class.java)
        response.suggestions
    } catch (e: Exception) {
        try {
            objectMapper.readValue(content, objectMapper.typeFactory.constructCollectionType(List::class.java, LinkSuggestionResponse::class.java))
        } catch (e2: Exception) {
            null
        }
    }
}

// --- Result types ---

sealed class AiLinkSuggestionsResult {
    data class Success(val suggestions: List<AiLinkDiscoveryService.LinkSuggestion>) : AiLinkSuggestionsResult()
    data class Error(val message: String) : AiLinkSuggestionsResult()
}

// --- Internal data classes ---

@JsonIgnoreProperties(ignoreUnknown = true)
data class LinkSuggestionsAiResponse(
    val suggestions: List<LinkSuggestionResponse>?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class LinkSuggestionResponse(
    val strategyGoalId: String?,
    val pdpGoalId: String?,
    val personId: String?,
    val personName: String?,
    val strategyGoalTitle: String?,
    val pdpGoalTitle: String?,
    val matchScore: Int?,
    val reasoning: String?
)
