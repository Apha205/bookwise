@file:OptIn(org.readium.r2.shared.ExperimentalReadiumApi::class)

package com.bookwise.reader

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewModelScope
import com.bookwise.BookwiseApp
import com.bookwise.data.repository.BookRepository
import com.bookwise.data.repository.ReadingRepository
import com.bookwise.domain.model.Book
import com.bookwise.domain.model.ReadingProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.readium.navigator.web.reflowable.ReflowableWebGoLocation
import org.readium.navigator.web.reflowable.ReflowableWebRenditionController
import org.readium.navigator.web.reflowable.ReflowableWebRenditionFactory
import org.readium.navigator.web.reflowable.ReflowableWebRenditionState
import org.readium.navigator.common.Selection
import org.readium.navigator.web.reflowable.ReflowableWebSelectionLocation
import org.readium.navigator.web.reflowable.preferences.ReflowableWebPreferences
import org.readium.navigator.web.reflowable.preferences.ReflowableWebSettings
import org.readium.r2.navigator.preferences.ReadingProgression
import androidx.compose.runtime.snapshotFlow
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.locateProgression
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.toUrl
import org.json.JSONObject
import java.io.File

sealed interface ReaderUiState {
    data object Loading : ReaderUiState
    data class Error(val message: String) : ReaderUiState
    data class Ready(
        val book: Book,
        val publication: Publication,
        val renditionState: ReflowableWebRenditionState,
        val tocEntries: List<TocEntry> = emptyList(),
        val totalProgress: Double? = null,
        val chapterTitle: String? = null
    ) : ReaderUiState
}

data class TocEntry(val title: String, val url: Url)

