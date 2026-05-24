package com.peoplemanager.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.peoplemanager.adapters.persistence.JpaUserRepositoryAdapter
import com.peoplemanager.domain.User
import com.peoplemanager.domain.UserId
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
 * Full-stack integration test for Quick Notes.
 *
 * Verifies:
 * - CRUD operations work end-to-end with real PostgreSQL
 * - userId scoping is enforced (data isolation between managers)
 * - Status transitions work correctly
 * - Cross-user access returns 404
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class QuickNoteIntegrationTest {

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

        // Create two users
        userA = userRepository.save(User(
            id = UserId.generate(),
            oidcSubject = "subject-a",
            oidcIssuer = "http://localhost:9000",
            displayName = "Manager A",
            email = "a@test.com"
        ))
        userB = userRepository.save(User(
            id = UserId.generate(),
            oidcSubject = "subject-b",
            oidcIssuer = "http://localhost:9000",
            displayName = "Manager B",
            email = "b@test.com"
        ))

        // Create a person for user A
        val personResult = mockMvc.perform(
            post("/api/v1/persons")
                .with(authentication(authenticatedJwt(userA.id)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name": "Alice Smith", "roleTitle": "Engineer"}""")
        ).andExpect(status().isCreated).andReturn()

        personAId = objectMapper.readTree(personResult.response.contentAsString).get("id").asText()
    }

    @Nested
    inner class CrudOperations {

        @Test
        fun `should create a quick note without person`() {
            mockMvc.perform(
                post("/api/v1/quick-notes")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"text": "Remember to check in with team"}""")
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.text").value("Remember to check in with team"))
                .andExpect(jsonPath("$.status").value("INBOX"))
                .andExpect(jsonPath("$.sensitive").value(false))
                .andExpect(jsonPath("$.personId").isEmpty)
        }

        @Test
        fun `should create a quick note with person`() {
            mockMvc.perform(
                post("/api/v1/quick-notes")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"text": "Discuss promotion", "personId": "$personAId"}""")
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.text").value("Discuss promotion"))
                .andExpect(jsonPath("$.personId").value(personAId))
        }

        @Test
        fun `should create a sensitive quick note`() {
            mockMvc.perform(
                post("/api/v1/quick-notes")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"text": "Health situation", "sensitive": true}""")
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.sensitive").value(true))
        }

        @Test
        fun `should get a quick note by id`() {
            val createResult = mockMvc.perform(
                post("/api/v1/quick-notes")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"text": "Test note"}""")
            ).andExpect(status().isCreated).andReturn()

            val noteId = objectMapper.readTree(createResult.response.contentAsString).get("id").asText()

            mockMvc.perform(
                get("/api/v1/quick-notes/$noteId")
                    .with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(noteId))
                .andExpect(jsonPath("$.text").value("Test note"))
        }

        @Test
        fun `should update a quick note`() {
            val createResult = mockMvc.perform(
                post("/api/v1/quick-notes")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"text": "Original text"}""")
            ).andExpect(status().isCreated).andReturn()

            val noteId = objectMapper.readTree(createResult.response.contentAsString).get("id").asText()

            mockMvc.perform(
                put("/api/v1/quick-notes/$noteId")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"text": "Updated text", "sensitive": true}""")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.text").value("Updated text"))
                .andExpect(jsonPath("$.sensitive").value(true))
        }

        @Test
        fun `should delete a quick note`() {
            val createResult = mockMvc.perform(
                post("/api/v1/quick-notes")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"text": "To be deleted"}""")
            ).andExpect(status().isCreated).andReturn()

            val noteId = objectMapper.readTree(createResult.response.contentAsString).get("id").asText()

            mockMvc.perform(
                delete("/api/v1/quick-notes/$noteId")
                    .with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isNoContent)

            // Verify it's gone
            mockMvc.perform(
                get("/api/v1/quick-notes/$noteId")
                    .with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isNotFound)
        }

        @Test
        fun `should list quick notes with pagination`() {
            // Create 3 notes
            repeat(3) { i ->
                mockMvc.perform(
                    post("/api/v1/quick-notes")
                        .with(authentication(authenticatedJwt(userA.id)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"text": "Note $i"}""")
                ).andExpect(status().isCreated)
            }

            mockMvc.perform(
                get("/api/v1/quick-notes?page=0&size=2")
                    .with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
        }

        @Test
        fun `should filter quick notes by status`() {
            // Create a note and archive it
            val createResult = mockMvc.perform(
                post("/api/v1/quick-notes")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"text": "To archive"}""")
            ).andExpect(status().isCreated).andReturn()

            val noteId = objectMapper.readTree(createResult.response.contentAsString).get("id").asText()

            mockMvc.perform(
                post("/api/v1/quick-notes/$noteId/archive")
                    .with(authentication(authenticatedJwt(userA.id)))
            ).andExpect(status().isOk)

            // Create another note that stays in INBOX
            mockMvc.perform(
                post("/api/v1/quick-notes")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"text": "Still in inbox"}""")
            ).andExpect(status().isCreated)

            // Filter by INBOX
            mockMvc.perform(
                get("/api/v1/quick-notes?status=INBOX")
                    .with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].text").value("Still in inbox"))
        }
    }

    @Nested
    inner class StatusTransitions {

        @Test
        fun `should attach a quick note to a 1-1 entry`() {
            // First create a 1:1 entry for the person
            val entryResult = mockMvc.perform(
                post("/api/v1/persons/$personAId/one-on-one-entries")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"meetingDate": "2026-05-10T10:00:00Z", "notesMarkdown": "Test meeting"}""")
            ).andExpect(status().isCreated).andReturn()

            val entryId = objectMapper.readTree(entryResult.response.contentAsString).get("id").asText()

            // Create a quick note
            val createResult = mockMvc.perform(
                post("/api/v1/quick-notes")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"text": "Attach to 1:1"}""")
            ).andExpect(status().isCreated).andReturn()

            val noteId = objectMapper.readTree(createResult.response.contentAsString).get("id").asText()

            // Attach the note to the entry
            mockMvc.perform(
                post("/api/v1/quick-notes/$noteId/attach")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"entryId": "$entryId"}""")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.status").value("ATTACHED"))
                .andExpect(jsonPath("$.attachedEntryId").value(entryId))
        }

        @Test
        fun `should convert a quick note to action item`() {
            val createResult = mockMvc.perform(
                post("/api/v1/quick-notes")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"text": "Convert to action item"}""")
            ).andExpect(status().isCreated).andReturn()

            val noteId = objectMapper.readTree(createResult.response.contentAsString).get("id").asText()

            mockMvc.perform(
                post("/api/v1/quick-notes/$noteId/convert")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"personId": "$personAId"}""")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.status").value("CONVERTED"))
                .andExpect(jsonPath("$.personId").value(personAId))

            // Verify the action item was actually created for the person
            mockMvc.perform(
                get("/api/v1/persons/$personAId/action-items")
                    .with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content[0].title").value("Convert to action item"))
        }

        @Test
        fun `should archive a quick note`() {
            val createResult = mockMvc.perform(
                post("/api/v1/quick-notes")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"text": "Archive this"}""")
            ).andExpect(status().isCreated).andReturn()

            val noteId = objectMapper.readTree(createResult.response.contentAsString).get("id").asText()

            mockMvc.perform(
                post("/api/v1/quick-notes/$noteId/archive")
                    .with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.status").value("ARCHIVED"))
        }

        @Test
        fun `should reject attach on already attached note`() {
            // Create a 1:1 entry
            val entryResult = mockMvc.perform(
                post("/api/v1/persons/$personAId/one-on-one-entries")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"meetingDate": "2026-05-10T11:00:00Z", "notesMarkdown": "Meeting"}""")
            ).andExpect(status().isCreated).andReturn()

            val entryId = objectMapper.readTree(entryResult.response.contentAsString).get("id").asText()

            val createResult = mockMvc.perform(
                post("/api/v1/quick-notes")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"text": "Already attached"}""")
            ).andExpect(status().isCreated).andReturn()

            val noteId = objectMapper.readTree(createResult.response.contentAsString).get("id").asText()

            // Attach first time
            mockMvc.perform(
                post("/api/v1/quick-notes/$noteId/attach")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"entryId": "$entryId"}""")
            ).andExpect(status().isOk)

            // Try to attach again
            mockMvc.perform(
                post("/api/v1/quick-notes/$noteId/attach")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"entryId": "$entryId"}""")
            ).andExpect(status().isBadRequest)
        }
    }

    @Nested
    inner class AssignToPerson {

        @Test
        fun `should assign quick note to person`() {
            val createResult = mockMvc.perform(
                post("/api/v1/quick-notes")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"text": "Assign to Alice"}""")
            ).andExpect(status().isCreated).andReturn()

            val noteId = objectMapper.readTree(createResult.response.contentAsString).get("id").asText()

            mockMvc.perform(
                post("/api/v1/quick-notes/$noteId/assign")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"personId": "$personAId"}""")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.personId").value(personAId))
        }
    }

    @Nested
    inner class SelfAssignedNotes {

        @Test
        fun `should create a self-assigned quick note`() {
            mockMvc.perform(
                post("/api/v1/quick-notes")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"text": "My personal reminder", "selfAssigned": true}""")
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.text").value("My personal reminder"))
                .andExpect(jsonPath("$.selfAssigned").value(true))
                .andExpect(jsonPath("$.personId").isEmpty)
        }

        @Test
        fun `should filter quick notes by selfAssigned`() {
            // Create a self-assigned note
            mockMvc.perform(
                post("/api/v1/quick-notes")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"text": "Self note", "selfAssigned": true}""")
            ).andExpect(status().isCreated)

            // Create a regular note
            mockMvc.perform(
                post("/api/v1/quick-notes")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"text": "Regular note"}""")
            ).andExpect(status().isCreated)

            // Filter by selfAssigned=true
            mockMvc.perform(
                get("/api/v1/quick-notes?selfAssigned=true")
                    .with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].text").value("Self note"))
                .andExpect(jsonPath("$.content[0].selfAssigned").value(true))
        }

        @Test
        fun `should clear selfAssigned when assigning to person`() {
            // Create a self-assigned note
            val createResult = mockMvc.perform(
                post("/api/v1/quick-notes")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"text": "Was self-assigned", "selfAssigned": true}""")
            ).andExpect(status().isCreated).andReturn()

            val noteId = objectMapper.readTree(createResult.response.contentAsString).get("id").asText()

            // Assign to a person
            mockMvc.perform(
                post("/api/v1/quick-notes/$noteId/assign")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"personId": "$personAId"}""")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.selfAssigned").value(false))
                .andExpect(jsonPath("$.personId").value(personAId))

            // Should no longer appear in self-assigned filter
            mockMvc.perform(
                get("/api/v1/quick-notes?selfAssigned=true")
                    .with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.totalElements").value(0))
        }

        @Test
        fun `should not allow self-assigned and personId together on create`() {
            mockMvc.perform(
                post("/api/v1/quick-notes")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"text": "Invalid", "selfAssigned": true, "personId": "$personAId"}""")
            )
                .andExpect(status().isBadRequest)
        }

        @Test
        fun `should not allow user B to see user A self-assigned notes`() {
            // User A creates a self-assigned note
            mockMvc.perform(
                post("/api/v1/quick-notes")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"text": "A's private self note", "selfAssigned": true}""")
            ).andExpect(status().isCreated)

            // User B should see nothing
            mockMvc.perform(
                get("/api/v1/quick-notes?selfAssigned=true")
                    .with(authentication(authenticatedJwt(userB.id)))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.totalElements").value(0))
        }

        @Test
        fun `should assign existing note to self via endpoint`() {
            // Create a regular note
            val createResult = mockMvc.perform(
                post("/api/v1/quick-notes")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"text": "Assign me to myself later"}""")
            ).andExpect(status().isCreated).andReturn()

            val noteId = objectMapper.readTree(createResult.response.contentAsString).get("id").asText()

            // Assign to self
            mockMvc.perform(
                post("/api/v1/quick-notes/$noteId/assign-self")
                    .with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.selfAssigned").value(true))
                .andExpect(jsonPath("$.personId").isEmpty)

            // Should now appear in self-assigned filter
            mockMvc.perform(
                get("/api/v1/quick-notes?selfAssigned=true")
                    .with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].text").value("Assign me to myself later"))
        }
    }

    @Nested
    inner class DataIsolation {

        @Test
        fun `should not allow user B to see user A quick notes`() {
            // User A creates a note
            val createResult = mockMvc.perform(
                post("/api/v1/quick-notes")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"text": "Private note for A"}""")
            ).andExpect(status().isCreated).andReturn()

            val noteId = objectMapper.readTree(createResult.response.contentAsString).get("id").asText()

            // User B tries to access it
            mockMvc.perform(
                get("/api/v1/quick-notes/$noteId")
                    .with(authentication(authenticatedJwt(userB.id)))
            )
                .andExpect(status().isNotFound)
        }

        @Test
        fun `should not allow user B to delete user A quick notes`() {
            val createResult = mockMvc.perform(
                post("/api/v1/quick-notes")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"text": "A's note"}""")
            ).andExpect(status().isCreated).andReturn()

            val noteId = objectMapper.readTree(createResult.response.contentAsString).get("id").asText()

            mockMvc.perform(
                delete("/api/v1/quick-notes/$noteId")
                    .with(authentication(authenticatedJwt(userB.id)))
            )
                .andExpect(status().isNotFound)
        }

        @Test
        fun `should not allow user B to update user A quick notes`() {
            val createResult = mockMvc.perform(
                post("/api/v1/quick-notes")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"text": "A's private note"}""")
            ).andExpect(status().isCreated).andReturn()

            val noteId = objectMapper.readTree(createResult.response.contentAsString).get("id").asText()

            mockMvc.perform(
                put("/api/v1/quick-notes/$noteId")
                    .with(authentication(authenticatedJwt(userB.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"text": "Hacked!"}""")
            )
                .andExpect(status().isNotFound)
        }

        @Test
        fun `should only list user's own quick notes`() {
            // User A creates notes
            mockMvc.perform(
                post("/api/v1/quick-notes")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"text": "A's note 1"}""")
            ).andExpect(status().isCreated)

            mockMvc.perform(
                post("/api/v1/quick-notes")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"text": "A's note 2"}""")
            ).andExpect(status().isCreated)

            // User B creates a note
            mockMvc.perform(
                post("/api/v1/quick-notes")
                    .with(authentication(authenticatedJwt(userB.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"text": "B's note"}""")
            ).andExpect(status().isCreated)

            // User A should only see their own
            mockMvc.perform(
                get("/api/v1/quick-notes")
                    .with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.totalElements").value(2))

            // User B should only see their own
            mockMvc.perform(
                get("/api/v1/quick-notes")
                    .with(authentication(authenticatedJwt(userB.id)))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.totalElements").value(1))
        }
    }
}
