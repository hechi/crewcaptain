package com.peoplemanager.application

import com.peoplemanager.application.port.output.AiClientPort
import com.peoplemanager.application.port.output.AiCompletionResult
import com.peoplemanager.application.port.output.ActionItemRepository
import com.peoplemanager.application.port.output.KudosRepository
import com.peoplemanager.application.port.output.OneOnOneEntryRepository
import com.peoplemanager.application.port.output.PdpGoalRepository
import com.peoplemanager.application.port.output.PdpUpdateRepository
import com.peoplemanager.application.port.output.PersonRepository
import com.peoplemanager.application.port.output.UserSettingsRepository
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
        const val SYSTEM_PROMPT = "You are a leadership coach. Based on the provided context, " +
            "suggest 3-5 high-impact agenda items for the next 1:1 meeting. " +
            "RULES: " +
            "- Output ONLY the agenda items, one per line. " +
            "- Do NOT include any introduction, preamble, explanation, or closing text. " +
            "- Do NOT use markdown formatting (no bold, italic, headers, or links). " +
            "- Start each line with a dash followed by a space. " +
            "- Keep each item short and actionable (one sentence). " +
            "- Focus on follow-ups, blockers, growth opportunities, and recognition."
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
            systemPrompt = settings.effectiveAgendaPrepPrompt(),
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
        return content.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { line ->
                // Remove common bullet/numbered prefixes
                line.removePrefix("- ")
                    .removePrefix("• ")
                    .removePrefix("* ")
                    .replace(Regex("^\\d+[\\.\\)\\:]\\s*"), "") // "1. ", "1) ", "1: "
                    .trim()
            }
            .filter { it.isNotBlank() }
            // Filter out preamble/closing lines (not actual suggestions)
            .filter { line ->
                val lower = line.lowercase()
                !lower.startsWith("here are") &&
                !lower.startsWith("here's") &&
                !lower.startsWith("based on") &&
                !lower.startsWith("optional") &&
                !lower.startsWith("additional") &&
                !lower.startsWith("note:") &&
                !lower.startsWith("these ") &&
                !lower.startsWith("i suggest") &&
                !lower.startsWith("i recommend") &&
                !lower.startsWith("sure") &&
                !lower.startsWith("certainly") &&
                !lower.endsWith(":") &&
                !lower.contains("agenda items") &&
                !lower.contains("suggestions:")
            }
            // Strip markdown formatting
            .map { line ->
                line.replace(Regex("\\*\\*(.+?)\\*\\*"), "$1") // **bold**
                    .replace(Regex("__(.+?)__"), "$1")          // __bold__
                    .replace(Regex("\\*(.+?)\\*"), "$1")        // *italic*
                    .replace(Regex("_(.+?)_"), "$1")            // _italic_
                    .replace(Regex("`(.+?)`"), "$1")            // `code`
                    .replace(Regex("\\[(.+?)\\]\\(.+?\\)"), "$1") // [text](url)
                    .replace(Regex("^#+\\s*"), "")              // ## headers
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
