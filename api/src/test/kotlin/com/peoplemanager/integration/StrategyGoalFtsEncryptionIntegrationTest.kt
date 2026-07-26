package com.peoplemanager.integration

import tools.jackson.databind.ObjectMapper
import com.peoplemanager.adapters.persistence.JpaUserRepositoryAdapter
import com.peoplemanager.domain.User
import com.peoplemanager.domain.UserId
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotContain
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
import java.util.Base64
import java.util.UUID

/**
 * StrategyGoal FTS + Encryption Integration Tests
 *
 * Coverage:
 * - GIN Index Tests: index exists, function immutable, functional search, and userId scoping
 * - Encryption + Search Trade-off: non-sensitive appear, sensitive excluded, encrypted fields unsearchable
 * - Sensitive Flag Tests: storage encrypted when sensitive=true, plaintext when false, API decrypts
 *
 * Patterns are based on:
 * - FullTextSearchGinIndexIntegrationTest
 * - EncryptionIntegrationTest
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class StrategyGoalFtsEncryptionIntegrationTest {

    companion object {
        // Valid 32-byte key for tests
        private val TEST_ENCRYPTION_KEY = Base64.getEncoder().encodeToString(
            ByteArray(32) { (it + 11).toByte() }
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

    @Autowired lateinit var jdbcTemplate: JdbcTemplate
    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper
    @Autowired lateinit var userRepository: JpaUserRepositoryAdapter

    private lateinit var userA: User
    private lateinit var userB: User

    private fun authenticatedJwt(userId: UserId): JwtAuthenticationToken {
        val jwt = Jwt.withTokenValue("test-token")
            .header("alg", "RS256")
            .subject("test-subject-${'$'}{userId.value}")
            .issuer("http://localhost:9000")
            .claim("name", "Test User")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build()
        val authorities = listOf(SimpleGrantedAuthority("ROLE_USER"))
        val token = JwtAuthenticationToken(jwt, authorities, "test-subject-${'$'}{userId.value}")
        token.details = userId
        return token
    }

    @BeforeEach
    fun setup() {
        // Clean tables we touch
        jdbcTemplate.execute("DELETE FROM strategy_goal_pdp_goal_links")
        jdbcTemplate.execute("DELETE FROM strategy_goals")
        jdbcTemplate.execute("DELETE FROM pdp_updates")
        jdbcTemplate.execute("DELETE FROM pdp_goals")
        jdbcTemplate.execute("DELETE FROM action_items")
        jdbcTemplate.execute("DELETE FROM quick_notes")
        jdbcTemplate.execute("DELETE FROM one_on_one_entries")
        jdbcTemplate.execute("DELETE FROM persons")
        jdbcTemplate.execute("DELETE FROM users")

        // Create two users
        userA = userRepository.save(
            User(
                id = UserId.generate(),
                oidcSubject = "subject-a",
                oidcIssuer = "http://localhost:9000",
                displayName = "Manager A",
                email = "a@test.com"
            )
        )
        userB = userRepository.save(
            User(
                id = UserId.generate(),
                oidcSubject = "subject-b",
                oidcIssuer = "http://localhost:9000",
                displayName = "Manager B",
                email = "b@test.com"
            )
        )
    }

    // --- GIN Index Tests ---

    @Test
    fun `strategy_goals table has GIN index and search function is immutable`() {
        // Index exists
        val indexes = getGinIndexNames("strategy_goals")
        indexes shouldContainAll listOf("idx_strategy_goals_fts")

        // Function immutable
        verifyFunctionIsImmutable("strategy_goals_search_vector")
    }

    @Test
    fun `search returns strategy goals matching query and respects user scoping`() {
        // Insert goals for A and B directly (non-sensitive by default)
        jdbcTemplate.update(
            """
            INSERT INTO strategy_goals (id, user_id, title, description, status, sensitive, created_at, updated_at)
            VALUES (gen_random_uuid(), ?, 'Improve Engineering Excellence', 'Adopt Kotlin best practices', 'ACTIVE', false, NOW(), NOW())
            """,
            userA.id.value
        )
        jdbcTemplate.update(
            """
            INSERT INTO strategy_goals (id, user_id, title, description, status, sensitive, created_at, updated_at)
            VALUES (gen_random_uuid(), ?, 'Marketing Push', 'Q3 brand refresh', 'ACTIVE', false, NOW(), NOW())
            """,
            userB.id.value
        )

        // Search as user A - should match only A's goal
        val countA = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*) FROM strategy_goals sg
            WHERE sg.user_id = ?
              AND strategy_goals_search_vector(sg.title, sg.description)
                  @@ to_tsquery('english', 'engineering')
            """.trimIndent(),
            Long::class.java,
            userA.id.value
        )!!
        countA shouldBeGreaterThan 0L

        // As user B searching for 'engineering' should be zero
        val countB = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*) FROM strategy_goals sg
            WHERE sg.user_id = ?
              AND strategy_goals_search_vector(sg.title, sg.description)
                  @@ to_tsquery('english', 'engineering')
            """.trimIndent(),
            Long::class.java,
            userB.id.value
        )!!
        countB shouldBe 0L
    }

    // --- Encryption + Search Trade-off Tests ---

    @Test
    fun `non-sensitive strategy goals appear in search, sensitive are excluded and encrypted fields not searchable`() {
        // Create via API to exercise encryption adapter path as well
        val createAResult = mockMvc.perform(
            post("/api/v1/strategy-goals")
                .with(authentication(authenticatedJwt(userA.id)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{
                        "title": "Security hardening",
                        "description": "Adopt SAST + DAST across repos",
                        "sensitive": false
                    }""".trimIndent()
                )
        ).andExpect(status().isCreated).andReturn()

        val createSensitiveResult = mockMvc.perform(
            post("/api/v1/strategy-goals")
                .with(authentication(authenticatedJwt(userA.id)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{
                        "title": "Compensation Adjustment Plan",
                        "description": "PRIVATE salary band changes",
                        "sensitive": true
                    }""".trimIndent()
                )
        ).andExpect(status().isCreated).andReturn()

        val sensitiveId = objectMapper.readTree(createSensitiveResult.response.contentAsString).get("id").asText()

        // Search for a term present only in non-sensitive description
        val nonSensitiveCount = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*) FROM strategy_goals sg
            WHERE sg.user_id = ?
              AND sg.sensitive = false
              AND strategy_goals_search_vector(sg.title, sg.description)
                  @@ to_tsquery('english', 'sast | dast | security')
            """.trimIndent(),
            Long::class.java,
            userA.id.value
        )!!
        nonSensitiveCount shouldBeGreaterThan 0L

        // Search for a term present only in sensitive description/title → expect 0
        val sensitiveSearchCount = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*) FROM strategy_goals sg
            WHERE sg.user_id = ?
              AND strategy_goals_search_vector(sg.title, sg.description)
                  @@ to_tsquery('english', 'compensation | salary')
            """.trimIndent(),
            Long::class.java,
            userA.id.value
        )!!
        sensitiveSearchCount shouldBe 0L

        // Verify raw DB storage is encrypted for sensitive goal
        val rawTitle = jdbcTemplate.queryForObject(
            "SELECT title FROM strategy_goals WHERE id = ?::uuid",
            String::class.java,
            sensitiveId
        )
        val rawDescription = jdbcTemplate.queryForObject(
            "SELECT description FROM strategy_goals WHERE id = ?::uuid",
            String::class.java,
            sensitiveId
        )
        rawTitle shouldNotBe "Compensation Adjustment Plan"
        rawTitle!! shouldNotContain "Compensation"
        rawDescription shouldNotBe "PRIVATE salary band changes"
        rawDescription!! shouldNotContain "salary"
    }

    // --- Sensitive Flag Tests ---

    @Nested
    inner class SensitiveFlagStorageAndRead {
        @Test
        fun `sensitive strategy goal stored encrypted and decrypted on read`() {
            val result = mockMvc.perform(
                post("/api/v1/strategy-goals")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{
                            "title": "M&A Exploration",
                            "description": "Potential acquisition candidates",
                            "sensitive": true
                        }""".trimIndent()
                    )
            ).andExpect(status().isCreated).andReturn()

            val id = objectMapper.readTree(result.response.contentAsString).get("id").asText()

            // Raw DB should be encrypted
            val rawTitle = jdbcTemplate.queryForObject(
                "SELECT title FROM strategy_goals WHERE id = ?::uuid",
                String::class.java,
                id
            )
            val rawDesc = jdbcTemplate.queryForObject(
                "SELECT description FROM strategy_goals WHERE id = ?::uuid",
                String::class.java,
                id
            )
            rawTitle shouldNotBe "M&A Exploration"
            rawTitle!! shouldNotContain "M&A"
            rawDesc shouldNotBe "Potential acquisition candidates"
            rawDesc!! shouldNotContain "acquisition"

            // API read should decrypt
            mockMvc.perform(
                get("/api/v1/strategy-goals/$id")
                    .with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.title").value("M&A Exploration"))
                .andExpect(jsonPath("$.description").value("Potential acquisition candidates"))
                .andExpect(jsonPath("$.sensitive").value(true))
        }

        @Test
        fun `non-sensitive strategy goal stored plaintext`() {
            val title = "Developer Productivity"
            val description = "Reduce CI time and flakiness"
            val result = mockMvc.perform(
                post("/api/v1/strategy-goals")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{
                            "title": "$title",
                            "description": "$description",
                            "sensitive": false
                        }""".trimIndent()
                    )
            ).andExpect(status().isCreated).andReturn()

            val id = objectMapper.readTree(result.response.contentAsString).get("id").asText()

            val rawTitle = jdbcTemplate.queryForObject(
                "SELECT title FROM strategy_goals WHERE id = ?::uuid",
                String::class.java,
                id
            )
            val rawDesc = jdbcTemplate.queryForObject(
                "SELECT description FROM strategy_goals WHERE id = ?::uuid",
                String::class.java,
                id
            )

            rawTitle shouldBe title
            rawDesc shouldBe description
        }
    }

    // --- Helpers ---

    private fun getGinIndexNames(tableName: String): List<String> {
        return jdbcTemplate.queryForList(
            """
            SELECT i.relname AS index_name
            FROM pg_index ix
            JOIN pg_class t ON t.oid = ix.indrelid
            JOIN pg_class i ON i.oid = ix.indexrelid
            JOIN pg_am am ON am.oid = i.relam
            JOIN pg_namespace n ON n.oid = t.relnamespace
            WHERE t.relname = ?
              AND n.nspname = 'public'
              AND am.amname = 'gin'
            """.trimIndent(),
            String::class.java,
            tableName
        )
    }

    private fun verifyFunctionIsImmutable(functionName: String) {
        val result = jdbcTemplate.queryForList(
            """
            SELECT p.provolatile
            FROM pg_proc p
            JOIN pg_namespace n ON n.oid = p.pronamespace
            WHERE p.proname = ?
              AND n.nspname = 'public'
            """.trimIndent(),
            String::class.java,
            functionName
        )
        result.size shouldBe 1
        result[0] shouldBe "i" // 'i' = immutable
    }
}
