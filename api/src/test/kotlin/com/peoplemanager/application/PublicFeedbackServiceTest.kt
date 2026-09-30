package com.peoplemanager.application

import com.peoplemanager.application.commands.SubmitFeedbackCommand
import com.peoplemanager.application.port.output.FeedbackLinkRepository
import com.peoplemanager.application.port.output.FeedbackResponseRepository
import com.peoplemanager.application.port.output.PersonRepository
import com.peoplemanager.application.queries.GetPublicFeedbackFormQuery
import com.peoplemanager.domain.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class PublicFeedbackServiceTest {

    private val linkRepository = mockk<FeedbackLinkRepository>()
    private val responseRepository = mockk<FeedbackResponseRepository>(relaxed = true)
    private val personRepository = mockk<PersonRepository>()
    private val auditLogService = mockk<AuditLogService>(relaxed = true)

    private val service = PublicFeedbackService(linkRepository, responseRepository, personRepository, auditLogService)

    private val userId = UserId.generate()
    private val personId = PersonId.generate()
    private val person = Person(id = personId, userId = userId, name = "Alex Doe", preferredName = "Alex")

    private val q1 = FeedbackQuestion("q1", FeedbackQuestionType.RATING, "Rate collaboration", required = true, lowLabel = "Low", highLabel = "High")
    private val q1b = FeedbackQuestion("q1b", FeedbackQuestionType.TEXT, "Explain low rating", required = true, showIf = ShowIfRule("q1", ShowIfOperator.LTE, 3))
    private val q2 = FeedbackQuestion("q2", FeedbackQuestionType.TEXT, "Optional comment", required = false)

    private fun link(
        revokedAt: Instant? = null,
        expiresAt: Instant = Instant.now().plus(14, ChronoUnit.DAYS)
    ) = FeedbackLink(
        id = FeedbackLinkId.generate(),
        userId = userId,
        personId = personId,
        token = "tok123",
        title = "Feedback for Alex",
        questions = listOf(q1, q1b, q2),
        expiresAt = expiresAt,
        revokedAt = revokedAt
    )

    @BeforeEach
    fun setup() = clearAllMocks()

    @Test
    fun `getForm returns preferred name and questions`() {
        every { linkRepository.findByToken("tok123") } returns link()
        every { personRepository.findByIdAndUserId(personId, userId) } returns person

        val form = service.getForm(GetPublicFeedbackFormQuery("tok123"))
        form.personName shouldBe "Alex"
        form.questions.size shouldBe 3
    }

    @Test
    fun `getForm throws not-found for unknown token`() {
        every { linkRepository.findByToken("nope") } returns null
        shouldThrow<FeedbackLinkTokenNotFoundException> {
            service.getForm(GetPublicFeedbackFormQuery("nope"))
        }
    }

    @Test
    fun `getForm throws not-found when person was soft-deleted`() {
        every { linkRepository.findByToken("tok123") } returns link()
        every { personRepository.findByIdAndUserId(personId, userId) } returns null // deleted -> not found
        shouldThrow<FeedbackLinkTokenNotFoundException> {
            service.getForm(GetPublicFeedbackFormQuery("tok123"))
        }
    }

    @Test
    fun `submit rejects expired link`() {
        every { linkRepository.findByToken("tok123") } returns link(expiresAt = Instant.now().minus(1, ChronoUnit.DAYS))
        shouldThrow<FeedbackLinkExpiredException> {
            service.submit(SubmitFeedbackCommand("tok123", true, null, null, listOf(FeedbackAnswer("q1", ratingValue = 4)), null))
        }
    }

    @Test
    fun `submit rejects revoked link`() {
        every { linkRepository.findByToken("tok123") } returns link(revokedAt = Instant.now())
        shouldThrow<FeedbackLinkRevokedException> {
            service.submit(SubmitFeedbackCommand("tok123", true, null, null, listOf(FeedbackAnswer("q1", ratingValue = 4)), null))
        }
    }

    @Test
    fun `submit stores anonymous response and drops name when anonymous`() {
        every { linkRepository.findByToken("tok123") } returns link()
        every { personRepository.findByIdAndUserId(personId, userId) } returns person
        every { responseRepository.save(any()) } answers { firstArg() }

        val saved = service.submit(
            SubmitFeedbackCommand("tok123", true, "Sam", "s@x.io", listOf(FeedbackAnswer("q1", ratingValue = 5)), "Great work")
        )
        saved.anonymous shouldBe true
        saved.submitterName shouldBe null
        saved.additionalComments shouldBe "Great work"
        saved.status shouldBe FeedbackResponseStatus.PENDING
    }

    @Test
    fun `submit keeps name when not anonymous and name provided`() {
        every { linkRepository.findByToken("tok123") } returns link()
        every { personRepository.findByIdAndUserId(personId, userId) } returns person
        every { responseRepository.save(any()) } answers { firstArg() }

        val saved = service.submit(
            SubmitFeedbackCommand("tok123", false, "Sam", "s@x.io", listOf(FeedbackAnswer("q1", ratingValue = 5)), null)
        )
        saved.anonymous shouldBe false
        saved.submitterName shouldBe "Sam"
    }

    @Test
    fun `submit enforces required question`() {
        every { linkRepository.findByToken("tok123") } returns link()
        every { personRepository.findByIdAndUserId(personId, userId) } returns person

        shouldThrow<FeedbackSubmissionInvalidException> {
            // q1 (required rating) not answered
            service.submit(SubmitFeedbackCommand("tok123", true, null, null, listOf(FeedbackAnswer("q2", textValue = "hi")), null))
        }
    }

    @Test
    fun `submit requires visible follow-up when branch condition met`() {
        every { linkRepository.findByToken("tok123") } returns link()
        every { personRepository.findByIdAndUserId(personId, userId) } returns person

        // q1 = 2 (<=3) makes q1b visible & required, but it's not answered
        shouldThrow<FeedbackSubmissionInvalidException> {
            service.submit(SubmitFeedbackCommand("tok123", true, null, null, listOf(FeedbackAnswer("q1", ratingValue = 2)), null))
        }
    }

    @Test
    fun `submit does not require hidden follow-up when branch condition not met`() {
        every { linkRepository.findByToken("tok123") } returns link()
        every { personRepository.findByIdAndUserId(personId, userId) } returns person
        every { responseRepository.save(any()) } answers { firstArg() }

        // q1 = 5 (>3) hides q1b, so submission with just q1 is valid
        val saved = service.submit(SubmitFeedbackCommand("tok123", true, null, null, listOf(FeedbackAnswer("q1", ratingValue = 5)), null))
        saved.ratingValues() shouldBe listOf(5)
    }

    @Test
    fun `submit ignores answers for unknown question ids`() {
        every { linkRepository.findByToken("tok123") } returns link()
        every { personRepository.findByIdAndUserId(personId, userId) } returns person
        every { responseRepository.save(any()) } answers { firstArg() }

        val saved = service.submit(
            SubmitFeedbackCommand(
                "tok123", true, null, null,
                listOf(FeedbackAnswer("q1", ratingValue = 5), FeedbackAnswer("ghost", textValue = "ignored")),
                null
            )
        )
        // Only the known q1 answer survives; the unknown "ghost" is dropped.
        saved.answers.map { it.questionId } shouldBe listOf("q1")
    }

    @Test
    fun `submit rejects over-long text`() {
        every { linkRepository.findByToken("tok123") } returns link()
        every { personRepository.findByIdAndUserId(personId, userId) } returns person
        val long = "x".repeat(PublicFeedbackService.MAX_TEXT_LENGTH + 1)
        shouldThrow<FeedbackSubmissionInvalidException> {
            service.submit(SubmitFeedbackCommand("tok123", true, null, null, listOf(FeedbackAnswer("q1", ratingValue = 4), FeedbackAnswer("q2", textValue = long)), null))
        }
    }
}
