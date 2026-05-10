package com.peoplemanager.adapters.web

import com.peoplemanager.adapters.auth.AuthenticatedUser
import com.peoplemanager.adapters.web.dto.SearchResponse
import com.peoplemanager.application.ports.SearchQueryPort
import com.peoplemanager.application.queries.SearchQuery
import com.peoplemanager.domain.SearchResultType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
class SearchController(
    private val searchQueryPort: SearchQueryPort
) {

    @GetMapping("/search")
    fun search(
        @RequestParam q: String,
        @RequestParam(required = false) type: List<SearchResultType>?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<SearchResponse> {
        if (q.isBlank()) {
            return ResponseEntity.badRequest().build()
        }

        val userId = AuthenticatedUser.getUserId()
        val query = SearchQuery(
            userId = userId,
            query = q,
            types = type?.takeIf { it.isNotEmpty() },
            page = page,
            size = size.coerceIn(1, 100)
        )
        val results = searchQueryPort.search(query)
        return ResponseEntity.ok(SearchResponse.from(results))
    }
}
