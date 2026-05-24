package com.peoplemanager.domain

import java.time.Instant

data class StrategyGoalPdpGoalLink(
    val id: StrategyGoalPdpGoalLinkId,
    val userId: UserId,
    val strategyGoalId: StrategyGoalId,
    val pdpGoalId: PdpGoalId,
    val personId: PersonId,
    val createdAt: Instant = Instant.now()
) {
    companion object {
        fun create(
            userId: UserId,
            strategyGoalId: StrategyGoalId,
            pdpGoalId: PdpGoalId,
            personId: PersonId
        ): StrategyGoalPdpGoalLink {
            return StrategyGoalPdpGoalLink(
                id = StrategyGoalPdpGoalLinkId.generate(),
                userId = userId,
                strategyGoalId = strategyGoalId,
                pdpGoalId = pdpGoalId,
                personId = personId
            )
        }
    }
}
