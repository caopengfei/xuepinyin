package com.rakor.xuepinyin

import com.rakor.xuepinyin.audio.VoicePack
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** 语音包资产完整性：keys.json 的每个 key 在源目录都有 manifest 记录与 >1KB 的 opus 文件 */
class VoicePackAssetsTest {

    private val assetsRoot = File("src/main/assets")
    private val keys = VoicePack.loadKeys(
        File(assetsRoot, "voicepack/keys.json").readText(Charsets.UTF_8))

    @Test
    fun `每个 key 都有 manifest 记录与有效音频文件`() {
        assertTrue("keys.json 为空", keys.isNotEmpty())
        keys.forEach { (key, ref) ->
            val manifest = File(assetsRoot, "audio/${ref.unit}/manifest.json")
            assertTrue("缺 manifest: ${ref.unit} (key=$key)", manifest.isFile)
            val lines = manifest.readText(Charsets.UTF_8)
                .replace(Regex("\\s"), "")
            assertTrue(
                "manifest 缺行: ${ref.unit}/line${ref.line} (key=$key)",
                lines.contains("\"file\":\"line${ref.line}.opus\""),
            )
            val audio = File(assetsRoot, "audio/${ref.unit}/line${ref.line}.opus")
            assertTrue("缺音频: ${audio.path} (key=$key)", audio.isFile)
            assertTrue("音频过小(<1KB): ${audio.path}", audio.length() > 1024)
        }
    }

    @Test
    fun `音频总量在 5MB 以内（APK 体积约束）`() {
        val total = File(assetsRoot, "audio").walkTopDown().filter { it.isFile }.sumOf { it.length() }
        assertTrue("音频总体积 ${total / 1024}KB 超限", total < 5L * 1024 * 1024)
    }
}
