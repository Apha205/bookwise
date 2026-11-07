package com.bookwise.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.preferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

class AppPreferences(private val context: Context) {
    private val fontScale = floatPreferencesKey("font_scale")
    private val themeMode = stringPreferencesKey("theme_mode")
    private val providerId = stringPreferencesKey("provider_id")
    private val modelId = stringPreferencesKey("model_id")
    private val endpointOverride = stringPreferencesKey("endpoint_override")
    private val hasKey = booleanPreferencesKey("has_api_key")
    private val zoomLevel = floatPreferencesKey("zoom_level")
    private val providerConfigJson = stringPreferencesKey("provider_config_json")

    fun fontScaleFlow(): Flow<Float> = context.preferencesDataStore.data.map { it[fontScale] ?: 1.0f }
    fun themeModeFlow(): Flow<String> = context.preferencesDataStore.data.map { it[themeMode] ?: "system" }
    fun providerFlow(): Flow<String?> = context.preferencesDataStore.data.map { it[providerId] }
    fun modelFlow(): Flow<String?> = context.preferencesDataStore.data.map { it[modelId] }
    fun endpointFlow(): Flow<String?> = context.preferencesDataStore.data.map { it[endpointOverride] }
    fun hasKeyFlow(): Flow<Boolean> = context.preferencesDataStore.data.map { it[hasKey] ?: false }
    fun zoomLevelFlow(): Flow<Float> = context.preferencesDataStore.data.map { it[zoomLevel] ?: 1.0f }

    suspend fun setFontScale(value: Float) {
        require(value in 0.8f..2.0f) { "Font scale out of range" }
        context.preferencesDataStore.edit { it[fontScale] = value }
    }

    suspend fun setThemeMode(value: String) = context.preferencesDataStore.edit { it[themeMode] = value }

    suspend fun setProvider(provider: String?, model: String?, endpoint: String?) {
        context.preferencesDataStore.edit {
            if (provider != null) it[providerId] = provider else it.remove(providerId)
            if (model != null) it[modelId] = model else it.remove(modelId)
            if (endpoint != null) it[endpointOverride] = endpoint else it.remove(endpointOverride)
        }
    }

    fun providerConfigJsonFlow(): Flow<String?> = context.preferencesDataStore.data.map { it[providerConfigJson] }
    suspend fun setProviderConfigJson(value: String?) = context.preferencesDataStore.edit {
        if (value != null) it[providerConfigJson] = value else it.remove(providerConfigJson)
    }
    suspend fun setZoomLevel(value: Float) = context.preferencesDataStore.edit { it[zoomLevel] = value.coerceIn(0.8f, 2.0f) }
}

class SettingsRepository(private val preferences: AppPreferences) {
    fun fontScaleFlow(): Flow<Float> = preferences.fontScaleFlow()
    fun themeModeFlow(): Flow<String> = preferences.themeModeFlow()
    fun providerFlow(): Flow<String?> = preferences.providerFlow()
    fun modelFlow(): Flow<String?> = preferences.modelFlow()
    fun endpointFlow(): Flow<String?> = preferences.endpointFlow()
    fun hasKeyFlow(): Flow<Boolean> = preferences.hasKeyFlow()
    fun zoomLevelFlow(): Flow<Float> = preferences.zoomLevelFlow()
    fun providerConfigJsonFlow(): Flow<String?> = preferences.providerConfigJsonFlow()

    suspend fun setFontScale(value: Float) = preferences.setFontScale(value)
    suspend fun setThemeMode(value: String) = preferences.setThemeMode(value)
    suspend fun setZoomLevel(value: Float) = preferences.setZoomLevel(value)
    suspend fun setProviderConfigJson(value: String?) = preferences.setProviderConfigJson(value)
    suspend fun setProvider(provider: String?, model: String?, endpoint: String?) =
        preferences.setProvider(provider, model, endpoint)
}
