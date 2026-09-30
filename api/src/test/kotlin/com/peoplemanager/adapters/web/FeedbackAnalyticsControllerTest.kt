package com.peoplemanager.adapters.web

import tools.jackson.databind.ObjectMapper
import com.peoplemanager.adapters.auth.SecurityConfig
import com.peoplemanager.adapters.auth.UserProvisioningJwtAuthenticationConverter
import com.peoplemanager.application.FeedbackAnalyticsService
import com.peoplemanager.application.FeedbackSummaryAiService
import com.peoplemanager.application.FeedbackSummaryResult
import com.peoplemanager.application.PersonNotFoundException
import com.peoplemanager.application.UserProvisioningService
import com.peoplemanager.domain.*
import com.peoplemanager.domain.service.FeedbackAnalytics
import com.peoplemanager.domain.service.RatingPeriod
import com.peoplemanager.domain.service.ThemeCount
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@WebMvcTest(controllers = [FeedbackAnalyticsController::class])
@Import(SecurityConfig::class, UserProvisioningJwtAuthenticationConverter::class, GlobalExceptionHandler::class, com.peoplemanager.adapters.auth.PublicFeedbackRateLimitFilter::class)
@TestPropertySource(properties = [
    "spring.datasource.url=jdbc:h2:mem:test",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://auth.example.com",
    "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://auth.example.com/jwks"
])
class FeedbackAnalyticsControllerTest {

    @TestConfiguration
    class TestConfig {
        @Bean fun analyticsService(): FeedbackAnalyticsService = mockk()
        @Bean fun summaryAiService(): FeedbackSummaryAiService = mockk()
        @Bean fun userProvisioningService(): UserProvisioningService = mockk()
    }

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var objectMapper: ObjectMapper
    @Autowired private lateinit var analyticsService: FeedbackAnalyticsService
    @Autowired private lateinit var summaryAiService: FeedbackSummaryAiService

    private val userId = UserId(UUID.randomUUID())
    private val personId = UUID.randomUUID()

    private fun jwt(): JwtAuthenticationToken {
        val jwt = Jwt.withTokenValue("t").header("alg", "RS256").subject("s")
            .issuer("https://auth.example.com").claim("name", "U")
            .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(3600)).build()
        return JwtAuthenticationToken(jwt, listOf(SimpleGrantedAuthority("ROLE_USER")), "s").apply { details = userId }
    }

    @Test
    fun `GET analytics returns computed values`() {
        every { analyticsService.getAnalytics(any()) } returns FeedbackAnalytics(
            totalResponses = 3,
            overallAverageRating = 4.33,
            periods = listOf(RatingPeriod("2026-Q1", 4.0, 2)),
            topThemes = listOf(ThemeCount("communication", 2))
        )
        mockMvc.perform(get("/api/v1/persons/$personId/feedback-analytics?bucket=QUARTER").with(authentication(jwt())))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalResponses").value(3))
            .andExpect(jsonPath("$.overallAverageRating").value(4.33))
            .andExpect(jsonPath("$.periods[0].label").value("2026-Q1"))
            .andExpect(jsonPath("$.topThemes[0].theme").value("communication"))
    }

    @Test
    fun `GET analytics 404 when person not owned`() {
        every { analyticsService.getAnalytics(any()) } throws PersonNotFoundException(PersonId(personId))
        mockMvc.perform(get("/api/v1/persons/$personId/feedback-analytics").with(authentication(jwt())))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `POST generate summary returns content on success`() {
        every { summaryAiService.generate(any()) } returns FeedbackSummaryResult.Success("Great collaborator", 4)
        mockMvc.perform(
            post("/api/v1/persons/$personId/feedback-summary/generate").with(authentication(jwt()))
                .contentType(MediaType.APPLICATION_JSON).content("{}")
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.content").value("Great collaborator"))
            .andExpect(jsonPath("$.responseCount").value(4))
    }

    @Test
    fun `POST generate summary returns 422 on error`() {
        every { summaryAiService.generate(any()) } returns FeedbackSummaryResult.Error("no feedback")
        mockMvc.perform(
            post("/api/v1/persons/$personId/feedback-summary/generate").with(authentication(jwt()))
                .contentType(MediaType.APPLICATION_JSON).content("{}")
        ).andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.message").value("no feedback"))
    }

    @Test
    fun `POST save summary returns 201`() {
        every { analyticsService.saveSummary(any()) } returns FeedbackSummary(
            id = FeedbackSummaryId.generate(), userId = userId, personId = PersonId(personId),
            periodFrom = LocalDate.of(2026, 1, 1), periodTo = LocalDate.of(2026, 6, 30),
            content = "Edited summary", responseCount = 4
        )
        mockMvc.perform(
            post("/api/v1/persons/$personId/feedback-summaries").with(authentication(jwt()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"periodFrom":"2026-01-01","periodTo":"2026-06-30","content":"Edited summary","responseCount":4}""")
        ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.content").value("Edited summary"))
    }

    @Test
    fun `POST save summary rejects blank content with 400`() {
        mockMvc.perform(
            post("/api/v1/persons/$personId/feedback-summaries").with(authentication(jwt()))
                .contentType(MediaType.APPLICATION_JSON).content("""{"content":""}""")
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `GET analytics requires auth`() {
        mockMvc.perform(get("/api/v1/persons/$personId/feedback-analytics"))
            .andExpect(status().isUnauthorized)
    }
}
