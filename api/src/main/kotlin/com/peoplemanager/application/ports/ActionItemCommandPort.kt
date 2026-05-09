package com.peoplemanager.application.ports

import com.peoplemanager.application.commands.CancelActionItemCommand
import com.peoplemanager.application.commands.CompleteActionItemCommand
import com.peoplemanager.application.commands.CreateActionItemCommand
import com.peoplemanager.application.commands.DeleteActionItemCommand
import com.peoplemanager.application.commands.UpdateActionItemCommand
import com.peoplemanager.domain.ActionItem

interface ActionItemCommandPort {
    fun createActionItem(command: CreateActionItemCommand): ActionItem
    fun updateActionItem(command: UpdateActionItemCommand): ActionItem
    fun completeActionItem(command: CompleteActionItemCommand): ActionItem
    fun cancelActionItem(command: CancelActionItemCommand): ActionItem
    fun deleteActionItem(command: DeleteActionItemCommand)
}
