package com.peoplemanager.adapters.web.dto

import com.peoplemanager.domain.AuditAction
import com.peoplemanager.domain.AuditEntityType
import com.peoplemanager.domain.AuditLogEntry
import org.springframework.data.domain.Page
import java.time.Instant

data class AuditLogEntryResponse(
    val id: String,
    val action: AuditAction,
    val entityType: AuditEntityType,
    val entityId: String,
    val personId: String?,
    val summary: String,
    val createdAt: Instant
) {
    companion object {
        fun from(entry: AuditLogEntry): AuditLogEntryResponse = AuditLogEntryResponse(
            id = entry.id.value.toString(),
            action = entry.action,
            entityType = entry.entityType,
            entityId = entry.entityId,
            personId = entry.personId?.value?.toString(),
            summary = entry.summary,
            createdAt = entry.createdAt
        )
    }
}

data class PaginatedAuditLogResponse(
    val content: List<AuditLogEntryResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
) {
    companion object {
        fun from(page: Page<AuditLogEntry>): PaginatedAuditLogResponse = PaginatedAuditLogResponse(
            content = page.content.map { AuditLogEntryResponse.from(it) },
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages
        )
    }
}
