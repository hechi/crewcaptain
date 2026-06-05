package com.peoplemanager.application

import com.peoplemanager.application.port.output.SearchRepository
import com.peoplemanager.application.queries.SearchQuery
import com.peoplemanager.domain.SearchResult
import com.peoplemanager.domain.SearchResultType
import com.peoplemanager.domain.UserId
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.util.UUID

class SearchServiceTest {

    private lateinit var searchRepository: SearchRepository
    private lateinit var searchService: SearchService

    private val userId = UserId(UUID.randomUUID())

    @BeforeEach
    fun setUp() {
        searchRepository = mockk()
        searchService = SearchService(searchRepository)
    }

    @Test
    fun `should return search results with pagination`() {
        val results = listOf(
            SearchResult(
                id = UUID.randomUUID(),
                type = SearchResultType.PERSON,
                title = "John Doe",
                snippet = "Software Engineer",
                personId = UUID.randomUUID(),
                personName = "John Doe",
                createdAt = Instant.now(),
                relevanceScore = 0.9
            ),
            SearchResult(
                id = UUID.randomUUID(),
                type = SearchResultType.ACTION_ITEM,
                title = "Review John's code",
                snippet = "Need to review PR",
                personId = UUID.randomUUID(),
                personName = "John Doe",
                createdAt = Instant.now(),
                relevanceScore = 0.7
            )
        )

        every { searchRepository.search(userId, "john:*", null, 0, 20) } returns Pair(results, 2L)

        val query = SearchQuery(userId = userId, query = "john")
        val searchResults = searchService.search(query)

        searchResults.results.size shouldBe 2
        searchResults.totalCount shouldBe 2
        searchResults.page shouldBe 0
        searchResults.size shouldBe 20
        searchResults.totalPages shouldBe 1
        searchResults.query shouldBe "john"
    }

    @Test
    fun `should return empty results for blank sanitized query`() {
        val query = SearchQuery(userId = userId, query = "!&|()*'\"")
        val searchResults = searchService.search(query)

        searchResults.results shouldBe emptyList()
        searchResults.totalCount shouldBe 0
        searchResults.totalPages shouldBe 0
    }

    @Test
    fun `should sanitize special characters from query`() {
        every { searchRepository.search(userId, "hello:* & world:*", null, 0, 20) } returns Pair(emptyList(), 0L)

        val query = SearchQuery(userId = userId, query = "hello & world")
        searchService.search(query)

        verify { searchRepository.search(userId, "hello:* & world:*", null, 0, 20) }
    }

    @Test
    fun `should convert multi-word query to AND-connected prefix terms`() {
        every { searchRepository.search(userId, "software:* & engineer:*", null, 0, 20) } returns Pair(emptyList(), 0L)

        val query = SearchQuery(userId = userId, query = "software engineer")
        searchService.search(query)

        verify { searchRepository.search(userId, "software:* & engineer:*", null, 0, 20) }
    }

    @Test
    fun `should pass type filter to repository`() {
        val types = listOf(SearchResultType.PERSON, SearchResultType.ACTION_ITEM)
        every { searchRepository.search(userId, "test:*", types, 0, 20) } returns Pair(emptyList(), 0L)

        val query = SearchQuery(userId = userId, query = "test", types = types)
        searchService.search(query)

        verify { searchRepository.search(userId, "test:*", types, 0, 20) }
    }

    @Test
    fun `should calculate correct offset for pagination`() {
        every { searchRepository.search(userId, "test:*", null, 40, 20) } returns Pair(emptyList(), 50L)

        val query = SearchQuery(userId = userId, query = "test", page = 2, size = 20)
        val searchResults = searchService.search(query)

        searchResults.page shouldBe 2
        searchResults.totalPages shouldBe 3
        verify { searchRepository.search(userId, "test:*", null, 40, 20) }
    }

    @Test
    fun `should calculate total pages correctly`() {
        every { searchRepository.search(userId, "test:*", null, 0, 10) } returns Pair(emptyList(), 25L)

        val query = SearchQuery(userId = userId, query = "test", size = 10)
        val searchResults = searchService.search(query)

        searchResults.totalPages shouldBe 3
    }

    @Test
    fun `should return zero total pages when no results`() {
        every { searchRepository.search(userId, "nothing:*", null, 0, 20) } returns Pair(emptyList(), 0L)

        val query = SearchQuery(userId = userId, query = "nothing")
        val searchResults = searchService.search(query)

        searchResults.totalPages shouldBe 0
        searchResults.totalCount shouldBe 0
    }

    @Test
    fun `should strip quotes from query`() {
        every { searchRepository.search(userId, "hello:* & world:*", null, 0, 20) } returns Pair(emptyList(), 0L)

        val query = SearchQuery(userId = userId, query = "\"hello world\"")
        searchService.search(query)

        verify { searchRepository.search(userId, "hello:* & world:*", null, 0, 20) }
    }

    @Test
    fun `should strip parentheses from query`() {
        every { searchRepository.search(userId, "test:*", null, 0, 20) } returns Pair(emptyList(), 0L)

        val query = SearchQuery(userId = userId, query = "(test)")
        searchService.search(query)

        verify { searchRepository.search(userId, "test:*", null, 0, 20) }
    }

    @Test
    fun `should handle single word query`() {
        every { searchRepository.search(userId, "development:*", null, 0, 20) } returns Pair(emptyList(), 0L)

        val query = SearchQuery(userId = userId, query = "development")
        searchService.search(query)

        verify { searchRepository.search(userId, "development:*", null, 0, 20) }
    }

    @Test
    fun `should reject blank query`() {
        assertThrows<IllegalArgumentException> {
            SearchQuery(userId = userId, query = "")
        }
    }

    @Test
    fun `should reject negative page`() {
        assertThrows<IllegalArgumentException> {
            SearchQuery(userId = userId, query = "test", page = -1)
        }
    }

    @Test
    fun `should reject size over 100`() {
        assertThrows<IllegalArgumentException> {
            SearchQuery(userId = userId, query = "test", size = 101)
        }
    }

    @Test
    fun `should reject size of 0`() {
        assertThrows<IllegalArgumentException> {
            SearchQuery(userId = userId, query = "test", size = 0)
        }
    }

    @Test
    fun `should preserve original query in results`() {
        every { searchRepository.search(userId, "hello:* & world:*", null, 0, 20) } returns Pair(emptyList(), 0L)

        val query = SearchQuery(userId = userId, query = "hello world")
        val searchResults = searchService.search(query)

        searchResults.query shouldBe "hello world"
    }
}
