package com.peoplemanager.application

import com.peoplemanager.application.ports.PdpGoalRepository
import com.peoplemanager.application.ports.PersonRepository
import com.peoplemanager.application.ports.StrategyGoalRepository
import com.peoplemanager.domain.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.kotlin.*
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDate
import java.util.*

class AiLinkDiscoveryServiceTest {

    private val strategyGoalRepository: StrategyGoalRepository = mock()
    private val pdpGoalRepository: PdpGoalRepository = mock()
    private val personRepository: PersonRepository = mock()
    private val linkService: StrategyGoalLinkService = mock()
    private val service = AiLinkDiscoveryService(
        strategyGoalRepository,
        pdpGoalRepository,
        personRepository,
        linkService
    )

    private val testUserId = UserId(UUID.randomUUID())
    private val pageable = PageRequest.of(0, 1000)

    @Test
    fun `should suggest links based on keyword matching`() {
        val strategyGoal = createTestStrategyGoal(
            title = "Improve Java Skills",
            description = "Team should learn Java programming"
        )
        val person = createTestPerson(name = "John Doe")
        val pdpGoal = createTestPdpGoal(
            personId = person.id,
            title = "Learn Java Development",
            description = "Master Java programming language"
        )

        whenever(strategyGoalRepository.findAllByUserId(testUserId, StrategyGoalStatus.ACTIVE, pageable))
            .doReturn(PageImpl(listOf(strategyGoal)))
        whenever(personRepository.findAllByUserIdUnpaged(testUserId))
            .doReturn(listOf(person))
        whenever(pdpGoalRepository.findAllByUserIdAndPersonId(testUserId, person.id, PdpGoalStatus.ACTIVE, pageable))
            .doReturn(PageImpl(listOf(pdpGoal)))
        whenever(linkService.getAllAlignmentScores(any()))
            .doReturn(emptyList())

        val suggestions = service.findLinkSuggestions(testUserId)

        assertTrue(suggestions.isNotEmpty())
        assertTrue(suggestions[0].matchScore > 0)
        assertEquals(strategyGoal.id, suggestions[0].strategyGoalId)
        assertEquals(pdpGoal.id, suggestions[0].pdpGoalId)
    }

    @Test
    fun `should not suggest already linked goals`() {
        val strategyGoal = createTestStrategyGoal(title = "Learn Kotlin")
        val person = createTestPerson()
        val pdpGoal = createTestPdpGoal(personId = person.id, title = "Learn Kotlin Programming")

        whenever(strategyGoalRepository.findAllByUserId(testUserId, StrategyGoalStatus.ACTIVE, pageable))
            .doReturn(PageImpl(listOf(strategyGoal)))
        whenever(personRepository.findAllByUserIdUnpaged(testUserId))
            .doReturn(listOf(person))
        whenever(pdpGoalRepository.findAllByUserIdAndPersonId(testUserId, person.id, PdpGoalStatus.ACTIVE, pageable))
            .doReturn(PageImpl(listOf(pdpGoal)))
        
        val alignmentScore = StrategyGoalLinkService.AlignmentScore(
            strategyGoalId = strategyGoal.id,
            strategyGoalTitle = strategyGoal.title,
            totalActivePdpGoals = 1,
            linkedPdpGoals = 1,
            alignmentPercentage = 100
        )
        whenever(linkService.getAllAlignmentScores(testUserId))
            .doReturn(listOf(alignmentScore))
        whenever(linkService.getLinkedPdpGoals(strategyGoal.id, testUserId))
            .doReturn(listOf(StrategyGoalLinkService.LinkedPdpGoalInfo(pdpGoal.id, person.id, pdpGoal.title)))

        val suggestions = service.findLinkSuggestions(testUserId)

        assertTrue(suggestions.isEmpty())
    }

