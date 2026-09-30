package com.peoplemanager.application

import com.peoplemanager.application.commands.*
import com.peoplemanager.application.port.input.FeedbackResponseCommandPort
import com.peoplemanager.application.port.input.FeedbackResponseQueryPort
import com.peoplemanager.application.port.input.ActionItemCommandPort
import com.peoplemanager.application.port.input.KudosCommandPort
import com.peoplemanager.application.port.input.QuickNoteCommandPort
import com.peoplemanager.application.port.output.FeedbackResponseRepository
import com.peoplemanager.application.port.output.PersonRepository
import com.peoplemanager.application.queries.ListFeedbackResponsesQuery
import com.peoplemanager.domain.AuditLogEntry
import com.peoplemanager.domain.FeedbackConversionType
import com.peoplemanager.domain.FeedbackResponse
import com.peoplemanager.domain.Person
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * Manager-facing feedback response review: approve, flag, pin, delete, bulk actions,
 * and conversion into a Kudo / Quick Note / Action Item. Conversions delegate to the
 * existing command ports so their validation and audit logging apply.
 */
@Service
@Transactional
class FeedbackResponseService(
    private val personRepository: PersonRepository,
    private val feedbackResponseRepository: FeedbackResponseRepository,
    private val auditLogService: AuditLogService,
    private val kudosCommandPort: KudosCommandPort,
    private val quickNoteCommandPort: QuickNoteCommandPort,
    private val actionItemCommandPort: ActionItemCommandPort
) : FeedbackResponseCommandPort, FeedbackResponseQueryPort {

    override fun updateResponse(command: UpdateFeedbackResponseCommand): FeedbackResponse {
        val existing = feedbackResponseRepository.findByIdAndUserId(command.responseId, command.userId)
            ?: throw FeedbackResponseNotFoundException(command.responseId)
        val person = personRepository.findByIdAndUserId(existing.personId, command.userId)

        var updated = existing
        if (command.approve == true && existing.status != com.peoplemanager.domain.FeedbackResponseStatus.APPROVED) {
            updated = updated.approve()
            auditLogService.record(
                AuditLogEntry.feedbackResponseApproved(command.userId, existing.id, existing.personId, person?.name ?: "Unknown")
            )
        }
        if (command.flagged != null && command.flagged != existing.flagged) {
            updated = updated.setFlagged(command.flagged)
            auditLogService.record(
                AuditLogEntry.feedbackResponseFlagged(command.userId, existing.id, existing.personId, person?.name ?: "Unknown", command.flagged)
            )
        }
        if (command.pinned != null && command.pinned != existing.pinned) {
            updated = updated.setPinned(command.pinned)
        }
        return feedbackResponseRepository.save(updated)
    }

    override fun deleteResponse(command: DeleteFeedbackResponseCommand) {
        val existing = feedbackResponseRepository.findByIdAndUserId(command.responseId, command.userId)
            ?: throw FeedbackResponseNotFoundException(command.responseId)
        val person = personRepository.findByIdAndUserId(existing.personId, command.userId)
        val deleted = feedbackResponseRepository.deleteByIdAndUserId(command.responseId, command.userId)
        if (!deleted) throw FeedbackResponseNotFoundException(command.responseId)
        auditLogService.record(
            AuditLogEntry.feedbackResponseDeleted(command.userId, existing.id, existing.personId, person?.name ?: "Unknown")
        )
    }

    override fun bulkUpdate(command: BulkUpdateFeedbackResponsesCommand) {
        command.responseIds.forEach { id ->
            when (command.action) {
                BulkResponseAction.APPROVE ->
                    updateResponse(UpdateFeedbackResponseCommand(command.userId, id, approve = true))
                BulkResponseAction.DELETE ->
                    deleteResponse(DeleteFeedbackResponseCommand(command.userId, id))
            }
        }
    }

    override fun convertResponse(command: ConvertFeedbackResponseCommand): FeedbackResponse {
        val existing = feedbackResponseRepository.findByIdAndUserId(command.responseId, command.userId)
            ?: throw FeedbackResponseNotFoundException(command.responseId)
        val person = personRepository.findByIdAndUserId(existing.personId, command.userId)
            ?: throw PersonNotFoundException(existing.personId)

        val content = command.text?.takeIf { it.isNotBlank() } ?: defaultConversionText(existing, person)
        require(content.isNotBlank()) { "Nothing to convert: the response has no usable text" }

        val targetId: String = when (command.type) {
            FeedbackConversionType.KUDO -> {
                val kudos = kudosCommandPort.createKudos(
                    CreateKudosCommand(
                        userId = command.userId,
                        personId = existing.personId,
                        date = LocalDate.now(),
                        text = content,
                        tags = listOf("feedback")
                    )
                )
                kudos.id.value.toString()
            }
            FeedbackConversionType.QUICK_NOTE -> {
                val note = quickNoteCommandPort.createQuickNote(
                    CreateQuickNoteCommand(
                        userId = command.userId,
                        personId = existing.personId,
                        text = content
                    )
                )
                note.id.value.toString()
            }
            FeedbackConversionType.ACTION_ITEM -> {
                val item = actionItemCommandPort.createActionItem(
                    CreateActionItemCommand(
                        userId = command.userId,
                        personId = existing.personId,
                        title = content.take(500),
                        description = if (content.length > 500) content else null
                    )
                )
                item.id.value.toString()
            }
        }

        val updated = feedbackResponseRepository.save(existing.markConverted(command.type, targetId))
        auditLogService.record(
            AuditLogEntry.feedbackResponseConverted(command.userId, existing.id, existing.personId, command.type, targetId)
        )
        return updated
    }

    @Transactional(readOnly = true)
    override fun listResponses(query: ListFeedbackResponsesQuery): List<FeedbackResponse> {
        personRepository.findByIdAndUserId(query.personId, query.userId)
            ?: throw PersonNotFoundException(query.personId)

        val base = if (query.status != null) {
            feedbackResponseRepository.findAllByUserIdAndPersonIdAndStatus(query.userId, query.personId, query.status)
        } else {
            feedbackResponseRepository.findAllByUserIdAndPersonId(query.userId, query.personId)
        }
        return if (query.flagged != null) base.filter { it.flagged == query.flagged } else base
    }

    private fun defaultConversionText(response: FeedbackResponse, person: Person): String {
        val parts = response.freeTextParts()
        return if (parts.isNotEmpty()) {
            parts.joinToString("\n\n")
        } else {
            "Feedback for ${person.name}"
        }
    }
}
