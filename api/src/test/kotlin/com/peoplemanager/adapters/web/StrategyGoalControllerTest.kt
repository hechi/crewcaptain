package com.peoplemanager.adapters.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.peoplemanager.adapters.auth.SecurityConfig
import com.peoplemanager.adapters.auth.UserProvisioningJwtAuthenticationConverter
import com.peoplemanager.adapters.web.dto.CreateStrategyGoalRequest
import com.peoplemanager.adapters.web.dto.LinkPdpGoalRequest
import com.peoplemanager.adapters.web.dto.UpdateStrategyGoalRequest
import com.peoplemanager.application.AiLinkDiscoveryService
import com.peoplemanager.application.StrategyGoalLinkService
import com.peoplemanager.application.StrategyGoalNotFoundException
import com.peoplemanager.application.StrategyGoalService
import com.peoplemanager.domain.*
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
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

@WebMvcTest(controllers = [StrategyGoalController::class])
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
class StrategyGoalControllerTest {

    @TestConfiguration
    class TestConfig {
        @Bean fun strategyGoalService(): StrategyGoalService = mockk()
        @Bean fun strategyGoalLinkService(): StrategyGoalLinkService = mockk()
        @Bean fun aiLinkDiscoveryService(): AiLinkDiscoveryService = mockk()
        // UserProvisioningService bean required by SecurityConfig
        @Bean fun userProvisioningService(): com.peoplemanager.application.UserProvisioningService = mockk()
    }

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var objectMapper: ObjectMapper
    @Autowired private lateinit var strategyGoalService: StrategyGoalService
    @Autowired private lateinit var strategyGoalLinkService: StrategyGoalLinkService
    @Autowired private lateinit var aiLinkDiscoveryService: AiLinkDiscoveryService

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
    private val otherUserId = UserId(UUID.randomUUID())
    private val goalId = StrategyGoalId(UUID.randomUUID())

    private fun sampleGoal(
        id: StrategyGoalId = goalId,
        uid: UserId = userId,
        title: String = "Increase team public speaking",
        status: StrategyGoalStatus = StrategyGoalStatus.ACTIVE,
        sensitive: Boolean = false
    ) = StrategyGoal(
        id = id,
        userId = uid,
        title = title,
        description = "Monthly lightning talks",
        targetDate = LocalDate.of(2026, 12, 31),
        status = status,
        sensitive = sensitive,
        createdAt = Instant.parse("2026-05-20T00:00:00Z"),
        updatedAt = Instant.parse("2026-05-20T00:00:00Z")
    )

    // ================= Authentication (401) tests =================
    @Test
    fun `unauthenticated requests return 401 for all strategy goal endpoints`() {
        val anyId = UUID.randomUUID()
        val personId = UUID.randomUUID()
        val pdpGoalId = UUID.randomUUID()

        // Core CRUD + transitions
        mockMvc.perform(post("/api/v1/strategy-goals").contentType(MediaType.APPLICATION_JSON).content("""{"title":"X"}"""))
            .andExpect(status().isUnauthorized)
        mockMvc.perform(get("/api/v1/strategy-goals"))
            .andExpect(status().isUnauthorized)
        mockMvc.perform(get("/api/v1/strategy-goals/$anyId"))
            .andExpect(status().isUnauthorized)
        mockMvc.perform(put("/api/v1/strategy-goals/$anyId").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnauthorized)
        mockMvc.perform(delete("/api/v1/strategy-goals/$anyId"))
            .andExpect(status().isUnauthorized)
        mockMvc.perform(post("/api/v1/strategy-goals/$anyId/achieve"))
            .andExpect(status().isUnauthorized)
        mockMvc.perform(post("/api/v1/strategy-goals/$anyId/drop"))
            .andExpect(status().isUnauthorized)

        // Links
        mockMvc.perform(post("/api/v1/strategy-goals/$anyId/links").contentType(MediaType.APPLICATION_JSON).content("""{"pdpGoalId":"$pdpGoalId","personId":"$personId"}"""))
            .andExpect(status().isUnauthorized)
        mockMvc.perform(get("/api/v1/strategy-goals/$anyId/links"))
            .andExpect(status().isUnauthorized)
        mockMvc.perform(delete("/api/v1/strategy-goals/$anyId/links/$pdpGoalId"))
            .andExpect(status().isUnauthorized)

        // Alignment & gap analysis
        mockMvc.perform(get("/api/v1/strategy-goals/$anyId/alignment"))
            .andExpect(status().isUnauthorized)
        mockMvc.perform(get("/api/v1/strategy-goals/alignment"))
            .andExpect(status().isUnauthorized)
        mockMvc.perform(get("/api/v1/strategy-goals/gap-analysis"))
            .andExpect(status().isUnauthorized)

        // Reverse lookup and AI suggestions
        mockMvc.perform(get("/api/v1/persons/$personId/pdp-goals/$pdpGoalId/strategy-goals"))
            .andExpect(status().isUnauthorized)
        mockMvc.perform(get("/api/v1/strategy-goals/ai-suggestions"))
            .andExpect(status().isUnauthorized)
    }

