package com.peoplemanager.adapters.web

import com.peoplemanager.adapters.auth.SecurityConfig
import com.peoplemanager.adapters.auth.UserProvisioningJwtAuthenticationConverter
import com.peoplemanager.application.UserProvisioningService
import com.peoplemanager.application.port.input.OneOnOneCommandPort
import com.peoplemanager.application.port.input.OneOnOneQueryPort
import io.kotest.property.Arb
import io.kotest.property.arbitrary.uuid
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import io.mockk.mockk
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

/**
 * Property-based test: Authentication required on all 1:1 endpoints (Property 13)
 *
 * For any 1:1 API endpoint (all combinations of HTTP methods and paths under
 * /api/v1/persons/{personId}/one-on-one-*), a request without a valid JWT Bearer token
 * SHALL receive a 401 Unauthorized response.
 *
 * **Validates: Requirements 11.8**
 */
@Tag("property")
@WebMvcTest(controllers = [OneOnOneController::class])
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
class OneOnOneAuthPropertyTest {

    @TestConfiguration
    class TestConfig {
        @Bean
        fun oneOnOneCommandPort(): OneOnOneCommandPort = mockk()

        @Bean
        fun oneOnOneQueryPort(): OneOnOneQueryPort = mockk()

        @Bean
        fun userProvisioningService(): UserProvisioningService = mockk()
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    /**
     * Property 13: Authentication required on all endpoints
     *
     * For any arbitrary personId UUID, unauthenticated PUT /api/v1/persons/{personId}/one-on-one-series
     * SHALL return 401 Unauthorized.
     *
     * **Validates: Requirements 11.8**
     */
    @Test
    fun `Property 13 - PUT one-on-one-series without JWT returns 401 for any personId`() = runBlocking {
        checkAll(100, Arb.uuid()) { personId: UUID ->
            mockMvc.perform(
                put("/api/v1/persons/$personId/one-on-one-series")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"cadenceType":"WEEKLY"}""")
            ).andExpect(status().isUnauthorized)
        }
        Unit
    }

    /**
     * Property 13: Authentication required on all endpoints
     *
     * For any arbitrary personId UUID, unauthenticated GET /api/v1/persons/{personId}/one-on-one-series
     * SHALL return 401 Unauthorized.
     *
     * **Validates: Requirements 11.8**
     */
    @Test
    fun `Property 13 - GET one-on-one-series without JWT returns 401 for any personId`() = runBlocking {
        checkAll(100, Arb.uuid()) { personId: UUID ->
            mockMvc.perform(
                get("/api/v1/persons/$personId/one-on-one-series")
            ).andExpect(status().isUnauthorized)
        }
        Unit
    }

    /**
     * Property 13: Authentication required on all endpoints
     *
     * For any arbitrary personId UUID, unauthenticated POST /api/v1/persons/{personId}/one-on-one-entries
     * SHALL return 401 Unauthorized.
     *
     * **Validates: Requirements 11.8**
     */
    @Test
    fun `Property 13 - POST one-on-one-entries without JWT returns 401 for any personId`() = runBlocking {
        checkAll(100, Arb.uuid()) { personId: UUID ->
            mockMvc.perform(
                post("/api/v1/persons/$personId/one-on-one-entries")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"meetingDate":"2025-05-08T14:00:00Z"}""")
            ).andExpect(status().isUnauthorized)
        }
        Unit
    }

    /**
     * Property 13: Authentication required on all endpoints
     *
     * For any arbitrary personId and entryId UUIDs, unauthenticated GET
     * /api/v1/persons/{personId}/one-on-one-entries/{entryId} SHALL return 401 Unauthorized.
     *
     * **Validates: Requirements 11.8**
     */
    @Test
    fun `Property 13 - GET one-on-one-entries by id without JWT returns 401 for any personId and entryId`() = runBlocking {
        checkAll(100, Arb.uuid(), Arb.uuid()) { personId: UUID, entryId: UUID ->
            mockMvc.perform(
                get("/api/v1/persons/$personId/one-on-one-entries/$entryId")
            ).andExpect(status().isUnauthorized)
        }
        Unit
    }

    /**
     * Property 13: Authentication required on all endpoints
     *
     * For any arbitrary personId and entryId UUIDs, unauthenticated PUT
     * /api/v1/persons/{personId}/one-on-one-entries/{entryId} SHALL return 401 Unauthorized.
     *
     * **Validates: Requirements 11.8**
     */
    @Test
    fun `Property 13 - PUT one-on-one-entries without JWT returns 401 for any personId and entryId`() = runBlocking {
        checkAll(100, Arb.uuid(), Arb.uuid()) { personId: UUID, entryId: UUID ->
            mockMvc.perform(
                put("/api/v1/persons/$personId/one-on-one-entries/$entryId")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"notesMarkdown":"test"}""")
            ).andExpect(status().isUnauthorized)
        }
        Unit
    }

    /**
     * Property 13: Authentication required on all endpoints
     *
     * For any arbitrary personId and entryId UUIDs, unauthenticated DELETE
     * /api/v1/persons/{personId}/one-on-one-entries/{entryId} SHALL return 401 Unauthorized.
     *
     * **Validates: Requirements 11.8**
     */
    @Test
    fun `Property 13 - DELETE one-on-one-entries without JWT returns 401 for any personId and entryId`() = runBlocking {
        checkAll(100, Arb.uuid(), Arb.uuid()) { personId: UUID, entryId: UUID ->
            mockMvc.perform(
                delete("/api/v1/persons/$personId/one-on-one-entries/$entryId")
            ).andExpect(status().isUnauthorized)
        }
        Unit
    }

    /**
     * Property 13: Authentication required on all endpoints
     *
     * For any arbitrary personId UUID, unauthenticated GET /api/v1/persons/{personId}/one-on-one-entries
     * SHALL return 401 Unauthorized.
     *
     * **Validates: Requirements 11.8**
     */
    @Test
    fun `Property 13 - GET one-on-one-entries list without JWT returns 401 for any personId`() = runBlocking {
        checkAll(100, Arb.uuid()) { personId: UUID ->
            mockMvc.perform(
                get("/api/v1/persons/$personId/one-on-one-entries")
            ).andExpect(status().isUnauthorized)
        }
        Unit
    }
}
