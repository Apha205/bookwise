package com.bookwise.reader

import android.webkit.WebView
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bookwise.R
import org.readium.r2.navigator.readium2.Navigator
import org.readium.r2.navigator.readium2.web.WebViewNavigatorFactory
import org.readium.r2.shared.publication.Locator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    onBackClick: () -> Unit,
    viewModel: ReaderViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showToc by remember { mutableStateOf(false) }

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
            ReaderUiState.Error -> ReaderErrorContent(
                modifier = Modifier.padding(padding),
                onRetry = { viewModel.tryAgain() },
                onBack = onBackClick
            )
            is ReaderUiState.Ready -> {
                Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                    ReadiumReaderContent(
                        modifier = Modifier.weight(1f),
                        viewModel = viewModel,
                        state = state
                    )
                    ReaderControls(
                        progression = 0f,
                        onProgressionChange = { viewModel.goToProgression(it) },
                        onPrevious = { viewModel.navigator?.moveToPrevious() },
                        onNext = { viewModel.navigator?.moveToNext() },
                        onOpenToc = { showToc = true }
                    )
                }
                if (showToc) {
                    TableOfContentsSheet(
                        entries = state.readingOrder,
                        onSelect = { entry ->
                            viewModel.goTo(entry)
                            showToc = false
                        },
                        onDismiss = { showToc = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun ReadiumReaderContent(
    viewModel: ReaderViewModel,
    state: ReaderUiState.Ready,
    modifier: Modifier = Modifier
) {
    val navigatorRef = remember { arrayOfNulls<Navigator>(1) }

    DisposableEffect(state.publication) {
        onDispose {
            navigatorRef[0]?.destroy()
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            val webView = WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
            }
            val factory = WebViewNavigatorFactory(
                webView = webView,
                listener = object : org.readium.r2.navigator.readium2.Navigator.Listener {
                    override fun onReadingLocationChanged(locator: Locator) {}
                    override fun onLinkActivated(locator: Locator, transition: org.readium.r2.navigator.readium2.Navigator.LinkTransition): Boolean = false
                    override fun onViewportChanged(locator: Locator, progression: Float, totalProgress: Double) {
                        viewModel.onReaderMoved(locator, progression, totalProgress, null)
                    }
                    override fun onContentLoaded(locator: Locator) {}
                },
                configuration = null
            )
            val navigator = factory.createNavigator(caller = ctx)
            navigatorRef[0] = navigator
            viewModel.navigator = navigator

            if (state.locator != null) {
                navigator.go(state.locator, animated = false)
            }
            webView
        },
        update = { _ -> }
    )
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
