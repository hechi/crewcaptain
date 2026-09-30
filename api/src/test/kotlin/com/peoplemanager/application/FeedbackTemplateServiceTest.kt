package com.peoplemanager.application

import com.peoplemanager.application.commands.CreateFeedbackTemplateCommand
import com.peoplemanager.application.commands.DeleteFeedbackTemplateCommand
import com.peoplemanager.application.commands.UpdateFeedbackTemplateCommand
import com.peoplemanager.application.port.output.FeedbackTemplateRepository
import com.peoplemanager.application.queries.GetFeedbackTemplateQuery
import com.peoplemanager.application.queries.ListFeedbackTemplatesQuery
import com.peoplemanager.domain.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FeedbackTemplateServiceTest {

    private val repository = mockk<FeedbackTemplateRepository>()
    private val auditLogService = mockk<AuditLogService>(relaxed = true)
    private val service = FeedbackTemplateService(repository, auditLogService)

    private val userId = UserId.generate()
    private val question = FeedbackQuestion("q1", FeedbackQuestionType.RATING, "Rate")

    @BeforeEach
    fun setup() = clearAllMocks()

    @Test
    fun `createTemplate persists and records audit`() {
        every { repository.save(any()) } answers { firstArg() }

        val result = service.createTemplate(
            CreateFeedbackTemplateCommand(userId, "Mid-year", "desc", listOf(question))
        )

        result.title shouldBe "Mid-year"
        result.userId shouldBe userId
        verify { repository.save(any()) }
        verify { auditLogService.record(match { it.action == AuditAction.CREATE && it.entityType == AuditEntityType.FEEDBACK_TEMPLATE }) }
    }

    @Test
    fun `updateTemplate throws when not owned`() {
        val id = FeedbackTemplateId.generate()
        every { repository.findByIdAndUserId(id, userId) } returns null

        shouldThrow<FeedbackTemplateNotFoundException> {
            service.updateTemplate(UpdateFeedbackTemplateCommand(userId, id, "t", null, listOf(question)))
        }
    }

    @Test
    fun `updateTemplate saves changes`() {
        val id = FeedbackTemplateId.generate()
        val existing = FeedbackTemplate(id, userId, "Old", null, listOf(question))
        every { repository.findByIdAndUserId(id, userId) } returns existing
        every { repository.save(any()) } answers { firstArg() }

        val result = service.updateTemplate(
            UpdateFeedbackTemplateCommand(userId, id, "New", "d", listOf(FeedbackQuestion("q1", FeedbackQuestionType.TEXT, "Explain")))
        )

        result.title shouldBe "New"
        result.questions[0].type shouldBe FeedbackQuestionType.TEXT
    }

    @Test
    fun `deleteTemplate throws when not found`() {
        val id = FeedbackTemplateId.generate()
        every { repository.deleteByIdAndUserId(id, userId) } returns false

        shouldThrow<FeedbackTemplateNotFoundException> {
            service.deleteTemplate(DeleteFeedbackTemplateCommand(userId, id))
        }
    }

    @Test
    fun `getTemplate returns owned template`() {
        val id = FeedbackTemplateId.generate()
        val t = FeedbackTemplate(id, userId, "T", null, listOf(question))
        every { repository.findByIdAndUserId(id, userId) } returns t

        service.getTemplate(GetFeedbackTemplateQuery(userId, id)).id shouldBe id
    }

    @Test
    fun `listTemplates delegates to repository`() {
        every { repository.findAllByUserId(userId) } returns listOf(
            FeedbackTemplate(FeedbackTemplateId.generate(), userId, "A", null, listOf(question))
        )
        service.listTemplates(ListFeedbackTemplatesQuery(userId)) shouldHaveSize 1
    }

    @Test
    fun `listStarterTemplates returns built-in starters`() {
        val starters = service.listStarterTemplates()
        starters shouldHaveSize 2
        starters.map { it.title } shouldBe listOf("Performance review", "Project retro")
    }
}
