package com.peoplemanager.adapters.web.dto

import com.peoplemanager.domain.ActionItem
import com.peoplemanager.domain.ActionItemOwnerType
import com.peoplemanager.domain.ActionItemStatus
import org.springframework.data.domain.Page
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class ActionItemResponse(
    val id: UUID,
    val personId: UUID,
    val title: String,
    val description: String?,
    val ownerType: ActionItemOwnerType,
    val dueDate: LocalDate?,
    val status: ActionItemStatus,
    val originatingEntryId: UUID?,
    val snoozedUntil: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        fun from(item: ActionItem): ActionItemResponse = ActionItemResponse(
            id = item.id.value,
            personId = item.personId.value,
            title = item.title,
            description = item.description,
            ownerType = item.ownerType,
            dueDate = item.dueDate,
            status = item.status,
            originatingEntryId = item.originatingEntryId?.value,
            snoozedUntil = item.snoozedUntil,
            createdAt = item.createdAt,
            updatedAt = item.updatedAt
        )
    }
}

data class PaginatedActionItemResponse(
    val content: List<ActionItemResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
) {
    companion object {
        fun from(pageResult: Page<ActionItem>): PaginatedActionItemResponse =
            PaginatedActionItemResponse(
                content = pageResult.content.map { ActionItemResponse.from(it) },
                page = pageResult.number,
                size = pageResult.size,
                totalElements = pageResult.totalElements,
                totalPages = pageResult.totalPages
            )
    }
}
