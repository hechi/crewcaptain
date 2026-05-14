package com.peoplemanager.application

import com.peoplemanager.application.commands.CancelActionItemCommand
import com.peoplemanager.application.commands.CompleteActionItemCommand
import com.peoplemanager.application.commands.CreateActionItemCommand
import com.peoplemanager.application.commands.DeleteActionItemCommand
import com.peoplemanager.application.commands.UpdateActionItemCommand
import com.peoplemanager.application.ports.ActionItemCommandPort
import com.peoplemanager.application.ports.ActionItemQueryPort
import com.peoplemanager.application.ports.ActionItemRepository
import com.peoplemanager.application.ports.PersonRepository
import com.peoplemanager.application.queries.CountOpenActionItemsQuery
import com.peoplemanager.application.queries.GetActionItemQuery
import com.peoplemanager.application.queries.ListActionItemsByPersonQuery
import com.peoplemanager.application.queries.ListAllActionItemsQuery
import com.peoplemanager.domain.ActionItem
import com.peoplemanager.domain.ActionItemId
import com.peoplemanager.domain.AuditLogEntry
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
        // Verify person belongs to user
        personRepository.findByIdAndUserId(command.personId, command.userId)
            ?: throw PersonNotFoundException(command.personId)

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
        val existing = actionItemRepository.findByIdAndUserIdAndPersonId(
            command.actionItemId, command.userId, command.personId
        ) ?: throw ActionItemNotFoundException(command.actionItemId)

        val completed = existing.complete()
        return actionItemRepository.save(completed)
    }

    override fun cancelActionItem(command: CancelActionItemCommand): ActionItem {
        val existing = actionItemRepository.findByIdAndUserIdAndPersonId(
            command.actionItemId, command.userId, command.personId
        ) ?: throw ActionItemNotFoundException(command.actionItemId)

        val canceled = existing.cancel()
        return actionItemRepository.save(canceled)
    }

    override fun deleteActionItem(command: DeleteActionItemCommand) {
        val existing = actionItemRepository.findByIdAndUserIdAndPersonId(
            command.actionItemId, command.userId, command.personId
        ) ?: throw ActionItemNotFoundException(command.actionItemId)
        val deleted = actionItemRepository.deleteByIdAndUserIdAndPersonId(
            command.actionItemId, command.userId, command.personId
        )
        if (!deleted) throw ActionItemNotFoundException(command.actionItemId)
        auditLogService.record(AuditLogEntry.actionItemDeleted(command.userId, command.actionItemId, command.personId, existing.title))
    }

    override fun getActionItem(query: GetActionItemQuery): ActionItem {
        return actionItemRepository.findByIdAndUserIdAndPersonId(
            query.actionItemId, query.userId, query.personId
        ) ?: throw ActionItemNotFoundException(query.actionItemId)
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
