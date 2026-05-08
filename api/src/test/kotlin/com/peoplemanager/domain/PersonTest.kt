package com.peoplemanager.domain

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.time.LocalDate

class PersonTest {

    private fun createValidPerson(
        name: String = "Jane Smith",
        moraleStatus: MoraleStatus = MoraleStatus.UNKNOWN,
        moraleNote: String? = null,
        pinnedRememberItems: List<PinnedRememberItem> = emptyList()
    ): Person = Person(
        id = PersonId.generate(),
        userId = UserId.generate(),
        name = name,
        preferredName = "Jane",
        roleTitle = "Senior Engineer",
        timezone = "Europe/Berlin",
        startDate = LocalDate.of(2024, 3, 15),
        email = "jane@example.com",
        tags = listOf("engineering", "senior"),
        moraleStatus = moraleStatus,
        moraleNote = moraleNote,
        pinnedRememberItems = pinnedRememberItems,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )

    // --- Name invariant tests (Requirement 2.2) ---

    @Test
    fun `should create Person with valid name`() {
        val person = createValidPerson(name = "Alice Johnson")

        person.name shouldBe "Alice Johnson"
        person.preferredName shouldBe "Jane"
        person.roleTitle shouldBe "Senior Engineer"
        person.moraleStatus shouldBe MoraleStatus.UNKNOWN
        person.pinnedRememberItems.shouldBeEmpty()
    }

    @Test
    fun `should reject blank name with IllegalArgumentException`() {
        val exception = assertThrows<IllegalArgumentException> {
            createValidPerson(name = "")
        }
        exception.message shouldBe "Person name must not be blank"
    }

    @Test
    fun `should reject whitespace-only name with IllegalArgumentException`() {
        val exception = assertThrows<IllegalArgumentException> {
            createValidPerson(name = "   ")
        }
        exception.message shouldBe "Person name must not be blank"
    }

    @Test
    fun `should reject tab-only name`() {
        assertThrows<IllegalArgumentException> {
            createValidPerson(name = "\t\t")
        }
    }

    @Test
    fun `should reject newline-only name`() {
        assertThrows<IllegalArgumentException> {
            createValidPerson(name = "\n\n")
        }
    }

    // --- Morale update tests (Requirement 8.1) ---

    @Test
    fun `updateMorale should return new Person with updated moraleStatus and moraleNote`() {
        val person = createValidPerson(
            moraleStatus = MoraleStatus.UNKNOWN,
            moraleNote = null
        )

        val updated = person.updateMorale(MoraleStatus.GREEN, "Had a great sprint review")

        updated.moraleStatus shouldBe MoraleStatus.GREEN
        updated.moraleNote shouldBe "Had a great sprint review"
    }

    @Test
    fun `updateMorale should preserve all other fields`() {
        val person = createValidPerson(
            moraleStatus = MoraleStatus.UNKNOWN,
            moraleNote = null
        )

        val updated = person.updateMorale(MoraleStatus.RED, "Struggling with workload")

        updated.id shouldBe person.id
        updated.userId shouldBe person.userId
        updated.name shouldBe person.name
        updated.preferredName shouldBe person.preferredName
        updated.roleTitle shouldBe person.roleTitle
        updated.timezone shouldBe person.timezone
        updated.startDate shouldBe person.startDate
        updated.email shouldBe person.email
        updated.tags shouldBe person.tags
        updated.pinnedRememberItems shouldBe person.pinnedRememberItems
        updated.createdAt shouldBe person.createdAt
        updated.updatedAt shouldNotBe person.updatedAt
    }

    @Test
    fun `updateMorale should allow null note`() {
        val person = createValidPerson(
            moraleStatus = MoraleStatus.GREEN,
            moraleNote = "Previous note"
        )

        val updated = person.updateMorale(MoraleStatus.YELLOW, null)

        updated.moraleStatus shouldBe MoraleStatus.YELLOW
        updated.moraleNote shouldBe null
    }

    // --- addRememberItem tests (Requirement 9.1) ---

    @Test
    fun `addRememberItem should append item to end of list`() {
        val person = createValidPerson()

        val updated = person.addRememberItem("Prefers async communication")

        updated.pinnedRememberItems shouldHaveSize 1
        updated.pinnedRememberItems[0].text shouldBe "Prefers async communication"
    }

    @Test
    fun `addRememberItem should set correct displayOrder starting at 0`() {
        val person = createValidPerson()

        val updated = person.addRememberItem("First item")

        updated.pinnedRememberItems[0].displayOrder shouldBe 0
    }

    @Test
    fun `addRememberItem should set sequential displayOrder for multiple items`() {
        val person = createValidPerson()

        val updated = person
            .addRememberItem("First item")
            .addRememberItem("Second item")
            .addRememberItem("Third item")

        updated.pinnedRememberItems shouldHaveSize 3
        updated.pinnedRememberItems[0].displayOrder shouldBe 0
        updated.pinnedRememberItems[1].displayOrder shouldBe 1
        updated.pinnedRememberItems[2].displayOrder shouldBe 2
    }

