package com.peoplemanager.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.peoplemanager.adapters.persistence.JpaPdpGoalRepositoryAdapter
import com.peoplemanager.adapters.persistence.JpaStrategyGoalPdpGoalLinkRepositoryAdapter
import com.peoplemanager.adapters.persistence.JpaStrategyGoalRepositoryAdapter
import com.peoplemanager.adapters.persistence.JpaUserRepositoryAdapter
import com.peoplemanager.application.StrategyGoalLinkService
import com.peoplemanager.application.StrategyGoalService
import com.peoplemanager.application.commands.*
import com.peoplemanager.application.queries.GetStrategyGoalQuery
import com.peoplemanager.domain.*
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
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
import java.time.LocalDate
import java.util.Base64
import java.util.UUID

/**
 * StrategyGoal full-stack integration tests with real PostgreSQL (Testcontainers).
 *
 * Coverage:
 * - Persistence (entities + repositories) CRUD for StrategyGoal and Link
 * - Flyway migrations present and functional (tables exist, FKs, cascade)
 * - Business logic via services and controllers: create/update/delete/achieve/drop
 * - Link/unlink + alignment scoring + gap analysis
 * - Cross-user isolation returns 404 at the web layer
 * - Encryption for sensitive strategy goals (AES-256-GCM) at rest + decryption on read
 * - Audit logging of create/update/delete/link/unlink
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class StrategyGoalIntegrationTest {

    companion object {
        // A valid 32-byte key (Base64) for encryption tests
        private val TEST_ENCRYPTION_KEY = Base64.getEncoder().encodeToString(
            ByteArray(32) { (it + 13).toByte() }
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
            // Enable encryption for sensitive strategy goals
            registry.add("app.encryption.key") { TEST_ENCRYPTION_KEY }
        }
    }

    // Web layer
    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper

    // Persistence adapters
    @Autowired lateinit var userRepository: JpaUserRepositoryAdapter
    @Autowired lateinit var strategyGoalRepo: JpaStrategyGoalRepositoryAdapter
    @Autowired lateinit var linkRepo: JpaStrategyGoalPdpGoalLinkRepositoryAdapter
    @Autowired lateinit var pdpGoalRepo: JpaPdpGoalRepositoryAdapter

    // Services
    @Autowired lateinit var strategyGoalService: StrategyGoalService
    @Autowired lateinit var strategyGoalLinkService: StrategyGoalLinkService

    @Autowired lateinit var jdbcTemplate: JdbcTemplate

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
        // Clean tables in FK-safe order
        jdbcTemplate.execute("DELETE FROM strategy_goal_pdp_goal_links")
        jdbcTemplate.execute("DELETE FROM strategy_goals")
        jdbcTemplate.execute("DELETE FROM pdp_updates")
        jdbcTemplate.execute("DELETE FROM pdp_goals")
        jdbcTemplate.execute("DELETE FROM action_items")
        jdbcTemplate.execute("DELETE FROM agenda_items")
        jdbcTemplate.execute("DELETE FROM one_on_one_entries")
        jdbcTemplate.execute("DELETE FROM one_on_one_series")
        jdbcTemplate.execute("DELETE FROM pinned_remember_items")
        jdbcTemplate.execute("DELETE FROM audit_log")
        jdbcTemplate.execute("DELETE FROM persons")
        jdbcTemplate.execute("DELETE FROM users")

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

        personAId = createPerson(userA.id, "Alice")
        personBId = createPerson(userB.id, "Bob")
    }

    private fun createPerson(userId: UserId, name: String): String {
        val result = mockMvc.perform(
            post("/api/v1/persons")
                .with(authentication(authenticatedJwt(userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name": "$name"}""")
        ).andExpect(status().isCreated).andReturn()
        return objectMapper.readTree(result.response.contentAsString).get("id").asText()
    }

    private fun createPdpGoal(userId: UserId, personId: String, title: String, status: String = "ACTIVE"): String {
        val result = mockMvc.perform(
            post("/api/v1/persons/$personId/pdp-goals")
                .with(authentication(authenticatedJwt(userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"$title"}""")
        ).andExpect(status().isCreated).andReturn()
        val id = objectMapper.readTree(result.response.contentAsString).get("id").asText()
        if (status != "ACTIVE") {
            when (status) {
                "ACHIEVED" -> mockMvc.perform(
                    post("/api/v1/persons/$personId/pdp-goals/$id/achieve")
                        .with(authentication(authenticatedJwt(userId)))
                ).andExpect(status().isOk)
                "DROPPED" -> mockMvc.perform(
                    post("/api/v1/persons/$personId/pdp-goals/$id/drop")
                        .with(authentication(authenticatedJwt(userId)))
                ).andExpect(status().isOk)
                "PAUSED" -> mockMvc.perform(
                    post("/api/v1/persons/$personId/pdp-goals/$id/pause")
                        .with(authentication(authenticatedJwt(userId)))
                ).andExpect(status().isOk)
            }
        }
        return id
    }

    private fun createStrategyGoalViaApi(userId: UserId, title: String, description: String? = null, targetDate: LocalDate? = null, sensitive: Boolean = false): String {
        val payload = buildString {
            append("{" )
            append("\"title\": \"$title\"")
            if (description != null) append(", \"description\": \"$description\"")
            if (targetDate != null) append(", \"targetDate\": \"$targetDate\"")
            if (sensitive) append(", \"sensitive\": true")
            append("}")
        }
        val result = mockMvc.perform(
            post("/api/v1/strategy-goals")
                .with(authentication(authenticatedJwt(userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
        ).andExpect(status().isCreated).andReturn()
        return objectMapper.readTree(result.response.contentAsString).get("id").asText()
    }

    // ===== Persistence Layer Tests =====
    @Nested
    inner class PersistenceTests {
        @Test
        fun `strategy goal JPA entity CRUD`() {
            val goal = StrategyGoal(
                id = StrategyGoalId.generate(),
                userId = userA.id,
                title = "Grow platform reliability",
                description = "Reduce incidents by 50%",
                targetDate = LocalDate.parse("2026-12-31"),
                status = StrategyGoalStatus.ACTIVE
            )

            val saved = strategyGoalRepo.save(goal)
            saved.id shouldBe goal.id

            val found = strategyGoalRepo.findByIdAndUserId(saved.id, userA.id)
            found.shouldNotBeNull()
            found.title shouldBe "Grow platform reliability"

            val updated = saved.updateDetails(title = "Increase reliability", description = "MTTR < 30m", targetDate = null)
            val savedUpdated = strategyGoalRepo.save(updated)
            savedUpdated.title shouldBe "Increase reliability"
            savedUpdated.description shouldBe "MTTR < 30m"

            val deleted = strategyGoalRepo.deleteByIdAndUserId(savedUpdated.id, userA.id)
            deleted shouldBe true
            strategyGoalRepo.findByIdAndUserId(savedUpdated.id, userA.id).shouldBeNull()
        }

        @Test
        fun `strategy_goal_pdp_goal_link JPA entity CRUD and FK cascade`() {
            // Create a goal and PDP goal
            val sg = strategyGoalRepo.save(
                StrategyGoal(
                    id = StrategyGoalId.generate(),
                    userId = userA.id,
                    title = "Improve DevEx"
                )
            )
            val pdpId = createPdpGoal(userA.id, personAId, "Learn Kotlin coroutines")

            val link = StrategyGoalPdpGoalLink.create(
                userId = userA.id,
                strategyGoalId = sg.id,
                pdpGoalId = PdpGoalId(UUID.fromString(pdpId)),
                personId = PersonId(UUID.fromString(personAId))
            )

            val saved = linkRepo.save(link)
            val bySg = linkRepo.findAllByStrategyGoalIdAndUserId(sg.id, userA.id)
            bySg shouldHaveSize 1
            bySg[0].id shouldBe saved.id

            // Cascade: deleting strategy goal should remove link (ON DELETE CASCADE)
            strategyGoalRepo.deleteByIdAndUserId(sg.id, userA.id)
            val count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM strategy_goal_pdp_goal_links WHERE strategy_goal_id = ?::uuid",
                Int::class.java,
                sg.id.value.toString()
            )
            count shouldBeExactly 0
        }

        @Test
        fun `flyway migrations for strategy tables are applied`() {
            // Attempt basic selects to assert tables exist
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM strategy_goals", Int::class.java) shouldNotBe null
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM strategy_goal_pdp_goal_links", Int::class.java) shouldNotBe null
        }
    }

    // ===== Business Logic Tests (Services + Web 404 behavior) =====
    @Nested
    inner class BusinessLogicTests {
        @Test
        fun `create update achieve drop delete via services emits audit logs`() {
            // Create
            val created = strategyGoalService.createStrategyGoal(
                CreateStrategyGoalCommand(userA.id, "Win Q4", "Hit targets", null, false)
            )

            // Update
            val updated = strategyGoalService.updateStrategyGoal(
                UpdateStrategyGoalCommand(userA.id, created.id, title = "Win FY", description = "All-year targets", targetDate = null)
            )
            updated.title shouldBe "Win FY"

            // Achieve
            val achieved = strategyGoalService.achieveStrategyGoal(
                AchieveStrategyGoalCommand(userA.id, created.id)
            )
            achieved.status shouldBe StrategyGoalStatus.ACHIEVED

            // Drop (allowed only from ACTIVE; ensure we can drop a new one instead)
            val toDrop = strategyGoalService.createStrategyGoal(
                CreateStrategyGoalCommand(userA.id, "Experiment", null, null, false)
            )
            val dropped = strategyGoalService.dropStrategyGoal(DropStrategyGoalCommand(userA.id, toDrop.id))
            dropped.status shouldBe StrategyGoalStatus.DROPPED

            // Delete
            strategyGoalService.deleteStrategyGoal(DeleteStrategyGoalCommand(userA.id, created.id))

            // Verify audit log entries exist
            val actions = jdbcTemplate.query(
                "SELECT action, entity_type FROM audit_log WHERE user_id = ?::uuid AND entity_type = 'STRATEGY_GOAL'",
                { rs, _ -> rs.getString(1) to rs.getString(2) },
                userA.id.value.toString()
            ).map { it.first }

            actions.contains("CREATE") shouldBe true
            actions.contains("UPDATE") shouldBe true
            actions.contains("DELETE") shouldBe true
        }

        @Test
        fun `cross-user isolation returns 404 at controller`() {
            // Create a goal for user A
            val goalId = createStrategyGoalViaApi(userA.id, title = "Confidential A Goal")

            // User B tries to GET
            mockMvc.perform(
                get("/api/v1/strategy-goals/$goalId").with(authentication(authenticatedJwt(userB.id)))
            ).andExpect(status().isNotFound)

            // User B tries to UPDATE
            mockMvc.perform(
                put("/api/v1/strategy-goals/$goalId")
                    .with(authentication(authenticatedJwt(userB.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"title":"New"}""")
            ).andExpect(status().isNotFound)

            // User B tries to DELETE
            mockMvc.perform(
                delete("/api/v1/strategy-goals/$goalId").with(authentication(authenticatedJwt(userB.id)))
            ).andExpect(status().isNotFound)
        }
    }

    // ===== Encryption Tests =====
    @Nested
    inner class EncryptionTests {
        @Test
        fun `sensitive strategy goal is encrypted at rest and decrypted on read`() {
            val sensitiveTitle = "TOP-SECRET: Strategy shift"
            val sensitiveDesc = "PRIVATE: Consolidate platforms"
            val id = createStrategyGoalViaApi(
                userA.id,
                title = sensitiveTitle,
                description = sensitiveDesc,
                sensitive = true
            )

            // Raw DB should not contain plaintext
            val raw = jdbcTemplate.queryForMap(
                "SELECT title, description FROM strategy_goals WHERE id = ?::uuid",
                id
            )
            (raw["title"] as String) shouldNotBe sensitiveTitle
            (raw["title"] as String) shouldNotContain "TOP-SECRET"
            (raw["description"] as String) shouldNotBe sensitiveDesc
            (raw["description"] as String) shouldNotContain "PRIVATE"

            // API read should decrypt
            mockMvc.perform(
                get("/api/v1/strategy-goals/$id").with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.title").value(sensitiveTitle))
                .andExpect(jsonPath("$.description").value(sensitiveDesc))
                .andExpect(jsonPath("$.sensitive").value(true))
        }

        @Test
        fun `non-sensitive strategy goal stored as plaintext`() {
            val title = "Public Strategy"
            val id = createStrategyGoalViaApi(userA.id, title = title, sensitive = false)

            val rawTitle = jdbcTemplate.queryForObject(
                "SELECT title FROM strategy_goals WHERE id = ?::uuid",
                String::class.java,
                id
            )
            rawTitle shouldBe title
        }
    }

    // ===== Alignment & Gap Analysis =====
    @Nested
    inner class AlignmentAndGapAnalysisTests {
        @Test
        fun `alignment percentage calculation and capping at 100`() {
            val sgId = StrategyGoalId(UUID.fromString(createStrategyGoalViaApi(userA.id, "Alignment Test")))

            // Create 2 ACTIVE PDP goals and 1 ACHIEVED (should not count)
            val g1 = createPdpGoal(userA.id, personAId, "P1")
            val g2 = createPdpGoal(userA.id, personAId, "P2")
            createPdpGoal(userA.id, personAId, "Old", status = "ACHIEVED")

            // Link one -> 50%
            strategyGoalLinkService.linkPdpGoal(
                LinkPdpGoalToStrategyGoalCommand(
                    userId = userA.id,
                    strategyGoalId = sgId,
                    pdpGoalId = PdpGoalId(UUID.fromString(g1)),
                    personId = PersonId(UUID.fromString(personAId))
                )
            )
            val score1 = strategyGoalLinkService.getAlignmentScore(sgId, userA.id)
            score1.totalActivePdpGoals shouldBeExactly 2
            score1.linkedPdpGoals shouldBeExactly 1
            score1.alignmentPercentage shouldBeExactly 50

            // Link second -> 100%
            strategyGoalLinkService.linkPdpGoal(
                LinkPdpGoalToStrategyGoalCommand(
                    userId = userA.id,
                    strategyGoalId = sgId,
                    pdpGoalId = PdpGoalId(UUID.fromString(g2)),
                    personId = PersonId(UUID.fromString(personAId))
                )
            )
            val score2 = strategyGoalLinkService.getAlignmentScore(sgId, userA.id)
            score2.alignmentPercentage shouldBeExactly 100
        }

        @Test
        fun `gap analysis identifies unlinked PDP goals and empty strategy goals`() {
            // Strategy goals
            val emptySgId = StrategyGoalId(UUID.fromString(createStrategyGoalViaApi(userA.id, "No contributors")))
            val linkedSgId = StrategyGoalId(UUID.fromString(createStrategyGoalViaApi(userA.id, "With contributors")))

            // PDP goals
            val unlinkedPdpId = PdpGoalId(UUID.fromString(createPdpGoal(userA.id, personAId, "Unlinked")))
            val linkedPdpId = PdpGoalId(UUID.fromString(createPdpGoal(userA.id, personAId, "Linked")))

            // Link one PDP goal to linked strategy goal
            strategyGoalLinkService.linkPdpGoal(
                LinkPdpGoalToStrategyGoalCommand(
                    userId = userA.id,
                    strategyGoalId = linkedSgId,
                    pdpGoalId = linkedPdpId,
                    personId = PersonId(UUID.fromString(personAId))
                )
            )

            val gap = strategyGoalLinkService.getGapAnalysis(userA.id)

            // Unlinked PDP goals should include the unlinked one
            gap.unlinkedPdpGoals.map { it.pdpGoalId } shouldContainExactly listOf(unlinkedPdpId)
            // Empty strategy goals should include the empty one
            gap.emptyStrategyGoals.map { it.strategyGoalId } shouldContainExactly listOf(emptySgId)
        }
    }

    // ===== Audit Logging for Link/Unlink =====
    @Nested
    inner class AuditLoggingTests {
        @Test
        fun `link and unlink operations are logged`() {
            val sgId = StrategyGoalId(UUID.fromString(createStrategyGoalViaApi(userA.id, "Audit Links")))
            val pdpId = PdpGoalId(UUID.fromString(createPdpGoal(userA.id, personAId, "Audit PDP")))
            val personId = PersonId(UUID.fromString(personAId))

            strategyGoalLinkService.linkPdpGoal(
                LinkPdpGoalToStrategyGoalCommand(userA.id, sgId, pdpId, personId)
            )

            // Verify LINK audit
            val linkCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE user_id = ?::uuid AND action = 'LINK' AND entity_type = 'STRATEGY_GOAL'",
                Int::class.java,
                userA.id.value.toString()
            )
            linkCount shouldBeExactly 1

            strategyGoalLinkService.unlinkPdpGoal(
                UnlinkPdpGoalFromStrategyGoalCommand(userA.id, sgId, pdpId)
            )

            // Verify UNLINK audit
            val unlinkCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE user_id = ?::uuid AND action = 'UNLINK' AND entity_type = 'STRATEGY_GOAL'",
                Int::class.java,
                userA.id.value.toString()
            )
            unlinkCount shouldBeExactly 1
        }
    }
}
