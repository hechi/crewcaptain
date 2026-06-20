package com.peoplemanager.adapters.web

import com.peoplemanager.adapters.auth.AuthenticatedUser
import com.peoplemanager.adapters.web.dto.UpdateUserSettingsRequest
import com.peoplemanager.adapters.web.dto.UserSettingsResponse
import com.peoplemanager.application.AiConfigResolver
import com.peoplemanager.application.AiConfigSource
import com.peoplemanager.application.UpdateUserSettingsCommand
import com.peoplemanager.application.UserSettingsService
import com.peoplemanager.domain.AiWritingStyle
import com.peoplemanager.domain.Theme
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1")
class UserSettingsController(
    private val userSettingsService: UserSettingsService,
    private val aiConfigResolver: AiConfigResolver
) {

    @GetMapping("/settings")
    fun getSettings(): ResponseEntity<UserSettingsResponse> {
        val userId = AuthenticatedUser.getUserId()
        val settings = userSettingsService.getSettings(userId)
        val resolvedConfig = aiConfigResolver.resolve(settings)
        return ResponseEntity.ok(UserSettingsResponse.from(settings, resolvedConfig))
    }

    @GetMapping("/settings/ai-status")
    fun getAiStatus(): ResponseEntity<AiStatusResponse> {
        val userId = AuthenticatedUser.getUserId()
        val settings = userSettingsService.getSettings(userId)
        val resolvedConfig = aiConfigResolver.resolve(settings)
        return ResponseEntity.ok(
            AiStatusResponse(
                available = resolvedConfig != null,
                source = resolvedConfig?.source?.name,
                adminDefaultsConfigured = aiConfigResolver.hasDefaults()
            )
        )
    }

    @PutMapping("/settings")
    fun updateSettings(
        @Valid @RequestBody request: UpdateUserSettingsRequest
    ): ResponseEntity<UserSettingsResponse> {
        val userId = AuthenticatedUser.getUserId()

        val theme = try {
            Theme.valueOf(request.theme.uppercase())
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.badRequest().build()
        }

        val writingStyle = try {
            AiWritingStyle.valueOf(request.aiWritingStyle.uppercase())
        } catch (e: IllegalArgumentException) {
            AiWritingStyle.NARRATIVE
        }

        val command = UpdateUserSettingsCommand(
            dueSoonDays = request.dueSoonDays,
            staleOneOnOneDays = request.staleOneOnOneDays,
            anniversaryLookaheadDays = request.anniversaryLookaheadDays,
            theme = theme,
            showAchievements = request.showAchievements,
            notifyActionItemOverdue = request.notifyActionItemOverdue,
            notifyActionItemDueSoon = request.notifyActionItemDueSoon,
            notifyStaleOneOnOne = request.notifyStaleOneOnOne,
            notifyUpcomingAnniversary = request.notifyUpcomingAnniversary,
            aiEnabled = request.aiEnabled,
            aiApiBaseUrl = request.aiApiBaseUrl,
            aiApiKey = request.aiApiKey,
            aiModelName = request.aiModelName,
            aiPrivacyMode = request.aiPrivacyMode,
            aiWritingStyle = writingStyle,
            kudosRefinementPrompt = request.kudosRefinementPrompt,
            pdpOptimizationPrompt = request.pdpOptimizationPrompt,
            agendaPrepPrompt = request.agendaPrepPrompt,
            narrativePrompt = request.narrativePrompt,
            outcomeExtractorPrompt = request.outcomeExtractorPrompt,
            trendRadarPrompt = request.trendRadarPrompt,
            linkSuggestionsPrompt = request.linkSuggestionsPrompt,
            strategyOptimizationPrompt = request.strategyOptimizationPrompt,
            triageHintPrompt = request.triageHintPrompt,
            aiAutoExecuteCommands = request.aiAutoExecuteCommands,
            commandTerminalPrompt = request.commandTerminalPrompt
        )

        val settings = userSettingsService.updateSettings(userId, command)
        val resolvedConfig = aiConfigResolver.resolve(settings)
        return ResponseEntity.ok(UserSettingsResponse.from(settings, resolvedConfig))
    }
}

data class AiStatusResponse(
    val available: Boolean,
    val source: String?,
    val adminDefaultsConfigured: Boolean
)
