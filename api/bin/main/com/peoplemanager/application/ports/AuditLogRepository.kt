package com.peoplemanager.application.ports

import com.peoplemanager.domain.AuditAction
import com.peoplemanager.domain.AuditEntityType
import com.peoplemanager.domain.AuditLogEntry
import com.peoplemanager.domain.UserId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface AuditLogRepository {
    fun save(entry: AuditLogEntry): AuditLogEntry
    fun findAllByUserId(
        userId: UserId,
        entityType: AuditEntityType? = null,
        action: AuditAction? = null,
        pageable: Pageable
    ): Page<AuditLogEntry>
}
