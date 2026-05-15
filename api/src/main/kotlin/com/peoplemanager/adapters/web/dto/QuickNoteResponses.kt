package com.peoplemanager.adapters.web.dto

import com.peoplemanager.domain.QuickNote
import com.peoplemanager.domain.QuickNoteStatus
import org.springframework.data.domain.Page
import java.time.Instant
import java.util.UUID

data class QuickNoteResponse(
    val id: UUID,
    val personId: UUID?,
    val text: String,
    val sensitive: Boolean,
    val selfAssigned: Boolean,
    val status: QuickNoteStatus,
    val attachedEntryId: UUID?,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        fun from(quickNote: QuickNote): QuickNoteResponse = QuickNoteResponse(
            id = quickNote.id.value,
            personId = quickNote.personId?.value,
            text = quickNote.text,
            sensitive = quickNote.sensitive,
            selfAssigned = quickNote.selfAssigned,
            status = quickNote.status,
            attachedEntryId = quickNote.attachedEntryId?.value,
            createdAt = quickNote.createdAt,
            updatedAt = quickNote.updatedAt
        )
    }
}

data class PaginatedQuickNoteResponse(
    val content: List<QuickNoteResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
) {
    companion object {
        fun from(pageResult: Page<QuickNote>): PaginatedQuickNoteResponse =
            PaginatedQuickNoteResponse(
                content = pageResult.content.map { QuickNoteResponse.from(it) },
                page = pageResult.number,
                size = pageResult.size,
                totalElements = pageResult.totalElements,
                totalPages = pageResult.totalPages
            )
    }
}
