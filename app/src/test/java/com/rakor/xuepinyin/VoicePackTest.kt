package com.rakor.xuepinyin

import com.rakor.xuepinyin.audio.VoicePack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class VoicePackTest {

    private val keysJson = """
    {"keys": {
      "lesson1/a": {"unit": "lesson1", "line": 1},
      "lesson1/a_tone0": {"unit": "lesson1", "line": 2},
      "common/1": {"unit": "common", "line": 1}
    }}
    """.trimIndent()

    private val manifestJson = """
    {"lines": [
      {"file": "line1.opus", "durationMs": 900},
      {"file": "line2.opus", "durationMs": 850}
    ]}
    """.trimIndent()

    private fun pack() = VoicePack.fromJson(keysJson) { unit -> manifestJson }

    @Test
    fun `key 解析为音频文件路径`() {
        val p = pack()
        assertTrue(p.contains("lesson1/a"))
        assertEquals("audio/lesson1/line1.opus", p.fileOf("lesson1/a"))
        assertEquals("audio/lesson1/line2.opus", p.fileOf("lesson1/a_tone0"))
    }

    @Test
    fun `缺失 key 返回 null 不抛异常`() {
        val p = pack()
        assertFalse(p.contains("lesson99/x"))
        assertNull(p.fileOf("lesson99/x"))
        assertNull(p.durationMsOf("lesson99/x"))
    }

    @Test
    fun `manifest 提供时长`() {
        assertEquals(900L, pack().durationMsOf("lesson1/a"))
        assertEquals(850L, pack().durationMsOf("lesson1/a_tone0"))
    }

    @Test
    fun `单元无 manifest 时时长为空但文件路径仍可解析`() {
        val p = VoicePack.fromJson(keysJson) { unit -> null }
        assertEquals("audio/common/line1.opus", p.fileOf("common/1"))
        assertNull(p.durationMsOf("common/1"))
    }

    @Test
    fun `真实 keys 覆盖全部课程字母与口诀四声`() {
        val keys = VoicePack.loadKeys(File("src/main/assets/voicepack/keys.json").readText(Charsets.UTF_8))
        val course = com.rakor.xuepinyin.data.CourseParser.parse(
            File("src/main/assets/lessons.json").readText(Charsets.UTF_8))
        course.lessons.forEach { lesson ->
            lesson.letters.forEach { letter ->
                assertTrue("缺呼读 key: lesson${lesson.id}/${letter.pinyin}",
                    "lesson${lesson.id}/${letter.pinyin}" in keys)
                assertTrue("缺口诀 key: lesson${lesson.id}/${letter.pinyin}_mn",
                    "lesson${lesson.id}/${letter.pinyin}_mn" in keys)
                letter.tones.forEachIndexed { i, _ ->
                    assertTrue("缺声调 key: lesson${lesson.id}/${letter.pinyin}_tone$i",
                        "lesson${lesson.id}/${letter.pinyin}_tone$i" in keys)
                }
            }
        }
        (1..6).forEach { assertTrue("common/$it" in keys) }
        (1..4).forEach { assertTrue("reward/$it" in keys) }
    }
}
