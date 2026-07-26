package com.peoplemanager.adapters.web

import com.peoplemanager.adapters.auth.SecurityConfig
import com.peoplemanager.adapters.auth.UserProvisioningJwtAuthenticationConverter
import com.peoplemanager.application.AiTriageHintService
import com.peoplemanager.application.UserProvisioningService
import com.peoplemanager.application.port.input.TriageCommandPort
import com.peoplemanager.application.port.input.TriageQueryPort
import com.peoplemanager.domain.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Nested
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@WebMvcTest(controllers = [TriageController::class])
@Import(SecurityConfig::class, UserProvisioningJwtAuthenticationConverter::class, GlobalExceptionHandler::class, TriageControllerTest.TestConfig::class)
@TestPropertySource(properties = [
    "spring.datasource.url=jdbc:h2:mem:test",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://auth.example.com",
    "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://auth.example.com/jwks"
])
class TriageControllerTest {

    @TestConfiguration
    class TestConfig {
        @Bean
        fun triageQueryPort(): TriageQueryPort = mockk()

        @Bean
        fun triageCommandPort(): TriageCommandPort = mockk()

        @Bean
        fun aiTriageHintService(): AiTriageHintService = mockk()

        @Bean
        fun userProvisioningService(): UserProvisioningService = mockk()
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var triageQueryPort: TriageQueryPort

    @Autowired
    private lateinit var triageCommandPort: TriageCommandPort

    @Autowired
    private lateinit var aiTriageHintService: AiTriageHintService

    private val userId = UserId(UUID.randomUUID())
    private val personId = PersonId(UUID.randomUUID())

    private fun authenticatedJwt(): JwtAuthenticationToken {
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

    @Nested
    inner class GetTriageQueueTests {

        @Test
        fun `should return triage queue`() {
            val items = listOf(
                TriageItem(
                    id = "ai-123",
                    type = TriageItemType.ACTION_ITEM_OVERDUE,
                    criticality = TriageCriticality.OVERDUE,
                    title = "Follow up",
                    personId = personId,
                    personName = "Alice",
                    dueDate = LocalDate.of(2026, 5, 1),
                    daysOverdue = 5,
                    ownerType = ActionItemOwnerType.MANAGER,
                    sourceActionItemId = ActionItemId.generate()
                )
            )
            every { triageQueryPort.getTriageQueue(any()) } returns items

            mockMvc.perform(
                get("/api/v1/triage")
                    .with(authentication(authenticatedJwt()))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.items[0].title").value("Follow up"))
                .andExpect(jsonPath("$.items[0].type").value("ACTION_ITEM_OVERDUE"))
                .andExpect(jsonPath("$.items[0].criticality").value("OVERDUE"))
                .andExpect(jsonPath("$.items[0].personName").value("Alice"))
                .andExpect(jsonPath("$.items[0].daysOverdue").value(5))
        }

        @Test
        fun `should return empty triage queue`() {
            every { triageQueryPort.getTriageQueue(any()) } returns emptyList()

            mockMvc.perform(
                get("/api/v1/triage")
                    .with(authentication(authenticatedJwt()))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.totalCount").value(0))
                .andExpect(jsonPath("$.items").isEmpty)
        }

        @Test
        fun `should pass filter parameters to query`() {
            every { triageQueryPort.getTriageQueue(any()) } returns emptyList()

            mockMvc.perform(
                get("/api/v1/triage")
                    .param("type", "ACTION_ITEM_OVERDUE")
                    .param("scope", "MINE")
                    .param("personId", personId.value.toString())
                    .with(authentication(authenticatedJwt()))
            )
                .andExpect(status().isOk)

            verify {
                triageQueryPort.getTriageQueue(match {
                    it.itemType == TriageItemType.ACTION_ITEM_OVERDUE &&
                    it.ownerScope == com.peoplemanager.application.queries.OwnerScope.MINE &&
                    it.personId == personId
                })
            }
        }

        @Test
        fun `should return 401 without authentication`() {
            mockMvc.perform(get("/api/v1/triage"))
                .andExpect(status().isUnauthorized)
        }
    }

    @Nested
    inner class HintTests {

        @Test
        fun `should return AI hint for triage item`() {
            val items = listOf(
                TriageItem(
                    id = "ai-123",
                    type = TriageItemType.ACTION_ITEM_OVERDUE,
                    criticality = TriageCriticality.OVERDUE,
                    title = "Follow up",
                    personId = personId,
                    personName = "Alice"
                )
            )
            every { triageQueryPort.getTriageQueue(any()) } returns items
            every { aiTriageHintService.generateHint(any(), any()) } returns
                AiTriageHintService.TriageHintResult(hint = "Set due date to Friday")

            mockMvc.perform(
                post("/api/v1/triage/items/ai-123/hint")
                    .with(authentication(authenticatedJwt()))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.hint").value("Set due date to Friday"))
        }

        @Test
        fun `should return 404 for unknown triage item`() {
            every { triageQueryPort.getTriageQueue(any()) } returns emptyList()

            mockMvc.perform(
                post("/api/v1/triage/items/unknown-id/hint")
                    .with(authentication(authenticatedJwt()))
            )
                .andExpect(status().isNotFound)
        }
    }

    @Nested
    inner class SnoozeTests {

        @Test
        fun `should snooze action item with days parameter`() {
            val actionItemId = ActionItemId.generate()
            val snoozedItem = ActionItem(
                id = actionItemId, userId = userId, personId = personId,
                title = "Snoozed", status = ActionItemStatus.OPEN,
                snoozedUntil = Instant.now().plusSeconds(259200)
            )
            every { triageCommandPort.snoozeActionItem(any()) } returns snoozedItem

            mockMvc.perform(
                post("/api/v1/triage/persons/${personId.value}/action-items/${actionItemId.value}/snooze")
                    .with(authentication(authenticatedJwt()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"days": 3}""")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.snoozedUntil").exists())
        }
    }
}
