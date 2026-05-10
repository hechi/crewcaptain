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
 * Full-stack integration test for Kudos.
 *
 * Verifies:
 * - CRUD operations work end-to-end with real PostgreSQL
 * - userId scoping is enforced (data isolation between managers)
 * - Cross-user access returns 404
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class KudosIntegrationTest {

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

    private fun createKudos(userId: UserId, personId: String, text: String, date: String = "2026-05-10", tags: List<String> = emptyList()): String {
        val tagsJson = objectMapper.writeValueAsString(tags)
        val body = """{"text": "$text", "date": "$date", "tags": $tagsJson}"""

        val result = mockMvc.perform(
            post("/api/v1/persons/$personId/kudos")
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
        fun `should create and retrieve kudos`() {
            val kudosId = createKudos(userA.id, personAId, "Great presentation!", "2026-05-10", listOf("impact"))

            mockMvc.perform(
                get("/api/v1/persons/$personAId/kudos/$kudosId")
                    .with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.text").value("Great presentation!"))
                .andExpect(jsonPath("$.date").value("2026-05-10"))
                .andExpect(jsonPath("$.tags[0]").value("impact"))
        }

        @Test
        fun `should create kudos without tags`() {
            val kudosId = createKudos(userA.id, personAId, "Well done!")

            mockMvc.perform(
                get("/api/v1/persons/$personAId/kudos/$kudosId")
                    .with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.text").value("Well done!"))
                .andExpect(jsonPath("$.tags").isArray)
                .andExpect(jsonPath("$.tags.length()").value(0))
        }

        @Test
        fun `should delete kudos`() {
            val kudosId = createKudos(userA.id, personAId, "To delete")

            mockMvc.perform(
                delete("/api/v1/persons/$personAId/kudos/$kudosId")
                    .with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isNoContent)

            // Verify it's gone
            mockMvc.perform(
                get("/api/v1/persons/$personAId/kudos/$kudosId")
                    .with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isNotFound)
        }

        @Test
        fun `should list kudos by person`() {
            createKudos(userA.id, personAId, "Kudos 1")
            createKudos(userA.id, personAId, "Kudos 2")

            mockMvc.perform(
                get("/api/v1/persons/$personAId/kudos")
                    .with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(2))
        }

        @Test
        fun `should list kudos sorted by date descending`() {
            createKudos(userA.id, personAId, "Older kudos", "2026-03-01")
            createKudos(userA.id, personAId, "Newer kudos", "2026-05-10")

            mockMvc.perform(
                get("/api/v1/persons/$personAId/kudos")
                    .with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content[0].text").value("Newer kudos"))
                .andExpect(jsonPath("$.content[1].text").value("Older kudos"))
        }
    }

    @Nested
    inner class DataIsolationTests {

        @Test
        fun `manager B cannot read manager A's kudos`() {
            val kudosId = createKudos(userA.id, personAId, "Private to A")

            // User B tries to access User A's kudos
            mockMvc.perform(
                get("/api/v1/persons/$personAId/kudos/$kudosId")
                    .with(authentication(authenticatedJwt(userB.id)))
            )
                .andExpect(status().isNotFound)
        }

        @Test
        fun `manager B cannot list manager A's kudos`() {
            createKudos(userA.id, personAId, "A's kudos 1")
            createKudos(userA.id, personAId, "A's kudos 2")

            // User B tries to list User A's person's kudos
            mockMvc.perform(
                get("/api/v1/persons/$personAId/kudos")
                    .with(authentication(authenticatedJwt(userB.id)))
            )
                .andExpect(status().isNotFound) // Person not found for user B
        }

        @Test
        fun `manager B cannot delete manager A's kudos`() {
            val kudosId = createKudos(userA.id, personAId, "A's kudos")

            mockMvc.perform(
                delete("/api/v1/persons/$personAId/kudos/$kudosId")
                    .with(authentication(authenticatedJwt(userB.id)))
            )
                .andExpect(status().isNotFound)
        }

        @Test
        fun `cross-user list all kudos only returns own items`() {
            createKudos(userA.id, personAId, "A's kudos")
            createKudos(userB.id, personBId, "B's kudos")

            // User A should only see their own
            mockMvc.perform(
                get("/api/v1/kudos")
                    .with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].text").value("A's kudos"))

            // User B should only see their own
            mockMvc.perform(
                get("/api/v1/kudos")
                    .with(authentication(authenticatedJwt(userB.id)))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].text").value("B's kudos"))
        }
    }
}
