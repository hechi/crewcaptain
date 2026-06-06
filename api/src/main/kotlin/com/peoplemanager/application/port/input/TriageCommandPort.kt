package com.peoplemanager.application.port.input

import com.peoplemanager.application.commands.SnoozeActionItemCommand
import com.peoplemanager.domain.ActionItem

interface TriageCommandPort {
    fun snoozeActionItem(command: SnoozeActionItemCommand): ActionItem
}
