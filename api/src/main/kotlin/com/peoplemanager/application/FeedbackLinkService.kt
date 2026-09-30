package com.peoplemanager.application

import com.peoplemanager.application.commands.BulkCreateFeedbackLinksCommand
import com.peoplemanager.application.commands.CreateFeedbackLinkCommand
import com.peoplemanager.application.commands.ExtendFeedbackLinkCommand
import com.peoplemanager.application.commands.RevokeFeedbackLinkCommand
import com.peoplemanager.application.port.input.FeedbackLinkCommandPort
import com.peoplemanager.application.port.input.FeedbackLinkQueryPort
import com.peoplemanager.application.port.input.FeedbackLinkWithUsage
import com.peoplemanager.application.port.output.FeedbackLinkRepository
import com.peoplemanager.application.port.output.FeedbackResponseRepository
import com.peoplemanager.application.port.output.FeedbackTemplateRepository
import com.peoplemanager.application.port.output.PersonRepository
import com.peoplemanager.application.queries.ListFeedbackLinksQuery
import com.peoplemanager.domain.AuditLogEntry
import com.peoplemanager.domain.FeedbackLink
import com.peoplemanager.domain.FeedbackLinkId
import com.peoplemanager.domain.FeedbackQuestion
import com.peoplemanager.domain.Person
import com.peoplemanager.domain.PersonId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
@Transactional
class FeedbackLinkService(
    private val personRepository: PersonRepository,
    private val feedbackTemplateRepository: FeedbackTemplateRepository,
    private val feedbackLinkRepository: FeedbackLinkRepository,
    private val feedbackResponseRepository: FeedbackResponseRepository,
    private val auditLogService: AuditLogService
) : FeedbackLinkCommandPort, FeedbackLinkQueryPort {

    override fun createLink(command: CreateFeedbackLinkCommand): FeedbackLink {
        val person = personRepository.findByIdAndUserId(command.personId, command.userId)
            ?: throw PersonNotFoundException(command.personId)

        val (title, description, questions, templateId) = resolveContent(command)
        val link = buildLink(
            userId = person.userId,
            personId = person.id,
            title = title,
            description = description,
            questions = questions,
            expiresInDays = command.expiresInDays,
            label = command.label,
            requestSubmitterInfo = command.requestSubmitterInfo,
            sourceTemplateId = templateId
        )
        val saved = feedbackLinkRepository.save(link)
        auditLogService.record(
            AuditLogEntry.feedbackLinkCreated(command.userId, saved.id, person.id, person.name, saved.title)
        )
        return saved
    }

    override fun bulkCreateLinks(command: BulkCreateFeedbackLinksCommand): List<Pair<FeedbackLink, String>> {
        require(command.personIds.isNotEmpty()) { "Select at least one person" }

        val template = feedbackTemplateRepository.findByIdAndUserId(command.templateId, command.userId)
            ?: throw FeedbackTemplateNotFoundException(command.templateId)
        require(template.questions.isNotEmpty()) { "Template has no questions" }

        return command.personIds.map { personId ->
            val person = personRepository.findByIdAndUserId(personId, command.userId)
                ?: throw PersonNotFoundException(personId)
            val link = buildLink(
                userId = command.userId,
                personId = person.id,
                title = template.title,
                description = template.description,
                questions = template.questions,
                expiresInDays = command.expiresInDays,
                label = command.label,
                requestSubmitterInfo = command.requestSubmitterInfo,
                sourceTemplateId = template.id
            )
            val saved = feedbackLinkRepository.save(link)
            auditLogService.record(
                AuditLogEntry.feedbackLinkCreated(command.userId, saved.id, person.id, person.name, saved.title)
            )
            saved to person.name
        }
    }

    override fun revokeLink(command: RevokeFeedbackLinkCommand): FeedbackLink {
        val link = feedbackLinkRepository.findByIdAndUserId(command.linkId, command.userId)
            ?: throw FeedbackLinkNotFoundException(command.linkId)
        val revoked = feedbackLinkRepository.save(link.revoke())
        val person = personRepository.findByIdAndUserId(revoked.personId, command.userId)
        auditLogService.record(
            AuditLogEntry.feedbackLinkRevoked(command.userId, revoked.id, revoked.personId, person?.name ?: "Unknown")
        )
        return revoked
    }

    override fun extendLink(command: ExtendFeedbackLinkCommand): FeedbackLink {
        require(command.expiresInDays in 1..FeedbackLink.MAX_EXPIRY_DAYS) {
            "Expiry must be between 1 and ${FeedbackLink.MAX_EXPIRY_DAYS} days"
        }
        val link = feedbackLinkRepository.findByIdAndUserId(command.linkId, command.userId)
            ?: throw FeedbackLinkNotFoundException(command.linkId)
        val newExpiry = Instant.now().plus(command.expiresInDays, ChronoUnit.DAYS)
        val extended = feedbackLinkRepository.save(link.extend(newExpiry))
        val person = personRepository.findByIdAndUserId(extended.personId, command.userId)
        auditLogService.record(
            AuditLogEntry.feedbackLinkExtended(command.userId, extended.id, extended.personId, person?.name ?: "Unknown")
        )
        return extended
    }

    @Transactional(readOnly = true)
    override fun listLinks(query: ListFeedbackLinksQuery): List<FeedbackLinkWithUsage> {
        personRepository.findByIdAndUserId(query.personId, query.userId)
            ?: throw PersonNotFoundException(query.personId)

        return feedbackLinkRepository.findAllByUserIdAndPersonId(query.userId, query.personId).map { link ->
            FeedbackLinkWithUsage(
                link = link,
                submissionCount = feedbackResponseRepository.countByLinkId(link.id),
                lastSubmissionAt = feedbackResponseRepository.findLatestCreatedAtByLinkId(link.id)
            )
        }
    }

    // --- helpers ---

    private data class ResolvedContent(
        val title: String,
        val description: String?,
        val questions: List<FeedbackQuestion>,
        val templateId: com.peoplemanager.domain.FeedbackTemplateId?
    )

    private fun resolveContent(command: CreateFeedbackLinkCommand): ResolvedContent {
        if (command.templateId != null) {
            val template = feedbackTemplateRepository.findByIdAndUserId(command.templateId, command.userId)
                ?: throw FeedbackTemplateNotFoundException(command.templateId)
            require(template.questions.isNotEmpty()) { "Template has no questions" }
            return ResolvedContent(template.title, template.description, template.questions, template.id)
        }
        val title = command.inlineTitle?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("A template or an inline title with questions is required")
        val questions = command.inlineQuestions?.takeIf { it.isNotEmpty() }
            ?: throw IllegalArgumentException("At least one question is required")
        return ResolvedContent(title, command.inlineDescription, questions, null)
    }

    private fun buildLink(
        userId: com.peoplemanager.domain.UserId,
        personId: PersonId,
        title: String,
        description: String?,
        questions: List<FeedbackQuestion>,
        expiresInDays: Long?,
        label: String?,
        requestSubmitterInfo: Boolean,
        sourceTemplateId: com.peoplemanager.domain.FeedbackTemplateId?
    ): FeedbackLink {
        val days = (expiresInDays ?: FeedbackLink.DEFAULT_EXPIRY_DAYS)
        require(days in 1..FeedbackLink.MAX_EXPIRY_DAYS) {
            "Expiry must be between 1 and ${FeedbackLink.MAX_EXPIRY_DAYS} days"
        }
        return FeedbackLink(
            id = FeedbackLinkId.generate(),
            userId = userId,
            personId = personId,
            token = generateUniqueToken(),
            title = title,
            description = description,
            questions = questions,
            label = label,
            requestSubmitterInfo = requestSubmitterInfo,
            expiresAt = Instant.now().plus(days, ChronoUnit.DAYS),
            sourceTemplateId = sourceTemplateId
        )
    }

    private fun generateUniqueToken(): String {
        repeat(5) {
            val token = FeedbackLink.generateToken()
            if (!feedbackLinkRepository.existsByToken(token)) return token
        }
        // Astronomically unlikely; fail loudly rather than risk a collision.
        throw IllegalStateException("Could not generate a unique feedback link token")
    }
}
