package com.peoplemanager.application

import com.peoplemanager.application.port.input.*
import com.peoplemanager.application.port.output.*
import com.peoplemanager.application.StrategyGoalLinkService.AlignmentScore
import com.peoplemanager.application.StrategyGoalLinkService.LinkedPdpGoalInfo
import com.peoplemanager.domain.*
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.util.UUID

class AiLinkDiscoveryServiceTest {

    private val strategyGoalRepository = mockk<StrategyGoalRepository>()
    private val pdpGoalRepository = mockk<PdpGoalRepository>()
    private val personRepository = mockk<PersonRepository>()
    private val linkService = mockk<StrategyGoalLinkService>(relaxed = true)
    private val userSettingsRepository = mockk<UserSettingsRepository>()
    private val aiClientPort = mockk<AiClientPort>()
    private val objectMapper = jacksonObjectMapper()
    private val aiConfigResolver = AiConfigResolver(defaultBaseUrl = "", defaultApiKey = "", defaultModel = "")

    private lateinit var service: AiLinkDiscoveryService

    private val userId = UserId(UUID.randomUUID())
    private val personId = PersonId(UUID.randomUUID())

    @BeforeEach
    fun setUp() {
        service = AiLinkDiscoveryService(
            strategyGoalRepository,
            pdpGoalRepository,
            personRepository,
            linkService,
            userSettingsRepository,
            aiClientPort,
            objectMapper,
            aiConfigResolver
        )
    }

    @Test
    fun `should return error when AI is not configured`() {
        every { userSettingsRepository.findByUserId(userId) } returns null

        val result = service.generateLinkSuggestions(userId)

        assertTrue(result is AiLinkSuggestionsResult.Error)
        assertTrue((result as AiLinkSuggestionsResult.Error).message.contains("not configured"))
    }

    @Test
    fun `should return error when AI is not enabled`() {
        val settings = UserSettings(
            userId = userId,
            aiEnabled = false
        )
        every { userSettingsRepository.findByUserId(userId) } returns settings

        val result = service.generateLinkSuggestions(userId)

        assertTrue(result is AiLinkSuggestionsResult.Error)
        assertTrue((result as AiLinkSuggestionsResult.Error).message.contains("not configured"))
    }

    @Test
    fun `should return error when no strategy goals exist`() {
        val settings = UserSettings(
            userId = userId,
            aiEnabled = true,
            aiApiBaseUrl = "http://localhost:11434",
            aiModelName = "llama3"
        )
        every { userSettingsRepository.findByUserId(userId) } returns settings

        val pageable = PageRequest.of(0, 1000)
        every { strategyGoalRepository.findAllByUserId(userId, StrategyGoalStatus.ACTIVE, pageable) } returns PageImpl(emptyList())

        val result = service.generateLinkSuggestions(userId)

        assertTrue(result is AiLinkSuggestionsResult.Error)
        assertTrue((result as AiLinkSuggestionsResult.Error).message.contains("No active strategy goals"))
    }

    @Test
    fun `should return error when no PDP goals exist`() {
        val settings = UserSettings(
            userId = userId,
            aiEnabled = true,
            aiApiBaseUrl = "http://localhost:11434",
            aiModelName = "llama3"
        )
        every { userSettingsRepository.findByUserId(userId) } returns settings

        val strategyGoal = StrategyGoal(
            id = StrategyGoalId.generate(),
            userId = userId,
            title = "Improve tooling",
            description = "Make CI faster",
            sensitive = false
        )

        val pageable = PageRequest.of(0, 1000)
        every { strategyGoalRepository.findAllByUserId(userId, StrategyGoalStatus.ACTIVE, pageable) } returns PageImpl(listOf(strategyGoal))
        every { personRepository.findAllByUserIdUnpaged(userId) } returns emptyList()

        val result = service.generateLinkSuggestions(userId)

        assertTrue(result is AiLinkSuggestionsResult.Error)
        assertTrue((result as AiLinkSuggestionsResult.Error).message.contains("No active PDP goals"))
    }

    @Test
    fun `should return error for all PDP goals already linked`() {
        val settings = UserSettings(
            userId = userId,
            aiEnabled = true,
            aiApiBaseUrl = "http://localhost:11434",
            aiModelName = "llama3"
        )
        every { userSettingsRepository.findByUserId(userId) } returns settings

        val strategyGoal = StrategyGoal(
            id = StrategyGoalId.generate(),
            userId = userId,
            title = "Improve tooling",
            sensitive = false
        )

        val pdpGoal = PdpGoal(
            id = PdpGoalId.generate(),
            userId = userId,
            personId = personId,
            title = "Learn CI/CD",
            status = PdpGoalStatus.ACTIVE
        )

        val person = Person(
            id = personId,
            userId = userId,
            name = "Bob"
        )

        val pageable = PageRequest.of(0, 1000)
        every { strategyGoalRepository.findAllByUserId(userId, StrategyGoalStatus.ACTIVE, pageable) } returns PageImpl(listOf(strategyGoal))
        every { personRepository.findAllByUserIdUnpaged(userId) } returns listOf(person)
        every { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, PdpGoalStatus.ACTIVE, pageable) } returns PageImpl(listOf(pdpGoal))