    @Test
    fun `multiple addRememberItem calls should maintain insertion order`() {
        val person = createValidPerson()

        val updated = person
            .addRememberItem("Alpha")
            .addRememberItem("Beta")
            .addRememberItem("Gamma")

        updated.pinnedRememberItems.map { it.text } shouldContainExactly listOf("Alpha", "Beta", "Gamma")
    }

    @Test
    fun `addRememberItem should generate unique IDs for each item`() {
        val person = createValidPerson()

        val updated = person
            .addRememberItem("Item A")
            .addRememberItem("Item B")

        val ids = updated.pinnedRememberItems.map { it.id }
        ids[0] shouldNotBe ids[1]
    }

    // --- removeRememberItem tests (Requirement 9.2) ---

    @Test
    fun `removeRememberItem should remove the specified item`() {
        val person = createValidPerson()
            .addRememberItem("Keep this")
            .addRememberItem("Remove this")
            .addRememberItem("Keep this too")

        val itemToRemove = person.pinnedRememberItems[1].id
        val updated = person.removeRememberItem(itemToRemove)

        updated.pinnedRememberItems shouldHaveSize 2
        updated.pinnedRememberItems.map { it.text } shouldContainExactly listOf("Keep this", "Keep this too")
    }

    @Test
    fun `removeRememberItem should preserve relative order of remaining items`() {
        val person = createValidPerson()
            .addRememberItem("First")
            .addRememberItem("Second")
            .addRememberItem("Third")
            .addRememberItem("Fourth")

        val itemToRemove = person.pinnedRememberItems[1].id // Remove "Second"
        val updated = person.removeRememberItem(itemToRemove)

        updated.pinnedRememberItems.map { it.text } shouldContainExactly listOf("First", "Third", "Fourth")
    }

    @Test
    fun `removeRememberItem should re-index displayOrder after removal`() {
        val person = createValidPerson()
            .addRememberItem("First")
            .addRememberItem("Second")
            .addRememberItem("Third")

        val itemToRemove = person.pinnedRememberItems[0].id // Remove "First"
        val updated = person.removeRememberItem(itemToRemove)

        updated.pinnedRememberItems[0].displayOrder shouldBe 0
        updated.pinnedRememberItems[1].displayOrder shouldBe 1
        updated.pinnedRememberItems[0].text shouldBe "Second"
        updated.pinnedRememberItems[1].text shouldBe "Third"
    }

    @Test
    fun `removeRememberItem with non-existent ID should return person with same items`() {
        val person = createValidPerson()
            .addRememberItem("Only item")

        val nonExistentId = RememberItemId.generate()
        val updated = person.removeRememberItem(nonExistentId)

        updated.pinnedRememberItems shouldHaveSize 1
        updated.pinnedRememberItems[0].text shouldBe "Only item"
    }

    // --- reorderRememberItems tests (Requirement 9.3) ---

    @Test
    fun `reorderRememberItems should produce items in specified order`() {
        val person = createValidPerson()
            .addRememberItem("Alpha")
            .addRememberItem("Beta")
            .addRememberItem("Gamma")

        val ids = person.pinnedRememberItems.map { it.id }
        // Reverse the order
        val newOrder = listOf(ids[2], ids[0], ids[1])

        val updated = person.reorderRememberItems(newOrder)

        updated.pinnedRememberItems.map { it.text } shouldContainExactly listOf("Gamma", "Alpha", "Beta")
        updated.pinnedRememberItems[0].displayOrder shouldBe 0
        updated.pinnedRememberItems[1].displayOrder shouldBe 1
        updated.pinnedRememberItems[2].displayOrder shouldBe 2
    }

    @Test
    fun `reorderRememberItems should not add or remove items`() {
        val person = createValidPerson()
            .addRememberItem("One")
            .addRememberItem("Two")
            .addRememberItem("Three")

        val ids = person.pinnedRememberItems.map { it.id }
        val newOrder = listOf(ids[1], ids[2], ids[0])

        val updated = person.reorderRememberItems(newOrder)

        updated.pinnedRememberItems shouldHaveSize 3
        val updatedIds = updated.pinnedRememberItems.map { it.id }.toSet()
        updatedIds shouldBe ids.toSet()
    }

    @Test
    fun `reorderRememberItems should throw for invalid IDs`() {
        val person = createValidPerson()
            .addRememberItem("One")
            .addRememberItem("Two")

        val invalidId = RememberItemId.generate()
        val ids = person.pinnedRememberItems.map { it.id }

        assertThrows<IllegalArgumentException> {
            person.reorderRememberItems(listOf(ids[0], invalidId))
        }
    }

    @Test
    fun `reorderRememberItems should preserve item text after reorder`() {
        val person = createValidPerson()
            .addRememberItem("First")
            .addRememberItem("Second")

        val ids = person.pinnedRememberItems.map { it.id }
        val updated = person.reorderRememberItems(listOf(ids[1], ids[0]))

        updated.pinnedRememberItems[0].text shouldBe "Second"
        updated.pinnedRememberItems[1].text shouldBe "First"
    }
}
