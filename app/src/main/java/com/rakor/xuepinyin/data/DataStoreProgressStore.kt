package com.rakor.xuepinyin.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.rakor.xuepinyin.domain.progress.ProgressState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.progressDataStore by preferencesDataStore(name = "pinyin_progress")

/** Preferences DataStore 实现：进度序列化为 JSON 存单 key；坏数据重置为空进度 */
class DataStoreProgressStore(private val context: Context) : ProgressStore {

    private val key = stringPreferencesKey("progress_json")
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun load(): ProgressState = withContext(Dispatchers.IO) {
        runCatching {
            val prefs = context.progressDataStore.data.first()
            prefs[key]?.let { json.decodeFromString<ProgressState>(it) } ?: ProgressState()
        }.getOrDefault(ProgressState())
    }

    override suspend fun save(state: ProgressState) = withContext(Dispatchers.IO) {
        context.progressDataStore.edit { prefs ->
            prefs[key] = json.encodeToString(state)
        }
        Unit
    }
}
