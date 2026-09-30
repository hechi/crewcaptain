package com.peoplemanager.adapters.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface SpringDataFeedbackResponseRepository : JpaRepository<FeedbackResponseEntity, UUID> {
    fun findByIdAndUserId(id: UUID, userId: UUID): FeedbackResponseEntity?
    fun findAllByUserIdAndPersonIdOrderByCreatedAtDesc(userId: UUID, personId: UUID): List<FeedbackResponseEntity>
    fun findAllByUserIdAndPersonIdAndStatusOrderByCreatedAtDesc(userId: UUID, personId: UUID, status: String): List<FeedbackResponseEntity>
    fun deleteByIdAndUserId(id: UUID, userId: UUID): Long
    fun countByLinkId(linkId: UUID): Long

    @Query("SELECT MAX(r.createdAt) FROM FeedbackResponseEntity r WHERE r.linkId = :linkId")
    fun findLatestCreatedAtByLinkId(@Param("linkId") linkId: UUID): Instant?
}
