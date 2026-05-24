package com.peoplemanager.application.commands

import com.peoplemanager.domain.PdpGoalId
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.StrategyGoalId
import com.peoplemanager.domain.UserId
import java.time.LocalDate

data class CreateStrategyGoalCommand(
    val userId: UserId,
    val title: String,
    val description: String? = null,
    val targetDate: LocalDate? = null,
    val sensitive: Boolean = false
)

data class UpdateStrategyGoalCommand(
    val userId: UserId,
    val strategyGoalId: StrategyGoalId,
    val title: String? = null,
    val description: String? = null,
    val targetDate: LocalDate? = null
)

data class AchieveStrategyGoalCommand(
    val userId: UserId,
    val strategyGoalId: StrategyGoalId
)

data class DropStrategyGoalCommand(
    val userId: UserId,
    val strategyGoalId: StrategyGoalId
)

data class DeleteStrategyGoalCommand(
    val userId: UserId,
    val strategyGoalId: StrategyGoalId
)

data class LinkPdpGoalToStrategyGoalCommand(
    val userId: UserId,
    val strategyGoalId: StrategyGoalId,
    val pdpGoalId: PdpGoalId,
    val personId: PersonId
)

data class UnlinkPdpGoalFromStrategyGoalCommand(
    val userId: UserId,
    val strategyGoalId: StrategyGoalId,
    val pdpGoalId: PdpGoalId
)
