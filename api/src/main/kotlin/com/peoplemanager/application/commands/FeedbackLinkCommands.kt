package com.peoplemanager.application.commands

import com.peoplemanager.domain.FeedbackAnswer
import com.peoplemanager.domain.FeedbackConversionType
import com.peoplemanager.domain.FeedbackLinkId
import com.peoplemanager.domain.FeedbackQuestion
import com.peoplemanager.domain.FeedbackResponseId
import com.peoplemanager.domain.FeedbackTemplateId
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.UserId

/**
 * Create a link for one person. Either [templateId] (snapshot a saved template) or an
 * inline [title]/[questions] (an AI draft or starter that wasn't saved) must be provided.
 */
data class CreateFeedbackLinkCommand(
    val userId: UserId,
    val personId: PersonId,
    val templateId: FeedbackTemplateId? = null,
    val inlineTitle: String? = null,
    val inlineDescription: String? = null,
    val inlineQuestions: List<FeedbackQuestion>? = null,
    val expiresInDays: Long? = null,
    val label: String? = null,
    val requestSubmitterInfo: Boolean = true
)

data class BulkCreateFeedbackLinksCommand(
    val userId: UserId,
    val personIds: List<PersonId>,
    val templateId: FeedbackTemplateId,
    val expiresInDays: Long? = null,
    val label: String? = null,
    val requestSubmitterInfo: Boolean = true
)

data class RevokeFeedbackLinkCommand(
    val userId: UserId,
    val linkId: FeedbackLinkId
)

data class ExtendFeedbackLinkCommand(
    val userId: UserId,
    val linkId: FeedbackLinkId,
    val expiresInDays: Long
)

/** Public, unauthenticated submission against a token. */
data class SubmitFeedbackCommand(
    val token: String,
    val anonymous: Boolean,
    val submitterName: String?,
    val submitterEmail: String?,
    val answers: List<FeedbackAnswer>,
    val additionalComments: String?
)

// ===== Response management =====

data class UpdateFeedbackResponseCommand(
    val userId: UserId,
    val responseId: FeedbackResponseId,
    val approve: Boolean? = null,
    val flagged: Boolean? = null,
    val pinned: Boolean? = null
)

data class DeleteFeedbackResponseCommand(
    val userId: UserId,
    val responseId: FeedbackResponseId
)

enum class BulkResponseAction { APPROVE, DELETE }

data class BulkUpdateFeedbackResponsesCommand(
    val userId: UserId,
    val responseIds: List<FeedbackResponseId>,
    val action: BulkResponseAction
)

data class ConvertFeedbackResponseCommand(
    val userId: UserId,
    val responseId: FeedbackResponseId,
    val type: FeedbackConversionType,
    val text: String? = null
)
