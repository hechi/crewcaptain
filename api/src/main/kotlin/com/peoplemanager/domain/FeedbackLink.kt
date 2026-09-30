package com.peoplemanager.domain

import java.security.SecureRandom
import java.time.Instant
import java.util.Base64

/**
 * A shareable, unauthenticated feedback link for a single person.
 *
 * Always scoped to the owning manager ([userId]) and a [personId] they own.
 * The [questions] are a snapshot of the template at creation time, so editing or
 * deleting the source template never alters this link or its collected responses.
 *
 * Links are multi-use until [expiresAt]. Revocation is final.
 */
data class FeedbackLink(
    val id: FeedbackLinkId,
    val userId: UserId,
    val personId: PersonId,
    val token: String,
    val title: String,
    val description: String? = null,
    /** Snapshot of the template's questions at creation time. */
    val questions: List<FeedbackQuestion>,
    /** Internal label for the manager (not shown to submitters). */
    val label: String? = null,
    /** Whether the public form should offer optional name/email fields. */
    val requestSubmitterInfo: Boolean = true,
    val expiresAt: Instant,
    val revokedAt: Instant? = null,
    /** Source template id, for "create another from the same template". Nullable if the template was deleted. */
    val sourceTemplateId: FeedbackTemplateId? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    init {
        require(token.isNotBlank()) { "Feedback link token must not be blank" }
        require(title.isNotBlank()) { "Feedback link title must not be blank" }
        require(questions.isNotEmpty()) { "A feedback link must have at least one question" }
    }

    fun statusAt(now: Instant): FeedbackLinkStatus = when {
        revokedAt != null -> FeedbackLinkStatus.REVOKED
        !now.isBefore(expiresAt) -> FeedbackLinkStatus.EXPIRED
        else -> FeedbackLinkStatus.ACTIVE
    }

    /** True when the link can currently accept submissions. */
    fun acceptsSubmissionsAt(now: Instant): Boolean = statusAt(now) == FeedbackLinkStatus.ACTIVE

    fun revoke(): FeedbackLink {
        require(revokedAt == null) { "Feedback link is already revoked" }
        val now = Instant.now()
        return copy(revokedAt = now, updatedAt = now)
    }

    fun extend(newExpiresAt: Instant): FeedbackLink {
        require(revokedAt == null) { "Cannot extend a revoked link" }
        require(newExpiresAt.isAfter(expiresAt)) { "New expiry must be later than the current expiry" }
        return copy(expiresAt = newExpiresAt, updatedAt = Instant.now())
    }

    companion object {
        const val DEFAULT_EXPIRY_DAYS = 14L
        const val MAX_EXPIRY_DAYS = 90L
        private val RANDOM = SecureRandom()

        /** Generates a 128-bit URL-safe token (22 chars, no padding). */
        fun generateToken(): String {
            val bytes = ByteArray(16)
            RANDOM.nextBytes(bytes)
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        }
    }
}
