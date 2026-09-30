package com.peoplemanager.application

import com.peoplemanager.domain.FeedbackLinkId
import com.peoplemanager.domain.FeedbackResponseId

class FeedbackLinkNotFoundException(linkId: FeedbackLinkId) :
    RuntimeException("Feedback link not found: ${linkId.value}")

class FeedbackResponseNotFoundException(responseId: FeedbackResponseId) :
    RuntimeException("Feedback response not found: ${responseId.value}")

/** Public token did not resolve to an existing link (or its person was deleted). */
class FeedbackLinkTokenNotFoundException : RuntimeException("Feedback link not found")

/** Public link exists but is expired. */
class FeedbackLinkExpiredException : RuntimeException("This link has expired")

/** Public link exists but was revoked. */
class FeedbackLinkRevokedException : RuntimeException("This link has been revoked")

/** Submission failed validation (e.g. missing required answers, out-of-range values). */
class FeedbackSubmissionInvalidException(message: String) : RuntimeException(message)