    // ================= Security (404 cross-manager) =================
    @Test
    fun `GET strategy-goals by id returns 404 when goal belongs to different user`() {
        every { strategyGoalService.getStrategyGoal(any()) } throws StrategyGoalNotFoundException(goalId)

        mockMvc.perform(
            get("/api/v1/strategy-goals/${goalId.value}")
                .with(authentication(authenticatedJwt(otherUserId)))
        ).andExpect(status().isNotFound)
    }

    @Test
    fun `PUT strategy-goals returns 404 when goal belongs to different user`() {
        every { strategyGoalService.updateStrategyGoal(any()) } throws StrategyGoalNotFoundException(goalId)

        mockMvc.perform(
            put("/api/v1/strategy-goals/${goalId.value}")
                .with(authentication(authenticatedJwt(otherUserId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(UpdateStrategyGoalRequest(title = "Updated")))
        ).andExpect(status().isNotFound)
    }

    @Test
    fun `DELETE strategy-goals returns 404 when goal belongs to different user`() {
        every { strategyGoalService.deleteStrategyGoal(any()) } throws StrategyGoalNotFoundException(goalId)

        mockMvc.perform(
            delete("/api/v1/strategy-goals/${goalId.value}")
                .with(authentication(authenticatedJwt(otherUserId)))
        ).andExpect(status().isNotFound)
    }

    // ================= userId scoping =================
    @Test
    fun `list endpoint returns only authenticated user's strategy goals`() {
        val myGoal = sampleGoal(title = "My Goal A")
        val page = PageImpl(listOf(myGoal), PageRequest.of(0, 20), 1)
        every { strategyGoalService.listStrategyGoals(any()) } returns page

        mockMvc.perform(
            get("/api/v1/strategy-goals")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].title").value("My Goal A"))
    }

    @Test
    fun `cannot access another user's strategy goal (404)`() {
        every { strategyGoalService.getStrategyGoal(any()) } throws StrategyGoalNotFoundException(goalId)

        mockMvc.perform(
            get("/api/v1/strategy-goals/${goalId.value}")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isNotFound)
    }

    // ================= Happy paths =================
    @Test
    fun `create strategy goal returns 201`() {
        val created = sampleGoal()
        every { strategyGoalService.createStrategyGoal(any()) } returns created

        val req = CreateStrategyGoalRequest(
            title = "Increase team public speaking",
            description = "Monthly lightning talks",
            targetDate = LocalDate.of(2026, 12, 31),
            sensitive = false
        )

        mockMvc.perform(
            post("/api/v1/strategy-goals")
                .with(authentication(authenticatedJwt(userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(created.id.value.toString()))
            .andExpect(jsonPath("$.title").value("Increase team public speaking"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.targetDate").value("2026-12-31"))
    }

    @Test
    fun `get strategy goal returns 200 with correct data`() {
        val goal = sampleGoal()
        every { strategyGoalService.getStrategyGoal(any()) } returns goal

        mockMvc.perform(
            get("/api/v1/strategy-goals/${goalId.value}")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(goalId.value.toString()))
            .andExpect(jsonPath("$.title").value(goal.title))
            .andExpect(jsonPath("$.description").value("Monthly lightning talks"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
    }

    @Test
    fun `list strategy goals returns paginated results`() {
        val goals = listOf(sampleGoal(title = "A"), sampleGoal(id = StrategyGoalId(UUID.randomUUID()), title = "B"))
        val page = PageImpl(goals, PageRequest.of(0, 20), 2)
        every { strategyGoalService.listStrategyGoals(any()) } returns page

        mockMvc.perform(
            get("/api/v1/strategy-goals")
                .with(authentication(authenticatedJwt(userId)))
                .param("page", "0")
                .param("size", "20")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.totalPages").value(1))
    }

    // ----- Additional coverage for links/alignment basic happy paths -----
    @Test
    fun `link and list linked PDP goals endpoints are reachable when authenticated`() {
        val sgId = goalId.value
        every { strategyGoalLinkService.linkPdpGoal(any()) } returns Unit
        every { strategyGoalLinkService.getLinkedPdpGoals(any(), any()) } returns emptyList()

        val linkReq = LinkPdpGoalRequest(pdpGoalId = UUID.randomUUID().toString(), personId = UUID.randomUUID().toString())

        mockMvc.perform(
            post("/api/v1/strategy-goals/$sgId/links")
                .with(authentication(authenticatedJwt(userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(linkReq))
        ).andExpect(status().isCreated)

        mockMvc.perform(
            get("/api/v1/strategy-goals/$sgId/links")
                .with(authentication(authenticatedJwt(userId)))
        ).andExpect(status().isOk)
    }

    @Test
    fun `alignment and gap analysis endpoints return 200`() {
        every { strategyGoalLinkService.getAlignmentScore(any(), any()) } returns 
            StrategyGoalLinkService.AlignmentScore(goalId, "G", 10, 3, 30)
        every { strategyGoalLinkService.getAllAlignmentScores(any()) } returns emptyList()
        every { strategyGoalLinkService.getGapAnalysis(any()) } returns 
            StrategyGoalLinkService.GapAnalysis(emptyList(), emptyList())

        mockMvc.perform(
            get("/api/v1/strategy-goals/${goalId.value}/alignment")
                .with(authentication(authenticatedJwt(userId)))
        ).andExpect(status().isOk)

        mockMvc.perform(
            get("/api/v1/strategy-goals/alignment")
                .with(authentication(authenticatedJwt(userId)))
        ).andExpect(status().isOk)

        mockMvc.perform(
            get("/api/v1/strategy-goals/gap-analysis")
                .with(authentication(authenticatedJwt(userId)))
        ).andExpect(status().isOk)
    }

    @Test
    fun `reverse lookup and AI suggestions endpoints return 200`() {
        every { strategyGoalLinkService.getStrategyGoalsByPdpGoal(any(), any(), any()) } returns emptyList()
        every { aiLinkDiscoveryService.findLinkSuggestions(any()) } returns emptyList()

        val personId = UUID.randomUUID(); val pdpGoalId = UUID.randomUUID()
        mockMvc.perform(
            get("/api/v1/persons/$personId/pdp-goals/$pdpGoalId/strategy-goals")
                .with(authentication(authenticatedJwt(userId)))
        ).andExpect(status().isOk)

        mockMvc.perform(
            get("/api/v1/strategy-goals/ai-suggestions")
                .with(authentication(authenticatedJwt(userId)))
        ).andExpect(status().isOk)
    }
}
