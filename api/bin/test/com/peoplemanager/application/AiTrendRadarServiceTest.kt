package com.peoplemanager.application

import com.peoplemanager.application.ports.*
import com.peoplemanager.domain.*
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

class AiTrendRadarServiceTest {

    private val userSettingsRepository = mockk<UserSettingsRepository>()
    private val personRepository = mockk<PersonRepository>()
    private val entryRepository = mockk<OneOnOneEntryRepository>()
    private val actionItemRepository = mockk<ActionItemRepository>()
    private val pdpGoalRepository = mockk<PdpGoalRepository>()
    private val pdpUpdateRepository = mockk<PdpUpdateRepository>()
    private val kudosRepository = mockk<KudosRepository>()
    private val aiClientPort = mockk<AiClientPort>()
    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    private lateinit var service: AiTrendRadarService

    private val userId = UserId(UUID.randomUUID())
    private val personId = PersonId(UUID.randomUUID())
    private val person = Person(
        id = personId,
        userId = userId,
        name = "John Doe",
        moraleStatus = MoraleStatus.GREEN
    )

    private val configuredSettings = UserSettings(
        userId = userId,
        aiEnabled = true,
        aiApiBaseUrl = "http://localhost:11434/v1",
        aiModelName = "llama3"
    )

    @BeforeEach
    fun setUp() {
        service = AiTrendRadarService(
            userSettingsRepository,
            personRepository,
            entryRepository,
            actionItemRepository,
            pdpGoalRepository,
            pdpUpdateRepository,
            kudosRepository,
            aiClientPort,
            objectMapper
        )
    }

    @Test
    fun `should return error when person not found`() {
        every { personRepository.findByIdAndUserId(personId, userId) } returns null

        val exception = assertThrows(PersonNotFoundException::class.java) {
            service.generateInsights(userId, personId)
        }
        assertEquals(personId, exception.personId)
    }

    @Test
    fun `should return error when AI not configured`() {
        every { personRepository.findByIdAndUserId(personId, userId) } returns person
        every { userSettingsRepository.findByUserId(userId) } returns null

        val result = service.generateInsights(userId, personId)

        assertTrue(result is AiTrendRadarResult.Error)
        assertTrue((result as AiTrendRadarResult.Error).message.contains("not configured"))
    }

    @Test
    fun `should return error when AI not enabled`() {
        every { personRepository.findByIdAndUserId(personId, userId) } returns person
        every { userSettingsRepository.findByUserId(userId) } returns UserSettings(userId = userId, aiEnabled = false)

        val result = service.generateInsights(userId, personId)

        assertTrue(result is AiTrendRadarResult.Error)
        assertTrue((result as AiTrendRadarResult.Error).message.contains("not enabled"))
    }

