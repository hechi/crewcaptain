package com.peoplemanager.application

import com.peoplemanager.application.commands.CancelActionItemCommand
import com.peoplemanager.application.commands.CompleteActionItemCommand
import com.peoplemanager.application.commands.CreateActionItemCommand
import com.peoplemanager.application.commands.DeleteActionItemCommand
import com.peoplemanager.application.commands.UpdateActionItemCommand
import com.peoplemanager.application.port.input.ActionItemCommandPort
import com.peoplemanager.application.port.input.ActionItemQueryPort
import com.peoplemanager.application.port.output.ActionItemRepository
import com.peoplemanager.application.port.output.PersonRepository
import com.peoplemanager.application.queries.CountOpenActionItemsQuery
import com.peoplemanager.application.queries.GetActionItemQuery
import com.peoplemanager.application.queries.ListActionItemsByPersonQuery
import com.peoplemanager.application.queries.ListAllActionItemsQuery
import com.peoplemanager.domain.ActionItem
import com.peoplemanager.domain.ActionItemId
import com.peoplemanager.domain.AuditLogEntry
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.UserId
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional
class ActionItemService(
    private val personRepository: PersonRepository,
    private val actionItemRepository: ActionItemRepository,
    private val auditLogService: AuditLogService
) : ActionItemCommandPort, ActionItemQueryPort {

    override fun createActionItem(command: CreateActionItemCommand): ActionItem {
        // Verify person belongs to user (skipped for person-less self TODOs)
        if (command.personId != null) {
            personRepository.findByIdAndUserId(command.personId, command.userId)
                ?: throw PersonNotFoundException(command.personId)
        }

        val actionItem = ActionItem(
            id = ActionItemId.generate(),
            userId = command.userId,
            personId = command.personId,
            title = command.title,
            description = command.description,
            ownerType = command.ownerType,
            dueDate = command.dueDate,
            originatingEntryId = command.originatingEntryId
        )

        val saved = actionItemRepository.save(actionItem)
        auditLogService.record(AuditLogEntry.actionItemCreated(command.userId, saved.id, command.personId, saved.title))
        return saved
    }

    override fun updateActionItem(command: UpdateActionItemCommand): ActionItem {
        val existing = actionItemRepository.findByIdAndUserIdAndPersonId(
            command.actionItemId, command.userId, command.personId
        ) ?: throw ActionItemNotFoundException(command.actionItemId)

        val updated = existing.updateDetails(
            title = command.title,
            description = command.description,
            ownerType = command.ownerType,
            dueDate = command.dueDate
        )

        val saved = actionItemRepository.save(updated)
        auditLogService.record(AuditLogEntry.actionItemUpdated(command.userId, saved.id, command.personId, saved.title))
        return saved
    }

    override fun completeActionItem(command: CompleteActionItemCommand): ActionItem {
        val existing = findForUser(command.actionItemId, command.userId, command.personId)
            ?: throw ActionItemNotFoundException(command.actionItemId)

        val completed = existing.complete()
        return actionItemRepository.save(completed)
    }

    override fun cancelActionItem(command: CancelActionItemCommand): ActionItem {
        val existing = findForUser(command.actionItemId, command.userId, command.personId)
            ?: throw ActionItemNotFoundException(command.actionItemId)

        val canceled = existing.cancel()
        return actionItemRepository.save(canceled)
    }

    override fun deleteActionItem(command: DeleteActionItemCommand) {
        val existing = findForUser(command.actionItemId, command.userId, command.personId)
            ?: throw ActionItemNotFoundException(command.actionItemId)
        val deleted = if (command.personId != null) {
            actionItemRepository.deleteByIdAndUserIdAndPersonId(
                command.actionItemId, command.userId, command.personId
            )
        } else {
            actionItemRepository.deleteByIdAndUserId(command.actionItemId, command.userId)
        }
        if (!deleted) throw ActionItemNotFoundException(command.actionItemId)
        auditLogService.record(AuditLogEntry.actionItemDeleted(command.userId, command.actionItemId, command.personId, existing.title))
    }

    override fun getActionItem(query: GetActionItemQuery): ActionItem {
        return findForUser(query.actionItemId, query.userId, query.personId)
            ?: throw ActionItemNotFoundException(query.actionItemId)
    }

    /**
     * Resolves an action item scoped to the user. When a personId is supplied the lookup is
     * additionally scoped to that person; otherwise the user-scoped lookup is used, which is how
     * person-less (self-assigned) TODOs are reached.
     */
    private fun findForUser(
        actionItemId: ActionItemId,
        userId: UserId,
        personId: PersonId?
    ): ActionItem? = if (personId != null) {
        actionItemRepository.findByIdAndUserIdAndPersonId(actionItemId, userId, personId)
    } else {
        actionItemRepository.findByIdAndUserId(actionItemId, userId)
    }

    override fun listActionItemsByPerson(query: ListActionItemsByPersonQuery): Page<ActionItem> {
        // Verify person belongs to user
        personRepository.findByIdAndUserId(query.personId, query.userId)
            ?: throw PersonNotFoundException(query.personId)

        val pageable = PageRequest.of(query.page, query.size, Sort.by(Sort.Direction.ASC, "dueDate"))

        return if (query.originatingEntryId != null) {
            actionItemRepository.findAllByUserIdAndPersonIdAndOriginatingEntryId(
                query.userId, query.personId, query.originatingEntryId, pageable
            )
        } else {
            actionItemRepository.findAllByUserIdAndPersonId(
                query.userId, query.personId, query.status, pageable
            )
        }
    }

    override fun listAllActionItems(query: ListAllActionItemsQuery): Page<ActionItem> {
        val pageable = PageRequest.of(query.page, query.size, Sort.by(Sort.Direction.ASC, "dueDate"))

        return if (query.overdueOnly) {
            actionItemRepository.findOverdueByUserId(query.userId, LocalDate.now(), pageable)
        } else {
            actionItemRepository.findAllByUserId(query.userId, query.status, pageable)
        }
    }

    override fun countOpenActionItems(query: CountOpenActionItemsQuery): Long {
        return actionItemRepository.countOpenByUserIdAndPersonId(query.userId, query.personId)
    }
}
