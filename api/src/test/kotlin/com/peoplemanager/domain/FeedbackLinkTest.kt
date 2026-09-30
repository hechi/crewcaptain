package com.peoplemanager.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class FeedbackLinkTest {

    private val userId = UserId.generate()
    private val personId = PersonId.generate()
    private val question = FeedbackQuestion("q1", FeedbackQuestionType.RATING, "Rate")

    private fun link(
        expiresAt: Instant = Instant.now().plus(14, ChronoUnit.DAYS),
        revokedAt: Instant? = null
    ) = FeedbackLink(
        id = FeedbackLinkId.generate(),
        userId = userId,
        personId = personId,
        token = FeedbackLink.generateToken(),
        title = "Feedback for Alex",
        questions = listOf(question),
        expiresAt = expiresAt,
        revokedAt = revokedAt
    )

    @Test
    fun `generateToken produces a unique url-safe 22-char token`() {
        val a = FeedbackLink.generateToken()
        val b = FeedbackLink.generateToken()
        a shouldNotBe b
        a.length shouldBe 22
        a.all { it.isLetterOrDigit() || it == '-' || it == '_' } shouldBe true
    }

    @Test
    fun `rejects empty questions`() {
        shouldThrow<IllegalArgumentException> {
            FeedbackLink(
                id = FeedbackLinkId.generate(),
                userId = userId,
                personId = personId,
                token = "abc",
                title = "t",
                questions = emptyList(),
                expiresAt = Instant.now().plusSeconds(60)
            )
        }
    }

    @Nested
    inner class Status {
        @Test
        fun `active before expiry`() {
            val l = link()
            l.statusAt(Instant.now()) shouldBe FeedbackLinkStatus.ACTIVE
            l.acceptsSubmissionsAt(Instant.now()) shouldBe true
        }

        @Test
        fun `expired at or after expiry`() {
            val past = Instant.now().minus(1, ChronoUnit.DAYS)
            val l = link(expiresAt = past)
            l.statusAt(Instant.now()) shouldBe FeedbackLinkStatus.EXPIRED
            l.acceptsSubmissionsAt(Instant.now()) shouldBe false
        }

        @Test
        fun `revoked takes precedence over expiry`() {
            val l = link(revokedAt = Instant.now())
            l.statusAt(Instant.now()) shouldBe FeedbackLinkStatus.REVOKED
            l.acceptsSubmissionsAt(Instant.now()) shouldBe false
        }
    }

    @Nested
    inner class Mutations {
        @Test
        fun `revoke sets revokedAt`() {
            val l = link().revoke()
            l.revokedAt shouldNotBe null
        }

        @Test
        fun `cannot revoke twice`() {
            val l = link().revoke()
            shouldThrow<IllegalArgumentException> { l.revoke() }
        }

        @Test
        fun `extend moves expiry later`() {
            val l = link()
            val newExpiry = l.expiresAt.plus(7, ChronoUnit.DAYS)
            l.extend(newExpiry).expiresAt shouldBe newExpiry
        }

        @Test
        fun `extend must be later than current expiry`() {
            val l = link()
            shouldThrow<IllegalArgumentException> {
                l.extend(l.expiresAt.minus(1, ChronoUnit.DAYS))
            }
        }

        @Test
        fun `cannot extend a revoked link`() {
            val l = link().revoke()
            shouldThrow<IllegalArgumentException> {
                l.extend(Instant.now().plus(30, ChronoUnit.DAYS))
            }
        }
    }
}
