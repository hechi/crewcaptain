package com.peoplemanager.integration

import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class PrometheusMetricsIntegrationTest {

    companion object {
        private const val TEST_METRICS_TOKEN = "test-metrics-token-12345"

        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16")

        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
            registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri") { "http://localhost:9000" }
            registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri") { "http://localhost:9000/jwks" }
            registry.add("app.metrics.token") { TEST_METRICS_TOKEN }
            registry.add("management.endpoints.web.exposure.include") { "health,prometheus" }
            registry.add("management.endpoint.prometheus.enabled") { "true" }
        }
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `should return prometheus metrics with valid token`() {
        val result = mockMvc.perform(
            get("/actuator/prometheus")
                .header("Authorization", "Bearer $TEST_METRICS_TOKEN")
        )
            .andExpect(status().isOk)
            .andReturn()

        val body = result.response.contentAsString
        body shouldContain "jvm_memory"
        body shouldContain "crewcaptain_one_on_ones"
        body shouldContain "crewcaptain_one_on_ones_last_7_days"
    }

    @Test
    fun `should return 401 without token`() {
        mockMvc.perform(get("/actuator/prometheus"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should return 401 with invalid token`() {
        mockMvc.perform(
            get("/actuator/prometheus")
                .header("Authorization", "Bearer wrong-token")
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should still allow health endpoint without any token`() {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk)
    }

    @Test
    fun `should include application tag in metrics`() {
        val result = mockMvc.perform(
            get("/actuator/prometheus")
                .header("Authorization", "Bearer $TEST_METRICS_TOKEN")
        )
            .andExpect(status().isOk)
            .andReturn()

        val body = result.response.contentAsString
        body shouldContain "application=\"crewcaptain\""
    }

    @Test
    fun `should expose hikari connection pool metrics`() {
        val result = mockMvc.perform(
            get("/actuator/prometheus")
                .header("Authorization", "Bearer $TEST_METRICS_TOKEN")
        )
            .andExpect(status().isOk)
            .andReturn()

        val body = result.response.contentAsString
        body shouldContain "hikaricp"
    }
}
