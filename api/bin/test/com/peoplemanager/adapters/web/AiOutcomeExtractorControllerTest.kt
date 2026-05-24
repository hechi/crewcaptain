package com.peoplemanager.adapters.web

import com.peoplemanager.adapters.auth.SecurityConfig
import com.peoplemanager.adapters.auth.UserProvisioningJwtAuthenticationConverter
import com.peoplemanager.application.AiExtractionResult
import com.peoplemanager.application.AiOutcomeExtractorService
import com.peoplemanager.application.ApplyOutcomesResult
import com.peoplemanager.application.ExtractedActionItem
import com.peoplemanager.application.PersonNotFoundException
import com.peoplemanager.application.OneOnOneEntryNotFoundException
import com.peoplemanager.application.UserProvisioningService
import com.peoplemanager.domain.OneOnOneEntryId
import com.peoplemanager.domain.PersonId
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

@WebMvcTest(controllers = [AiOutcomeExtractorController::class])
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
class AiOutcomeExtractorControllerTest {

    @TestConfiguration
    class TestConfig {
        @Bean
        fun aiOutcomeExtractorService(): AiOutcomeExtractorService = mockk()

        @Bean
        fun userProvisioningService(): UserProvisioningService = mockk()
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var aiOutcomeExtractorService: AiOutcomeExtractorService

    private val userId = UserId(UUID.randomUUID())
    private val personId = UUID.randomUUID()
    private val entryId = UUID.randomUUID()

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

    // ===== POST /api/v1/persons/{personId}/one-on-one-entries/{entryId}/extract-outcomes =====

    @Test
    fun `extract-outcomes should return action items and decisions on success`() {
        every { aiOutcomeExtractorService.extractOutcomes(any(), any(), any()) } returns
            AiExtractionResult.Success(
                actionItems = listOf(
                    ExtractedActionItem("Finish docs", "PERSON", 7),
                    ExtractedActionItem("Set up access", "MANAGER", 3)
                ),
                decisions = listOf("Move to biweekly cadence")
            )

        mockMvc.perform(
            post("/api/v1/persons/$personId/one-on-one-entries/$entryId/extract-outcomes")
                .with(authentication(authenticatedJwt()))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.actionItems").isArray)
            .andExpect(jsonPath("$.actionItems.length()").value(2))
            .andExpect(jsonPath("$.actionItems[0].title").value("Finish docs"))
            .andExpect(jsonPath("$.actionItems[0].ownerType").value("PERSON"))
            .andExpect(jsonPath("$.actionItems[0].suggestedDaysToDue").value(7))
            .andExpect(jsonPath("$.actionItems[1].title").value("Set up access"))
            .andExpect(jsonPath("$.actionItems[1].ownerType").value("MANAGER"))
            .andExpect(jsonPath("$.decisions").isArray)
            .andExpect(jsonPath("$.decisions.length()").value(1))
            .andExpect(jsonPath("$.decisions[0]").value("Move to biweekly cadence"))
            .andExpect(jsonPath("$.error").doesNotExist())
    }

    @Test
    fun `extract-outcomes should return error message when extraction fails`() {
        every { aiOutcomeExtractorService.extractOutcomes(any(), any(), any()) } returns
            AiExtractionResult.Error("AI not configured")

        mockMvc.perform(
            post("/api/v1/persons/$personId/one-on-one-entries/$entryId/extract-outcomes")
                .with(authentication(authenticatedJwt()))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.actionItems").isArray)
            .andExpect(jsonPath("$.actionItems.length()").value(0))
            .andExpect(jsonPath("$.decisions").isArray)
            .andExpect(jsonPath("$.decisions.length()").value(0))
            .andExpect(jsonPath("$.error").value("AI not configured"))
    }

    @Test
    fun `extract-outcomes should return 404 when person not found`() {
        every { aiOutcomeExtractorService.extractOutcomes(any(), any(), any()) } throws
            PersonNotFoundException(PersonId(personId))

        mockMvc.perform(
            post("/api/v1/persons/$personId/one-on-one-entries/$entryId/extract-outcomes")
                .with(authentication(authenticatedJwt()))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `extract-outcomes should return 404 when entry not found`() {
        every { aiOutcomeExtractorService.extractOutcomes(any(), any(), any()) } throws
            OneOnOneEntryNotFoundException(OneOnOneEntryId(entryId))

        mockMvc.perform(
            post("/api/v1/persons/$personId/one-on-one-entries/$entryId/extract-outcomes")
                .with(authentication(authenticatedJwt()))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `extract-outcomes should return 401 without authentication`() {
        mockMvc.perform(
            post("/api/v1/persons/$personId/one-on-one-entries/$entryId/extract-outcomes")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isUnauthorized)
    }

    // ===== POST /api/v1/persons/{personId}/one-on-one-entries/{entryId}/apply-outcomes =====

    @Test
    fun `apply-outcomes should create action items and append decisions`() {
        every { aiOutcomeExtractorService.applyOutcomes(any(), any(), any(), any()) } returns
            ApplyOutcomesResult(actionItemsCreated = 2, decisionsAppended = 1)

        mockMvc.perform(
            post("/api/v1/persons/$personId/one-on-one-entries/$entryId/apply-outcomes")
                .with(authentication(authenticatedJwt()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "actionItems": [
                            {"title": "Finish docs", "ownerType": "PERSON", "suggestedDaysToDue": 7},
                            {"title": "Set up access", "ownerType": "MANAGER", "suggestedDaysToDue": 3}
                        ],
                        "decisions": ["Move to biweekly cadence"]
                    }
                """.trimIndent())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.actionItemsCreated").value(2))
            .andExpect(jsonPath("$.decisionsAppended").value(1))
    }

    @Test
    fun `apply-outcomes should work with empty decisions`() {
        every { aiOutcomeExtractorService.applyOutcomes(any(), any(), any(), any()) } returns
            ApplyOutcomesResult(actionItemsCreated = 1, decisionsAppended = 0)

        mockMvc.perform(
            post("/api/v1/persons/$personId/one-on-one-entries/$entryId/apply-outcomes")
                .with(authentication(authenticatedJwt()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "actionItems": [
                            {"title": "Do something", "ownerType": "MANAGER"}
                        ],
                        "decisions": []
                    }
                """.trimIndent())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.actionItemsCreated").value(1))
            .andExpect(jsonPath("$.decisionsAppended").value(0))
    }

    @Test
    fun `apply-outcomes should return 404 when person not found`() {
        every { aiOutcomeExtractorService.applyOutcomes(any(), any(), any(), any()) } throws
            PersonNotFoundException(PersonId(personId))

        mockMvc.perform(
            post("/api/v1/persons/$personId/one-on-one-entries/$entryId/apply-outcomes")
                .with(authentication(authenticatedJwt()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"actionItems": [], "decisions": []}""")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `apply-outcomes should return 401 without authentication`() {
        mockMvc.perform(
            post("/api/v1/persons/$personId/one-on-one-entries/$entryId/apply-outcomes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"actionItems": [], "decisions": []}""")
        )
            .andExpect(status().isUnauthorized)
    }
}
