package com.bookwise.tutor

import com.bookwise.data.provider.ChatRepository
import com.bookwise.data.provider.ProviderRepository
import com.bookwise.data.repository.ExplanationRepository
import com.bookwise.domain.model.Explanation
import com.bookwise.domain.model.TutorAction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class TutorRepository(
    private val chatRepository: ChatRepository,
    private val providerRepository: ProviderRepository,
    private val explanationRepository: ExplanationRepository
) {
    fun streamChat(messages: List<com.bookwise.data.provider.ChatMessage>): Flow<String> {
        return kotlinx.coroutines.flow.flow {
            val config = providerRepository.config.first()
            val key = providerRepository.loadKey() ?: throw IllegalStateException("No API key configured")
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
}

