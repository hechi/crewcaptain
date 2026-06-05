package com.peoplemanager.application

import com.peoplemanager.application.port.input.AuditLogQueryPort
import com.peoplemanager.application.port.output.AuditLogRepository
import com.peoplemanager.application.queries.GetAuditLogQuery
import com.peoplemanager.domain.AuditLogEntry
import org.springframework.data.domain.Page
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class AuditLogService(
    private val auditLogRepository: AuditLogRepository
) : AuditLogQueryPort {

    fun record(entry: AuditLogEntry): AuditLogEntry {
        return auditLogRepository.save(entry)
    }

    @Transactional(readOnly = true)
    override fun getAuditLog(query: GetAuditLogQuery): Page<AuditLogEntry> {
        return auditLogRepository.findAllByUserId(
            userId = query.userId,
            entityType = query.entityType,
            action = query.action,
            pageable = query.pageable
        )
    }
}
