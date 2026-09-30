package com.peoplemanager.adapters.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SpringDataFeedbackSummaryRepository : JpaRepository<FeedbackSummaryEntity, UUID> {
    fun findAllByUserIdAndPersonIdOrderByCreatedAtDesc(userId: UUID, personId: UUID): List<FeedbackSummaryEntity>
    fun findFirstByUserIdAndPersonIdOrderByCreatedAtDesc(userId: UUID, personId: UUID): FeedbackSummaryEntity?
}
