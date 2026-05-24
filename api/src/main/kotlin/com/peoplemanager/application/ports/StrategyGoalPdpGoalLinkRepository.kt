package com.peoplemanager.application.ports

import com.peoplemanager.domain.PdpGoalId
import com.peoplemanager.domain.PersonId
import com.peoplemanager.domain.StrategyGoalId
import com.peoplemanager.domain.StrategyGoalPdpGoalLink
import com.peoplemanager.domain.StrategyGoalPdpGoalLinkId
import com.peoplemanager.domain.UserId

interface StrategyGoalPdpGoalLinkRepository {
    fun save(link: StrategyGoalPdpGoalLink): StrategyGoalPdpGoalLink
    fun findByIdAndUserId(linkId: StrategyGoalPdpGoalLinkId, userId: UserId): StrategyGoalPdpGoalLink?
    fun findAllByStrategyGoalIdAndUserId(strategyGoalId: StrategyGoalId, userId: UserId): List<StrategyGoalPdpGoalLink>
    fun findAllByPdpGoalIdAndUserId(pdpGoalId: PdpGoalId, userId: UserId): List<StrategyGoalPdpGoalLink>
    fun findAllByUserId(userId: UserId): List<StrategyGoalPdpGoalLink>
    fun findAllByUserIdAndPersonId(userId: UserId, personId: PersonId): List<StrategyGoalPdpGoalLink>
    fun existsByStrategyGoalIdAndPdpGoalIdAndUserId(strategyGoalId: StrategyGoalId, pdpGoalId: PdpGoalId, userId: UserId): Boolean
    fun countByStrategyGoalIdAndUserId(strategyGoalId: StrategyGoalId, userId: UserId): Long
    fun deleteByStrategyGoalIdAndPdpGoalIdAndUserId(strategyGoalId: StrategyGoalId, pdpGoalId: PdpGoalId, userId: UserId): Boolean
    fun deleteByStrategyGoalIdAndUserId(strategyGoalId: StrategyGoalId, userId: UserId): Long
    fun deleteByPdpGoalIdAndUserId(pdpGoalId: PdpGoalId, userId: UserId): Long
}
