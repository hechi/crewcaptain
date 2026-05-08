package com.peoplemanager.adapters.web.dto

import com.peoplemanager.domain.AgendaItem
import com.peoplemanager.domain.CadenceType
import com.peoplemanager.domain.OneOnOneEntry
import com.peoplemanager.domain.OneOnOneSeries
import org.springframework.data.domain.Page
import java.time.Instant
import java.util.UUID

data class OneOnOneSeriesResponse(
    val id: UUID,
    val personId: UUID,
    val cadenceType: CadenceType,
    val customIntervalDays: Int?,
    val templateMarkdown: String?,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        fun from(series: OneOnOneSeries): OneOnOneSeriesResponse = OneOnOneSeriesResponse(
            id = series.id.value,
            personId = series.personId.value,
            cadenceType = series.cadenceType,
            customIntervalDays = series.customIntervalDays,
            templateMarkdown = series.templateMarkdown,
            createdAt = series.createdAt,
            updatedAt = series.updatedAt
        )
    }
}

data class OneOnOneEntryResponse(
    val id: UUID,
    val personId: UUID,
    val meetingDate: Instant,
    val agendaItems: List<AgendaItemResponse>,
    val notesMarkdown: String?,
    val outcomesMarkdown: String?,
    val sensitive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        fun from(entry: OneOnOneEntry): OneOnOneEntryResponse = OneOnOneEntryResponse(
            id = entry.id.value,
            personId = entry.personId.value,
            meetingDate = entry.meetingDate,
            agendaItems = entry.agendaItems.map { AgendaItemResponse.from(it) },
            notesMarkdown = entry.notesMarkdown,
            outcomesMarkdown = entry.outcomesMarkdown,
            sensitive = entry.sensitive,
            createdAt = entry.createdAt,
            updatedAt = entry.updatedAt
        )
    }
}

data class AgendaItemResponse(
    val id: UUID,
    val text: String,
    val checked: Boolean,
    val displayOrder: Int,
    val createdAt: Instant
) {
    companion object {
        fun from(item: AgendaItem): AgendaItemResponse = AgendaItemResponse(
            id = item.id.value,
            text = item.text,
            checked = item.checked,
            displayOrder = item.displayOrder,
            createdAt = item.createdAt
        )
    }
}

data class PaginatedOneOnOneEntryResponse(
    val content: List<OneOnOneEntryResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
) {
    companion object {
        fun from(pageResult: Page<OneOnOneEntry>): PaginatedOneOnOneEntryResponse =
            PaginatedOneOnOneEntryResponse(
                content = pageResult.content.map { OneOnOneEntryResponse.from(it) },
                page = pageResult.number,
                size = pageResult.size,
                totalElements = pageResult.totalElements,
                totalPages = pageResult.totalPages
            )
    }
}
