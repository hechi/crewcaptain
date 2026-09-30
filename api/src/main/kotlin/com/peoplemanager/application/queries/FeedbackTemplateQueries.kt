package com.peoplemanager.application.queries

import com.peoplemanager.domain.FeedbackTemplateId
import com.peoplemanager.domain.UserId

data class GetFeedbackTemplateQuery(
    val userId: UserId,
    val templateId: FeedbackTemplateId
)

data class ListFeedbackTemplatesQuery(
    val userId: UserId
)
