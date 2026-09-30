package com.peoplemanager.adapters.web

import com.peoplemanager.adapters.auth.AuthenticatedUser
import com.peoplemanager.adapters.web.dto.BulkUpdateFeedbackResponsesRequest
import com.peoplemanager.adapters.web.dto.ConvertFeedbackResponseRequest
import com.peoplemanager.adapters.web.dto.FeedbackResponseItemResponse
import com.peoplemanager.adapters.web.dto.UpdateFeedbackResponseRequest
import com.peoplemanager.application.commands.*
import com.peoplemanager.application.port.input.FeedbackResponseCommandPort
import com.peoplemanager.application.port.input.FeedbackResponseQueryPort
import com.peoplemanager.application.queries.ListFeedbackResponsesQuery
import com.peoplemanager.domain.FeedbackConversionType
import com.peoplemanager.domain.FeedbackResponseId
import com.peoplemanager.domain.FeedbackResponseStatus
import com.peoplemanager.domain.PersonId
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class FeedbackResponseController(
    private val commandPort: FeedbackResponseCommandPort,
    private val queryPort: FeedbackResponseQueryPort
) {

    @GetMapping("/persons/{personId}/feedback-responses")
    fun list(
        @PathVariable personId: UUID,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) flagged: Boolean?
    ): ResponseEntity<List<FeedbackResponseItemResponse>> {
        val userId = AuthenticatedUser.getUserId()
        val statusEnum = status?.let { FeedbackResponseStatus.valueOf(it.uppercase()) }
        val responses = queryPort.listResponses(
            ListFeedbackResponsesQuery(userId, PersonId(personId), statusEnum, flagged)
        )
        return ResponseEntity.ok(responses.map { FeedbackResponseItemResponse.from(it) })
    }

    @PatchMapping("/feedback-responses/{responseId}")
    fun update(
        @PathVariable responseId: UUID,
        @RequestBody request: UpdateFeedbackResponseRequest
    ): ResponseEntity<FeedbackResponseItemResponse> {
        val userId = AuthenticatedUser.getUserId()
        val updated = commandPort.updateResponse(
            UpdateFeedbackResponseCommand(
                userId = userId,
                responseId = FeedbackResponseId(responseId),
                approve = request.approve,
                flagged = request.flagged,
                pinned = request.pinned
            )
        )
        return ResponseEntity.ok(FeedbackResponseItemResponse.from(updated))
    }

    @DeleteMapping("/feedback-responses/{responseId}")
    fun delete(@PathVariable responseId: UUID): ResponseEntity<Void> {
        val userId = AuthenticatedUser.getUserId()
        commandPort.deleteResponse(DeleteFeedbackResponseCommand(userId, FeedbackResponseId(responseId)))
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/feedback-responses/bulk")
    fun bulk(@Valid @RequestBody request: BulkUpdateFeedbackResponsesRequest): ResponseEntity<Void> {
        val userId = AuthenticatedUser.getUserId()
        val action = when (request.action?.uppercase()) {
            "APPROVE" -> BulkResponseAction.APPROVE
            "DELETE" -> BulkResponseAction.DELETE
            else -> throw IllegalArgumentException("action must be APPROVE or DELETE")
        }
        commandPort.bulkUpdate(
            BulkUpdateFeedbackResponsesCommand(
                userId = userId,
                responseIds = request.responseIds!!.map { FeedbackResponseId(it) },
                action = action
            )
        )
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/feedback-responses/{responseId}/convert")
    fun convert(
        @PathVariable responseId: UUID,
        @RequestBody request: ConvertFeedbackResponseRequest
    ): ResponseEntity<FeedbackResponseItemResponse> {
        val userId = AuthenticatedUser.getUserId()
        val type = when (request.type?.uppercase()) {
            "KUDO" -> FeedbackConversionType.KUDO
            "QUICK_NOTE" -> FeedbackConversionType.QUICK_NOTE
            "ACTION_ITEM" -> FeedbackConversionType.ACTION_ITEM
            else -> throw IllegalArgumentException("type must be KUDO, QUICK_NOTE or ACTION_ITEM")
        }
        val updated = commandPort.convertResponse(
            ConvertFeedbackResponseCommand(userId, FeedbackResponseId(responseId), type, request.text)
        )
        return ResponseEntity.ok(FeedbackResponseItemResponse.from(updated))
    }
}
