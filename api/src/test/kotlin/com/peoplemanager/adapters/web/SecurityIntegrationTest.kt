package com.peoplemanager.adapters.web

import com.peoplemanager.adapters.auth.SecurityConfig
import com.peoplemanager.adapters.auth.UserProvisioningJwtAuthenticationConverter
import com.peoplemanager.application.UserProvisioningService
import com.peoplemanager.domain.User
import com.peoplemanager.domain.UserId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.stereotype.Controller
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseBody
import java.time.Instant
import java.util.UUID

@WebMvcTest(controllers = [TestSecurityController::class])
@Import(SecurityConfig::class, UserProvisioningJwtAuthenticationConverter::class)
@TestPropertySource(properties = [
    "spring.datasource.url=jdbc:h2:mem:test",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://auth.example.com",
    "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://auth.example.com/jwks"
])
class SecurityIntegrationTest {

    @TestConfiguration
    class TestConfig {
        @Bean
        fun userProvisioningService(): UserProvisioningService = mockk()
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userProvisioningService: UserProvisioningService

    @Test
    fun `should return 401 when no JWT is present`() {
        mockMvc.perform(get("/api/v1/test"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should return 401 when invalid JWT token is provided`() {
        mockMvc.perform(
            get("/api/v1/test")
                .header("Authorization", "Bearer invalid-token")
        ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `should allow authenticated access with valid JWT`() {
        // The jwt() post-processor bypasses actual JWT decoding and sets
        // authentication directly in the SecurityContext.
        mockMvc.perform(
            get("/api/v1/test")
                .with(jwt().jwt { builder ->
                    builder
                        .subject("sub-test")
                        .issuer("https://auth.example.com")
                        .claim("name", "Test User")
                })
        ).andExpect(status().isOk)
    }

    @Test
    fun `should allow unauthenticated access to actuator health endpoint`() {
        // Security config permits /actuator/health without auth.
        // In a @WebMvcTest slice, actuator endpoints are not loaded, so we
        // verify the security layer does not return 401 (it returns 404 instead).
        val result = mockMvc.perform(get("/actuator/health")).andReturn()
        result.response.status shouldNotBe 401
    }

    @Test
    fun `converter should provision user and set userId in token details`() {
        val userId = UserId(UUID.randomUUID())
        val user = User(
            id = userId,
            oidcSubject = "sub-123",
            oidcIssuer = "https://auth.example.com",
            displayName = "Test User",
            email = "test@example.com",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        every {
            userProvisioningService.provisionUser(
                "sub-123",
                "https://auth.example.com",
                "Test User",
                "test@example.com"
            )
        } returns user

        val converter = UserProvisioningJwtAuthenticationConverter(userProvisioningService)

        val jwt = Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .subject("sub-123")
            .issuer("https://auth.example.com")
            .claim("name", "Test User")
            .claim("email", "test@example.com")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build()

        val authentication = converter.convert(jwt)

        authentication.isAuthenticated shouldBe true
        authentication.details shouldBe userId

        verify(exactly = 1) {
            userProvisioningService.provisionUser(
                "sub-123",
                "https://auth.example.com",
                "Test User",
                "test@example.com"
            )
        }
    }

    @Test
    fun `converter should throw when JWT is missing subject claim`() {
        val converter = UserProvisioningJwtAuthenticationConverter(userProvisioningService)

        val jwt = Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .issuer("https://auth.example.com")
            .claim("email", "test@example.com")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build()

        org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            converter.convert(jwt)
        }
    }

    @Test
    fun `converter should throw when JWT is missing issuer claim`() {
        val converter = UserProvisioningJwtAuthenticationConverter(userProvisioningService)

        val jwt = Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .subject("sub-123")
            .claim("email", "test@example.com")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build()

        org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            converter.convert(jwt)
        }
    }
}

/**
 * Minimal controller used only for security integration testing.
 */
@Controller
class TestSecurityController {
    @GetMapping("/api/v1/test")
    @ResponseBody
    fun testEndpoint(): Map<String, String> = mapOf("status" to "ok")
}
