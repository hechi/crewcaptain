package com.peoplemanager.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.peoplemanager.adapters.persistence.JpaUserRepositoryAdapter
import com.peoplemanager.domain.User
import com.peoplemanager.domain.UserId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotContain
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
import java.util.Base64

/**
 * Integration tests verifying that sensitive content is encrypted at rest in the database.
 *
 * These tests use a real PostgreSQL database via Testcontainers and verify:
 * - Sensitive 1:1 entry notes/outcomes are stored encrypted in the DB
 * - Sensitive quick note text is stored encrypted in the DB
 * - Sensitive PDP update text is stored encrypted in the DB
 * - Non-sensitive content is stored in plaintext
 * - Encrypted content is correctly decrypted when read back via API
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class EncryptionIntegrationTest {

    companion object {
        // A valid 32-byte key for testing
        private val TEST_ENCRYPTION_KEY = Base64.getEncoder().encodeToString(
            ByteArray(32) { (it + 42).toByte() }
        )

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
            registry.add("app.encryption.key") { TEST_ENCRYPTION_KEY }
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
        jdbcTemplate.execute("DELETE FROM quick_notes")
        jdbcTemplate.execute("DELETE FROM kudos")
        jdbcTemplate.execute("DELETE FROM action_items")
        jdbcTemplate.execute("DELETE FROM pdp_updates")
        jdbcTemplate.execute("DELETE FROM pdp_goals")
        jdbcTemplate.execute("DELETE FROM agenda_items")
        jdbcTemplate.execute("DELETE FROM one_on_one_entries")
        jdbcTemplate.execute("DELETE FROM one_on_one_series")
        jdbcTemplate.execute("DELETE FROM pinned_remember_items")
        jdbcTemplate.execute("DELETE FROM persons")
        jdbcTemplate.execute("DELETE FROM users")

        // Create user
        user = userRepository.save(User(
            id = UserId.generate(),
            oidcSubject = "subject-enc",
            oidcIssuer = "http://localhost:9000",
            displayName = "Encryption Test User",
            email = "enc@test.com"
        ))

        // Create a person
        val personResult = mockMvc.perform(
            post("/api/v1/persons")
                .with(authentication(authenticatedJwt(user.id)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name": "Test Person", "roleTitle": "Engineer"}""")
        ).andExpect(status().isCreated).andReturn()

        personId = objectMapper.readTree(personResult.response.contentAsString).get("id").asText()
    }

    @Nested
    inner class OneOnOneEntryEncryption {

        @Test
        fun `should store sensitive 1-1 entry notes encrypted in database`() {
            val sensitiveNotes = "CONFIDENTIAL: Employee disclosed health condition"
            val sensitiveOutcomes = "PRIVATE: Agreed to flexible schedule accommodation"

            val result = mockMvc.perform(
                post("/api/v1/persons/$personId/one-on-one-entries")
                    .with(authentication(authenticatedJwt(user.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "meetingDate": "2026-05-10T10:00:00Z",
                            "notesMarkdown": "$sensitiveNotes",
                            "outcomesMarkdown": "$sensitiveOutcomes",
                            "sensitive": true
                        }
                    """.trimIndent())
            ).andExpect(status().isCreated).andReturn()

            val entryId = objectMapper.readTree(result.response.contentAsString).get("id").asText()

            // Verify the raw database content is NOT plaintext
            val rawNotes = jdbcTemplate.queryForObject(
                "SELECT notes_markdown FROM one_on_one_entries WHERE id = ?::uuid",
                String::class.java,
                entryId
            )
            val rawOutcomes = jdbcTemplate.queryForObject(
                "SELECT outcomes_markdown FROM one_on_one_entries WHERE id = ?::uuid",
                String::class.java,
                entryId
            )

            rawNotes shouldNotBe sensitiveNotes
            rawNotes!! shouldNotContain "CONFIDENTIAL"
            rawOutcomes shouldNotBe sensitiveOutcomes
            rawOutcomes!! shouldNotContain "PRIVATE"
        }

        @Test
        fun `should decrypt sensitive 1-1 entry notes when read via API`() {
            val sensitiveNotes = "CONFIDENTIAL: Performance improvement plan discussed"

            val result = mockMvc.perform(
                post("/api/v1/persons/$personId/one-on-one-entries")
                    .with(authentication(authenticatedJwt(user.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "meetingDate": "2026-05-10T10:00:00Z",
                            "notesMarkdown": "$sensitiveNotes",
                            "sensitive": true
                        }
                    """.trimIndent())
            ).andExpect(status().isCreated).andReturn()

            val entryId = objectMapper.readTree(result.response.contentAsString).get("id").asText()

            // Read back via API — should be decrypted
            mockMvc.perform(
                get("/api/v1/persons/$personId/one-on-one-entries/$entryId")
                    .with(authentication(authenticatedJwt(user.id)))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.notesMarkdown").value(sensitiveNotes))
                .andExpect(jsonPath("$.sensitive").value(true))
        }

        @Test
        fun `should store non-sensitive 1-1 entry notes in plaintext`() {
            val plainNotes = "Regular meeting notes about project progress"

            val result = mockMvc.perform(
                post("/api/v1/persons/$personId/one-on-one-entries")
                    .with(authentication(authenticatedJwt(user.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "meetingDate": "2026-05-10T11:00:00Z",
                            "notesMarkdown": "$plainNotes",
                            "sensitive": false
                        }
                    """.trimIndent())
            ).andExpect(status().isCreated).andReturn()

            val entryId = objectMapper.readTree(result.response.contentAsString).get("id").asText()

            // Non-sensitive content should be stored as plaintext
            val rawNotes = jdbcTemplate.queryForObject(
                "SELECT notes_markdown FROM one_on_one_entries WHERE id = ?::uuid",
                String::class.java,
                entryId
            )

            rawNotes shouldBe plainNotes
        }

        @Test
        fun `should encrypt content when entry is updated to sensitive`() {
            val notes = "Initially non-sensitive but will become sensitive"

            // Create as non-sensitive
            val result = mockMvc.perform(
                post("/api/v1/persons/$personId/one-on-one-entries")
                    .with(authentication(authenticatedJwt(user.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "meetingDate": "2026-05-10T12:00:00Z",
                            "notesMarkdown": "$notes",
                            "sensitive": false
                        }
                    """.trimIndent())
            ).andExpect(status().isCreated).andReturn()

            val entryId = objectMapper.readTree(result.response.contentAsString).get("id").asText()

            // Verify stored as plaintext
            val rawBefore = jdbcTemplate.queryForObject(
                "SELECT notes_markdown FROM one_on_one_entries WHERE id = ?::uuid",
                String::class.java,
                entryId
            )
            rawBefore shouldBe notes

            // Update to sensitive
            mockMvc.perform(
                put("/api/v1/persons/$personId/one-on-one-entries/$entryId")
                    .with(authentication(authenticatedJwt(user.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "notesMarkdown": "$notes",
                            "sensitive": true
                        }
                    """.trimIndent())
            ).andExpect(status().isOk)

            // Verify now stored encrypted
            val rawAfter = jdbcTemplate.queryForObject(
                "SELECT notes_markdown FROM one_on_one_entries WHERE id = ?::uuid",
                String::class.java,
                entryId
            )
            rawAfter shouldNotBe notes
            rawAfter!! shouldNotContain "Initially non-sensitive"
        }
    }

    @Nested
    inner class QuickNoteEncryption {

        @Test
        fun `should store sensitive quick note text encrypted in database`() {
            val sensitiveText = "SENSITIVE: Employee shared personal family situation"

            val result = mockMvc.perform(
                post("/api/v1/quick-notes")
                    .with(authentication(authenticatedJwt(user.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"text": "$sensitiveText", "sensitive": true}""")
            ).andExpect(status().isCreated).andReturn()

            val noteId = objectMapper.readTree(result.response.contentAsString).get("id").asText()

            // Verify raw DB content is encrypted
            val rawText = jdbcTemplate.queryForObject(
                "SELECT text FROM quick_notes WHERE id = ?::uuid",
                String::class.java,
                noteId
            )

            rawText shouldNotBe sensitiveText
            rawText!! shouldNotContain "SENSITIVE"
            rawText shouldNotContain "family situation"
        }

        @Test
        fun `should decrypt sensitive quick note when read via API`() {
            val sensitiveText = "PRIVATE: Discussed salary concerns"

            val result = mockMvc.perform(
                post("/api/v1/quick-notes")
                    .with(authentication(authenticatedJwt(user.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"text": "$sensitiveText", "sensitive": true}""")
            ).andExpect(status().isCreated).andReturn()

            val noteId = objectMapper.readTree(result.response.contentAsString).get("id").asText()

            // Read back via API — should be decrypted
            mockMvc.perform(
                get("/api/v1/quick-notes/$noteId")
                    .with(authentication(authenticatedJwt(user.id)))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.text").value(sensitiveText))
                .andExpect(jsonPath("$.sensitive").value(true))
        }

        @Test
        fun `should store non-sensitive quick note in plaintext`() {
            val plainText = "Remember to schedule team lunch"

            val result = mockMvc.perform(
                post("/api/v1/quick-notes")
                    .with(authentication(authenticatedJwt(user.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"text": "$plainText", "sensitive": false}""")
            ).andExpect(status().isCreated).andReturn()

            val noteId = objectMapper.readTree(result.response.contentAsString).get("id").asText()

            val rawText = jdbcTemplate.queryForObject(
                "SELECT text FROM quick_notes WHERE id = ?::uuid",
                String::class.java,
                noteId
            )

            rawText shouldBe plainText
        }
    }

    @Nested
    inner class PdpUpdateEncryption {

        private lateinit var goalId: String

        @BeforeEach
        fun createGoal() {
            val goalResult = mockMvc.perform(
                post("/api/v1/persons/$personId/pdp-goals")
                    .with(authentication(authenticatedJwt(user.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "title": "Improve communication",
                            "description": "Work on presentation skills",
                            "targetDate": "2026-12-31"
                        }
                    """.trimIndent())
            ).andExpect(status().isCreated).andReturn()

            goalId = objectMapper.readTree(goalResult.response.contentAsString).get("id").asText()
        }

        @Test
        fun `should store sensitive PDP update text encrypted in database`() {
            val sensitiveUpdate = "CONFIDENTIAL: Struggling with anxiety affecting work"

            val result = mockMvc.perform(
                post("/api/v1/persons/$personId/pdp-goals/$goalId/updates")
                    .with(authentication(authenticatedJwt(user.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"textMarkdown": "$sensitiveUpdate", "sensitive": true}""")
            ).andExpect(status().isCreated).andReturn()

            val updateId = objectMapper.readTree(result.response.contentAsString).get("id").asText()

            // Verify raw DB content is encrypted
            val rawText = jdbcTemplate.queryForObject(
                "SELECT text_markdown FROM pdp_updates WHERE id = ?::uuid",
                String::class.java,
                updateId
            )

            rawText shouldNotBe sensitiveUpdate
            rawText!! shouldNotContain "CONFIDENTIAL"
            rawText shouldNotContain "anxiety"
        }

        @Test
        fun `should decrypt sensitive PDP update when read via API`() {
            val sensitiveUpdate = "PRIVATE: Personal challenges discussed"

            mockMvc.perform(
                post("/api/v1/persons/$personId/pdp-goals/$goalId/updates")
                    .with(authentication(authenticatedJwt(user.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"textMarkdown": "$sensitiveUpdate", "sensitive": true}""")
            ).andExpect(status().isCreated)

            // Read back via API — should be decrypted
            mockMvc.perform(
                get("/api/v1/persons/$personId/pdp-goals/$goalId/updates")
                    .with(authentication(authenticatedJwt(user.id)))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content[0].textMarkdown").value(sensitiveUpdate))
                .andExpect(jsonPath("$.content[0].sensitive").value(true))
        }

        @Test
        fun `should store non-sensitive PDP update in plaintext`() {
            val plainUpdate = "Completed presentation skills workshop"

            val result = mockMvc.perform(
                post("/api/v1/persons/$personId/pdp-goals/$goalId/updates")
                    .with(authentication(authenticatedJwt(user.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"textMarkdown": "$plainUpdate", "sensitive": false}""")
            ).andExpect(status().isCreated).andReturn()

            val updateId = objectMapper.readTree(result.response.contentAsString).get("id").asText()

            val rawText = jdbcTemplate.queryForObject(
                "SELECT text_markdown FROM pdp_updates WHERE id = ?::uuid",
                String::class.java,
                updateId
            )

            rawText shouldBe plainUpdate
        }
    }
}
