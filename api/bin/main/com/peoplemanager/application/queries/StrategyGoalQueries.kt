package com.peoplemanager.application.queries

import com.peoplemanager.domain.StrategyGoalId
import com.peoplemanager.domain.StrategyGoalStatus
import com.peoplemanager.domain.UserId

data class GetStrategyGoalQuery(
    val userId: UserId,
    val strategyGoalId: StrategyGoalId
)

data class ListStrategyGoalsQuery(
    val userId: UserId,
    val status: StrategyGoalStatus? = null,
    val page: Int = 0,
    val size: Int = 20
)

data class GetAlignmentScoreQuery(
    val userId: UserId,
    val strategyGoalId: StrategyGoalId
)

data class GetAllAlignmentScoresQuery(
    val userId: UserId
)

data class GetGapAnalysisQuery(
    val userId: UserId
)

data class GetLinkedPdpGoalsQuery(
    val userId: UserId,
    val strategyGoalId: StrategyGoalId
)
