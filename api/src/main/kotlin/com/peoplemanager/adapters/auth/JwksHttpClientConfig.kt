package com.peoplemanager.adapters.auth

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.JwkSetUriJwtDecoderBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestTemplate

/**
 * Configures the HTTP client used by the OAuth2 resource server to fetch the
 * OIDC provider's JWKS (JSON Web Key Set) for JWT signature verification.
 *
 * Spring Security 7 (Spring Boot 4) changed the default JWKS `RestOperations`
 * to a `RestTemplate` with a **500ms connect and 500ms read timeout**
 * (`NimbusJwtDecoder.RestTemplateWithNimbusDefaultTimeouts`). Spring Security 6
 * used effectively unbounded timeouts. For a remote provider reached over the
 * internet, 500ms is frequently too short (TLS handshake + response), so JWKS
 * retrieval fails with `ResourceAccessException: ... Read timed out` even
 * though the endpoint is reachable.
 *
 * This customizer overrides only the HTTP client with sane, configurable
 * timeouts, leaving the rest of Spring Boot's JWT decoder auto-configuration
 * (issuer validation, JWS algorithm discovery, etc.) untouched.
 */
@Configuration
class JwksHttpClientConfig(
    @param:Value("\${app.security.jwks.connect-timeout-ms:5000}") private val connectTimeoutMs: Int,
    @param:Value("\${app.security.jwks.read-timeout-ms:10000}") private val readTimeoutMs: Int,
) {

    fun jwksRequestFactory(): SimpleClientHttpRequestFactory =
        SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(connectTimeoutMs)
            setReadTimeout(readTimeoutMs)
        }

    @Bean
    fun jwksTimeoutCustomizer(): JwkSetUriJwtDecoderBuilderCustomizer =
        JwkSetUriJwtDecoderBuilderCustomizer { builder ->
            builder.restOperations(RestTemplate(jwksRequestFactory()))
        }
}
