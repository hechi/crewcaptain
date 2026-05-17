package com.peoplemanager.adapters.web

import com.peoplemanager.adapters.auth.AuthenticatedUser
import com.peoplemanager.adapters.web.dto.UpdateUserSettingsRequest
import com.peoplemanager.adapters.web.dto.UserSettingsResponse
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
    private val userSettingsService: UserSettingsService
) {

    @GetMapping("/settings")
    fun getSettings(): ResponseEntity<UserSettingsResponse> {
        val userId = AuthenticatedUser.getUserId()
        val settings = userSettingsService.getSettings(userId)
        return ResponseEntity.ok(UserSettingsResponse.from(settings))
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
            aiWritingStyle = writingStyle
        )

        val settings = userSettingsService.updateSettings(userId, command)
        return ResponseEntity.ok(UserSettingsResponse.from(settings))
    }
}
