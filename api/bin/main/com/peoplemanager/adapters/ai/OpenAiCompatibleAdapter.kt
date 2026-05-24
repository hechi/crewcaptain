package com.peoplemanager.adapters.ai

import com.peoplemanager.application.ports.AiClientPort
import com.peoplemanager.application.ports.AiCompletionResult
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Adapter that communicates with any OpenAI-compatible API
 * (OpenAI, Ollama, LiteLLM, vLLM, etc.).
 *
 * Uses the /chat/completions endpoint with the standard message format.
 */
@Component
class OpenAiCompatibleAdapter(
    private val objectMapper: ObjectMapper
) : AiClientPort {

    private val logger = LoggerFactory.getLogger(OpenAiCompatibleAdapter::class.java)

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    override fun chatCompletion(
        baseUrl: String,
        apiKey: String?,
        model: String,
        systemPrompt: String,
        userMessage: String
    ): AiCompletionResult {
        val url = "${baseUrl.trimEnd('/')}/chat/completions"

        val requestBody = ChatCompletionRequest(
            model = model,
            messages = listOf(
                ChatMessage(role = "system", content = systemPrompt),
                ChatMessage(role = "user", content = userMessage)
            ),
            temperature = 0.7,
            max_tokens = 1024
        )

        val jsonBody = objectMapper.writeValueAsString(requestBody)

        val requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(60))
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))

        if (!apiKey.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $apiKey")
        }

        return try {
            val response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() in 200..299) {
                val chatResponse = objectMapper.readValue(response.body(), ChatCompletionResponse::class.java)
                val content = chatResponse.choices.firstOrNull()?.message?.content
                if (content.isNullOrBlank()) {
                    AiCompletionResult.Error("AI returned an empty response.")
                } else {
                    AiCompletionResult.Success(content)
                }
            } else {
                logger.warn("AI API returned status ${response.statusCode()}: ${response.body().take(200)}")
                when (response.statusCode()) {
                    401 -> AiCompletionResult.Error("AI API authentication failed. Please check your API key in Settings.")
                    403 -> AiCompletionResult.Error("AI API access denied. Please check your API key permissions.")
                    404 -> AiCompletionResult.Error("AI API endpoint not found. Please check your API Base URL in Settings.")
                    429 -> AiCompletionResult.Error("AI API rate limit exceeded. Please try again later.")
                    else -> AiCompletionResult.Error("AI API returned an error (HTTP ${response.statusCode()}). Please check your configuration.")
                }
            }
        } catch (e: java.net.ConnectException) {
            logger.warn("Failed to connect to AI API at $url: ${e.message}")
            AiCompletionResult.Error("Cannot connect to AI API. Please verify the API Base URL is reachable.")
        } catch (e: java.net.http.HttpTimeoutException) {
            logger.warn("AI API request timed out: ${e.message}")
            AiCompletionResult.Error("AI API request timed out. The model may be loading or the server is slow.")
        } catch (e: Exception) {
            logger.error("Unexpected error calling AI API: ${e.message}", e)
            AiCompletionResult.Error("An unexpected error occurred while contacting the AI API.")
        }
    }
}

// --- Request/Response DTOs for OpenAI-compatible API ---

data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.7,
    val max_tokens: Int = 1024
)

data class ChatMessage(
    val role: String,
    val content: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ChatCompletionResponse(
    val choices: List<ChatChoice> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ChatChoice(
    val message: ChatMessage? = null
)
