package com.bookwise.tutor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bookwise.domain.model.Explanation
import com.bookwise.domain.model.TutorAction
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface TutorUiState {
    data object Idle : TutorUiState
    data object Loading : TutorUiState
    data class Streaming(val text: String) : TutorUiState
    data class Done(val text: String) : TutorUiState
    data class Error(val message: String) : TutorUiState
}

class TutorViewModel(
    private val tutorRepository: TutorRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<TutorUiState>(TutorUiState.Idle)
    val uiState: StateFlow<TutorUiState> = _uiState.asStateFlow()

    private var streamJob: Job? = null
    private var currentPassage: String = ""
    private var currentLocatorJson: String? = null
    private var currentBookId: String = ""
    private var currentAction: TutorAction = TutorAction.EXPLAIN
    private var currentQuestion: String? = null
    private var currentLanguage: String? = null

    fun start(
        bookId: String,
        passage: String,
        locatorJson: String?,
        action: TutorAction,
        question: String? = null,
        targetLanguage: String? = null
    ) {
        currentBookId = bookId
        currentPassage = passage
        currentLocatorJson = locatorJson
        currentAction = action
        currentQuestion = question
        currentLanguage = targetLanguage
        _uiState.value = TutorUiState.Loading
        streamJob = viewModelScope.launch {
            val buffer = StringBuilder()
            try {
                tutorRepository.streamExplanation(action, passage, question, targetLanguage).collect { delta ->
                    buffer.append(delta)
                    _uiState.value = TutorUiState.Streaming(buffer.toString())
                }
                _uiState.value = TutorUiState.Done(buffer.toString())
            } catch (e: Exception) {
                _uiState.value = TutorUiState.Error(e.message ?: "Failed")
            }
        }
    }

    fun stop() {
        streamJob?.cancel()
        streamJob = null
    }

    fun save() {
        val text = (_uiState.value as? TutorUiState.Done)?.text
            ?: (_uiState.value as? TutorUiState.Streaming)?.text
            ?: return
        viewModelScope.launch {
            tutorRepository.saveExplanation(
                bookId = currentBookId,
                locatorJson = currentLocatorJson,
                selectedText = currentPassage,
                action = currentAction,
                prompt = currentQuestion,
                response = text
            )
        }
    }

    fun reset() {
        stop()
        _uiState.value = TutorUiState.Idle
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
