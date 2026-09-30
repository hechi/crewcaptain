package com.peoplemanager.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class FeedbackTemplateTest {

    private val userId = UserId.generate()

    private fun rating(id: String, showIf: ShowIfRule? = null) =
        FeedbackQuestion(id = id, type = FeedbackQuestionType.RATING, text = "Rate $id", showIf = showIf)

    private fun text(id: String, showIf: ShowIfRule? = null) =
        FeedbackQuestion(id = id, type = FeedbackQuestionType.TEXT, text = "Explain $id", showIf = showIf)

    private fun template(questions: List<FeedbackQuestion>) = FeedbackTemplate(
        id = FeedbackTemplateId.generate(),
        userId = userId,
        title = "Mid-year review",
        questions = questions
    )

    @Nested
    inner class QuestionValidation {
        @Test
        fun `rejects blank question text`() {
            shouldThrow<IllegalArgumentException> {
                FeedbackQuestion(id = "q1", type = FeedbackQuestionType.TEXT, text = "")
            }
        }

        @Test
        fun `rejects self-referencing showIf`() {
            shouldThrow<IllegalArgumentException> {
                FeedbackQuestion(
                    id = "q1",
                    type = FeedbackQuestionType.TEXT,
                    text = "Follow up",
                    showIf = ShowIfRule("q1", ShowIfOperator.LTE, 3)
                )
            }
        }

        @Test
        fun `rejects showIf value out of range`() {
            shouldThrow<IllegalArgumentException> {
                ShowIfRule("q1", ShowIfOperator.LTE, 6)
            }
        }
    }

    @Nested
    inner class TemplateValidation {
        @Test
        fun `accepts a valid template`() {
            val t = template(listOf(rating("q1"), text("q2")))
            t.questions.size shouldBe 2
        }

        @Test
        fun `rejects blank title`() {
            shouldThrow<IllegalArgumentException> {
                FeedbackTemplate(FeedbackTemplateId.generate(), userId, title = "", questions = listOf(rating("q1")))
            }
        }

        @Test
        fun `rejects duplicate question ids`() {
            shouldThrow<IllegalArgumentException> {
                template(listOf(rating("q1"), text("q1")))
            }
        }

        @Test
        fun `rejects more than max questions`() {
            val many = (1..FeedbackTemplate.MAX_QUESTIONS + 1).map { text("q$it") }
            shouldThrow<IllegalArgumentException> { template(many) }
        }

        @Test
        fun `allows a follow-up text question conditioned on an earlier rating`() {
            val t = template(
                listOf(
                    rating("q1"),
                    text("q1b", showIf = ShowIfRule("q1", ShowIfOperator.LTE, 3))
                )
            )
            t.questions[1].showIf?.questionId shouldBe "q1"
        }

        @Test
        fun `rejects showIf referencing an unknown question`() {
            shouldThrow<IllegalArgumentException> {
                template(listOf(text("q2", showIf = ShowIfRule("nope", ShowIfOperator.LTE, 3))))
            }
        }

        @Test
        fun `rejects showIf referencing a later question`() {
            shouldThrow<IllegalArgumentException> {
                template(
                    listOf(
                        text("q1", showIf = ShowIfRule("q2", ShowIfOperator.LTE, 3)),
                        rating("q2")
                    )
                )
            }
        }

        @Test
        fun `rejects showIf referencing a text question`() {
            shouldThrow<IllegalArgumentException> {
                template(
                    listOf(
                        text("q1"),
                        text("q2", showIf = ShowIfRule("q1", ShowIfOperator.LTE, 3))
                    )
                )
            }
        }

        @Test
        fun `rejects chained branching (showIf referencing a conditional question)`() {
            shouldThrow<IllegalArgumentException> {
                template(
                    listOf(
                        rating("q1"),
                        rating("q2", showIf = ShowIfRule("q1", ShowIfOperator.LTE, 3)),
                        text("q3", showIf = ShowIfRule("q2", ShowIfOperator.LTE, 3))
                    )
                )
            }
        }

        @Test
        fun `update replaces content and bumps updatedAt`() {
            val t = template(listOf(rating("q1")))
            val updated = t.update("New title", "desc", listOf(text("q1")))
            updated.title shouldBe "New title"
            updated.description shouldBe "desc"
            updated.questions[0].type shouldBe FeedbackQuestionType.TEXT
        }
    }
}
