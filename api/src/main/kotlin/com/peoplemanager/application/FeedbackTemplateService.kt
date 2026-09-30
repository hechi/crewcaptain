package com.peoplemanager.application

import com.peoplemanager.application.commands.CreateFeedbackTemplateCommand
import com.peoplemanager.application.commands.DeleteFeedbackTemplateCommand
import com.peoplemanager.application.commands.UpdateFeedbackTemplateCommand
import com.peoplemanager.application.port.input.FeedbackTemplateCommandPort
import com.peoplemanager.application.port.input.FeedbackTemplateQueryPort
import com.peoplemanager.application.port.output.FeedbackTemplateRepository
import com.peoplemanager.application.queries.GetFeedbackTemplateQuery
import com.peoplemanager.application.queries.ListFeedbackTemplatesQuery
import com.peoplemanager.domain.AuditLogEntry
import com.peoplemanager.domain.FeedbackStarterTemplates
import com.peoplemanager.domain.FeedbackTemplate
import com.peoplemanager.domain.FeedbackTemplateId
import com.peoplemanager.domain.UserId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class FeedbackTemplateService(
    private val feedbackTemplateRepository: FeedbackTemplateRepository,
    private val auditLogService: AuditLogService
) : FeedbackTemplateCommandPort, FeedbackTemplateQueryPort {

    override fun createTemplate(command: CreateFeedbackTemplateCommand): FeedbackTemplate {
        val template = FeedbackTemplate(
            id = FeedbackTemplateId.generate(),
            userId = command.userId,
            title = command.title,
            description = command.description,
            questions = command.questions
        )
        val saved = feedbackTemplateRepository.save(template)
        auditLogService.record(AuditLogEntry.feedbackTemplateCreated(command.userId, saved.id, saved.title))
        return saved
    }

    override fun updateTemplate(command: UpdateFeedbackTemplateCommand): FeedbackTemplate {
        val existing = feedbackTemplateRepository.findByIdAndUserId(command.templateId, command.userId)
            ?: throw FeedbackTemplateNotFoundException(command.templateId)

        val updated = existing.update(command.title, command.description, command.questions)
        val saved = feedbackTemplateRepository.save(updated)
        auditLogService.record(AuditLogEntry.feedbackTemplateUpdated(command.userId, saved.id, saved.title))
        return saved
    }

    override fun deleteTemplate(command: DeleteFeedbackTemplateCommand) {
        val deleted = feedbackTemplateRepository.deleteByIdAndUserId(command.templateId, command.userId)
        if (!deleted) throw FeedbackTemplateNotFoundException(command.templateId)
        auditLogService.record(AuditLogEntry.feedbackTemplateDeleted(command.userId, command.templateId))
    }

    @Transactional(readOnly = true)
    override fun getTemplate(query: GetFeedbackTemplateQuery): FeedbackTemplate {
        return feedbackTemplateRepository.findByIdAndUserId(query.templateId, query.userId)
            ?: throw FeedbackTemplateNotFoundException(query.templateId)
    }

    @Transactional(readOnly = true)
    override fun listTemplates(query: ListFeedbackTemplatesQuery): List<FeedbackTemplate> {
        return feedbackTemplateRepository.findAllByUserId(query.userId)
    }

    @Transactional(readOnly = true)
    override fun listStarterTemplates(): List<FeedbackTemplate> {
        // Starters are seeded against a placeholder user; the frontend copies them into
        // the manager's own library via createTemplate. Use a stable throwaway UserId.
        return FeedbackStarterTemplates.forUser(UserId.generate())
    }
}
