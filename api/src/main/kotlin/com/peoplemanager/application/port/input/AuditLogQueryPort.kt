package com.peoplemanager.application.port.input

import com.peoplemanager.application.queries.GetAuditLogQuery
import com.peoplemanager.domain.AuditLogEntry
import org.springframework.data.domain.Page

interface AuditLogQueryPort {
    fun getAuditLog(query: GetAuditLogQuery): Page<AuditLogEntry>
}
