package com.peoplemanager.adapters.web

import com.peoplemanager.adapters.auth.AuthenticatedUser
import com.peoplemanager.application.AiCommandTerminalService
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/ai")
class AiCommandTerminalController(
    private val aiCommandTerminalService: AiCommandTerminalService
) {

    @PostMapping("/command")
    fun parseCommand(
        @Valid @RequestBody request: AiCommandRequest
    ): ResponseEntity<AiCommandResponse> {
        val userId = AuthenticatedUser.getUserId()

        val result = aiCommandTerminalService.parseCommand(userId, request.input)

        return ResponseEntity.ok(
            AiCommandResponse(
                intent = result.intent,
                targetPersonId = result.targetPersonId,
                content = result.content,
                dueDate = result.dueDate,
                meetingDate = result.meetingDate,
                tags = result.tags,
                sensitive = result.sensitive,
                error = result.error
            )
        )
    }

    @GetMapping("/command/directory")
    fun getPersonDirectory(): ResponseEntity<List<PersonDirectoryEntryResponse>> {
        val userId = AuthenticatedUser.getUserId()

        val directory = aiCommandTerminalService.getPersonDirectory(userId)

        return ResponseEntity.ok(
            directory.map { PersonDirectoryEntryResponse(id = it.id, preferredName = it.preferredName) }
        )
    }
}

data class AiCommandRequest(
    @field:NotBlank(message = "Command input is required")
    val input: String
)

data class AiCommandResponse(
    val intent: String?,
    val targetPersonId: String?,
    val content: String?,
    val dueDate: String?,
    val meetingDate: String?,
    val tags: List<String>,
    val sensitive: Boolean,
    val error: String?
)

data class PersonDirectoryEntryResponse(
    val id: String,
    val preferredName: String
)
