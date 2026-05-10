package com.peoplemanager.integration

import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID

@SpringBootTest
@Testcontainers
class FullTextSearchGinIndexIntegrationTest {

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
    lateinit var jdbcTemplate: JdbcTemplate

    // --- GIN index existence tests ---

    @Test
    fun `persons table has GIN index for full-text search`() {
        val indexes = getGinIndexNames("persons")
        indexes shouldContainAll listOf("idx_persons_fts")
    }

    @Test
    fun `one_on_one_entries table has GIN index for full-text search`() {
        val indexes = getGinIndexNames("one_on_one_entries")
        indexes shouldContainAll listOf("idx_one_on_one_entries_fts")
    }

    @Test
    fun `quick_notes table has GIN index for full-text search`() {
        val indexes = getGinIndexNames("quick_notes")
        indexes shouldContainAll listOf("idx_quick_notes_fts")
    }

    @Test
    fun `action_items table has GIN index for full-text search`() {
        val indexes = getGinIndexNames("action_items")
        indexes shouldContainAll listOf("idx_action_items_fts")
    }

    @Test
    fun `pdp_goals table has GIN index for full-text search`() {
        val indexes = getGinIndexNames("pdp_goals")
        indexes shouldContainAll listOf("idx_pdp_goals_fts")
    }

    @Test
    fun `pdp_updates table has GIN index for full-text search`() {
        val indexes = getGinIndexNames("pdp_updates")
        indexes shouldContainAll listOf("idx_pdp_updates_fts")
    }

    @Test
    fun `kudos table has GIN index for full-text search`() {
        val indexes = getGinIndexNames("kudos")
        indexes shouldContainAll listOf("idx_kudos_fts")
    }

    // --- Search vector function existence tests ---

    @Test
    fun `persons_search_vector function exists and is immutable`() {
        verifyFunctionIsImmutable("persons_search_vector")
    }

    @Test
    fun `one_on_one_entries_search_vector function exists and is immutable`() {
        verifyFunctionIsImmutable("one_on_one_entries_search_vector")
    }

    @Test
    fun `quick_notes_search_vector function exists and is immutable`() {
        verifyFunctionIsImmutable("quick_notes_search_vector")
    }

    @Test
    fun `action_items_search_vector function exists and is immutable`() {
        verifyFunctionIsImmutable("action_items_search_vector")
    }

    @Test
    fun `pdp_goals_search_vector function exists and is immutable`() {
        verifyFunctionIsImmutable("pdp_goals_search_vector")
    }

    @Test
    fun `pdp_updates_search_vector function exists and is immutable`() {
        verifyFunctionIsImmutable("pdp_updates_search_vector")
    }

    @Test
    fun `kudos_search_vector function exists and is immutable`() {
        verifyFunctionIsImmutable("kudos_search_vector")
    }

    // --- Functional tests: search using the wrapper functions works ---

