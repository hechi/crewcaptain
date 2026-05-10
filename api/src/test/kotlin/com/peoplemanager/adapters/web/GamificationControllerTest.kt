package com.peoplemanager.adapters.web

import com.peoplemanager.adapters.auth.SecurityConfig
import com.peoplemanager.adapters.auth.UserProvisioningJwtAuthenticationConverter
import com.peoplemanager.application.UserProvisioningService
import com.peoplemanager.application.ports.GamificationQueryPort
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
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@WebMvcTest(controllers = [GamificationController::class])
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
class GamificationControllerTest {

    @TestConfiguration
    class TestConfig {
        @Bean
        fun gamificationQueryPort(): GamificationQueryPort = mockk()

        @Bean
        fun userProvisioningService(): UserProvisioningService = mockk()
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var gamificationQueryPort: GamificationQueryPort

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
    fun `should return 401 when not authenticated`() {
        mockMvc.perform(get("/api/v1/gamification/stats"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should return gamification stats for authenticated user`() {
        val stats = GamificationStats(
            streaks = StreakData(currentStreak = 5, longestStreak = 10, totalOneOnOnesHeld = 25),
            achievements = listOf(
                Achievement(AchievementType.FIRST_ONE_ON_ONE, LocalDate.of(2026, 1, 15), "First 1:1", "Held your first 1:1 meeting"),
                Achievement(AchievementType.TEN_ONE_ON_ONES, LocalDate.of(2026, 3, 1), "10 1:1s", "Held 10 one-on-one meetings")
            ),
            activityHeatmap = listOf(
                ActivityDay(LocalDate.of(2026, 5, 1), 2),
                ActivityDay(LocalDate.of(2026, 5, 2), 0)
            ),
            pdpProgress = PdpProgressSummary(totalActive = 3, totalAchieved = 2, totalPaused = 1, totalDropped = 0)
        )

        every { gamificationQueryPort.getGamificationStats(any()) } returns stats

        mockMvc.perform(
            get("/api/v1/gamification/stats")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.streaks.currentStreak").value(5))
            .andExpect(jsonPath("$.streaks.longestStreak").value(10))
            .andExpect(jsonPath("$.streaks.totalOneOnOnesHeld").value(25))
            .andExpect(jsonPath("$.achievements").isArray)
            .andExpect(jsonPath("$.achievements[0].type").value("FIRST_ONE_ON_ONE"))
            .andExpect(jsonPath("$.achievements[0].label").value("First 1:1"))
            .andExpect(jsonPath("$.achievements[1].type").value("TEN_ONE_ON_ONES"))
            .andExpect(jsonPath("$.activityHeatmap").isArray)
            .andExpect(jsonPath("$.activityHeatmap[0].date").value("2026-05-01"))
            .andExpect(jsonPath("$.activityHeatmap[0].count").value(2))
            .andExpect(jsonPath("$.pdpProgress.totalActive").value(3))
            .andExpect(jsonPath("$.pdpProgress.totalAchieved").value(2))
            .andExpect(jsonPath("$.pdpProgress.completionPercentage").value(33))
    }

    @Test
    fun `should use default heatmapDays of 90`() {
        val stats = GamificationStats(
            streaks = StreakData(currentStreak = 0, longestStreak = 0, totalOneOnOnesHeld = 0),
            achievements = emptyList(),
            activityHeatmap = emptyList(),
            pdpProgress = PdpProgressSummary(totalActive = 0, totalAchieved = 0, totalPaused = 0, totalDropped = 0)
        )

        every { gamificationQueryPort.getGamificationStats(any()) } returns stats

        mockMvc.perform(
            get("/api/v1/gamification/stats")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)

        verify {
            gamificationQueryPort.getGamificationStats(match { it.heatmapDays == 90 })
        }
    }

    @Test
    fun `should accept custom heatmapDays parameter`() {
        val stats = GamificationStats(
            streaks = StreakData(currentStreak = 0, longestStreak = 0, totalOneOnOnesHeld = 0),
            achievements = emptyList(),
            activityHeatmap = emptyList(),
            pdpProgress = PdpProgressSummary(totalActive = 0, totalAchieved = 0, totalPaused = 0, totalDropped = 0)
        )

        every { gamificationQueryPort.getGamificationStats(any()) } returns stats

        mockMvc.perform(
            get("/api/v1/gamification/stats")
                .param("heatmapDays", "30")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)

        verify {
            gamificationQueryPort.getGamificationStats(match { it.heatmapDays == 30 })
        }
    }

    @Test
    fun `should return empty achievements when no milestones reached`() {
        val stats = GamificationStats(
            streaks = StreakData(currentStreak = 0, longestStreak = 0, totalOneOnOnesHeld = 0),
            achievements = emptyList(),
            activityHeatmap = emptyList(),
            pdpProgress = PdpProgressSummary(totalActive = 0, totalAchieved = 0, totalPaused = 0, totalDropped = 0)
        )

        every { gamificationQueryPort.getGamificationStats(any()) } returns stats

        mockMvc.perform(
            get("/api/v1/gamification/stats")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.achievements").isEmpty)
            .andExpect(jsonPath("$.streaks.currentStreak").value(0))
    }

    @Test
    fun `should return PDP progress with completion percentage`() {
        val stats = GamificationStats(
            streaks = StreakData(currentStreak = 0, longestStreak = 0, totalOneOnOnesHeld = 0),
            achievements = emptyList(),
            activityHeatmap = emptyList(),
            pdpProgress = PdpProgressSummary(totalActive = 5, totalAchieved = 5, totalPaused = 0, totalDropped = 0)
        )

        every { gamificationQueryPort.getGamificationStats(any()) } returns stats

        mockMvc.perform(
            get("/api/v1/gamification/stats")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.pdpProgress.completionPercentage").value(50))
    }

    @Test
    fun `should scope stats by authenticated user`() {
        val stats = GamificationStats(
            streaks = StreakData(currentStreak = 0, longestStreak = 0, totalOneOnOnesHeld = 0),
            achievements = emptyList(),
            activityHeatmap = emptyList(),
            pdpProgress = PdpProgressSummary(totalActive = 0, totalAchieved = 0, totalPaused = 0, totalDropped = 0)
        )

        every { gamificationQueryPort.getGamificationStats(any()) } returns stats

        mockMvc.perform(
            get("/api/v1/gamification/stats")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)

        verify {
            gamificationQueryPort.getGamificationStats(match { it.userId == userId })
        }
    }

    @Test
    fun `should return activity heatmap data`() {
        val today = LocalDate.now()
        val stats = GamificationStats(
            streaks = StreakData(currentStreak = 0, longestStreak = 0, totalOneOnOnesHeld = 0),
            achievements = emptyList(),
            activityHeatmap = listOf(
                ActivityDay(today, 3),
                ActivityDay(today.minusDays(1), 1),
                ActivityDay(today.minusDays(2), 0)
            ),
            pdpProgress = PdpProgressSummary(totalActive = 0, totalAchieved = 0, totalPaused = 0, totalDropped = 0)
        )

        every { gamificationQueryPort.getGamificationStats(any()) } returns stats

        mockMvc.perform(
            get("/api/v1/gamification/stats")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.activityHeatmap").isArray)
            .andExpect(jsonPath("$.activityHeatmap[0].count").value(3))
            .andExpect(jsonPath("$.activityHeatmap[1].count").value(1))
            .andExpect(jsonPath("$.activityHeatmap[2].count").value(0))
    }
}
