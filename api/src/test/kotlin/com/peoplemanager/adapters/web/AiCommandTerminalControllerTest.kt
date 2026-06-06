package com.peoplemanager.adapters.web

import com.peoplemanager.adapters.auth.SecurityConfig
import com.peoplemanager.adapters.auth.UserProvisioningJwtAuthenticationConverter
import com.peoplemanager.application.AiCommandTerminalService
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.Instant
import java.util.UUID

@WebMvcTest(controllers = [AiCommandTerminalController::class])
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
class AiCommandTerminalControllerTest {

    @TestConfiguration
    class TestConfig {
        @Bean
        fun aiCommandTerminalService(): AiCommandTerminalService = mockk()

        @Bean
        fun userProvisioningService(): UserProvisioningService = mockk()
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var aiCommandTerminalService: AiCommandTerminalService

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

    // ===== POST /api/v1/ai/command =====

    @Test
    fun `command should return parsed action on success`() {
        every { aiCommandTerminalService.parseCommand(any(), any()) } returns
            AiCommandTerminalService.CommandParseResult(
                intent = "create_action_item",
                targetPersonId = UUID.randomUUID().toString(),
                content = "Follow up on deadline",
                dueDate = "2026-06-13",
                meetingDate = null,
                tags = listOf("project"),
                sensitive = false,
                error = null
            )

        mockMvc.perform(
            post("/api/v1/ai/command")
                .with(authentication(authenticatedJwt()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"input": "Remind Alice to follow up on deadline by Friday"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.intent").value("create_action_item"))
            .andExpect(jsonPath("$.content").value("Follow up on deadline"))
            .andExpect(jsonPath("$.dueDate").value("2026-06-13"))
            .andExpect(jsonPath("$.tags[0]").value("project"))
            .andExpect(jsonPath("$.sensitive").value(false))
            .andExpect(jsonPath("$.error").doesNotExist())
    }

    @Test
    fun `command should return error message when AI fails`() {
        every { aiCommandTerminalService.parseCommand(any(), any()) } returns
            AiCommandTerminalService.CommandParseResult.error("AI not configured")

        mockMvc.perform(
            post("/api/v1/ai/command")
                .with(authentication(authenticatedJwt()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"input": "Create a note"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.error").value("AI not configured"))
            .andExpect(jsonPath("$.intent").doesNotExist())
    }

    @Test
    fun `command should return 400 when input is blank`() {
        mockMvc.perform(
            post("/api/v1/ai/command")
                .with(authentication(authenticatedJwt()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"input": ""}""")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `command should return 401 without authentication`() {
        mockMvc.perform(
            post("/api/v1/ai/command")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"input": "Create a note"}""")
        )
            .andExpect(status().isUnauthorized)
    }

    // ===== GET /api/v1/ai/command/directory =====

    @Test
    fun `directory should return person list`() {
        val personId = UUID.randomUUID().toString()
        every { aiCommandTerminalService.getPersonDirectory(any()) } returns listOf(
            AiCommandTerminalService.PersonDirectoryEntry(id = personId, preferredName = "Alice")
        )

        mockMvc.perform(
            get("/api/v1/ai/command/directory")
                .with(authentication(authenticatedJwt()))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(personId))
            .andExpect(jsonPath("$[0].preferredName").value("Alice"))
    }

    @Test
    fun `directory should return empty list when no persons`() {
        every { aiCommandTerminalService.getPersonDirectory(any()) } returns emptyList()

        mockMvc.perform(
            get("/api/v1/ai/command/directory")
                .with(authentication(authenticatedJwt()))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$").isEmpty)
    }

    @Test
    fun `directory should return 401 without authentication`() {
        mockMvc.perform(
            get("/api/v1/ai/command/directory")
        )
            .andExpect(status().isUnauthorized)
    }
}
