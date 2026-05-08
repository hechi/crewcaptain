package com.peoplemanager.domain

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant

/**
 * Property-based tests for AgendaItem domain entity.
 *
 * **Validates: Requirements 7.2, 7.5**
 */
@Tag("property")
class AgendaItemPropertyTest {

    /**
     * Property 8: Agenda item text non-blank invariant (whitespace-only rejection)
     *
     * For any string composed entirely of whitespace (including empty string),
     * attempting to construct an AgendaItem with that text SHALL be rejected
     * with an IllegalArgumentException.
     *
     * **Validates: Requirements 7.2, 7.5**
     */
    @Test
    fun `Property 8 - whitespace-only strings should be rejected for agenda item text`() = runBlocking {
        // Generator for whitespace-only strings: combinations of spaces, tabs, newlines, etc.
        val whitespaceChars = listOf(' ', '\t', '\n', '\r', '\u000C', '\u000B')
        val whitespaceOnlyArb: Arb<String> = Arb.int(0..20).map { length ->
            (0 until length).map { whitespaceChars.random() }.joinToString("")
        }

        checkAll(100, whitespaceOnlyArb) { whitespaceText ->
            assertThrows<IllegalArgumentException> {
                AgendaItem(
                    id = AgendaItemId.generate(),
                    text = whitespaceText,
                    checked = false,
                    displayOrder = 0,
                    createdAt = Instant.now()
                )
            }.message shouldBe "Agenda item text must not be blank"
        }
        Unit
    }

    /**
     * Property 8 (positive case): Non-blank strings should be accepted for agenda item text
     *
     * For any non-blank string, constructing an AgendaItem with that text
     * should succeed without throwing.
     *
     * **Validates: Requirements 7.2, 7.5**
     */
    @Test
    fun `Property 8 - non-blank strings should be accepted for agenda item text`() = runBlocking {
        val nonBlankStringArb: Arb<String> = Arb.string(1..200).filter { it.isNotBlank() }

        checkAll(100, nonBlankStringArb) { validText ->
            val agendaItem = AgendaItem(
                id = AgendaItemId.generate(),
                text = validText,
                checked = false,
                displayOrder = 0,
                createdAt = Instant.now()
            )
            agendaItem.text shouldBe validText
            agendaItem shouldNotBe null
        }
        Unit
    }
}
