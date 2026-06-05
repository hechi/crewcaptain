package com.peoplemanager.application.port.output

import com.peoplemanager.domain.StrategyGoal
import com.peoplemanager.domain.StrategyGoalId
import com.peoplemanager.domain.StrategyGoalStatus
import com.peoplemanager.domain.UserId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface StrategyGoalRepository {
    fun save(strategyGoal: StrategyGoal): StrategyGoal
    fun findByIdAndUserId(strategyGoalId: StrategyGoalId, userId: UserId): StrategyGoal?
    fun findAllByUserId(userId: UserId, status: StrategyGoalStatus?, pageable: Pageable): Page<StrategyGoal>
    fun deleteByIdAndUserId(strategyGoalId: StrategyGoalId, userId: UserId): Boolean
    fun countActiveByUserId(userId: UserId): Long
    fun countByUserIdAndStatus(userId: UserId, status: StrategyGoalStatus): Long
}
