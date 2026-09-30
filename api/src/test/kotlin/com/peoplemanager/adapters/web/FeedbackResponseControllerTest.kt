package com.peoplemanager.adapters.web

import tools.jackson.databind.ObjectMapper
import com.peoplemanager.adapters.auth.SecurityConfig
import com.peoplemanager.adapters.auth.UserProvisioningJwtAuthenticationConverter
import com.peoplemanager.application.FeedbackResponseNotFoundException
import com.peoplemanager.application.UserProvisioningService
import com.peoplemanager.application.port.input.FeedbackResponseCommandPort
import com.peoplemanager.application.port.input.FeedbackResponseQueryPort
import com.peoplemanager.domain.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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

@WebMvcTest(controllers = [FeedbackResponseController::class])
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
class FeedbackResponseControllerTest {

    @TestConfiguration
    class TestConfig {
        @Bean fun commandPort(): FeedbackResponseCommandPort = mockk()
        @Bean fun queryPort(): FeedbackResponseQueryPort = mockk()
        @Bean fun userProvisioningService(): UserProvisioningService = mockk()
    }

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var objectMapper: ObjectMapper
    @Autowired private lateinit var commandPort: FeedbackResponseCommandPort
    @Autowired private lateinit var queryPort: FeedbackResponseQueryPort

    private val userId = UserId(UUID.randomUUID())
    private val personId = PersonId(UUID.randomUUID())
    private val responseId = FeedbackResponseId.generate()

    private fun jwt(): JwtAuthenticationToken {
        val jwt = Jwt.withTokenValue("t").header("alg", "RS256").subject("s")
            .issuer("https://auth.example.com").claim("name", "U")
            .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(3600)).build()
        return JwtAuthenticationToken(jwt, listOf(SimpleGrantedAuthority("ROLE_USER")), "s").apply { details = userId }
    }

    private fun response(status: FeedbackResponseStatus = FeedbackResponseStatus.PENDING) = FeedbackResponse(
        id = responseId, userId = userId, personId = personId, linkId = FeedbackLinkId.generate(),
        answers = listOf(FeedbackAnswer("q1", ratingValue = 5)), status = status
    )

    @Test
    fun `GET list returns responses`() {
        every { queryPort.listResponses(any()) } returns listOf(response())
        mockMvc.perform(get("/api/v1/persons/${personId.value}/feedback-responses").with(authentication(jwt())))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].status").value("PENDING"))
    }

    @Test
    fun `PATCH approve returns updated`() {
        every { commandPort.updateResponse(any()) } returns response(FeedbackResponseStatus.APPROVED)
        mockMvc.perform(
            patch("/api/v1/feedback-responses/${responseId.value}").with(authentication(jwt()))
                .contentType(MediaType.APPLICATION_JSON).content("""{"approve":true}""")
        ).andExpect(status().isOk).andExpect(jsonPath("$.status").value("APPROVED"))
    }

    @Test
    fun `DELETE returns 204`() {
        every { commandPort.deleteResponse(any()) } returns Unit
        mockMvc.perform(delete("/api/v1/feedback-responses/${responseId.value}").with(authentication(jwt())))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `DELETE returns 404 when missing`() {
        every { commandPort.deleteResponse(any()) } throws FeedbackResponseNotFoundException(responseId)
        mockMvc.perform(delete("/api/v1/feedback-responses/${responseId.value}").with(authentication(jwt())))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `POST bulk approve returns 204`() {
        every { commandPort.bulkUpdate(any()) } returns Unit
        mockMvc.perform(
            post("/api/v1/feedback-responses/bulk").with(authentication(jwt()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"responseIds":["${responseId.value}"],"action":"APPROVE"}""")
        ).andExpect(status().isNoContent)
        verify { commandPort.bulkUpdate(any()) }
    }

    @Test
    fun `POST convert returns updated with conversion info`() {
        every { commandPort.convertResponse(any()) } returns response().markConverted(FeedbackConversionType.KUDO, "abc")
        mockMvc.perform(
            post("/api/v1/feedback-responses/${responseId.value}/convert").with(authentication(jwt()))
                .contentType(MediaType.APPLICATION_JSON).content("""{"type":"KUDO"}""")
        ).andExpect(status().isOk).andExpect(jsonPath("$.convertedToType").value("KUDO"))
    }

    @Test
    fun `POST convert rejects bad type with 400`() {
        mockMvc.perform(
            post("/api/v1/feedback-responses/${responseId.value}/convert").with(authentication(jwt()))
                .contentType(MediaType.APPLICATION_JSON).content("""{"type":"WIDGET"}""")
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `GET list requires auth`() {
        mockMvc.perform(get("/api/v1/persons/${personId.value}/feedback-responses"))
            .andExpect(status().isUnauthorized)
    }
}
