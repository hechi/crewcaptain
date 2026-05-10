package com.peoplemanager.application.queries

import com.peoplemanager.domain.SearchResultType
import com.peoplemanager.domain.UserId

data class SearchQuery(
    val userId: UserId,
    val query: String,
    val types: List<SearchResultType>? = null,
    val page: Int = 0,
    val size: Int = 20
) {
    init {
        require(query.isNotBlank()) { "Search query must not be blank" }
        require(page >= 0) { "Page must be non-negative" }
        require(size in 1..100) { "Size must be between 1 and 100" }
    }
}
