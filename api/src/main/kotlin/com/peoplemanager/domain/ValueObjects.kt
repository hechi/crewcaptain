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

@JvmInline
value class WorkspaceId(val value: UUID) {
    companion object {
        fun generate(): WorkspaceId = WorkspaceId(UUID.randomUUID())
    }
}

@JvmInline
value class StrategyGoalId(val value: UUID) {
    companion object {
        fun generate(): StrategyGoalId = StrategyGoalId(UUID.randomUUID())
    }
}

@JvmInline
value class StrategyGoalPdpGoalLinkId(val value: UUID) {
    companion object {
        fun generate(): StrategyGoalPdpGoalLinkId = StrategyGoalPdpGoalLinkId(UUID.randomUUID())
    }
}

enum class StrategyGoalStatus {
    ACTIVE, ACHIEVED, DROPPED
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

@JvmInline
value class FeedbackTemplateId(val value: UUID) {
    companion object {
        fun generate(): FeedbackTemplateId = FeedbackTemplateId(UUID.randomUUID())
    }
}

@JvmInline
value class FeedbackLinkId(val value: UUID) {
    companion object {
        fun generate(): FeedbackLinkId = FeedbackLinkId(UUID.randomUUID())
    }
}

@JvmInline
value class FeedbackResponseId(val value: UUID) {
    companion object {
        fun generate(): FeedbackResponseId = FeedbackResponseId(UUID.randomUUID())
    }
}

@JvmInline
value class FeedbackSummaryId(val value: UUID) {
    companion object {
        fun generate(): FeedbackSummaryId = FeedbackSummaryId(UUID.randomUUID())
    }
}

enum class FeedbackQuestionType {
    /** 1-5 numeric rating with endpoint labels. */
    RATING,

    /** 5-point Likert scale, stored as 1..5 (Strongly disagree..Strongly agree). */
    LIKERT,

    /** Free-text answer. */
    TEXT
}

/** Comparison operator for a single-level conditional show rule. */
enum class ShowIfOperator {
    LTE, // show when the referenced answer value is <= threshold
    GTE, // show when the referenced answer value is >= threshold
    EQ   // show when the referenced answer value == threshold
}

/** Lifecycle state of a feedback link, derived from expiry/revocation. */
enum class FeedbackLinkStatus {
    ACTIVE, EXPIRED, REVOKED
}

/** Manager review state of a submitted response. */
enum class FeedbackResponseStatus {
    /** Newly submitted, awaiting manager review. */
    PENDING,

    /** Manager cleared it for use in packets/prep/summaries/export. */
    APPROVED
}

/** Target of a response conversion. */
enum class FeedbackConversionType {
    KUDO, QUICK_NOTE, ACTION_ITEM
}
