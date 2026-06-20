package com.peoplemanager.application

import com.peoplemanager.domain.UserSettings
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Resolves the effective AI configuration for a user.
 *
 * Resolution order:
 * 1. User's personal settings (if they have a complete config: baseUrl + model)
 * 2. Admin/team defaults from environment variables
 * 3. null (AI not available)
 *
 * This allows admins to provide a team-wide AI subscription via env vars,
 * while individual users can override with their own server/key/model.
 */
@Component
class AiConfigResolver(
    @Value("\${app.ai.defaults.base-url:}") private val defaultBaseUrl: String,
    @Value("\${app.ai.defaults.api-key:}") private val defaultApiKey: String,
    @Value("\${app.ai.defaults.model:}") private val defaultModel: String
) {

    /**
     * Resolves the effective AI config for a user.
     * Returns null if no usable configuration is available.
     */
    fun resolve(settings: UserSettings): ResolvedAiConfig? {
        // User has complete personal AI config (baseUrl + model set)
        if (settings.isAiConfigured()) {
            return ResolvedAiConfig(
                baseUrl = settings.aiApiBaseUrl!!,
                apiKey = settings.aiApiKey,
                model = settings.aiModelName!!,
                source = AiConfigSource.USER_SETTINGS
            )
        }

        // Fall back to admin defaults if available
        if (hasDefaults()) {
            return ResolvedAiConfig(
                baseUrl = defaultBaseUrl,
                apiKey = defaultApiKey.takeIf { it.isNotBlank() },
                model = defaultModel,
                source = AiConfigSource.ADMIN_DEFAULTS
            )
        }

        return null
    }

    /**
     * Returns true if admin defaults are configured (baseUrl + model are non-blank).
     */
    fun hasDefaults(): Boolean =
        defaultBaseUrl.isNotBlank() && defaultModel.isNotBlank()
}

/**
 * The resolved, effective AI configuration for a request.
 */
data class ResolvedAiConfig(
    val baseUrl: String,
    val apiKey: String?,
    val model: String,
    val source: AiConfigSource
)

/**
 * Indicates where the AI configuration came from.
 */
enum class AiConfigSource {
    /** User configured their own AI server/key/model */
    USER_SETTINGS,
    /** Using admin-provided defaults from environment variables */
    ADMIN_DEFAULTS
}
