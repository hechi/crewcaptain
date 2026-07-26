package com.peoplemanager.adapters.web

import tools.jackson.databind.ObjectMapper
import com.peoplemanager.adapters.auth.SecurityConfig
import com.peoplemanager.adapters.auth.UserProvisioningJwtAuthenticationConverter
import com.peoplemanager.application.UserProvisioningService
import com.peoplemanager.application.port.output.BulkImportResult
import com.peoplemanager.application.port.output.PersonBulkImportPort
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.UserId
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.util.UUID

@WebMvcTest(controllers = [PersonBulkImportController::class])
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
class PersonBulkImportControllerTest {

    @TestConfiguration
    class TestConfig {
        @Bean
        fun personBulkImportPort(): PersonBulkImportPort = mockk()

        @Bean
        fun userProvisioningService(): UserProvisioningService = mockk()
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var personBulkImportPort: PersonBulkImportPort

    private fun authenticatedJwt(userId: UserId = UserId(UUID.randomUUID())): JwtAuthenticationToken {
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
    fun `POST persons import - returns 200 with success count on valid CSV`() {
        val userId = UserId(UUID.randomUUID())
        val csvContent = "name,email\nAlice,alice@example.com\nBob,bob@example.com"
        val file = MockMultipartFile("file", "people.csv", "text/csv", csvContent.toByteArray())

        every { personBulkImportPort.importPersonsFromCsv(any()) } returns BulkImportResult(
            successCount = 2,
            errorCount = 0,
            createdPersonIds = listOf(PersonId.generate(), PersonId.generate()),
            errors = emptyList()
        )

        mockMvc.perform(
            multipart("/api/v1/persons/import")
                .file(file)
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.successCount").value(2))
            .andExpect(jsonPath("$.errorCount").value(0))
            .andExpect(jsonPath("$.errors").isEmpty)
    }

    @Test
    fun `POST persons import - returns 200 with errors for partially valid CSV`() {
        val userId = UserId(UUID.randomUUID())
        val csvContent = "name,email\nAlice,alice@example.com\n,invalid"
        val file = MockMultipartFile("file", "people.csv", "text/csv", csvContent.toByteArray())

        every { personBulkImportPort.importPersonsFromCsv(any()) } returns BulkImportResult(
            successCount = 1,
            errorCount = 1,
            createdPersonIds = listOf(PersonId.generate()),
            errors = listOf("Row 3: Name must not be blank")
        )

        mockMvc.perform(
            multipart("/api/v1/persons/import")
                .file(file)
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.successCount").value(1))
            .andExpect(jsonPath("$.errorCount").value(1))
            .andExpect(jsonPath("$.errors[0]").value("Row 3: Name must not be blank"))
    }

    @Test
    fun `POST persons import - returns 400 for empty file`() {
        val userId = UserId(UUID.randomUUID())
        val file = MockMultipartFile("file", "people.csv", "text/csv", ByteArray(0))

        mockMvc.perform(
            multipart("/api/v1/persons/import")
                .file(file)
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.successCount").value(0))
            .andExpect(jsonPath("$.errorCount").value(1))
            .andExpect(jsonPath("$.errors[0]").value("Uploaded file is empty"))
    }

    @Test
    fun `POST persons import - returns 400 for non-CSV content type`() {
        val userId = UserId(UUID.randomUUID())
        val file = MockMultipartFile("file", "people.json", "application/json", "{\"name\":\"test\"}".toByteArray())

        mockMvc.perform(
            multipart("/api/v1/persons/import")
                .file(file)
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errors[0]").value("File must be a CSV (received: application/json)"))
    }

    @Test
    fun `POST persons import - returns 401 without authentication`() {
        val csvContent = "name\nAlice"
        val file = MockMultipartFile("file", "people.csv", "text/csv", csvContent.toByteArray())

        mockMvc.perform(
            multipart("/api/v1/persons/import")
                .file(file)
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `POST persons import - passes userId from authenticated user to command`() {
        val userId = UserId(UUID.randomUUID())
        val csvContent = "name\nAlice"
        val file = MockMultipartFile("file", "people.csv", "text/csv", csvContent.toByteArray())

        every { personBulkImportPort.importPersonsFromCsv(match { it.userId == userId }) } returns BulkImportResult(
            successCount = 1,
            errorCount = 0,
            createdPersonIds = listOf(PersonId.generate()),
            errors = emptyList()
        )

        mockMvc.perform(
            multipart("/api/v1/persons/import")
                .file(file)
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)

        verify { personBulkImportPort.importPersonsFromCsv(match { it.userId == userId }) }
    }

    @Test
    fun `POST persons import - accepts text csv content type variants`() {
        val userId = UserId(UUID.randomUUID())
        val csvContent = "name\nAlice"
        val file = MockMultipartFile("file", "people.csv", "text/csv; charset=utf-8", csvContent.toByteArray())

        every { personBulkImportPort.importPersonsFromCsv(any()) } returns BulkImportResult(
            successCount = 1,
            errorCount = 0,
            createdPersonIds = listOf(PersonId.generate()),
            errors = emptyList()
        )

        mockMvc.perform(
            multipart("/api/v1/persons/import")
                .file(file)
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.successCount").value(1))
    }

    @Test
    fun `POST persons import - accepts file with no content type`() {
        val userId = UserId(UUID.randomUUID())
        val csvContent = "name\nAlice"
        val file = MockMultipartFile("file", "people.csv", null, csvContent.toByteArray())

        every { personBulkImportPort.importPersonsFromCsv(any()) } returns BulkImportResult(
            successCount = 1,
            errorCount = 0,
            createdPersonIds = listOf(PersonId.generate()),
            errors = emptyList()
        )

        mockMvc.perform(
            multipart("/api/v1/persons/import")
                .file(file)
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.successCount").value(1))
    }

    @Test
    fun `POST persons import - returns all errors from service`() {
        val userId = UserId(UUID.randomUUID())
        val csvContent = "name\nAlice"
        val file = MockMultipartFile("file", "people.csv", "text/csv", csvContent.toByteArray())

        every { personBulkImportPort.importPersonsFromCsv(any()) } returns BulkImportResult(
            successCount = 0,
            errorCount = 2,
            createdPersonIds = emptyList(),
            errors = listOf("Row 2: Name must not be blank", "Row 3: Invalid start_date")
        )

        mockMvc.perform(
            multipart("/api/v1/persons/import")
                .file(file)
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.errorCount").value(2))
            .andExpect(jsonPath("$.errors").isArray)
            .andExpect(jsonPath("$.errors.length()").value(2))
    }

    @Test
    fun `POST persons import - handles large successful import`() {
        val userId = UserId(UUID.randomUUID())
        val csvContent = "name\n" + (1..100).joinToString("\n") { "Person $it" }
        val file = MockMultipartFile("file", "people.csv", "text/csv", csvContent.toByteArray())

        every { personBulkImportPort.importPersonsFromCsv(any()) } returns BulkImportResult(
            successCount = 100,
            errorCount = 0,
            createdPersonIds = (1..100).map { PersonId.generate() },
            errors = emptyList()
        )

        mockMvc.perform(
            multipart("/api/v1/persons/import")
                .file(file)
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.successCount").value(100))
            .andExpect(jsonPath("$.errorCount").value(0))
    }
}
