package com.peoplemanager.application.port.input

import com.peoplemanager.application.queries.SearchQuery
import com.peoplemanager.domain.SearchResults

interface SearchQueryPort {
    fun search(query: SearchQuery): SearchResults
}
