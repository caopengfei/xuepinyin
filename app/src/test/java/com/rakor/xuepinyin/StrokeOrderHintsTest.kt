package com.rakor.xuepinyin

import com.rakor.xuepinyin.data.CourseParser
import com.rakor.xuepinyin.domain.StrokeOrderHints
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class StrokeOrderHintsTest {

    @Test
    fun `47 个字母全部有笔顺口诀`() {
        val course = CourseParser.parse(File("src/main/assets/lessons.json").readText(Charsets.UTF_8))
        course.allLetters().forEach { letter ->
            val hint = StrokeOrderHints.hintOf(letter.pinyin)
            assertTrue("字母 ${letter.pinyin} 缺少笔顺口诀", hint.isNotBlank())
        }
    }
}
