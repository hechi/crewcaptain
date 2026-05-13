package com.peoplemanager.adapters.auth

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class MetricsTokenFilterTest {

    private val validToken = "my-secret-metrics-token"

    @Test
    fun `should allow request with valid bearer token`() {
        val filter = MetricsTokenFilter(validToken)
        val request = MockHttpServletRequest("GET", "/actuator/prometheus")
        request.addHeader("Authorization", "Bearer $validToken")
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()

        filter.doFilter(request, response, chain)

        response.status shouldBe 200
        chain.request shouldBe request
    }

    @Test
    fun `should return 401 when no Authorization header is present`() {
        val filter = MetricsTokenFilter(validToken)
        val request = MockHttpServletRequest("GET", "/actuator/prometheus")
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()

        filter.doFilter(request, response, chain)

        response.status shouldBe 401
        response.contentAsString shouldContain "Bearer token required"
        chain.request shouldBe null
    }

    @Test
    fun `should return 401 when Authorization header does not start with Bearer`() {
        val filter = MetricsTokenFilter(validToken)
        val request = MockHttpServletRequest("GET", "/actuator/prometheus")
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz")
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()

        filter.doFilter(request, response, chain)

        response.status shouldBe 401
        response.contentAsString shouldContain "Bearer token required"
        chain.request shouldBe null
    }

    @Test
    fun `should return 401 when token is invalid`() {
        val filter = MetricsTokenFilter(validToken)
        val request = MockHttpServletRequest("GET", "/actuator/prometheus")
        request.addHeader("Authorization", "Bearer wrong-token")
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()

        filter.doFilter(request, response, chain)

        response.status shouldBe 401
        response.contentAsString shouldContain "Invalid metrics token"
        chain.request shouldBe null
    }

    @Test
    fun `should return 403 when no METRICS_TOKEN is configured`() {
        val filter = MetricsTokenFilter("")
        val request = MockHttpServletRequest("GET", "/actuator/prometheus")
        request.addHeader("Authorization", "Bearer some-token")
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()

        filter.doFilter(request, response, chain)

        response.status shouldBe 403
        response.contentAsString shouldContain "Metrics endpoint is disabled"
        chain.request shouldBe null
    }

    @Test
    fun `should return 403 when METRICS_TOKEN is blank`() {
        val filter = MetricsTokenFilter("   ")
        val request = MockHttpServletRequest("GET", "/actuator/prometheus")
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()

        filter.doFilter(request, response, chain)

        response.status shouldBe 403
        response.contentAsString shouldContain "Metrics endpoint is disabled"
        chain.request shouldBe null
    }

    @Test
    fun `should not filter non-prometheus requests`() {
        val filter = MetricsTokenFilter(validToken)
        val request = MockHttpServletRequest("GET", "/api/v1/persons")
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()

        filter.doFilter(request, response, chain)

        response.status shouldBe 200
        chain.request shouldBe request
    }

    @Test
    fun `should not filter health endpoint`() {
        val filter = MetricsTokenFilter(validToken)
        val request = MockHttpServletRequest("GET", "/actuator/health")
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()

        filter.doFilter(request, response, chain)

        response.status shouldBe 200
        chain.request shouldBe request
    }

    @Test
    fun `should handle token with extra whitespace by trimming`() {
        val filter = MetricsTokenFilter(validToken)
        val request = MockHttpServletRequest("GET", "/actuator/prometheus")
        request.addHeader("Authorization", "Bearer   $validToken  ")
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()

        filter.doFilter(request, response, chain)

        // Token is trimmed, so it should match
        response.status shouldBe 200
        chain.request shouldBe request
    }
}
