package com.peoplemanager.adapters.web

import com.peoplemanager.adapters.auth.SecurityConfig
import com.peoplemanager.adapters.auth.UserProvisioningJwtAuthenticationConverter
import com.peoplemanager.application.AiTrendRadarResult
import com.peoplemanager.application.AiTrendRadarService
import com.peoplemanager.application.PersonNotFoundException
import com.peoplemanager.application.TrendDimension
import com.peoplemanager.application.TrendRadarInsight
import com.peoplemanager.application.UserProvisioningService
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.UserId
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.Instant
import java.util.UUID

@WebMvcTest(controllers = [AiTrendRadarController::class])
@Import(SecurityConfig::class, UserProvisioningJwtAuthenticationConverter::class, GlobalExceptionHandler::class)
@TestPropertySource(properties = [
    "spring.datasource.url=jdbc:h2:mem:test",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://auth.example.com",
    "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://auth.example.com/jwks"
])
class AiTrendRadarControllerTest {

    @TestConfiguration
    class TestConfig {
        @Bean
        fun aiTrendRadarService(): AiTrendRadarService = mockk()

        @Bean
        fun userProvisioningService(): UserProvisioningService = mockk()
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var aiTrendRadarService: AiTrendRadarService

    private val userId = UserId(UUID.randomUUID())
    private val personId = UUID.randomUUID().toString()

    private fun authenticatedJwt(uid: UserId = userId): JwtAuthenticationToken {
        val jwt = Jwt.withTokenValue("test-token")
            .header("alg", "RS256")
            .subject("test-subject")
            .issuer("https://auth.example.com")
            .claim("name", "Test User")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build()
        val authorities = listOf(SimpleGrantedAuthority("ROLE_USER"))
        val token = JwtAuthenticationToken(jwt, authorities, "test-subject")
        token.details = uid
        return token
    }

    @Test
    fun `should return insights on success`() {
        val insights = listOf(
            TrendRadarInsight("Burnout Risk", "High workload with low recognition.", TrendDimension.MORALE, 72),
            TrendRadarInsight("Growth Stagnation", "No PDP updates in 60 days.", TrendDimension.WORK_GROWTH_BALANCE, 55),
            TrendRadarInsight("Strong Output", "Consistent task completion.", TrendDimension.RECOGNITION, 80)
        )
        every { aiTrendRadarService.generateInsights(any(), any()) } returns AiTrendRadarResult.Success(insights)

        mockMvc.perform(
            post("/api/v1/persons/$personId/ai-trend-radar")
                .with(authentication(authenticatedJwt()))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.insights.length()").value(3))
            .andExpect(jsonPath("$.insights[0].title").value("Burnout Risk"))
            .andExpect(jsonPath("$.insights[0].dimension").value("MORALE"))
            .andExpect(jsonPath("$.insights[0].confidenceScore").value(72))
            .andExpect(jsonPath("$.insufficientData").value(false))
            .andExpect(jsonPath("$.error").doesNotExist())
    }

    @Test
    fun `should return insufficient data response`() {
        every { aiTrendRadarService.generateInsights(any(), any()) } returns
            AiTrendRadarResult.InsufficientData(2, "Scanning horizon... Need 2 more 1:1(s) to establish a baseline.")

        mockMvc.perform(
            post("/api/v1/persons/$personId/ai-trend-radar")
                .with(authentication(authenticatedJwt()))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.insights").isEmpty)
            .andExpect(jsonPath("$.insufficientData").value(true))
            .andExpect(jsonPath("$.meetingsNeeded").value(2))
            .andExpect(jsonPath("$.error").value("Scanning horizon... Need 2 more 1:1(s) to establish a baseline."))
    }

    @Test
    fun `should return error response when AI fails`() {
        every { aiTrendRadarService.generateInsights(any(), any()) } returns
            AiTrendRadarResult.Error("AI API connection refused")

        mockMvc.perform(
            post("/api/v1/persons/$personId/ai-trend-radar")
                .with(authentication(authenticatedJwt()))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.insights").isEmpty)
            .andExpect(jsonPath("$.insufficientData").value(false))
            .andExpect(jsonPath("$.error").value("AI API connection refused"))
    }

    @Test
    fun `should return 404 when person not found`() {
        every { aiTrendRadarService.generateInsights(any(), any()) } throws
            PersonNotFoundException(PersonId(UUID.fromString(personId)))

        mockMvc.perform(
            post("/api/v1/persons/$personId/ai-trend-radar")
                .with(authentication(authenticatedJwt()))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should return 401 without authentication`() {
        mockMvc.perform(
            post("/api/v1/persons/$personId/ai-trend-radar")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should scope request by authenticated user`() {
        val insights = listOf(
            TrendRadarInsight("Test", "Test desc", TrendDimension.MORALE, 50)
        )
        every { aiTrendRadarService.generateInsights(userId, PersonId(UUID.fromString(personId))) } returns
            AiTrendRadarResult.Success(insights)

        mockMvc.perform(
            post("/api/v1/persons/$personId/ai-trend-radar")
                .with(authentication(authenticatedJwt()))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.insights[0].title").value("Test"))
    }
}
