package com.bookwise.tutor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bookwise.data.provider.ChatMessage
import com.bookwise.data.provider.ChatRepository
import com.bookwise.data.provider.ProviderRepository
import com.bookwise.data.repository.ExplanationRepository
import com.bookwise.domain.model.Explanation
import com.bookwise.domain.model.TutorAction
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TutorChatMessage(val role: String, val content: String)

class TutorViewModel(
    private val tutorRepository: TutorRepository
) : ViewModel() {

    private val _messages = MutableStateFlow<List<TutorChatMessage>>(emptyList())
    val messages: StateFlow<List<TutorChatMessage>> = _messages.asStateFlow()

    private val _streamingText = MutableStateFlow("")
    val streamingText: StateFlow<String> = _streamingText.asStateFlow()

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var passage = ""
    private var bookId = ""
    private var locatorJson: String? = null
    private var streamJob: Job? = null

    fun init(passage: String, bookId: String, locatorJson: String?) {
        this.passage = passage
        this.bookId = bookId
        this.locatorJson = locatorJson
        _messages.value = emptyList()
        _streamingText.value = ""
        _error.value = null
        _isStreaming.value = false
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || _isStreaming.value) return
        val userMsg = TutorChatMessage("user", text.trim())
        _messages.value = _messages.value + userMsg
        _isStreaming.value = true
        _streamingText.value = ""
        _error.value = null

        streamJob = viewModelScope.launch {
            val buffer = StringBuilder()
            try {
                val chatMessages = buildChatMessages()
                tutorRepository.streamChat(chatMessages).collect { delta ->
                    buffer.append(delta)
                    _streamingText.value = buffer.toString()
                }
                if (buffer.isNotEmpty()) {
                    _messages.value = _messages.value + TutorChatMessage("assistant", buffer.toString())
                } else {
                    _error.value = "No response received. Check the model name and API key."
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed"
            } finally {
                _streamingText.value = ""
                _isStreaming.value = false
            }
        }
    }

    private fun buildChatMessages(): List<ChatMessage> {
        val systemPrompt = "You are a helpful reading tutor. The user selected this passage from a book:\n\n\"\"\"\n$passage\n\"\"\"\n\nAnswer their questions about this passage. Be concise and helpful."
        val msgs = mutableListOf(ChatMessage("system", systemPrompt))
        _messages.value.forEach { msgs.add(ChatMessage(it.role, it.content)) }
        return msgs
    }

    fun stop() {
        streamJob?.cancel()
        streamJob = null
        _isStreaming.value = false
        if (_streamingText.value.isNotEmpty()) {
            _messages.value = _messages.value + TutorChatMessage("assistant", _streamingText.value)
            _streamingText.value = ""
        }
    }

    fun save() {
        val lastAssistant = _messages.value.lastOrNull { it.role == "assistant" }?.content ?: return
        val lastUser = _messages.value.lastOrNull { it.role == "user" }?.content
        viewModelScope.launch {
            tutorRepository.saveExplanation(
                bookId = bookId,
                locatorJson = locatorJson,
                selectedText = passage,
                action = TutorAction.EXPLAIN,
                prompt = lastUser,
                response = lastAssistant
            )
        }
    }

    fun reset() {
        stop()
        _messages.value = emptyList()
        _streamingText.value = ""
        _error.value = null
    }

    companion object {
        fun factory(tutorRepository: TutorRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(TutorViewModel::class.java))
                    return TutorViewModel(tutorRepository) as T
                }
            }
    }
}
