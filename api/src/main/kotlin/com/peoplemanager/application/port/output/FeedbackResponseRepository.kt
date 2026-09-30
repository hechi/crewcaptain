package com.peoplemanager.application.port.output

import com.peoplemanager.domain.FeedbackLinkId
import com.peoplemanager.domain.FeedbackResponse
import com.peoplemanager.domain.FeedbackResponseId
import com.peoplemanager.domain.FeedbackResponseStatus
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.UserId

interface FeedbackResponseRepository {
    fun save(response: FeedbackResponse): FeedbackResponse
    fun findByIdAndUserId(id: FeedbackResponseId, userId: UserId): FeedbackResponse?
    fun findAllByUserIdAndPersonId(userId: UserId, personId: PersonId): List<FeedbackResponse>
    fun findAllByUserIdAndPersonIdAndStatus(userId: UserId, personId: PersonId, status: FeedbackResponseStatus): List<FeedbackResponse>
    fun deleteByIdAndUserId(id: FeedbackResponseId, userId: UserId): Boolean
    fun countByLinkId(linkId: FeedbackLinkId): Long
    fun findLatestCreatedAtByLinkId(linkId: FeedbackLinkId): java.time.Instant?
}
