@file:OptIn(org.readium.r2.shared.ExperimentalReadiumApi::class)

package com.bookwise.reader

import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.readium.navigator.web.reflowable.ReflowableWebRendition
import org.readium.navigator.web.reflowable.ReflowableWebRenditionController
import org.readium.r2.shared.ExperimentalReadiumApi
import com.bookwise.R
import com.bookwise.tutor.TutorSheet
import com.bookwise.tutor.TutorViewModel

private const val MENU_ASK_TUTOR = 1001

@OptIn(ExperimentalMaterial3Api::class, ExperimentalReadiumApi::class)
@Composable
fun ReaderScreen(
    onBackClick: () -> Unit,
    viewModel: ReaderViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    tutorViewModel: TutorViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val totalProgress by viewModel.totalProgress.collectAsStateWithLifecycle()
    val zoomPercent by viewModel.zoomPercent.collectAsStateWithLifecycle()
    val isFixedLayout by viewModel.isFixedLayout.collectAsStateWithLifecycle()
    val selectedText by viewModel.selectedText.collectAsStateWithLifecycle()
    val selectedLocatorJson by viewModel.selectedLocatorJson.collectAsStateWithLifecycle()
    var showToc by remember { mutableStateOf(false) }
    var showTutor by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val askTutorLabel = stringResource(R.string.reader_select_ask_tutor)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = when (val s = uiState) {
                        is ReaderUiState.Ready -> s.book.title
                        else -> stringResource(R.string.reader_title)
                    }
                    Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.reader_back)
                        )
                    }
                },
                actions = {
                    if (uiState is ReaderUiState.Ready) {
                        IconButton(onClick = { showToc = true }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                                contentDescription = stringResource(R.string.reader_contents)
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            ReaderUiState.Loading -> Box(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            is ReaderUiState.Error -> ReaderErrorContent(
                modifier = Modifier.padding(padding),
                onRetry = { viewModel.tryAgain() },
                onBack = onBackClick
            )
            is ReaderUiState.Ready -> {
                if (isFixedLayout) {
                    Box(
                        modifier = Modifier.padding(padding).fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Fixed-layout EPUB", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.padding(8.dp))
                            Text(text = "This book uses a fixed layout (comics, picture books) which is not yet supported in the reader.", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.padding(12.dp))
                            Button(onClick = onBackClick) {
                                Text(text = stringResource(R.string.reader_back))
                            }
                        }
                    }
                } else {
                    Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                        ReflowableWebRendition(
                            state = state.renditionState,
                            modifier = Modifier.weight(1f),
                            textSelectionActionModeCallback = object : ActionMode.Callback {
                                override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                                    menu?.add(0, MENU_ASK_TUTOR, 0, askTutorLabel)
                                    return true
                                }
                                override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean = false
                                override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                                    return when (item?.itemId) {
                                        MENU_ASK_TUTOR -> {
                                            scope.launch {
                                                viewModel.captureSelection()
                                                showTutor = true
                                            }
                                            mode?.finish()
                                            true
                                        }
                                        else -> false
                                    }
                                }
                                override fun onDestroyActionMode(mode: ActionMode?) {}
                            }
                        )
                        ReaderControls(
                            progression = (totalProgress ?: state.totalProgress ?: 0.0).toFloat(),
                            zoomPercent = zoomPercent,
                            onProgressionChange = { viewModel.goToProgression(it) },
                            onZoomChange = { viewModel.setZoom(it) },
                            onPrevious = {
                                val ctrl = state.renditionState.controller
                                if (ctrl != null) scope.launch { ctrl.moveBackward() }
                            },
                            onNext = {
                                val ctrl = state.renditionState.controller
                                if (ctrl != null) scope.launch { ctrl.moveForward() }
                            },
                            onOpenToc = { showToc = true }
                        )
                    }
                    if (showToc) {
                        TableOfContentsSheet(
                            entries = state.tocEntries,
                            onSelect = { entry ->
                                viewModel.goTo(entry.url)
                                showToc = false
                            },
                            onDismiss = { showToc = false }
                        )
                    }
                    if (showTutor && selectedText != null) {
                        TutorSheet(
                            bookId = state.book.id,
                            passage = selectedText!!,
                            locatorJson = selectedLocatorJson,
                            viewModel = tutorViewModel,
                            onDismiss = { showTutor = false },
                            onSave = { }
                        )
                    }
                    LaunchedEffect(state.renditionState) {
                        while (state.renditionState.controller == null) {
                            delay(50)
                        }
                        val controller = state.renditionState.controller as ReflowableWebRenditionController
                        viewModel.onControllerAvailable(controller)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderErrorContent(
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = stringResource(R.string.reader_error_title))
            Spacer(modifier = Modifier.padding(8.dp))
            Text(text = stringResource(R.string.reader_error_message))
            Spacer(modifier = Modifier.padding(12.dp))
            Button(onClick = onRetry) {
                Text(text = stringResource(R.string.reader_retry))
            }
            Spacer(modifier = Modifier.padding(8.dp))
            TextButton(onClick = onBack) {
                Text(text = stringResource(R.string.reader_back))
            }
        }
    }
}
