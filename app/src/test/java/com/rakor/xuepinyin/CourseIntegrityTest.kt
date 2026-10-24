package com.rakor.xuepinyin

import com.rakor.xuepinyin.data.CourseParser
import com.rakor.xuepinyin.domain.model.LetterType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** 直接读 assets 源文件，纯 JVM 可跑 */
class CourseIntegrityTest {
    private val course = CourseParser.parse(
        File("src/main/assets/lessons.json").readText(Charsets.UTF_8)
    )

    @Test fun `每课 unitId 与所属单元区间一致`() {
        course.lessons.forEach { l ->
            val unit = course.units.firstOrNull { l.id in it.lessonRange }
            assertTrue("课 ${l.id} 不落在任何单元区间", unit != null)
            assertEquals("课 ${l.id} 的 unitId 错误", unit!!.id, l.unitId)
        }
    }

    @Test fun `3 个单元 13 课，课程号 1-13 连续`() {
        assertEquals(3, course.units.size)
        assertEquals(13, course.lessons.size)
        assertEquals((1..13).toList(), course.lessons.map { it.id })
    }

    @Test fun `单元划分 1-6、7-10、11-13`() {
        assertEquals(1..6, course.units[0].lessonRange)
        assertEquals(7..10, course.units[1].lessonRange)
        assertEquals(11..13, course.units[2].lessonRange)
    }

    @Test fun `47 个字母：单韵母6 复韵母17 特殊1 声母23`() {
        val letters = course.allLetters()
        assertEquals(47, letters.size)
        assertEquals(6, letters.count { it.type == LetterType.SINGLE_FINAL })
        assertEquals(17, letters.count { it.type == LetterType.COMPOUND_FINAL })
        assertEquals(1, letters.count { it.type == LetterType.SPECIAL })
        assertEquals(23, letters.count { it.type == LetterType.INITIAL })
    }

    @Test fun `拼音2 为 i u ü y w`() {
        assertEquals(listOf("i", "u", "ü", "y", "w"), course.lesson(2)!!.letters.map { it.pinyin })
    }

    @Test fun `韵母都有四声且声母无声调`() {
        course.allLetters().forEach { l ->
            when (l.type) {
                LetterType.INITIAL -> assertEquals("声母 ${l.pinyin} 不应有声调", 0, l.tones.size)
                else -> assertEquals("韵母 ${l.pinyin} 应有四声", 4, l.tones.size)
            }
        }
    }

    @Test fun `标调位置抽查：ai 标在 a，ui 标在 i，iu 标在 u`() {
        val letters = course.allLetters()
        assertEquals(listOf("āi", "ái", "ǎi", "ài"), letters.first { it.pinyin == "ai" }.tones)
        assertEquals(listOf("uī", "uí", "uǐ", "uì"), letters.first { it.pinyin == "ui" }.tones)
        assertEquals(listOf("iū", "iú", "iǔ", "iù"), letters.first { it.pinyin == "iu" }.tones)
    }

    @Test fun `整体认读音节共 16 个且分布正确`() {
        val holistic = course.lessons.flatMap { it.holisticSyllables }
        assertEquals(
            setOf("yi", "wu", "yu", "zi", "ci", "si", "zhi", "chi", "shi", "ri",
                  "ye", "yue", "yuan", "yin", "yun", "ying"),
            holistic.toSet(),
        )
        assertEquals(16, holistic.size)
    }

    @Test fun `每课至少一条声调操练`() {
        course.lessons.forEach { l ->
            assertTrue("课 ${l.id} 缺少 toneDrills", l.toneDrills.isNotEmpty())
        }
    }

    @Test fun `每课字母都有情境口诀`() {
        course.allLetters().forEach { l ->
            assertTrue("字母 ${l.pinyin} 缺少口诀", !l.mnemonic.isNullOrBlank())
            assertTrue("字母 ${l.pinyin} 缺少情境图", !l.sceneEmoji.isNullOrBlank())
        }
    }
}
