package com.bookwise.data.provider

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean = true,
    val temperature: Double? = null
)

@Serializable
data class ChatChoice(
    val index: Int = 0,
    val delta: ChatDelta? = null,
    val message: ChatMessage? = null,
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
data class ChatDelta(
    val role: String? = null,
    val content: String? = null
)

@Serializable
data class ChatResponse(
    val id: String? = null,
    val choices: List<ChatChoice> = emptyList()
)

@Serializable
data class ProviderErrorBody(
    val error: ProviderErrorDetail? = null
)

@Serializable
data class ProviderErrorDetail(
    val message: String? = null,
    val type: String? = null,
    val code: String? = null
)

@Serializable
enum class ProviderPreset(val displayName: String, val baseUrl: String) {
    OPENAI("OpenAI", "https://api.openai.com/v1"),
    ANTHROPIC("Anthropic", "https://api.anthropic.com/v1"),
    GEMINI("Google Gemini", "https://generativelanguage.googleapis.com/v1beta/openai"),
    GROQ("Groq", "https://api.groq.com/openai/v1"),
    MISTRAL("Mistral", "https://api.mistral.ai/v1"),
    OLLAMA("Ollama (local)", "http://10.0.2.2:11434/v1"),
    CUSTOM("Custom", "")
}

@Serializable
data class ProviderConfig(
    val preset: ProviderPreset = ProviderPreset.OPENAI,
    val baseUrl: String = ProviderPreset.OPENAI.baseUrl,
    val model: String = "gpt-4o-mini",
    val hasKey: Boolean = false,
    val temperature: Double = 0.7,
    val extraHeaders: Map<String, String> = emptyMap()
)

sealed class ProviderError(message: String) : Exception(message) {
    class Unauthorized(m: String = "Invalid API key") : ProviderError(m)
    class RateLimited(m: String = "Rate limited") : ProviderError(m)
    class NotFound(m: String = "Model or endpoint not found") : ProviderError(m)
    class Network(m: String = "Network error") : ProviderError(m)
    class Unknown(m: String = "Unknown error") : ProviderError(m)
}
