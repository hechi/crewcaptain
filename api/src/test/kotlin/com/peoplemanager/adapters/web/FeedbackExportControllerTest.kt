package com.peoplemanager.adapters.web

import com.peoplemanager.adapters.auth.SecurityConfig
import com.peoplemanager.adapters.auth.UserProvisioningJwtAuthenticationConverter
import com.peoplemanager.application.FeedbackExportService
import com.peoplemanager.application.PersonNotFoundException
import com.peoplemanager.application.UserProvisioningService
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.UserId
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.util.UUID

@WebMvcTest(controllers = [FeedbackExportController::class])
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
class FeedbackExportControllerTest {

    @TestConfiguration
    class TestConfig {
        @Bean fun feedbackExportService(): FeedbackExportService = mockk()
        @Bean fun userProvisioningService(): UserProvisioningService = mockk()
    }

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var feedbackExportService: FeedbackExportService

    private val userId = UserId(UUID.randomUUID())
    private val personId = UUID.randomUUID()

    private fun jwt(): JwtAuthenticationToken {
        val jwt = Jwt.withTokenValue("t").header("alg", "RS256").subject("s")
            .issuer("https://auth.example.com").claim("name", "U")
            .issuedAt(java.time.Instant.now()).expiresAt(java.time.Instant.now().plusSeconds(3600)).build()
        return JwtAuthenticationToken(jwt, listOf(SimpleGrantedAuthority("ROLE_USER")), "s").apply { details = userId }
    }

    @Test
    fun `markdown export returns text markdown attachment`() {
        every { feedbackExportService.exportMarkdown(any(), any(), any()) } returns "# Peer Feedback"
        mockMvc.perform(get("/api/v1/persons/$personId/feedback-export?format=md").with(authentication(jwt())))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/markdown; charset=UTF-8"))
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"peer-feedback.md\""))
    }

    @Test
    fun `pdf export returns application pdf attachment`() {
        every { feedbackExportService.exportPdf(any(), any(), any()) } returns byteArrayOf(0x25, 0x50, 0x44, 0x46)
        mockMvc.perform(get("/api/v1/persons/$personId/feedback-export?format=pdf").with(authentication(jwt())))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "application/pdf"))
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"peer-feedback.pdf\""))
    }

    @Test
    fun `unknown format returns 400`() {
        mockMvc.perform(get("/api/v1/persons/$personId/feedback-export?format=xml").with(authentication(jwt())))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `returns 404 when person not owned`() {
        every { feedbackExportService.exportMarkdown(any(), any(), any()) } throws PersonNotFoundException(PersonId(personId))
        mockMvc.perform(get("/api/v1/persons/$personId/feedback-export?format=md").with(authentication(jwt())))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `requires authentication`() {
        mockMvc.perform(get("/api/v1/persons/$personId/feedback-export?format=md"))
            .andExpect(status().isUnauthorized)
    }
}
