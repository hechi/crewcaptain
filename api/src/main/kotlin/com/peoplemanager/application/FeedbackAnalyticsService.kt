package com.peoplemanager.application

import com.peoplemanager.application.commands.SaveFeedbackSummaryCommand
import com.peoplemanager.application.port.output.FeedbackResponseRepository
import com.peoplemanager.application.port.output.FeedbackSummaryRepository
import com.peoplemanager.application.port.output.PersonRepository
import com.peoplemanager.application.queries.GetFeedbackAnalyticsQuery
import com.peoplemanager.application.queries.ListFeedbackSummariesQuery
import com.peoplemanager.domain.AuditLogEntry
import com.peoplemanager.domain.FeedbackResponse
import com.peoplemanager.domain.FeedbackSummary
import com.peoplemanager.domain.FeedbackSummaryId
import com.peoplemanager.domain.service.FeedbackAnalytics
import com.peoplemanager.domain.service.FeedbackAnalyticsCalculator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Computes feedback analytics (deterministic, AI-free) and manages saved AI summaries.
 * Analytics are built from approved, non-flagged responses; they work regardless of
 * whether an AI provider is configured.
 */
@Service
@Transactional
class FeedbackAnalyticsService(
    private val personRepository: PersonRepository,
    private val feedbackResponseRepository: FeedbackResponseRepository,
    private val feedbackSummaryRepository: FeedbackSummaryRepository,
    private val auditLogService: AuditLogService
) {

    @Transactional(readOnly = true)
    fun getAnalytics(query: GetFeedbackAnalyticsQuery): FeedbackAnalytics {
        personRepository.findByIdAndUserId(query.personId, query.userId)
            ?: throw PersonNotFoundException(query.personId)

        val responses = usableResponses(query.userId, query.personId, query.from, query.to)
        return FeedbackAnalyticsCalculator.compute(responses, query.bucket)
    }

    @Transactional(readOnly = true)
    fun listSummaries(query: ListFeedbackSummariesQuery): List<FeedbackSummary> {
        personRepository.findByIdAndUserId(query.personId, query.userId)
            ?: throw PersonNotFoundException(query.personId)
        return feedbackSummaryRepository.findAllByUserIdAndPersonId(query.userId, query.personId)
    }

    fun saveSummary(command: SaveFeedbackSummaryCommand): FeedbackSummary {
        val person = personRepository.findByIdAndUserId(command.personId, command.userId)
            ?: throw PersonNotFoundException(command.personId)

        val summary = FeedbackSummary(
            id = FeedbackSummaryId.generate(),
            userId = command.userId,
            personId = command.personId,
            periodFrom = command.periodFrom,
            periodTo = command.periodTo,
            content = command.content,
            responseCount = command.responseCount
        )
        val saved = feedbackSummaryRepository.save(summary)
        auditLogService.record(
            AuditLogEntry.feedbackSummarySaved(command.userId, saved.id, command.personId, person.name)
        )
        return saved
    }

    internal fun usableResponses(
        userId: com.peoplemanager.domain.UserId,
        personId: com.peoplemanager.domain.PersonId,
        from: LocalDate?,
        to: LocalDate?
    ): List<FeedbackResponse> {
        return feedbackResponseRepository.findAllByUserIdAndPersonId(userId, personId)
            .filter { it.isUsable }
            .filter { r ->
                val d = r.createdAt.atZone(ZoneOffset.UTC).toLocalDate()
                (from == null || !d.isBefore(from)) && (to == null || !d.isAfter(to))
            }
    }
}
