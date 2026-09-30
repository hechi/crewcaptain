package com.peoplemanager.application.port.input

import com.peoplemanager.application.commands.*
import com.peoplemanager.application.queries.GetPublicFeedbackFormQuery
import com.peoplemanager.application.queries.ListFeedbackLinksQuery
import com.peoplemanager.application.queries.ListFeedbackResponsesQuery
import com.peoplemanager.domain.FeedbackLink
import com.peoplemanager.domain.FeedbackResponse

/** Manager-facing link management. */
interface FeedbackLinkCommandPort {
    fun createLink(command: CreateFeedbackLinkCommand): FeedbackLink
    fun bulkCreateLinks(command: BulkCreateFeedbackLinksCommand): List<Pair<FeedbackLink, String>>
    fun revokeLink(command: RevokeFeedbackLinkCommand): FeedbackLink
    fun extendLink(command: ExtendFeedbackLinkCommand): FeedbackLink
}

interface FeedbackLinkQueryPort {
    fun listLinks(query: ListFeedbackLinksQuery): List<FeedbackLinkWithUsage>
}

/** A link plus derived usage stats for the manager view. */
data class FeedbackLinkWithUsage(
    val link: FeedbackLink,
    val submissionCount: Long,
    val lastSubmissionAt: java.time.Instant?
)

/** Public, unauthenticated form fetch + submission. */
interface PublicFeedbackPort {
    fun getForm(query: GetPublicFeedbackFormQuery): PublicFeedbackFormView
    fun submit(command: SubmitFeedbackCommand): FeedbackResponse
}

/** Only the minimal data safe to expose publicly. */
data class PublicFeedbackFormView(
    val personName: String,
    val title: String,
    val description: String?,
    val requestSubmitterInfo: Boolean,
    val questions: List<com.peoplemanager.domain.FeedbackQuestion>
)

/** Manager-facing response management. */
interface FeedbackResponseCommandPort {
    fun updateResponse(command: UpdateFeedbackResponseCommand): FeedbackResponse
    fun deleteResponse(command: DeleteFeedbackResponseCommand)
    fun bulkUpdate(command: BulkUpdateFeedbackResponsesCommand)
    fun convertResponse(command: ConvertFeedbackResponseCommand): FeedbackResponse
}

interface FeedbackResponseQueryPort {
    fun listResponses(query: ListFeedbackResponsesQuery): List<FeedbackResponse>
}
