package com.peoplemanager.application

import com.peoplemanager.domain.StrategyGoalId

class StrategyGoalNotFoundException(strategyGoalId: StrategyGoalId) : RuntimeException(
    "Strategy goal not found: ${strategyGoalId.value}"
)
