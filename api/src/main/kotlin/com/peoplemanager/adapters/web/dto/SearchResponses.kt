package com.peoplemanager.adapters.web.dto

import com.peoplemanager.domain.SearchResult
import com.peoplemanager.domain.SearchResultType
import com.peoplemanager.domain.SearchResults
import java.time.Instant
import java.util.UUID

data class SearchResultResponse(
    val id: UUID,
    val type: SearchResultType,
    val title: String,
    val snippet: String?,
    val personId: UUID?,
    val personName: String?,
    val sensitive: Boolean,
    val createdAt: Instant,
    val relevanceScore: Double
) {
    companion object {
        fun from(result: SearchResult): SearchResultResponse = SearchResultResponse(
            id = result.id,
            type = result.type,
            title = result.title,
            snippet = if (result.sensitive) null else result.snippet,
            personId = result.personId,
            personName = result.personName,
            sensitive = result.sensitive,
            createdAt = result.createdAt,
            relevanceScore = result.relevanceScore
        )
    }
}

data class SearchResponse(
    val results: List<SearchResultResponse>,
    val query: String,
    val totalCount: Long,
    val page: Int,
    val size: Int,
    val totalPages: Int
) {
    companion object {
        fun from(searchResults: SearchResults): SearchResponse = SearchResponse(
            results = searchResults.results.map { SearchResultResponse.from(it) },
            query = searchResults.query,
            totalCount = searchResults.totalCount,
            page = searchResults.page,
            size = searchResults.size,
            totalPages = searchResults.totalPages
        )
    }
}
