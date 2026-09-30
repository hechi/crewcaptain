package com.peoplemanager.adapters.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SpringDataFeedbackLinkRepository : JpaRepository<FeedbackLinkEntity, UUID> {
    fun findByIdAndUserId(id: UUID, userId: UUID): FeedbackLinkEntity?
    fun findAllByUserIdAndPersonIdOrderByCreatedAtDesc(userId: UUID, personId: UUID): List<FeedbackLinkEntity>
    fun findByToken(token: String): FeedbackLinkEntity?
    fun existsByToken(token: String): Boolean
}
