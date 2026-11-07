package com.bookwise.tutor

import com.bookwise.data.preferences.AppPreferences
import com.bookwise.data.provider.ChatRepository
import com.bookwise.data.provider.ProviderConfig
import com.bookwise.data.provider.ProviderRepository
import com.bookwise.data.repository.ExplanationRepository
import com.bookwise.domain.model.Explanation
import com.bookwise.domain.model.TutorAction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class TutorRepository(
    private val chatRepository: ChatRepository,
    private val providerRepository: ProviderRepository,
    private val explanationRepository: ExplanationRepository
) {
    fun streamExplanation(
        action: TutorAction,
        passage: String,
        question: String?,
        targetLanguage: String?
    ): Flow<String> {
        return kotlinx.coroutines.flow.flow {
            val config = providerRepository.config.first()
            val key = providerRepository.loadKey() ?: throw IllegalStateException("No API key")
            val messages = buildMessages(action, passage, question, targetLanguage)
            chatRepository.streamChat(config, key, messages).collect { emit(it) }
        }
    }

    suspend fun saveExplanation(
        bookId: String,
        locatorJson: String?,
        selectedText: String,
        action: TutorAction,
        prompt: String?,
        response: String
    ): String = explanationRepository.save(bookId, locatorJson, selectedText, action, prompt, response)

    suspend fun getExplanations(bookId: String): List<Explanation> =
        explanationRepository.getForBook(bookId)

    suspend fun deleteExplanation(id: String) = explanationRepository.delete(id)

    private fun buildMessages(
        action: TutorAction,
        passage: String,
        question: String?,
        targetLanguage: String?
    ): List<com.bookwise.data.provider.ChatMessage> {
        val trimmedPassage = if (passage.length > 2000) passage.take(2000) + "…" else passage
        val system = if (question.isNullOrBlank()) {
            "You are a helpful reading tutor. Explain the given passage from a book in clear, accessible terms. Keep it concise."
        } else {
            "You are a helpful reading tutor. The user asks: \"$question\". Answer based on the given passage. Keep it concise."
        }
        val userContent = buildString {
            append("Passage:\n\"\"\"\n").append(trimmedPassage).append("\n\"\"\"\n")
            if (question != null) append("\nQuestion: ").append(question)
        }
        return listOf(
            com.bookwise.data.provider.ChatMessage("system", system),
            com.bookwise.data.provider.ChatMessage("user", userContent)
        )
    }
}
