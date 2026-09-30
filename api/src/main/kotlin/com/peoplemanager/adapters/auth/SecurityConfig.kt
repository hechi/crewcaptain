package com.peoplemanager.adapters.auth

import tools.jackson.databind.json.JsonMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.intercept.AuthorizationFilter
import java.time.Instant

@Configuration
@EnableWebSecurity
class SecurityConfig(
    @Value("\${app.metrics.token:}") private val metricsToken: String
) {

    /**
     * Public, unauthenticated chain for the feedback submission endpoints.
     * This is the only place in the app that permits anonymous access, and it is
     * scoped narrowly to the public feedback path. A per-IP rate-limit filter guards
     * the write endpoint. No bearer token is parsed here.
     */
    @Bean
    @Order(0)
    fun publicFeedbackSecurityFilterChain(
        http: HttpSecurity,
        rateLimitFilter: PublicFeedbackRateLimitFilter
    ): SecurityFilterChain {
        http
            .securityMatcher("/api/v1/public/feedback/**")
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth -> auth.anyRequest().permitAll() }
            .addFilterBefore(rateLimitFilter, AuthorizationFilter::class.java)
        return http.build()
    }

    @Bean
    @Order(1)
    fun actuatorSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .securityMatcher("/actuator/**")
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth.requestMatchers("/actuator/health").permitAll()
                auth.requestMatchers("/actuator/prometheus").permitAll()
                auth.anyRequest().denyAll()
            }
            .addFilterBefore(
                MetricsTokenFilter(metricsToken),
                AuthorizationFilter::class.java
            )
        return http.build()
    }

    @Bean
    @Order(2)
    fun securityFilterChain(
        http: HttpSecurity,
        jwtConverter: UserProvisioningJwtAuthenticationConverter
    ): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth.anyRequest().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.jwtAuthenticationConverter(jwtConverter)
                }
            }
            .exceptionHandling { exceptions ->
                exceptions.authenticationEntryPoint(jsonAuthenticationEntryPoint())
            }
        return http.build()
    }

    @Bean
    fun jsonAuthenticationEntryPoint(): AuthenticationEntryPoint {
        return AuthenticationEntryPoint { _: HttpServletRequest, response: HttpServletResponse, authException: AuthenticationException ->
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            response.contentType = MediaType.APPLICATION_JSON_VALUE
            val objectMapper = JsonMapper.builder().build()
            val body = mapOf(
                "status" to 401,
                "error" to "Unauthorized",
                "message" to (authException.message ?: "Authentication required"),
                "timestamp" to Instant.now().toString()
            )
            response.writer.write(objectMapper.writeValueAsString(body))
        }
    }
}
