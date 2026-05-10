package com.peoplemanager.adapters.persistence

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface SpringDataAuditLogRepository : JpaRepository<AuditLogEntryEntity, UUID> {

    fun findAllByUserIdOrderByCreatedAtDesc(userId: UUID, pageable: Pageable): Page<AuditLogEntryEntity>

    fun findAllByUserIdAndEntityTypeOrderByCreatedAtDesc(userId: UUID, entityType: String, pageable: Pageable): Page<AuditLogEntryEntity>

    fun findAllByUserIdAndActionOrderByCreatedAtDesc(userId: UUID, action: String, pageable: Pageable): Page<AuditLogEntryEntity>

    fun findAllByUserIdAndEntityTypeAndActionOrderByCreatedAtDesc(userId: UUID, entityType: String, action: String, pageable: Pageable): Page<AuditLogEntryEntity>
}
