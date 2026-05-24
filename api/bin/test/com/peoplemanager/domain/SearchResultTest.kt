package com.peoplemanager.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class SearchResultTest {

    @Test
    fun `should create SearchResult with all fields`() {
        val id = UUID.randomUUID()
        val personId = UUID.randomUUID()
        val now = Instant.now()

        val result = SearchResult(
            id = id,
            type = SearchResultType.PERSON,
            title = "John Doe",
            snippet = "Software Engineer",
            personId = personId,
            personName = "John Doe",
            sensitive = false,
            createdAt = now,
            relevanceScore = 0.85
        )

        result.id shouldBe id
        result.type shouldBe SearchResultType.PERSON
        result.title shouldBe "John Doe"
        result.snippet shouldBe "Software Engineer"
        result.personId shouldBe personId
        result.personName shouldBe "John Doe"
        result.sensitive shouldBe false
        result.createdAt shouldBe now
        result.relevanceScore shouldBe 0.85
    }

    @Test
    fun `should create SearchResult with null optional fields`() {
        val result = SearchResult(
            id = UUID.randomUUID(),
            type = SearchResultType.QUICK_NOTE,
            title = "A quick note",
            snippet = null,
            personId = null,
            personName = null,
            sensitive = true,
            createdAt = Instant.now(),
            relevanceScore = 0.5
        )

        result.snippet shouldBe null
        result.personId shouldBe null
        result.personName shouldBe null
        result.sensitive shouldBe true
    }

    @Test
    fun `should create SearchResult with default values`() {
        val result = SearchResult(
            id = UUID.randomUUID(),
            type = SearchResultType.ACTION_ITEM,
            title = "Follow up",
            snippet = "Description",
            personId = UUID.randomUUID(),
            personName = "Jane",
            createdAt = Instant.now()
        )

        result.sensitive shouldBe false
        result.relevanceScore shouldBe 0.0
    }

    @Test
    fun `SearchResultType should have all expected values`() {
        val types = SearchResultType.entries
        types.size shouldBe 8
        types shouldBe listOf(
            SearchResultType.PERSON,
            SearchResultType.ONE_ON_ONE_ENTRY,
            SearchResultType.QUICK_NOTE,
            SearchResultType.ACTION_ITEM,
            SearchResultType.PDP_GOAL,
            SearchResultType.PDP_UPDATE,
            SearchResultType.KUDOS,
            SearchResultType.STRATEGY_GOAL
        )
    }

    @Test
    fun `should create SearchResults with pagination metadata`() {
        val results = SearchResults(
            results = listOf(
                SearchResult(
                    id = UUID.randomUUID(),
                    type = SearchResultType.PERSON,
                    title = "Test",
                    snippet = null,
                    personId = null,
                    personName = null,
                    createdAt = Instant.now()
                )
            ),
            query = "test",
            totalCount = 25,
            page = 1,
            size = 10,
            totalPages = 3
        )

        results.results.size shouldBe 1
        results.query shouldBe "test"
        results.totalCount shouldBe 25
        results.page shouldBe 1
        results.size shouldBe 10
        results.totalPages shouldBe 3
    }

    @Test
    fun `should create empty SearchResults`() {
        val results = SearchResults(
            results = emptyList(),
            query = "nonexistent",
            totalCount = 0,
            page = 0,
            size = 20,
            totalPages = 0
        )

        results.results shouldBe emptyList()
        results.totalCount shouldBe 0
        results.totalPages shouldBe 0
    }
}
