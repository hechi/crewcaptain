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
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.UserId
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

@Service
@Transactional(readOnly = true)
class AiTrendRadarService(
    private val userSettingsRepository: UserSettingsRepository,
    private val personRepository: PersonRepository,
    private val entryRepository: OneOnOneEntryRepository,
    private val actionItemRepository: ActionItemRepository,
    private val pdpGoalRepository: PdpGoalRepository,
    private val pdpUpdateRepository: PdpUpdateRepository,
    private val kudosRepository: KudosRepository,
    private val aiClientPort: AiClientPort,
    private val objectMapper: ObjectMapper
) {

    companion object {
        private const val LOOKBACK_DAYS = 90L
        private const val MAX_PAGE_SIZE = 1000
    }

    fun generateInsights(userId: UserId, personId: PersonId): AiTrendRadarResult {
        // Verify person belongs to user
        val person = personRepository.findByIdAndUserId(personId, userId)
            ?: throw PersonNotFoundException(personId)

        // Load user settings to check AI configuration
        val settings = userSettingsRepository.findByUserId(userId)
            ?: return AiTrendRadarResult.Error("AI Assistant is not configured. Please configure it in Settings.")

        if (!settings.isAiConfigured()) {
            return AiTrendRadarResult.Error("AI Assistant is not enabled or not fully configured. Please check Settings.")
        }

        val privacyMode = settings.aiPrivacyMode
        val now = LocalDate.now()
        val lookbackStart = now.minusDays(LOOKBACK_DAYS)

        // Aggregate metadata
        val metadata = aggregateMetadata(userId, personId, lookbackStart, now, privacyMode)

        if (metadata.isInsufficient()) {
            return AiTrendRadarResult.InsufficientData(
                meetingsNeeded = maxOf(0, 3 - metadata.meetingCount),
                message = "Scanning horizon... Need ${maxOf(1, 3 - metadata.meetingCount)} more 1:1(s) to establish a baseline."
            )
        }

        // Build the user prompt with statistical summary
        val userPrompt = buildUserPrompt(person.name, metadata)

        // Call the LLM
        val result = aiClientPort.chatCompletion(
            baseUrl = settings.aiApiBaseUrl!!,
            apiKey = settings.aiApiKey,
            model = settings.aiModelName!!,
            systemPrompt = settings.effectiveTrendRadarPrompt(),
            userMessage = userPrompt
        )

        return when (result) {
            is AiCompletionResult.Success -> parseInsights(result.content, metadata)
            is AiCompletionResult.Error -> AiTrendRadarResult.Error(result.message)
        }
    }

    internal fun aggregateMetadata(
        userId: UserId,
        personId: PersonId,
        dateFrom: LocalDate,
        dateTo: LocalDate,
        privacyMode: Boolean
    ): TrendRadarMetadata {
        val dateFromInstant = dateFrom.atStartOfDay().toInstant(ZoneOffset.UTC)
        val dateToInstant = dateTo.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)

        // 1:1 Entries in range
        val entriesPage = entryRepository.findAllByUserIdAndPersonId(
            userId, personId,
            PageRequest.of(0, MAX_PAGE_SIZE, Sort.by(Sort.Direction.DESC, "meetingDate"))
        )
        val entriesInRange = entriesPage.content.filter { entry ->
            val d = entry.meetingDate
            !d.isBefore(dateFromInstant) && d.isBefore(dateToInstant)
        }

        val meetingCount = entriesInRange.size
        val firstMeetingDate = entriesInRange.minByOrNull { it.meetingDate }?.meetingDate
        val lastMeetingDate = entriesInRange.maxByOrNull { it.meetingDate }?.meetingDate
        val dataSpanDays = if (firstMeetingDate != null && lastMeetingDate != null) {
            ChronoUnit.DAYS.between(
                firstMeetingDate.atZone(ZoneOffset.UTC).toLocalDate(),
                lastMeetingDate.atZone(ZoneOffset.UTC).toLocalDate()
            ).toInt()
        } else 0

        // Outcomes (non-sensitive) for topic analysis
        val outcomes = if (!privacyMode) {
            entriesInRange
                .filter { !it.sensitive }
                .mapNotNull { it.outcomesMarkdown }
                .filter { it.isNotBlank() }
        } else {
            emptyList()
        }

        // Morale from person
        val person = personRepository.findByIdAndUserId(personId, userId)
        val currentMorale = person?.moraleStatus?.name ?: "UNKNOWN"

        // Action Items in range
        val actionItemsPage = actionItemRepository.findAllByUserIdAndPersonId(
            userId, personId, null,
            PageRequest.of(0, MAX_PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"))
        )
        val actionItemsInRange = actionItemsPage.content.filter { item ->
            val d = item.createdAt
            !d.isBefore(dateFromInstant) && d.isBefore(dateToInstant)
        }
        val actionItemsCreated = actionItemsInRange.size
        val actionItemsClosed = actionItemsInRange.count { it.status == ActionItemStatus.DONE }
        val actionItemsCanceled = actionItemsInRange.count { it.status == ActionItemStatus.CANCELED }

        // PDP Goals and updates
        val goalsPage = pdpGoalRepository.findAllByUserIdAndPersonId(
            userId, personId, null,
            PageRequest.of(0, MAX_PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"))
        )
        val activeGoals = goalsPage.content.count { it.status.name == "ACTIVE" }

        // PDP Updates in range
        var pdpUpdateCount = 0
        for (goal in goalsPage.content) {
            val updates = pdpUpdateRepository.findAllByGoalIdAndUserId(
                goal.id, userId,
                PageRequest.of(0, MAX_PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"))
            ).content
            pdpUpdateCount += updates.count { update ->
                val d = update.createdAt
                !d.isBefore(dateFromInstant) && d.isBefore(dateToInstant)
            }
        }

        // Kudos in range
        val kudosPage = kudosRepository.findAllByUserIdAndPersonId(
            userId, personId,
            PageRequest.of(0, MAX_PAGE_SIZE, Sort.by(Sort.Direction.DESC, "date"))
        )
        val kudosInRange = kudosPage.content.filter { kudos ->
            val d = kudos.date
            !d.isBefore(dateFrom) && !d.isAfter(dateTo)
        }
        val kudosCount = kudosInRange.size
        val kudosTagDistribution = kudosInRange
            .flatMap { it.tags }
            .groupingBy { it }
            .eachCount()

        return TrendRadarMetadata(
            meetingCount = meetingCount,
            dataSpanDays = dataSpanDays,
            currentMorale = currentMorale,
            outcomes = outcomes,
            actionItemsCreated = actionItemsCreated,
            actionItemsClosed = actionItemsClosed,
            actionItemsCanceled = actionItemsCanceled,
            activeGoals = activeGoals,
            pdpUpdateCount = pdpUpdateCount,
            kudosCount = kudosCount,
            kudosTagDistribution = kudosTagDistribution
        )
    }

    internal fun buildUserPrompt(personName: String, metadata: TrendRadarMetadata): String {
        val sections = mutableListOf<String>()

        sections.add("## Person: $personName")
        sections.add("## Data Window: Last 90 days")
        sections.add("## Current Morale: ${metadata.currentMorale}")

        sections.add("""
            |## Meeting Activity
            |- Total 1:1 meetings: ${metadata.meetingCount}
            |- Data span: ${metadata.dataSpanDays} days
        """.trimMargin())

        sections.add("""
            |## Action Items (90 days)
            |- Created: ${metadata.actionItemsCreated}
            |- Completed: ${metadata.actionItemsClosed}
            |- Canceled: ${metadata.actionItemsCanceled}
            |- Completion rate: ${if (metadata.actionItemsCreated > 0) "${(metadata.actionItemsClosed * 100) / metadata.actionItemsCreated}%" else "N/A"}
        """.trimMargin())

        sections.add("""
            |## Growth & Development
            |- Active PDP goals: ${metadata.activeGoals}
            |- PDP updates recorded: ${metadata.pdpUpdateCount}
        """.trimMargin())

        sections.add("""
            |## Recognition
            |- Kudos recorded: ${metadata.kudosCount}
            |- Tag distribution: ${if (metadata.kudosTagDistribution.isNotEmpty()) metadata.kudosTagDistribution.entries.joinToString(", ") { "${it.key}: ${it.value}" } else "None"}
        """.trimMargin())

        if (metadata.outcomes.isNotEmpty()) {
            val topicSummary = metadata.outcomes.joinToString("\n") { "- $it" }
            sections.add("## 1:1 Outcome Summaries\n$topicSummary")
        }

        return sections.joinToString("\n\n")
    }

    internal fun parseInsights(content: String, metadata: TrendRadarMetadata): AiTrendRadarResult {
        return try {
            // Try to parse JSON response
            val cleanContent = content
                .replace(Regex("^```json\\s*", RegexOption.MULTILINE), "")
                .replace(Regex("^```\\s*$", RegexOption.MULTILINE), "")
                .trim()

            val response = objectMapper.readValue(cleanContent, TrendRadarAiResponse::class.java)

            if (response.insights.isNullOrEmpty()) {
                return AiTrendRadarResult.Error("AI returned no insights. Please try again.")
            }

            val insights = response.insights.map { insight ->
                TrendRadarInsight(
                    title = insight.title ?: "Untitled Insight",
                    description = insight.description ?: "",
                    dimension = parseDimension(insight.dimension),
                    confidenceScore = (insight.confidence_score ?: computeBaseConfidence(metadata)).coerceIn(0, 100)
                )
            }

            AiTrendRadarResult.Success(insights)
        } catch (e: Exception) {
            // If JSON parsing fails, try to extract useful content
            AiTrendRadarResult.Error("Failed to parse AI response. Please try again.")
        }
    }

    private fun parseDimension(dimension: String?): TrendDimension {
        return when (dimension?.uppercase()) {
            "MORALE" -> TrendDimension.MORALE
            "WORK_GROWTH_BALANCE" -> TrendDimension.WORK_GROWTH_BALANCE
            "RECOGNITION" -> TrendDimension.RECOGNITION
            "MEETING_EFFICACY" -> TrendDimension.MEETING_EFFICACY
            else -> TrendDimension.MORALE
        }
    }

    internal fun computeBaseConfidence(metadata: TrendRadarMetadata): Int {
        // Confidence based on data volume
        val meetingScore = when {
            metadata.meetingCount >= 8 -> 40
            metadata.meetingCount >= 4 -> 25
            else -> 10
        }
        val spanScore = when {
            metadata.dataSpanDays >= 60 -> 40
            metadata.dataSpanDays >= 30 -> 25
            else -> 10
        }
        val dataRichness = when {
            metadata.actionItemsCreated > 0 && metadata.kudosCount > 0 && metadata.pdpUpdateCount > 0 -> 20
            metadata.actionItemsCreated > 0 || metadata.kudosCount > 0 -> 10
            else -> 0
        }
        return (meetingScore + spanScore + dataRichness).coerceIn(0, 100)
    }
}

// --- Result types ---

sealed class AiTrendRadarResult {
    data class Success(val insights: List<TrendRadarInsight>) : AiTrendRadarResult()
    data class Error(val message: String) : AiTrendRadarResult()
    data class InsufficientData(val meetingsNeeded: Int, val message: String) : AiTrendRadarResult()
}

data class TrendRadarInsight(
    val title: String,
    val description: String,
    val dimension: TrendDimension,
    val confidenceScore: Int
)

enum class TrendDimension {
    MORALE,
    WORK_GROWTH_BALANCE,
    RECOGNITION,
    MEETING_EFFICACY
}

// --- Internal data classes ---

data class TrendRadarMetadata(
    val meetingCount: Int,
    val dataSpanDays: Int,
    val currentMorale: String,
    val outcomes: List<String>,
    val actionItemsCreated: Int,
    val actionItemsClosed: Int,
    val actionItemsCanceled: Int,
    val activeGoals: Int,
    val pdpUpdateCount: Int,
    val kudosCount: Int,
    val kudosTagDistribution: Map<String, Int>
) {
    fun isInsufficient(): Boolean = meetingCount < 2
}

// --- AI Response parsing ---

@JsonIgnoreProperties(ignoreUnknown = true)
data class TrendRadarAiResponse(
    val insights: List<TrendRadarAiInsight>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TrendRadarAiInsight(
    val title: String? = null,
    val description: String? = null,
    val dimension: String? = null,
    val confidence_score: Int? = null
)
