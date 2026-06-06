package com.peoplemanager.application.commands

import com.peoplemanager.domain.ActionItemId
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.UserId
import java.time.Instant

data class SnoozeActionItemCommand(
    val userId: UserId,
    val personId: PersonId,
    val actionItemId: ActionItemId,
    val snoozedUntil: Instant
)
