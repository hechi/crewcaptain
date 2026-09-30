package com.peoplemanager.application

import com.peoplemanager.application.commands.*
import com.peoplemanager.application.port.output.FeedbackLinkRepository
import com.peoplemanager.application.port.output.FeedbackResponseRepository
import com.peoplemanager.application.port.output.FeedbackTemplateRepository
import com.peoplemanager.application.port.output.PersonRepository
import com.peoplemanager.application.queries.ListFeedbackLinksQuery
import com.peoplemanager.domain.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FeedbackLinkServiceTest {

    private val personRepository = mockk<PersonRepository>()
    private val templateRepository = mockk<FeedbackTemplateRepository>()
    private val linkRepository = mockk<FeedbackLinkRepository>()
    private val responseRepository = mockk<FeedbackResponseRepository>(relaxed = true)
    private val auditLogService = mockk<AuditLogService>(relaxed = true)

    private val service = FeedbackLinkService(personRepository, templateRepository, linkRepository, responseRepository, auditLogService)

    private val userId = UserId.generate()
    private val personId = PersonId.generate()
    private val person = Person(id = personId, userId = userId, name = "Alex")
    private val question = FeedbackQuestion("q1", FeedbackQuestionType.RATING, "Rate")

    @BeforeEach
    fun setup() {
        clearAllMocks()
        every { linkRepository.existsByToken(any()) } returns false
        every { linkRepository.save(any()) } answers { firstArg() }
    }

    @Test
    fun `createLink from template snapshots questions`() {
        val templateId = FeedbackTemplateId.generate()
        val template = FeedbackTemplate(templateId, userId, "Mid-year", "d", listOf(question))
        every { personRepository.findByIdAndUserId(personId, userId) } returns person
        every { templateRepository.findByIdAndUserId(templateId, userId) } returns template

        val link = service.createLink(CreateFeedbackLinkCommand(userId, personId, templateId = templateId))

        link.title shouldBe "Mid-year"
        link.questions shouldHaveSize 1
        link.sourceTemplateId shouldBe templateId
        link.token.length shouldBe 22
        verify { auditLogService.record(match { it.action == AuditAction.CREATE && it.entityType == AuditEntityType.FEEDBACK_LINK }) }
    }

    @Test
    fun `createLink from inline questions works without a template`() {
        every { personRepository.findByIdAndUserId(personId, userId) } returns person
        val link = service.createLink(
            CreateFeedbackLinkCommand(userId, personId, inlineTitle = "Ad-hoc", inlineQuestions = listOf(question))
        )
        link.title shouldBe "Ad-hoc"
        link.sourceTemplateId shouldBe null
    }

    @Test
    fun `createLink rejects when neither template nor inline provided`() {
        every { personRepository.findByIdAndUserId(personId, userId) } returns person
        shouldThrow<IllegalArgumentException> {
            service.createLink(CreateFeedbackLinkCommand(userId, personId))
        }
    }

    @Test
    fun `createLink rejects unowned person`() {
        every { personRepository.findByIdAndUserId(personId, userId) } returns null
        shouldThrow<PersonNotFoundException> {
            service.createLink(CreateFeedbackLinkCommand(userId, personId, inlineTitle = "x", inlineQuestions = listOf(question)))
        }
    }

    @Test
    fun `createLink rejects expiry beyond max`() {
        every { personRepository.findByIdAndUserId(personId, userId) } returns person
        shouldThrow<IllegalArgumentException> {
            service.createLink(CreateFeedbackLinkCommand(userId, personId, inlineTitle = "x", inlineQuestions = listOf(question), expiresInDays = 999))
        }
    }

    @Test
    fun `bulkCreate makes one link per person`() {
        val templateId = FeedbackTemplateId.generate()
        val template = FeedbackTemplate(templateId, userId, "Retro", null, listOf(question))
        val p2 = PersonId.generate()
        every { templateRepository.findByIdAndUserId(templateId, userId) } returns template
        every { personRepository.findByIdAndUserId(personId, userId) } returns person
        every { personRepository.findByIdAndUserId(p2, userId) } returns Person(id = p2, userId = userId, name = "Bo")

        val results = service.bulkCreateLinks(BulkCreateFeedbackLinksCommand(userId, listOf(personId, p2), templateId))
        results shouldHaveSize 2
        results.map { it.second } shouldBe listOf("Alex", "Bo")
    }

    @Test
    fun `revoke marks link revoked`() {
        val id = FeedbackLinkId.generate()
        val link = FeedbackLink(id, userId, personId, "tok", "t", questions = listOf(question), expiresAt = java.time.Instant.now().plusSeconds(9999))
        every { linkRepository.findByIdAndUserId(id, userId) } returns link
        every { personRepository.findByIdAndUserId(personId, userId) } returns person

        val revoked = service.revokeLink(RevokeFeedbackLinkCommand(userId, id))
        revoked.revokedAt shouldBe revoked.revokedAt // present
        (revoked.revokedAt != null) shouldBe true
        verify { auditLogService.record(match { it.action == AuditAction.REVOKE }) }
    }

    @Test
    fun `revoke throws when link not owned`() {
        val id = FeedbackLinkId.generate()
        every { linkRepository.findByIdAndUserId(id, userId) } returns null
        shouldThrow<FeedbackLinkNotFoundException> {
            service.revokeLink(RevokeFeedbackLinkCommand(userId, id))
        }
    }

    @Test
    fun `listLinks includes usage stats`() {
        val id = FeedbackLinkId.generate()
        val link = FeedbackLink(id, userId, personId, "tok", "t", questions = listOf(question), expiresAt = java.time.Instant.now().plusSeconds(9999))
        every { personRepository.findByIdAndUserId(personId, userId) } returns person
        every { linkRepository.findAllByUserIdAndPersonId(userId, personId) } returns listOf(link)
        every { responseRepository.countByLinkId(id) } returns 3
        every { responseRepository.findLatestCreatedAtByLinkId(id) } returns java.time.Instant.now()

        val result = service.listLinks(ListFeedbackLinksQuery(userId, personId))
        result shouldHaveSize 1
        result[0].submissionCount shouldBe 3
    }
}