    @Test
    fun `should return insufficient data when fewer than 2 meetings`() {
        every { personRepository.findByIdAndUserId(personId, userId) } returns person
        every { userSettingsRepository.findByUserId(userId) } returns configuredSettings
        every { entryRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(emptyList())
        every { actionItemRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns PageImpl(emptyList())
        every { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns PageImpl(emptyList())
        every { kudosRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(emptyList())

        val result = service.generateInsights(userId, personId)

        assertTrue(result is AiTrendRadarResult.InsufficientData)
        val insufficientResult = result as AiTrendRadarResult.InsufficientData
        assertTrue(insufficientResult.message.contains("Scanning horizon"))
    }

    @Test
    fun `should return insufficient data when only 1 meeting exists`() {
        val entry = createEntry(Instant.now().minusSeconds(86400))
        every { personRepository.findByIdAndUserId(personId, userId) } returns person
        every { userSettingsRepository.findByUserId(userId) } returns configuredSettings
        every { entryRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(listOf(entry))
        every { actionItemRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns PageImpl(emptyList())
        every { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns PageImpl(emptyList())
        every { kudosRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(emptyList())

        val result = service.generateInsights(userId, personId)

        assertTrue(result is AiTrendRadarResult.InsufficientData)
    }

    @Test
    fun `should call AI and return insights on success`() {
        val entries = listOf(
            createEntry(Instant.now().minusSeconds(86400 * 7)),
            createEntry(Instant.now().minusSeconds(86400 * 14)),
            createEntry(Instant.now().minusSeconds(86400 * 21))
        )
        val actionItems = listOf(
            createActionItem(ActionItemStatus.DONE),
            createActionItem(ActionItemStatus.OPEN),
            createActionItem(ActionItemStatus.DONE)
        )

        every { personRepository.findByIdAndUserId(personId, userId) } returns person
        every { userSettingsRepository.findByUserId(userId) } returns configuredSettings
        every { entryRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(entries)
        every { actionItemRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns PageImpl(actionItems)
        every { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns PageImpl(emptyList())
        every { kudosRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(emptyList())

        val aiResponse = """
            {"insights": [
                {"title": "High Output", "description": "Strong task completion rate.", "dimension": "WORK_GROWTH_BALANCE", "confidence_score": 65},
                {"title": "Recognition Gap", "description": "No kudos recorded despite output.", "dimension": "RECOGNITION", "confidence_score": 50},
                {"title": "Consistent Engagement", "description": "Regular meeting cadence.", "dimension": "MEETING_EFFICACY", "confidence_score": 70}
            ]}
        """.trimIndent()

        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns AiCompletionResult.Success(aiResponse)

        val result = service.generateInsights(userId, personId)

        assertTrue(result is AiTrendRadarResult.Success)
        val success = result as AiTrendRadarResult.Success
        assertEquals(3, success.insights.size)
        assertEquals("High Output", success.insights[0].title)
        assertEquals(TrendDimension.WORK_GROWTH_BALANCE, success.insights[0].dimension)
        assertEquals(65, success.insights[0].confidenceScore)
    }

    @Test
    fun `should return error when AI returns error`() {
        val entries = listOf(
            createEntry(Instant.now().minusSeconds(86400 * 7)),
            createEntry(Instant.now().minusSeconds(86400 * 14))
        )

        every { personRepository.findByIdAndUserId(personId, userId) } returns person
        every { userSettingsRepository.findByUserId(userId) } returns configuredSettings
        every { entryRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(entries)
        every { actionItemRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns PageImpl(emptyList())
        every { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns PageImpl(emptyList())
        every { kudosRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(emptyList())
        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns AiCompletionResult.Error("Connection refused")

        val result = service.generateInsights(userId, personId)

        assertTrue(result is AiTrendRadarResult.Error)
        assertEquals("Connection refused", (result as AiTrendRadarResult.Error).message)
    }

    @Test
    fun `should respect privacy mode and exclude outcomes`() {
        val entries = listOf(
            createEntry(Instant.now().minusSeconds(86400 * 7), sensitive = false, outcomes = "Discussed project"),
            createEntry(Instant.now().minusSeconds(86400 * 14), sensitive = true, outcomes = "Private matter")
        )

        val privacySettings = configuredSettings.copy(aiPrivacyMode = true)

        every { personRepository.findByIdAndUserId(personId, userId) } returns person
        every { userSettingsRepository.findByUserId(userId) } returns privacySettings
        every { entryRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(entries)
        every { actionItemRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns PageImpl(emptyList())
        every { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns PageImpl(emptyList())
        every { kudosRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(emptyList())

        val aiResponse = """{"insights": [{"title": "Test", "description": "Test desc", "dimension": "MORALE", "confidence_score": 40}]}"""
        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns AiCompletionResult.Success(aiResponse)

        service.generateInsights(userId, personId)

        // Verify the user message sent to AI does NOT contain outcomes (privacy mode)
        verify {
            aiClientPort.chatCompletion(any(), any(), any(), any(), match { userMessage ->
                !userMessage.contains("Discussed project") && !userMessage.contains("Private matter")
            })
        }
    }

    @Test
    fun `should include outcomes when privacy mode is off`() {
        val entries = listOf(
            createEntry(Instant.now().minusSeconds(86400 * 7), sensitive = false, outcomes = "Discussed project timeline"),
            createEntry(Instant.now().minusSeconds(86400 * 14), sensitive = false, outcomes = "Reviewed goals")
        )

        val noPrivacySettings = configuredSettings.copy(aiPrivacyMode = false)

        every { personRepository.findByIdAndUserId(personId, userId) } returns person
        every { userSettingsRepository.findByUserId(userId) } returns noPrivacySettings
        every { entryRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(entries)
        every { actionItemRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns PageImpl(emptyList())
        every { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns PageImpl(emptyList())
        every { kudosRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(emptyList())

        val aiResponse = """{"insights": [{"title": "Test", "description": "Test desc", "dimension": "MORALE", "confidence_score": 40}]}"""
        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns AiCompletionResult.Success(aiResponse)

        service.generateInsights(userId, personId)

        verify {
            aiClientPort.chatCompletion(any(), any(), any(), any(), match { userMessage ->
                userMessage.contains("Discussed project timeline") && userMessage.contains("Reviewed goals")
            })
        }
    }

    @Test
    fun `should use custom trend radar prompt from settings`() {
        val entries = listOf(
            createEntry(Instant.now().minusSeconds(86400 * 7)),
            createEntry(Instant.now().minusSeconds(86400 * 14))
        )
        val customPromptSettings = configuredSettings.copy(trendRadarPrompt = "Custom radar prompt")

        every { personRepository.findByIdAndUserId(personId, userId) } returns person
        every { userSettingsRepository.findByUserId(userId) } returns customPromptSettings
        every { entryRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(entries)
        every { actionItemRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns PageImpl(emptyList())
        every { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns PageImpl(emptyList())
        every { kudosRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(emptyList())

        val aiResponse = """{"insights": [{"title": "Test", "description": "Test desc", "dimension": "MORALE", "confidence_score": 40}]}"""
        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns AiCompletionResult.Success(aiResponse)

        service.generateInsights(userId, personId)

        verify {
            aiClientPort.chatCompletion(any(), any(), any(), eq("Custom radar prompt"), any())
        }
    }

    @Test
    fun `should handle malformed AI JSON response gracefully`() {
        val entries = listOf(
            createEntry(Instant.now().minusSeconds(86400 * 7)),
            createEntry(Instant.now().minusSeconds(86400 * 14))
        )

        every { personRepository.findByIdAndUserId(personId, userId) } returns person
        every { userSettingsRepository.findByUserId(userId) } returns configuredSettings
        every { entryRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(entries)
        every { actionItemRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns PageImpl(emptyList())
        every { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns PageImpl(emptyList())
        every { kudosRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(emptyList())
        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns AiCompletionResult.Success("This is not JSON at all")

        val result = service.generateInsights(userId, personId)

        assertTrue(result is AiTrendRadarResult.Error)
        assertTrue((result as AiTrendRadarResult.Error).message.contains("parse"))
    }

    @Test
    fun `should strip markdown code fences from AI response`() {
        val entries = listOf(
            createEntry(Instant.now().minusSeconds(86400 * 7)),
            createEntry(Instant.now().minusSeconds(86400 * 14))
        )

        every { personRepository.findByIdAndUserId(personId, userId) } returns person
        every { userSettingsRepository.findByUserId(userId) } returns configuredSettings
        every { entryRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(entries)
        every { actionItemRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns PageImpl(emptyList())
        every { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns PageImpl(emptyList())
        every { kudosRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(emptyList())

        val aiResponse = """```json
{"insights": [{"title": "Wrapped", "description": "In code fences", "dimension": "MORALE", "confidence_score": 55}]}
```"""
        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns AiCompletionResult.Success(aiResponse)

        val result = service.generateInsights(userId, personId)

        assertTrue(result is AiTrendRadarResult.Success)
        assertEquals("Wrapped", (result as AiTrendRadarResult.Success).insights[0].title)
    }

    @Test
    fun `should clamp confidence score to 0-100 range`() {
        val entries = listOf(
            createEntry(Instant.now().minusSeconds(86400 * 7)),
            createEntry(Instant.now().minusSeconds(86400 * 14))
        )

        every { personRepository.findByIdAndUserId(personId, userId) } returns person
        every { userSettingsRepository.findByUserId(userId) } returns configuredSettings
        every { entryRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(entries)
        every { actionItemRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns PageImpl(emptyList())
        every { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns PageImpl(emptyList())
        every { kudosRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(emptyList())

        val aiResponse = """{"insights": [{"title": "Over", "description": "Too high", "dimension": "MORALE", "confidence_score": 150}]}"""
        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns AiCompletionResult.Success(aiResponse)

        val result = service.generateInsights(userId, personId)

        assertTrue(result is AiTrendRadarResult.Success)
        assertEquals(100, (result as AiTrendRadarResult.Success).insights[0].confidenceScore)
    }

    @Test
    fun `should aggregate kudos tag distribution in metadata`() {
        val now = LocalDate.now()
        val lookbackStart = now.minusDays(90)

        val kudosList = listOf(
            createKudos(now.minusDays(5), listOf("Leadership", "Support")),
            createKudos(now.minusDays(10), listOf("Leadership")),
            createKudos(now.minusDays(20), listOf("Technical", "Support"))
        )

        every { personRepository.findByIdAndUserId(personId, userId) } returns person
        every { entryRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(emptyList())
        every { actionItemRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns PageImpl(emptyList())
        every { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, null, any()) } returns PageImpl(emptyList())
        every { pdpUpdateRepository.findAllByGoalIdAndUserId(any(), userId, any()) } returns PageImpl(emptyList())
        every { kudosRepository.findAllByUserIdAndPersonId(userId, personId, any()) } returns PageImpl(kudosList)

        val metadata = service.aggregateMetadata(userId, personId, lookbackStart, now, false)

        assertEquals(3, metadata.kudosCount)
        assertEquals(2, metadata.kudosTagDistribution["Leadership"])
        assertEquals(2, metadata.kudosTagDistribution["Support"])
        assertEquals(1, metadata.kudosTagDistribution["Technical"])
    }

    @Test
    fun `computeBaseConfidence should return high score for rich data`() {
        val metadata = TrendRadarMetadata(
            meetingCount = 10,
            dataSpanDays = 75,
            currentMorale = "GREEN",
            outcomes = emptyList(),
            actionItemsCreated = 5,
            actionItemsClosed = 3,
            actionItemsCanceled = 0,
            activeGoals = 2,
            pdpUpdateCount = 4,
            kudosCount = 3,
            kudosTagDistribution = mapOf("Leadership" to 2, "Technical" to 1)
        )

        val confidence = service.computeBaseConfidence(metadata)
        assertEquals(100, confidence) // 40 (meetings) + 40 (span) + 20 (richness)
    }

    @Test
    fun `computeBaseConfidence should return low score for thin data`() {
        val metadata = TrendRadarMetadata(
            meetingCount = 2,
            dataSpanDays = 10,
            currentMorale = "UNKNOWN",
            outcomes = emptyList(),
            actionItemsCreated = 0,
            actionItemsClosed = 0,
            actionItemsCanceled = 0,
            activeGoals = 0,
            pdpUpdateCount = 0,
            kudosCount = 0,
            kudosTagDistribution = emptyMap()
        )

        val confidence = service.computeBaseConfidence(metadata)
        assertEquals(20, confidence) // 10 (meetings) + 10 (span) + 0 (richness)
    }

    @Test
    fun `metadata isInsufficient should return true when fewer than 2 meetings`() {
        val metadata = TrendRadarMetadata(
            meetingCount = 1,
            dataSpanDays = 5,
            currentMorale = "GREEN",
            outcomes = emptyList(),
            actionItemsCreated = 0,
            actionItemsClosed = 0,
            actionItemsCanceled = 0,
            activeGoals = 0,
            pdpUpdateCount = 0,
            kudosCount = 0,
            kudosTagDistribution = emptyMap()
        )

        assertTrue(metadata.isInsufficient())
    }

    @Test
    fun `metadata isInsufficient should return false when 2 or more meetings`() {
        val metadata = TrendRadarMetadata(
            meetingCount = 2,
            dataSpanDays = 14,
            currentMorale = "GREEN",
            outcomes = emptyList(),
            actionItemsCreated = 0,
            actionItemsClosed = 0,
            actionItemsCanceled = 0,
            activeGoals = 0,
            pdpUpdateCount = 0,
            kudosCount = 0,
            kudosTagDistribution = emptyMap()
        )

        assertFalse(metadata.isInsufficient())
    }

    // --- Helper methods ---

    private fun createEntry(
        meetingDate: Instant = Instant.now(),
        sensitive: Boolean = false,
        outcomes: String? = null
    ) = com.peoplemanager.domain.OneOnOneEntry(
        id = com.peoplemanager.domain.OneOnOneEntryId(UUID.randomUUID()),
        userId = userId,
        personId = personId,
        meetingDate = meetingDate,
        sensitive = sensitive,
        outcomesMarkdown = outcomes,
        notesMarkdown = null,
        createdAt = meetingDate,
        updatedAt = meetingDate
    )

    private fun createActionItem(status: ActionItemStatus) = com.peoplemanager.domain.ActionItem(
        id = com.peoplemanager.domain.ActionItemId(UUID.randomUUID()),
        userId = userId,
        personId = personId,
        title = "Test action item",
        status = status,
        ownerType = com.peoplemanager.domain.ActionItemOwnerType.MANAGER,
        createdAt = Instant.now().minusSeconds(86400),
        updatedAt = Instant.now()
    )

    private fun createKudos(date: LocalDate, tags: List<String>) = com.peoplemanager.domain.Kudos(
        id = com.peoplemanager.domain.KudosId(UUID.randomUUID()),
        userId = userId,
        personId = personId,
        date = date,
        text = "Great work",
        tags = tags,
        createdAt = date.atStartOfDay().toInstant(ZoneOffset.UTC),
        updatedAt = date.atStartOfDay().toInstant(ZoneOffset.UTC)
    )
}
