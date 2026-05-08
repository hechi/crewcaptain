package com.peoplemanager.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.peoplemanager.adapters.persistence.JpaUserRepositoryAdapter
import com.peoplemanager.domain.User
import com.peoplemanager.domain.UserId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
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
 * Full-stack integration tests for 1:1 Entry Management features.
 *
 * Covers Properties 6, 7, 3, 4, 10, 11, 12 via end-to-end API calls
 * against a real PostgreSQL database using Testcontainers.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class OneOnOneFullStackIntegrationTest {

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

    private lateinit var user: User
    private lateinit var personId: String

    @BeforeEach
    fun setUp() {
        // Clean up tables in correct order (respecting FK constraints)
        jdbcTemplate.execute("DELETE FROM agenda_items")
        jdbcTemplate.execute("DELETE FROM one_on_one_entries")
        jdbcTemplate.execute("DELETE FROM one_on_one_series")
        jdbcTemplate.execute("DELETE FROM pinned_remember_items")
        jdbcTemplate.execute("DELETE FROM persons")
        jdbcTemplate.execute("DELETE FROM users")

        // Create a user
        user = userRepository.save(
            User(
                id = UserId.generate(),
                oidcSubject = "test-user-subject",
                oidcIssuer = "https://issuer.example.com",
                displayName = "Test User",
                email = "testuser@example.com",
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
        )

        // Create a person via the API
        personId = createPersonViaApi(user.id, "Test Person")
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

    private fun createEntryViaApi(
        userId: UserId,
        personId: String,
        meetingDate: String,
        notesMarkdown: String? = "## Notes\nSome discussion",
        sensitive: Boolean = false
    ): String {
        val requestBody = mutableMapOf<String, Any?>(
            "meetingDate" to meetingDate,
            "agendaItems" to listOf(
                mapOf("text" to "Agenda item 1", "checked" to false)
            ),
            "outcomesMarkdown" to "## Outcomes\nAgreed on next steps",
            "sensitive" to sensitive
        )
        if (notesMarkdown != null) {
            requestBody["notesMarkdown"] = notesMarkdown
        }

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

    private fun upsertSeriesViaApi(userId: UserId, personId: String, templateMarkdown: String?) {
        val requestBody = mutableMapOf<String, Any?>(
            "cadenceType" to "BIWEEKLY"
        )
        if (templateMarkdown != null) {
            requestBody["templateMarkdown"] = templateMarkdown
        }
        mockMvc.perform(
            put("/api/v1/persons/$personId/one-on-one-series")
                .with(authentication(authenticatedJwt(userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody))
        ).andExpect(status().isOk)
    }

    // ===== Task 8.2: Pagination and ordering integration test (Properties 6, 7) =====

    @Nested
    inner class PaginationAndOrderingTests {

        @Test
        @Tag("property")
        fun `pagination metadata is correct for N entries with page size S`() {
            // Create 5 entries with various dates
            createEntryViaApi(user.id, personId, "2025-01-01T10:00:00Z")
            createEntryViaApi(user.id, personId, "2025-02-01T10:00:00Z")
            createEntryViaApi(user.id, personId, "2025-03-01T10:00:00Z")
            createEntryViaApi(user.id, personId, "2025-04-01T10:00:00Z")
            createEntryViaApi(user.id, personId, "2025-05-01T10:00:00Z")

            // Request page 0 with size 2
            val result = mockMvc.perform(
                get("/api/v1/persons/$personId/one-on-one-entries?page=0&size=2")
                    .with(authentication(authenticatedJwt(user.id)))
            )
                .andExpect(status().isOk)
                .andReturn()

            val response = objectMapper.readTree(result.response.contentAsString)
            response.get("totalElements").asLong() shouldBe 5
            response.get("totalPages").asInt() shouldBe 3 // ceil(5/2) = 3
            response.get("page").asInt() shouldBe 0
            response.get("size").asInt() shouldBe 2
            response.get("content").size() shouldBe 2
        }

        @Test
        @Tag("property")
        fun `last page contains remaining entries`() {
            // Create 5 entries
            createEntryViaApi(user.id, personId, "2025-01-01T10:00:00Z")
            createEntryViaApi(user.id, personId, "2025-02-01T10:00:00Z")
            createEntryViaApi(user.id, personId, "2025-03-01T10:00:00Z")
            createEntryViaApi(user.id, personId, "2025-04-01T10:00:00Z")
            createEntryViaApi(user.id, personId, "2025-05-01T10:00:00Z")

            // Request page 2 with size 2 (last page should have 1 entry)
            val result = mockMvc.perform(
                get("/api/v1/persons/$personId/one-on-one-entries?page=2&size=2")
                    .with(authentication(authenticatedJwt(user.id)))
            )
                .andExpect(status().isOk)
                .andReturn()

            val response = objectMapper.readTree(result.response.contentAsString)
            response.get("totalElements").asLong() shouldBe 5
            response.get("totalPages").asInt() shouldBe 3
            response.get("page").asInt() shouldBe 2
            response.get("content").size() shouldBe 1
        }

        @Test
        @Tag("property")
        fun `entries are returned in reverse chronological order`() {
            // Create entries with various dates (not in order)
            createEntryViaApi(user.id, personId, "2025-03-15T10:00:00Z")
            createEntryViaApi(user.id, personId, "2025-01-10T10:00:00Z")
            createEntryViaApi(user.id, personId, "2025-05-20T10:00:00Z")
            createEntryViaApi(user.id, personId, "2025-02-05T10:00:00Z")
            createEntryViaApi(user.id, personId, "2025-04-25T10:00:00Z")

            // List all entries
            val result = mockMvc.perform(
                get("/api/v1/persons/$personId/one-on-one-entries?page=0&size=10")
                    .with(authentication(authenticatedJwt(user.id)))
            )
                .andExpect(status().isOk)
                .andReturn()

            val response = objectMapper.readTree(result.response.contentAsString)
            val content = response.get("content")
            content.size() shouldBe 5

            // Verify reverse chronological order
            val dates = (0 until content.size()).map {
                Instant.parse(content.get(it).get("meetingDate").asText())
            }
            for (i in 0 until dates.size - 1) {
                assert(dates[i] >= dates[i + 1]) {
                    "Expected entries in reverse chronological order but got ${dates[i]} before ${dates[i + 1]}"
                }
            }
        }

        @Test
        @Tag("property")
        fun `default pagination returns page 0 with size 20`() {
            // Create 3 entries
            createEntryViaApi(user.id, personId, "2025-01-01T10:00:00Z")
            createEntryViaApi(user.id, personId, "2025-02-01T10:00:00Z")
            createEntryViaApi(user.id, personId, "2025-03-01T10:00:00Z")

            // Request without pagination params
            val result = mockMvc.perform(
                get("/api/v1/persons/$personId/one-on-one-entries")
                    .with(authentication(authenticatedJwt(user.id)))
            )
                .andExpect(status().isOk)
                .andReturn()

            val response = objectMapper.readTree(result.response.contentAsString)
            response.get("totalElements").asLong() shouldBe 3
            response.get("totalPages").asInt() shouldBe 1
            response.get("page").asInt() shouldBe 0
            response.get("size").asInt() shouldBe 20
            response.get("content").size() shouldBe 3
        }
    }

    // ===== Task 8.3: Template prefill integration test (Properties 3, 4) =====

    @Nested
    inner class TemplatePrefillTests {

        @Test
        @Tag("property")
        fun `entry created without notes gets prefilled from series template`() {
            val template = "## Agenda\n- [ ] Review action items\n- [ ] Check-in\n\n## Notes\n"

            // Set up series with template
            upsertSeriesViaApi(user.id, personId, template)

            // Create entry without notes (notesMarkdown not in request body)
            val requestBody = mapOf(
                "meetingDate" to "2025-05-08T14:00:00Z",
                "agendaItems" to listOf(
                    mapOf("text" to "Discuss project", "checked" to false)
                )
            )
            val result = mockMvc.perform(
                post("/api/v1/persons/$personId/one-on-one-entries")
                    .with(authentication(authenticatedJwt(user.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody))
            )
                .andExpect(status().isCreated)
                .andReturn()

            val response = objectMapper.readTree(result.response.contentAsString)
            response.get("notesMarkdown").asText() shouldBe template
        }

        @Test
        @Tag("property")
        fun `entry created with explicit notes does NOT get template applied`() {
            val template = "## Agenda\n- [ ] Review action items\n\n## Notes\n"
            val explicitNotes = "## My Custom Notes\nThese are my own notes"

            // Set up series with template
            upsertSeriesViaApi(user.id, personId, template)

            // Create entry with explicit notes
            val requestBody = mapOf(
                "meetingDate" to "2025-05-08T14:00:00Z",
                "notesMarkdown" to explicitNotes,
                "agendaItems" to listOf(
                    mapOf("text" to "Discuss project", "checked" to false)
                )
            )
            val result = mockMvc.perform(
                post("/api/v1/persons/$personId/one-on-one-entries")
                    .with(authentication(authenticatedJwt(user.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody))
            )
                .andExpect(status().isCreated)
                .andReturn()

            val response = objectMapper.readTree(result.response.contentAsString)
            response.get("notesMarkdown").asText() shouldBe explicitNotes
        }

        @Test
        @Tag("property")
        fun `entry created with empty string notes does NOT get template applied`() {
            val template = "## Agenda\n- [ ] Review action items\n\n## Notes\n"

            // Set up series with template
            upsertSeriesViaApi(user.id, personId, template)

            // Create entry with empty string notes
            val requestBody = mapOf(
                "meetingDate" to "2025-05-08T14:00:00Z",
                "notesMarkdown" to "",
                "agendaItems" to listOf(
                    mapOf("text" to "Discuss project", "checked" to false)
                )
            )
            val result = mockMvc.perform(
                post("/api/v1/persons/$personId/one-on-one-entries")
                    .with(authentication(authenticatedJwt(user.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody))
            )
                .andExpect(status().isCreated)
                .andReturn()

            val response = objectMapper.readTree(result.response.contentAsString)
            response.get("notesMarkdown").asText() shouldBe ""
        }
    }

    // ===== Task 8.4: Delete-then-retrieve integration test (Property 10) =====

    @Nested
    inner class DeleteThenRetrieveTests {

        @Test
        @Tag("property")
        fun `deleted entry returns 404 on subsequent GET`() {
            // Create an entry
            val entryId = createEntryViaApi(user.id, personId, "2025-05-08T14:00:00Z")

            // Verify it exists
            mockMvc.perform(
                get("/api/v1/persons/$personId/one-on-one-entries/$entryId")
                    .with(authentication(authenticatedJwt(user.id)))
            ).andExpect(status().isOk)

            // Delete it
            mockMvc.perform(
                delete("/api/v1/persons/$personId/one-on-one-entries/$entryId")
                    .with(authentication(authenticatedJwt(user.id)))
            ).andExpect(status().isNoContent)

            // Verify GET returns 404
            mockMvc.perform(
                get("/api/v1/persons/$personId/one-on-one-entries/$entryId")
                    .with(authentication(authenticatedJwt(user.id)))
            ).andExpect(status().isNotFound)
        }

        @Test
        @Tag("property")
        fun `deleted entry no longer appears in list`() {
            // Create two entries
            val entryId1 = createEntryViaApi(user.id, personId, "2025-05-01T10:00:00Z")
            val entryId2 = createEntryViaApi(user.id, personId, "2025-05-08T10:00:00Z")

            // Verify both exist in list
            val beforeResult = mockMvc.perform(
                get("/api/v1/persons/$personId/one-on-one-entries")
                    .with(authentication(authenticatedJwt(user.id)))
            )
                .andExpect(status().isOk)
                .andReturn()

            val beforeResponse = objectMapper.readTree(beforeResult.response.contentAsString)
            beforeResponse.get("totalElements").asLong() shouldBe 2

            // Delete one entry
            mockMvc.perform(
                delete("/api/v1/persons/$personId/one-on-one-entries/$entryId1")
                    .with(authentication(authenticatedJwt(user.id)))
            ).andExpect(status().isNoContent)

            // Verify list now has only 1 entry
            val afterResult = mockMvc.perform(
                get("/api/v1/persons/$personId/one-on-one-entries")
                    .with(authentication(authenticatedJwt(user.id)))
            )
                .andExpect(status().isOk)
                .andReturn()

            val afterResponse = objectMapper.readTree(afterResult.response.contentAsString)
            afterResponse.get("totalElements").asLong() shouldBe 1
            afterResponse.get("content").get(0).get("id").asText() shouldBe entryId2
        }
    }

    // ===== Task 8.5: Last 1:1 date integration test (Property 11) =====

    @Nested
    inner class Last1on1DateTests {

        @Test
        @Tag("property")
        fun `person at-a-glance returns max meeting date across entries`() {
            // Create entries with various dates
            createEntryViaApi(user.id, personId, "2025-01-15T10:00:00Z")
            createEntryViaApi(user.id, personId, "2025-03-20T10:00:00Z")
            createEntryViaApi(user.id, personId, "2025-02-10T10:00:00Z")

            // Get person and check at-a-glance
            val result = mockMvc.perform(
                get("/api/v1/persons/$personId")
                    .with(authentication(authenticatedJwt(user.id)))
            )
                .andExpect(status().isOk)
                .andReturn()

            val response = objectMapper.readTree(result.response.contentAsString)
            val atAGlance = response.get("atAGlance")
            atAGlance.shouldNotBeNull()
            val last1on1Date = atAGlance.get("last1on1Date").asText()
            last1on1Date shouldBe "2025-03-20T10:00:00Z"
        }

        @Test
        @Tag("property")
        fun `person at-a-glance returns null when no entries exist`() {
            // No entries created — get person and check at-a-glance
            val result = mockMvc.perform(
                get("/api/v1/persons/$personId")
                    .with(authentication(authenticatedJwt(user.id)))
            )
                .andExpect(status().isOk)
                .andReturn()

            val response = objectMapper.readTree(result.response.contentAsString)
            val atAGlance = response.get("atAGlance")
            atAGlance.shouldNotBeNull()
            atAGlance.get("last1on1Date").isNull shouldBe true
        }

        @Test
        @Tag("property")
        fun `person at-a-glance returns null after all entries deleted`() {
            // Create entries
            val entryId1 = createEntryViaApi(user.id, personId, "2025-01-15T10:00:00Z")
            val entryId2 = createEntryViaApi(user.id, personId, "2025-03-20T10:00:00Z")

            // Verify last1on1Date is set
            val beforeResult = mockMvc.perform(
                get("/api/v1/persons/$personId")
                    .with(authentication(authenticatedJwt(user.id)))
            )
                .andExpect(status().isOk)
                .andReturn()

            val beforeResponse = objectMapper.readTree(beforeResult.response.contentAsString)
            beforeResponse.get("atAGlance").get("last1on1Date").isNull shouldBe false

            // Delete all entries
            mockMvc.perform(
                delete("/api/v1/persons/$personId/one-on-one-entries/$entryId1")
                    .with(authentication(authenticatedJwt(user.id)))
            ).andExpect(status().isNoContent)

            mockMvc.perform(
                delete("/api/v1/persons/$personId/one-on-one-entries/$entryId2")
                    .with(authentication(authenticatedJwt(user.id)))
            ).andExpect(status().isNoContent)

            // Verify last1on1Date is now null
            val afterResult = mockMvc.perform(
                get("/api/v1/persons/$personId")
                    .with(authentication(authenticatedJwt(user.id)))
            )
                .andExpect(status().isOk)
                .andReturn()

            val afterResponse = objectMapper.readTree(afterResult.response.contentAsString)
            afterResponse.get("atAGlance").get("last1on1Date").isNull shouldBe true
        }
    }

    // ===== Task 8.6: Sensitive flag integration test (Property 12) =====

    @Nested
    inner class SensitiveFlagTests {

        @Test
        @Tag("property")
        fun `entry created with sensitive=true returns sensitive=true on retrieval`() {
            // Create entry with sensitive=true
            val entryId = createEntryViaApi(
                user.id, personId, "2025-05-08T14:00:00Z",
                sensitive = true
            )

            // Retrieve and verify
            val result = mockMvc.perform(
                get("/api/v1/persons/$personId/one-on-one-entries/$entryId")
                    .with(authentication(authenticatedJwt(user.id)))
            )
                .andExpect(status().isOk)
                .andReturn()

            val response = objectMapper.readTree(result.response.contentAsString)
            response.get("sensitive").asBoolean() shouldBe true
        }

        @Test
        @Tag("property")
        fun `entry created with sensitive=false returns sensitive=false on retrieval`() {
            // Create entry with sensitive=false
            val entryId = createEntryViaApi(
                user.id, personId, "2025-05-08T14:00:00Z",
                sensitive = false
            )

            // Retrieve and verify
            val result = mockMvc.perform(
                get("/api/v1/persons/$personId/one-on-one-entries/$entryId")
                    .with(authentication(authenticatedJwt(user.id)))
            )
                .andExpect(status().isOk)
                .andReturn()

            val response = objectMapper.readTree(result.response.contentAsString)
            response.get("sensitive").asBoolean() shouldBe false
        }

        @Test
        @Tag("property")
        fun `updating sensitive from true to false persists correctly`() {
            // Create entry with sensitive=true
            val entryId = createEntryViaApi(
                user.id, personId, "2025-05-08T14:00:00Z",
                sensitive = true
            )

            // Verify it's sensitive
            val beforeResult = mockMvc.perform(
                get("/api/v1/persons/$personId/one-on-one-entries/$entryId")
                    .with(authentication(authenticatedJwt(user.id)))
            )
                .andExpect(status().isOk)
                .andReturn()

            objectMapper.readTree(beforeResult.response.contentAsString)
                .get("sensitive").asBoolean() shouldBe true

            // Update to sensitive=false
            val updateRequest = mapOf("sensitive" to false)
            mockMvc.perform(
                put("/api/v1/persons/$personId/one-on-one-entries/$entryId")
                    .with(authentication(authenticatedJwt(user.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest))
            ).andExpect(status().isOk)

            // Verify it's no longer sensitive
            val afterResult = mockMvc.perform(
                get("/api/v1/persons/$personId/one-on-one-entries/$entryId")
                    .with(authentication(authenticatedJwt(user.id)))
            )
                .andExpect(status().isOk)
                .andReturn()

            objectMapper.readTree(afterResult.response.contentAsString)
                .get("sensitive").asBoolean() shouldBe false
        }

        @Test
        @Tag("property")
        fun `updating sensitive from false to true persists correctly`() {
            // Create entry with sensitive=false
            val entryId = createEntryViaApi(
                user.id, personId, "2025-05-08T14:00:00Z",
                sensitive = false
            )

            // Update to sensitive=true
            val updateRequest = mapOf("sensitive" to true)
            mockMvc.perform(
                put("/api/v1/persons/$personId/one-on-one-entries/$entryId")
                    .with(authentication(authenticatedJwt(user.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest))
            ).andExpect(status().isOk)

            // Verify it's now sensitive
            val result = mockMvc.perform(
                get("/api/v1/persons/$personId/one-on-one-entries/$entryId")
                    .with(authentication(authenticatedJwt(user.id)))
            )
                .andExpect(status().isOk)
                .andReturn()

            objectMapper.readTree(result.response.contentAsString)
                .get("sensitive").asBoolean() shouldBe true
        }
    }
}
