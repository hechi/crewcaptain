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
 * Full-stack analytics + summary integration:
 * submit two anonymous ratings -> approve both -> analytics shows the average and themes ->
 * save an edited summary -> the review packet includes the Peer Feedback section + summary.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class FeedbackAnalyticsIntegrationTest {

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

    private lateinit var user: User
    private lateinit var personId: String

    private fun jwt(userId: UserId): JwtAuthenticationToken {
        val jwt = Jwt.withTokenValue("t").header("alg", "RS256").subject("s-${userId.value}")
            .issuer("http://localhost:9000").claim("name", "U")
            .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(3600)).build()
        return JwtAuthenticationToken(jwt, listOf(SimpleGrantedAuthority("ROLE_USER")), "s-${userId.value}")
            .apply { details = userId }
    }

    @BeforeEach
    fun setup() {
        jdbcTemplate.execute("DELETE FROM feedback_summaries")
        jdbcTemplate.execute("DELETE FROM feedback_responses")
        jdbcTemplate.execute("DELETE FROM feedback_links")
        jdbcTemplate.execute("DELETE FROM persons")
        jdbcTemplate.execute("DELETE FROM users")
        user = User(UserId.generate(), "subj", "http://localhost:9000", "M", "m@test.com")
        userRepository.save(user)
        val result = mockMvc.perform(
            post("/api/v1/persons").with(authentication(jwt(user.id)))
                .contentType(MediaType.APPLICATION_JSON).content("""{"name":"Alice"}""")
        ).andExpect(status().isCreated).andReturn()
        personId = objectMapper.readTree(result.response.contentAsString).get("id").asText()
    }

    private fun createLinkToken(): String {
        val body = """
          {"title":"Feedback for Alice",
           "questions":[{"id":"q1","type":"RATING","text":"Rate","required":true},
                        {"id":"q2","type":"TEXT","text":"Comments","required":false}],
           "expiresInDays":14}
        """.trimIndent()
        val result = mockMvc.perform(
            post("/api/v1/persons/$personId/feedback-links").with(authentication(jwt(user.id)))
                .contentType(MediaType.APPLICATION_JSON).content(body)
        ).andExpect(status().isCreated).andReturn()
        return objectMapper.readTree(result.response.contentAsString).get("token").asText()
    }

    private fun submit(token: String, rating: Int, comment: String) {
        mockMvc.perform(
            post("/api/v1/public/feedback/$token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"anonymous":true,"answers":[{"questionId":"q1","ratingValue":$rating},{"questionId":"q2","textValue":"$comment"}]}""")
        ).andExpect(status().isCreated)
    }

    private fun approveAll() {
        val list = mockMvc.perform(
            get("/api/v1/persons/$personId/feedback-responses").with(authentication(jwt(user.id)))
        ).andReturn()
        val node = objectMapper.readTree(list.response.contentAsString)
        for (i in 0 until node.size()) {
            val id = node.get(i).get("id").asText()
            mockMvc.perform(
                patch("/api/v1/feedback-responses/$id").with(authentication(jwt(user.id)))
                    .contentType(MediaType.APPLICATION_JSON).content("""{"approve":true}""")
            ).andExpect(status().isOk)
        }
    }

    @Test
    fun `analytics reflects approved ratings and themes`() {
        val token = createLinkToken()
        submit(token, 4, "excellent communication")
        submit(token, 2, "communication could improve")
        approveAll()

        mockMvc.perform(
            get("/api/v1/persons/$personId/feedback-analytics?bucket=MONTH").with(authentication(jwt(user.id)))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.totalResponses").value(2))
            .andExpect(jsonPath("$.overallAverageRating").value(3.0))
            .andExpect(jsonPath("$.topThemes[?(@.theme == 'communication')].count").value(2))
    }

    @Test
    fun `unapproved responses are excluded from analytics`() {
        val token = createLinkToken()
        submit(token, 5, "great")
        // not approved
        mockMvc.perform(
            get("/api/v1/persons/$personId/feedback-analytics").with(authentication(jwt(user.id)))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.totalResponses").value(0))
    }

    @Test
    fun `saved summary is included in the review packet`() {
        val token = createLinkToken()
        submit(token, 5, "amazing mentor")
        approveAll()

        mockMvc.perform(
            post("/api/v1/persons/$personId/feedback-summaries").with(authentication(jwt(user.id)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"periodFrom":"2026-01-01","periodTo":"2026-12-31","content":"Alice is a standout mentor.","responseCount":1}""")
        ).andExpect(status().isCreated)

        val packet = mockMvc.perform(
            get("/api/v1/persons/$personId/review-packet?dateFrom=2026-01-01&dateTo=2026-12-31").with(authentication(jwt(user.id)))
        ).andExpect(status().isOk).andReturn().response.contentAsString

        assert(packet.contains("## Peer Feedback")) { "packet missing Peer Feedback section" }
        assert(packet.contains("Alice is a standout mentor.")) { "packet missing saved summary" }
    }

    @Test
    fun `feedback markdown export works end to end`() {
        val token = createLinkToken()
        submit(token, 4, "reliable and kind")
        approveAll()

        val md = mockMvc.perform(
            get("/api/v1/persons/$personId/feedback-export?format=md").with(authentication(jwt(user.id)))
        ).andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/markdown; charset=UTF-8"))
            .andReturn().response.contentAsString

        assert(md.contains("# Peer Feedback: Alice"))
        assert(md.contains("reliable and kind"))
    }

    @Test
    fun `feedback pdf export returns a PDF`() {
        val token = createLinkToken()
        submit(token, 4, "great")
        approveAll()

        val bytes = mockMvc.perform(
            get("/api/v1/persons/$personId/feedback-export?format=pdf").with(authentication(jwt(user.id)))
        ).andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "application/pdf"))
            .andReturn().response.contentAsByteArray

        assert(bytes.size > 100)
        assert(String(bytes.copyOfRange(0, 4), Charsets.US_ASCII) == "%PDF")
    }
}
