package com.bookwise.tutor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bookwise.R
import com.bookwise.domain.model.TutorAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorSheet(
    bookId: String,
    passage: String,
    locatorJson: String?,
    viewModel: TutorViewModel,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedAction by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(TutorAction.EXPLAIN) }
    var question by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    var language by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("English") }

    ModalBottomSheet(onDismissRequest = { viewModel.reset(); onDismiss() }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "Tutor", style = MaterialTheme.typography.titleLarge)
            Text(text = "\u201C${passage.take(120)}\u2026\u201D", style = MaterialTheme.typography.bodySmall)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TutorAction.entries.forEach { action ->
                    AssistChip(
                        onClick = { selectedAction = action },
                        label = { Text(action.label) }
                    )
                }
            }

            if (selectedAction == TutorAction.ASK) {
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    label = { Text("Your question") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (selectedAction == TutorAction.TRANSLATE) {
                OutlinedTextField(
                    value = language,
                    onValueChange = { language = it },
                    label = { Text("Target language") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        viewModel.start(
                            bookId = bookId,
                            passage = passage,
                            locatorJson = locatorJson,
                            action = selectedAction,
                            question = if (selectedAction == TutorAction.ASK) question else null,
                            targetLanguage = if (selectedAction == TutorAction.TRANSLATE) language else null
                        )
                    },
                    enabled = state is TutorUiState.Idle || state is TutorUiState.Done || state is TutorUiState.Error
                ) { Text("Ask") }
                if (state is TutorUiState.Loading || state is TutorUiState.Streaming) {
                    Button(onClick = { viewModel.stop() }) { Text("Stop") }
                }
                if (state is TutorUiState.Done) {
                    TextButton(onClick = { viewModel.save(); onSave() }) { Text("Save") }
                }
            }

            val currentState = state
            when (currentState) {
                TutorUiState.Idle -> {}
                TutorUiState.Loading -> CircularProgressIndicator()
                is TutorUiState.Streaming -> Text(text = currentState.text, style = MaterialTheme.typography.bodyMedium)
                is TutorUiState.Done -> Text(text = currentState.text, style = MaterialTheme.typography.bodyMedium)
                is TutorUiState.Error -> Text(text = currentState.message, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
