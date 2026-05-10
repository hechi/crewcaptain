package com.peoplemanager.adapters.web.dto

import com.peoplemanager.domain.MoraleStatus
import com.peoplemanager.domain.Person
import com.peoplemanager.domain.PinnedRememberItem
import org.springframework.data.domain.Page
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class PersonResponse(
    val id: UUID,
    val name: String,
    val preferredName: String?,
    val roleTitle: String?,
    val timezone: String?,
    val startDate: LocalDate?,
    val email: String?,
    val tags: List<String>,
    val moraleStatus: MoraleStatus,
    val moraleNote: String?,
    val pinnedRememberItems: List<RememberItemResponse>,
    val atAGlance: AtAGlanceResponse,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        fun from(person: Person, last1on1Date: Instant? = null, openActionItemsCount: Int? = null, activePdpGoalsCount: Int? = null): PersonResponse = PersonResponse(
            id = person.id.value,
            name = person.name,
            preferredName = person.preferredName,
            roleTitle = person.roleTitle,
            timezone = person.timezone,
            startDate = person.startDate,
            email = person.email,
            tags = person.tags,
            moraleStatus = person.moraleStatus,
            moraleNote = person.moraleNote,
            pinnedRememberItems = person.pinnedRememberItems.map { RememberItemResponse.from(it) },
            atAGlance = AtAGlanceResponse(
                last1on1Date = last1on1Date,
                openActionItemsCount = openActionItemsCount,
                activePdpGoalsSummary = if (activePdpGoalsCount != null && activePdpGoalsCount > 0) {
                    "$activePdpGoalsCount active"
                } else {
                    null
                }
            ),
            createdAt = person.createdAt,
            updatedAt = person.updatedAt
        )
    }
}

data class AtAGlanceResponse(
    val last1on1Date: Instant? = null,
    val openActionItemsCount: Int? = null,
    val activePdpGoalsSummary: String? = null
)

data class RememberItemResponse(
    val id: UUID,
    val text: String,
    val displayOrder: Int,
    val createdAt: Instant
) {
    companion object {
        fun from(item: PinnedRememberItem): RememberItemResponse = RememberItemResponse(
            id = item.id.value,
            text = item.text,
            displayOrder = item.displayOrder,
            createdAt = item.createdAt
        )
    }
}

data class PaginatedPersonResponse(
    val content: List<PersonResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
) {
    companion object {
        fun from(pageResult: Page<Person>): PaginatedPersonResponse = PaginatedPersonResponse(
            content = pageResult.content.map { PersonResponse.from(it) },
            page = pageResult.number,
            size = pageResult.size,
            totalElements = pageResult.totalElements,
            totalPages = pageResult.totalPages
        )
    }
}

data class ErrorResponse(
    val status: Int,
    val error: String,
    val message: String,
    val timestamp: Instant
)
