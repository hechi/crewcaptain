package com.peoplemanager.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class FeedbackResponseTest {

    private val userId = UserId.generate()
    private val personId = PersonId.generate()
    private val linkId = FeedbackLinkId.generate()

    private fun response(
        anonymous: Boolean = true,
        submitterName: String? = null,
        submitterEmail: String? = null,
        answers: List<FeedbackAnswer> = emptyList(),
        additionalComments: String? = null
    ) = FeedbackResponse(
        id = FeedbackResponseId.generate(),
        userId = userId,
        personId = personId,
        linkId = linkId,
        anonymous = anonymous,
        submitterName = submitterName,
        submitterEmail = submitterEmail,
        answers = answers,
        additionalComments = additionalComments
    )

    @Nested
    inner class Validation {
        @Test
        fun `anonymous response cannot carry name or email`() {
            shouldThrow<IllegalArgumentException> {
                response(anonymous = true, submitterName = "Sam")
            }
        }

        @Test
        fun `named response is allowed`() {
            val r = response(anonymous = false, submitterName = "Sam", submitterEmail = "s@x.io")
            r.submitterName shouldBe "Sam"
        }

        @Test
        fun `rating answer must be 1 to 5`() {
            shouldThrow<IllegalArgumentException> {
                FeedbackAnswer(questionId = "q1", ratingValue = 6)
            }
        }
    }

    @Nested
    inner class Lifecycle {
        @Test
        fun `pending and unflagged is not usable`() {
            response().isUsable shouldBe false
        }

        @Test
        fun `approved and unflagged is usable`() {
            response().approve().isUsable shouldBe true
        }

        @Test
        fun `approved but flagged is not usable`() {
            response().approve().setFlagged(true).isUsable shouldBe false
        }

        @Test
        fun `pin and convert transitions`() {
            val r = response().approve().setPinned(true).markConverted(FeedbackConversionType.KUDO, "abc")
            r.pinned shouldBe true
            r.convertedToType shouldBe FeedbackConversionType.KUDO
            r.convertedToId shouldBe "abc"
        }
    }

    @Nested
    inner class Extraction {
        @Test
        fun `freeTextParts collects text answers and comments, dropping blanks`() {
            val r = response(
                answers = listOf(
                    FeedbackAnswer("q1", ratingValue = 4),
                    FeedbackAnswer("q2", textValue = "Clear communicator"),
                    FeedbackAnswer("q3", textValue = "  ")
                ),
                additionalComments = "Keep it up"
            )
            r.freeTextParts() shouldContainExactly listOf("Clear communicator", "Keep it up")
        }

        @Test
        fun `ratingValues collects numeric answers`() {
            val r = response(
                answers = listOf(
                    FeedbackAnswer("q1", ratingValue = 4),
                    FeedbackAnswer("q2", ratingValue = 2),
                    FeedbackAnswer("q3", textValue = "text")
                )
            )
            r.ratingValues() shouldContainExactly listOf(4, 2)
        }
    }
}
