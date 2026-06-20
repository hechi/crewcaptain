package com.peoplemanager.application

import com.peoplemanager.domain.UserId
import com.peoplemanager.domain.UserSettings
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import java.util.UUID

class AiConfigResolverTest {

    private val userId = UserId(UUID.randomUUID())

    // --- No defaults, no user config ---

    @Test
    fun `should return null when no defaults and no user AI settings`() {
        val resolver = AiConfigResolver(
            defaultBaseUrl = "",
            defaultApiKey = "",
            defaultModel = ""
        )
        val settings = UserSettings.createDefault(userId)

        val result = resolver.resolve(settings)

        result shouldBe null
    }

    // --- Defaults only (user has no personal config) ---

    @Test
    fun `should return defaults when user has AI disabled and defaults are set`() {
        val resolver = AiConfigResolver(
            defaultBaseUrl = "http://team-ollama:11434/v1",
            defaultApiKey = "team-key",
            defaultModel = "llama3"
        )
        val settings = UserSettings.createDefault(userId) // aiEnabled=false, no personal config

        val result = resolver.resolve(settings)

        result shouldNotBe null
        result!!.baseUrl shouldBe "http://team-ollama:11434/v1"
        result.apiKey shouldBe "team-key"
        result.model shouldBe "llama3"
        result.source shouldBe AiConfigSource.ADMIN_DEFAULTS
    }

    @Test
    fun `should return defaults when user has AI disabled with no personal config`() {
        val resolver = AiConfigResolver(
            defaultBaseUrl = "http://team-ollama:11434/v1",
            defaultApiKey = "team-key",
            defaultModel = "llama3"
        )
        // User has not configured their own AI settings (aiEnabled=false, defaults apply)
        val settings = UserSettings(
            userId = userId,
            aiEnabled = false,
            aiApiBaseUrl = null,
            aiModelName = null,
            aiApiKey = null
        )

        val result = resolver.resolve(settings)

        result shouldNotBe null
        result!!.baseUrl shouldBe "http://team-ollama:11434/v1"
        result.apiKey shouldBe "team-key"
        result.model shouldBe "llama3"
        result.source shouldBe AiConfigSource.ADMIN_DEFAULTS
    }

    @Test
    fun `should return null when defaults only have base URL but no model`() {
        val resolver = AiConfigResolver(
            defaultBaseUrl = "http://team-ollama:11434/v1",
            defaultApiKey = "",
            defaultModel = ""
        )
        val settings = UserSettings.createDefault(userId)

        val result = resolver.resolve(settings)

        result shouldBe null
    }

    // --- User config takes priority ---

    @Test
    fun `should return user config when user has full AI settings`() {
        val resolver = AiConfigResolver(
            defaultBaseUrl = "http://team-ollama:11434/v1",
            defaultApiKey = "team-key",
            defaultModel = "llama3"
        )
        val settings = UserSettings(
            userId = userId,
            aiEnabled = true,
            aiApiBaseUrl = "http://my-server:11434/v1",
            aiModelName = "gpt-4o",
            aiApiKey = "my-personal-key"
        )

        val result = resolver.resolve(settings)

        result shouldNotBe null
        result!!.baseUrl shouldBe "http://my-server:11434/v1"
        result.apiKey shouldBe "my-personal-key"
        result.model shouldBe "gpt-4o"
        result.source shouldBe AiConfigSource.USER_SETTINGS
    }

    @Test
    fun `should return user config even when defaults are empty`() {
        val resolver = AiConfigResolver(
            defaultBaseUrl = "",
            defaultApiKey = "",
            defaultModel = ""
        )
        val settings = UserSettings(
            userId = userId,
            aiEnabled = true,
            aiApiBaseUrl = "http://my-server:11434/v1",
            aiModelName = "gpt-4o",
            aiApiKey = "my-key"
        )

        val result = resolver.resolve(settings)

        result shouldNotBe null
        result!!.baseUrl shouldBe "http://my-server:11434/v1"
        result.apiKey shouldBe "my-key"
        result.model shouldBe "gpt-4o"
        result.source shouldBe AiConfigSource.USER_SETTINGS
    }

    // --- Partial user config does NOT override defaults ---

    @Test
    fun `should return defaults when user has baseUrl but no model`() {
        val resolver = AiConfigResolver(
            defaultBaseUrl = "http://team-ollama:11434/v1",
            defaultApiKey = "team-key",
            defaultModel = "llama3"
        )
        // User has partial config (not isAiConfigured since aiEnabled=false)
        val settings = UserSettings(
            userId = userId,
            aiEnabled = false,
            aiApiBaseUrl = "http://my-server:11434/v1",
            aiModelName = null,
            aiApiKey = null
        )

        val result = resolver.resolve(settings)

        result shouldNotBe null
        result!!.baseUrl shouldBe "http://team-ollama:11434/v1"
        result.apiKey shouldBe "team-key"
        result.model shouldBe "llama3"
        result.source shouldBe AiConfigSource.ADMIN_DEFAULTS
    }

    // --- Defaults without API key (e.g., local Ollama) ---

    @Test
    fun `should return defaults with null api key when no key is set`() {
        val resolver = AiConfigResolver(
            defaultBaseUrl = "http://team-ollama:11434/v1",
            defaultApiKey = "",
            defaultModel = "llama3"
        )
        val settings = UserSettings.createDefault(userId)

        val result = resolver.resolve(settings)

        result shouldNotBe null
        result!!.baseUrl shouldBe "http://team-ollama:11434/v1"
        result.apiKey shouldBe null
        result.model shouldBe "llama3"
        result.source shouldBe AiConfigSource.ADMIN_DEFAULTS
    }

    // --- hasDefaults() ---

    @Test
    fun `hasDefaults should return true when base URL and model are configured`() {
        val resolver = AiConfigResolver(
            defaultBaseUrl = "http://team-ollama:11434/v1",
            defaultApiKey = "",
            defaultModel = "llama3"
        )

        resolver.hasDefaults() shouldBe true
    }

    @Test
    fun `hasDefaults should return false when base URL is empty`() {
        val resolver = AiConfigResolver(
            defaultBaseUrl = "",
            defaultApiKey = "key",
            defaultModel = "llama3"
        )

        resolver.hasDefaults() shouldBe false
    }

    @Test
    fun `hasDefaults should return false when model is empty`() {
        val resolver = AiConfigResolver(
            defaultBaseUrl = "http://team-ollama:11434/v1",
            defaultApiKey = "",
            defaultModel = ""
        )

        resolver.hasDefaults() shouldBe false
    }
}
