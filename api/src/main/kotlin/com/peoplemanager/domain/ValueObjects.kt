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

data class OidcIdentity(
    val subject: String,
    val issuer: String
) {
    init {
        require(subject.isNotBlank()) { "OIDC subject must not be blank" }
        require(issuer.isNotBlank()) { "OIDC issuer must not be blank" }
    }
}
