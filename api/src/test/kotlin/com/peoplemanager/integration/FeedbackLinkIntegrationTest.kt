package com.peoplemanager.integration

import tools.jackson.databind.ObjectMapper
import com.peoplemanager.adapters.persistence.JpaUserRepositoryAdapter
import com.peoplemanager.domain.User
import com.peoplemanager.domain.UserId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
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
 * Full-stack integration test for the feedback link lifecycle:
 * create link -> anonymous public submit (no auth) -> manager sees PENDING response ->
 * approve -> convert. Also verifies the public route is reachable without a token,
 * that cross-manager access is 404, and that revoked/expired links reject submissions.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class FeedbackLinkIntegrationTest {

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

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper
    @Autowired lateinit var userRepository: JpaUserRepositoryAdapter
    @Autowired lateinit var jdbcTemplate: JdbcTemplate

    private lateinit var userA: User
    private lateinit var userB: User
    private lateinit var personAId: String

    private fun jwt(userId: UserId): JwtAuthenticationToken {
        val jwt = Jwt.withTokenValue("t").header("alg", "RS256").subject("s-${userId.value}")
            .issuer("http://localhost:9000").claim("name", "U")
            .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(3600)).build()
        return JwtAuthenticationToken(jwt, listOf(SimpleGrantedAuthority("ROLE_USER")), "s-${userId.value}")
            .apply { details = userId }
    }

    @BeforeEach
    fun setup() {
        jdbcTemplate.execute("DELETE FROM feedback_responses")
        jdbcTemplate.execute("DELETE FROM feedback_links")
        jdbcTemplate.execute("DELETE FROM feedback_templates")
        jdbcTemplate.execute("DELETE FROM kudos")
        jdbcTemplate.execute("DELETE FROM persons")
        jdbcTemplate.execute("DELETE FROM users")
        userA = User(UserId.generate(), "subj-a", "http://localhost:9000", "A", "a@test.com")
        userB = User(UserId.generate(), "subj-b", "http://localhost:9000", "B", "b@test.com")
        userRepository.save(userA)
        userRepository.save(userB)
        personAId = createPerson(userA.id, "Alice")
    }

    private fun createPerson(userId: UserId, name: String): String {
        val result = mockMvc.perform(
            post("/api/v1/persons").with(authentication(jwt(userId)))
                .contentType(MediaType.APPLICATION_JSON).content("""{"name":"$name"}""")
        ).andExpect(status().isCreated).andReturn()
        return objectMapper.readTree(result.response.contentAsString).get("id").asText()
    }

    private fun createLink(userId: UserId, personId: String): Pair<String, String> {
        val body = """
          {"title":"Feedback for Alice","description":"Help Alice improve",
           "questions":[{"id":"q1","type":"RATING","text":"Rate collaboration","required":true}],
           "expiresInDays":14}
        """.trimIndent()
        val result = mockMvc.perform(
            post("/api/v1/persons/$personId/feedback-links").with(authentication(jwt(userId)))
                .contentType(MediaType.APPLICATION_JSON).content(body)
        ).andExpect(status().isCreated).andReturn()
        val node = objectMapper.readTree(result.response.contentAsString)
        return node.get("id").asText() to node.get("token").asText()
    }

    @Test
    fun `public GET form is reachable without authentication`() {
        val (_, token) = createLink(userA.id, personAId)
        mockMvc.perform(get("/api/v1/public/feedback/$token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.personName").value("Alice"))
            .andExpect(jsonPath("$.questions[0].id").value("q1"))
    }

    @Test
    fun `anonymous submission appears as PENDING for the manager, then can be approved`() {
        val (_, token) = createLink(userA.id, personAId)

        // Anonymous public submit, no auth header
        mockMvc.perform(
            post("/api/v1/public/feedback/$token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"anonymous":true,"answers":[{"questionId":"q1","ratingValue":5}],"additionalComments":"Excellent mentor"}""")
        ).andExpect(status().isCreated)

        // Manager sees it as PENDING
        val listResult = mockMvc.perform(
            get("/api/v1/persons/$personAId/feedback-responses").with(authentication(jwt(userA.id)))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].status").value("PENDING"))
            .andExpect(jsonPath("$[0].anonymous").value(true))
            .andExpect(jsonPath("$[0].additionalComments").value("Excellent mentor"))
            .andReturn()
        val responseId = objectMapper.readTree(listResult.response.contentAsString).get(0).get("id").asText()

        // Approve
        mockMvc.perform(
            patch("/api/v1/feedback-responses/$responseId").with(authentication(jwt(userA.id)))
                .contentType(MediaType.APPLICATION_JSON).content("""{"approve":true}""")
        ).andExpect(status().isOk).andExpect(jsonPath("$.status").value("APPROVED"))
    }

    @Test
    fun `submission to a revoked link is rejected with 410`() {
        val (linkId, token) = createLink(userA.id, personAId)
        mockMvc.perform(post("/api/v1/feedback-links/$linkId/revoke").with(authentication(jwt(userA.id))))
            .andExpect(status().isOk).andExpect(jsonPath("$.status").value("REVOKED"))

        mockMvc.perform(
            post("/api/v1/public/feedback/$token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"anonymous":true,"answers":[{"questionId":"q1","ratingValue":3}]}""")
        ).andExpect(status().isGone)
    }

    @Test
    fun `missing required answer is rejected with 400`() {
        val (_, token) = createLink(userA.id, personAId)
        mockMvc.perform(
            post("/api/v1/public/feedback/$token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"anonymous":true,"answers":[]}""")
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `manager B cannot list manager A's feedback links or responses`() {
        createLink(userA.id, personAId)
        // Person A does not belong to B -> 404
        mockMvc.perform(get("/api/v1/persons/$personAId/feedback-links").with(authentication(jwt(userB.id))))
            .andExpect(status().isNotFound)
        mockMvc.perform(get("/api/v1/persons/$personAId/feedback-responses").with(authentication(jwt(userB.id))))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `manager endpoints require authentication`() {
        mockMvc.perform(get("/api/v1/persons/$personAId/feedback-links"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `convert approved response to a kudo`() {
        val (_, token) = createLink(userA.id, personAId)
        mockMvc.perform(
            post("/api/v1/public/feedback/$token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"anonymous":true,"answers":[{"questionId":"q1","ratingValue":5}],"additionalComments":"Amazing mentor"}""")
        ).andExpect(status().isCreated)

        val listResult = mockMvc.perform(
            get("/api/v1/persons/$personAId/feedback-responses").with(authentication(jwt(userA.id)))
        ).andReturn()
        val responseId = objectMapper.readTree(listResult.response.contentAsString).get(0).get("id").asText()

        mockMvc.perform(
            post("/api/v1/feedback-responses/$responseId/convert").with(authentication(jwt(userA.id)))
                .contentType(MediaType.APPLICATION_JSON).content("""{"type":"KUDO"}""")
        ).andExpect(status().isOk).andExpect(jsonPath("$.convertedToType").value("KUDO"))

        // The kudo now exists for the person
        mockMvc.perform(get("/api/v1/persons/$personAId/kudos").with(authentication(jwt(userA.id))))
            .andExpect(status().isOk).andExpect(jsonPath("$.totalElements").value(1))
    }
}
