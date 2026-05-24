package com.peoplemanager.adapters.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.peoplemanager.adapters.auth.SecurityConfig
import com.peoplemanager.adapters.auth.UserProvisioningJwtAuthenticationConverter
import com.peoplemanager.adapters.web.dto.CreatePdpGoalRequest
import com.peoplemanager.adapters.web.dto.CreatePdpUpdateRequest
import com.peoplemanager.adapters.web.dto.UpdatePdpGoalRequest
import com.peoplemanager.application.PdpGoalNotFoundException
import com.peoplemanager.application.PdpUpdateNotFoundException
import com.peoplemanager.application.PersonNotFoundException
import com.peoplemanager.application.UserProvisioningService
import com.peoplemanager.application.ports.PdpGoalCommandPort
import com.peoplemanager.application.ports.PdpGoalQueryPort
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
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
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

@WebMvcTest(controllers = [PdpGoalController::class])
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
class PdpGoalControllerTest {

    @TestConfiguration
    class TestConfig {
        @Bean
        fun pdpGoalCommandPort(): PdpGoalCommandPort = mockk()

        @Bean
        fun pdpGoalQueryPort(): PdpGoalQueryPort = mockk()

        @Bean
        fun userProvisioningService(): UserProvisioningService = mockk()
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var pdpGoalCommandPort: PdpGoalCommandPort

    @Autowired
    private lateinit var pdpGoalQueryPort: PdpGoalQueryPort

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
    private val goalId = PdpGoalId(UUID.randomUUID())

    private fun sampleGoal(
        id: PdpGoalId = goalId,
        title: String = "Improve public speaking",
        status: PdpGoalStatus = PdpGoalStatus.ACTIVE
    ) = PdpGoal(
        id = id,
        userId = userId,
        personId = personId,
        title = title,
        description = "Practice presentations monthly",
        targetDate = LocalDate.of(2026, 12, 31),
        status = status,
        createdAt = Instant.parse("2026-05-10T10:00:00Z"),
        updatedAt = Instant.parse("2026-05-10T10:00:00Z")
    )

