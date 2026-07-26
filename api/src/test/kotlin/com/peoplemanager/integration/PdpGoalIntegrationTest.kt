package com.peoplemanager.integration

import tools.jackson.databind.ObjectMapper
import com.peoplemanager.adapters.persistence.JpaUserRepositoryAdapter
import com.peoplemanager.domain.User
import com.peoplemanager.domain.UserId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Instant

/**
 * Full-stack integration test for PDP Goals.
 *
 * Verifies:
 * - CRUD operations work end-to-end with real PostgreSQL
 * - userId scoping is enforced (data isolation between managers)
 * - Status transitions (ACTIVE → ACHIEVED/PAUSED/DROPPED, PAUSED → ACTIVE)
 * - Progress updates CRUD
 * - Cross-user access returns 404
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class PdpGoalIntegrationTest {

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16")

        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
            registry.add("spring.flyway.enabled") { "true" }
            registry.add("spring.jpa.hibernate.ddl-auto") { "validate" }
            registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri") { "http://localhost:9000" }
            registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri") { "http://localhost:9000/jwks" }
        }
    }

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var userRepository: JpaUserRepositoryAdapter

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    private lateinit var userA: User
    private lateinit var userB: User
    private lateinit var personAId: String
    private lateinit var personBId: String

    private fun authenticatedJwt(userId: UserId): JwtAuthenticationToken {
        val jwt = Jwt.withTokenValue("test-token")
            .header("alg", "RS256")
            .subject("test-subject-${userId.value}")
            .issuer("http://localhost:9000")
            .claim("name", "Test User")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build()
        val authorities = listOf(SimpleGrantedAuthority("ROLE_USER"))
        val token = JwtAuthenticationToken(jwt, authorities, "test-subject-${userId.value}")
        token.details = userId
        return token
    }

    @BeforeEach
    fun setup() {
        // Clean up in correct order (respecting FK constraints)
        jdbcTemplate.execute("DELETE FROM pdp_updates")
        jdbcTemplate.execute("DELETE FROM pdp_goals")
        jdbcTemplate.execute("DELETE FROM action_items")
        jdbcTemplate.execute("DELETE FROM agenda_items")
        jdbcTemplate.execute("DELETE FROM one_on_one_entries")
        jdbcTemplate.execute("DELETE FROM one_on_one_series")
        jdbcTemplate.execute("DELETE FROM pinned_remember_items")
        jdbcTemplate.execute("DELETE FROM persons")
        jdbcTemplate.execute("DELETE FROM users")

        // Create two users
        userA = User(
            id = UserId.generate(),
            oidcSubject = "subject-a",
            oidcIssuer = "http://localhost:9000",
            displayName = "Manager A",
            email = "a@test.com"
        )
        userB = User(
            id = UserId.generate(),
            oidcSubject = "subject-b",
            oidcIssuer = "http://localhost:9000",
            displayName = "Manager B",
            email = "b@test.com"
        )
        userRepository.save(userA)
        userRepository.save(userB)

        // Create persons for each user
        personAId = createPerson(userA.id, "Alice")
        personBId = createPerson(userB.id, "Bob")
    }

    private fun createPerson(userId: UserId, name: String): String {
        val result = mockMvc.perform(
            post("/api/v1/persons")
                .with(authentication(authenticatedJwt(userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name": "$name"}""")
        )
            .andExpect(status().isCreated)
            .andReturn()

        val response = objectMapper.readTree(result.response.contentAsString)
        return response.get("id").asText()
    }

    private fun createPdpGoal(userId: UserId, personId: String, title: String, targetDate: String? = null): String {
        val body = buildString {
            append("""{"title": "$title"""")
            if (targetDate != null) append(""", "targetDate": "$targetDate"""")
            append("}")
        }

        val result = mockMvc.perform(
            post("/api/v1/persons/$personId/pdp-goals")
                .with(authentication(authenticatedJwt(userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isCreated)
            .andReturn()

        val response = objectMapper.readTree(result.response.contentAsString)
        return response.get("id").asText()
    }

    @Nested
    inner class CrudTests {

        @Test
        fun `should create and retrieve PDP goal`() {
            val goalId = createPdpGoal(userA.id, personAId, "Improve public speaking", "2026-12-31")

            mockMvc.perform(
                get("/api/v1/persons/$personAId/pdp-goals/$goalId")
                    .with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.title").value("Improve public speaking"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.targetDate").value("2026-12-31"))
        }

        @Test
        fun `should update PDP goal`() {
            val goalId = createPdpGoal(userA.id, personAId, "Original title")

            mockMvc.perform(
                put("/api/v1/persons/$personAId/pdp-goals/$goalId")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"title": "Updated title", "description": "New description"}""")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.title").value("Updated title"))
                .andExpect(jsonPath("$.description").value("New description"))
        }

        @Test
        fun `should delete PDP goal`() {
            val goalId = createPdpGoal(userA.id, personAId, "To delete")

            mockMvc.perform(
                delete("/api/v1/persons/$personAId/pdp-goals/$goalId")
                    .with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isNoContent)

            // Verify it's gone
            mockMvc.perform(
                get("/api/v1/persons/$personAId/pdp-goals/$goalId")
                    .with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isNotFound)
        }

        @Test
        fun `should list PDP goals by person`() {
            createPdpGoal(userA.id, personAId, "Goal 1")
            createPdpGoal(userA.id, personAId, "Goal 2")

            mockMvc.perform(
                get("/api/v1/persons/$personAId/pdp-goals")
                    .with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(2))
        }

        @Test
        fun `should filter PDP goals by status`() {
            val goalId = createPdpGoal(userA.id, personAId, "To achieve")
            createPdpGoal(userA.id, personAId, "Still active")

            // Achieve one
            mockMvc.perform(
                post("/api/v1/persons/$personAId/pdp-goals/$goalId/achieve")
                    .with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isOk)

            // Filter by ACTIVE
            mockMvc.perform(
                get("/api/v1/persons/$personAId/pdp-goals?status=ACTIVE")
                    .with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Still active"))
        }
    }

    @Nested
    inner class StatusTransitionTests {

        @Test
        fun `should achieve an active goal`() {
            val goalId = createPdpGoal(userA.id, personAId, "To achieve")

            mockMvc.perform(
                post("/api/v1/persons/$personAId/pdp-goals/$goalId/achieve")
                    .with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.status").value("ACHIEVED"))
        }

        @Test
        fun `should pause an active goal`() {
            val goalId = createPdpGoal(userA.id, personAId, "To pause")

            mockMvc.perform(
                post("/api/v1/persons/$personAId/pdp-goals/$goalId/pause")
                    .with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.status").value("PAUSED"))
        }

        @Test
        fun `should drop an active goal`() {
            val goalId = createPdpGoal(userA.id, personAId, "To drop")

            mockMvc.perform(
                post("/api/v1/persons/$personAId/pdp-goals/$goalId/drop")
                    .with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.status").value("DROPPED"))
        }

        @Test
        fun `should resume a paused goal`() {
            val goalId = createPdpGoal(userA.id, personAId, "To pause then resume")

            // Pause first
            mockMvc.perform(
                post("/api/v1/persons/$personAId/pdp-goals/$goalId/pause")
                    .with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isOk)

            // Resume
            mockMvc.perform(
                post("/api/v1/persons/$personAId/pdp-goals/$goalId/resume")
                    .with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.status").value("ACTIVE"))
        }

        @Test
        fun `should not achieve an already achieved goal`() {
            val goalId = createPdpGoal(userA.id, personAId, "Already achieved")

            mockMvc.perform(
                post("/api/v1/persons/$personAId/pdp-goals/$goalId/achieve")
                    .with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isOk)

            mockMvc.perform(
                post("/api/v1/persons/$personAId/pdp-goals/$goalId/achieve")
                    .with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isBadRequest)
        }

        @Test
        fun `should not resume an active goal`() {
            val goalId = createPdpGoal(userA.id, personAId, "Active goal")

            mockMvc.perform(
                post("/api/v1/persons/$personAId/pdp-goals/$goalId/resume")
                    .with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isBadRequest)
        }
    }

    @Nested
    inner class ProgressUpdateTests {

        @Test
        fun `should add progress update to goal`() {
            val goalId = createPdpGoal(userA.id, personAId, "Goal with updates")

            mockMvc.perform(
                post("/api/v1/persons/$personAId/pdp-goals/$goalId/updates")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"textMarkdown": "Completed first milestone"}""")
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.textMarkdown").value("Completed first milestone"))
                .andExpect(jsonPath("$.sensitive").value(false))
        }

        @Test
        fun `should add sensitive progress update`() {
            val goalId = createPdpGoal(userA.id, personAId, "Goal with sensitive update")

            mockMvc.perform(
                post("/api/v1/persons/$personAId/pdp-goals/$goalId/updates")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"textMarkdown": "Private note", "sensitive": true}""")
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.sensitive").value(true))
        }

        @Test
        fun `should list progress updates for goal`() {
            val goalId = createPdpGoal(userA.id, personAId, "Goal with updates")

            // Add two updates
            mockMvc.perform(
                post("/api/v1/persons/$personAId/pdp-goals/$goalId/updates")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"textMarkdown": "Update 1"}""")
            )
                .andExpect(status().isCreated)

            mockMvc.perform(
                post("/api/v1/persons/$personAId/pdp-goals/$goalId/updates")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"textMarkdown": "Update 2"}""")
            )
                .andExpect(status().isCreated)

            mockMvc.perform(
                get("/api/v1/persons/$personAId/pdp-goals/$goalId/updates")
                    .with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.totalElements").value(2))
        }

        @Test
        fun `should delete progress update`() {
            val goalId = createPdpGoal(userA.id, personAId, "Goal with update to delete")

            val result = mockMvc.perform(
                post("/api/v1/persons/$personAId/pdp-goals/$goalId/updates")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"textMarkdown": "To delete"}""")
            )
                .andExpect(status().isCreated)
                .andReturn()

            val updateId = objectMapper.readTree(result.response.contentAsString).get("id").asText()

            mockMvc.perform(
                delete("/api/v1/persons/$personAId/pdp-goals/$goalId/updates/$updateId")
                    .with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isNoContent)

            // Verify it's gone
            mockMvc.perform(
                get("/api/v1/persons/$personAId/pdp-goals/$goalId/updates")
                    .with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.totalElements").value(0))
        }

        @Test
        fun `should cascade delete updates when goal is deleted`() {
            val goalId = createPdpGoal(userA.id, personAId, "Goal to cascade delete")

            mockMvc.perform(
                post("/api/v1/persons/$personAId/pdp-goals/$goalId/updates")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"textMarkdown": "Will be cascade deleted"}""")
            )
                .andExpect(status().isCreated)

            // Delete the goal
            mockMvc.perform(
                delete("/api/v1/persons/$personAId/pdp-goals/$goalId")
                    .with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isNoContent)

            // Goal should be gone
            mockMvc.perform(
                get("/api/v1/persons/$personAId/pdp-goals/$goalId")
                    .with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isNotFound)
        }
    }

    @Nested
    inner class DataIsolationTests {

        @Test
        fun `manager B cannot read manager A's PDP goals`() {
            val goalId = createPdpGoal(userA.id, personAId, "Private to A")

            mockMvc.perform(
                get("/api/v1/persons/$personAId/pdp-goals/$goalId")
                    .with(authentication(authenticatedJwt(userB.id)))
            )
                .andExpect(status().isNotFound)
        }

        @Test
        fun `manager B cannot list manager A's PDP goals`() {
            createPdpGoal(userA.id, personAId, "A's goal 1")
            createPdpGoal(userA.id, personAId, "A's goal 2")

            mockMvc.perform(
                get("/api/v1/persons/$personAId/pdp-goals")
                    .with(authentication(authenticatedJwt(userB.id)))
            )
                .andExpect(status().isNotFound) // Person not found for user B
        }

        @Test
        fun `manager B cannot achieve manager A's PDP goal`() {
            val goalId = createPdpGoal(userA.id, personAId, "A's goal")

            mockMvc.perform(
                post("/api/v1/persons/$personAId/pdp-goals/$goalId/achieve")
                    .with(authentication(authenticatedJwt(userB.id)))
            )
                .andExpect(status().isNotFound)
        }

        @Test
        fun `manager B cannot delete manager A's PDP goal`() {
            val goalId = createPdpGoal(userA.id, personAId, "A's goal")

            mockMvc.perform(
                delete("/api/v1/persons/$personAId/pdp-goals/$goalId")
                    .with(authentication(authenticatedJwt(userB.id)))
            )
                .andExpect(status().isNotFound)
        }

        @Test
        fun `manager B cannot add updates to manager A's PDP goal`() {
            val goalId = createPdpGoal(userA.id, personAId, "A's goal")

            mockMvc.perform(
                post("/api/v1/persons/$personAId/pdp-goals/$goalId/updates")
                    .with(authentication(authenticatedJwt(userB.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"textMarkdown": "Unauthorized update"}""")
            )
                .andExpect(status().isNotFound)
        }
    }
}
