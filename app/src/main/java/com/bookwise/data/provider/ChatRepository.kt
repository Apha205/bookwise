package com.bookwise.data.provider

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.setBody
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json

interface ChatRepository {
    fun streamChat(config: ProviderConfig, apiKey: String, messages: List<ChatMessage>): Flow<String>
    suspend fun testConnection(config: ProviderConfig, apiKey: String): Result<Unit>
}

class ChatRepositoryImpl : ChatRepository {

    private val json = Json { ignoreUnknownKeys = true }

    private val client = HttpClient(io.ktor.client.engine.cio.CIO) {
        install(ContentNegotiation) { json(json) }
    }

    override fun streamChat(config: ProviderConfig, apiKey: String, messages: List<ChatMessage>): Flow<String> = flow {
        val url = "${config.baseUrl.trimEnd('/')}/chat/completions"
        val response: HttpResponse = client.post(url) {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            config.extraHeaders.forEach { (k, v) -> header(k, v) }
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(ChatRequest(model = config.model, messages = messages, stream = true, temperature = config.temperature))
        }
        if (!response.status.isSuccess()) {
            throw mapHttpError(response.status.value)
        }
        val channel = response.bodyAsChannel()
        while (!channel.isClosedForRead) {
            val line = channel.readUTF8Line() ?: break
            val trimmed = line.trim()
            if (trimmed.isEmpty() || !trimmed.startsWith("data:")) continue
            val data = trimmed.removePrefix("data:").trim()
            if (data == "[DONE]") break
            runCatching {
                val chunk = json.decodeFromString<ChatResponse>(data)
                chunk.choices.firstOrNull()?.delta?.content
            }.getOrNull()?.let { emit(it) }
        }
    }

    override suspend fun testConnection(config: ProviderConfig, apiKey: String): Result<Unit> = runCatching {
        val url = "${config.baseUrl.trimEnd('/')}/chat/completions"
        val response: HttpResponse = client.post(url) {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            config.extraHeaders.forEach { (k, v) -> header(k, v) }
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(ChatRequest(model = config.model, messages = listOf(ChatMessage("user", "hi")), stream = false, temperature = 0.0))
        }
        if (!response.status.isSuccess()) throw mapHttpError(response.status.value)
    }

    private fun mapHttpError(code: Int): ProviderError = when (code) {
        401, 403 -> ProviderError.Unauthorized()
        429 -> ProviderError.RateLimited()
        404 -> ProviderError.NotFound()
        in 500..599 -> ProviderError.Network("Server error $code")
        else -> ProviderError.Unknown("HTTP $code")
    }
}
