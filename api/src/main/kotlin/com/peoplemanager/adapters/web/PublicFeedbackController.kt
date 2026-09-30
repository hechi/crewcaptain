package com.peoplemanager.adapters.web

import com.peoplemanager.adapters.web.dto.PublicFeedbackFormResponse
import com.peoplemanager.adapters.web.dto.SubmitPublicFeedbackRequest
import com.peoplemanager.application.commands.SubmitFeedbackCommand
import com.peoplemanager.application.port.input.PublicFeedbackPort
import com.peoplemanager.application.queries.GetPublicFeedbackFormQuery
import com.peoplemanager.domain.FeedbackAnswer
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Public, unauthenticated endpoints for the shareable feedback form.
 * Served under the public feedback path, which is permitAll in SecurityConfig.
 * MUST NOT reference AuthenticatedUser.
 */
@RestController
@RequestMapping("/api/v1/public/feedback")
class PublicFeedbackController(
    private val publicFeedbackPort: PublicFeedbackPort
) {

    private val logger = LoggerFactory.getLogger(PublicFeedbackController::class.java)

    @GetMapping("/{token}")
    fun getForm(@PathVariable token: String): ResponseEntity<PublicFeedbackFormResponse> {
        val view = publicFeedbackPort.getForm(GetPublicFeedbackFormQuery(token))
        return ResponseEntity.ok(PublicFeedbackFormResponse.from(view))
    }

    @PostMapping("/{token}")
    fun submit(
        @PathVariable token: String,
        @RequestBody request: SubmitPublicFeedbackRequest
    ): ResponseEntity<Void> {
        // Honeypot: real users never fill the hidden 'website' field. Silently accept
        // (return 204) so bots can't distinguish rejection, but never persist.
        if (!request.website.isNullOrBlank()) {
            logger.debug("Rejected feedback submission via honeypot for token ${token.take(6)}...")
            return ResponseEntity.noContent().build()
        }

        publicFeedbackPort.submit(
            SubmitFeedbackCommand(
                token = token,
                anonymous = request.anonymous,
                submitterName = request.submitterName,
                submitterEmail = request.submitterEmail,
                answers = request.answers.map { FeedbackAnswer(it.questionId, it.ratingValue, it.textValue) },
                additionalComments = request.additionalComments
            )
        )
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }
}
