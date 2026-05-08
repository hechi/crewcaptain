package com.peoplemanager.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import java.time.Instant

class OneOnOneEntryTest {

    private fun createValidEntry(
        agendaItems: List<AgendaItem> = emptyList(),
        notesMarkdown: String? = null,
        outcomesMarkdown: String? = null,
        sensitive: Boolean = false
    ) = OneOnOneEntry(
        id = OneOnOneEntryId.generate(),
        userId = UserId.generate(),
        personId = PersonId.generate(),
        meetingDate = Instant.now(),
        agendaItems = agendaItems,
        notesMarkdown = notesMarkdown,
        outcomesMarkdown = outcomesMarkdown,
        sensitive = sensitive
    )

    @Test
    fun `should create entry with valid data`() {
        val entry = createValidEntry(
            notesMarkdown = "Some notes",
            outcomesMarkdown = "Some outcomes"
        )
        entry.notesMarkdown shouldBe "Some notes"
        entry.outcomesMarkdown shouldBe "Some outcomes"
        entry.sensitive shouldBe false
    }

    @Test
    fun `should create entry with agenda items`() {
        val items = listOf(
            AgendaItem(id = AgendaItemId.generate(), text = "Item 1", displayOrder = 0),
            AgendaItem(id = AgendaItemId.generate(), text = "Item 2", displayOrder = 1)
        )
        val entry = createValidEntry(agendaItems = items)
        entry.agendaItems.size shouldBe 2
        entry.agendaItems[0].text shouldBe "Item 1"
        entry.agendaItems[1].text shouldBe "Item 2"
    }

    @Test
    fun `should reject entry with blank agenda item text`() {
        shouldThrow<IllegalArgumentException> {
            createValidEntry(
                agendaItems = listOf(
                    AgendaItem(id = AgendaItemId.generate(), text = "   ", displayOrder = 0)
                )
            )
        }
    }

    @Test
    fun `should reject entry with empty agenda item text`() {
        shouldThrow<IllegalArgumentException> {
            createValidEntry(
                agendaItems = listOf(
                    AgendaItem(id = AgendaItemId.generate(), text = "", displayOrder = 0)
                )
            )
        }
    }

    @Test
    fun `should update notes`() {
        val entry = createValidEntry(notesMarkdown = "Original")
        val updated = entry.updateNotes("Updated notes")
        updated.notesMarkdown shouldBe "Updated notes"
        updated.updatedAt shouldNotBe entry.updatedAt
    }

    @Test
    fun `should update notes to null`() {
        val entry = createValidEntry(notesMarkdown = "Original")
        val updated = entry.updateNotes(null)
        updated.notesMarkdown shouldBe null
    }

    @Test
    fun `should update outcomes`() {
        val entry = createValidEntry(outcomesMarkdown = "Original")
        val updated = entry.updateOutcomes("New outcomes")
        updated.outcomesMarkdown shouldBe "New outcomes"
        updated.updatedAt shouldNotBe entry.updatedAt
    }

    @Test
    fun `should toggle sensitive flag from false to true`() {
        val entry = createValidEntry(sensitive = false)
        val toggled = entry.toggleSensitive()
        toggled.sensitive shouldBe true
    }

    @Test
    fun `should toggle sensitive flag from true to false`() {
        val entry = createValidEntry(sensitive = true)
        val toggled = entry.toggleSensitive()
        toggled.sensitive shouldBe false
    }

    @Test
    fun `should update agenda items`() {
        val entry = createValidEntry(
            agendaItems = listOf(
                AgendaItem(id = AgendaItemId.generate(), text = "Old item", displayOrder = 0)
            )
        )
        val newItems = listOf(
            AgendaItem(id = AgendaItemId.generate(), text = "New item 1", displayOrder = 0),
            AgendaItem(id = AgendaItemId.generate(), text = "New item 2", displayOrder = 1)
        )
        val updated = entry.updateAgendaItems(newItems)
        updated.agendaItems.size shouldBe 2
        updated.agendaItems[0].text shouldBe "New item 1"
        updated.agendaItems[1].text shouldBe "New item 2"
    }

    @Test
    fun `should create entry with sensitive flag true`() {
        val entry = createValidEntry(sensitive = true)
        entry.sensitive shouldBe true
    }
}
