package com.peoplemanager.application

import com.peoplemanager.application.ports.PdpGoalRepository
import com.peoplemanager.application.ports.PersonRepository
import com.peoplemanager.application.ports.StrategyGoalRepository
import com.peoplemanager.domain.*
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

    private lateinit var service: AiLinkDiscoveryService

    private val userId = UserId(UUID.randomUUID())
    private val personId = PersonId(UUID.randomUUID())

    @BeforeEach
    fun setUp() {
        service = AiLinkDiscoveryService(
            strategyGoalRepository,
            pdpGoalRepository,
            personRepository,
            linkService
        )
    }

    @Test
    fun `should not suggest links for sensitive strategy goals`() {
        val sensitiveGoal = StrategyGoal(
            id = StrategyGoalId.generate(),
            userId = userId,
            title = "Improve culture",
            description = "Private details",
            sensitive = true
        )

        val nonSensitiveGoal = StrategyGoal(
            id = StrategyGoalId.generate(),
            userId = userId,
            title = "Improve tooling",
            description = "Make CI faster",
            sensitive = false
        )

        val pdpGoal = PdpGoal(
            id = PdpGoalId.generate(),
            userId = userId,
            personId = personId,
            title = "Improve tooling CI",
            status = PdpGoalStatus.ACTIVE
        )

        val person = Person(
            id = personId,
            userId = userId,
            name = "Bob"
        )

        val pageable = PageRequest.of(0, 1000)

        every { strategyGoalRepository.findAllByUserId(userId, StrategyGoalStatus.ACTIVE, pageable) } returns PageImpl(listOf(sensitiveGoal, nonSensitiveGoal))
        every { personRepository.findAllByUserIdUnpaged(userId) } returns listOf(person)
        every { pdpGoalRepository.findAllByUserIdAndPersonId(userId, personId, PdpGoalStatus.ACTIVE, pageable) } returns PageImpl(listOf(pdpGoal))
        every { linkService.getAllAlignmentScores(userId) } returns emptyList()

        val suggestions = service.findLinkSuggestions(userId)

        // Should include suggestion for non-sensitive goal only
        assertTrue(suggestions.any { it.strategyGoalId == nonSensitiveGoal.id })
        // Must not include sensitive goal
        assertFalse(suggestions.any { it.strategyGoalId == sensitiveGoal.id })
    }
}
