package com.peoplemanager.application.queries

import com.peoplemanager.domain.AuditAction
import com.peoplemanager.domain.AuditEntityType
import com.peoplemanager.domain.UserId
import org.springframework.data.domain.Pageable

data class GetAuditLogQuery(
    val userId: UserId,
    val entityType: AuditEntityType? = null,
    val action: AuditAction? = null,
    val pageable: Pageable
)
