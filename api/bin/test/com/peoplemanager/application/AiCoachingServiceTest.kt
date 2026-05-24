package com.peoplemanager.application

import com.peoplemanager.application.ports.AiClientPort
import com.peoplemanager.application.ports.AiCompletionResult
import com.peoplemanager.application.ports.UserSettingsRepository
import com.peoplemanager.domain.AiWritingStyle
import com.peoplemanager.domain.Theme
import com.peoplemanager.domain.UserId
import com.peoplemanager.domain.UserSettings
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.util.UUID

class AiCoachingServiceTest {

    private val userSettingsRepository: UserSettingsRepository = mockk()
    private val aiClientPort: AiClientPort = mockk()
    private val service = AiCoachingService(userSettingsRepository, aiClientPort)

    private val userId = UserId(UUID.randomUUID())
    private val configuredSettings = UserSettings(
        userId = userId,
        aiEnabled = true,
        aiApiBaseUrl = "http://localhost:11434/v1",
        aiModelName = "llama3",
        aiApiKey = "test-key"
    )

    @BeforeEach
    fun setup() {
        every { userSettingsRepository.findByUserId(userId) } returns configuredSettings
    }

    // ===== refineKudos =====

    @Test
    fun `refineKudos should return refined text on success`() {
        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns
            AiCompletionResult.Success("Refined: Great job on the presentation!")

        val result = service.refineKudos(userId, "Good job on the presentation")

        result.shouldBeInstanceOf<AiCoachingResult.Success>()
        result.content shouldBe "Refined: Great job on the presentation!"
    }

    @Test
    fun `refineKudos should use default prompt when no custom prompt is set`() {
        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns
            AiCompletionResult.Success("Refined text")

        service.refineKudos(userId, "Draft text")

        verify {
            aiClientPort.chatCompletion(
                baseUrl = "http://localhost:11434/v1",
                apiKey = "test-key",
                model = "llama3",
                systemPrompt = UserSettings.DEFAULT_KUDOS_REFINEMENT_PROMPT,
                userMessage = "Draft: Draft text"
            )
        }
    }

    @Test
    fun `refineKudos should use custom prompt when set`() {
        val customPrompt = "Be a radical candor coach. Draft: {{draft}}"
        val settingsWithCustomPrompt = configuredSettings.copy(kudosRefinementPrompt = customPrompt)
        every { userSettingsRepository.findByUserId(userId) } returns settingsWithCustomPrompt
        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns
            AiCompletionResult.Success("Custom refined text")

        service.refineKudos(userId, "My draft")

        verify {
            aiClientPort.chatCompletion(
                baseUrl = "http://localhost:11434/v1",
                apiKey = "test-key",
                model = "llama3",
                systemPrompt = customPrompt,
                userMessage = "Draft: My draft"
            )
        }
    }

    @Test
    fun `refineKudos should return error when AI is not configured`() {
        every { userSettingsRepository.findByUserId(userId) } returns null

        val result = service.refineKudos(userId, "Draft text")

        result.shouldBeInstanceOf<AiCoachingResult.Error>()
        result.message shouldBe "AI Assistant is not configured. Please configure it in Settings."
    }

    @Test
    fun `refineKudos should return error when AI is disabled`() {
        every { userSettingsRepository.findByUserId(userId) } returns configuredSettings.copy(aiEnabled = false)

        val result = service.refineKudos(userId, "Draft text")

        result.shouldBeInstanceOf<AiCoachingResult.Error>()
        result.message shouldBe "AI Assistant is not configured. Please configure it in Settings."
    }

    @Test
    fun `refineKudos should return error when draft is blank`() {
        val result = service.refineKudos(userId, "   ")

        result.shouldBeInstanceOf<AiCoachingResult.Error>()
        result.message shouldBe "Kudos draft cannot be empty."
    }

    @Test
    fun `refineKudos should return error when AI API fails`() {
        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns
            AiCompletionResult.Error("Connection refused")

        val result = service.refineKudos(userId, "Draft text")

        result.shouldBeInstanceOf<AiCoachingResult.Error>()
        result.message shouldBe "Connection refused"
    }

    // ===== optimizePdpGoal =====

    @Test
    fun `optimizePdpGoal should return optimized goal on success`() {
        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns
            AiCompletionResult.Success("Title: Improve public speaking\nDescription: Present at 2 team meetings per month")

        val result = service.optimizePdpGoal(userId, "Get better at speaking", "Practice more")

        result.shouldBeInstanceOf<AiCoachingResult.Success>()
        result.content shouldBe "Title: Improve public speaking\nDescription: Present at 2 team meetings per month"
    }

    @Test
    fun `optimizePdpGoal should use default prompt when no custom prompt is set`() {
        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns
            AiCompletionResult.Success("Optimized")

        service.optimizePdpGoal(userId, "My goal", "My description")

        verify {
            aiClientPort.chatCompletion(
                baseUrl = "http://localhost:11434/v1",
                apiKey = "test-key",
                model = "llama3",
                systemPrompt = UserSettings.DEFAULT_PDP_OPTIMIZATION_PROMPT,
                userMessage = "Title: My goal\nDescription: My description"
            )
        }
    }

    @Test
    fun `optimizePdpGoal should use custom prompt when set`() {
        val customPrompt = "Rewrite for executive review. Focus on business value."
        val settingsWithCustomPrompt = configuredSettings.copy(pdpOptimizationPrompt = customPrompt)
        every { userSettingsRepository.findByUserId(userId) } returns settingsWithCustomPrompt
        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns
            AiCompletionResult.Success("Executive goal")

        service.optimizePdpGoal(userId, "My goal", null)

        verify {
            aiClientPort.chatCompletion(
                baseUrl = "http://localhost:11434/v1",
                apiKey = "test-key",
                model = "llama3",
                systemPrompt = customPrompt,
                userMessage = "Title: My goal"
            )
        }
    }

    @Test
    fun `optimizePdpGoal should handle null description`() {
        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns
            AiCompletionResult.Success("Optimized")

        service.optimizePdpGoal(userId, "My goal", null)

        verify {
            aiClientPort.chatCompletion(
                baseUrl = any(),
                apiKey = any(),
                model = any(),
                systemPrompt = any(),
                userMessage = "Title: My goal"
            )
        }
    }

    @Test
    fun `optimizePdpGoal should return error when title is blank`() {
        val result = service.optimizePdpGoal(userId, "  ", "description")

        result.shouldBeInstanceOf<AiCoachingResult.Error>()
        result.message shouldBe "Goal title cannot be empty."
    }

    @Test
    fun `optimizePdpGoal should return error when AI is not configured`() {
        every { userSettingsRepository.findByUserId(userId) } returns null

        val result = service.optimizePdpGoal(userId, "My goal", "desc")

        result.shouldBeInstanceOf<AiCoachingResult.Error>()
        result.message shouldBe "AI Assistant is not configured. Please configure it in Settings."
    }

    @Test
    fun `optimizePdpGoal should return error when AI API fails`() {
        every { aiClientPort.chatCompletion(any(), any(), any(), any(), any()) } returns
            AiCompletionResult.Error("Timeout")

        val result = service.optimizePdpGoal(userId, "My goal", "desc")

        result.shouldBeInstanceOf<AiCoachingResult.Error>()
        result.message shouldBe "Timeout"
    }
}