    @Test
    fun `should return empty list when no strategy goals exist`() {
        whenever(strategyGoalRepository.findAllByUserId(testUserId, StrategyGoalStatus.ACTIVE, pageable))
            .doReturn(PageImpl(emptyList()))

        val suggestions = service.findLinkSuggestions(testUserId)

        assertTrue(suggestions.isEmpty())
    }

    @Test
    fun `should calculate match score correctly`() {
        val strategyGoal = createTestStrategyGoal(
            title = "Modernize Tech Stack",
            description = "Move to modern technologies"
        )
        val person = createTestPerson()
        val pdpGoal = createTestPdpGoal(
            personId = person.id,
            title = "Modernize Tech Stack",
            description = "Learn new technologies"
        )

        whenever(strategyGoalRepository.findAllByUserId(testUserId, StrategyGoalStatus.ACTIVE, pageable))
            .doReturn(PageImpl(listOf(strategyGoal)))
        whenever(personRepository.findAllByUserIdUnpaged(testUserId))
            .doReturn(listOf(person))
        whenever(pdpGoalRepository.findAllByUserIdAndPersonId(testUserId, person.id, PdpGoalStatus.ACTIVE, pageable))
            .doReturn(PageImpl(listOf(pdpGoal)))
        whenever(linkService.getAllAlignmentScores(any()))
            .doReturn(emptyList())

        val suggestions = service.findLinkSuggestions(testUserId)

        assertEquals(1, suggestions.size)
        assertTrue(suggestions[0].matchScore >= 70, "Expected high match score for identical titles")
    }

    @Test
    fun `should limit suggestions to top 10`() {
        val strategyGoal = createTestStrategyGoal(title = "Development Goals")
        val person = createTestPerson()
        
        val pdpGoals = (1..15).map { index ->
            createTestPdpGoal(
                personId = person.id,
                title = "Development Goal $index",
                description = "Learn something new"
            )
        }

        whenever(strategyGoalRepository.findAllByUserId(testUserId, StrategyGoalStatus.ACTIVE, pageable))
            .doReturn(PageImpl(listOf(strategyGoal)))
        whenever(personRepository.findAllByUserIdUnpaged(testUserId))
            .doReturn(listOf(person))
        whenever(pdpGoalRepository.findAllByUserIdAndPersonId(testUserId, person.id, PdpGoalStatus.ACTIVE, pageable))
            .doReturn(PageImpl(pdpGoals))
        whenever(linkService.getAllAlignmentScores(any()))
            .doReturn(emptyList())

        val suggestions = service.findLinkSuggestions(testUserId)

        assertTrue(suggestions.size <= 10)
    }

    private fun createTestStrategyGoal(
        id: StrategyGoalId = StrategyGoalId.generate(),
        title: String = "Test Strategy Goal",
        description: String? = "Test Description"
    ): StrategyGoal {
        return StrategyGoal(
            id = id,
            userId = testUserId,
            title = title,
            description = description,
            targetDate = LocalDate.now().plusMonths(3),
            sensitive = false,
            status = StrategyGoalStatus.ACTIVE
        )
    }

    private fun createTestPerson(
        id: PersonId = PersonId.generate(),
        name: String = "Test Person"
    ): Person {
        return Person(
            id = id,
            userId = testUserId,
            name = name,
            preferredName = name,
            roleTitle = "Developer",
            timezone = "UTC",
            startDate = LocalDate.now().minusYears(1),
            email = "test@example.com",
            tags = emptyList(),
            moraleStatus = MoraleStatus.UNKNOWN,
            pinnedRememberItems = emptyList()
        )
    }

    private fun createTestPdpGoal(
        id: PdpGoalId = PdpGoalId.generate(),
        personId: PersonId,
        title: String = "Test PDP Goal",
        description: String? = "Test Description"
    ): PdpGoal {
        return PdpGoal(
            id = id,
            userId = testUserId,
            personId = personId,
            title = title,
            description = description,
            targetDate = LocalDate.now().plusMonths(3),
            status = PdpGoalStatus.ACTIVE
        )
    }
}
