package com.peoplemanager.application

import com.peoplemanager.application.ports.SearchQueryPort
import com.peoplemanager.application.ports.SearchRepository
import com.peoplemanager.application.queries.SearchQuery
import com.peoplemanager.domain.SearchResults
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.math.ceil

@Service
@Transactional(readOnly = true)
class SearchService(
    private val searchRepository: SearchRepository
) : SearchQueryPort {

    override fun search(query: SearchQuery): SearchResults {
        val sanitizedQuery = sanitizeSearchQuery(query.query)
        if (sanitizedQuery.isBlank()) {
            return SearchResults(
                results = emptyList(),
                query = query.query,
                totalCount = 0,
                page = query.page,
                size = query.size,
                totalPages = 0
            )
        }

        val offset = query.page * query.size
        val (results, totalCount) = searchRepository.search(
            userId = query.userId,
            searchTerms = sanitizedQuery,
            types = query.types,
            offset = offset,
            limit = query.size
        )

        val totalPages = if (totalCount == 0L) 0 else ceil(totalCount.toDouble() / query.size).toInt()

        return SearchResults(
            results = results,
            query = query.query,
            totalCount = totalCount,
            page = query.page,
            size = query.size,
            totalPages = totalPages
        )
    }

    /**
     * Sanitize the search query for use with PostgreSQL full-text search.
     * Removes special characters that could break tsquery parsing and
     * converts space-separated words to AND-connected terms.
     */
    private fun sanitizeSearchQuery(input: String): String {
        // Remove characters that are special in tsquery syntax
        val cleaned = input.replace(Regex("[!&|():*<>'\"]"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")

        if (cleaned.isBlank()) return ""

        // Convert space-separated words to prefix-match terms connected with AND
        // This allows partial word matching (e.g., "dev" matches "development")
        return cleaned.split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" & ") { "$it:*" }
    }
}
