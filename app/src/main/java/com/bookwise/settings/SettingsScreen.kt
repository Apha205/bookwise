package com.bookwise.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bookwise.R
import com.bookwise.data.provider.ProviderPreset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var presetExpanded by remember { mutableStateOf(false) }
    var showKey by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.reader_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "Provider", style = MaterialTheme.typography.titleMedium)

            // Preset dropdown
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { presetExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(text = state.config.preset.displayName)
                }
                DropdownMenu(expanded = presetExpanded, onDismissRequest = { presetExpanded = false }) {
                    ProviderPreset.entries.forEach { preset ->
                        DropdownMenuItem(
                            text = { Text(preset.displayName) },
                            onClick = {
                                viewModel.updatePreset(preset)
                                presetExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = state.config.baseUrl,
                onValueChange = { viewModel.updateBaseUrl(it) },
                label = { Text("Base URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = state.config.model,
                onValueChange = { viewModel.updateModel(it) },
                label = { Text("Model") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = state.apiKeyInput,
                onValueChange = { viewModel.updateApiKey(it) },
                label = { Text(if (state.hasKey) "API key (stored)" else "API key") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showKey = !showKey }) {
                        Text(if (showKey) "Hide" else "Show")
                    }
                }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.save() }, enabled = !state.saving) {
                    Text(if (state.saving) "Saving..." else "Save")
                }
                OutlinedButton(onClick = { viewModel.testConnection() }, enabled = !state.testing) {
                    Text(if (state.testing) "Testing..." else "Test connection")
                }
                if (state.hasKey) {
                    OutlinedButton(onClick = { viewModel.clearKey() }) { Text("Clear key") }
                }
            }

            state.testResult?.let {
                Text(text = it, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Reading", style = MaterialTheme.typography.titleMedium)

            Text(text = "Font size: ${(state.fontScale * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = state.fontScale,
                onValueChange = { viewModel.updateFontScale(it) },
                valueRange = 0.8f..2.0f,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { viewModel.updateThemeMode("system") },
                    enabled = state.themeMode != "system"
                ) { Text("System") }
                OutlinedButton(
                    onClick = { viewModel.updateThemeMode("light") },
                    enabled = state.themeMode != "light"
                ) { Text("Light") }
                OutlinedButton(
                    onClick = { viewModel.updateThemeMode("dark") },
                    enabled = state.themeMode != "dark"
                ) { Text("Dark") }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Keys are stored encrypted with Android Keystore and never leave the device.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
