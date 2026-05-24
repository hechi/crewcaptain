package com.peoplemanager.adapters.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface SpringDataStrategyGoalPdpGoalLinkRepository : JpaRepository<StrategyGoalPdpGoalLinkEntity, UUID> {

    fun findByIdAndUserId(id: UUID, userId: UUID): StrategyGoalPdpGoalLinkEntity?

    fun findAllByStrategyGoalIdAndUserId(strategyGoalId: UUID, userId: UUID): List<StrategyGoalPdpGoalLinkEntity>

    fun findAllByPdpGoalIdAndUserId(pdpGoalId: UUID, userId: UUID): List<StrategyGoalPdpGoalLinkEntity>

    fun findAllByUserId(userId: UUID): List<StrategyGoalPdpGoalLinkEntity>

    @Query("SELECT l FROM StrategyGoalPdpGoalLinkEntity l WHERE l.userId = :userId AND l.personId = :personId")
    fun findAllByUserIdAndPersonId(
        @Param("userId") userId: UUID,
        @Param("personId") personId: UUID
    ): List<StrategyGoalPdpGoalLinkEntity>

    fun existsByStrategyGoalIdAndPdpGoalIdAndUserId(strategyGoalId: UUID, pdpGoalId: UUID, userId: UUID): Boolean

    @Query("SELECT COUNT(l) FROM StrategyGoalPdpGoalLinkEntity l WHERE l.strategyGoalId = :strategyGoalId AND l.userId = :userId")
    fun countByStrategyGoalIdAndUserId(
        @Param("strategyGoalId") strategyGoalId: UUID,
        @Param("userId") userId: UUID
    ): Long

    fun deleteByStrategyGoalIdAndPdpGoalIdAndUserId(strategyGoalId: UUID, pdpGoalId: UUID, userId: UUID): Long

    fun deleteByStrategyGoalIdAndUserId(strategyGoalId: UUID, userId: UUID): Long

    fun deleteByPdpGoalIdAndUserId(pdpGoalId: UUID, userId: UUID): Long
}
