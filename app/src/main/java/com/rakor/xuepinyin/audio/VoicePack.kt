package com.rakor.xuepinyin.audio

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** 语音包：语义 key -> (单元, 行号)，配合各单元 manifest 提供文件路径与时长 */
class VoicePack(
    private val keys: Map<String, KeyRef>,
    private val manifests: Map<String, VoiceManifest>,
) {
    @Serializable
    data class KeyRef(val unit: String, val line: Int)

    data class VoiceManifest(val durationsMs: List<Long>)

    fun contains(key: String): Boolean = key in keys

    /** assets 内相对路径，如 "audio/lesson1/line3.opus"；缺 key 返回 null（静默降级） */
    fun fileOf(key: String): String? =
        keys[key]?.let { "audio/${it.unit}/line${it.line}.$AUDIO_EXT" }

    fun durationMsOf(key: String): Long? {
        val ref = keys[key] ?: return null
        val durations = manifests[ref.unit]?.durationsMs ?: return null
        return durations.getOrNull(ref.line - 1)
    }

    companion object {
        const val AUDIO_EXT = "opus"

        @Serializable
        private data class KeysDto(val keys: Map<String, KeyRef>)

        @Serializable
        private data class ManifestLine(val file: String, val durationMs: Long = 0)

        @Serializable
        private data class ManifestDto(val lines: List<ManifestLine>)

        private val json = Json { ignoreUnknownKeys = true }

        fun loadKeys(keysJson: String): Map<String, KeyRef> =
            json.decodeFromString<KeysDto>(keysJson).keys

        /** manifestProvider：给定单元名返回 manifest JSON（不存在返回 null） */
        fun fromJson(keysJson: String, manifestProvider: (unit: String) -> String?): VoicePack {
            val keys = loadKeys(keysJson)
            val manifests = keys.values.map { it.unit }.distinct().mapNotNull { unit ->
                manifestProvider(unit)?.let { text ->
                    unit to VoiceManifest(json.decodeFromString<ManifestDto>(text).lines.map { it.durationMs })
                }
            }.toMap()
            return VoicePack(keys, manifests)
        }

        fun fromAssets(context: Context): VoicePack = fromJson(
            keysJson = context.assets.open("voicepack/keys.json").bufferedReader().use { it.readText() },
        ) { unit ->
            runCatching {
                context.assets.open("audio/$unit/manifest.json").bufferedReader().use { it.readText() }
            }.getOrNull()
        }
    }
}
