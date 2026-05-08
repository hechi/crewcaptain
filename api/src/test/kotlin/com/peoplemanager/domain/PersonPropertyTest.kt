package com.peoplemanager.domain

import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Property-based tests for Person domain aggregate.
 *
 * **Validates: Requirements 2.2, 2.5, 4.3, 4.5**
 */
@Tag("property")
class PersonPropertyTest {

    /**
     * Property 3: Blank name rejection
     *
     * For any string composed entirely of whitespace (including empty string),
     * attempting to create a Person with that string as the name SHALL be rejected
     * with an IllegalArgumentException.
     *
     * **Validates: Requirements 2.2, 2.5, 4.3, 4.5**
     */
    @Test
    fun `Property 3 - blank name rejection - whitespace-only strings should be rejected`() = runBlocking {
        // Generator for whitespace-only strings: combinations of space, tab, newline, carriage return, form feed
        val whitespaceChars = listOf(' ', '\t', '\n', '\r', '\u000C', '\u000B', '\u00A0')
        val whitespaceStringArb: Arb<String> = arbitrary {
            val length = Arb.int(0..100).bind()
            buildString {
                repeat(length) {
                    append(Arb.of(whitespaceChars).bind())
                }
            }
        }

        checkAll(100, whitespaceStringArb) { blankName ->
            assertThrows<IllegalArgumentException> {
                Person(
                    id = PersonId.generate(),
                    userId = UserId.generate(),
                    name = blankName
                )
            }
        }
        Unit
    }

    /**
     * Property 11: Remember item addition preserves order and content
     *
     * For any Person and any sequence of non-blank text strings added as remember items,
     * the resulting pinned remember items list SHALL contain all added items in insertion order,
     * with each item's text matching the input exactly.
     *
     * **Validates: Requirements 9.1, 9.4**
     */
    @Test
    fun `Property 11 - remember item addition preserves order and content`() = runBlocking {
        val nonBlankStringArb: Arb<String> = arbitrary {
            var s = Arb.string(1..50).bind()
            while (s.isBlank()) {
                s = Arb.string(1..50).bind()
            }
            s
        }

        val textListArb: Arb<List<String>> = Arb.list(nonBlankStringArb, 1..20)

        checkAll(100, textListArb) { texts ->
            var person = Person(
                id = PersonId.generate(),
                userId = UserId.generate(),
                name = "Test Person"
            )

            for (text in texts) {
                person = person.addRememberItem(text)
            }

            // Verify count matches
            person.pinnedRememberItems.size shouldBe texts.size

            // Verify order and content
            person.pinnedRememberItems.forEachIndexed { index, item ->
                item.text shouldBe texts[index]
                item.displayOrder shouldBe index
            }
        }
        Unit
    }

    /**
     * Property 12: Remember item removal
     *
     * For any Person with N pinned remember items and any valid item ID from that list,
     * removing the item SHALL result in a list of N-1 items where the removed item is
     * no longer present and all other items retain their relative order.
     *
     * **Validates: Requirements 9.2**
     */
    @Test
    fun `Property 12 - remember item removal preserves relative order of remaining items`() = runBlocking {
        val nonBlankStringArb: Arb<String> = arbitrary {
            var s = Arb.string(1..50).bind()
            while (s.isBlank()) {
                s = Arb.string(1..50).bind()
            }
            s
        }

        val textListArb: Arb<List<String>> = Arb.list(nonBlankStringArb, 2..15)

        checkAll(100, textListArb) { texts ->
            // Build a person with N remember items
            var person = Person(
                id = PersonId.generate(),
                userId = UserId.generate(),
                name = "Test Person"
            )
            for (text in texts) {
                person = person.addRememberItem(text)
            }

            val n = person.pinnedRememberItems.size
            // Pick a random index to remove
            val removeIndex = Arb.int(0 until n).bind()
            val itemToRemove = person.pinnedRememberItems[removeIndex]

            // Remove the item
            val updatedPerson = person.removeRememberItem(itemToRemove.id)

            // Verify N-1 items remain
            updatedPerson.pinnedRememberItems.size shouldBe (n - 1)

            // Verify removed item is no longer present
            updatedPerson.pinnedRememberItems.none { it.id == itemToRemove.id } shouldBe true

            // Verify relative order of remaining items is preserved
            val expectedTexts = texts.filterIndexed { index, _ -> index != removeIndex }
            updatedPerson.pinnedRememberItems.map { it.text } shouldBe expectedTexts

            // Verify displayOrder is re-indexed correctly
            updatedPerson.pinnedRememberItems.forEachIndexed { index, item ->
                item.displayOrder shouldBe index
            }
        }
        Unit
    }

    /**
     * Property 13: Remember item reorder is a permutation
     *
     * For any Person with pinned remember items and any valid permutation of their IDs,
     * reordering SHALL result in the items appearing in the specified order with no items
     * added or removed (the set of items is unchanged, only displayOrder changes).
     *
     * **Validates: Requirements 9.3**
     */
    @Test
    fun `Property 13 - remember item reorder is a permutation with no additions or removals`() = runBlocking {
        val nonBlankStringArb: Arb<String> = arbitrary {
            var s = Arb.string(1..50).bind()
            while (s.isBlank()) {
                s = Arb.string(1..50).bind()
            }
            s
        }

        val textListArb: Arb<List<String>> = Arb.list(nonBlankStringArb, 2..15)

        checkAll(100, textListArb) { texts ->
            // Build a person with remember items
            var person = Person(
                id = PersonId.generate(),
                userId = UserId.generate(),
                name = "Test Person"
            )
            for (text in texts) {
                person = person.addRememberItem(text)
            }

            val originalItems = person.pinnedRememberItems
            // Create a random permutation of the IDs
            val shuffledIds = originalItems.map { it.id }.shuffled()

            // Reorder
            val reorderedPerson = person.reorderRememberItems(shuffledIds)

            // Verify same number of items (no additions or removals)
            reorderedPerson.pinnedRememberItems.size shouldBe originalItems.size

            // Verify items appear in the new order specified by shuffledIds
            reorderedPerson.pinnedRememberItems.forEachIndexed { index, item ->
                item.id shouldBe shuffledIds[index]
                item.displayOrder shouldBe index
            }

            // Verify no items were added or removed (same set of IDs)
            val originalIdSet = originalItems.map { it.id }.toSet()
            val reorderedIdSet = reorderedPerson.pinnedRememberItems.map { it.id }.toSet()
            reorderedIdSet shouldBe originalIdSet

            // Verify text content is preserved (no data corruption)
            val originalTextById = originalItems.associate { it.id to it.text }
            reorderedPerson.pinnedRememberItems.forEach { item ->
                item.text shouldBe originalTextById[item.id]
            }
        }
        Unit
    }
}