    @Test
    fun `persons can be searched using persons_search_vector`() {
        val userId = createTestUser()
        jdbcTemplate.update(
            """
            INSERT INTO persons (id, user_id, name, preferred_name, role_title, email, tags, morale_status, created_at, updated_at)
            VALUES (gen_random_uuid(), ?, 'Alice Johnson', 'Ali', 'Senior Engineer', 'alice@example.com', '{kotlin,spring}', 'GREEN', NOW(), NOW())
            """,
            userId
        )

        val count = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*) FROM persons
            WHERE user_id = ?
              AND persons_search_vector(name, preferred_name, role_title, email, tags)
                  @@ to_tsquery('english', 'alice')
            """,
            Long::class.java,
            userId
        )!!
        count shouldBeGreaterThan 0L
    }

    @Test
    fun `action_items can be searched using action_items_search_vector`() {
        val userId = createTestUser()
        val personId = createTestPerson(userId)
        jdbcTemplate.update(
            """
            INSERT INTO action_items (id, user_id, person_id, title, description, status, created_at, updated_at)
            VALUES (gen_random_uuid(), ?, ?, 'Review quarterly performance report', 'Check metrics and KPIs', 'OPEN', NOW(), NOW())
            """,
            userId, personId
        )

        val count = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*) FROM action_items
            WHERE user_id = ?
              AND action_items_search_vector(title, description)
                  @@ to_tsquery('english', 'quarterly & performance')
            """,
            Long::class.java,
            userId
        )!!
        count shouldBeGreaterThan 0L
    }

    @Test
    fun `quick_notes can be searched using quick_notes_search_vector`() {
        val userId = createTestUser()
        jdbcTemplate.update(
            """
            INSERT INTO quick_notes (id, user_id, text, sensitive, status, created_at, updated_at)
            VALUES (gen_random_uuid(), ?, 'Remember to discuss promotion timeline with team lead', false, 'INBOX', NOW(), NOW())
            """,
            userId
        )

        val count = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*) FROM quick_notes
            WHERE user_id = ?
              AND quick_notes_search_vector(text)
                  @@ to_tsquery('english', 'promotion & timeline')
            """,
            Long::class.java,
            userId
        )!!
        count shouldBeGreaterThan 0L
    }

    @Test
    fun `kudos can be searched by text and tags using kudos_search_vector`() {
        val userId = createTestUser()
        val personId = createTestPerson(userId)
        jdbcTemplate.update(
            """
            INSERT INTO kudos (id, user_id, person_id, date, text, tags, created_at, updated_at)
            VALUES (gen_random_uuid(), ?, ?, '2026-05-10', 'Great presentation at the all-hands meeting', '{leadership,communication}', NOW(), NOW())
            """,
            userId, personId
        )

        val count = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*) FROM kudos
            WHERE user_id = ?
              AND kudos_search_vector(text, tags)
                  @@ to_tsquery('english', 'presentation')
            """,
            Long::class.java,
            userId
        )!!
        count shouldBeGreaterThan 0L

        // Also verify tag content is searchable
        val tagCount = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*) FROM kudos
            WHERE user_id = ?
              AND kudos_search_vector(text, tags)
                  @@ to_tsquery('english', 'leadership')
            """,
            Long::class.java,
            userId
        )!!
        tagCount shouldBeGreaterThan 0L
    }

    @Test
    fun `one_on_one_entries can be searched using one_on_one_entries_search_vector`() {
        val userId = createTestUser()
        val personId = createTestPerson(userId)
        jdbcTemplate.update(
            """
            INSERT INTO one_on_one_entries (id, user_id, person_id, meeting_date, notes_markdown, outcomes_markdown, sensitive, created_at, updated_at)
            VALUES (gen_random_uuid(), ?, ?, NOW(), 'Discussed career development and mentoring opportunities', 'Agreed to find a mentor by next month', false, NOW(), NOW())
            """,
            userId, personId
        )

        val count = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*) FROM one_on_one_entries
            WHERE user_id = ?
              AND one_on_one_entries_search_vector(notes_markdown, outcomes_markdown)
                  @@ to_tsquery('english', 'career & development')
            """,
            Long::class.java,
            userId
        )!!
        count shouldBeGreaterThan 0L
    }

    @Test
    fun `pdp_goals can be searched using pdp_goals_search_vector`() {
        val userId = createTestUser()
        val personId = createTestPerson(userId)
        jdbcTemplate.update(
            """
            INSERT INTO pdp_goals (id, user_id, person_id, title, description, status, created_at, updated_at)
            VALUES (gen_random_uuid(), ?, ?, 'Improve public speaking skills', 'Practice presentations and attend Toastmasters', 'ACTIVE', NOW(), NOW())
            """,
            userId, personId
        )

        val count = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*) FROM pdp_goals
            WHERE user_id = ?
              AND pdp_goals_search_vector(title, description)
                  @@ to_tsquery('english', 'speaking & skills')
            """,
            Long::class.java,
            userId
        )!!
        count shouldBeGreaterThan 0L
    }

    @Test
    fun `pdp_updates can be searched using pdp_updates_search_vector`() {
        val userId = createTestUser()
        val personId = createTestPerson(userId)
        val goalId = UUID.randomUUID()
        jdbcTemplate.update(
            """
            INSERT INTO pdp_goals (id, user_id, person_id, title, status, created_at, updated_at)
            VALUES (?, ?, ?, 'Test Goal', 'ACTIVE', NOW(), NOW())
            """,
            goalId, userId, personId
        )
        jdbcTemplate.update(
            """
            INSERT INTO pdp_updates (id, goal_id, user_id, text_markdown, sensitive, created_at)
            VALUES (gen_random_uuid(), ?, ?, 'Completed the advanced Kubernetes certification course', false, NOW())
            """,
            goalId, userId
        )

        val count = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*) FROM pdp_updates
            WHERE user_id = ?
              AND pdp_updates_search_vector(text_markdown)
                  @@ to_tsquery('english', 'kubernetes & certification')
            """,
            Long::class.java,
            userId
        )!!
        count shouldBeGreaterThan 0L
    }

    // --- Helper methods ---

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
            """,
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
            """,
            String::class.java,
            functionName
        )
        result.size shouldBe 1
        result[0] shouldBe "i" // 'i' = immutable
    }

    private fun createTestUser(): UUID {
        val userId = UUID.randomUUID()
        jdbcTemplate.update(
            """
            INSERT INTO users (id, oidc_subject, oidc_issuer, display_name, email, created_at, updated_at)
            VALUES (?, ?, 'http://localhost:9000', 'Test User', 'test@example.com', NOW(), NOW())
            """,
            userId, "test-subject-${userId}"
        )
        return userId
    }

    private fun createTestPerson(userId: UUID): UUID {
        val personId = UUID.randomUUID()
        jdbcTemplate.update(
            """
            INSERT INTO persons (id, user_id, name, morale_status, created_at, updated_at)
            VALUES (?, ?, 'Test Person', 'GREEN', NOW(), NOW())
            """,
            personId, userId
        )
        return personId
    }
}
