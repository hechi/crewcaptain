package com.peoplemanager.domain

import java.util.UUID

@JvmInline
value class UserId(val value: UUID) {
    companion object {
        fun generate(): UserId = UserId(UUID.randomUUID())
    }
}

@JvmInline
value class PersonId(val value: UUID) {
    companion object {
        fun generate(): PersonId = PersonId(UUID.randomUUID())
    }
}

@JvmInline
value class RememberItemId(val value: UUID) {
    companion object {
        fun generate(): RememberItemId = RememberItemId(UUID.randomUUID())
    }
}

@JvmInline
value class OneOnOneSeriesId(val value: UUID) {
    companion object {
        fun generate(): OneOnOneSeriesId = OneOnOneSeriesId(UUID.randomUUID())
    }
}

@JvmInline
value class OneOnOneEntryId(val value: UUID) {
    companion object {
        fun generate(): OneOnOneEntryId = OneOnOneEntryId(UUID.randomUUID())
    }
}

@JvmInline
value class AgendaItemId(val value: UUID) {
    companion object {
        fun generate(): AgendaItemId = AgendaItemId(UUID.randomUUID())
    }
}

enum class MoraleStatus {
    GREEN, YELLOW, RED, UNKNOWN
}

@JvmInline
value class ActionItemId(val value: UUID) {
    companion object {
        fun generate(): ActionItemId = ActionItemId(UUID.randomUUID())
    }
}

enum class CadenceType {
    WEEKLY, BIWEEKLY, MONTHLY, CUSTOM
}

enum class ActionItemStatus {
    OPEN, DONE, CANCELED
}

enum class ActionItemOwnerType {
    MANAGER, PERSON
}

@JvmInline
value class PdpGoalId(val value: UUID) {
    companion object {
        fun generate(): PdpGoalId = PdpGoalId(UUID.randomUUID())
    }
}

@JvmInline
value class PdpUpdateId(val value: UUID) {
    companion object {
        fun generate(): PdpUpdateId = PdpUpdateId(UUID.randomUUID())
    }
}

enum class PdpGoalStatus {
    ACTIVE, ACHIEVED, PAUSED, DROPPED
}

@JvmInline
value class KudosId(val value: UUID) {
    companion object {
        fun generate(): KudosId = KudosId(UUID.randomUUID())
    }
}

@JvmInline
value class QuickNoteId(val value: UUID) {
    companion object {
        fun generate(): QuickNoteId = QuickNoteId(UUID.randomUUID())
    }
}

enum class QuickNoteStatus {
    INBOX, ATTACHED, CONVERTED, ARCHIVED
}

data class OidcIdentity(
    val subject: String,
    val issuer: String
) {
    init {
        require(subject.isNotBlank()) { "OIDC subject must not be blank" }
        require(issuer.isNotBlank()) { "OIDC issuer must not be blank" }
    }
}
