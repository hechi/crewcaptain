package com.peoplemanager.adapters.persistence

import com.peoplemanager.application.ports.AuditLogRepository
import com.peoplemanager.domain.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class JpaAuditLogRepositoryAdapter(
    private val springDataAuditLogRepository: SpringDataAuditLogRepository
) : AuditLogRepository {

    override fun save(entry: AuditLogEntry): AuditLogEntry {
        val entity = entry.toEntity()
        return springDataAuditLogRepository.save(entity).toDomain()
    }

    override fun findAllByUserId(
        userId: UserId,
        entityType: AuditEntityType?,
        action: AuditAction?,
        pageable: Pageable
    ): Page<AuditLogEntry> {
        val page = when {
            entityType != null && action != null ->
                springDataAuditLogRepository.findAllByUserIdAndEntityTypeAndActionOrderByCreatedAtDesc(
                    userId.value, entityType.name, action.name, pageable
                )
            entityType != null ->
                springDataAuditLogRepository.findAllByUserIdAndEntityTypeOrderByCreatedAtDesc(
                    userId.value, entityType.name, pageable
                )
            action != null ->
                springDataAuditLogRepository.findAllByUserIdAndActionOrderByCreatedAtDesc(
                    userId.value, action.name, pageable
                )
            else ->
                springDataAuditLogRepository.findAllByUserIdOrderByCreatedAtDesc(
                    userId.value, pageable
                )
        }
        return page.map { it.toDomain() }
    }

    private fun AuditLogEntry.toEntity(): AuditLogEntryEntity = AuditLogEntryEntity(
        id = this.id.value,
        userId = this.userId.value,
        action = this.action.name,
        entityType = this.entityType.name,
        entityId = this.entityId,
        personId = this.personId?.value,
        summary = this.summary,
        createdAt = this.createdAt
    )

    private fun AuditLogEntryEntity.toDomain(): AuditLogEntry = AuditLogEntry(
        id = AuditLogEntryId(this.id),
        userId = UserId(this.userId),
        action = AuditAction.valueOf(this.action),
        entityType = AuditEntityType.valueOf(this.entityType),
        entityId = this.entityId,
        personId = this.personId?.let { PersonId(it) },
        summary = this.summary,
        createdAt = this.createdAt
    )
}
