package com.peoplemanager.adapters.auth

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.test.util.ReflectionTestUtils

/**
 * Guards against the Spring Security 7 / Spring Boot 4 regression where the
 * default JWKS `RestOperations` uses a 500ms connect AND read timeout
 * (`NimbusJwtDecoder$RestTemplateWithNimbusDefaultTimeouts`). A remote OIDC
 * provider reached over the internet routinely needs more than 500ms for the
 * TLS handshake plus response, producing "Read timed out" on JWKS retrieval
 * even though the endpoint is perfectly reachable (e.g. via curl).
 */
class JwksHttpClientConfigTest {

    @Test
    fun `builds a JWKS request factory with the configured timeouts`() {
        val config = JwksHttpClientConfig(connectTimeoutMs = 5000, readTimeoutMs = 10000)

        val factory = config.jwksRequestFactory()

        ReflectionTestUtils.getField(factory, "connectTimeout") shouldBe 5000
        ReflectionTestUtils.getField(factory, "readTimeout") shouldBe 10000
    }

    @Test
    fun `read timeout is well above the 500ms default that caused JWKS read timeouts`() {
        val config = JwksHttpClientConfig(connectTimeoutMs = 5000, readTimeoutMs = 10000)

        val readTimeout = ReflectionTestUtils.getField(config.jwksRequestFactory(), "readTimeout") as Int

        (readTimeout > 500) shouldBe true
    }

    @Test
    fun `customizer applies the configured rest operations to the decoder builder`() {
        val config = JwksHttpClientConfig(connectTimeoutMs = 1234, readTimeoutMs = 5678)
        val builder = NimbusJwtDecoder.withJwkSetUri("https://example.com/application/o/app/jwks/")

        // Applying our customizer must not throw and swaps in our RestTemplate.
        config.jwksTimeoutCustomizer().customize(builder)
    }
}
