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
import io.kotest.matchers.collections.shouldContainExactly
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
    inner class Tags {

        @Test
        fun `should create a quick note with a tag`() {
            mockMvc.perform(
                post("/api/v1/quick-notes")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"text": "Tagged note", "selfAssigned": true, "tag": "hiring"}""")
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.tag").value("hiring"))
        }

        @Test
        fun `should reject a tag longer than 30 characters`() {
            val longTag = "a".repeat(31)
            mockMvc.perform(
                post("/api/v1/quick-notes")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"text": "Tagged note", "selfAssigned": true, "tag": "$longTag"}""")
            )
                .andExpect(status().isBadRequest)
        }

        @Test
        fun `should filter self-assigned notes by tag`() {
            mockMvc.perform(
                post("/api/v1/quick-notes")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"text": "Hiring note", "selfAssigned": true, "tag": "hiring"}""")
            ).andExpect(status().isCreated)

            mockMvc.perform(
                post("/api/v1/quick-notes")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"text": "Budget note", "selfAssigned": true, "tag": "budget"}""")
            ).andExpect(status().isCreated)

            mockMvc.perform(
                get("/api/v1/quick-notes?selfAssigned=true&tag=hiring")
                    .with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].text").value("Hiring note"))
                .andExpect(jsonPath("$.content[0].tag").value("hiring"))
        }

        @Test
        fun `should set and clear a tag via update`() {
            val createResult = mockMvc.perform(
                post("/api/v1/quick-notes")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"text": "Note to tag", "selfAssigned": true}""")
            ).andExpect(status().isCreated).andReturn()

            val noteId = objectMapper.readTree(createResult.response.contentAsString).get("id").asText()

            mockMvc.perform(
                put("/api/v1/quick-notes/$noteId")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"tag": "followup"}""")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.tag").value("followup"))

            // Explicitly clear the tag
            mockMvc.perform(
                put("/api/v1/quick-notes/$noteId")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"tag": null, "updateTag": true}""")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.tag").isEmpty)
        }
    }

    @Nested
    inner class ConvertToPersonLessTodo {

        @Test
        fun `should convert a self-assigned note into a person-less action item`() {
            val createResult = mockMvc.perform(
                post("/api/v1/quick-notes")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"text": "Prepare the budget deck", "selfAssigned": true}""")
            ).andExpect(status().isCreated).andReturn()

            val noteId = objectMapper.readTree(createResult.response.contentAsString).get("id").asText()

            // Convert without a personId -> person-less TODO
            mockMvc.perform(
                post("/api/v1/quick-notes/$noteId/convert")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{}""")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.status").value("CONVERTED"))
                .andExpect(jsonPath("$.personId").isEmpty)
                .andExpect(jsonPath("$.selfAssigned").value(true))

            // The person-less action item is reachable via the cross-person list
            // and links back to the originating quick note.
            mockMvc.perform(
                get("/api/v1/action-items")
                    .with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Prepare the budget deck"))
                .andExpect(jsonPath("$.content[0].personId").isEmpty)
                .andExpect(jsonPath("$.content[0].originatingQuickNoteId").value(noteId))
        }

        @Test
        fun `should convert with a due date`() {
            val createResult = mockMvc.perform(
                post("/api/v1/quick-notes")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"text": "Submit report", "selfAssigned": true}""")
            ).andExpect(status().isCreated).andReturn()

            val noteId = objectMapper.readTree(createResult.response.contentAsString).get("id").asText()

            mockMvc.perform(
                post("/api/v1/quick-notes/$noteId/convert")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"dueDate": "2026-07-01"}""")
            )
                .andExpect(status().isOk)

            mockMvc.perform(
                get("/api/v1/action-items")
                    .with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content[0].dueDate").value("2026-07-01"))
        }

        @Test
        fun `should complete a person-less action item via the by-id endpoint`() {
            val createResult = mockMvc.perform(
                post("/api/v1/quick-notes")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"text": "Call the vendor", "selfAssigned": true}""")
            ).andExpect(status().isCreated).andReturn()

            val noteId = objectMapper.readTree(createResult.response.contentAsString).get("id").asText()

            mockMvc.perform(
                post("/api/v1/quick-notes/$noteId/convert")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{}""")
            ).andExpect(status().isOk)

            val listResult = mockMvc.perform(
                get("/api/v1/action-items")
                    .with(authentication(authenticatedJwt(userA.id)))
            ).andExpect(status().isOk).andReturn()

            val actionItemId = objectMapper.readTree(listResult.response.contentAsString)
                .get("content").get(0).get("id").asText()

            mockMvc.perform(
                post("/api/v1/action-items/$actionItemId/complete")
                    .with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.status").value("DONE"))
        }

        @Test
        fun `should not allow user B to complete user A person-less action item`() {
            val createResult = mockMvc.perform(
                post("/api/v1/quick-notes")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"text": "A's private todo", "selfAssigned": true}""")
            ).andExpect(status().isCreated).andReturn()

            val noteId = objectMapper.readTree(createResult.response.contentAsString).get("id").asText()

            mockMvc.perform(
                post("/api/v1/quick-notes/$noteId/convert")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{}""")
            ).andExpect(status().isOk)

            val listResult = mockMvc.perform(
                get("/api/v1/action-items")
                    .with(authentication(authenticatedJwt(userA.id)))
            ).andExpect(status().isOk).andReturn()

            val actionItemId = objectMapper.readTree(listResult.response.contentAsString)
                .get("content").get(0).get("id").asText()

            // User B must not be able to reach it
            mockMvc.perform(
                post("/api/v1/action-items/$actionItemId/complete")
                    .with(authentication(authenticatedJwt(userB.id)))
            )
                .andExpect(status().isNotFound)
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

    @Nested
    inner class Reordering {

        private fun createSelfNote(text: String): String {
            val result = mockMvc.perform(
                post("/api/v1/quick-notes")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"text": "$text", "selfAssigned": true}""")
            ).andExpect(status().isCreated).andReturn()
            return objectMapper.readTree(result.response.contentAsString).get("id").asText()
        }

        private fun listSelfAssignedTexts(): List<String> {
            val result = mockMvc.perform(
                get("/api/v1/quick-notes?selfAssigned=true")
                    .with(authentication(authenticatedJwt(userA.id)))
            ).andExpect(status().isOk).andReturn()
            val content = objectMapper.readTree(result.response.contentAsString).get("content")
            return (0 until content.size()).map { content.get(it).get("text").asText() }
        }

        @Test
        fun `newest self-assigned note appears on top`() {
            createSelfNote("first")
            createSelfNote("second")
            createSelfNote("third")

            // Newest-first by default.
            listSelfAssignedTexts() shouldContainExactly listOf("third", "second", "first")
        }

        @Test
        fun `moving a note down swaps it with the note below`() {
            createSelfNote("first")   // ends bottom
            createSelfNote("second")  // ends middle
            val thirdId = createSelfNote("third") // ends top

            // Order is [third, second, first]; move "third" down -> [second, third, first]
            mockMvc.perform(
                post("/api/v1/quick-notes/$thirdId/move")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"direction": "DOWN"}""")
            ).andExpect(status().isOk)

            listSelfAssignedTexts() shouldContainExactly listOf("second", "third", "first")
        }

        @Test
        fun `moving a note up swaps it with the note above`() {
            createSelfNote("first")   // bottom
            val secondId = createSelfNote("second") // middle
            createSelfNote("third")   // top

            // Order [third, second, first]; move "second" up -> [second, third, first]
            mockMvc.perform(
                post("/api/v1/quick-notes/$secondId/move")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"direction": "UP"}""")
            ).andExpect(status().isOk)

            listSelfAssignedTexts() shouldContainExactly listOf("second", "third", "first")
        }

        @Test
        fun `reorder persists across requests`() {
            createSelfNote("alpha")
            val betaId = createSelfNote("beta") // top

            mockMvc.perform(
                post("/api/v1/quick-notes/$betaId/move")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"direction": "DOWN"}""")
            ).andExpect(status().isOk)

            // A fresh list request reflects the persisted order.
            listSelfAssignedTexts() shouldContainExactly listOf("alpha", "beta")
        }

        @Test
        fun `rejects an invalid direction`() {
            val id = createSelfNote("note")
            mockMvc.perform(
                post("/api/v1/quick-notes/$id/move")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"direction": "SIDEWAYS"}""")
            ).andExpect(status().isBadRequest)
        }

        @Test
        fun `should not allow user B to move user A note`() {
            val id = createSelfNote("A's note")
            mockMvc.perform(
                post("/api/v1/quick-notes/$id/move")
                    .with(authentication(authenticatedJwt(userB.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"direction": "UP"}""")
            ).andExpect(status().isNotFound)
        }

        @Test
        fun `drag-drop reorder moves a note to an absolute position`() {
            createSelfNote("first")  // bottom
            createSelfNote("second") // middle
            val thirdId = createSelfNote("third") // top

            // Order [third, second, first]; drag "third" (index 0) to index 2 -> [second, first, third]
            mockMvc.perform(
                post("/api/v1/quick-notes/$thirdId/reorder")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"targetIndex": 2}""")
            ).andExpect(status().isOk)

            listSelfAssignedTexts() shouldContainExactly listOf("second", "first", "third")
        }

        @Test
        fun `drag-drop reorder persists and clamps an out-of-range index`() {
            val aId = createSelfNote("alpha") // bottom
            createSelfNote("beta")            // top

            // Order [beta, alpha]; drag "alpha" (index 1) to a too-large index -> clamps to last, [beta, alpha] unchanged;
            // drag "beta" to index 99 -> clamps to last -> [alpha, beta]
            mockMvc.perform(
                post("/api/v1/quick-notes/$aId/reorder")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"targetIndex": 99}""")
            ).andExpect(status().isOk)

            listSelfAssignedTexts() shouldContainExactly listOf("beta", "alpha")
        }

        @Test
        fun `drag-drop reorder rejects a negative index`() {
            val id = createSelfNote("note")
            mockMvc.perform(
                post("/api/v1/quick-notes/$id/reorder")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"targetIndex": -1}""")
            ).andExpect(status().isBadRequest)
        }

        @Test
        fun `should not allow user B to reorder user A note`() {
            val id = createSelfNote("A's note")
            mockMvc.perform(
                post("/api/v1/quick-notes/$id/reorder")
                    .with(authentication(authenticatedJwt(userB.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"targetIndex": 0}""")
            ).andExpect(status().isNotFound)
        }
    }
}
