package com.peoplemanager.adapters.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SpringDataFeedbackTemplateRepository : JpaRepository<FeedbackTemplateEntity, UUID> {
    fun findByIdAndUserId(id: UUID, userId: UUID): FeedbackTemplateEntity?
    fun findAllByUserIdOrderByUpdatedAtDesc(userId: UUID): List<FeedbackTemplateEntity>
    fun deleteByIdAndUserId(id: UUID, userId: UUID): Long
}
