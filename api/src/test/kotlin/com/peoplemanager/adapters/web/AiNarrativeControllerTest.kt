package com.peoplemanager.adapters.web

import com.peoplemanager.adapters.auth.SecurityConfig
import com.peoplemanager.adapters.auth.UserProvisioningJwtAuthenticationConverter
import com.peoplemanager.application.AiNarrativeResult
import com.peoplemanager.application.AiNarrativeService
import com.peoplemanager.application.PersonNotFoundException
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

@WebMvcTest(controllers = [AiNarrativeController::class])
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
class AiNarrativeControllerTest {

    @TestConfiguration
    class TestConfig {
        @Bean
        fun aiNarrativeService(): AiNarrativeService = mockk()

        @Bean
        fun userProvisioningService(): UserProvisioningService = mockk()
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var aiNarrativeService: AiNarrativeService

    private val userId = UserId(UUID.randomUUID())
    private val personId = UUID.randomUUID()

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
    fun `should return narrative on success`() {
        every { aiNarrativeService.generateNarrative(any(), any(), any(), any()) } returns AiNarrativeResult.Success(
            "Alice demonstrated exceptional impact through her leadership..."
        )

        mockMvc.perform(
            post("/api/v1/persons/$personId/ai-narrative")
                .with(authentication(authenticatedJwt()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"dateFrom":"2026-01-01","dateTo":"2026-06-30"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.narrative").value("Alice demonstrated exceptional impact through her leadership..."))
            .andExpect(jsonPath("$.error").doesNotExist())
    }

    @Test
    fun `should return error message when AI fails`() {
        every { aiNarrativeService.generateNarrative(any(), any(), any(), any()) } returns AiNarrativeResult.Error(
            "Cannot connect to AI API."
        )

        mockMvc.perform(
            post("/api/v1/persons/$personId/ai-narrative")
                .with(authentication(authenticatedJwt()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"dateFrom":"2026-01-01","dateTo":"2026-06-30"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.narrative").doesNotExist())
            .andExpect(jsonPath("$.error").value("Cannot connect to AI API."))
    }

    @Test
    fun `should return 404 when person not found`() {
        every { aiNarrativeService.generateNarrative(any(), any(), any(), any()) } throws PersonNotFoundException(PersonId(personId))

        mockMvc.perform(
            post("/api/v1/persons/$personId/ai-narrative")
                .with(authentication(authenticatedJwt()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"dateFrom":"2026-01-01","dateTo":"2026-06-30"}""")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should return 401 without authentication`() {
        mockMvc.perform(
            post("/api/v1/persons/$personId/ai-narrative")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"dateFrom":"2026-01-01","dateTo":"2026-06-30"}""")
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should return 400 when request body is missing dates`() {
        mockMvc.perform(
            post("/api/v1/persons/$personId/ai-narrative")
                .with(authentication(authenticatedJwt()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{}""")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should accept valid date range and pass to service`() {
        every { aiNarrativeService.generateNarrative(any(), any(), any(), any()) } returns AiNarrativeResult.Success(
            "Performance narrative content"
        )

        mockMvc.perform(
            post("/api/v1/persons/$personId/ai-narrative")
                .with(authentication(authenticatedJwt()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"dateFrom":"2026-01-01","dateTo":"2026-03-31"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.narrative").value("Performance narrative content"))
    }
}
