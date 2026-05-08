package com.peoplemanager.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class AgendaItemTest {

    @Test
    fun `should create agenda item with valid text`() {
        val item = AgendaItem(
            id = AgendaItemId.generate(),
            text = "Review action items",
            displayOrder = 0
        )
        item.text shouldBe "Review action items"
        item.checked shouldBe false
        item.displayOrder shouldBe 0
    }

    @Test
    fun `should create agenda item with checked true`() {
        val item = AgendaItem(
            id = AgendaItemId.generate(),
            text = "Done item",
            checked = true,
            displayOrder = 1
        )
        item.checked shouldBe true
    }

    @Test
    fun `should reject blank text`() {
        shouldThrow<IllegalArgumentException> {
            AgendaItem(
                id = AgendaItemId.generate(),
                text = "   ",
                displayOrder = 0
            )
        }.message shouldBe "Agenda item text must not be blank"
    }

    @Test
    fun `should reject empty text`() {
        shouldThrow<IllegalArgumentException> {
            AgendaItem(
                id = AgendaItemId.generate(),
                text = "",
                displayOrder = 0
            )
        }.message shouldBe "Agenda item text must not be blank"
    }
}