        val alignmentScore = AlignmentScore(
            strategyGoalId = strategyGoal.id,
            strategyGoalTitle = strategyGoal.title,
            totalActivePdpGoals = 1,
            linkedPdpGoals = 1,
            alignmentPercentage = 100
        )
        every { linkService.getAllAlignmentScores(userId) } returns listOf(alignmentScore)
        every { linkService.getLinkedPdpGoals(strategyGoal.id, userId) } returns listOf(
            LinkedPdpGoalInfo(pdpGoal.id, personId, pdpGoal.title)
        )

        val result = service.generateLinkSuggestions(userId)

        assertTrue(result is AiLinkSuggestionsResult.Error)
        assertTrue((result as AiLinkSuggestionsResult.Error).message.contains("already linked"))
    }

    @Test
    fun `should return success with suggestions from LLM`() {
        val settings = UserSettings(
            userId = userId,
            aiEnabled = true,
            aiApiBaseUrl = "http://localhost:11434",
            aiModelName = "llama3"
        )
        every { userSettingsRepository.findByUserId(userId) } returns settings

        val strategyGoalId = StrategyGoalId.generate()
        val pdpGoalId = PdpGoalId.generate()
        val testPersonId = PersonId.generate()

        val strategyGoal = StrategyGoal(
            id = strategyGoalId,
            userId = userId,
            title = "Improve tooling",
            sensitive = false
        )

        val pdpGoal = PdpGoal(
            id = pdpGoalId,
            userId = userId,
            personId = testPersonId,
            title = "Learn CI/CD",
            status = PdpGoalStatus.ACTIVE
        )

        val person = Person(
            id = testPersonId,
            userId = userId,
            name = "Bob"
        )

        val pageable = PageRequest.of(0, 1000)
        every { strategyGoalRepository.findAllByUserId(userId, StrategyGoalStatus.ACTIVE, pageable) } returns PageImpl(listOf(strategyGoal))
        every { personRepository.findAllByUserIdUnpaged(userId) } returns listOf(person)
        every { pdpGoalRepository.findAllByUserIdAndPersonId(userId, testPersonId, PdpGoalStatus.ACTIVE, pageable) } returns PageImpl(listOf(pdpGoal))
        every { linkService.getAllAlignmentScores(userId) } returns emptyList()

        val aiResponse = """
            {
                "suggestions": [
                    {
                        "strategyGoalId": "${strategyGoalId.value}",
                        "pdpGoalId": "${pdpGoalId.value}",
                        "personId": "${testPersonId.value}",
                        "personName": "Bob",
                        "strategyGoalTitle": "Improve tooling",
                        "pdpGoalTitle": "Learn CI/CD",
                        "matchScore": 85,
                        "reasoning": "Both goals focus on CI/CD"
                    }
                ]
            }
        """.trimIndent()

        every {
            aiClientPort.chatCompletion(
                baseUrl = any(),
                apiKey = any(),
                model = any(),
                systemPrompt = any(),
                userMessage = any()
            )
        } returns AiCompletionResult.Success(aiResponse)

        val result = service.generateLinkSuggestions(userId)

        if (result is AiLinkSuggestionsResult.Error) {
            println("Error: ${result.message}")
        }

        assertTrue(result is AiLinkSuggestionsResult.Success, "Expected Success but got Error: ${(result as? AiLinkSuggestionsResult.Error)?.message}")
        val suggestions = (result as AiLinkSuggestionsResult.Success).suggestions
        assertEquals(1, suggestions.size)
        assertEquals(strategyGoalId, suggestions[0].strategyGoalId)
        assertEquals(pdpGoalId, suggestions[0].pdpGoalId)
        assertEquals(85, suggestions[0].matchScore)
    }

    @Test
    fun `should return error when LLM fails`() {
        val settings = UserSettings(
            userId = userId,
            aiEnabled = true,
            aiApiBaseUrl = "http://localhost:11434",
            aiModelName = "llama3"
        )
        every { userSettingsRepository.findByUserId(userId) } returns settings

        val strategyGoal = StrategyGoal(
            id = StrategyGoalId.generate(),
            userId = userId,
            title = "Improve tooling",
            sensitive = false
        )

        val pdpGoal = PdpGoal(
            id = PdpGoalId.generate(),
            userId = userId,
            personId = personId,
            title = "Learn CI/CD",
            status = PdpGoalStatus.ACTIVE
        )

        val person = Person(
            id = personId,
            userId = userId,
            name = "Bob"
        )

        val pageable = PageRequest.of(0, 1000)
        every { strategyGoalRepository.findAllByUserId(userId, StrategyGoalStatus.ACTIVE, pageable) } returns PageImpl(listOf(strategyGoal))
        every { personRepository.findAllByUserIdUnpaged(userId) } returns listOf(person)
        every { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, PdpGoalStatus.ACTIVE, pageable) } returns PageImpl(listOf(pdpGoal))
        every { linkService.getAllAlignmentScores(userId) } returns emptyList()

        every {
            aiClientPort.chatCompletion(
                baseUrl = any(),
                apiKey = any(),
                model = any(),
                systemPrompt = any(),
                userMessage = any()
            )
        } returns AiCompletionResult.Error("Connection timeout")

        val result = service.generateLinkSuggestions(userId)

        assertTrue(result is AiLinkSuggestionsResult.Error)
        assertTrue((result as AiLinkSuggestionsResult.Error).message.contains("Connection timeout"))
    }
}
