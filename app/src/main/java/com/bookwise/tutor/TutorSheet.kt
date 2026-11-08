package com.bookwise.tutor

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorSheet(
    passage: String,
    viewModel: TutorViewModel,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val streamingText by viewModel.streamingText.collectAsStateWithLifecycle()
    val isStreaming by viewModel.isStreaming.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, streamingText) {
        val count = messages.size + if (streamingText.isNotEmpty()) 1 else 0
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    ModalBottomSheet(
        onDismissRequest = { viewModel.reset(); onDismiss() },
        sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            Text(
                text = "Tutor",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = { viewModel.reset(); onDismiss() })
                    }
                    .padding(vertical = 4.dp)
            )

            // Full selected passage (scrollable, stays visible)
            Text(
                text = "\u201C$passage\u201D",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 80.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Chat area (scrollable, takes available space)
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .heightIn(min = 60.dp, max = 240.dp)
                    .pointerInput(listState) {
                        detectVerticalDragGestures { change, dragAmount ->
                            val atTop = listState.firstVisibleItemIndex == 0 &&
                                listState.firstVisibleItemScrollOffset == 0
                            if (atTop && dragAmount > 0) {
                                // Consume downward drags at top to prevent swipe-to-dismiss
                                change.consume()
                            }
                        }
                    },
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { msg ->
                    ChatBubble(role = msg.role, text = msg.content)
                }
                if (isStreaming && streamingText.isNotEmpty()) {
                    item {
                        ChatBubble(role = "assistant", text = streamingText, isStreaming = true)
                    }
                }
                if (isStreaming && streamingText.isEmpty()) {
                    item { BreathingDots() }
                }
                if (error != null) {
                    item {
                        Text(text = error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Fixed input row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("Ask anything about this passage...") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    enabled = !isStreaming
                )
                IconButton(
                    onClick = {
                        viewModel.sendMessage(input)
                        input = ""
                    },
                    enabled = input.isNotBlank() && !isStreaming
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(role: String, text: String, isStreaming: Boolean = false) {
    val isUser = role == "user"
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Text(
            text = if (isUser) "You" else "Tutor",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = parseMarkdown(text + if (isStreaming) " \u2026" else ""),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun BreathingDots() {
    val transition = rememberInfiniteTransition(label = "dots")
    val alpha1 by transition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "d1"
    )
    val alpha2 by transition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 200), RepeatMode.Reverse), label = "d2"
    )
    val alpha3 by transition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 400), RepeatMode.Reverse), label = "d3"
    )
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        listOf(alpha1, alpha2, alpha3).forEach { a ->
            Box(modifier = Modifier.alpha(a)) {
                Text(text = "\u2022", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

private fun parseMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    val boldRegex = Regex("\\*\\*(.+?)\\*\\*")
    val italicRegex = Regex("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)")
    var lastIndex = 0
    val matches = (boldRegex.findAll(text) + italicRegex.findAll(text))
        .sortedBy { it.range.first }
    for (match in matches) {
        if (match.range.first < lastIndex) continue
        append(text.substring(lastIndex, match.range.first))
        val style = if (match.value.startsWith("**")) {
            SpanStyle(fontWeight = FontWeight.Bold)
        } else {
            SpanStyle(fontStyle = FontStyle.Italic)
        }
        pushStyle(style)
        append(match.groupValues[1])
        pop()
        lastIndex = match.range.last + 1
    }
    append(text.substring(lastIndex))
}
