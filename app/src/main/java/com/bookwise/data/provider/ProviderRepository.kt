package com.bookwise.data.provider

import com.bookwise.data.preferences.AppPreferences
import com.bookwise.data.secure.SecureKeyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface ProviderRepository {
    val config: Flow<ProviderConfig>
    suspend fun updateConfig(config: ProviderConfig)
    suspend fun saveKey(key: String)
    suspend fun clearKey()
    fun hasKey(): Boolean
    fun loadKey(): String?
}

class ProviderRepositoryImpl(
    private val appPreferences: AppPreferences,
    private val secureKeyRepository: SecureKeyRepository
) : ProviderRepository {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    override val config: Flow<ProviderConfig> = appPreferences.providerConfigJsonFlow().map { stored ->
        if (stored.isNullOrBlank()) ProviderConfig()
        else runCatching { json.decodeFromString<ProviderConfig>(stored) }.getOrDefault(ProviderConfig())
    }

    override suspend fun updateConfig(config: ProviderConfig) {
        appPreferences.setProviderConfigJson(json.encodeToString(config))
    }

    override suspend fun saveKey(key: String) {
        secureKeyRepository.saveKey(key)
    }

    override suspend fun clearKey() {
        secureKeyRepository.clearKey()
    }

    override fun hasKey(): Boolean = secureKeyRepository.hasKey()

    override fun loadKey(): String? = secureKeyRepository.loadKey()
}
