package com.peoplemanager.adapters.web

import tools.jackson.databind.ObjectMapper
import com.peoplemanager.adapters.auth.SecurityConfig
import com.peoplemanager.adapters.auth.UserProvisioningJwtAuthenticationConverter
import com.peoplemanager.adapters.web.dto.FeedbackQuestionDto
import com.peoplemanager.adapters.web.dto.GenerateFeedbackTemplateRequest
import com.peoplemanager.adapters.web.dto.SaveFeedbackTemplateRequest
import com.peoplemanager.application.FeedbackTemplateAiService
import com.peoplemanager.application.FeedbackTemplateGenerationResult
import com.peoplemanager.application.FeedbackTemplateNotFoundException
import com.peoplemanager.application.UserProvisioningService
import com.peoplemanager.application.port.input.FeedbackTemplateCommandPort
import com.peoplemanager.application.port.input.FeedbackTemplateQueryPort
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
import java.util.UUID

@WebMvcTest(controllers = [FeedbackTemplateController::class])
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
class FeedbackTemplateControllerTest {

    @TestConfiguration
    class TestConfig {
        @Bean fun commandPort(): FeedbackTemplateCommandPort = mockk()
        @Bean fun queryPort(): FeedbackTemplateQueryPort = mockk()
        @Bean fun aiService(): FeedbackTemplateAiService = mockk()
        @Bean fun userProvisioningService(): UserProvisioningService = mockk()
    }

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var objectMapper: ObjectMapper
    @Autowired private lateinit var commandPort: FeedbackTemplateCommandPort
    @Autowired private lateinit var queryPort: FeedbackTemplateQueryPort
    @Autowired private lateinit var aiService: FeedbackTemplateAiService

    private val userId = UserId(UUID.randomUUID())

    private fun jwt(): JwtAuthenticationToken {
        val jwt = Jwt.withTokenValue("t").header("alg", "RS256").subject("s")
            .issuer("https://auth.example.com").claim("name", "U")
            .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(3600)).build()
        return JwtAuthenticationToken(jwt, listOf(SimpleGrantedAuthority("ROLE_USER")), "s").apply { details = userId }
    }

    private fun template() = FeedbackTemplate(
        id = FeedbackTemplateId.generate(),
        userId = userId,
        title = "Mid-year",
        description = "d",
        questions = listOf(FeedbackQuestion("q1", FeedbackQuestionType.RATING, "Rate"))
    )

    @Test
    fun `POST creates template and returns 201`() {
        every { commandPort.createTemplate(any()) } returns template()
        val request = SaveFeedbackTemplateRequest(
            title = "Mid-year",
            description = "d",
            questions = listOf(FeedbackQuestionDto("q1", "RATING", "Rate"))
        )
        mockMvc.perform(
            post("/api/v1/feedback-templates")
                .with(authentication(jwt()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.title").value("Mid-year"))
            .andExpect(jsonPath("$.questions[0].type").value("RATING"))
    }

    @Test
    fun `POST returns 400 when title blank`() {
        val request = SaveFeedbackTemplateRequest(title = "", questions = emptyList())
        mockMvc.perform(
            post("/api/v1/feedback-templates")
                .with(authentication(jwt()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `GET list returns templates`() {
        every { queryPort.listTemplates(any()) } returns listOf(template())
        mockMvc.perform(get("/api/v1/feedback-templates").with(authentication(jwt())))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
    }

    @Test
    fun `GET starters returns two starters`() {
        every { queryPort.listStarterTemplates() } returns FeedbackStarterTemplates.forUser(userId)
        mockMvc.perform(get("/api/v1/feedback-templates/starters").with(authentication(jwt())))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].title").value("Performance review"))
    }

    @Test
    fun `GET by id returns 404 when missing`() {
        every { queryPort.getTemplate(any()) } throws FeedbackTemplateNotFoundException(FeedbackTemplateId.generate())
        mockMvc.perform(get("/api/v1/feedback-templates/${UUID.randomUUID()}").with(authentication(jwt())))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `DELETE returns 204`() {
        every { commandPort.deleteTemplate(any()) } returns Unit
        mockMvc.perform(delete("/api/v1/feedback-templates/${UUID.randomUUID()}").with(authentication(jwt())))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `POST ai-generate returns draft on success`() {
        every { aiService.generate(any()) } returns FeedbackTemplateGenerationResult.Success(template())
        mockMvc.perform(
            post("/api/v1/feedback-templates/ai-generate")
                .with(authentication(jwt()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(GenerateFeedbackTemplateRequest("retro")))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.title").value("Mid-year"))
    }

    @Test
    fun `POST ai-generate returns 422 on AI error`() {
        every { aiService.generate(any()) } returns FeedbackTemplateGenerationResult.Error("nope")
        mockMvc.perform(
            post("/api/v1/feedback-templates/ai-generate")
                .with(authentication(jwt()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(GenerateFeedbackTemplateRequest("retro")))
        )
            .andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.message").value("nope"))
    }

    @Test
    fun `GET list returns 401 without auth`() {
        mockMvc.perform(get("/api/v1/feedback-templates")).andExpect(status().isUnauthorized)
    }
}
