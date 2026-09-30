package com.peoplemanager.adapters.web

import com.peoplemanager.adapters.auth.AuthenticatedUser
import com.peoplemanager.adapters.web.dto.FeedbackTemplateDraftResponse
import com.peoplemanager.adapters.web.dto.FeedbackTemplateResponse
import com.peoplemanager.adapters.web.dto.GenerateFeedbackTemplateRequest
import com.peoplemanager.adapters.web.dto.SaveFeedbackTemplateRequest
import com.peoplemanager.application.FeedbackTemplateAiService
import com.peoplemanager.application.FeedbackTemplateGenerationResult
import com.peoplemanager.application.commands.CreateFeedbackTemplateCommand
import com.peoplemanager.application.commands.DeleteFeedbackTemplateCommand
import com.peoplemanager.application.commands.GenerateFeedbackTemplateCommand
import com.peoplemanager.application.commands.UpdateFeedbackTemplateCommand
import com.peoplemanager.application.port.input.FeedbackTemplateCommandPort
import com.peoplemanager.application.port.input.FeedbackTemplateQueryPort
import com.peoplemanager.application.queries.GetFeedbackTemplateQuery
import com.peoplemanager.application.queries.ListFeedbackTemplatesQuery
import com.peoplemanager.domain.FeedbackTemplateId
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/feedback-templates")
class FeedbackTemplateController(
    private val commandPort: FeedbackTemplateCommandPort,
    private val queryPort: FeedbackTemplateQueryPort,
    private val aiService: FeedbackTemplateAiService
) {

    @GetMapping
    fun list(): ResponseEntity<List<FeedbackTemplateResponse>> {
        val userId = AuthenticatedUser.getUserId()
        val templates = queryPort.listTemplates(ListFeedbackTemplatesQuery(userId))
        return ResponseEntity.ok(templates.map { FeedbackTemplateResponse.from(it) })
    }

    @GetMapping("/starters")
    fun starters(): ResponseEntity<List<FeedbackTemplateDraftResponse>> {
        // Requires auth (any manager); starters are static drafts.
        AuthenticatedUser.getUserId()
        return ResponseEntity.ok(queryPort.listStarterTemplates().map { FeedbackTemplateDraftResponse.from(it) })
    }

    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID): ResponseEntity<FeedbackTemplateResponse> {
        val userId = AuthenticatedUser.getUserId()
        val template = queryPort.getTemplate(GetFeedbackTemplateQuery(userId, FeedbackTemplateId(id)))
        return ResponseEntity.ok(FeedbackTemplateResponse.from(template))
    }

    @PostMapping
    fun create(@Valid @RequestBody request: SaveFeedbackTemplateRequest): ResponseEntity<FeedbackTemplateResponse> {
        val userId = AuthenticatedUser.getUserId()
        val template = commandPort.createTemplate(
            CreateFeedbackTemplateCommand(
                userId = userId,
                title = request.title!!,
                description = request.description,
                questions = request.questions.map { it.toDomain() }
            )
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(FeedbackTemplateResponse.from(template))
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody request: SaveFeedbackTemplateRequest
    ): ResponseEntity<FeedbackTemplateResponse> {
        val userId = AuthenticatedUser.getUserId()
        val template = commandPort.updateTemplate(
            UpdateFeedbackTemplateCommand(
                userId = userId,
                templateId = FeedbackTemplateId(id),
                title = request.title!!,
                description = request.description,
                questions = request.questions.map { it.toDomain() }
            )
        )
        return ResponseEntity.ok(FeedbackTemplateResponse.from(template))
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: UUID): ResponseEntity<Void> {
        val userId = AuthenticatedUser.getUserId()
        commandPort.deleteTemplate(DeleteFeedbackTemplateCommand(userId, FeedbackTemplateId(id)))
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/ai-generate")
    fun generate(@Valid @RequestBody request: GenerateFeedbackTemplateRequest): ResponseEntity<Any> {
        val userId = AuthenticatedUser.getUserId()
        val result = aiService.generate(GenerateFeedbackTemplateCommand(userId, request.brief!!))
        return when (result) {
            is FeedbackTemplateGenerationResult.Success ->
                ResponseEntity.ok(FeedbackTemplateDraftResponse.from(result.template))
            is FeedbackTemplateGenerationResult.Error ->
                ResponseEntity.unprocessableEntity().body(mapOf("message" to result.message))
        }
    }
}
