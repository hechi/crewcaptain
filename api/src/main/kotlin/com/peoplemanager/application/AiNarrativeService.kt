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
import com.peoplemanager.domain.AiWritingStyle
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.ReviewPacketSummary
import com.peoplemanager.domain.UserId
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneOffset

@Service
@Transactional(readOnly = true)
class AiNarrativeService(
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
        const val SYSTEM_PROMPT = "You are an expert Leadership Coach and People Manager. " +
            "Your goal is to draft a professional, objective, and supportive performance review narrative " +
            "based on raw meeting notes, kudos, and goal progress. " +
            "Maintain a professional tone: concise, impactful, and forward-looking. " +
            "Do NOT include any preamble, introduction, or meta-commentary. " +
            "Output ONLY the narrative content."

        private const val MAX_PAGE_SIZE = 1000
    }

    fun generateNarrative(
        userId: UserId,
        personId: PersonId,
        dateFrom: LocalDate,
        dateTo: LocalDate
    ): AiNarrativeResult {
        // Verify person belongs to user
        val person = personRepository.findByIdAndUserId(personId, userId)
            ?: throw PersonNotFoundException(personId)

        // Load user settings to check AI configuration
        val settings = userSettingsRepository.findByUserId(userId)
            ?: return AiNarrativeResult.Error("AI Assistant is not configured. Please configure it in Settings.")

        if (!settings.isAiConfigured()) {
            return AiNarrativeResult.Error("AI Assistant is not enabled or not fully configured. Please check Settings.")
        }

        val privacyMode = settings.aiPrivacyMode
        val writingStyle = settings.aiWritingStyle

        // Gather context within date range
        val context = buildNarrativeContext(userId, personId, dateFrom, dateTo, privacyMode)

        if (context.isBlank()) {
            return AiNarrativeResult.Error(
                "No data available for the selected period. Add some 1:1 notes, action items, goals, or kudos first."
            )
        }

        // Build the user prompt
        val userPrompt = buildUserPrompt(person.name, dateFrom, dateTo, context, writingStyle)

        // Call the LLM
        val result = aiClientPort.chatCompletion(
            baseUrl = settings.aiApiBaseUrl!!,
            apiKey = settings.aiApiKey,
            model = settings.aiModelName!!,
            systemPrompt = settings.effectiveNarrativePrompt(),
            userMessage = userPrompt
        )

        return when (result) {
            is AiCompletionResult.Success -> AiNarrativeResult.Success(result.content)
            is AiCompletionResult.Error -> AiNarrativeResult.Error(result.message)
        }
    }

    internal fun buildNarrativeContext(
        userId: UserId,
        personId: PersonId,
        dateFrom: LocalDate,
        dateTo: LocalDate,
        privacyMode: Boolean
    ): String {
        val sections = mutableListOf<String>()

        // Kudos in date range
        val kudosPage = kudosRepository.findAllByUserIdAndPersonId(
            userId, personId,
            PageRequest.of(0, MAX_PAGE_SIZE, Sort.by(Sort.Direction.DESC, "date"))
        )
        val kudosInRange = kudosPage.content.filter { kudos ->
            val d = kudos.date
            !d.isBefore(dateFrom) && !d.isAfter(dateTo)
        }
        if (kudosInRange.isNotEmpty()) {
            val kudosText = kudosInRange.joinToString("\n") { kudos ->
                val tagsStr = if (kudos.tags.isNotEmpty()) " [${kudos.tags.joinToString(", ")}]" else ""
                "- ${kudos.text}$tagsStr"
            }
            sections.add("## Kudos Received\n$kudosText")
        }

        // PDP Goals with updates in date range
        val goalsPage = pdpGoalRepository.findAllByUserIdAndPersonId(
            userId, personId, null,
            PageRequest.of(0, MAX_PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"))
        )
        val goalsInRange = goalsPage.content.filter { goal ->
            val d = goal.createdAt.atZone(ZoneOffset.UTC).toLocalDate()
            !d.isBefore(dateFrom) && !d.isAfter(dateTo)
        }
        if (goalsInRange.isNotEmpty()) {
            val goalSections = goalsInRange.map { goal ->
                val updates = pdpUpdateRepository.findAllByGoalIdAndUserId(
                    goal.id, userId,
                    PageRequest.of(0, MAX_PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"))
                ).content
                    .filter { update ->
                        val ud = update.createdAt.atZone(ZoneOffset.UTC).toLocalDate()
                        !ud.isBefore(dateFrom) && !ud.isAfter(dateTo)
                    }
                    .filter { !privacyMode || !it.sensitive }

                val updatesText = if (updates.isNotEmpty()) {
                    "\n  Progress:\n" + updates.joinToString("\n") { "  - ${it.textMarkdown}" }
                } else ""

                "- ${goal.title} [${goal.status.name}]$updatesText"
            }
            sections.add("## PDP Goals\n${goalSections.joinToString("\n")}")
        }

        // 1:1 Outcomes in date range
        val entriesPage = entryRepository.findAllByUserIdAndPersonId(
            userId, personId,
            PageRequest.of(0, MAX_PAGE_SIZE, Sort.by(Sort.Direction.DESC, "meetingDate"))
        )
        val entriesInRange = entriesPage.content.filter { entry ->
            val d = entry.meetingDate.atZone(ZoneOffset.UTC).toLocalDate()
            !d.isBefore(dateFrom) && !d.isAfter(dateTo)
        }
        val outcomeEntries = entriesInRange
            .filter { !privacyMode || !it.sensitive }
            .filter { !it.outcomesMarkdown.isNullOrBlank() }
        if (outcomeEntries.isNotEmpty()) {
            val outcomesText = outcomeEntries.joinToString("\n") { entry ->
                val dateStr = entry.meetingDate.atZone(ZoneOffset.UTC).toLocalDate().toString()
                "- ($dateStr) ${entry.outcomesMarkdown}"
            }
            sections.add("## Key 1:1 Outcomes\n$outcomesText")
        }

        // Action Items statistics and completed titles
        val actionItemsPage = actionItemRepository.findAllByUserIdAndPersonId(
            userId, personId, null,
            PageRequest.of(0, MAX_PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"))
        )
        val actionItemsInRange = actionItemsPage.content.filter { item ->
            val d = item.createdAt.atZone(ZoneOffset.UTC).toLocalDate()
            !d.isBefore(dateFrom) && !d.isAfter(dateTo)
        }
        if (actionItemsInRange.isNotEmpty()) {
            val completed = actionItemsInRange.filter { it.status == ActionItemStatus.DONE }
            val total = actionItemsInRange.size
            val completedTitles = completed.take(10).joinToString("\n") { "- ${it.title}" }
            val statsLine = "$total total action items, ${completed.size} completed"
            val section = if (completedTitles.isNotBlank()) {
                "## Action Items\n$statsLine\n\nSignificant completed tasks:\n$completedTitles"
            } else {
                "## Action Items\n$statsLine"
            }
            sections.add(section)
        }

        return sections.joinToString("\n\n")
    }

    internal fun buildUserPrompt(
        personName: String,
        dateFrom: LocalDate,
        dateTo: LocalDate,
        context: String,
        writingStyle: AiWritingStyle
    ): String {
        val styleInstruction = when (writingStyle) {
            AiWritingStyle.NARRATIVE -> """
                |Write a 3-paragraph performance narrative:
                |Paragraph 1: Highlight core achievements and impact based on Kudos and completed action items.
                |Paragraph 2: Summarize growth and development based on PDP goals and 1:1 outcomes.
                |Paragraph 3: Provide a forward-looking statement on areas for next-level growth.
            """.trimMargin()
            AiWritingStyle.BULLET_POINTS -> """
                |Write the performance review as structured bullet points:
                |- Section 1: Key Achievements & Impact (3-5 bullets)
                |- Section 2: Growth & Development (3-5 bullets)
                |- Section 3: Forward-Looking Recommendations (2-3 bullets)
            """.trimMargin()
            AiWritingStyle.CONCISE -> """
                |Write a concise 1-paragraph performance summary (4-6 sentences) covering achievements, growth, and next steps.
            """.trimMargin()
        }

        return """
            |Please generate a performance narrative for $personName for the period $dateFrom to $dateTo.
            |
            |**Data Provided:**
            |$context
            |
            |**Requirements:**
            |$styleInstruction
            |Tone: Professional & Objective
        """.trimMargin()
    }
}

sealed class AiNarrativeResult {
    data class Success(val narrative: String) : AiNarrativeResult()
    data class Error(val message: String) : AiNarrativeResult()
}
