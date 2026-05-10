package com.peoplemanager.adapters.persistence

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface SpringDataNotificationRepository : JpaRepository<NotificationEntity, UUID> {

    fun findByIdAndUserId(id: UUID, userId: UUID): NotificationEntity?

    fun findAllByUserIdOrderByCreatedAtDesc(userId: UUID, pageable: Pageable): Page<NotificationEntity>

    fun findAllByUserIdAndReadAtIsNullOrderByCreatedAtDesc(userId: UUID, pageable: Pageable): Page<NotificationEntity>

    fun countByUserIdAndReadAtIsNull(userId: UUID): Long

    @Modifying
    @Query("UPDATE NotificationEntity n SET n.readAt = :readAt WHERE n.userId = :userId AND n.readAt IS NULL")
    fun markAllAsReadByUserId(@Param("userId") userId: UUID, @Param("readAt") readAt: Instant): Int

    @Query(
        "SELECT CASE WHEN COUNT(n) > 0 THEN true ELSE false END FROM NotificationEntity n " +
        "WHERE n.userId = :userId AND n.type = :type AND n.referenceId = :referenceId AND n.createdAt > :createdAfter"
    )
    fun existsByUserIdAndTypeAndReferenceIdAndCreatedAfter(
        @Param("userId") userId: UUID,
        @Param("type") type: String,
        @Param("referenceId") referenceId: String,
        @Param("createdAfter") createdAfter: Instant
    ): Boolean
}
