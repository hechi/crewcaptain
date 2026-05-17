package com.peoplemanager.application

import com.peoplemanager.application.ports.AiClientPort
import com.peoplemanager.application.ports.AiCompletionResult
import com.peoplemanager.application.ports.ActionItemRepository
import com.peoplemanager.application.ports.KudosRepository
import com.peoplemanager.application.ports.OneOnOneEntryRepository
import com.peoplemanager.application.ports.PdpGoalRepository
import com.peoplemanager.application.ports.PdpUpdateRepository
import com.peoplemanager.application.ports.PersonRepository
import com.peoplemanager.application.ports.UserSettingsRepository
import com.peoplemanager.domain.ActionItemStatus
import com.peoplemanager.domain.PdpGoalStatus
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.UserId
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AiPrepService(
    private val userSettingsRepository: UserSettingsRepository,
    private val personRepository: PersonRepository,
    private val entryRepository: OneOnOneEntryRepository,
    private val actionItemRepository: ActionItemRepository,
    private val pdpGoalRepository: PdpGoalRepository,
    private val pdpUpdateRepository: PdpUpdateRepository,
    private val kudosRepository: KudosRepository,
    private val aiClientPort: AiClientPort
) {

    companion object {
        const val SYSTEM_PROMPT = "Act as a leadership coach. Based on these notes and progress items, " +
            "suggest 3-5 high-impact agenda items for the next 1:1 meeting. " +
            "Format each suggestion as a short, actionable bullet point. " +
            "Focus on follow-ups, blockers, growth opportunities, and recognition."
    }

    fun generateAgendaSuggestions(userId: UserId, personId: PersonId): AiPrepResult {
        // Verify person belongs to user
        val person = personRepository.findByIdAndUserId(personId, userId)
            ?: throw PersonNotFoundException(personId)

        // Load user settings to check AI configuration
        val settings = userSettingsRepository.findByUserId(userId)
            ?: return AiPrepResult.Error("AI Assistant is not configured. Please configure it in Settings.")

        if (!settings.isAiConfigured()) {
            return AiPrepResult.Error("AI Assistant is not enabled or not fully configured. Please check Settings.")
        }

        val privacyMode = settings.aiPrivacyMode

        // Gather context
        val context = buildContext(userId, personId, privacyMode)

        if (context.isBlank()) {
            return AiPrepResult.Error("No context available to generate suggestions. Add some 1:1 notes, action items, or goals first.")
        }

        // Call the LLM
        val result = aiClientPort.chatCompletion(
            baseUrl = settings.aiApiBaseUrl!!,
            apiKey = settings.aiApiKey,
            model = settings.aiModelName!!,
            systemPrompt = SYSTEM_PROMPT,
            userMessage = context
        )

        return when (result) {
            is AiCompletionResult.Success -> AiPrepResult.Success(parseSuggestions(result.content))
            is AiCompletionResult.Error -> AiPrepResult.Error(result.message)
        }
    }

    internal fun buildContext(userId: UserId, personId: PersonId, privacyMode: Boolean): String {
        val sections = mutableListOf<String>()

        // Last 2 entries (notes + outcomes)
        val recentEntries = entryRepository.findAllByUserIdAndPersonId(
            userId, personId,
            PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "meetingDate"))
        ).content

        if (recentEntries.isNotEmpty()) {
            val entrySections = recentEntries.mapNotNull { entry ->
                // Skip sensitive entries in privacy mode
                if (privacyMode && entry.sensitive) return@mapNotNull null

                val parts = mutableListOf<String>()
                if (!entry.notesMarkdown.isNullOrBlank()) {
                    parts.add("Notes: ${entry.notesMarkdown}")
                }
                if (!entry.outcomesMarkdown.isNullOrBlank()) {
                    parts.add("Outcomes: ${entry.outcomesMarkdown}")
                }
                if (parts.isEmpty()) null else "Meeting (${entry.meetingDate}):\n${parts.joinToString("\n")}"
            }
            if (entrySections.isNotEmpty()) {
                sections.add("## Recent 1:1 Notes\n${entrySections.joinToString("\n\n")}")
            }
        }

        // Open action items
        val openItems = actionItemRepository.findAllByUserIdAndPersonId(
            userId, personId, ActionItemStatus.OPEN,
            PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "dueDate"))
        ).content

        if (openItems.isNotEmpty()) {
            val itemTitles = openItems.joinToString("\n") { "- ${it.title}" }
            sections.add("## Open Action Items\n$itemTitles")
        }

        // Active PDP goals + recent updates
        val activeGoals = pdpGoalRepository.findAllByUserIdAndPersonId(
            userId, personId, PdpGoalStatus.ACTIVE,
            PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "updatedAt"))
        ).content

        if (activeGoals.isNotEmpty()) {
            val goalSections = activeGoals.map { goal ->
                val updates = pdpUpdateRepository.findAllByGoalIdAndUserId(
                    goal.id, userId,
                    PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "createdAt"))
                ).content
                    .filter { !privacyMode || !it.sensitive }
                    .joinToString("\n") { "  - ${it.textMarkdown}" }

                if (updates.isNotBlank()) {
                    "- ${goal.title}\n  Recent progress:\n$updates"
                } else {
                    "- ${goal.title}"
                }
            }
            sections.add("## Active PDP Goals\n${goalSections.joinToString("\n")}")
        }

        // Kudos since last 1:1
        val kudosPage = kudosRepository.findAllByUserIdAndPersonId(
            userId, personId,
            PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "date"))
        )
        val recentKudos = if (recentEntries.isNotEmpty()) {
            val lastMeetingDate = recentEntries.first().meetingDate
            kudosPage.content.filter { it.createdAt.isAfter(lastMeetingDate) }
        } else {
            kudosPage.content.take(5)
        }

        if (recentKudos.isNotEmpty()) {
            val kudosText = recentKudos.joinToString("\n") { "- ${it.text}" }
            sections.add("## Recent Kudos\n$kudosText")
        }

        return sections.joinToString("\n\n")
    }

    private fun parseSuggestions(content: String): List<String> {
        // Parse bullet points from LLM response
        return content.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { line ->
                // Remove common bullet prefixes
                line.removePrefix("- ")
                    .removePrefix("• ")
                    .removePrefix("* ")
                    .removePrefix("1. ")
                    .removePrefix("2. ")
                    .removePrefix("3. ")
                    .removePrefix("4. ")
                    .removePrefix("5. ")
                    .trim()
            }
            .filter { it.isNotBlank() }
            .take(5)
    }
}

sealed class AiPrepResult {
    data class Success(val suggestions: List<String>) : AiPrepResult()
    data class Error(val message: String) : AiPrepResult()
}
