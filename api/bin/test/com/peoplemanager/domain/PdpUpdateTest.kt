package com.peoplemanager.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class PdpUpdateTest {

    private val userId = UserId.generate()
    private val goalId = PdpGoalId.generate()

    @Test
    fun `should create update with valid fields`() {
        val update = PdpUpdate(
            id = PdpUpdateId.generate(),
            goalId = goalId,
            userId = userId,
            textMarkdown = "Completed first presentation"
        )

        update.textMarkdown shouldBe "Completed first presentation"
        update.sensitive shouldBe false
        update.goalId shouldBe goalId
        update.userId shouldBe userId
    }

    @Test
    fun `should create sensitive update`() {
        val update = PdpUpdate(
            id = PdpUpdateId.generate(),
            goalId = goalId,
            userId = userId,
            textMarkdown = "Private progress note",
            sensitive = true
        )

        update.sensitive shouldBe true
    }

    @Test
    fun `should reject blank text`() {
        shouldThrow<IllegalArgumentException> {
            PdpUpdate(
                id = PdpUpdateId.generate(),
                goalId = goalId,
                userId = userId,
                textMarkdown = ""
            )
        }.message shouldBe "PDP update text must not be blank"
    }

    @Test
    fun `should reject whitespace-only text`() {
        shouldThrow<IllegalArgumentException> {
            PdpUpdate(
                id = PdpUpdateId.generate(),
                goalId = goalId,
                userId = userId,
                textMarkdown = "   "
            )
        }.message shouldBe "PDP update text must not be blank"
    }
}
