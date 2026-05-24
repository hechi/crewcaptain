package com.peoplemanager.domain

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

class StrategyGoalPdpGoalLinkTest {

    private val userId = UserId.generate()
    private val strategyGoalId = StrategyGoalId.generate()
    private val pdpGoalId = PdpGoalId.generate()
    private val personId = PersonId.generate()

    @Test
    fun `should create link with factory method`() {
        val link = StrategyGoalPdpGoalLink.create(
            userId = userId,
            strategyGoalId = strategyGoalId,
            pdpGoalId = pdpGoalId,
            personId = personId
        )

        link.id shouldNotBe null
        link.userId shouldBe userId
        link.strategyGoalId shouldBe strategyGoalId
        link.pdpGoalId shouldBe pdpGoalId
        link.personId shouldBe personId
    }

    @Test
    fun `should generate unique ids for each link`() {
        val link1 = StrategyGoalPdpGoalLink.create(
            userId = userId,
            strategyGoalId = strategyGoalId,
            pdpGoalId = pdpGoalId,
            personId = personId
        )

        val link2 = StrategyGoalPdpGoalLink.create(
            userId = userId,
            strategyGoalId = strategyGoalId,
            pdpGoalId = pdpGoalId,
            personId = personId
        )

        link1.id shouldNotBe link2.id
    }

    @Test
    fun `should preserve all values in link`() {
        val link = StrategyGoalPdpGoalLink.create(
            userId = userId,
            strategyGoalId = strategyGoalId,
            pdpGoalId = pdpGoalId,
            personId = personId
        )

        link.userId.value shouldBe userId.value
        link.strategyGoalId.value shouldBe strategyGoalId.value
        link.pdpGoalId.value shouldBe pdpGoalId.value
        link.personId.value shouldBe personId.value
    }
}
