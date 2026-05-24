package com.peoplemanager.adapters.web

import com.peoplemanager.adapters.auth.SecurityConfig
import com.peoplemanager.adapters.auth.UserProvisioningJwtAuthenticationConverter
import com.peoplemanager.application.UserProvisioningService
import com.peoplemanager.application.ports.DashboardQueryPort
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
import java.time.temporal.ChronoUnit
import java.util.UUID

@WebMvcTest(controllers = [DashboardController::class])
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
class DashboardControllerTest {

    @TestConfiguration
    class TestConfig {
        @Bean
        fun dashboardQueryPort(): DashboardQueryPort = mockk()

        @Bean
        fun userProvisioningService(): UserProvisioningService = mockk()
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var dashboardQueryPort: DashboardQueryPort

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
    private val personId = PersonId(UUID.randomUUID())

    @Test
    fun `should return 401 when not authenticated`() {
        mockMvc.perform(get("/api/v1/dashboard"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should return empty dashboard when no data`() {
        every { dashboardQueryPort.getDashboard(any()) } returns DashboardData(
            overdueActionItems = emptyList(),
            dueSoonActionItems = emptyList(),
            staleOneOnOnes = emptyList(),
            upcomingAnniversaries = emptyList()
        )

        mockMvc.perform(
            get("/api/v1/dashboard")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.overdueActionItems").isArray)
            .andExpect(jsonPath("$.overdueActionItems").isEmpty)
            .andExpect(jsonPath("$.dueSoonActionItems").isArray)
            .andExpect(jsonPath("$.dueSoonActionItems").isEmpty)
            .andExpect(jsonPath("$.staleOneOnOnes").isArray)
            .andExpect(jsonPath("$.staleOneOnOnes").isEmpty)
            .andExpect(jsonPath("$.upcomingAnniversaries").isArray)
            .andExpect(jsonPath("$.upcomingAnniversaries").isEmpty)
    }

    @Test
    fun `should return dashboard with overdue action items`() {
        val dashboardData = DashboardData(
            overdueActionItems = listOf(
                DashboardActionItem(
                    id = ActionItemId.generate(),
                    personId = personId,
                    personName = "Alice Smith",
                    title = "Review PR",
                    dueDate = LocalDate.now().minusDays(2),
                    ownerType = ActionItemOwnerType.PERSON
                )
            ),
            dueSoonActionItems = emptyList(),
            staleOneOnOnes = emptyList(),
            upcomingAnniversaries = emptyList()
        )

        every { dashboardQueryPort.getDashboard(any()) } returns dashboardData

        mockMvc.perform(
            get("/api/v1/dashboard")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.overdueActionItems[0].title").value("Review PR"))
            .andExpect(jsonPath("$.overdueActionItems[0].personName").value("Alice Smith"))
            .andExpect(jsonPath("$.overdueActionItems[0].ownerType").value("PERSON"))
            .andExpect(jsonPath("$.overdueActionItems[0].personId").value(personId.value.toString()))
    }

    @Test
    fun `should return dashboard with due soon action items`() {
        val dashboardData = DashboardData(
            overdueActionItems = emptyList(),
            dueSoonActionItems = listOf(
                DashboardActionItem(
                    id = ActionItemId.generate(),
                    personId = personId,
                    personName = "Bob Jones",
                    title = "Submit report",
                    dueDate = LocalDate.now().plusDays(2),
                    ownerType = ActionItemOwnerType.MANAGER
                )
            ),
            staleOneOnOnes = emptyList(),
            upcomingAnniversaries = emptyList()
        )

        every { dashboardQueryPort.getDashboard(any()) } returns dashboardData

        mockMvc.perform(
            get("/api/v1/dashboard")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.dueSoonActionItems[0].title").value("Submit report"))
            .andExpect(jsonPath("$.dueSoonActionItems[0].personName").value("Bob Jones"))
            .andExpect(jsonPath("$.dueSoonActionItems[0].ownerType").value("MANAGER"))
    }

    @Test
    fun `should return dashboard with stale 1-on-1 reminders`() {
        val lastMeeting = Instant.now().minus(15, ChronoUnit.DAYS)
        val dashboardData = DashboardData(
            overdueActionItems = emptyList(),
            dueSoonActionItems = emptyList(),
            staleOneOnOnes = listOf(
                StaleOneOnOneReminder(
                    personId = personId,
                    personName = "Alice Smith",
                    cadenceType = CadenceType.WEEKLY,
                    customIntervalDays = null,
                    lastMeetingDate = lastMeeting,
                    daysSinceLastMeeting = 15,
                    expectedIntervalDays = 7
                )
            ),
            upcomingAnniversaries = emptyList()
        )

        every { dashboardQueryPort.getDashboard(any()) } returns dashboardData

        mockMvc.perform(
            get("/api/v1/dashboard")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.staleOneOnOnes[0].personName").value("Alice Smith"))
            .andExpect(jsonPath("$.staleOneOnOnes[0].cadenceType").value("WEEKLY"))
            .andExpect(jsonPath("$.staleOneOnOnes[0].daysSinceLastMeeting").value(15))
            .andExpect(jsonPath("$.staleOneOnOnes[0].expectedIntervalDays").value(7))
    }

    @Test
    fun `should return dashboard with upcoming anniversaries`() {
        val dashboardData = DashboardData(
            overdueActionItems = emptyList(),
            dueSoonActionItems = emptyList(),
            staleOneOnOnes = emptyList(),
            upcomingAnniversaries = listOf(
                UpcomingAnniversary(
                    personId = personId,
                    personName = "Alice Smith",
                    startDate = LocalDate.of(2023, 5, 15),
                    anniversaryDate = LocalDate.now().plusDays(5),
                    yearsCompleted = 3,
                    daysUntil = 5
                )
            )
        )

        every { dashboardQueryPort.getDashboard(any()) } returns dashboardData

        mockMvc.perform(
            get("/api/v1/dashboard")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.upcomingAnniversaries[0].personName").value("Alice Smith"))
            .andExpect(jsonPath("$.upcomingAnniversaries[0].yearsCompleted").value(3))
            .andExpect(jsonPath("$.upcomingAnniversaries[0].daysUntil").value(5))
    }

    @Test
    fun `should pass custom dueSoonDays parameter`() {
        every { dashboardQueryPort.getDashboard(any()) } returns DashboardData(
            overdueActionItems = emptyList(),
            dueSoonActionItems = emptyList(),
            staleOneOnOnes = emptyList(),
            upcomingAnniversaries = emptyList()
        )

        mockMvc.perform(
            get("/api/v1/dashboard")
                .param("dueSoonDays", "7")
                .param("anniversaryLookaheadDays", "60")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)

        verify {
            dashboardQueryPort.getDashboard(match {
                it.dueSoonDays == 7 && it.anniversaryLookaheadDays == 60
            })
        }
    }

    @Test
    fun `should use default parameters when not specified`() {
        every { dashboardQueryPort.getDashboard(any()) } returns DashboardData(
            overdueActionItems = emptyList(),
            dueSoonActionItems = emptyList(),
            staleOneOnOnes = emptyList(),
            upcomingAnniversaries = emptyList()
        )

        mockMvc.perform(
            get("/api/v1/dashboard")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)

        verify {
            dashboardQueryPort.getDashboard(match {
                it.dueSoonDays == 3 && it.anniversaryLookaheadDays == 30
            })
        }
    }

    @Test
    fun `should return full dashboard with all sections populated`() {
        val dashboardData = DashboardData(
            overdueActionItems = listOf(
                DashboardActionItem(
                    id = ActionItemId.generate(),
                    personId = personId,
                    personName = "Alice",
                    title = "Overdue task",
                    dueDate = LocalDate.now().minusDays(1),
                    ownerType = ActionItemOwnerType.PERSON
                )
            ),
            dueSoonActionItems = listOf(
                DashboardActionItem(
                    id = ActionItemId.generate(),
                    personId = personId,
                    personName = "Bob",
                    title = "Due soon task",
                    dueDate = LocalDate.now().plusDays(2),
                    ownerType = ActionItemOwnerType.MANAGER
                )
            ),
            staleOneOnOnes = listOf(
                StaleOneOnOneReminder(
                    personId = personId,
                    personName = "Charlie",
                    cadenceType = CadenceType.BIWEEKLY,
                    customIntervalDays = null,
                    lastMeetingDate = Instant.now().minus(20, ChronoUnit.DAYS),
                    daysSinceLastMeeting = 20,
                    expectedIntervalDays = 14
                )
            ),
            upcomingAnniversaries = listOf(
                UpcomingAnniversary(
                    personId = personId,
                    personName = "Diana",
                    startDate = LocalDate.of(2022, 5, 15),
                    anniversaryDate = LocalDate.now().plusDays(5),
                    yearsCompleted = 4,
                    daysUntil = 5
                )
            )
        )

        every { dashboardQueryPort.getDashboard(any()) } returns dashboardData

        mockMvc.perform(
            get("/api/v1/dashboard")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.overdueActionItems").isNotEmpty)
            .andExpect(jsonPath("$.dueSoonActionItems").isNotEmpty)
            .andExpect(jsonPath("$.staleOneOnOnes").isNotEmpty)
            .andExpect(jsonPath("$.upcomingAnniversaries").isNotEmpty)
    }
}
