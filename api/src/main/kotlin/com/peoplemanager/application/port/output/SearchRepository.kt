package com.peoplemanager.application.port.output

import com.peoplemanager.domain.SearchResult
import com.peoplemanager.domain.SearchResultType
import com.peoplemanager.domain.UserId

interface SearchRepository {
    /**
     * Perform full-text search across all manager data, scoped by userId.
     * Returns results ordered by relevance score descending.
     *
     * @param userId The authenticated manager's user ID (security scoping)
     * @param searchTerms The search query string (will be converted to tsquery)
     * @param types Optional filter to restrict results to specific types
     * @param offset Pagination offset
     * @param limit Pagination limit
     * @return Pair of (results list, total count)
     */
    fun search(
        userId: UserId,
        searchTerms: String,
        types: List<SearchResultType>?,
        offset: Int,
        limit: Int
    ): Pair<List<SearchResult>, Long>
}
