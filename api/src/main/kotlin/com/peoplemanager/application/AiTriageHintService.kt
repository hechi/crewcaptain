package com.peoplemanager.application

import com.peoplemanager.application.port.output.AiClientPort
import com.peoplemanager.application.port.output.AiCompletionResult
import com.peoplemanager.application.port.output.UserSettingsRepository
import com.peoplemanager.domain.TriageItem
import com.peoplemanager.domain.TriageItemType
import com.peoplemanager.domain.UserId
import org.springframework.stereotype.Service

@Service
class AiTriageHintService(
    private val aiClientPort: AiClientPort,
    private val userSettingsRepository: UserSettingsRepository
) {

    data class TriageHintResult(
        val hint: String? = null,
        val error: String? = null
    )

    /**
     * Generate an AI hint for a triage item.
     * Returns null hint if:
     * - AI is not enabled/configured
     * - Privacy mode is ON and item is sensitive
     */
    fun generateHint(userId: UserId, item: TriageItem): TriageHintResult {
        val settings = userSettingsRepository.findByUserId(userId)
            ?: return TriageHintResult(error = "Settings not found")

        if (!settings.isAiConfigured()) {
            return TriageHintResult(error = "AI not configured")
        }

        // Respect privacy mode: skip sensitive items
        if (settings.aiPrivacyMode && item.sensitive) {
            return TriageHintResult(hint = null, error = null)
        }

        val systemPrompt = settings.effectiveTriageHintPrompt()
        val userMessage = buildUserMessage(item)

        return when (val result = aiClientPort.chatCompletion(
            baseUrl = settings.aiApiBaseUrl!!,
            apiKey = settings.aiApiKey,
            model = settings.aiModelName!!,
            systemPrompt = systemPrompt,
            userMessage = userMessage
        )) {
            is AiCompletionResult.Success -> TriageHintResult(hint = result.content.trim())
            is AiCompletionResult.Error -> TriageHintResult(error = result.message)
        }
    }

    private fun buildUserMessage(item: TriageItem): String {
        return buildString {
            appendLine("Triage Item:")
            appendLine("- Type: ${item.type.name}")
            appendLine("- Title: ${item.title}")
            appendLine("- Person: ${item.personName}")
            item.dueDate?.let { appendLine("- Due Date: $it") }
            item.daysOverdue?.let { appendLine("- Days Overdue: $it") }
            item.daysUntilDue?.let { appendLine("- Days Until Due: $it") }
            item.ownerType?.let { appendLine("- Owner: $it") }
            when (item.type) {
                TriageItemType.ACTION_ITEM_OVERDUE ->
                    appendLine("- Context: This action item is overdue and needs immediate attention.")
                TriageItemType.ACTION_ITEM_DUE_SOON ->
                    appendLine("- Context: This action item is due soon.")
                TriageItemType.STALE_ONE_ON_ONE ->
                    appendLine("- Context: The 1:1 cadence has been missed.")
                TriageItemType.UPCOMING_ANNIVERSARY ->
                    appendLine("- Context: Work anniversary is coming up.")
            }
        }
    }
}
