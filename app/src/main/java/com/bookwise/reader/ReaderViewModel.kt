package com.bookwise.reader

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookwise.data.repository.BookRepository
import com.bookwise.data.repository.ReadingRepository
import com.bookwise.domain.model.Book
import com.bookwise.domain.model.ReadingProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.readium.r2.navigator.readium2.Navigator
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.LocatorCollection
import org.readium.r2.shared.publication.LocatorFragment
import org.readium.r2.shared.publication.LocatorLocations
import org.readium.r2.shared.publication.LocatorText
import org.readium.r2.shared.publication.Publication
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.util.FileAsset
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Either
import org.readium.r2.shared.util.failure
import org.readium.r2.shared.util.isLeft
import org.readium.r2.shared.util.onLeft
import java.io.File

sealed interface ReaderUiState {
    data object Loading : ReaderUiState
    data object Error : ReaderUiState
    data class Ready(
        val book: Book,
        val publication: Publication,
        val readingOrder: List<ReadiumTocEntry> = emptyList(),
        val locator: Locator? = null,
        val totalProgress: Double? = null,
        val chapterTitle: String? = null
    ) : ReaderUiState
}

data class ReadiumTocEntry(
    val title: String,
    val href: AbsoluteUrl?,
    val locator: Locator?
)

class ReaderViewModel(
    private val context: Context,
    private val bookId: String,
    private val bookRepository: BookRepository,
    private val readingRepository: ReadingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReaderUiState>(ReaderUiState.Loading)
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    private var publication: Publication? = null
    var navigator: Navigator? = null

    init {
        viewModelScope.launch {
            loadBook()
        }
    }

    private suspend fun loadBook() {
        _uiState.value = ReaderUiState.Loading
        try {
            val book = bookRepository.getById(bookId) ?: run {
                _uiState.value = ReaderUiState.Error
                return
            }
            val file = File(book.internalPath)
            if (!file.exists()) {
                _uiState.value = ReaderUiState.Error
                return
            }
            val asset = FileAsset(file, mediaType = "application/epub+zip")
            val opener = PublicationOpener()
            val openResult = opener.open(asset)
            if (openResult.isLeft) {
                _uiState.value = ReaderUiState.Error
                return
            }
            val pub = openResult.onLeft { null } ?: run {
                _uiState.value = ReaderUiState.Error
                return
            }
            publication = pub
            val readingOrder = pub.readingOrder.map { link ->
                val title = link.title ?: link.href?.toString() ?: "Unknown"
                val locator = Locator.fromLink(link)
                ReadiumTocEntry(title = title, href = link.href, locator = locator)
            }
            val savedProgress = readingRepository.getProgress(bookId)
            val initialLocator = readingOrder.firstOrNull()?.locator
            _uiState.value = ReaderUiState.Ready(
                book = book,
                publication = pub,
                readingOrder = readingOrder,
                locator = initialLocator,
                totalProgress = savedProgress?.totalProgress,
                chapterTitle = savedProgress?.chapterTitle
            )
        } catch (e: Exception) {
            _uiState.value = ReaderUiState.Error
        }
    }

    fun onReaderMoved(locator: org.readium.r2.shared.publication.Locator, progression: Float?, totalProgress: Double?, title: String?) {
        viewModelScope.launch {
            val progress = ReadingProgress(
                bookId = bookId,
                locatorJson = locator.toString(),
                progression = progression,
                chapterHref = locator.href.toString(),
                chapterTitle = title,
                totalProgress = totalProgress,
                updatedAtMillis = System.currentTimeMillis()
            )
            readingRepository.saveProgress(progress)
        }
    }

    fun goTo(entry: ReadiumTocEntry) {
        entry.locator?.let { navigator?.go(it, animated = false) }
    }

    fun goToProgression(fraction: Float) {
        val ready = _uiState.value as? ReaderUiState.Ready ?: return
        val order = ready.readingOrder
        if (order.isEmpty()) return
        val index = ((fraction * order.size).toInt()).coerceIn(0, order.size - 1)
        order[index].locator?.let { navigator?.go(it, animated = false) }
    }

    fun tryAgain() {
        viewModelScope.launch { loadBook() }
    }

    override fun onCleared() {
        super.onCleared()
        navigator?.destroy()
        publication?.close()
    }
}
