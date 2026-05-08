package com.peoplemanager.integration

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.ints.shouldBeGreaterThan

@SpringBootTest
@Testcontainers
class FlywayMigrationTest {

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

    @Test
    fun `all Flyway migrations apply cleanly`() {
        // If the context loaded with flyway.enabled=true and ddl-auto=validate,
        // migrations ran successfully and Hibernate validated the schema.
        // Verify Flyway history table exists and has entries.
        val migrationCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true",
            Int::class.java
        )!!
        migrationCount shouldBeGreaterThan 0
    }

    @Test
    fun `users table has expected columns`() {
        val columns = getColumnNames("users")
        columns shouldContainAll listOf(
            "id", "oidc_subject", "oidc_issuer", "display_name",
            "email", "created_at", "updated_at"
        )
    }

    @Test
    fun `persons table has expected columns`() {
        val columns = getColumnNames("persons")
        columns shouldContainAll listOf(
            "id", "user_id", "name", "preferred_name", "role_title",
            "timezone", "start_date", "email", "tags", "morale_status",
            "morale_note", "created_at", "updated_at"
        )
    }

    @Test
    fun `pinned_remember_items table has expected columns`() {
        val columns = getColumnNames("pinned_remember_items")
        columns shouldContainAll listOf(
            "id", "person_id", "text", "display_order", "created_at"
        )
    }

    @Test
    fun `users table has unique constraint on oidc_subject and oidc_issuer`() {
        val constraints = jdbcTemplate.queryForList(
            """
            SELECT con.conname
            FROM pg_constraint con
            JOIN pg_class rel ON rel.oid = con.conrelid
            JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
            WHERE rel.relname = 'users'
              AND con.contype = 'u'
            """,
            String::class.java
        )
        constraints.size shouldBe 1
        constraints[0] shouldBe "uq_users_oidc"
    }

    @Test
    fun `persons table has index on user_id`() {
        val indexes = getIndexNames("persons")
        indexes shouldContainAll listOf("idx_persons_user_id")
    }

    @Test
    fun `persons table has composite index on user_id and morale_status`() {
        val indexes = getIndexNames("persons")
        indexes shouldContainAll listOf("idx_persons_morale_status")
    }

    @Test
    fun `pinned_remember_items table has index on person_id`() {
        val indexes = getIndexNames("pinned_remember_items")
        indexes shouldContainAll listOf("idx_pinned_remember_items_person_id")
    }

    // --- one_on_one_series table tests ---

    @Test
    fun `one_on_one_series table has expected columns`() {
        val columns = getColumnNames("one_on_one_series")
        columns shouldContainAll listOf(
            "id", "user_id", "person_id", "cadence_type",
            "custom_interval_days", "template_markdown",
            "created_at", "updated_at"
        )
    }

    @Test
    fun `one_on_one_series table has unique constraint on user_id and person_id`() {
        val constraints = jdbcTemplate.queryForList(
            """
            SELECT con.conname
            FROM pg_constraint con
            JOIN pg_class rel ON rel.oid = con.conrelid
            JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
            WHERE rel.relname = 'one_on_one_series'
              AND con.contype = 'u'
            """,
            String::class.java
        )
        constraints.size shouldBe 1
        constraints[0] shouldBe "uq_one_on_one_series_user_person"
    }

    @Test
    fun `one_on_one_series table has index on user_id and person_id`() {
        val indexes = getIndexNames("one_on_one_series")
        indexes shouldContainAll listOf("idx_one_on_one_series_user_person")
    }

    @Test
    fun `one_on_one_series table has foreign keys to users and persons`() {
        val fkConstraints = getForeignKeyConstraints("one_on_one_series")
        fkConstraints.size shouldBe 2
        val referencedTables = fkConstraints.map { it["referenced_table"] }
        referencedTables shouldContainAll listOf("users", "persons")
    }

    // --- one_on_one_entries table tests ---

    @Test
    fun `one_on_one_entries table has expected columns`() {
        val columns = getColumnNames("one_on_one_entries")
        columns shouldContainAll listOf(
            "id", "user_id", "person_id", "meeting_date",
            "notes_markdown", "outcomes_markdown", "sensitive",
            "created_at", "updated_at"
        )
    }

    @Test
    fun `one_on_one_entries table has index on user_id and person_id`() {
        val indexes = getIndexNames("one_on_one_entries")
        indexes shouldContainAll listOf("idx_one_on_one_entries_user_person")
    }

    @Test
    fun `one_on_one_entries table has index on person_id and meeting_date`() {
        val indexes = getIndexNames("one_on_one_entries")
        indexes shouldContainAll listOf("idx_one_on_one_entries_person_date")
    }

    @Test
    fun `one_on_one_entries table has foreign keys to users and persons`() {
        val fkConstraints = getForeignKeyConstraints("one_on_one_entries")
        fkConstraints.size shouldBe 2
        val referencedTables = fkConstraints.map { it["referenced_table"] }
        referencedTables shouldContainAll listOf("users", "persons")
    }

    // --- agenda_items table tests ---

    @Test
    fun `agenda_items table has expected columns`() {
        val columns = getColumnNames("agenda_items")
        columns shouldContainAll listOf(
            "id", "entry_id", "text", "checked",
            "display_order", "created_at"
        )
    }

    @Test
    fun `agenda_items table has index on entry_id`() {
        val indexes = getIndexNames("agenda_items")
        indexes shouldContainAll listOf("idx_agenda_items_entry_id")
    }

    @Test
    fun `agenda_items table has foreign key to one_on_one_entries with cascade delete`() {
        val fkConstraints = getForeignKeyConstraints("agenda_items")
        fkConstraints.size shouldBe 1
        fkConstraints[0]["referenced_table"] shouldBe "one_on_one_entries"
        fkConstraints[0]["delete_rule"] shouldBe "CASCADE"
    }

    private fun getColumnNames(tableName: String): List<String> {
        return jdbcTemplate.queryForList(
            """
            SELECT column_name
            FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name = ?
            """,
            String::class.java,
            tableName
        )
    }

    private fun getIndexNames(tableName: String): List<String> {
        return jdbcTemplate.queryForList(
            """
            SELECT indexname
            FROM pg_indexes
            WHERE schemaname = 'public' AND tablename = ?
            """,
            String::class.java,
            tableName
        )
    }

    private fun getForeignKeyConstraints(tableName: String): List<Map<String, String>> {
        return jdbcTemplate.queryForList(
            """
            SELECT
                ccu.table_name AS referenced_table,
                rc.delete_rule AS delete_rule
            FROM information_schema.table_constraints tc
            JOIN information_schema.referential_constraints rc
                ON tc.constraint_name = rc.constraint_name
                AND tc.constraint_schema = rc.constraint_schema
            JOIN information_schema.constraint_column_usage ccu
                ON rc.unique_constraint_name = ccu.constraint_name
                AND rc.unique_constraint_schema = ccu.constraint_schema
            WHERE tc.table_name = ?
              AND tc.constraint_type = 'FOREIGN KEY'
              AND tc.table_schema = 'public'
            """,
            tableName
        ).map { row ->
            mapOf(
                "referenced_table" to (row["referenced_table"] as String),
                "delete_rule" to (row["delete_rule"] as String)
            )
        }
    }
}