    @Test
    fun `POST should create PDP goal and return 201`() {
        val goal = sampleGoal()
        every { pdpGoalCommandPort.createPdpGoal(any()) } returns goal

        mockMvc.perform(
            post("/api/v1/persons/${personId.value}/pdp-goals")
                .with(authentication(authenticatedJwt(userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    CreatePdpGoalRequest("Improve public speaking", "Practice presentations monthly", LocalDate.of(2026, 12, 31))
                ))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(goalId.value.toString()))
            .andExpect(jsonPath("$.title").value("Improve public speaking"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.targetDate").value("2026-12-31"))
    }

    @Test
    fun `POST should return 400 when title is blank`() {
        mockMvc.perform(
            post("/api/v1/persons/${personId.value}/pdp-goals")
                .with(authentication(authenticatedJwt(userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title": ""}""")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `POST should return 404 when person not found`() {
        every { pdpGoalCommandPort.createPdpGoal(any()) } throws PersonNotFoundException(personId)

        mockMvc.perform(
            post("/api/v1/persons/${personId.value}/pdp-goals")
                .with(authentication(authenticatedJwt(userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title": "Goal"}""")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `GET list should return paginated goals`() {
        val goals = listOf(sampleGoal())
        val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))
        every { pdpGoalQueryPort.listPdpGoalsByPerson(any()) } returns PageImpl(goals, pageable, 1)

        mockMvc.perform(
            get("/api/v1/persons/${personId.value}/pdp-goals")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].title").value("Improve public speaking"))
    }

    @Test
    fun `GET by id should return goal`() {
        every { pdpGoalQueryPort.getPdpGoal(any()) } returns sampleGoal()

        mockMvc.perform(
            get("/api/v1/persons/${personId.value}/pdp-goals/${goalId.value}")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.title").value("Improve public speaking"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
    }

    @Test
    fun `GET by id should return 404 when not found`() {
        every { pdpGoalQueryPort.getPdpGoal(any()) } throws PdpGoalNotFoundException(goalId)

        mockMvc.perform(
            get("/api/v1/persons/${personId.value}/pdp-goals/${goalId.value}")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `PUT should update goal`() {
        val updated = sampleGoal(title = "Updated title")
        every { pdpGoalCommandPort.updatePdpGoal(any()) } returns updated

        mockMvc.perform(
            put("/api/v1/persons/${personId.value}/pdp-goals/${goalId.value}")
                .with(authentication(authenticatedJwt(userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title": "Updated title"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.title").value("Updated title"))
    }

    @Test
    fun `POST achieve should transition to ACHIEVED`() {
        val achieved = sampleGoal(status = PdpGoalStatus.ACHIEVED)
        every { pdpGoalCommandPort.achievePdpGoal(any()) } returns achieved

        mockMvc.perform(
            post("/api/v1/persons/${personId.value}/pdp-goals/${goalId.value}/achieve")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("ACHIEVED"))
    }

    @Test
    fun `POST pause should transition to PAUSED`() {
        val paused = sampleGoal(status = PdpGoalStatus.PAUSED)
        every { pdpGoalCommandPort.pausePdpGoal(any()) } returns paused

        mockMvc.perform(
            post("/api/v1/persons/${personId.value}/pdp-goals/${goalId.value}/pause")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("PAUSED"))
    }

    @Test
    fun `POST drop should transition to DROPPED`() {
        val dropped = sampleGoal(status = PdpGoalStatus.DROPPED)
        every { pdpGoalCommandPort.dropPdpGoal(any()) } returns dropped

        mockMvc.perform(
            post("/api/v1/persons/${personId.value}/pdp-goals/${goalId.value}/drop")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("DROPPED"))
    }

    @Test
    fun `POST resume should transition to ACTIVE`() {
        val resumed = sampleGoal(status = PdpGoalStatus.ACTIVE)
        every { pdpGoalCommandPort.resumePdpGoal(any()) } returns resumed

        mockMvc.perform(
            post("/api/v1/persons/${personId.value}/pdp-goals/${goalId.value}/resume")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("ACTIVE"))
    }

    @Test
    fun `DELETE should return 204`() {
        every { pdpGoalCommandPort.deletePdpGoal(any()) } returns Unit

        mockMvc.perform(
            delete("/api/v1/persons/${personId.value}/pdp-goals/${goalId.value}")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isNoContent)
    }

    @Test
    fun `DELETE should return 404 when not found`() {
        every { pdpGoalCommandPort.deletePdpGoal(any()) } throws PdpGoalNotFoundException(goalId)

        mockMvc.perform(
            delete("/api/v1/persons/${personId.value}/pdp-goals/${goalId.value}")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `POST update should create progress update and return 201`() {
        val update = PdpUpdate(
            id = PdpUpdateId.generate(),
            goalId = goalId,
            userId = userId,
            textMarkdown = "Completed first milestone",
            sensitive = false,
            createdAt = Instant.parse("2026-05-10T12:00:00Z")
        )
        every { pdpGoalCommandPort.addPdpUpdate(any()) } returns update

        mockMvc.perform(
            post("/api/v1/persons/${personId.value}/pdp-goals/${goalId.value}/updates")
                .with(authentication(authenticatedJwt(userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"textMarkdown": "Completed first milestone"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.textMarkdown").value("Completed first milestone"))
            .andExpect(jsonPath("$.sensitive").value(false))
    }

    @Test
    fun `POST update should return 400 when text is blank`() {
        mockMvc.perform(
            post("/api/v1/persons/${personId.value}/pdp-goals/${goalId.value}/updates")
                .with(authentication(authenticatedJwt(userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"textMarkdown": ""}""")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `GET updates should return paginated updates`() {
        val updates = listOf(
            PdpUpdate(
                id = PdpUpdateId.generate(),
                goalId = goalId,
                userId = userId,
                textMarkdown = "Progress note",
                createdAt = Instant.parse("2026-05-10T12:00:00Z")
            )
        )
        val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))
        every { pdpGoalQueryPort.listPdpUpdatesByGoal(any()) } returns PageImpl(updates, pageable, 1)

        mockMvc.perform(
            get("/api/v1/persons/${personId.value}/pdp-goals/${goalId.value}/updates")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].textMarkdown").value("Progress note"))
    }

    @Test
    fun `DELETE update should return 204`() {
        every { pdpGoalCommandPort.deletePdpUpdate(any()) } returns Unit

        val updateId = UUID.randomUUID()
        mockMvc.perform(
            delete("/api/v1/persons/${personId.value}/pdp-goals/${goalId.value}/updates/$updateId")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isNoContent)
    }

    @Test
    fun `should return 401 for unauthenticated request`() {
        mockMvc.perform(
            get("/api/v1/persons/${personId.value}/pdp-goals")
        )
            .andExpect(status().isUnauthorized)
    }
}
