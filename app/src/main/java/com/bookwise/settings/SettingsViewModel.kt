package com.bookwise.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bookwise.data.preferences.SettingsRepository
import com.bookwise.data.provider.ChatRepository
import com.bookwise.data.provider.ProviderConfig
import com.bookwise.data.provider.ProviderPreset
import com.bookwise.data.provider.ProviderRepository
import com.bookwise.data.secure.SecureKeyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SettingsUiState(
    val config: ProviderConfig = ProviderConfig(),
    val apiKeyInput: String = "",
    val hasKey: Boolean = false,
    val testing: Boolean = false,
    val testResult: String? = null,
    val saving: Boolean = false
)

class SettingsViewModel(
    private val providerRepository: ProviderRepository,
    private val chatRepository: ChatRepository,
    private val secureKeyRepository: SecureKeyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val cfg = providerRepository.config.first()
            _uiState.value = _uiState.value.copy(config = cfg, hasKey = providerRepository.hasKey())
        }
    }

    fun updatePreset(preset: ProviderPreset) {
        val current = _uiState.value.config
        _uiState.value = _uiState.value.copy(
            config = current.copy(preset = preset, baseUrl = if (preset == ProviderPreset.CUSTOM) current.baseUrl else preset.baseUrl)
        )
    }

    fun updateBaseUrl(value: String) {
        _uiState.value = _uiState.value.copy(config = _uiState.value.config.copy(baseUrl = value))
    }

    fun updateModel(value: String) {
        _uiState.value = _uiState.value.copy(config = _uiState.value.config.copy(model = value))
    }

    fun updateTemperature(value: Double) {
        _uiState.value = _uiState.value.copy(config = _uiState.value.config.copy(temperature = value))
    }

    fun updateApiKey(value: String) {
        _uiState.value = _uiState.value.copy(apiKeyInput = value)
    }

    fun save() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(saving = true)
            val state = _uiState.value
            providerRepository.updateConfig(state.config)
            if (state.apiKeyInput.isNotBlank()) {
                providerRepository.saveKey(state.apiKeyInput)
                _uiState.value = _uiState.value.copy(apiKeyInput = "", hasKey = true)
            }
            _uiState.value = _uiState.value.copy(saving = false)
        }
    }

    fun clearKey() {
        viewModelScope.launch {
            providerRepository.clearKey()
            _uiState.value = _uiState.value.copy(hasKey = false, apiKeyInput = "")
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(testing = true, testResult = null)
            val state = _uiState.value
            val key = state.apiKeyInput.ifBlank { providerRepository.loadKey() } ?: ""
            val result = chatRepository.testConnection(state.config, key)
            _uiState.value = _uiState.value.copy(
                testing = false,
                testResult = if (result.isSuccess) "Connection OK" else (result.exceptionOrNull()?.message ?: "Failed")
            )
        }
    }

    companion object {
        fun factory(
            providerRepository: ProviderRepository,
            chatRepository: ChatRepository,
            secureKeyRepository: SecureKeyRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(SettingsViewModel::class.java))
                return SettingsViewModel(providerRepository, chatRepository, secureKeyRepository) as T
            }
        }
    }
}
