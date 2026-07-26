package com.peoplemanager.integration

import tools.jackson.databind.ObjectMapper
import com.peoplemanager.adapters.persistence.JpaUserRepositoryAdapter
import com.peoplemanager.domain.User
import com.peoplemanager.domain.UserId
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Instant

/**
 * Full-stack integration test for 1:1 Entry data isolation (Property 5).
 *
 * Verifies that User B cannot access, modify, or delete User A's 1:1 entries or series.
 * Cross-user access returns 404 (not 403) to avoid confirming resource existence.
 *
 * **Validates: Requirements 1.6, 2.7, 3.2, 3.3, 4.3, 5.2, 5.3, 6.5, 6.6**
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class OneOnOneDataIsolationIntegrationTest {

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

    @BeforeEach
    fun setUp() {
        // Clean up tables in correct order (respecting FK constraints)
        jdbcTemplate.execute("DELETE FROM agenda_items")
        jdbcTemplate.execute("DELETE FROM one_on_one_entries")
        jdbcTemplate.execute("DELETE FROM one_on_one_series")
        jdbcTemplate.execute("DELETE FROM pinned_remember_items")
        jdbcTemplate.execute("DELETE FROM persons")
        jdbcTemplate.execute("DELETE FROM users")

        // Create two users
        userA = userRepository.save(
            User(
                id = UserId.generate(),
                oidcSubject = "user-a-subject",
                oidcIssuer = "https://issuer.example.com",
                displayName = "User A",
                email = "usera@example.com",
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
        )

        userB = userRepository.save(
            User(
                id = UserId.generate(),
                oidcSubject = "user-b-subject",
                oidcIssuer = "https://issuer.example.com",
                displayName = "User B",
                email = "userb@example.com",
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
        )

        // Create a person for each user via the API
        personAId = createPersonViaApi(userA.id, "Person A")
        personBId = createPersonViaApi(userB.id, "Person B")
    }

    private fun authenticatedJwt(userId: UserId): JwtAuthenticationToken {
        val jwt = Jwt.withTokenValue("test-token")
            .header("alg", "RS256")
            .subject("test-subject")
            .issuer("https://auth.example.com")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build()
        val authorities = listOf(SimpleGrantedAuthority("ROLE_USER"))
        val token = JwtAuthenticationToken(jwt, authorities, "test-subject")
        token.details = userId
        return token
    }

    private fun createPersonViaApi(userId: UserId, name: String): String {
        val requestBody = mapOf("name" to name)
        val result = mockMvc.perform(
            post("/api/v1/persons")
                .with(authentication(authenticatedJwt(userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody))
        )
            .andExpect(status().isCreated)
            .andReturn()

        val responseJson = objectMapper.readTree(result.response.contentAsString)
        return responseJson.get("id").asText()
    }

    private fun createEntryViaApi(userId: UserId, personId: String, meetingDate: String = "2025-05-08T14:00:00Z"): String {
        val requestBody = mapOf(
            "meetingDate" to meetingDate,
            "agendaItems" to listOf(
                mapOf("text" to "Discuss project timeline", "checked" to false),
                mapOf("text" to "Review Q2 goals", "checked" to false)
            ),
            "notesMarkdown" to "## Notes\nSome discussion points",
            "outcomesMarkdown" to "## Outcomes\nAgreed on timeline",
            "sensitive" to false
        )
        val result = mockMvc.perform(
            post("/api/v1/persons/$personId/one-on-one-entries")
                .with(authentication(authenticatedJwt(userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody))
        )
            .andExpect(status().isCreated)
            .andReturn()

        val responseJson = objectMapper.readTree(result.response.contentAsString)
        return responseJson.get("id").asText()
    }

    private fun upsertSeriesViaApi(userId: UserId, personId: String) {
        val requestBody = mapOf(
            "cadenceType" to "BIWEEKLY",
            "templateMarkdown" to "## Template\n- [ ] Review action items"
        )
        mockMvc.perform(
            put("/api/v1/persons/$personId/one-on-one-series")
                .with(authentication(authenticatedJwt(userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody))
        ).andExpect(status().isOk)
    }

    // ===== Property 5: Data isolation across users for 1:1 entries =====

    @Nested
    inner class EntryDataIsolationTests {

        @Test
        @Tag("property")
        fun `User B cannot GET User A's entry by ID - returns 404`() {
            val entryId = createEntryViaApi(userA.id, personAId)

            // User B tries to access User A's entry using User A's person
            mockMvc.perform(
                get("/api/v1/persons/$personAId/one-on-one-entries/$entryId")
                    .with(authentication(authenticatedJwt(userB.id)))
            ).andExpect(status().isNotFound)
        }

        @Test
        @Tag("property")
        fun `User B cannot PUT (update) User A's entry - returns 404`() {
            val entryId = createEntryViaApi(userA.id, personAId)

            val updateRequest = mapOf(
                "notesMarkdown" to "## Hacked Notes\nUser B trying to modify"
            )
            mockMvc.perform(
                put("/api/v1/persons/$personAId/one-on-one-entries/$entryId")
                    .with(authentication(authenticatedJwt(userB.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest))
            ).andExpect(status().isNotFound)
        }

        @Test
        @Tag("property")
        fun `User B cannot DELETE User A's entry - returns 404`() {
            val entryId = createEntryViaApi(userA.id, personAId)

            mockMvc.perform(
                delete("/api/v1/persons/$personAId/one-on-one-entries/$entryId")
                    .with(authentication(authenticatedJwt(userB.id)))
            ).andExpect(status().isNotFound)
        }

        @Test
        @Tag("property")
        fun `User B listing User A's person entries returns 404`() {
            // Create entries for User A
            createEntryViaApi(userA.id, personAId, "2025-05-01T10:00:00Z")
            createEntryViaApi(userA.id, personAId, "2025-05-08T10:00:00Z")

            // User B tries to list entries for User A's person
            mockMvc.perform(
                get("/api/v1/persons/$personAId/one-on-one-entries")
                    .with(authentication(authenticatedJwt(userB.id)))
            ).andExpect(status().isNotFound)
        }

        @Test
        @Tag("property")
        fun `list endpoint returns only authenticated user's entries`() {
            // Create entries for both users
            createEntryViaApi(userA.id, personAId, "2025-05-01T10:00:00Z")
            createEntryViaApi(userA.id, personAId, "2025-05-08T10:00:00Z")
            createEntryViaApi(userB.id, personBId, "2025-05-15T10:00:00Z")

            // User A sees only their entries
            val resultA = mockMvc.perform(
                get("/api/v1/persons/$personAId/one-on-one-entries")
                    .with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isOk)
                .andReturn()

            val responseA = objectMapper.readTree(resultA.response.contentAsString)
            responseA.get("totalElements").asInt() shouldBe 2

            // User B sees only their entries
            val resultB = mockMvc.perform(
                get("/api/v1/persons/$personBId/one-on-one-entries")
                    .with(authentication(authenticatedJwt(userB.id)))
            )
                .andExpect(status().isOk)
                .andReturn()

            val responseB = objectMapper.readTree(resultB.response.contentAsString)
            responseB.get("totalElements").asInt() shouldBe 1
        }

        @Test
        @Tag("property")
        fun `cross-user entry access returns 404 not 403`() {
            val entryId = createEntryViaApi(userA.id, personAId)

            // Verify it's specifically 404, not 403
            val result = mockMvc.perform(
                get("/api/v1/persons/$personAId/one-on-one-entries/$entryId")
                    .with(authentication(authenticatedJwt(userB.id)))
            ).andReturn()

            result.response.status shouldBe 404
            val body = objectMapper.readTree(result.response.contentAsString)
            body.get("status").asInt() shouldBe 404
            body.get("error").asText() shouldBe "Not Found"
        }

        @Test
        @Tag("property")
        fun `User A's entry still exists after User B's failed access attempts`() {
            val entryId = createEntryViaApi(userA.id, personAId)

            // User B tries various operations
            mockMvc.perform(
                get("/api/v1/persons/$personAId/one-on-one-entries/$entryId")
                    .with(authentication(authenticatedJwt(userB.id)))
            ).andExpect(status().isNotFound)

            mockMvc.perform(
                delete("/api/v1/persons/$personAId/one-on-one-entries/$entryId")
                    .with(authentication(authenticatedJwt(userB.id)))
            ).andExpect(status().isNotFound)

            // User A can still access their entry
            mockMvc.perform(
                get("/api/v1/persons/$personAId/one-on-one-entries/$entryId")
                    .with(authentication(authenticatedJwt(userA.id)))
            ).andExpect(status().isOk)
        }

        @Test
        @Tag("property")
        fun `User B cannot create entries for User A's person - returns 404`() {
            val requestBody = mapOf(
                "meetingDate" to "2025-06-01T10:00:00Z",
                "notesMarkdown" to "Hacked entry"
            )

            mockMvc.perform(
                post("/api/v1/persons/$personAId/one-on-one-entries")
                    .with(authentication(authenticatedJwt(userB.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody))
            ).andExpect(status().isNotFound)
        }
    }

    @Nested
    inner class SeriesDataIsolationTests {

        @Test
        @Tag("property")
        fun `User B cannot GET User A's series - returns 404`() {
            upsertSeriesViaApi(userA.id, personAId)

            mockMvc.perform(
                get("/api/v1/persons/$personAId/one-on-one-series")
                    .with(authentication(authenticatedJwt(userB.id)))
            ).andExpect(status().isNotFound)
        }

        @Test
        @Tag("property")
        fun `User B cannot upsert series for User A's person - returns 404`() {
            val requestBody = mapOf(
                "cadenceType" to "WEEKLY",
                "templateMarkdown" to "Hacked template"
            )

            mockMvc.perform(
                put("/api/v1/persons/$personAId/one-on-one-series")
                    .with(authentication(authenticatedJwt(userB.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody))
            ).andExpect(status().isNotFound)
        }

        @Test
        @Tag("property")
        fun `cross-user series access returns 404 not 403`() {
            upsertSeriesViaApi(userA.id, personAId)

            val result = mockMvc.perform(
                get("/api/v1/persons/$personAId/one-on-one-series")
                    .with(authentication(authenticatedJwt(userB.id)))
            ).andReturn()

            result.response.status shouldBe 404
            val body = objectMapper.readTree(result.response.contentAsString)
            body.get("status").asInt() shouldBe 404
            body.get("error").asText() shouldBe "Not Found"
        }
    }
}
