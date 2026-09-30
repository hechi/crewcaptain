package com.peoplemanager.application.queries

import com.peoplemanager.domain.FeedbackResponseStatus
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.UserId

data class ListFeedbackLinksQuery(
    val userId: UserId,
    val personId: PersonId
)

data class ListFeedbackResponsesQuery(
    val userId: UserId,
    val personId: PersonId,
    val status: FeedbackResponseStatus? = null,
    val flagged: Boolean? = null
)

/** Public form fetch by token. */
data class GetPublicFeedbackFormQuery(
    val token: String
)
