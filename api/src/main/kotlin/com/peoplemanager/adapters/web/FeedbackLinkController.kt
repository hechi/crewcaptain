package com.peoplemanager.adapters.web

import com.peoplemanager.adapters.auth.AuthenticatedUser
import com.peoplemanager.adapters.web.dto.*
import com.peoplemanager.application.commands.*
import com.peoplemanager.application.port.input.FeedbackLinkCommandPort
import com.peoplemanager.application.port.input.FeedbackLinkQueryPort
import com.peoplemanager.application.queries.ListFeedbackLinksQuery
import com.peoplemanager.domain.FeedbackLinkId
import com.peoplemanager.domain.FeedbackTemplateId
import com.peoplemanager.domain.PersonId
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class FeedbackLinkController(
    private val commandPort: FeedbackLinkCommandPort,
    private val queryPort: FeedbackLinkQueryPort
) {

    @PostMapping("/persons/{personId}/feedback-links")
    fun create(
        @PathVariable personId: UUID,
        @Valid @RequestBody request: CreateFeedbackLinkRequest
    ): ResponseEntity<FeedbackLinkResponse> {
        val userId = AuthenticatedUser.getUserId()
        val link = commandPort.createLink(
            CreateFeedbackLinkCommand(
                userId = userId,
                personId = PersonId(personId),
                templateId = request.templateId?.let { FeedbackTemplateId(it) },
                inlineTitle = request.title,
                inlineDescription = request.description,
                inlineQuestions = request.questions?.map { it.toDomain() },
                expiresInDays = request.expiresInDays,
                label = request.label,
                requestSubmitterInfo = request.requestSubmitterInfo ?: true
            )
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(FeedbackLinkResponse.from(link))
    }

    @GetMapping("/persons/{personId}/feedback-links")
    fun list(@PathVariable personId: UUID): ResponseEntity<List<FeedbackLinkResponse>> {
        val userId = AuthenticatedUser.getUserId()
        val links = queryPort.listLinks(ListFeedbackLinksQuery(userId, PersonId(personId)))
        return ResponseEntity.ok(links.map { FeedbackLinkResponse.from(it) })
    }

    @PostMapping("/feedback-links/bulk")
    fun bulkCreate(@Valid @RequestBody request: BulkCreateFeedbackLinksRequest): ResponseEntity<BulkFeedbackLinkResultResponse> {
        val userId = AuthenticatedUser.getUserId()
        val templateId = request.templateId ?: throw IllegalArgumentException("templateId is required")
        val results = commandPort.bulkCreateLinks(
            BulkCreateFeedbackLinksCommand(
                userId = userId,
                personIds = request.personIds!!.map { PersonId(it) },
                templateId = FeedbackTemplateId(templateId),
                expiresInDays = request.expiresInDays,
                label = request.label,
                requestSubmitterInfo = request.requestSubmitterInfo ?: true
            )
        )
        val items = results.map { (link, name) ->
            BulkFeedbackLinkItem(link.personId.value, name, link.token, link.id.value)
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(BulkFeedbackLinkResultResponse(items))
    }

    @PostMapping("/feedback-links/{linkId}/revoke")
    fun revoke(@PathVariable linkId: UUID): ResponseEntity<FeedbackLinkResponse> {
        val userId = AuthenticatedUser.getUserId()
        val link = commandPort.revokeLink(RevokeFeedbackLinkCommand(userId, FeedbackLinkId(linkId)))
        return ResponseEntity.ok(FeedbackLinkResponse.from(link))
    }

    @PostMapping("/feedback-links/{linkId}/extend")
    fun extend(
        @PathVariable linkId: UUID,
        @RequestBody request: ExtendFeedbackLinkRequest
    ): ResponseEntity<FeedbackLinkResponse> {
        val userId = AuthenticatedUser.getUserId()
        val days = request.expiresInDays ?: throw IllegalArgumentException("expiresInDays is required")
        val link = commandPort.extendLink(ExtendFeedbackLinkCommand(userId, FeedbackLinkId(linkId), days))
        return ResponseEntity.ok(FeedbackLinkResponse.from(link))
    }
}
