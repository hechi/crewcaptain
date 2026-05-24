package com.peoplemanager.adapters.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.peoplemanager.adapters.auth.SecurityConfig
import com.peoplemanager.adapters.web.dto.*
import com.peoplemanager.application.*
import com.peoplemanager.domain.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageImpl
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.Instant
import java.time.LocalDate
import java.util.*

@WebMvcTest(StrategyGoalController::class)
@Import(SecurityConfig::class)
class StrategyGoalControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @TestConfiguration
    class TestConfig {
        @Bean
        fun strategyGoalService(): StrategyGoalService = mockk()

        @Bean
        fun strategyGoalLinkService(): StrategyGoalLinkService = mockk()

        @Bean
        fun aiLinkDiscoveryService(): AiLinkDiscoveryService = mockk()
    }

    @Autowired
    private lateinit var strategyGoalService: StrategyGoalService

    @Autowired
    private lateinit var strategyGoalLinkService: StrategyGoalLinkService

    @Autowired
    private lateinit var aiLinkDiscoveryService: AiLinkDiscoveryService

    private val testUserId = UserId(UUID.randomUUID())
    private val testJwt = Jwt.withTokenValue("test-token")
        .header("alg", "RS256")
        .subject(testUserId.value.toString())
        .issuer("test-issuer")
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(3600))
        .claim("sub", testUserId.value.toString())
        .build()

    private fun jwtAuthentication() = jwt()
        .jwt(testJwt)
        .authorities(SimpleGrantedAuthority("ROLE_USER"))

    @Test
    fun `should create strategy goal with valid request`() {
        val request = CreateStrategyGoalRequest(
            title = "Test Strategy Goal",
            description = "Test Description",
            targetDate = LocalDate.now().plusMonths(3),
            sensitive = false
        )
        
        val goal = createTestStrategyGoal()
        every { strategyGoalService.createStrategyGoal(any()) } returns goal

        mockMvc.perform(
            post("/api/v1/strategy-goals")
                .with(jwtAuthentication())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(goal.id.value.toString()))
            .andExpect(jsonPath("$.title").value(goal.title))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
    }

    @Test
    fun `should return 401 when creating strategy goal without authentication`() {
        val request = CreateStrategyGoalRequest(
            title = "Test Strategy Goal",
            description = null,
            targetDate = null,
            sensitive = false
        )

        mockMvc.perform(
            post("/api/v1/strategy-goals")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should list strategy goals for authenticated user`() {
        val goals = listOf(createTestStrategyGoal(), createTestStrategyGoal())
        every { strategyGoalService.listStrategyGoals(any()) } returns PageImpl(goals)

        mockMvc.perform(
            get("/api/v1/strategy-goals")
                .with(jwtAuthentication())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").isArray)
            .andExpect(jsonPath("$.content.length()").value(2))
    }

    @Test
    fun `should get strategy goal by id`() {
        val goal = createTestStrategyGoal()
        every { strategyGoalService.getStrategyGoal(any()) } returns goal

        mockMvc.perform(
            get("/api/v1/strategy-goals/{id}", goal.id.value)
                .with(jwtAuthentication())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(goal.id.value.toString()))
    }

    @Test
    fun `should return 404 when strategy goal not found`() {
        val goalId = StrategyGoalId.generate()
        every { strategyGoalService.getStrategyGoal(any()) } throws StrategyGoalNotFoundException(goalId)

        mockMvc.perform(
            get("/api/v1/strategy-goals/{id}", goalId.value)
                .with(jwtAuthentication())
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should delete strategy goal`() {
        val goalId = StrategyGoalId.generate()
        every { strategyGoalService.deleteStrategyGoal(any()) } returns Unit

        mockMvc.perform(
            delete("/api/v1/strategy-goals/{id}", goalId.value)
                .with(jwtAuthentication())
        )
            .andExpect(status().isNoContent)
    }

    @Test
    fun `should achieve strategy goal`() {
        val goal = createTestStrategyGoal()
        every { strategyGoalService.achieveStrategyGoal(any()) } returns goal.copy(status = StrategyGoalStatus.ACHIEVED)

        mockMvc.perform(
            post("/api/v1/strategy-goals/{id}/achieve", goal.id.value)
                .with(jwtAuthentication())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("ACHIEVED"))
    }

    @Test
    fun `should drop strategy goal`() {
        val goal = createTestStrategyGoal()
        every { strategyGoalService.dropStrategyGoal(any()) } returns goal.copy(status = StrategyGoalStatus.DROPPED)

        mockMvc.perform(
            post("/api/v1/strategy-goals/{id}/drop", goal.id.value)
                .with(jwtAuthentication())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("DROPPED"))
    }

    @Test
    fun `should get alignment score for strategy goal`() {
        val goalId = StrategyGoalId.generate()
        val score = StrategyGoalLinkService.AlignmentScore(
            strategyGoalId = goalId,
            strategyGoalTitle = "Test Goal",
            totalActivePdpGoals = 10,
            linkedPdpGoals = 5,
            alignmentPercentage = 50
        )
        
        every { strategyGoalLinkService.getAlignmentScore(any(), any()) } returns score

        mockMvc.perform(
            get("/api/v1/strategy-goals/{id}/alignment", goalId.value)
                .with(jwtAuthentication())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.strategyGoalId").value(goalId.value.toString()))
            .andExpect(jsonPath("$.alignmentPercentage").value(50))
    }

    @Test
    fun `should get AI link suggestions`() {
        val suggestions = listOf(
            AiLinkDiscoveryService.LinkSuggestion(
                strategyGoalId = StrategyGoalId.generate(),
                strategyGoalTitle = "Strategy Goal",
                pdpGoalId = PdpGoalId.generate(),
                personId = PersonId.generate(),
                pdpGoalTitle = "PDP Goal",
                personName = "John Doe",
                matchScore = 75,
                reasoning = "Good match based on keywords"
            )
        )
        
        every { aiLinkDiscoveryService.findLinkSuggestions(any()) } returns suggestions

        mockMvc.perform(
            get("/api/v1/strategy-goals/ai-suggestions")
                .with(jwtAuthentication())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$[0].matchScore").value(75))
    }

    private fun createTestStrategyGoal(
        id: StrategyGoalId = StrategyGoalId.generate(),
        status: StrategyGoalStatus = StrategyGoalStatus.ACTIVE
    ): StrategyGoal {
        return StrategyGoal(
            id = id,
            userId = testUserId,
            title = "Test Strategy Goal",
            description = "Test Description",
            targetDate = LocalDate.now().plusMonths(3),
            sensitive = false,
            status = status
        )
    }
}
