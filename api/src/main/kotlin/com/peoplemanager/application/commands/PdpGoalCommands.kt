package com.peoplemanager.application.commands

import com.peoplemanager.domain.PdpGoalId
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.UserId
import java.time.LocalDate

data class CreatePdpGoalCommand(
    val userId: UserId,
    val personId: PersonId,
    val title: String,
    val description: String? = null,
    val targetDate: LocalDate? = null
)

data class UpdatePdpGoalCommand(
    val userId: UserId,
    val personId: PersonId,
    val goalId: PdpGoalId,
    val title: String? = null,
    val description: String? = null,
    val targetDate: LocalDate? = null
)

data class AchievePdpGoalCommand(
    val userId: UserId,
    val personId: PersonId,
    val goalId: PdpGoalId
)

data class PausePdpGoalCommand(
    val userId: UserId,
    val personId: PersonId,
    val goalId: PdpGoalId
)

data class DropPdpGoalCommand(
    val userId: UserId,
    val personId: PersonId,
    val goalId: PdpGoalId
)

data class ResumePdpGoalCommand(
    val userId: UserId,
    val personId: PersonId,
    val goalId: PdpGoalId
)

data class DeletePdpGoalCommand(
    val userId: UserId,
    val personId: PersonId,
    val goalId: PdpGoalId
)

data class AddPdpUpdateCommand(
    val userId: UserId,
    val personId: PersonId,
    val goalId: PdpGoalId,
    val textMarkdown: String,
    val sensitive: Boolean = false
)

data class DeletePdpUpdateCommand(
    val userId: UserId,
    val personId: PersonId,
    val goalId: PdpGoalId,
    val updateId: com.peoplemanager.domain.PdpUpdateId
)
