package com.peoplemanager.application

import com.peoplemanager.application.port.output.FeedbackResponseRepository
import com.peoplemanager.application.port.output.PdfRendererPort
import com.peoplemanager.application.port.output.PersonRepository
import com.peoplemanager.domain.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FeedbackExportServiceTest {

    private val personRepository = mockk<PersonRepository>()
    private val responseRepository = mockk<FeedbackResponseRepository>()
    private val pdfRenderer = mockk<PdfRendererPort>()
    private val service = FeedbackExportService(personRepository, responseRepository, pdfRenderer)

    private val userId = UserId.generate()
    private val personId = PersonId.generate()
    private val person = Person(id = personId, userId = userId, name = "Alex")

    private fun resp(id: FeedbackResponseId, approved: Boolean, flagged: Boolean = false) = FeedbackResponse(
        id = id, userId = userId, personId = personId, linkId = FeedbackLinkId.generate(),
        answers = listOf(FeedbackAnswer("q1", ratingValue = 5)),
        status = if (approved) FeedbackResponseStatus.APPROVED else FeedbackResponseStatus.PENDING,
        flagged = flagged
    )

    @BeforeEach
    fun setup() {
        every { personRepository.findByIdAndUserId(personId, userId) } returns person
    }

    @Test
    fun `markdown export defaults to usable responses only`() {
        val approved = resp(FeedbackResponseId.generate(), approved = true)
        val pending = resp(FeedbackResponseId.generate(), approved = false)
        val flagged = resp(FeedbackResponseId.generate(), approved = true, flagged = true)
        every { responseRepository.findAllByUserIdAndPersonId(userId, personId) } returns listOf(approved, pending, flagged)

        val md = service.exportMarkdown(userId, personId, null)
        md shouldContain "**Responses included:** 1"
    }

    @Test
    fun `explicit id list overrides the usable-only default`() {
        val a = resp(FeedbackResponseId.generate(), approved = false)
        every { responseRepository.findAllByUserIdAndPersonId(userId, personId) } returns listOf(a)

        val md = service.exportMarkdown(userId, personId, listOf(a.id))
        md shouldContain "**Responses included:** 1"
    }

    @Test
    fun `pdf export delegates to the renderer with generated markdown`() {
        val a = resp(FeedbackResponseId.generate(), approved = true)
        every { responseRepository.findAllByUserIdAndPersonId(userId, personId) } returns listOf(a)
        val mdSlot = slot<String>()
        every { pdfRenderer.renderMarkdownToPdf(capture(mdSlot), any()) } returns byteArrayOf(1, 2, 3)

        val bytes = service.exportPdf(userId, personId, null)
        bytes.size shouldBe 3
        mdSlot.captured shouldContain "Peer Feedback: Alex"
    }

    @Test
    fun `throws when person not owned`() {
        every { personRepository.findByIdAndUserId(personId, userId) } returns null
        shouldThrow<PersonNotFoundException> { service.exportMarkdown(userId, personId, null) }
    }
}
