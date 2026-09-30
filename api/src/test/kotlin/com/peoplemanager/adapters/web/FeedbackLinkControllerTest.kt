package com.peoplemanager.adapters.web

import tools.jackson.databind.ObjectMapper
import com.peoplemanager.adapters.auth.SecurityConfig
import com.peoplemanager.adapters.auth.UserProvisioningJwtAuthenticationConverter
import com.peoplemanager.adapters.web.dto.BulkCreateFeedbackLinksRequest
import com.peoplemanager.adapters.web.dto.CreateFeedbackLinkRequest
import com.peoplemanager.adapters.web.dto.FeedbackQuestionDto
import com.peoplemanager.application.FeedbackLinkNotFoundException
import com.peoplemanager.application.PersonNotFoundException
import com.peoplemanager.application.UserProvisioningService
import com.peoplemanager.application.port.input.FeedbackLinkCommandPort
import com.peoplemanager.application.port.input.FeedbackLinkQueryPort
import com.peoplemanager.application.port.input.FeedbackLinkWithUsage
import com.peoplemanager.domain.*
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
import java.time.temporal.ChronoUnit
import java.util.UUID

@WebMvcTest(controllers = [FeedbackLinkController::class])
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
class FeedbackLinkControllerTest {

    @TestConfiguration
    class TestConfig {
        @Bean fun commandPort(): FeedbackLinkCommandPort = mockk()
        @Bean fun queryPort(): FeedbackLinkQueryPort = mockk()
        @Bean fun userProvisioningService(): UserProvisioningService = mockk()
    }

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var objectMapper: ObjectMapper
    @Autowired private lateinit var commandPort: FeedbackLinkCommandPort
    @Autowired private lateinit var queryPort: FeedbackLinkQueryPort

    private val userId = UserId(UUID.randomUUID())
    private val personId = PersonId(UUID.randomUUID())

    private fun jwt(): JwtAuthenticationToken {
        val jwt = Jwt.withTokenValue("t").header("alg", "RS256").subject("s")
            .issuer("https://auth.example.com").claim("name", "U")
            .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(3600)).build()
        return JwtAuthenticationToken(jwt, listOf(SimpleGrantedAuthority("ROLE_USER")), "s").apply { details = userId }
    }

    private fun link() = FeedbackLink(
        id = FeedbackLinkId.generate(), userId = userId, personId = personId,
        token = "tok123", title = "Feedback for Alex",
        questions = listOf(FeedbackQuestion("q1", FeedbackQuestionType.RATING, "Rate")),
        expiresAt = Instant.now().plus(14, ChronoUnit.DAYS)
    )

    @Test
    fun `POST create returns 201 with token`() {
        every { commandPort.createLink(any()) } returns link()
        val request = CreateFeedbackLinkRequest(
            title = "Feedback for Alex",
            questions = listOf(FeedbackQuestionDto("q1", "RATING", "Rate"))
        )
        mockMvc.perform(
            post("/api/v1/persons/${personId.value}/feedback-links").with(authentication(jwt()))
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.token").value("tok123"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
    }

    @Test
    fun `POST create returns 404 when person not owned`() {
        every { commandPort.createLink(any()) } throws PersonNotFoundException(personId)
        val request = CreateFeedbackLinkRequest(title = "t", questions = listOf(FeedbackQuestionDto("q1", "RATING", "Rate")))
        mockMvc.perform(
            post("/api/v1/persons/${personId.value}/feedback-links").with(authentication(jwt()))
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isNotFound)
    }

    @Test
    fun `GET list returns links with usage`() {
        every { queryPort.listLinks(any()) } returns listOf(FeedbackLinkWithUsage(link(), 2, Instant.now()))
        mockMvc.perform(get("/api/v1/persons/${personId.value}/feedback-links").with(authentication(jwt())))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].submissionCount").value(2))
    }

    @Test
    fun `POST revoke returns updated link`() {
        every { commandPort.revokeLink(any()) } returns link().revoke()
        mockMvc.perform(post("/api/v1/feedback-links/${UUID.randomUUID()}/revoke").with(authentication(jwt())))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("REVOKED"))
    }

    @Test
    fun `POST revoke returns 404 when not owned`() {
        every { commandPort.revokeLink(any()) } throws FeedbackLinkNotFoundException(FeedbackLinkId.generate())
        mockMvc.perform(post("/api/v1/feedback-links/${UUID.randomUUID()}/revoke").with(authentication(jwt())))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `POST bulk returns 201 with per-person links`() {
        every { commandPort.bulkCreateLinks(any()) } returns listOf(link() to "Alex")
        val request = BulkCreateFeedbackLinksRequest(
            personIds = listOf(personId.value),
            templateId = UUID.randomUUID()
        )
        mockMvc.perform(
            post("/api/v1/feedback-links/bulk").with(authentication(jwt()))
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.links[0].personName").value("Alex"))
    }

    @Test
    fun `GET list requires auth`() {
        mockMvc.perform(get("/api/v1/persons/${personId.value}/feedback-links"))
            .andExpect(status().isUnauthorized)
    }
}
