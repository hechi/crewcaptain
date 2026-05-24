package com.peoplemanager.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.peoplemanager.adapters.persistence.JpaUserRepositoryAdapter
import com.peoplemanager.domain.MoraleStatus
import com.peoplemanager.domain.User
import com.peoplemanager.domain.UserId
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.alphanumeric
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Instant
import java.util.UUID

/**
 * Full-stack integration tests that exercise Controller → Service → Repository → Database.
 * Uses Testcontainers for PostgreSQL and Spring Security Test for authentication.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class FullStackIntegrationTest {

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

    @BeforeEach
    fun setUp() {
        jdbcTemplate.execute("DELETE FROM pinned_remember_items")
        jdbcTemplate.execute("DELETE FROM persons")
        jdbcTemplate.execute("DELETE FROM users")

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

    private fun createPersonViaApi(userId: UserId, name: String, tags: List<String> = emptyList()): String {
        val requestBody = mapOf(
            "name" to name,
            "tags" to tags
        )
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

    private fun setMoraleViaApi(userId: UserId, personId: String, status: String, note: String? = null) {
        val requestBody = mutableMapOf<String, Any?>("status" to status)
        if (note != null) requestBody["note"] = note

        mockMvc.perform(
            put("/api/v1/persons/$personId/morale")
                .with(authentication(authenticatedJwt(userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody))
        ).andExpect(status().isOk)
    }

    // ===== Task 9.1: Property 6 - Data isolation across users =====

    /**
     * Property 6: Data isolation across users
     *
     * For any two distinct Users (A and B) and any Person belonging to User A,
     * User B attempting to retrieve, update, delete, set morale, or manage remember items
     * for that Person SHALL receive a 404 Not Found response.
     * Additionally, listing Persons as User B SHALL never include User A's Persons.
     *
     * **Validates: Requirements 3.2, 3.3, 4.4, 5.2, 5.3, 6.4, 8.5, 9.5, 12.4, 12.5**
     */
    @Nested
    inner class DataIsolationTests {

        @Test
        @Tag("property")
        fun `User B cannot GET User A's person - returns 404`() {
            val personId = createPersonViaApi(userA.id, "Alice's Person")

            mockMvc.perform(
                get("/api/v1/persons/$personId")
                    .with(authentication(authenticatedJwt(userB.id)))
            ).andExpect(status().isNotFound)
        }

        @Test
        @Tag("property")
        fun `User B cannot PUT (update) User A's person - returns 404`() {
            val personId = createPersonViaApi(userA.id, "Alice's Person")

            val updateRequest = mapOf("name" to "Hacked Name")
            mockMvc.perform(
                put("/api/v1/persons/$personId")
                    .with(authentication(authenticatedJwt(userB.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest))
            ).andExpect(status().isNotFound)
        }

        @Test
        @Tag("property")
        fun `User B cannot DELETE User A's person - returns 404`() {
            val personId = createPersonViaApi(userA.id, "Alice's Person")

            mockMvc.perform(
                delete("/api/v1/persons/$personId")
                    .with(authentication(authenticatedJwt(userB.id)))
            ).andExpect(status().isNotFound)
        }

        @Test
        @Tag("property")
        fun `User B cannot set morale on User A's person - returns 404`() {
            val personId = createPersonViaApi(userA.id, "Alice's Person")

            val moraleRequest = mapOf("status" to "GREEN", "note" to "Hacked morale")
            mockMvc.perform(
                put("/api/v1/persons/$personId/morale")
                    .with(authentication(authenticatedJwt(userB.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(moraleRequest))
            ).andExpect(status().isNotFound)
        }

        @Test
        @Tag("property")
        fun `User B cannot add remember items to User A's person - returns 404`() {
            val personId = createPersonViaApi(userA.id, "Alice's Person")

            val rememberRequest = mapOf("text" to "Hacked remember item")
            mockMvc.perform(
                post("/api/v1/persons/$personId/remember-items")
                    .with(authentication(authenticatedJwt(userB.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(rememberRequest))
            ).andExpect(status().isNotFound)
        }

        @Test
        @Tag("property")
        fun `User B cannot delete remember items from User A's person - returns 404`() {
            val personId = createPersonViaApi(userA.id, "Alice's Person")

            // Add a remember item as User A
            val addRequest = mapOf("text" to "Remember this")
            val addResult = mockMvc.perform(
                post("/api/v1/persons/$personId/remember-items")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(addRequest))
            ).andExpect(status().isCreated).andReturn()

            val items = objectMapper.readTree(addResult.response.contentAsString)
            val itemId = items[0].get("id").asText()

            // User B tries to delete it
            mockMvc.perform(
                delete("/api/v1/persons/$personId/remember-items/$itemId")
                    .with(authentication(authenticatedJwt(userB.id)))
            ).andExpect(status().isNotFound)
        }

        @Test
        @Tag("property")
        fun `list endpoint returns only authenticated user's persons`() {
            createPersonViaApi(userA.id, "Alice Person 1")
            createPersonViaApi(userA.id, "Alice Person 2")
            createPersonViaApi(userB.id, "Bob Person 1")

            // User A sees only their persons
            val resultA = mockMvc.perform(
                get("/api/v1/persons")
                    .with(authentication(authenticatedJwt(userA.id)))
            )
                .andExpect(status().isOk)
                .andReturn()

            val responseA = objectMapper.readTree(resultA.response.contentAsString)
            responseA.get("totalElements").asInt() shouldBe 2
            val namesA = responseA.get("content").map { it.get("name").asText() }
            namesA.all { it.startsWith("Alice") } shouldBe true

            // User B sees only their persons
            val resultB = mockMvc.perform(
                get("/api/v1/persons")
                    .with(authentication(authenticatedJwt(userB.id)))
            )
                .andExpect(status().isOk)
                .andReturn()

            val responseB = objectMapper.readTree(resultB.response.contentAsString)
            responseB.get("totalElements").asInt() shouldBe 1
            responseB.get("content")[0].get("name").asText() shouldBe "Bob Person 1"
        }

        @Test
        @Tag("property")
        fun `cross-user access returns 404 not 403`() {
            val personId = createPersonViaApi(userA.id, "Alice's Person")

            // Verify it's specifically 404, not 403
            val result = mockMvc.perform(
                get("/api/v1/persons/$personId")
                    .with(authentication(authenticatedJwt(userB.id)))
            ).andReturn()

            result.response.status shouldBe 404
            val body = objectMapper.readTree(result.response.contentAsString)
            body.get("status").asInt() shouldBe 404
            body.get("error").asText() shouldBe "Not Found"
        }
    }

    // ===== Task 9.2: Properties 7, 8, 9 - Pagination and filtering =====

    @Nested
    inner class PaginationAndFilteringTests {

        /**
         * Property 7: Pagination metadata correctness
         *
         * For any User with N Persons and any valid page size S, the list endpoint SHALL return
         * totalElements equal to N, totalPages equal to ceil(N/S), and the content array size
         * SHALL be min(S, N - page*S) for valid page numbers.
         *
         * **Validates: Requirements 6.1, 6.2, 6.3**
         */
        @Test
        @Tag("property")
        fun `Property 7 - pagination metadata correctness`() {
            // Create 7 persons for User A
            for (i in 1..7) {
                createPersonViaApi(userA.id, "Person $i")
            }

            // Test with page size 3
            val page0 = mockMvc.perform(
                get("/api/v1/persons")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .param("page", "0")
                    .param("size", "3")
            ).andExpect(status().isOk).andReturn()

            val response0 = objectMapper.readTree(page0.response.contentAsString)
            response0.get("totalElements").asInt() shouldBe 7
            response0.get("totalPages").asInt() shouldBe 3 // ceil(7/3) = 3
            response0.get("content").size() shouldBe 3 // min(3, 7 - 0*3) = 3
            response0.get("page").asInt() shouldBe 0
            response0.get("size").asInt() shouldBe 3

            // Page 1
            val page1 = mockMvc.perform(
                get("/api/v1/persons")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .param("page", "1")
                    .param("size", "3")
            ).andExpect(status().isOk).andReturn()

            val response1 = objectMapper.readTree(page1.response.contentAsString)
            response1.get("totalElements").asInt() shouldBe 7
            response1.get("totalPages").asInt() shouldBe 3
            response1.get("content").size() shouldBe 3 // min(3, 7 - 1*3) = 3

            // Page 2 (last page, partial)
            val page2 = mockMvc.perform(
                get("/api/v1/persons")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .param("page", "2")
                    .param("size", "3")
            ).andExpect(status().isOk).andReturn()

            val response2 = objectMapper.readTree(page2.response.contentAsString)
            response2.get("totalElements").asInt() shouldBe 7
            response2.get("totalPages").asInt() shouldBe 3
            response2.get("content").size() shouldBe 1 // min(3, 7 - 2*3) = 1

            // Test with page size 20 (all fit in one page)
            val allInOne = mockMvc.perform(
                get("/api/v1/persons")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .param("page", "0")
                    .param("size", "20")
            ).andExpect(status().isOk).andReturn()

            val responseAll = objectMapper.readTree(allInOne.response.contentAsString)
            responseAll.get("totalElements").asInt() shouldBe 7
            responseAll.get("totalPages").asInt() shouldBe 1
            responseAll.get("content").size() shouldBe 7
        }

        /**
         * Property 8: Default alphabetical sort order
         *
         * For any User with multiple Persons, the list endpoint without explicit sort parameters
         * SHALL return Persons ordered alphabetically by name (case-insensitive).
         *
         * **Validates: Requirements 6.5**
         */
        @Test
        @Tag("property")
        fun `Property 8 - default alphabetical sort order`() {
            // Create persons in non-alphabetical order
            createPersonViaApi(userA.id, "Zara")
            createPersonViaApi(userA.id, "Alice")
            createPersonViaApi(userA.id, "Mike")
            createPersonViaApi(userA.id, "Bob")
            createPersonViaApi(userA.id, "Eve")

            val result = mockMvc.perform(
                get("/api/v1/persons")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .param("page", "0")
                    .param("size", "20")
            ).andExpect(status().isOk).andReturn()

            val response = objectMapper.readTree(result.response.contentAsString)
            val names = response.get("content").map { it.get("name").asText() }

            names shouldContainExactly listOf("Alice", "Bob", "Eve", "Mike", "Zara")
        }

        /**
         * Property 9: Filter correctness
         *
         * For any User with Persons having various tags and morale statuses,
         * filtering by tag SHALL return only Persons containing that tag,
         * filtering by morale SHALL return only Persons with that status,
         * and filtering by both SHALL return only Persons satisfying both criteria simultaneously.
         *
         * **Validates: Requirements 7.1, 7.2, 7.3**
         */
        @Test
        @Tag("property")
        fun `Property 9 - filter by tag returns correct subset`() {
            createPersonViaApi(userA.id, "Alice", tags = listOf("engineering", "senior"))
            createPersonViaApi(userA.id, "Bob", tags = listOf("engineering"))
            createPersonViaApi(userA.id, "Charlie", tags = listOf("design"))
            createPersonViaApi(userA.id, "Diana", tags = listOf("product"))

            val result = mockMvc.perform(
                get("/api/v1/persons")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .param("tag", "engineering")
            ).andExpect(status().isOk).andReturn()

            val response = objectMapper.readTree(result.response.contentAsString)
            response.get("totalElements").asInt() shouldBe 2
            val names = response.get("content").map { it.get("name").asText() }
            names shouldContainExactly listOf("Alice", "Bob")
        }

        @Test
        @Tag("property")
        fun `Property 9 - filter by morale returns correct subset`() {
            val aliceId = createPersonViaApi(userA.id, "Alice")
            val bobId = createPersonViaApi(userA.id, "Bob")
            val charlieId = createPersonViaApi(userA.id, "Charlie")

            setMoraleViaApi(userA.id, aliceId, "GREEN")
            setMoraleViaApi(userA.id, bobId, "RED")
            setMoraleViaApi(userA.id, charlieId, "GREEN")

            val result = mockMvc.perform(
                get("/api/v1/persons")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .param("morale", "GREEN")
            ).andExpect(status().isOk).andReturn()

            val response = objectMapper.readTree(result.response.contentAsString)
            response.get("totalElements").asInt() shouldBe 2
            val names = response.get("content").map { it.get("name").asText() }
            names shouldContainExactly listOf("Alice", "Charlie")
        }

        @Test
        @Tag("property")
        fun `Property 9 - combined tag and morale filter returns intersection`() {
            val aliceId = createPersonViaApi(userA.id, "Alice", tags = listOf("engineering"))
            val bobId = createPersonViaApi(userA.id, "Bob", tags = listOf("engineering"))
            val charlieId = createPersonViaApi(userA.id, "Charlie", tags = listOf("design"))

            setMoraleViaApi(userA.id, aliceId, "GREEN")
            setMoraleViaApi(userA.id, bobId, "RED")
            setMoraleViaApi(userA.id, charlieId, "GREEN")

            val result = mockMvc.perform(
                get("/api/v1/persons")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .param("tag", "engineering")
                    .param("morale", "GREEN")
            ).andExpect(status().isOk).andReturn()

            val response = objectMapper.readTree(result.response.contentAsString)
            response.get("totalElements").asInt() shouldBe 1
            response.get("content")[0].get("name").asText() shouldBe "Alice"
        }
    }

    // ===== Task 9.3: Property 5 - Delete then retrieval returns not found =====

    /**
     * Property 5: Delete then retrieval returns not found
     *
     * For any Person belonging to the authenticated User, after successful deletion,
     * retrieving that Person by ID SHALL return a 404 Not Found response.
     *
     * **Validates: Requirements 5.1, 5.4**
     */
    @Nested
    inner class DeleteThenRetrieveTests {

        @Test
        @Tag("property")
        fun `Property 5 - delete then GET returns 404`() {
            val personId = createPersonViaApi(userA.id, "Person to Delete")

            // Verify person exists
            mockMvc.perform(
                get("/api/v1/persons/$personId")
                    .with(authentication(authenticatedJwt(userA.id)))
            ).andExpect(status().isOk)

            // Delete the person
            mockMvc.perform(
                delete("/api/v1/persons/$personId")
                    .with(authentication(authenticatedJwt(userA.id)))
            ).andExpect(status().isNoContent)

            // Verify GET returns 404
            mockMvc.perform(
                get("/api/v1/persons/$personId")
                    .with(authentication(authenticatedJwt(userA.id)))
            ).andExpect(status().isNotFound)
        }

        @Test
        @Tag("property")
        fun `Property 5 - delete then update returns 404`() {
            val personId = createPersonViaApi(userA.id, "Person to Delete")

            // Delete the person
            mockMvc.perform(
                delete("/api/v1/persons/$personId")
                    .with(authentication(authenticatedJwt(userA.id)))
            ).andExpect(status().isNoContent)

            // Verify PUT returns 404
            val updateRequest = mapOf("name" to "Updated Name")
            mockMvc.perform(
                put("/api/v1/persons/$personId")
                    .with(authentication(authenticatedJwt(userA.id)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest))
            ).andExpect(status().isNotFound)
        }

        @Test
        @Tag("property")
        fun `Property 5 - delete then delete again returns 404`() {
            val personId = createPersonViaApi(userA.id, "Person to Delete")

            // Delete the person
            mockMvc.perform(
                delete("/api/v1/persons/$personId")
                    .with(authentication(authenticatedJwt(userA.id)))
            ).andExpect(status().isNoContent)

            // Verify second DELETE returns 404
            mockMvc.perform(
                delete("/api/v1/persons/$personId")
                    .with(authentication(authenticatedJwt(userA.id)))
            ).andExpect(status().isNotFound)
        }

        @Test
        @Tag("property")
        fun `Property 5 - deleted person does not appear in list`() {
            val personId = createPersonViaApi(userA.id, "Person to Delete")
            createPersonViaApi(userA.id, "Person to Keep")

            // Delete one person
            mockMvc.perform(
                delete("/api/v1/persons/$personId")
                    .with(authentication(authenticatedJwt(userA.id)))
            ).andExpect(status().isNoContent)

            // Verify list only contains the remaining person
            val result = mockMvc.perform(
                get("/api/v1/persons")
                    .with(authentication(authenticatedJwt(userA.id)))
            ).andExpect(status().isOk).andReturn()

            val response = objectMapper.readTree(result.response.contentAsString)
            response.get("totalElements").asInt() shouldBe 1
            response.get("content")[0].get("name").asText() shouldBe "Person to Keep"
        }
    }

    // ===== Task 9.4: Property 14 - Invalid morale status rejection =====

    /**
     * Property 14: Invalid morale status rejection
     *
     * For any string that is not one of GREEN, YELLOW, RED, or UNKNOWN,
     * providing it as a morale status value SHALL result in a 400 Bad Request response.
     *
     * Minimum 100 iterations.
     *
     * **Validates: Requirements 7.4, 7.5, 8.4**
     */
    @Nested
    inner class InvalidMoraleStatusTests {

        @Test
        @Tag("property")
        fun `Property 14 - invalid morale status strings are rejected with 400`() = runBlocking {
            val personId = createPersonViaApi(userA.id, "Test Person for Morale")

            val validStatuses = setOf("GREEN", "YELLOW", "RED", "UNKNOWN")

            // Generate arbitrary strings that are NOT valid morale statuses
            // Use only alphanumeric characters to avoid JSON encoding issues
            val invalidStatusArb = Arb.string(1..20, Codepoint.alphanumeric())
                .filter { it.uppercase() !in validStatuses }

            checkAll(100, invalidStatusArb) { invalidStatus ->
                val requestJson = """{"status": "$invalidStatus"}"""

                val result = mockMvc.perform(
                    put("/api/v1/persons/$personId/morale")
                        .with(authentication(authenticatedJwt(userA.id)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                ).andReturn()

                result.response.status shouldBe 400
            }
            Unit
        }

        @Test
        @Tag("property")
        fun `Property 14 - specific invalid morale values are rejected`() {
            val personId = createPersonViaApi(userA.id, "Test Person for Morale")

            val invalidValues = listOf(
                "BLUE", "green", "Yellow", "red", "unknown",
                "HAPPY", "SAD", "NEUTRAL", "", "123", "NULL",
                "GREEN1", "REDD", "YELLLOW"
            )

            for (invalidStatus in invalidValues) {
                val requestJson = """{"status": "$invalidStatus"}"""

                val result = mockMvc.perform(
                    put("/api/v1/persons/$personId/morale")
                        .with(authentication(authenticatedJwt(userA.id)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                ).andReturn()

                result.response.status shouldBe 400
            }
        }
    }
}
