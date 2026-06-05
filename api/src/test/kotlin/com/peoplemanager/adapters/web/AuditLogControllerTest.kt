package com.peoplemanager.adapters.web

import com.peoplemanager.adapters.auth.SecurityConfig
import com.peoplemanager.adapters.auth.UserProvisioningJwtAuthenticationConverter
import com.peoplemanager.application.UserProvisioningService
import com.peoplemanager.application.port.input.AuditLogQueryPort
import com.peoplemanager.application.queries.GetAuditLogQuery
import com.peoplemanager.domain.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.Instant
import java.util.UUID

@WebMvcTest(controllers = [AuditLogController::class])
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
class AuditLogControllerTest {

    @TestConfiguration
    class TestConfig {
        @Bean
        fun auditLogQueryPort(): AuditLogQueryPort = mockk()

        @Bean
        fun userProvisioningService(): UserProvisioningService = mockk()
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var auditLogQueryPort: AuditLogQueryPort

    private val userId = UserId(UUID.randomUUID())
    private val personId = PersonId(UUID.randomUUID())

    private fun authenticatedJwt(userId: UserId = this.userId): JwtAuthenticationToken {
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
    fun `should return paginated audit log entries`() {
        val entry = AuditLogEntry(
            id = AuditLogEntryId.generate(),
            userId = userId,
            action = AuditAction.CREATE,
            entityType = AuditEntityType.PERSON,
            entityId = personId.value.toString(),
            personId = personId,
            summary = "Created person \"John\"",
            createdAt = Instant.parse("2026-05-11T10:00:00Z")
        )
        every { auditLogQueryPort.getAuditLog(any()) } returns PageImpl(listOf(entry), PageRequest.of(0, 20), 1)

        mockMvc.perform(
            get("/api/v1/audit-log")
                .with(authentication(authenticatedJwt()))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].action").value("CREATE"))
            .andExpect(jsonPath("$.content[0].entityType").value("PERSON"))
            .andExpect(jsonPath("$.content[0].entityId").value(personId.value.toString()))
            .andExpect(jsonPath("$.content[0].personId").value(personId.value.toString()))
            .andExpect(jsonPath("$.content[0].summary").value("Created person \"John\""))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.totalPages").value(1))
    }

    @Test
    fun `should filter by entity type`() {
        every { auditLogQueryPort.getAuditLog(any()) } returns PageImpl(emptyList(), PageRequest.of(0, 20), 0)

        mockMvc.perform(
            get("/api/v1/audit-log")
                .param("entityType", "ACTION_ITEM")
                .with(authentication(authenticatedJwt()))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(0))

        verify {
            auditLogQueryPort.getAuditLog(match {
                it.entityType == AuditEntityType.ACTION_ITEM && it.action == null
            })
        }
    }

    @Test
    fun `should filter by action`() {
        every { auditLogQueryPort.getAuditLog(any()) } returns PageImpl(emptyList(), PageRequest.of(0, 20), 0)

        mockMvc.perform(
            get("/api/v1/audit-log")
                .param("action", "DELETE")
                .with(authentication(authenticatedJwt()))
        )
            .andExpect(status().isOk)

        verify {
            auditLogQueryPort.getAuditLog(match {
                it.action == AuditAction.DELETE && it.entityType == null
            })
        }
    }

    @Test
    fun `should filter by both entity type and action`() {
        every { auditLogQueryPort.getAuditLog(any()) } returns PageImpl(emptyList(), PageRequest.of(0, 20), 0)

        mockMvc.perform(
            get("/api/v1/audit-log")
                .param("entityType", "PERSON")
                .param("action", "CREATE")
                .with(authentication(authenticatedJwt()))
        )
            .andExpect(status().isOk)

        verify {
            auditLogQueryPort.getAuditLog(match {
                it.entityType == AuditEntityType.PERSON && it.action == AuditAction.CREATE
            })
        }
    }

    @Test
    fun `should support pagination parameters`() {
        every { auditLogQueryPort.getAuditLog(any()) } returns PageImpl(emptyList(), PageRequest.of(2, 10), 30)

        mockMvc.perform(
            get("/api/v1/audit-log")
                .param("page", "2")
                .param("size", "10")
                .with(authentication(authenticatedJwt()))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.page").value(2))
            .andExpect(jsonPath("$.size").value(10))
            .andExpect(jsonPath("$.totalElements").value(30))
            .andExpect(jsonPath("$.totalPages").value(3))
    }

    @Test
    fun `should return 401 for unauthenticated request`() {
        mockMvc.perform(get("/api/v1/audit-log"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should return empty page when no audit entries exist`() {
        every { auditLogQueryPort.getAuditLog(any()) } returns PageImpl(emptyList(), PageRequest.of(0, 20), 0)

        mockMvc.perform(
            get("/api/v1/audit-log")
                .with(authentication(authenticatedJwt()))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(0))
            .andExpect(jsonPath("$.totalElements").value(0))
    }

    @Test
    fun `should scope audit log by authenticated user`() {
        every { auditLogQueryPort.getAuditLog(any()) } returns PageImpl(emptyList(), PageRequest.of(0, 20), 0)

        mockMvc.perform(
            get("/api/v1/audit-log")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)

        verify {
            auditLogQueryPort.getAuditLog(match { it.userId == userId })
        }
    }

    @Test
    fun `should return entry with null personId`() {
        val entry = AuditLogEntry(
            id = AuditLogEntryId.generate(),
            userId = userId,
            action = AuditAction.UPDATE,
            entityType = AuditEntityType.USER_SETTINGS,
            entityId = userId.value.toString(),
            personId = null,
            summary = "Updated user settings",
            createdAt = Instant.parse("2026-05-11T10:00:00Z")
        )
        every { auditLogQueryPort.getAuditLog(any()) } returns PageImpl(listOf(entry), PageRequest.of(0, 20), 1)

        mockMvc.perform(
            get("/api/v1/audit-log")
                .with(authentication(authenticatedJwt()))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].personId").isEmpty)
            .andExpect(jsonPath("$.content[0].entityType").value("USER_SETTINGS"))
    }
}
