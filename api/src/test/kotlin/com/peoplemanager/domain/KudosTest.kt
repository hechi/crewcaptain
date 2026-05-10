package com.peoplemanager.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class KudosTest {

    private val userId = UserId.generate()
    private val personId = PersonId.generate()

    private fun createKudos(
        text: String = "Great job on the presentation!",
        date: LocalDate = LocalDate.of(2026, 5, 10),
        tags: List<String> = listOf("impact", "collaboration")
    ) = Kudos(
        id = KudosId.generate(),
        userId = userId,
        personId = personId,
        date = date,
        text = text,
        tags = tags
    )

    @Nested
    inner class CreationTests {

        @Test
        fun `should create kudos with all fields`() {
            val kudos = createKudos()

            kudos.text shouldBe "Great job on the presentation!"
            kudos.date shouldBe LocalDate.of(2026, 5, 10)
            kudos.tags shouldContainExactly listOf("impact", "collaboration")
            kudos.userId shouldBe userId
            kudos.personId shouldBe personId
            kudos.id shouldNotBe null
            kudos.createdAt shouldNotBe null
            kudos.updatedAt shouldNotBe null
        }

        @Test
        fun `should create kudos with minimal fields`() {
            val kudos = Kudos(
                id = KudosId.generate(),
                userId = userId,
                personId = personId,
                date = LocalDate.of(2026, 5, 10),
                text = "Well done!"
            )

            kudos.text shouldBe "Well done!"
            kudos.tags.shouldBeEmpty()
        }

        @Test
        fun `should create kudos with empty tags list`() {
            val kudos = createKudos(tags = emptyList())

            kudos.tags.shouldBeEmpty()
        }

        @Test
        fun `should create kudos with markdown text`() {
            val markdownText = "## Great work!\n\n- Led the team meeting\n- **Excellent** communication"
            val kudos = createKudos(text = markdownText)

            kudos.text shouldBe markdownText
        }

        @Test
        fun `should reject blank text`() {
            shouldThrow<IllegalArgumentException> {
                createKudos(text = "")
            }.message shouldBe "Kudos text must not be blank"
        }

        @Test
        fun `should reject whitespace-only text`() {
            shouldThrow<IllegalArgumentException> {
                createKudos(text = "   ")
            }.message shouldBe "Kudos text must not be blank"
        }

        @Test
        fun `should preserve date value`() {
            val date = LocalDate.of(2026, 3, 15)
            val kudos = createKudos(date = date)

            kudos.date shouldBe date
        }

        @Test
        fun `should allow multiple tags`() {
            val tags = listOf("leadership", "impact", "collaboration", "innovation")
            val kudos = createKudos(tags = tags)

            kudos.tags shouldContainExactly tags
        }
    }
}
