package com.peoplemanager.application

import com.peoplemanager.application.commands.SubmitFeedbackCommand
import com.peoplemanager.application.port.input.PublicFeedbackFormView
import com.peoplemanager.application.port.input.PublicFeedbackPort
import com.peoplemanager.application.port.output.FeedbackLinkRepository
import com.peoplemanager.application.port.output.FeedbackResponseRepository
import com.peoplemanager.application.port.output.PersonRepository
import com.peoplemanager.application.queries.GetPublicFeedbackFormQuery
import com.peoplemanager.domain.AuditLogEntry
import com.peoplemanager.domain.FeedbackAnswer
import com.peoplemanager.domain.FeedbackLink
import com.peoplemanager.domain.FeedbackLinkStatus
import com.peoplemanager.domain.FeedbackQuestion
import com.peoplemanager.domain.FeedbackQuestionType
import com.peoplemanager.domain.FeedbackResponse
import com.peoplemanager.domain.FeedbackResponseId
import com.peoplemanager.domain.ShowIfOperator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Handles the public, unauthenticated feedback form: fetching the form definition and
 * accepting submissions. Never exposes owner data beyond the person's name.
 *
 * Resolution goes token -> link -> person (scoped by the link's owner userId), so a
 * soft-deleted person resolves to "not found" and the userId invariant is preserved.
 */
@Service
@Transactional
class PublicFeedbackService(
    private val feedbackLinkRepository: FeedbackLinkRepository,
    private val feedbackResponseRepository: FeedbackResponseRepository,
    private val personRepository: PersonRepository,
    private val auditLogService: AuditLogService
) : PublicFeedbackPort {

    companion object {
        const val MAX_TEXT_LENGTH = 5000
    }

    @Transactional(readOnly = true)
    override fun getForm(query: GetPublicFeedbackFormQuery): PublicFeedbackFormView {
        val link = resolveActiveLink(query.token)
        val person = personRepository.findByIdAndUserId(link.personId, link.userId)
            ?: throw FeedbackLinkTokenNotFoundException()

        return PublicFeedbackFormView(
            personName = person.preferredName?.takeIf { it.isNotBlank() } ?: person.name,
            title = link.title,
            description = link.description,
            requestSubmitterInfo = link.requestSubmitterInfo,
            questions = link.questions
        )
    }

    override fun submit(command: SubmitFeedbackCommand): FeedbackResponse {
        val link = resolveActiveLink(command.token)
        val person = personRepository.findByIdAndUserId(link.personId, link.userId)
            ?: throw FeedbackLinkTokenNotFoundException()

        val answers = validateAndNormalize(link.questions, command.answers)
        validateComments(command.additionalComments)

        val anonymous = command.anonymous || (command.submitterName.isNullOrBlank() && command.submitterEmail.isNullOrBlank())
        val response = FeedbackResponse(
            id = FeedbackResponseId.generate(),
            userId = link.userId,
            personId = link.personId,
            linkId = link.id,
            submitterName = if (anonymous) null else command.submitterName?.take(200),
            submitterEmail = if (anonymous) null else command.submitterEmail?.take(320),
            anonymous = anonymous,
            answers = answers,
            additionalComments = command.additionalComments?.trim()?.takeIf { it.isNotBlank() }
        )
        val saved = feedbackResponseRepository.save(response)
        auditLogService.record(
            AuditLogEntry.feedbackResponseSubmitted(link.userId, saved.id, link.personId, person.name)
        )
        return saved
    }

    private fun resolveActiveLink(token: String): FeedbackLink {
        val link = feedbackLinkRepository.findByToken(token)
            ?: throw FeedbackLinkTokenNotFoundException()
        return when (link.statusAt(Instant.now())) {
            FeedbackLinkStatus.ACTIVE -> link
            FeedbackLinkStatus.EXPIRED -> throw FeedbackLinkExpiredException()
            FeedbackLinkStatus.REVOKED -> throw FeedbackLinkRevokedException()
        }
    }

    /**
     * Validates the submission against the snapshotted questions:
     * - unknown question ids are dropped
     * - rating/likert answers must be 1..5
     * - text answers are length-capped
     * - required questions that are *visible* (their showIf is satisfied) must be answered
     */
    private fun validateAndNormalize(
        questions: List<FeedbackQuestion>,
        submitted: List<FeedbackAnswer>
    ): List<FeedbackAnswer> {
        val byId = questions.associateBy { it.id }
        val answerById = submitted.associateBy { it.questionId }

        // Keep only answers for known questions, normalized by type.
        val normalized = submitted.mapNotNull { ans ->
            val q = byId[ans.questionId] ?: return@mapNotNull null
            when (q.type) {
                FeedbackQuestionType.RATING, FeedbackQuestionType.LIKERT -> {
                    val v = ans.ratingValue ?: return@mapNotNull null
                    if (v !in 1..5) throw FeedbackSubmissionInvalidException("Rating for '${q.text}' must be 1-5")
                    FeedbackAnswer(q.id, ratingValue = v)
                }
                FeedbackQuestionType.TEXT -> {
                    val t = ans.textValue?.trim()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    if (t.length > MAX_TEXT_LENGTH) {
                        throw FeedbackSubmissionInvalidException("An answer exceeds the ${MAX_TEXT_LENGTH}-character limit")
                    }
                    FeedbackAnswer(q.id, textValue = t)
                }
            }
        }

        // Required-visible check.
        for (q in questions) {
            if (!q.required) continue
            if (!isVisible(q, answerById)) continue
            val hasAnswer = normalized.any { it.questionId == q.id }
            if (!hasAnswer) {
                throw FeedbackSubmissionInvalidException("Please answer: ${q.text}")
            }
        }

        return normalized
    }

    private fun isVisible(q: FeedbackQuestion, answerById: Map<String, FeedbackAnswer>): Boolean {
        val rule = q.showIf ?: return true
        val ref = answerById[rule.questionId]?.ratingValue ?: return false
        return when (rule.operator) {
            ShowIfOperator.LTE -> ref <= rule.value
            ShowIfOperator.GTE -> ref >= rule.value
            ShowIfOperator.EQ -> ref == rule.value
        }
    }

    private fun validateComments(comments: String?) {
        if (comments != null && comments.length > MAX_TEXT_LENGTH) {
            throw FeedbackSubmissionInvalidException("Comments exceed the ${MAX_TEXT_LENGTH}-character limit")
        }
    }
}
