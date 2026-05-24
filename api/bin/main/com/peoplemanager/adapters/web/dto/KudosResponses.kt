package com.peoplemanager.adapters.web.dto

import com.peoplemanager.domain.Kudos
import org.springframework.data.domain.Page
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class KudosResponse(
    val id: UUID,
    val personId: UUID,
    val date: LocalDate,
    val text: String,
    val tags: List<String>,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        fun from(kudos: Kudos): KudosResponse = KudosResponse(
            id = kudos.id.value,
            personId = kudos.personId.value,
            date = kudos.date,
            text = kudos.text,
            tags = kudos.tags,
            createdAt = kudos.createdAt,
            updatedAt = kudos.updatedAt
        )
    }
}

data class PaginatedKudosResponse(
    val content: List<KudosResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
) {
    companion object {
        fun from(pageResult: Page<Kudos>): PaginatedKudosResponse =
            PaginatedKudosResponse(
                content = pageResult.content.map { KudosResponse.from(it) },
                page = pageResult.number,
                size = pageResult.size,
                totalElements = pageResult.totalElements,
                totalPages = pageResult.totalPages
            )
    }
}
