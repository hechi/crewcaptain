package com.peoplemanager.domain

import java.time.Instant
import java.util.UUID

@JvmInline
value class AuditLogEntryId(val value: UUID) {
    companion object {
        fun generate(): AuditLogEntryId = AuditLogEntryId(UUID.randomUUID())
    }
}

enum class AuditAction {
    CREATE,
    UPDATE,
    DELETE,
    RESTORE,
    LINK,
    UNLINK
}

enum class AuditEntityType {
    PERSON,
    ONE_ON_ONE_ENTRY,
    ONE_ON_ONE_SERIES,
    ACTION_ITEM,
    PDP_GOAL,
    PDP_UPDATE,
    KUDOS,
    QUICK_NOTE,
    USER_SETTINGS,
    WORKSPACE,
    STRATEGY_GOAL
}

/**
 * Immutable audit log entry recording a key action performed by a manager.
 * Always scoped to a single user (manager). Used for the manager's own traceability.
 * Never shared across users.
 */
data class AuditLogEntry(
    val id: AuditLogEntryId = AuditLogEntryId.generate(),
    val userId: UserId,
    val action: AuditAction,
    val entityType: AuditEntityType,
    val entityId: String,
    val personId: PersonId? = null,
    val summary: String,
    val createdAt: Instant = Instant.now()
) {
    init {
        require(entityId.isNotBlank()) { "Entity ID must not be blank" }
        require(summary.isNotBlank()) { "Audit log summary must not be blank" }
        require(summary.length <= 500) { "Audit log summary must not exceed 500 characters" }
    }

    companion object {
        fun personCreated(userId: UserId, personId: PersonId, personName: String): AuditLogEntry =
            AuditLogEntry(
                userId = userId,
                action = AuditAction.CREATE,
                entityType = AuditEntityType.PERSON,
                entityId = personId.value.toString(),
                personId = personId,
                summary = "Created person \"$personName\""
            )

        fun personUpdated(userId: UserId, personId: PersonId, personName: String): AuditLogEntry =
            AuditLogEntry(
                userId = userId,
                action = AuditAction.UPDATE,
                entityType = AuditEntityType.PERSON,
                entityId = personId.value.toString(),
                personId = personId,
                summary = "Updated person \"$personName\""
            )

        fun personDeleted(userId: UserId, personId: PersonId, personName: String): AuditLogEntry =
            AuditLogEntry(
                userId = userId,
                action = AuditAction.DELETE,
                entityType = AuditEntityType.PERSON,
                entityId = personId.value.toString(),
                personId = personId,
                summary = "Deleted person \"$personName\""
            )

        fun personRestored(userId: UserId, personId: PersonId, personName: String): AuditLogEntry =
            AuditLogEntry(
                userId = userId,
                action = AuditAction.RESTORE,
                entityType = AuditEntityType.PERSON,
                entityId = personId.value.toString(),
                personId = personId,
                summary = "Restored person \"$personName\""
            )

        fun personPermanentlyDeleted(userId: UserId, personId: PersonId, personName: String): AuditLogEntry =
            AuditLogEntry(
                userId = userId,
                action = AuditAction.DELETE,
                entityType = AuditEntityType.PERSON,
                entityId = personId.value.toString(),
                personId = null,
                summary = "Permanently deleted person \"$personName\""
            )

        fun oneOnOneEntryCreated(userId: UserId, entryId: OneOnOneEntryId, personId: PersonId, personName: String): AuditLogEntry =
            AuditLogEntry(
                userId = userId,
                action = AuditAction.CREATE,
                entityType = AuditEntityType.ONE_ON_ONE_ENTRY,
                entityId = entryId.value.toString(),
                personId = personId,
                summary = "Created 1:1 entry for \"$personName\""
            )

        fun oneOnOneEntryUpdated(userId: UserId, entryId: OneOnOneEntryId, personId: PersonId, personName: String): AuditLogEntry =
            AuditLogEntry(
                userId = userId,
                action = AuditAction.UPDATE,
                entityType = AuditEntityType.ONE_ON_ONE_ENTRY,
                entityId = entryId.value.toString(),
                personId = personId,
                summary = "Updated 1:1 entry for \"$personName\""
            )

        fun oneOnOneEntryDeleted(userId: UserId, entryId: OneOnOneEntryId, personId: PersonId, personName: String): AuditLogEntry =
            AuditLogEntry(
                userId = userId,
                action = AuditAction.DELETE,
                entityType = AuditEntityType.ONE_ON_ONE_ENTRY,
                entityId = entryId.value.toString(),
                personId = personId,
                summary = "Deleted 1:1 entry for \"$personName\""
            )

        fun actionItemCreated(userId: UserId, actionItemId: ActionItemId, personId: PersonId, title: String): AuditLogEntry =
            AuditLogEntry(
                userId = userId,
                action = AuditAction.CREATE,
                entityType = AuditEntityType.ACTION_ITEM,
                entityId = actionItemId.value.toString(),
                personId = personId,
                summary = "Created action item \"$title\""
            )

        fun actionItemUpdated(userId: UserId, actionItemId: ActionItemId, personId: PersonId, title: String): AuditLogEntry =
            AuditLogEntry(
                userId = userId,
                action = AuditAction.UPDATE,
                entityType = AuditEntityType.ACTION_ITEM,
                entityId = actionItemId.value.toString(),
                personId = personId,
                summary = "Updated action item \"$title\""
            )

        fun actionItemDeleted(userId: UserId, actionItemId: ActionItemId, personId: PersonId, title: String): AuditLogEntry =
            AuditLogEntry(
                userId = userId,
                action = AuditAction.DELETE,
                entityType = AuditEntityType.ACTION_ITEM,
                entityId = actionItemId.value.toString(),
                personId = personId,
                summary = "Deleted action item \"$title\""
            )

        fun pdpGoalCreated(userId: UserId, goalId: PdpGoalId, personId: PersonId, title: String): AuditLogEntry =
            AuditLogEntry(
                userId = userId,
                action = AuditAction.CREATE,
                entityType = AuditEntityType.PDP_GOAL,
                entityId = goalId.value.toString(),
                personId = personId,
                summary = "Created PDP goal \"$title\""
            )

        fun pdpGoalUpdated(userId: UserId, goalId: PdpGoalId, personId: PersonId, title: String): AuditLogEntry =
            AuditLogEntry(
                userId = userId,
                action = AuditAction.UPDATE,
                entityType = AuditEntityType.PDP_GOAL,
                entityId = goalId.value.toString(),
                personId = personId,
                summary = "Updated PDP goal \"$title\""
            )

        fun pdpGoalDeleted(userId: UserId, goalId: PdpGoalId, personId: PersonId, title: String): AuditLogEntry =
            AuditLogEntry(
                userId = userId,
                action = AuditAction.DELETE,
                entityType = AuditEntityType.PDP_GOAL,
                entityId = goalId.value.toString(),
                personId = personId,
                summary = "Deleted PDP goal \"$title\""
            )

        fun kudosCreated(userId: UserId, kudosId: KudosId, personId: PersonId, personName: String): AuditLogEntry =
            AuditLogEntry(
                userId = userId,
                action = AuditAction.CREATE,
                entityType = AuditEntityType.KUDOS,
                entityId = kudosId.value.toString(),
                personId = personId,
                summary = "Created kudos for \"$personName\""
            )

        fun kudosDeleted(userId: UserId, kudosId: KudosId, personId: PersonId, personName: String): AuditLogEntry =
            AuditLogEntry(
                userId = userId,
                action = AuditAction.DELETE,
                entityType = AuditEntityType.KUDOS,
                entityId = kudosId.value.toString(),
                personId = personId,
                summary = "Deleted kudos for \"$personName\""
            )

        fun quickNoteCreated(userId: UserId, noteId: QuickNoteId): AuditLogEntry =
            AuditLogEntry(
                userId = userId,
                action = AuditAction.CREATE,
                entityType = AuditEntityType.QUICK_NOTE,
                entityId = noteId.value.toString(),
                summary = "Created quick note"
            )

        fun quickNoteUpdated(userId: UserId, noteId: QuickNoteId): AuditLogEntry =
            AuditLogEntry(
                userId = userId,
                action = AuditAction.UPDATE,
                entityType = AuditEntityType.QUICK_NOTE,
                entityId = noteId.value.toString(),
                summary = "Updated quick note"
            )

        fun quickNoteDeleted(userId: UserId, noteId: QuickNoteId): AuditLogEntry =
            AuditLogEntry(
                userId = userId,
                action = AuditAction.DELETE,
                entityType = AuditEntityType.QUICK_NOTE,
                entityId = noteId.value.toString(),
                summary = "Deleted quick note"
            )

        fun userSettingsUpdated(userId: UserId): AuditLogEntry =
            AuditLogEntry(
                userId = userId,
                action = AuditAction.UPDATE,
                entityType = AuditEntityType.USER_SETTINGS,
                entityId = userId.value.toString(),
                summary = "Updated user settings"
            )

        fun workspaceCreated(userId: UserId, workspaceId: WorkspaceId, name: String): AuditLogEntry =
            AuditLogEntry(
                userId = userId,
                action = AuditAction.CREATE,
                entityType = AuditEntityType.WORKSPACE,
                entityId = workspaceId.value.toString(),
                summary = "Created workspace \"$name\""
            )

        fun workspaceUpdated(userId: UserId, workspaceId: WorkspaceId, name: String): AuditLogEntry =
            AuditLogEntry(
                userId = userId,
                action = AuditAction.UPDATE,
                entityType = AuditEntityType.WORKSPACE,
                entityId = workspaceId.value.toString(),
                summary = "Updated workspace \"$name\""
            )

        fun workspaceDeleted(userId: UserId, workspaceId: WorkspaceId, name: String): AuditLogEntry =
            AuditLogEntry(
                userId = userId,
                action = AuditAction.DELETE,
                entityType = AuditEntityType.WORKSPACE,
                entityId = workspaceId.value.toString(),
                summary = "Deleted workspace \"$name\""
            )

        fun strategyGoalCreated(userId: UserId, strategyGoalId: StrategyGoalId, title: String): AuditLogEntry =
            AuditLogEntry(
                userId = userId,
                action = AuditAction.CREATE,
                entityType = AuditEntityType.STRATEGY_GOAL,
                entityId = strategyGoalId.value.toString(),
                summary = "Created strategy goal \"$title\""
            )

        fun strategyGoalUpdated(userId: UserId, strategyGoalId: StrategyGoalId, title: String): AuditLogEntry =
            AuditLogEntry(
                userId = userId,
                action = AuditAction.UPDATE,
                entityType = AuditEntityType.STRATEGY_GOAL,
                entityId = strategyGoalId.value.toString(),
                summary = "Updated strategy goal \"$title\""
            )

        fun strategyGoalDeleted(userId: UserId, strategyGoalId: StrategyGoalId, title: String): AuditLogEntry =
            AuditLogEntry(
                userId = userId,
                action = AuditAction.DELETE,
                entityType = AuditEntityType.STRATEGY_GOAL,
                entityId = strategyGoalId.value.toString(),
                summary = "Deleted strategy goal \"$title\""
            )

        fun strategyGoalLinked(userId: UserId, strategyGoalId: StrategyGoalId, strategyGoalTitle: String, pdpGoalTitle: String): AuditLogEntry =
            AuditLogEntry(
                userId = userId,
                action = AuditAction.LINK,
                entityType = AuditEntityType.STRATEGY_GOAL,
                entityId = strategyGoalId.value.toString(),
                summary = "Linked PDP goal \"$pdpGoalTitle\" to strategy goal \"$strategyGoalTitle\""
            )

        fun strategyGoalUnlinked(userId: UserId, strategyGoalId: StrategyGoalId, strategyGoalTitle: String, pdpGoalTitle: String): AuditLogEntry =
            AuditLogEntry(
                userId = userId,
                action = AuditAction.UNLINK,
                entityType = AuditEntityType.STRATEGY_GOAL,
                entityId = strategyGoalId.value.toString(),
                summary = "Unlinked PDP goal \"$pdpGoalTitle\" from strategy goal \"$strategyGoalTitle\""
            )

        fun strategyGoalAchieved(userId: UserId, strategyGoalId: StrategyGoalId, title: String): AuditLogEntry =
            AuditLogEntry(
                userId = userId,
                action = AuditAction.UPDATE,
                entityType = AuditEntityType.STRATEGY_GOAL,
                entityId = strategyGoalId.value.toString(),
                summary = "Marked strategy goal \"$title\" as achieved"
            )

        fun strategyGoalDropped(userId: UserId, strategyGoalId: StrategyGoalId, title: String): AuditLogEntry =
            AuditLogEntry(
                userId = userId,
                action = AuditAction.UPDATE,
                entityType = AuditEntityType.STRATEGY_GOAL,
                entityId = strategyGoalId.value.toString(),
                summary = "Dropped strategy goal \"$title\""
            )
    }
}
