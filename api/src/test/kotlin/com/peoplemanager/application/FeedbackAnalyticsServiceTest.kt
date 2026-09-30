package com.peoplemanager.application

import com.peoplemanager.application.commands.SaveFeedbackSummaryCommand
import com.peoplemanager.application.port.output.FeedbackResponseRepository
import com.peoplemanager.application.port.output.FeedbackSummaryRepository
import com.peoplemanager.application.port.output.PersonRepository
import com.peoplemanager.application.queries.GetFeedbackAnalyticsQuery
import com.peoplemanager.application.queries.ListFeedbackSummariesQuery
import com.peoplemanager.domain.*
import com.peoplemanager.domain.service.AnalyticsBucket
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class FeedbackAnalyticsServiceTest {

    private val personRepository = mockk<PersonRepository>()
    private val responseRepository = mockk<FeedbackResponseRepository>()
    private val summaryRepository = mockk<FeedbackSummaryRepository>(relaxed = true)
    private val auditLogService = mockk<AuditLogService>(relaxed = true)
    private val service = FeedbackAnalyticsService(personRepository, responseRepository, summaryRepository, auditLogService)

    private val userId = UserId.generate()
    private val personId = PersonId.generate()
    private val person = Person(id = personId, userId = userId, name = "Alex")

    private fun resp(date: LocalDate, rating: Int, approved: Boolean = true, flagged: Boolean = false, text: String? = null) =
        FeedbackResponse(
            id = FeedbackResponseId.generate(), userId = userId, personId = personId, linkId = FeedbackLinkId.generate(),
            answers = listOf(FeedbackAnswer("q1", ratingValue = rating)),
            additionalComments = text,
            status = if (approved) FeedbackResponseStatus.APPROVED else FeedbackResponseStatus.PENDING,
            flagged = flagged,
            createdAt = date.atStartOfDay(ZoneOffset.UTC).toInstant()
        )

    @BeforeEach
    fun setup() {
        clearAllMocks()
        every { personRepository.findByIdAndUserId(personId, userId) } returns person
    }

    @Test
    fun `analytics uses only approved non-flagged responses`() {
        every { responseRepository.findAllByUserIdAndPersonId(userId, personId) } returns listOf(
            resp(LocalDate.of(2026, 1, 5), 4, approved = true),
            resp(LocalDate.of(2026, 1, 6), 2, approved = false),   // pending -> excluded
            resp(LocalDate.of(2026, 1, 7), 1, approved = true, flagged = true) // flagged -> excluded
        )
        val a = service.getAnalytics(GetFeedbackAnalyticsQuery(userId, personId, bucket = AnalyticsBucket.MONTH))
        a.totalResponses shouldBe 1
        a.overallAverageRating!! shouldBe (4.0 plusOrMinus 0.001)
    }

    @Test
    fun `analytics respects date range`() {
        every { responseRepository.findAllByUserIdAndPersonId(userId, personId) } returns listOf(
            resp(LocalDate.of(2026, 1, 5), 4),
            resp(LocalDate.of(2026, 8, 5), 2)
        )
        val a = service.getAnalytics(
            GetFeedbackAnalyticsQuery(userId, personId, from = LocalDate.of(2026, 1, 1), to = LocalDate.of(2026, 6, 30), bucket = AnalyticsBucket.QUARTER)
        )
        a.totalResponses shouldBe 1
    }

    @Test
    fun `analytics throws for unowned person`() {
        every { personRepository.findByIdAndUserId(personId, userId) } returns null
        shouldThrow<PersonNotFoundException> {
            service.getAnalytics(GetFeedbackAnalyticsQuery(userId, personId))
        }
    }

    @Test
    fun `saveSummary persists and audits`() {
        every { summaryRepository.save(any()) } answers { firstArg() }
        val saved = service.saveSummary(
            SaveFeedbackSummaryCommand(userId, personId, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30), "Great collaborator", 5)
        )
        saved.content shouldBe "Great collaborator"
        verify { auditLogService.record(match { it.entityType == AuditEntityType.FEEDBACK_SUMMARY }) }
    }

    @Test
    fun `listSummaries delegates to repository`() {
        every { summaryRepository.findAllByUserIdAndPersonId(userId, personId) } returns emptyList()
        service.listSummaries(ListFeedbackSummariesQuery(userId, personId)) shouldBe emptyList()
    }
}
