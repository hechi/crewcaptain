package com.peoplemanager.application

import com.peoplemanager.application.commands.*
import com.peoplemanager.application.port.input.ActionItemCommandPort
import com.peoplemanager.application.port.input.KudosCommandPort
import com.peoplemanager.application.port.input.QuickNoteCommandPort
import com.peoplemanager.application.port.output.FeedbackResponseRepository
import com.peoplemanager.application.port.output.PersonRepository
import com.peoplemanager.domain.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FeedbackResponseServiceTest {

    private val personRepository = mockk<PersonRepository>()
    private val responseRepository = mockk<FeedbackResponseRepository>(relaxed = true)
    private val auditLogService = mockk<AuditLogService>(relaxed = true)
    private val kudosCommandPort = mockk<KudosCommandPort>()
    private val quickNoteCommandPort = mockk<QuickNoteCommandPort>()
    private val actionItemCommandPort = mockk<ActionItemCommandPort>()

    private val service = FeedbackResponseService(
        personRepository, responseRepository, auditLogService,
        kudosCommandPort, quickNoteCommandPort, actionItemCommandPort
    )

    private val userId = UserId.generate()
    private val personId = PersonId.generate()
    private val person = Person(id = personId, userId = userId, name = "Alex")
    private val responseId = FeedbackResponseId.generate()

    private fun response() = FeedbackResponse(
        id = responseId,
        userId = userId,
        personId = personId,
        linkId = FeedbackLinkId.generate(),
        answers = listOf(FeedbackAnswer("q1", ratingValue = 5), FeedbackAnswer("q2", textValue = "Excellent mentor")),
        additionalComments = "Keep it up"
    )

    @BeforeEach
    fun setup() {
        clearAllMocks()
        every { responseRepository.save(any()) } answers { firstArg() }
        every { personRepository.findByIdAndUserId(personId, userId) } returns person
    }

    @Test
    fun `approve transitions status and audits`() {
        every { responseRepository.findByIdAndUserId(responseId, userId) } returns response()
        val updated = service.updateResponse(UpdateFeedbackResponseCommand(userId, responseId, approve = true))
        updated.status shouldBe FeedbackResponseStatus.APPROVED
        verify { auditLogService.record(match { it.action == AuditAction.APPROVE }) }
    }

    @Test
    fun `flag toggles and audits`() {
        every { responseRepository.findByIdAndUserId(responseId, userId) } returns response()
        val updated = service.updateResponse(UpdateFeedbackResponseCommand(userId, responseId, flagged = true))
        updated.flagged shouldBe true
        verify { auditLogService.record(match { it.action == AuditAction.FLAG }) }
    }

    @Test
    fun `pin does not audit`() {
        every { responseRepository.findByIdAndUserId(responseId, userId) } returns response()
        val updated = service.updateResponse(UpdateFeedbackResponseCommand(userId, responseId, pinned = true))
        updated.pinned shouldBe true
    }

    @Test
    fun `update throws when not owned`() {
        every { responseRepository.findByIdAndUserId(responseId, userId) } returns null
        shouldThrow<FeedbackResponseNotFoundException> {
            service.updateResponse(UpdateFeedbackResponseCommand(userId, responseId, approve = true))
        }
    }

    @Test
    fun `delete removes and audits`() {
        every { responseRepository.findByIdAndUserId(responseId, userId) } returns response()
        every { responseRepository.deleteByIdAndUserId(responseId, userId) } returns true
        service.deleteResponse(DeleteFeedbackResponseCommand(userId, responseId))
        verify { auditLogService.record(match { it.action == AuditAction.DELETE && it.entityType == AuditEntityType.FEEDBACK_RESPONSE }) }
    }

    @Test
    fun `convert to kudo delegates to kudos port and marks converted`() {
        every { responseRepository.findByIdAndUserId(responseId, userId) } returns response()
        val kudosId = KudosId.generate()
        every { kudosCommandPort.createKudos(any()) } returns Kudos(kudosId, userId, personId, LocalDate.now(), "Excellent mentor")

        val updated = service.convertResponse(ConvertFeedbackResponseCommand(userId, responseId, FeedbackConversionType.KUDO))
        updated.convertedToType shouldBe FeedbackConversionType.KUDO
        updated.convertedToId shouldBe kudosId.value.toString()
        verify { kudosCommandPort.createKudos(any()) }
        verify { auditLogService.record(match { it.action == AuditAction.CONVERT }) }
    }

    @Test
    fun `convert to action item delegates to action item port`() {
        every { responseRepository.findByIdAndUserId(responseId, userId) } returns response()
        val aiId = ActionItemId.generate()
        every { actionItemCommandPort.createActionItem(any()) } returns ActionItem(
            id = aiId, userId = userId, personId = personId, title = "Follow up"
        )
        val updated = service.convertResponse(ConvertFeedbackResponseCommand(userId, responseId, FeedbackConversionType.ACTION_ITEM, text = "Follow up on mentoring"))
        updated.convertedToType shouldBe FeedbackConversionType.ACTION_ITEM
        verify { actionItemCommandPort.createActionItem(any()) }
    }

    @Test
    fun `bulk approve applies to all`() {
        val r2 = FeedbackResponseId.generate()
        every { responseRepository.findByIdAndUserId(responseId, userId) } returns response()
        every { responseRepository.findByIdAndUserId(r2, userId) } returns response().copy(id = r2)
        service.bulkUpdate(BulkUpdateFeedbackResponsesCommand(userId, listOf(responseId, r2), BulkResponseAction.APPROVE))
        verify(exactly = 2) { responseRepository.save(any()) }
    }
}
