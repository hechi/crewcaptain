package com.peoplemanager.adapters.web

import com.peoplemanager.adapters.auth.AuthenticatedUser
import com.peoplemanager.adapters.web.dto.PaginatedAuditLogResponse
import com.peoplemanager.application.port.input.AuditLogQueryPort
import com.peoplemanager.application.queries.GetAuditLogQuery
import com.peoplemanager.domain.AuditAction
import com.peoplemanager.domain.AuditEntityType
import org.springframework.data.domain.PageRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/audit-log")
class AuditLogController(
    private val auditLogQueryPort: AuditLogQueryPort
) {

    @GetMapping
    fun getAuditLog(
        @RequestParam(required = false) entityType: AuditEntityType?,
        @RequestParam(required = false) action: AuditAction?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PaginatedAuditLogResponse> {
        val userId = AuthenticatedUser.getUserId()
        val query = GetAuditLogQuery(
            userId = userId,
            entityType = entityType,
            action = action,
            pageable = PageRequest.of(page, size)
        )
        val result = auditLogQueryPort.getAuditLog(query)
        return ResponseEntity.ok(PaginatedAuditLogResponse.from(result))
    }
}
