package com.peoplemanager.application.commands

import com.peoplemanager.domain.FeedbackQuestion
import com.peoplemanager.domain.FeedbackTemplateId
import com.peoplemanager.domain.UserId

data class CreateFeedbackTemplateCommand(
    val userId: UserId,
    val title: String,
    val description: String?,
    val questions: List<FeedbackQuestion>
)

data class UpdateFeedbackTemplateCommand(
    val userId: UserId,
    val templateId: FeedbackTemplateId,
    val title: String,
    val description: String?,
    val questions: List<FeedbackQuestion>
)

data class DeleteFeedbackTemplateCommand(
    val userId: UserId,
    val templateId: FeedbackTemplateId
)

/** Ask AI to draft a template from a short brief. Result is NOT persisted. */
data class GenerateFeedbackTemplateCommand(
    val userId: UserId,
    val brief: String
)
