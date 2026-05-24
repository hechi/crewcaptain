package com.peoplemanager.domain

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant

class WorkspaceTest {

    private fun createValidWorkspace(
        name: String = "My Team",
        description: String? = "Direct reports",
        displayOrder: Int = 0
    ): Workspace = Workspace(
        id = WorkspaceId.generate(),
        userId = UserId.generate(),
        name = name,
        description = description,
        displayOrder = displayOrder,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )

    // --- Name invariant tests ---

    @Test
    fun `should create Workspace with valid name`() {
        val workspace = createValidWorkspace(name = "My Team")
        workspace.name shouldBe "My Team"
        workspace.description shouldBe "Direct reports"
        workspace.displayOrder shouldBe 0
    }

    @Test
    fun `should reject blank name`() {
        val exception = assertThrows<IllegalArgumentException> {
            createValidWorkspace(name = "")
        }
        exception.message shouldBe "Workspace name must not be blank"
    }

    @Test
    fun `should reject whitespace-only name`() {
        val exception = assertThrows<IllegalArgumentException> {
            createValidWorkspace(name = "   ")
        }
        exception.message shouldBe "Workspace name must not be blank"
    }

    @Test
    fun `should reject name exceeding 100 characters`() {
        val longName = "a".repeat(101)
        val exception = assertThrows<IllegalArgumentException> {
            createValidWorkspace(name = longName)
        }
        exception.message shouldBe "Workspace name must not exceed 100 characters"
    }

    @Test
    fun `should accept name with exactly 100 characters`() {
        val name = "a".repeat(100)
        val workspace = createValidWorkspace(name = name)
        workspace.name shouldBe name
    }

    // --- Description invariant tests ---

    @Test
    fun `should accept null description`() {
        val workspace = createValidWorkspace(description = null)
        workspace.description shouldBe null
    }

    @Test
    fun `should reject description exceeding 500 characters`() {
        val longDescription = "a".repeat(501)
        val exception = assertThrows<IllegalArgumentException> {
            createValidWorkspace(description = longDescription)
        }
        exception.message shouldBe "Workspace description must not exceed 500 characters"
    }

    @Test
    fun `should accept description with exactly 500 characters`() {
        val description = "a".repeat(500)
        val workspace = createValidWorkspace(description = description)
        workspace.description shouldBe description
    }

    // --- updateDetails tests ---

    @Test
    fun `should update name`() {
        val workspace = createValidWorkspace(name = "Old Name")
        val updated = workspace.updateDetails(name = "New Name")
        updated.name shouldBe "New Name"
        updated.description shouldBe workspace.description
        updated.updatedAt shouldNotBe workspace.updatedAt
    }

    @Test
    fun `should update description`() {
        val workspace = createValidWorkspace(description = "Old desc")
        val updated = workspace.updateDetails(description = "New desc")
        updated.description shouldBe "New desc"
        updated.name shouldBe workspace.name
    }

    @Test
    fun `should update both name and description`() {
        val workspace = createValidWorkspace()
        val updated = workspace.updateDetails(name = "New Name", description = "New desc")
        updated.name shouldBe "New Name"
        updated.description shouldBe "New desc"
    }

    @Test
    fun `should keep existing values when nulls passed to updateDetails`() {
        val workspace = createValidWorkspace(name = "Keep Me", description = "Keep Me Too")
        val updated = workspace.updateDetails(name = null, description = null)
        updated.name shouldBe "Keep Me"
        updated.description shouldBe "Keep Me Too"
    }

    @Test
    fun `should reject blank name in updateDetails`() {
        val workspace = createValidWorkspace()
        assertThrows<IllegalArgumentException> {
            workspace.updateDetails(name = "")
        }
    }

    @Test
    fun `should reject name exceeding 100 chars in updateDetails`() {
        val workspace = createValidWorkspace()
        assertThrows<IllegalArgumentException> {
            workspace.updateDetails(name = "a".repeat(101))
        }
    }

    @Test
    fun `should reject description exceeding 500 chars in updateDetails`() {
        val workspace = createValidWorkspace()
        assertThrows<IllegalArgumentException> {
            workspace.updateDetails(description = "a".repeat(501))
        }
    }

    // --- reorder tests ---

    @Test
    fun `should reorder workspace`() {
        val workspace = createValidWorkspace(displayOrder = 0)
        val reordered = workspace.reorder(3)
        reordered.displayOrder shouldBe 3
        reordered.updatedAt shouldNotBe workspace.updatedAt
    }

    @Test
    fun `should reject negative display order`() {
        val workspace = createValidWorkspace()
        assertThrows<IllegalArgumentException> {
            workspace.reorder(-1)
        }
    }

    @Test
    fun `should accept zero display order`() {
        val workspace = createValidWorkspace(displayOrder = 5)
        val reordered = workspace.reorder(0)
        reordered.displayOrder shouldBe 0
    }
}
