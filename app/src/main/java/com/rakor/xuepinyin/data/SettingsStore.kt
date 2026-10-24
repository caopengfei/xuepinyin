package com.rakor.xuepinyin.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.settingsDataStore by preferencesDataStore(name = "pinyin_settings")

@Serializable
data class AppSettings(
    val eyeRestEnabled: Boolean = true,
)

interface SettingsStore {
    suspend fun load(): AppSettings
    suspend fun save(settings: AppSettings)
}

class InMemorySettingsStore(initial: AppSettings = AppSettings()) : SettingsStore {
    private var state = initial
    override suspend fun load() = state
    override suspend fun save(settings: AppSettings) { state = settings }
}

class DataStoreSettingsStore(private val context: Context) : SettingsStore {
    private val key = stringPreferencesKey("settings_json")
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun load(): AppSettings = runCatching {
        val prefs = context.settingsDataStore.data.first()
        prefs[key]?.let { json.decodeFromString<AppSettings>(it) } ?: AppSettings()
    }.getOrDefault(AppSettings())

    override suspend fun save(settings: AppSettings) {
        context.settingsDataStore.edit { it[key] = json.encodeToString(settings) }
    }
}

/** 设置仓库：内存 StateFlow + 写穿持久化（与进度仓库同构） */
class SettingsRepository(private val store: SettingsStore) {
    private val _state = MutableStateFlow(AppSettings())
    val state: StateFlow<AppSettings> = _state.asStateFlow()

    suspend fun initialize() { _state.value = store.load() }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(_state.value)
        _state.value = next
        store.save(next)
    }
}