class ReaderViewModel(
    private val application: Application,
    private val bookId: String,
    private val bookRepository: BookRepository,
    private val readingRepository: ReadingRepository
) : ViewModel() {

    private val app = application as BookwiseApp

    private val _uiState = MutableStateFlow<ReaderUiState>(ReaderUiState.Loading)
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    private val _totalProgress = MutableStateFlow<Double?>(null)
    val totalProgress: StateFlow<Double?> = _totalProgress.asStateFlow()

    private var publication: Publication? = null
    private var renditionState: ReflowableWebRenditionState? = null
    private var chapterTitlesByHref: Map<String, String> = emptyMap()

    init {
        viewModelScope.launch { loadBook() }
    }

    private suspend fun loadBook() {
        _uiState.value = ReaderUiState.Loading
        try {
            val book = bookRepository.getById(bookId) ?: run {
                _uiState.value = ReaderUiState.Error("Book not found")
                return
            }
            val file = File(book.internalPath)
            if (!file.exists()) {
                _uiState.value = ReaderUiState.Error("File missing")
                return
            }
            val url = file.toUrl(false)
            val asset = app.assetRetriever.retrieve(url).getOrElse {
                _uiState.value = ReaderUiState.Error(it.message)
                return
            }
            val pub = app.publicationOpener.open(asset, allowUserInteraction = false).getOrElse {
                _uiState.value = ReaderUiState.Error(it.message)
                return
            }
            publication = pub

            val savedProgress = readingRepository.getProgress(bookId)
            val initialLocator = savedProgress?.locatorJson?.let { json ->
                try { Locator.fromJSON(org.json.JSONObject(json)) } catch (e: Exception) { null }
            }

            val factory = ReflowableWebRenditionFactory(
                application = application,
                publication = pub,
                configuration = org.readium.navigator.web.reflowable.ReflowableWebConfiguration()
            )
            if (factory == null) {
                _uiState.value = ReaderUiState.Error("Unsupported publication")
                pub.close()
                return
            }

            val fontScale = try { app.settingsRepository.fontScaleFlow().first() } catch (e: Exception) { 1.0f }
            val zoomLevel = try { app.settingsRepository.zoomLevelFlow().first() } catch (e: Exception) { 1.0f }
            _fontScale.value = fontScale
            _zoomPercent.value = (zoomLevel * 100).toInt().coerceIn(80, 200)
            val initialSettings = ReflowableWebSettings(
                backgroundColor = ReflowableWebPreferences.LightTheme.backgroundColor!!,
                columnCount = null,
                fontFamily = null,
                fontSize = fontScale.toDouble() * zoomLevel.toDouble(),
                fontWeight = null,
                hyphens = null,
                imageFilter = null,
                language = null,
                letterSpacing = null,
                ligatures = null,
                lineHeight = null,
                linkColor = ReflowableWebPreferences.LightTheme.linkColor!!,
                maximalLineLength = null,
                minimalLineLength = null,
                minMargins = 1.0,
                optimalLineLength = 1.0,
                overridePublisherColors = false,
                paragraphIndent = null,
                paragraphSpacing = null,
                readingProgression = ReadingProgression.LTR,
                scroll = false,
                textAlign = null,
                textColor = ReflowableWebPreferences.LightTheme.textColor!!,
                textNormalization = false,
                verticalText = false,
                visitedColor = ReflowableWebPreferences.LightTheme.visitedColor!!,
                wordSpacing = null
            )

            val initialLocation = initialLocator?.let { ReflowableWebGoLocation(it) }
            val state = factory.createRenditionState(
                initialSettings = initialSettings,
                initialLocation = initialLocation
            ).getOrElse {
                _uiState.value = ReaderUiState.Error("Could not render book")
                pub.close()
                return
            }
            renditionState = state

            val tocEntries = pub.tableOfContents.map { link ->
                TocEntry(title = link.title ?: link.url().toString(), url = link.url())
            }
            chapterTitlesByHref = tocEntries.associate { it.url.toString() to it.title }

            _uiState.value = ReaderUiState.Ready(
                book = book,
                publication = pub,
                renditionState = state,
                tocEntries = tocEntries,
                totalProgress = savedProgress?.totalProgress,
                chapterTitle = savedProgress?.chapterTitle
            )
            _totalProgress.value = savedProgress?.totalProgress
        } catch (e: Exception) {
            _uiState.value = ReaderUiState.Error(e.message ?: "Unknown error")
        }
    }

    fun onControllerAvailable(controller: ReflowableWebRenditionController) {
        viewModelScope.launch {
            snapshotFlow { controller.location }.collect { loc ->
                onLocationChanged(loc)
            }
        }
    }

    private suspend fun onLocationChanged(loc: org.readium.navigator.web.reflowable.ReflowableWebLocation) {
        val progress = ReadingProgress(
            bookId = bookId,
            locatorJson = loc.toLocator().toJSON().toString(),
            progression = loc.progression.value.toFloat(),
            chapterHref = loc.href.toString(),
            chapterTitle = chapterTitlesByHref[loc.href.toString()],
            totalProgress = loc.totalProgression.value,
            updatedAtMillis = System.currentTimeMillis()
        )
        try { readingRepository.saveProgress(progress) } catch (e: Exception) { }
        _totalProgress.value = loc.totalProgression.value
    }

    fun goToProgression(fraction: Float) {
        val pub = publication ?: return
        val ctrl = renditionState?.controller ?: return
        viewModelScope.launch {
            pub.locateProgression(fraction.toDouble())?.let { ctrl.goTo(ReflowableWebGoLocation(it)) }
        }
    }

    fun goTo(url: Url) {
        val ctrl = renditionState?.controller ?: return
        viewModelScope.launch { ctrl.goTo(url) }
    }

    private val _selectedText = MutableStateFlow<String?>(null)
    val selectedText: StateFlow<String?> = _selectedText.asStateFlow()

    private val _selectedLocatorJson = MutableStateFlow<String?>(null)
    val selectedLocatorJson: StateFlow<String?> = _selectedLocatorJson.asStateFlow()

    suspend fun captureSelection() {
        val ctrl = renditionState?.controller ?: return
        val sel = ctrl.currentSelection() ?: return
        val locator = sel.location.toLocator()
        _selectedText.value = locator.text.highlight
        _selectedLocatorJson.value = locator.toJSON().toString()
    }

    private val _fontScale = MutableStateFlow(1.0f)
    private val _zoomPercent = MutableStateFlow(100)
    val zoomPercent: StateFlow<Int> = _zoomPercent.asStateFlow()

    private fun applyFontSize() {
        val ctrl = renditionState?.controller ?: return
        ctrl.settings = ctrl.settings.copy(fontSize = _fontScale.value.toDouble() * (_zoomPercent.value / 100.0))
    }

    init {
        viewModelScope.launch { loadBook() }
        viewModelScope.launch {
            app.settingsRepository.fontScaleFlow().collect { scale ->
                _fontScale.value = scale
                applyFontSize()
            }
        }
        viewModelScope.launch {
            app.settingsRepository.zoomLevelFlow().collect { level ->
                _zoomPercent.value = (level * 100).toInt().coerceIn(80, 200)
                applyFontSize()
            }
        }
    }

    fun tryAgain() {
        viewModelScope.launch { loadBook() }
    }

    fun setZoom(percent: Int) {
        val clamped = percent.coerceIn(80, 200)
        _zoomPercent.value = clamped
        applyFontSize()
        viewModelScope.launch { app.settingsRepository.setZoomLevel(clamped / 100.0f) }
    }

    fun clearSelection() {
        _selectedText.value = null
        _selectedLocatorJson.value = null
        viewModelScope.launch {
            renditionState?.controller?.clearSelection()
        }
    }

    override fun onCleared() {
        super.onCleared()
        publication?.close()
    }

    companion object {
        fun factory(
            application: Application,
            bookId: String,
            bookRepository: BookRepository,
            readingRepository: ReadingRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                require(modelClass.isAssignableFrom(ReaderViewModel::class.java))
                return ReaderViewModel(application, bookId, bookRepository, readingRepository) as T
            }
        }
    }
}
