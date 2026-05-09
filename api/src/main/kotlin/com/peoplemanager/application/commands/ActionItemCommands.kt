package com.peoplemanager.application.commands

import com.peoplemanager.domain.ActionItemId
import com.peoplemanager.domain.ActionItemOwnerType
import com.peoplemanager.domain.OneOnOneEntryId
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.UserId
import java.time.LocalDate

data class CreateActionItemCommand(
    val userId: UserId,
    val personId: PersonId,
    val title: String,
    val description: String? = null,
    val ownerType: ActionItemOwnerType = ActionItemOwnerType.MANAGER,
    val dueDate: LocalDate? = null,
    val originatingEntryId: OneOnOneEntryId? = null
)

data class UpdateActionItemCommand(
    val userId: UserId,
    val personId: PersonId,
    val actionItemId: ActionItemId,
    val title: String? = null,
    val description: String? = null,
    val ownerType: ActionItemOwnerType? = null,
    val dueDate: LocalDate? = null
)

data class CompleteActionItemCommand(
    val userId: UserId,
    val personId: PersonId,
    val actionItemId: ActionItemId
)

data class CancelActionItemCommand(
    val userId: UserId,
    val personId: PersonId,
    val actionItemId: ActionItemId
)

data class DeleteActionItemCommand(
    val userId: UserId,
    val personId: PersonId,
    val actionItemId: ActionItemId
)
