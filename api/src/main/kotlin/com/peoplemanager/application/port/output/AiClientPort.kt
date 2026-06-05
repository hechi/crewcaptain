package com.peoplemanager.application.port.output

/**
 * Port interface for communicating with an OpenAI-compatible LLM API.
 * Implementations handle HTTP transport and response parsing.
 */
interface AiClientPort {

    /**
     * Sends a chat completion request to the configured LLM API.
     *
     * @param baseUrl The API base URL (e.g., "http://ollama:11434/v1")
     * @param apiKey The API key for authentication (may be empty for local models)
     * @param model The model name to use (e.g., "llama3", "gpt-4o")
     * @param systemPrompt The system-level instruction for the LLM
     * @param userMessage The user-level message containing the context
     * @return The LLM's response text, or null if the request failed
     */
    fun chatCompletion(
        baseUrl: String,
        apiKey: String?,
        model: String,
        systemPrompt: String,
        userMessage: String
    ): AiCompletionResult
}

/**
 * Result of an AI completion request.
 */
sealed class AiCompletionResult {
    data class Success(val content: String) : AiCompletionResult()
    data class Error(val message: String) : AiCompletionResult()
}
