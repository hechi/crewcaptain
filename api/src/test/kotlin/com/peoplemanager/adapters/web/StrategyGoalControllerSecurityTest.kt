package com.peoplemanager.adapters.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.peoplemanager.adapters.auth.SecurityConfig
import com.peoplemanager.adapters.web.dto.CreateStrategyGoalRequest
import com.peoplemanager.application.AiLinkDiscoveryService
import com.peoplemanager.application.StrategyGoalLinkService
import com.peoplemanager.application.StrategyGoalNotFoundException
import com.peoplemanager.application.StrategyGoalService
import com.peoplemanager.domain.StrategyGoal
import com.peoplemanager.domain.StrategyGoalId
import com.peoplemanager.domain.StrategyGoalStatus
import com.peoplemanager.domain.UserId
import io.mockk.every
import io.mockk.mockk
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
class StrategyGoalControllerSecurityTest {

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

    private val testUserId = UserId(UUID.randomUUID())
    private val otherUserId = UserId(UUID.randomUUID())
    
    private fun jwtForUser(userId: UserId) = jwt()
        .jwt(
            Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject(userId.value.toString())
                .issuer("test-issuer")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("sub", userId.value.toString())
                .build()
        )
        .authorities(SimpleGrantedAuthority("ROLE_USER"))

    @Test
    fun `should return 401 when accessing strategy goals without authentication`() {
        mockMvc.perform(get("/api/v1/strategy-goals"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should return 401 when creating strategy goal without authentication`() {
        val request = CreateStrategyGoalRequest(
            title = "Test Goal",
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
    fun `should return 404 when accessing other users strategy goal`() {
        val goalId = StrategyGoalId.generate()
        
        every { strategyGoalService.getStrategyGoal(any()) } 
            throws StrategyGoalNotFoundException(goalId)

        mockMvc.perform(
            get("/api/v1/strategy-goals/{id}", goalId.value)
                .with(jwtForUser(testUserId))
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should return 404 when deleting other users strategy goal`() {
        val goalId = StrategyGoalId.generate()
        
        every { strategyGoalService.deleteStrategyGoal(any()) } 
            throws StrategyGoalNotFoundException(goalId)

        mockMvc.perform(
            delete("/api/v1/strategy-goals/{id}", goalId.value)
                .with(jwtForUser(testUserId))
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should only list strategy goals for authenticated user`() {
        val userGoal = createTestStrategyGoal(userId = testUserId)
        
        every { strategyGoalService.listStrategyGoals(any()) } 
            returns PageImpl(listOf(userGoal))

        mockMvc.perform(
            get("/api/v1/strategy-goals")
                .with(jwtForUser(testUserId))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").isArray)
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].userId").doesNotExist())
    }

    private fun createTestStrategyGoal(
        id: StrategyGoalId = StrategyGoalId.generate(),
        userId: UserId = testUserId,
        status: StrategyGoalStatus = StrategyGoalStatus.ACTIVE
    ): StrategyGoal {
        return StrategyGoal(
            id = id,
            userId = userId,
            title = "Test Strategy Goal",
            description = "Test Description",
            targetDate = LocalDate.now().plusMonths(3),
            sensitive = false,
            status = status
        )
    }
}
