package com.peoplemanager.domain

import java.time.Instant
import java.util.UUID

/**
 * Represents a single search result from full-text search across all manager data.
 * This is a read-only value object — not an aggregate.
 */
data class SearchResult(
    val id: UUID,
    val type: SearchResultType,
    val title: String,
    val snippet: String?,
    val personId: UUID?,
    val personName: String?,
    val sensitive: Boolean = false,
    val createdAt: Instant,
    val relevanceScore: Double = 0.0
)

enum class SearchResultType {
    PERSON,
    ONE_ON_ONE_ENTRY,
    QUICK_NOTE,
    ACTION_ITEM,
    PDP_GOAL,
    PDP_UPDATE,
    KUDOS,
    STRATEGY_GOAL
}

/**
 * Paginated search results with metadata.
 */
data class SearchResults(
    val results: List<SearchResult>,
    val query: String,
    val totalCount: Long,
    val page: Int,
    val size: Int,
    val totalPages: Int
)
