package com.peoplemanager.adapters.web

import com.peoplemanager.adapters.auth.SecurityConfig
import com.peoplemanager.adapters.auth.UserProvisioningJwtAuthenticationConverter
import com.peoplemanager.application.PersonNotFoundException
import com.peoplemanager.application.UserProvisioningService
import com.peoplemanager.application.port.output.PersonExportPort
import com.peoplemanager.application.queries.ExportPersonDataQuery
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.UserId
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@WebMvcTest(controllers = [PersonExportController::class])
@Import(SecurityConfig::class, UserProvisioningJwtAuthenticationConverter::class, GlobalExceptionHandler::class)
@TestPropertySource(properties = [
    "spring.datasource.url=jdbc:h2:mem:test",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://auth.example.com",
    "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://auth.example.com/jwks"
])
class PersonExportControllerTest {

    @TestConfiguration
    class TestConfig {
        @Bean
        fun personExportPort(): PersonExportPort = mockk()

        @Bean
        fun userProvisioningService(): UserProvisioningService = mockk()
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var personExportPort: PersonExportPort

    private val testUserId = UUID.randomUUID()
    private val testPersonId = UUID.randomUUID()

    private fun authenticatedJwt(userId: UserId = UserId(testUserId)): JwtAuthenticationToken {
        val jwt = Jwt.withTokenValue("test-token")
            .header("alg", "RS256")
            .subject("test-subject")
            .issuer("https://auth.example.com")
            .claim("name", "Test User")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build()
        val authorities = listOf(SimpleGrantedAuthority("ROLE_USER"))
        val token = JwtAuthenticationToken(jwt, authorities, "test-subject")
        token.details = userId
        return token
    }

    @Test
    fun `should return 401 when not authenticated`() {
        mockMvc.perform(get("/api/v1/persons/$testPersonId/export"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should return markdown content with correct headers`() {
        val markdown = "# Jane Smith\n\n## Profile\n"
        every { personExportPort.exportPersonMarkdown(any()) } returns markdown

        mockMvc.perform(
            get("/api/v1/persons/$testPersonId/export")
                .with(authentication(authenticatedJwt()))
        )
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "text/markdown; charset=UTF-8"))
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"export.md\""))
            .andExpect(content().string(markdown))
    }

    @Test
    fun `should return 404 when person not found`() {
        every { personExportPort.exportPersonMarkdown(any()) } throws
            PersonNotFoundException(PersonId(testPersonId))

        mockMvc.perform(
            get("/api/v1/persons/$testPersonId/export")
                .with(authentication(authenticatedJwt()))
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should pass date range parameters to service`() {
        val querySlot = slot<ExportPersonDataQuery>()
        every { personExportPort.exportPersonMarkdown(capture(querySlot)) } returns "# Export"

        mockMvc.perform(
            get("/api/v1/persons/$testPersonId/export")
                .param("dateFrom", "2024-01-01")
                .param("dateTo", "2024-06-30")
                .with(authentication(authenticatedJwt()))
        )
            .andExpect(status().isOk)

        val capturedQuery = querySlot.captured
        assert(capturedQuery.dateFrom == LocalDate.of(2024, 1, 1)) { "dateFrom should be 2024-01-01" }
        assert(capturedQuery.dateTo == LocalDate.of(2024, 6, 30)) { "dateTo should be 2024-06-30" }
    }

    @Test
    fun `should work without date range parameters`() {
        val querySlot = slot<ExportPersonDataQuery>()
        every { personExportPort.exportPersonMarkdown(capture(querySlot)) } returns "# Export"

        mockMvc.perform(
            get("/api/v1/persons/$testPersonId/export")
                .with(authentication(authenticatedJwt()))
        )
            .andExpect(status().isOk)

        val capturedQuery = querySlot.captured
        assert(capturedQuery.dateFrom == null) { "dateFrom should be null" }
        assert(capturedQuery.dateTo == null) { "dateTo should be null" }
    }

    @Test
    fun `should pass personId from path to service`() {
        val querySlot = slot<ExportPersonDataQuery>()
        every { personExportPort.exportPersonMarkdown(capture(querySlot)) } returns "# Export"

        mockMvc.perform(
            get("/api/v1/persons/$testPersonId/export")
                .with(authentication(authenticatedJwt()))
        )
            .andExpect(status().isOk)

        val capturedQuery = querySlot.captured
        assert(capturedQuery.personId.value == testPersonId) { "personId should match path variable" }
    }

    @Test
    fun `should scope export by authenticated userId`() {
        val querySlot = slot<ExportPersonDataQuery>()
        every { personExportPort.exportPersonMarkdown(capture(querySlot)) } returns "# Export"

        mockMvc.perform(
            get("/api/v1/persons/$testPersonId/export")
                .with(authentication(authenticatedJwt()))
        )
            .andExpect(status().isOk)

        val capturedQuery = querySlot.captured
        assert(capturedQuery.userId.value == testUserId) { "userId should be the authenticated user's ID" }
    }

    @Test
    fun `should return content-length header`() {
        val markdown = "# Jane Smith\n\n## Profile\n"
        every { personExportPort.exportPersonMarkdown(any()) } returns markdown

        mockMvc.perform(
            get("/api/v1/persons/$testPersonId/export")
                .with(authentication(authenticatedJwt()))
        )
            .andExpect(status().isOk)
            .andExpect(header().longValue("Content-Length", markdown.toByteArray(Charsets.UTF_8).size.toLong()))
    }
}
