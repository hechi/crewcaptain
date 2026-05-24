package com.peoplemanager.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant

class AuditLogEntryTest {

    private val userId = UserId.generate()
    private val personId = PersonId.generate()

    @Nested
    inner class ValidationTests {

        @Test
        fun `should create a valid audit log entry`() {
            val entry = AuditLogEntry(
                userId = userId,
                action = AuditAction.CREATE,
                entityType = AuditEntityType.PERSON,
                entityId = "some-entity-id",
                personId = personId,
                summary = "Created person"
            )

            entry.userId shouldBe userId
            entry.action shouldBe AuditAction.CREATE
            entry.entityType shouldBe AuditEntityType.PERSON
            entry.entityId shouldBe "some-entity-id"
            entry.personId shouldBe personId
            entry.summary shouldBe "Created person"
            entry.id shouldNotBe null
            entry.createdAt shouldNotBe null
        }

        @Test
        fun `should reject blank entity ID`() {
            shouldThrow<IllegalArgumentException> {
                AuditLogEntry(
                    userId = userId,
                    action = AuditAction.CREATE,
                    entityType = AuditEntityType.PERSON,
                    entityId = "",
                    summary = "Created person"
                )
            }.message shouldBe "Entity ID must not be blank"
        }

        @Test
        fun `should reject blank summary`() {
            shouldThrow<IllegalArgumentException> {
                AuditLogEntry(
                    userId = userId,
                    action = AuditAction.CREATE,
                    entityType = AuditEntityType.PERSON,
                    entityId = "some-id",
                    summary = ""
                )
            }.message shouldBe "Audit log summary must not be blank"
        }

        @Test
        fun `should reject summary exceeding 500 characters`() {
            shouldThrow<IllegalArgumentException> {
                AuditLogEntry(
                    userId = userId,
                    action = AuditAction.CREATE,
                    entityType = AuditEntityType.PERSON,
                    entityId = "some-id",
                    summary = "a".repeat(501)
                )
            }.message shouldBe "Audit log summary must not exceed 500 characters"
        }

        @Test
        fun `should allow summary of exactly 500 characters`() {
            val entry = AuditLogEntry(
                userId = userId,
                action = AuditAction.CREATE,
                entityType = AuditEntityType.PERSON,
                entityId = "some-id",
                summary = "a".repeat(500)
            )
            entry.summary.length shouldBe 500
        }

        @Test
        fun `should allow null personId`() {
            val entry = AuditLogEntry(
                userId = userId,
                action = AuditAction.UPDATE,
                entityType = AuditEntityType.USER_SETTINGS,
                entityId = "some-id",
                personId = null,
                summary = "Updated settings"
            )
            entry.personId shouldBe null
        }
    }

    @Nested
    inner class FactoryMethodTests {

        @Test
        fun `personCreated should create correct entry`() {
            val entry = AuditLogEntry.personCreated(userId, personId, "John Doe")

            entry.userId shouldBe userId
            entry.action shouldBe AuditAction.CREATE
            entry.entityType shouldBe AuditEntityType.PERSON
            entry.entityId shouldBe personId.value.toString()
            entry.personId shouldBe personId
            entry.summary shouldBe "Created person \"John Doe\""
        }

        @Test
        fun `personUpdated should create correct entry`() {
            val entry = AuditLogEntry.personUpdated(userId, personId, "Jane Smith")

            entry.action shouldBe AuditAction.UPDATE
            entry.entityType shouldBe AuditEntityType.PERSON
            entry.summary shouldBe "Updated person \"Jane Smith\""
        }

        @Test
        fun `personDeleted should create correct entry`() {
            val entry = AuditLogEntry.personDeleted(userId, personId, "Bob")

            entry.action shouldBe AuditAction.DELETE
            entry.entityType shouldBe AuditEntityType.PERSON
            entry.summary shouldBe "Deleted person \"Bob\""
        }

        @Test
        fun `personRestored should create correct entry`() {
            val entry = AuditLogEntry.personRestored(userId, personId, "Alice")

            entry.action shouldBe AuditAction.RESTORE
            entry.entityType shouldBe AuditEntityType.PERSON
            entry.summary shouldBe "Restored person \"Alice\""
        }

        @Test
        fun `personPermanentlyDeleted should create correct entry`() {
            val entry = AuditLogEntry.personPermanentlyDeleted(userId, personId, "Charlie")

            entry.action shouldBe AuditAction.DELETE
            entry.entityType shouldBe AuditEntityType.PERSON
            entry.entityId shouldBe personId.value.toString()
            entry.personId shouldBe null
            entry.summary shouldBe "Permanently deleted person \"Charlie\""
        }

        @Test
        fun `oneOnOneEntryCreated should create correct entry`() {
            val entryId = OneOnOneEntryId.generate()
            val entry = AuditLogEntry.oneOnOneEntryCreated(userId, entryId, personId, "John")

            entry.action shouldBe AuditAction.CREATE
            entry.entityType shouldBe AuditEntityType.ONE_ON_ONE_ENTRY
            entry.entityId shouldBe entryId.value.toString()
            entry.personId shouldBe personId
            entry.summary shouldBe "Created 1:1 entry for \"John\""
        }

        @Test
        fun `actionItemCreated should create correct entry`() {
            val actionItemId = ActionItemId.generate()
            val entry = AuditLogEntry.actionItemCreated(userId, actionItemId, personId, "Fix bug")

            entry.action shouldBe AuditAction.CREATE
            entry.entityType shouldBe AuditEntityType.ACTION_ITEM
            entry.entityId shouldBe actionItemId.value.toString()
            entry.personId shouldBe personId
            entry.summary shouldBe "Created action item \"Fix bug\""
        }

        @Test
        fun `pdpGoalCreated should create correct entry`() {
            val goalId = PdpGoalId.generate()
            val entry = AuditLogEntry.pdpGoalCreated(userId, goalId, personId, "Learn Kotlin")

            entry.action shouldBe AuditAction.CREATE
            entry.entityType shouldBe AuditEntityType.PDP_GOAL
            entry.entityId shouldBe goalId.value.toString()
            entry.summary shouldBe "Created PDP goal \"Learn Kotlin\""
        }

        @Test
        fun `kudosCreated should create correct entry`() {
            val kudosId = KudosId.generate()
            val entry = AuditLogEntry.kudosCreated(userId, kudosId, personId, "John")

            entry.action shouldBe AuditAction.CREATE
            entry.entityType shouldBe AuditEntityType.KUDOS
            entry.entityId shouldBe kudosId.value.toString()
            entry.summary shouldBe "Created kudos for \"John\""
        }

        @Test
        fun `quickNoteCreated should create correct entry`() {
            val noteId = QuickNoteId.generate()
            val entry = AuditLogEntry.quickNoteCreated(userId, noteId)

            entry.action shouldBe AuditAction.CREATE
            entry.entityType shouldBe AuditEntityType.QUICK_NOTE
            entry.entityId shouldBe noteId.value.toString()
            entry.personId shouldBe null
            entry.summary shouldBe "Created quick note"
        }

        @Test
        fun `userSettingsUpdated should create correct entry`() {
            val entry = AuditLogEntry.userSettingsUpdated(userId)

            entry.action shouldBe AuditAction.UPDATE
            entry.entityType shouldBe AuditEntityType.USER_SETTINGS
            entry.entityId shouldBe userId.value.toString()
            entry.personId shouldBe null
            entry.summary shouldBe "Updated user settings"
        }
    }
}
