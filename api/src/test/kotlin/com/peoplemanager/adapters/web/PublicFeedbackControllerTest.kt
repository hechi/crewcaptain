package com.peoplemanager.adapters.web

import tools.jackson.databind.ObjectMapper
import com.peoplemanager.adapters.auth.PublicFeedbackRateLimitFilter
import com.peoplemanager.adapters.auth.SecurityConfig
import com.peoplemanager.adapters.auth.UserProvisioningJwtAuthenticationConverter
import com.peoplemanager.adapters.web.dto.FeedbackAnswerDto
import com.peoplemanager.adapters.web.dto.SubmitPublicFeedbackRequest
import com.peoplemanager.application.FeedbackLinkExpiredException
import com.peoplemanager.application.FeedbackLinkRevokedException
import com.peoplemanager.application.FeedbackLinkTokenNotFoundException
import com.peoplemanager.application.UserProvisioningService
import com.peoplemanager.application.port.input.PublicFeedbackFormView
import com.peoplemanager.application.port.input.PublicFeedbackPort
import com.peoplemanager.domain.FeedbackAnswer
import com.peoplemanager.domain.FeedbackLinkId
import com.peoplemanager.domain.FeedbackQuestion
import com.peoplemanager.domain.FeedbackQuestionType
import com.peoplemanager.domain.FeedbackResponse
import com.peoplemanager.domain.FeedbackResponseId
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.UserId
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.util.UUID

@WebMvcTest(controllers = [PublicFeedbackController::class])
@Import(SecurityConfig::class, UserProvisioningJwtAuthenticationConverter::class, GlobalExceptionHandler::class, PublicFeedbackRateLimitFilter::class)
@TestPropertySource(properties = [
    "spring.datasource.url=jdbc:h2:mem:test",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://auth.example.com",
    "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://auth.example.com/jwks",
    "app.feedback.rate-limit.max-submissions=100"
])
class PublicFeedbackControllerTest {

    @TestConfiguration
    class TestConfig {
        @Bean fun publicFeedbackPort(): PublicFeedbackPort = mockk()
        @Bean fun userProvisioningService(): UserProvisioningService = mockk()
    }

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var objectMapper: ObjectMapper
    @Autowired private lateinit var publicFeedbackPort: PublicFeedbackPort

    @org.junit.jupiter.api.BeforeEach
    fun resetMocks() = io.mockk.clearMocks(publicFeedbackPort)

    private fun form() = PublicFeedbackFormView(
        personName = "Alex",
        title = "Feedback for Alex",
        description = "Help Alex improve",
        requestSubmitterInfo = true,
        questions = listOf(FeedbackQuestion("q1", FeedbackQuestionType.RATING, "Rate"))
    )

    @Test
    fun `GET form works without authentication`() {
        every { publicFeedbackPort.getForm(any()) } returns form()
        mockMvc.perform(get("/api/v1/public/feedback/tok123"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.personName").value("Alex"))
            .andExpect(jsonPath("$.questions[0].type").value("RATING"))
    }

    @Test
    fun `GET form returns 404 for unknown token`() {
        every { publicFeedbackPort.getForm(any()) } throws FeedbackLinkTokenNotFoundException()
        mockMvc.perform(get("/api/v1/public/feedback/nope"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `GET form returns 410 when expired`() {
        every { publicFeedbackPort.getForm(any()) } throws FeedbackLinkExpiredException()
        mockMvc.perform(get("/api/v1/public/feedback/tok"))
            .andExpect(status().isGone)
            .andExpect(jsonPath("$.message").value("This link has expired"))
    }

    @Test
    fun `GET form returns 410 when revoked`() {
        every { publicFeedbackPort.getForm(any()) } throws FeedbackLinkRevokedException()
        mockMvc.perform(get("/api/v1/public/feedback/tok"))
            .andExpect(status().isGone)
            .andExpect(jsonPath("$.message").value("This link has been revoked"))
    }

    @Test
    fun `POST submit works without authentication and returns 201`() {
        every { publicFeedbackPort.submit(any()) } returns sampleResponse()
        val body = SubmitPublicFeedbackRequest(
            anonymous = true,
            answers = listOf(FeedbackAnswerDto("q1", 5, null))
        )
        mockMvc.perform(
            post("/api/v1/public/feedback/tok123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))
        ).andExpect(status().isCreated)

        verify { publicFeedbackPort.submit(any()) }
    }

    @Test
    fun `POST with filled honeypot is silently accepted and NOT persisted`() {
        val body = SubmitPublicFeedbackRequest(
            anonymous = true,
            answers = listOf(FeedbackAnswerDto("q1", 5, null)),
            website = "http://spam.example.com"
        )
        mockMvc.perform(
            post("/api/v1/public/feedback/tok123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))
        ).andExpect(status().isNoContent)

        verify(exactly = 0) { publicFeedbackPort.submit(any()) }
    }

    @Test
    fun `POST forwards answers to the port`() {
        val cmdSlot = slot<com.peoplemanager.application.commands.SubmitFeedbackCommand>()
        every { publicFeedbackPort.submit(capture(cmdSlot)) } returns sampleResponse()
        val body = SubmitPublicFeedbackRequest(
            anonymous = false,
            submitterName = "Sam",
            answers = listOf(FeedbackAnswerDto("q1", 4, null), FeedbackAnswerDto("q2", null, "great"))
        )
        mockMvc.perform(
            post("/api/v1/public/feedback/tok123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))
        ).andExpect(status().isCreated)

        cmdSlot.captured.token shouldBeEqualTo "tok123"
        cmdSlot.captured.answers.size shouldBeEqualTo 2
    }

    private infix fun <T> T.shouldBeEqualTo(expected: T) { assert(this == expected) { "expected $expected but was $this" } }

    private fun sampleResponse() = FeedbackResponse(
        id = FeedbackResponseId.generate(),
        userId = UserId(UUID.randomUUID()),
        personId = PersonId(UUID.randomUUID()),
        linkId = FeedbackLinkId.generate(),
        answers = listOf(FeedbackAnswer("q1", ratingValue = 5))
    )
}
