package com.peoplemanager.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.peoplemanager.adapters.persistence.JpaUserRepositoryAdapter
import com.peoplemanager.domain.User
import com.peoplemanager.domain.UserId
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
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
 * Full-stack integration test for Action Items.
 *
 * Verifies:
 * - CRUD operations work end-to-end with real PostgreSQL
 * - userId scoping is enforced (data isolation between managers)
 * - Status transitions (OPEN → DONE, OPEN → CANCELED)
 * - Cross-user access returns 404
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class ActionItemIntegrationTest {

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
        // Clean up
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

    private fun createActionItem(userId: UserId, personId: String, title: String, dueDate: String? = null): String {
        val body = buildString {
            append("""{"title": "$title"""")
            if (dueDate != null) append(""", "dueDate": "$dueDate"""")
            append("}")
        }

        val result = mockMvc.perform(
            post("/api/v1/persons/$personId/action-items")
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
        fun `should create and retrieve action item`() {
            val itemId = createActionItem(userA.id, personAId, "Follow up on project", "2026-05-20")

            mockMvc.perform(
                get("/api/v1/persons/$personAId/action-items/$itemId")
                    .with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.title").value("Follow up on project"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.dueDate").value("2026-05-20"))
        }

        @Test
        fun `should update action item`() {
            val itemId = createActionItem(userA.id, personAId, "Original title")

            mockMvc.perform(
                put("/api/v1/persons/$personAId/action-items/$itemId")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"title": "Updated title", "ownerType": "PERSON"}""")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.title").value("Updated title"))
                .andExpect(jsonPath("$.ownerType").value("PERSON"))
        }

        @Test
        fun `should delete action item`() {
            val itemId = createActionItem(userA.id, personAId, "To delete")

            mockMvc.perform(
                delete("/api/v1/persons/$personAId/action-items/$itemId")
                    .with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isNoContent)

            // Verify it's gone
            mockMvc.perform(
                get("/api/v1/persons/$personAId/action-items/$itemId")
                    .with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isNotFound)
        }

        @Test
        fun `should list action items by person`() {
            createActionItem(userA.id, personAId, "Item 1")
            createActionItem(userA.id, personAId, "Item 2")

            mockMvc.perform(
                get("/api/v1/persons/$personAId/action-items")
                    .with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(2))
        }

        @Test
        fun `should filter action items by status`() {
            val itemId = createActionItem(userA.id, personAId, "To complete")
            createActionItem(userA.id, personAId, "Still open")

            // Complete one
            mockMvc.perform(
                post("/api/v1/persons/$personAId/action-items/$itemId/complete")
                    .with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isOk)

            // Filter by OPEN
            mockMvc.perform(
                get("/api/v1/persons/$personAId/action-items?status=OPEN")
                    .with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Still open"))
        }
    }

    @Nested
    inner class StatusTransitionTests {

        @Test
        fun `should complete an open action item`() {
            val itemId = createActionItem(userA.id, personAId, "To complete")

            mockMvc.perform(
                post("/api/v1/persons/$personAId/action-items/$itemId/complete")
                    .with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.status").value("DONE"))
        }

        @Test
        fun `should cancel an open action item`() {
            val itemId = createActionItem(userA.id, personAId, "To cancel")

            mockMvc.perform(
                post("/api/v1/persons/$personAId/action-items/$itemId/cancel")
                    .with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.status").value("CANCELED"))
        }

        @Test
        fun `should not complete an already done item`() {
            val itemId = createActionItem(userA.id, personAId, "Already done")

            // Complete it first
            mockMvc.perform(
                post("/api/v1/persons/$personAId/action-items/$itemId/complete")
                    .with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isOk)

            // Try to complete again
            mockMvc.perform(
                post("/api/v1/persons/$personAId/action-items/$itemId/complete")
                    .with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isBadRequest)
        }

        @Test
        fun `should not cancel an already done item`() {
            val itemId = createActionItem(userA.id, personAId, "Done item")

            mockMvc.perform(
                post("/api/v1/persons/$personAId/action-items/$itemId/complete")
                    .with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isOk)

            mockMvc.perform(
                post("/api/v1/persons/$personAId/action-items/$itemId/cancel")
                    .with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isBadRequest)
        }
    }

    @Nested
    inner class DataIsolationTests {

        @Test
        fun `manager B cannot read manager A's action items`() {
            val itemId = createActionItem(userA.id, personAId, "Private to A")

            // User B tries to access User A's action item
            mockMvc.perform(
                get("/api/v1/persons/$personAId/action-items/$itemId")
                    .with(authentication(authenticatedJwt(userB.id)))
            )
                .andExpect(status().isNotFound)
        }

        @Test
        fun `manager B cannot list manager A's action items`() {
            createActionItem(userA.id, personAId, "A's item 1")
            createActionItem(userA.id, personAId, "A's item 2")

            // User B tries to list User A's person's action items
            mockMvc.perform(
                get("/api/v1/persons/$personAId/action-items")
                    .with(authentication(authenticatedJwt(userB.id)))
            )
                .andExpect(status().isNotFound) // Person not found for user B
        }

        @Test
        fun `manager B cannot complete manager A's action item`() {
            val itemId = createActionItem(userA.id, personAId, "A's task")

            mockMvc.perform(
                post("/api/v1/persons/$personAId/action-items/$itemId/complete")
                    .with(authentication(authenticatedJwt(userB.id)))
            )
                .andExpect(status().isNotFound)
        }

        @Test
        fun `manager B cannot delete manager A's action item`() {
            val itemId = createActionItem(userA.id, personAId, "A's task")

            mockMvc.perform(
                delete("/api/v1/persons/$personAId/action-items/$itemId")
                    .with(authentication(authenticatedJwt(userB.id)))
            )
                .andExpect(status().isNotFound)
        }

        @Test
        fun `cross-user list all action items only returns own items`() {
            createActionItem(userA.id, personAId, "A's item")
            createActionItem(userB.id, personBId, "B's item")

            // User A should only see their own
            mockMvc.perform(
                get("/api/v1/action-items")
                    .with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("A's item"))

            // User B should only see their own
            mockMvc.perform(
                get("/api/v1/action-items")
                    .with(authentication(authenticatedJwt(userB.id)))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("B's item"))
        }
    }

    @Nested
    inner class OverdueTests {

        @Test
        fun `should return overdue items when overdueOnly is true`() {
            // Create an item with past due date
            createActionItem(userA.id, personAId, "Overdue item", "2026-01-01")
            // Create an item with future due date
            createActionItem(userA.id, personAId, "Future item", "2027-12-31")

            mockMvc.perform(
                get("/api/v1/action-items?overdueOnly=true")
                    .with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Overdue item"))
        }
    }
}
