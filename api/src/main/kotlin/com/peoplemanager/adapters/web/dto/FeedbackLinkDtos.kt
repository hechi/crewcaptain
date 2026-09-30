package com.peoplemanager.adapters.web.dto

import com.peoplemanager.application.port.input.FeedbackLinkWithUsage
import com.peoplemanager.application.port.input.PublicFeedbackFormView
import com.peoplemanager.domain.FeedbackLink
import com.peoplemanager.domain.FeedbackResponse
import jakarta.validation.constraints.NotEmpty
import java.time.Instant
import java.util.UUID

// ===== Link requests =====

data class CreateFeedbackLinkRequest(
    val templateId: UUID? = null,
    val title: String? = null,
    val description: String? = null,
    val questions: List<FeedbackQuestionDto>? = null,
    val expiresInDays: Long? = null,
    val label: String? = null,
    val requestSubmitterInfo: Boolean? = null
)

data class BulkCreateFeedbackLinksRequest(
    @field:NotEmpty(message = "Select at least one person")
    val personIds: List<UUID>?,
    val templateId: UUID?,
    val expiresInDays: Long? = null,
    val label: String? = null,
    val requestSubmitterInfo: Boolean? = null
)

data class ExtendFeedbackLinkRequest(
    val expiresInDays: Long?
)

// ===== Link responses =====

data class FeedbackLinkResponse(
    val id: UUID,
    val personId: UUID,
    val token: String,
    val title: String,
    val description: String?,
    val label: String?,
    val requestSubmitterInfo: Boolean,
    val status: String,
    val expiresAt: Instant,
    val revokedAt: Instant?,
    val submissionCount: Long,
    val lastSubmissionAt: Instant?,
    val createdAt: Instant
) {
    companion object {
        fun from(usage: FeedbackLinkWithUsage): FeedbackLinkResponse {
            val l = usage.link
            return FeedbackLinkResponse(
                id = l.id.value,
                personId = l.personId.value,
                token = l.token,
                title = l.title,
                description = l.description,
                label = l.label,
                requestSubmitterInfo = l.requestSubmitterInfo,
                status = l.statusAt(Instant.now()).name,
                expiresAt = l.expiresAt,
                revokedAt = l.revokedAt,
                submissionCount = usage.submissionCount,
                lastSubmissionAt = usage.lastSubmissionAt,
                createdAt = l.createdAt
            )
        }

        /** For freshly created links (no usage yet). */
        fun from(link: FeedbackLink): FeedbackLinkResponse = FeedbackLinkResponse(
            id = link.id.value,
            personId = link.personId.value,
            token = link.token,
            title = link.title,
            description = link.description,
            label = link.label,
            requestSubmitterInfo = link.requestSubmitterInfo,
            status = link.statusAt(Instant.now()).name,
            expiresAt = link.expiresAt,
            revokedAt = link.revokedAt,
            submissionCount = 0,
            lastSubmissionAt = null,
            createdAt = link.createdAt
        )
    }
}

data class BulkFeedbackLinkResultResponse(
    val links: List<BulkFeedbackLinkItem>
)

data class BulkFeedbackLinkItem(
    val personId: UUID,
    val personName: String,
    val token: String,
    val linkId: UUID
)

// ===== Response management DTOs =====

data class FeedbackAnswerDto(
    val questionId: String,
    val ratingValue: Int? = null,
    val textValue: String? = null
)

data class FeedbackResponseItemResponse(
    val id: UUID,
    val personId: UUID,
    val linkId: UUID,
    val submitterName: String?,
    val submitterEmail: String?,
    val anonymous: Boolean,
    val answers: List<FeedbackAnswerDto>,
    val additionalComments: String?,
    val status: String,
    val flagged: Boolean,
    val pinned: Boolean,
    val convertedToType: String?,
    val convertedToId: String?,
    val createdAt: Instant
) {
    companion object {
        fun from(r: FeedbackResponse): FeedbackResponseItemResponse = FeedbackResponseItemResponse(
            id = r.id.value,
            personId = r.personId.value,
            linkId = r.linkId.value,
            submitterName = r.submitterName,
            submitterEmail = r.submitterEmail,
            anonymous = r.anonymous,
            answers = r.answers.map { FeedbackAnswerDto(it.questionId, it.ratingValue, it.textValue) },
            additionalComments = r.additionalComments,
            status = r.status.name,
            flagged = r.flagged,
            pinned = r.pinned,
            convertedToType = r.convertedToType?.name,
            convertedToId = r.convertedToId,
            createdAt = r.createdAt
        )
    }
}

data class UpdateFeedbackResponseRequest(
    val approve: Boolean? = null,
    val flagged: Boolean? = null,
    val pinned: Boolean? = null
)

data class BulkUpdateFeedbackResponsesRequest(
    @field:NotEmpty(message = "Select at least one response")
    val responseIds: List<UUID>?,
    val action: String?
)

data class ConvertFeedbackResponseRequest(
    val type: String?,
    val text: String? = null
)

// ===== Public form DTOs =====

data class PublicFeedbackFormResponse(
    val personName: String,
    val title: String,
    val description: String?,
    val requestSubmitterInfo: Boolean,
    val questions: List<FeedbackQuestionDto>
) {
    companion object {
        fun from(view: PublicFeedbackFormView): PublicFeedbackFormResponse = PublicFeedbackFormResponse(
            personName = view.personName,
            title = view.title,
            description = view.description,
            requestSubmitterInfo = view.requestSubmitterInfo,
            questions = view.questions.map { FeedbackQuestionDto.from(it) }
        )
    }
}

data class SubmitPublicFeedbackRequest(
    val anonymous: Boolean = true,
    val submitterName: String? = null,
    val submitterEmail: String? = null,
    val answers: List<FeedbackAnswerDto> = emptyList(),
    val additionalComments: String? = null,
    /** Honeypot — must be empty. */
    val website: String? = null
)
