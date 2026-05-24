package com.peoplemanager.adapters.web

import com.peoplemanager.adapters.auth.SecurityConfig
import com.peoplemanager.adapters.auth.UserProvisioningJwtAuthenticationConverter
import com.peoplemanager.application.AiCoachingResult
import com.peoplemanager.application.AiCoachingService
import com.peoplemanager.application.UserProvisioningService
import com.peoplemanager.domain.UserId
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
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

@WebMvcTest(controllers = [AiCoachingController::class])
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
class AiCoachingControllerTest {

    @TestConfiguration
    class TestConfig {
        @Bean
        fun aiCoachingService(): AiCoachingService = mockk()

        @Bean
        fun userProvisioningService(): UserProvisioningService = mockk()
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var aiCoachingService: AiCoachingService

    private val userId = UserId(UUID.randomUUID())

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

    // ===== POST /api/v1/ai/refine-kudos =====

    @Test
    fun `refine-kudos should return refined text on success`() {
        every { aiCoachingService.refineKudos(any(), any()) } returns
            AiCoachingResult.Success("Refined kudos text using SBI framework")

        mockMvc.perform(
            post("/api/v1/ai/refine-kudos")
                .with(authentication(authenticatedJwt()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"draft": "Good job on the project"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.result").value("Refined kudos text using SBI framework"))
            .andExpect(jsonPath("$.error").doesNotExist())
    }

    @Test
    fun `refine-kudos should return error message when AI fails`() {
        every { aiCoachingService.refineKudos(any(), any()) } returns
            AiCoachingResult.Error("AI not configured")

        mockMvc.perform(
            post("/api/v1/ai/refine-kudos")
                .with(authentication(authenticatedJwt()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"draft": "Good job"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.result").doesNotExist())
            .andExpect(jsonPath("$.error").value("AI not configured"))
    }

    @Test
    fun `refine-kudos should return 400 when draft is blank`() {
        mockMvc.perform(
            post("/api/v1/ai/refine-kudos")
                .with(authentication(authenticatedJwt()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"draft": ""}""")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `refine-kudos should return 401 without authentication`() {
        mockMvc.perform(
            post("/api/v1/ai/refine-kudos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"draft": "Good job"}""")
        )
            .andExpect(status().isUnauthorized)
    }

    // ===== POST /api/v1/ai/optimize-pdp-goal =====

    @Test
    fun `optimize-pdp-goal should return optimized goal on success`() {
        every { aiCoachingService.optimizePdpGoal(any(), any(), any()) } returns
            AiCoachingResult.Success("Title: Deliver 3 presentations by Q3\nDescription: Present at team meetings")

        mockMvc.perform(
            post("/api/v1/ai/optimize-pdp-goal")
                .with(authentication(authenticatedJwt()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title": "Get better at speaking", "description": "Practice more"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.result").value("Title: Deliver 3 presentations by Q3\nDescription: Present at team meetings"))
            .andExpect(jsonPath("$.error").doesNotExist())
    }

    @Test
    fun `optimize-pdp-goal should work without description`() {
        every { aiCoachingService.optimizePdpGoal(any(), any(), any()) } returns
            AiCoachingResult.Success("Optimized goal")

        mockMvc.perform(
            post("/api/v1/ai/optimize-pdp-goal")
                .with(authentication(authenticatedJwt()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title": "Learn Kotlin"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.result").value("Optimized goal"))
    }

    @Test
    fun `optimize-pdp-goal should return error message when AI fails`() {
        every { aiCoachingService.optimizePdpGoal(any(), any(), any()) } returns
            AiCoachingResult.Error("Connection timeout")

        mockMvc.perform(
            post("/api/v1/ai/optimize-pdp-goal")
                .with(authentication(authenticatedJwt()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title": "My goal", "description": "desc"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.result").doesNotExist())
            .andExpect(jsonPath("$.error").value("Connection timeout"))
    }

    @Test
    fun `optimize-pdp-goal should return 400 when title is blank`() {
        mockMvc.perform(
            post("/api/v1/ai/optimize-pdp-goal")
                .with(authentication(authenticatedJwt()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title": "", "description": "desc"}""")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `optimize-pdp-goal should return 401 without authentication`() {
        mockMvc.perform(
            post("/api/v1/ai/optimize-pdp-goal")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title": "My goal"}""")
        )
            .andExpect(status().isUnauthorized)
    }
}
