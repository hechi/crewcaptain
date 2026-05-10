package com.peoplemanager.adapters.web

import com.peoplemanager.adapters.auth.SecurityConfig
import com.peoplemanager.adapters.auth.UserProvisioningJwtAuthenticationConverter
import com.peoplemanager.application.UserProvisioningService
import com.peoplemanager.application.ports.SearchQueryPort
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

@WebMvcTest(controllers = [SearchController::class])
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
class SearchControllerTest {

    @TestConfiguration
    class TestConfig {
        @Bean
        fun searchQueryPort(): SearchQueryPort = mockk()

        @Bean
        fun userProvisioningService(): UserProvisioningService = mockk()
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var searchQueryPort: SearchQueryPort

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

    private val userId = UserId(UUID.randomUUID())

    @Test
    fun `should return 401 when not authenticated`() {
        mockMvc.perform(get("/api/v1/search").param("q", "test"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should return 400 when query is blank`() {
        mockMvc.perform(
            get("/api/v1/search")
                .param("q", "  ")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should return 400 when query parameter is missing`() {
        mockMvc.perform(
            get("/api/v1/search")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should return empty results when no matches`() {
        every { searchQueryPort.search(any()) } returns SearchResults(
            results = emptyList(),
            query = "nonexistent",
            totalCount = 0,
            page = 0,
            size = 20,
            totalPages = 0
        )

        mockMvc.perform(
            get("/api/v1/search")
                .param("q", "nonexistent")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.results").isArray)
            .andExpect(jsonPath("$.results").isEmpty)
            .andExpect(jsonPath("$.query").value("nonexistent"))
            .andExpect(jsonPath("$.totalCount").value(0))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalPages").value(0))
    }

    @Test
    fun `should return search results with all fields`() {
        val personId = UUID.randomUUID()
        val resultId = UUID.randomUUID()

        every { searchQueryPort.search(any()) } returns SearchResults(
            results = listOf(
                SearchResult(
                    id = resultId,
                    type = SearchResultType.PERSON,
                    title = "John Doe",
                    snippet = "Software Engineer",
                    personId = personId,
                    personName = "John Doe",
                    sensitive = false,
                    createdAt = Instant.parse("2026-05-10T10:00:00Z"),
                    relevanceScore = 0.85
                )
            ),
            query = "john",
            totalCount = 1,
            page = 0,
            size = 20,
            totalPages = 1
        )

        mockMvc.perform(
            get("/api/v1/search")
                .param("q", "john")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.results[0].id").value(resultId.toString()))
            .andExpect(jsonPath("$.results[0].type").value("PERSON"))
            .andExpect(jsonPath("$.results[0].title").value("John Doe"))
            .andExpect(jsonPath("$.results[0].snippet").value("Software Engineer"))
            .andExpect(jsonPath("$.results[0].personId").value(personId.toString()))
            .andExpect(jsonPath("$.results[0].personName").value("John Doe"))
            .andExpect(jsonPath("$.results[0].sensitive").value(false))
            .andExpect(jsonPath("$.results[0].relevanceScore").value(0.85))
            .andExpect(jsonPath("$.query").value("john"))
            .andExpect(jsonPath("$.totalCount").value(1))
    }

    @Test
    fun `should hide snippet for sensitive results`() {
        every { searchQueryPort.search(any()) } returns SearchResults(
            results = listOf(
                SearchResult(
                    id = UUID.randomUUID(),
                    type = SearchResultType.QUICK_NOTE,
                    title = "Sensitive note",
                    snippet = "This should be hidden",
                    personId = null,
                    personName = null,
                    sensitive = true,
                    createdAt = Instant.now(),
                    relevanceScore = 0.7
                )
            ),
            query = "sensitive",
            totalCount = 1,
            page = 0,
            size = 20,
            totalPages = 1
        )

        mockMvc.perform(
            get("/api/v1/search")
                .param("q", "sensitive")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.results[0].sensitive").value(true))
            .andExpect(jsonPath("$.results[0].snippet").doesNotExist())
    }

    @Test
    fun `should pass type filter to search query`() {
        every { searchQueryPort.search(any()) } returns SearchResults(
            results = emptyList(),
            query = "test",
            totalCount = 0,
            page = 0,
            size = 20,
            totalPages = 0
        )

        mockMvc.perform(
            get("/api/v1/search")
                .param("q", "test")
                .param("type", "PERSON")
                .param("type", "ACTION_ITEM")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)

        verify {
            searchQueryPort.search(match {
                it.types == listOf(SearchResultType.PERSON, SearchResultType.ACTION_ITEM)
            })
        }
    }

    @Test
    fun `should pass pagination parameters`() {
        every { searchQueryPort.search(any()) } returns SearchResults(
            results = emptyList(),
            query = "test",
            totalCount = 0,
            page = 2,
            size = 10,
            totalPages = 0
        )

        mockMvc.perform(
            get("/api/v1/search")
                .param("q", "test")
                .param("page", "2")
                .param("size", "10")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)

        verify {
            searchQueryPort.search(match {
                it.page == 2 && it.size == 10
            })
        }
    }

    @Test
    fun `should cap size at 100`() {
        every { searchQueryPort.search(any()) } returns SearchResults(
            results = emptyList(),
            query = "test",
            totalCount = 0,
            page = 0,
            size = 100,
            totalPages = 0
        )

        mockMvc.perform(
            get("/api/v1/search")
                .param("q", "test")
                .param("size", "500")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)

        verify {
            searchQueryPort.search(match {
                it.size == 100
            })
        }
    }

    @Test
    fun `should return multiple result types`() {
        every { searchQueryPort.search(any()) } returns SearchResults(
            results = listOf(
                SearchResult(
                    id = UUID.randomUUID(),
                    type = SearchResultType.PERSON,
                    title = "Development Team Lead",
                    snippet = null,
                    personId = UUID.randomUUID(),
                    personName = "Development Team Lead",
                    createdAt = Instant.now(),
                    relevanceScore = 0.9
                ),
                SearchResult(
                    id = UUID.randomUUID(),
                    type = SearchResultType.PDP_GOAL,
                    title = "Leadership development",
                    snippet = "Develop leadership skills",
                    personId = UUID.randomUUID(),
                    personName = "Alice",
                    createdAt = Instant.now(),
                    relevanceScore = 0.8
                ),
                SearchResult(
                    id = UUID.randomUUID(),
                    type = SearchResultType.ACTION_ITEM,
                    title = "Complete development plan",
                    snippet = null,
                    personId = UUID.randomUUID(),
                    personName = "Bob",
                    createdAt = Instant.now(),
                    relevanceScore = 0.7
                )
            ),
            query = "development",
            totalCount = 3,
            page = 0,
            size = 20,
            totalPages = 1
        )

        mockMvc.perform(
            get("/api/v1/search")
                .param("q", "development")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.results.length()").value(3))
            .andExpect(jsonPath("$.results[0].type").value("PERSON"))
            .andExpect(jsonPath("$.results[1].type").value("PDP_GOAL"))
            .andExpect(jsonPath("$.results[2].type").value("ACTION_ITEM"))
            .andExpect(jsonPath("$.totalCount").value(3))
    }

    @Test
    fun `should scope search by authenticated user`() {
        every { searchQueryPort.search(any()) } returns SearchResults(
            results = emptyList(),
            query = "test",
            totalCount = 0,
            page = 0,
            size = 20,
            totalPages = 0
        )

        mockMvc.perform(
            get("/api/v1/search")
                .param("q", "test")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)

        verify {
            searchQueryPort.search(match {
                it.userId == userId
            })
        }
    }

    @Test
    fun `should use default pagination when not specified`() {
        every { searchQueryPort.search(any()) } returns SearchResults(
            results = emptyList(),
            query = "test",
            totalCount = 0,
            page = 0,
            size = 20,
            totalPages = 0
        )

        mockMvc.perform(
            get("/api/v1/search")
                .param("q", "test")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)

        verify {
            searchQueryPort.search(match {
                it.page == 0 && it.size == 20
            })
        }
    }

    @Test
    fun `should handle search without type filter`() {
        every { searchQueryPort.search(any()) } returns SearchResults(
            results = emptyList(),
            query = "test",
            totalCount = 0,
            page = 0,
            size = 20,
            totalPages = 0
        )

        mockMvc.perform(
            get("/api/v1/search")
                .param("q", "test")
                .with(authentication(authenticatedJwt(userId)))
        )
            .andExpect(status().isOk)

        verify {
            searchQueryPort.search(match {
                it.types == null
            })
        }
    }
}
