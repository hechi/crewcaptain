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
 * Full-stack integration test for feedback templates.
 * Verifies JSON question round-trip through real PostgreSQL, single-level branching
 * persistence, userId isolation (cross-manager 404), and that AI-generate degrades
 * gracefully when AI is not configured.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class FeedbackTemplateIntegrationTest {

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

    private fun jwt(userId: UserId): JwtAuthenticationToken {
        val jwt = Jwt.withTokenValue("t").header("alg", "RS256").subject("s-${userId.value}")
            .issuer("http://localhost:9000").claim("name", "U")
            .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(3600)).build()
        return JwtAuthenticationToken(jwt, listOf(SimpleGrantedAuthority("ROLE_USER")), "s-${userId.value}")
            .apply { details = userId }
    }

    @BeforeEach
    fun setup() {
        jdbcTemplate.execute("DELETE FROM feedback_templates")
        jdbcTemplate.execute("DELETE FROM users")
        userA = User(UserId.generate(), "subj-a", "http://localhost:9000", "A", "a@test.com")
        userB = User(UserId.generate(), "subj-b", "http://localhost:9000", "B", "b@test.com")
        userRepository.save(userA)
        userRepository.save(userB)
    }

    private val templateBody = """
        {
          "title": "Mid-year review",
          "description": "Peer input",
          "questions": [
            {"id": "q1", "type": "RATING", "text": "Rate collaboration", "required": true, "lowLabel": "Poor", "highLabel": "Excellent"},
            {"id": "q1b", "type": "TEXT", "text": "Explain a low rating", "required": false, "showIf": {"questionId": "q1", "operator": "LTE", "value": 3}}
          ]
        }
    """.trimIndent()

    private fun createTemplate(userId: UserId): String {
        val result = mockMvc.perform(
            post("/api/v1/feedback-templates")
                .with(authentication(jwt(userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(templateBody)
        ).andExpect(status().isCreated).andReturn()
        return objectMapper.readTree(result.response.contentAsString).get("id").asText()
    }

    @Test
    fun `creates and reads back template with branching preserved`() {
        val id = createTemplate(userA.id)

        mockMvc.perform(get("/api/v1/feedback-templates/$id").with(authentication(jwt(userA.id))))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.title").value("Mid-year review"))
            .andExpect(jsonPath("$.questions.length()").value(2))
            .andExpect(jsonPath("$.questions[0].type").value("RATING"))
            .andExpect(jsonPath("$.questions[0].lowLabel").value("Poor"))
            .andExpect(jsonPath("$.questions[1].showIf.questionId").value("q1"))
            .andExpect(jsonPath("$.questions[1].showIf.operator").value("LTE"))
            .andExpect(jsonPath("$.questions[1].showIf.value").value(3))
    }

    @Test
    fun `rejects invalid branching with 400`() {
        val badBody = """
            {"title":"Bad","questions":[
              {"id":"q1","type":"TEXT","text":"first"},
              {"id":"q2","type":"TEXT","text":"second","showIf":{"questionId":"q1","operator":"LTE","value":3}}
            ]}
        """.trimIndent()
        mockMvc.perform(
            post("/api/v1/feedback-templates")
                .with(authentication(jwt(userA.id)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(badBody)
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `manager B cannot read or delete manager A's template`() {
        val id = createTemplate(userA.id)

        mockMvc.perform(get("/api/v1/feedback-templates/$id").with(authentication(jwt(userB.id))))
            .andExpect(status().isNotFound)
        mockMvc.perform(delete("/api/v1/feedback-templates/$id").with(authentication(jwt(userB.id))))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `list returns only own templates`() {
        createTemplate(userA.id)
        createTemplate(userB.id)

        mockMvc.perform(get("/api/v1/feedback-templates").with(authentication(jwt(userA.id))))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
    }

    @Test
    fun `ai-generate returns 422 when AI not configured`() {
        mockMvc.perform(
            post("/api/v1/feedback-templates/ai-generate")
                .with(authentication(jwt(userA.id)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"brief":"Post-project retro"}""")
        ).andExpect(status().isUnprocessableEntity)
    }

    @Test
    fun `starters endpoint returns built-in starters`() {
        mockMvc.perform(get("/api/v1/feedback-templates/starters").with(authentication(jwt(userA.id))))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
    }
}
