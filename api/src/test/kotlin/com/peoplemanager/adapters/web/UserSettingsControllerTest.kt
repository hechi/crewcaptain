package com.peoplemanager.adapters.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.peoplemanager.adapters.auth.SecurityConfig
import com.peoplemanager.adapters.auth.UserProvisioningJwtAuthenticationConverter
import com.peoplemanager.application.UpdateUserSettingsCommand
import com.peoplemanager.application.UserProvisioningService
import com.peoplemanager.application.UserSettingsService
import com.peoplemanager.domain.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.Instant
import java.util.UUID

@WebMvcTest(controllers = [UserSettingsController::class])
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
class UserSettingsControllerTest {

    @TestConfiguration
    class TestConfig {
        @Bean
        fun userSettingsService(): UserSettingsService = mockk()

        @Bean
        fun userProvisioningService(): UserProvisioningService = mockk()
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userSettingsService: UserSettingsService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private fun authenticatedJwt(userId: UserId = UserId(UUID.randomUUID())): JwtAuthenticationToken {
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
        token.details = userId
        return token
    }

    private val userId = UserId(UUID.randomUUID())

    @Test
    fun `should return 401 when not authenticated on GET`() {
        mockMvc.perform(get("/api/v1/settings"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should return 401 when not authenticated on PUT`() {
        mockMvc.perform(put("/api/v1/settings")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should return default settings when none exist`() {
        val defaultSettings = UserSettings.createDefault(userId)
        every { userSettingsService.getSettings(any()) } returns defaultSettings

        mockMvc.perform(
            get("/api/v1/settings")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.dueSoonDays").value(3))
            .andExpect(jsonPath("$.staleOneOnOneDays").value(14))
            .andExpect(jsonPath("$.anniversaryLookaheadDays").value(30))
            .andExpect(jsonPath("$.theme").value("DARK"))
            .andExpect(jsonPath("$.showAchievements").value(true))
            .andExpect(jsonPath("$.notifyActionItemOverdue").value(true))
            .andExpect(jsonPath("$.notifyActionItemDueSoon").value(true))
            .andExpect(jsonPath("$.notifyStaleOneOnOne").value(true))
            .andExpect(jsonPath("$.notifyUpcomingAnniversary").value(true))
    }

    @Test
    fun `should return custom settings`() {
        val settings = UserSettings(
            userId = userId,
            dueSoonDays = 7,
            staleOneOnOneDays = 21,
            anniversaryLookaheadDays = 60,
            theme = Theme.LIGHT,
            showAchievements = false,
            notifyActionItemOverdue = false,
            notifyActionItemDueSoon = true,
            notifyStaleOneOnOne = false,
            notifyUpcomingAnniversary = true
        )
        every { userSettingsService.getSettings(any()) } returns settings

        mockMvc.perform(
            get("/api/v1/settings")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.dueSoonDays").value(7))
            .andExpect(jsonPath("$.staleOneOnOneDays").value(21))
            .andExpect(jsonPath("$.anniversaryLookaheadDays").value(60))
            .andExpect(jsonPath("$.theme").value("LIGHT"))
            .andExpect(jsonPath("$.showAchievements").value(false))
            .andExpect(jsonPath("$.notifyActionItemOverdue").value(false))
            .andExpect(jsonPath("$.notifyActionItemDueSoon").value(true))
            .andExpect(jsonPath("$.notifyStaleOneOnOne").value(false))
            .andExpect(jsonPath("$.notifyUpcomingAnniversary").value(true))
    }

    @Test
    fun `should update settings successfully`() {
        val updatedSettings = UserSettings(
            userId = userId,
            dueSoonDays = 5,
            staleOneOnOneDays = 10,
            anniversaryLookaheadDays = 45,
            theme = Theme.LIGHT,
            showAchievements = false,
            notifyActionItemOverdue = false,
            notifyActionItemDueSoon = false,
            notifyStaleOneOnOne = true,
            notifyUpcomingAnniversary = true
        )
        every { userSettingsService.updateSettings(any(), any()) } returns updatedSettings

        val requestBody = """
        {
            "dueSoonDays": 5,
            "staleOneOnOneDays": 10,
            "anniversaryLookaheadDays": 45,
            "theme": "LIGHT",
            "showAchievements": false,
            "notifyActionItemOverdue": false,
            "notifyActionItemDueSoon": false,
            "notifyStaleOneOnOne": true,
            "notifyUpcomingAnniversary": true
        }
        """.trimIndent()

        mockMvc.perform(
            put("/api/v1/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.dueSoonDays").value(5))
            .andExpect(jsonPath("$.staleOneOnOneDays").value(10))
            .andExpect(jsonPath("$.anniversaryLookaheadDays").value(45))
            .andExpect(jsonPath("$.theme").value("LIGHT"))
            .andExpect(jsonPath("$.showAchievements").value(false))
    }

    @Test
    fun `should return 400 for invalid theme value`() {
        val requestBody = """
        {
            "dueSoonDays": 3,
            "staleOneOnOneDays": 14,
            "anniversaryLookaheadDays": 30,
            "theme": "INVALID_THEME",
            "showAchievements": true,
            "notifyActionItemOverdue": true,
            "notifyActionItemDueSoon": true,
            "notifyStaleOneOnOne": true,
            "notifyUpcomingAnniversary": true
        }
        """.trimIndent()

        mockMvc.perform(
            put("/api/v1/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should return 400 for dueSoonDays out of range`() {
        val requestBody = """
        {
            "dueSoonDays": 0,
            "staleOneOnOneDays": 14,
            "anniversaryLookaheadDays": 30,
            "theme": "DARK",
            "showAchievements": true,
            "notifyActionItemOverdue": true,
            "notifyActionItemDueSoon": true,
            "notifyStaleOneOnOne": true,
            "notifyUpcomingAnniversary": true
        }
        """.trimIndent()

        mockMvc.perform(
            put("/api/v1/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should return 400 for staleOneOnOneDays out of range`() {
        val requestBody = """
        {
            "dueSoonDays": 3,
            "staleOneOnOneDays": 91,
            "anniversaryLookaheadDays": 30,
            "theme": "DARK",
            "showAchievements": true,
            "notifyActionItemOverdue": true,
            "notifyActionItemDueSoon": true,
            "notifyStaleOneOnOne": true,
            "notifyUpcomingAnniversary": true
        }
        """.trimIndent()

        mockMvc.perform(
            put("/api/v1/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should return 400 for anniversaryLookaheadDays out of range`() {
        val requestBody = """
        {
            "dueSoonDays": 3,
            "staleOneOnOneDays": 14,
            "anniversaryLookaheadDays": 91,
            "theme": "DARK",
            "showAchievements": true,
            "notifyActionItemOverdue": true,
            "notifyActionItemDueSoon": true,
            "notifyStaleOneOnOne": true,
            "notifyUpcomingAnniversary": true
        }
        """.trimIndent()

        mockMvc.perform(
            put("/api/v1/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should accept case-insensitive theme values`() {
        val updatedSettings = UserSettings(
            userId = userId,
            theme = Theme.LIGHT
        )
        every { userSettingsService.updateSettings(any(), any()) } returns updatedSettings

        val requestBody = """
        {
            "dueSoonDays": 3,
            "staleOneOnOneDays": 14,
            "anniversaryLookaheadDays": 30,
            "theme": "light",
            "showAchievements": true,
            "notifyActionItemOverdue": true,
            "notifyActionItemDueSoon": true,
            "notifyStaleOneOnOne": true,
            "notifyUpcomingAnniversary": true
        }
        """.trimIndent()

        mockMvc.perform(
            put("/api/v1/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)

        verify {
            userSettingsService.updateSettings(any(), match { it.theme == Theme.LIGHT })
        }
    }

    @Test
    fun `should pass correct userId from authentication context`() {
        val settings = UserSettings.createDefault(userId)
        every { userSettingsService.getSettings(userId) } returns settings

        mockMvc.perform(
            get("/api/v1/settings")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)

        verify { userSettingsService.getSettings(userId) }
    }

    @Test
    fun `should pass correct command to service on update`() {
        val settings = UserSettings(userId = userId, dueSoonDays = 7, theme = Theme.LIGHT)
        every { userSettingsService.updateSettings(any(), any()) } returns settings

        val requestBody = """
        {
            "dueSoonDays": 7,
            "staleOneOnOneDays": 21,
            "anniversaryLookaheadDays": 60,
            "theme": "LIGHT",
            "showAchievements": false,
            "notifyActionItemOverdue": false,
            "notifyActionItemDueSoon": true,
            "notifyStaleOneOnOne": false,
            "notifyUpcomingAnniversary": true
        }
        """.trimIndent()

        mockMvc.perform(
            put("/api/v1/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)

        verify {
            userSettingsService.updateSettings(userId, match {
                it.dueSoonDays == 7 &&
                it.staleOneOnOneDays == 21 &&
                it.anniversaryLookaheadDays == 60 &&
                it.theme == Theme.LIGHT &&
                !it.showAchievements &&
                !it.notifyActionItemOverdue &&
                it.notifyActionItemDueSoon &&
                !it.notifyStaleOneOnOne &&
                it.notifyUpcomingAnniversary
            })
        }
    }
}
