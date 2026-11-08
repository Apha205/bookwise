package com.bookwise.data.provider

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
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
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 60_000
        }
    }

    override fun streamChat(config: ProviderConfig, apiKey: String, messages: List<ChatMessage>): Flow<String> = flow {
        var completed = false
        var lastError: Exception? = null
        var attempt = 0
        while (!completed && attempt < 2) {
            attempt++
            try {
                val url = "${config.baseUrl.trimEnd('/')}/chat/completions"
                val response: HttpResponse = client.post(url) {
                    header(HttpHeaders.Authorization, "Bearer $apiKey")
                    config.extraHeaders.forEach { (k, v) -> header(k, v) }
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(ChatRequest(model = config.model, messages = messages, stream = true, temperature = config.temperature))
                }
                if (response.status.value == 429 && attempt == 1) {
                    kotlinx.coroutines.delay(2000)
                    continue
                }
                if (!response.status.isSuccess()) {
                    throw mapHttpError(response.status.value)
                }
                val channel = response.bodyAsChannel()
                val fullBody = StringBuilder()
                while (!channel.isClosedForRead) {
                    val line = channel.readUTF8Line() ?: break
                    fullBody.appendLine(line)
                    val trimmed = line.trim()
                    if (trimmed.isEmpty()) continue
                    if (!trimmed.startsWith("data:")) {
                        Log.d("ChatRepository", "non-SSE line: $trimmed")
                        continue
                    }
                    val data = trimmed.removePrefix("data:").trim()
                    if (data == "[DONE]") break
                    Log.d("ChatRepository", "SSE data: $data")
                    runCatching {
                        val errorBody = json.decodeFromString<ProviderErrorBody>(data)
                        errorBody.error?.message?.let { throw ProviderError.Unknown(it) }
                    }
                    runCatching {
                        val chunk = json.decodeFromString<ChatResponse>(data)
                        chunk.choices.firstOrNull()?.delta?.content
                            ?: chunk.choices.firstOrNull()?.message?.content
                    }.onFailure { e ->
                        Log.w("ChatRepository", "parse failed: ${e.message} for data: $data")
                    }.getOrNull()?.let { emit(it) }
                }
                if (fullBody.toString().trim().startsWith("{")) {
                    runCatching {
                        val resp = json.decodeFromString<ChatResponse>(fullBody.toString().trim())
                        resp.choices.firstOrNull()?.message?.content
                            ?: resp.choices.firstOrNull()?.delta?.content
                    }.getOrNull()?.let { emit(it) }
                }
                completed = true
            } catch (e: Exception) {
                lastError = e
                if (attempt == 1 || e !is ProviderError.RateLimited) {
                    throw e
                }
                kotlinx.coroutines.delay(2000)
            }
        }
        lastError?.let { throw it }
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
        val body = response.bodyAsText()
        Log.d("ChatRepository", "test response: $body")
        runCatching {
            val err = json.decodeFromString<ProviderErrorBody>(body)
            err.error?.message?.let { throw ProviderError.Unknown(it) }
        }
    }

    private fun mapHttpError(code: Int): ProviderError = when (code) {
        401, 403 -> ProviderError.Unauthorized()
        429 -> ProviderError.RateLimited()
        404 -> ProviderError.NotFound()
        in 500..599 -> ProviderError.Network("Server error $code")
        else -> ProviderError.Unknown("HTTP $code")
    }
}
