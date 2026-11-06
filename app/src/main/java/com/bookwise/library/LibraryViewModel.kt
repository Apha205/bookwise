package com.bookwise.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookwise.data.repository.BookRepository
import com.bookwise.domain.model.Book
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface LibraryUiState {
    data object Loading : LibraryUiState
    data object Empty : LibraryUiState
    data class Ready(val books: List<Book>) : LibraryUiState
    data class Error(val message: String, val books: List<Book> = emptyList()) : LibraryUiState
}

sealed interface LibraryEvent {
    data class ImportStarted(val isImporting: Boolean) : LibraryEvent
    data class ImportFinished(val outcome: BookImportHandler.ImportOutcome) : LibraryEvent
}

class LibraryViewModel(
    private val bookRepository: BookRepository,
    private val importHandler: BookImportHandler
) : ViewModel() {

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting

    val uiState: StateFlow<LibraryUiState> = bookRepository.observeBooks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        .let { books ->
            combine(books, _isImporting) { list, importing ->
                when {
                    importing -> LibraryUiState.Loading
                    list.isEmpty() -> LibraryUiState.Empty
                    else -> LibraryUiState.Ready(list)
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibraryUiState.Loading)
        }

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun clearError() { _errorMessage.value = null }

    fun importPicked(uriString: String) {
        if (uriString.isBlank()) {
            _errorMessage.value = "No file selected"
            return
        }
        viewModelScope.launch {
            _isImporting.value = true
            try {
                val outcome = importHandler.import(uriString)
                when (outcome) {
                    is BookImportHandler.ImportOutcome.Success -> _errorMessage.value = null
                    is BookImportHandler.ImportOutcome.Duplicate -> _errorMessage.value = "Already in library"
                    is BookImportHandler.ImportOutcome.Failure -> _errorMessage.value = outcome.message
                }
            } catch (e: Exception) {
                _errorMessage.value = "Import failed: ${e.message}"
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun remove(book: Book) {
        viewModelScope.launch {
            try {
                bookRepository.delete(book.id)
                java.io.File(book.internalPath).delete()
                book.coverPath?.let { java.io.File(it).delete() }
            } catch (e: Exception) {
                _errorMessage.value = "Could not remove book: ${e.message}"
            }
        }
    }

    suspend fun delete(book: Book) = remove(book)
}
