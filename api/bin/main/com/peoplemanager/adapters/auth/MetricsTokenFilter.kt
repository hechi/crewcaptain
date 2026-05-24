package com.peoplemanager.adapters.auth

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Filter that secures the /actuator/prometheus endpoint with a bearer token.
 * If no METRICS_TOKEN is configured, the endpoint is inaccessible (returns 403).
 */
class MetricsTokenFilter(
    private val metricsToken: String
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        if (request.requestURI == "/actuator/prometheus") {
            if (metricsToken.isBlank()) {
                response.status = HttpServletResponse.SC_FORBIDDEN
                response.contentType = MediaType.APPLICATION_JSON_VALUE
                response.writer.write("""{"status":403,"error":"Forbidden","message":"Metrics endpoint is disabled (no METRICS_TOKEN configured)"}""")
                return
            }

            val authHeader = request.getHeader("Authorization")
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                response.status = HttpServletResponse.SC_UNAUTHORIZED
                response.contentType = MediaType.APPLICATION_JSON_VALUE
                response.writer.write("""{"status":401,"error":"Unauthorized","message":"Bearer token required"}""")
                return
            }

            val token = authHeader.removePrefix("Bearer ").trim()
            if (token != metricsToken) {
                response.status = HttpServletResponse.SC_UNAUTHORIZED
                response.contentType = MediaType.APPLICATION_JSON_VALUE
                response.writer.write("""{"status":401,"error":"Unauthorized","message":"Invalid metrics token"}""")
                return
            }
        }

        filterChain.doFilter(request, response)
    }
}
