package com.peoplemanager.application.port.output

import com.peoplemanager.domain.FeedbackSummary
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.UserId

interface FeedbackSummaryRepository {
    fun save(summary: FeedbackSummary): FeedbackSummary
    fun findAllByUserIdAndPersonId(userId: UserId, personId: PersonId): List<FeedbackSummary>
    fun findLatestByUserIdAndPersonId(userId: UserId, personId: PersonId): FeedbackSummary?
}
