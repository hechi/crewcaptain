package com.peoplemanager.adapters.persistence

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "strategy_goal_pdp_goal_links")
class StrategyGoalPdpGoalLinkEntity(
    @Id
    @Column(name = "id")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    val userId: UUID = UUID.randomUUID(),

    @Column(name = "strategy_goal_id", nullable = false)
    val strategyGoalId: UUID = UUID.randomUUID(),

    @Column(name = "pdp_goal_id", nullable = false)
    val pdpGoalId: UUID = UUID.randomUUID(),

    @Column(name = "person_id", nullable = false)
    val personId: UUID = UUID.randomUUID(),

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)
