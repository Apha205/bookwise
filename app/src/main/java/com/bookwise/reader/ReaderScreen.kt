@file:OptIn(org.readium.r2.shared.ExperimentalReadiumApi::class)

package com.bookwise.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.launch
import org.readium.navigator.web.reflowable.ReflowableWebRendition
import org.readium.navigator.web.reflowable.ReflowableWebRenditionController
import org.readium.r2.shared.ExperimentalReadiumApi
import com.bookwise.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalReadiumApi::class)
@Composable
fun ReaderScreen(
    onBackClick: () -> Unit,
    viewModel: ReaderViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val totalProgress by viewModel.totalProgress.collectAsStateWithLifecycle()
    var showToc by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

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
                                imageVector = Icons.Filled.FormatListBulleted,
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
                Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                    ReflowableWebRendition(
                        state = state.renditionState,
                        modifier = Modifier.weight(1f)
                    )
                    ReaderControls(
                        progression = (totalProgress ?: state.totalProgress ?: 0.0).toFloat(),
                        onProgressionChange = { viewModel.goToProgression(it) },
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

                // Observe controller availability + location
                LaunchedEffect(state.renditionState) {
                    // wait for controller
                    while (state.renditionState.controller == null) {
                        kotlinx.coroutines.delay(50)
                    }
                    val controller = state.renditionState.controller as ReflowableWebRenditionController
                    viewModel.onControllerAvailable(controller)
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
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(8.dp))
            Text(text = stringResource(R.string.reader_error_message))
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(12.dp))
            Button(onClick = onRetry) {
                Text(text = stringResource(R.string.reader_retry))
            }
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(8.dp))
            TextButton(onClick = onBack) {
                Text(text = stringResource(R.string.reader_back))
            }
        }
    }
}
