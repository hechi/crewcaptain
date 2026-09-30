package com.peoplemanager.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FeedbackSummaryTest {

    private fun summary(
        content: String = "Alex is a strong collaborator.",
        from: LocalDate = LocalDate.of(2026, 1, 1),
        to: LocalDate = LocalDate.of(2026, 6, 30),
        count: Int = 3
    ) = FeedbackSummary(
        id = FeedbackSummaryId.generate(),
        userId = UserId.generate(),
        personId = PersonId.generate(),
        periodFrom = from,
        periodTo = to,
        content = content,
        responseCount = count
    )

    @Test
    fun `creates a valid summary`() {
        summary().responseCount shouldBe 3
    }

    @Test
    fun `rejects blank content`() {
        shouldThrow<IllegalArgumentException> { summary(content = "") }
    }

    @Test
    fun `rejects periodTo before periodFrom`() {
        shouldThrow<IllegalArgumentException> {
            summary(from = LocalDate.of(2026, 6, 30), to = LocalDate.of(2026, 1, 1))
        }
    }

    @Test
    fun `rejects negative response count`() {
        shouldThrow<IllegalArgumentException> { summary(count = -1) }
    }

    @Test
    fun `editContent updates and rejects blank`() {
        val edited = summary().editContent("Updated narrative")
        edited.content shouldBe "Updated narrative"
        shouldThrow<IllegalArgumentException> { summary().editContent("  ") }
    }
}
