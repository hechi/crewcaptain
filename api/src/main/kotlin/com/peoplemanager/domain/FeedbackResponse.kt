package com.peoplemanager.domain

import java.time.Instant

/**
 * A single answer to one question within a submitted response.
 *
 * Exactly one of [ratingValue] / [textValue] is populated depending on the question type.
 * For RATING and LIKERT questions the answer is stored as an integer 1..5.
 */
data class FeedbackAnswer(
    val questionId: String,
    val ratingValue: Int? = null,
    val textValue: String? = null
) {
    init {
        require(questionId.isNotBlank()) { "Answer questionId must not be blank" }
        require(ratingValue == null || ratingValue in 1..5) { "Rating answers must be between 1 and 5" }
    }
}

/**
 * A submission against a feedback link. Always scoped to the link owner ([userId])
 * and the [personId] the feedback is about.
 *
 * The submitter is anonymous by default. Free-text content in [answers] and
 * [additionalComments] is treated as sensitive and encrypted at rest by the adapter.
 */
data class FeedbackResponse(
    val id: FeedbackResponseId,
    val userId: UserId,
    val personId: PersonId,
    val linkId: FeedbackLinkId,
    val submitterName: String? = null,
    val submitterEmail: String? = null,
    val anonymous: Boolean = true,
    val answers: List<FeedbackAnswer> = emptyList(),
    val additionalComments: String? = null,
    val status: FeedbackResponseStatus = FeedbackResponseStatus.PENDING,
    val flagged: Boolean = false,
    val pinned: Boolean = false,
    val convertedToType: FeedbackConversionType? = null,
    val convertedToId: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    init {
        if (anonymous) {
            require(submitterName == null && submitterEmail == null) {
                "Anonymous responses must not carry submitter name or email"
            }
        }
    }

    /** True when this response should feed analytics, AI, packets and export. */
    val isUsable: Boolean get() = status == FeedbackResponseStatus.APPROVED && !flagged

    fun approve(): FeedbackResponse =
        copy(status = FeedbackResponseStatus.APPROVED, updatedAt = Instant.now())

    fun setFlagged(flagged: Boolean): FeedbackResponse =
        copy(flagged = flagged, updatedAt = Instant.now())

    fun setPinned(pinned: Boolean): FeedbackResponse =
        copy(pinned = pinned, updatedAt = Instant.now())

    fun markConverted(type: FeedbackConversionType, targetId: String): FeedbackResponse =
        copy(convertedToType = type, convertedToId = targetId, updatedAt = Instant.now())

    /** All non-blank free-text content (per-question text answers plus the final comment). */
    fun freeTextParts(): List<String> =
        (answers.mapNotNull { it.textValue } + listOfNotNull(additionalComments))
            .map { it.trim() }
            .filter { it.isNotBlank() }

    /** All numeric (rating + likert) answer values. */
    fun ratingValues(): List<Int> = answers.mapNotNull { it.ratingValue }
}
