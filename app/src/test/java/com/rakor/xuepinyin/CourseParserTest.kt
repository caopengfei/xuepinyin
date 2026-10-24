package com.rakor.xuepinyin

import com.rakor.xuepinyin.data.CourseParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseParserTest {

    private val sample = """
    {
      "units": [
        {"id": 1, "title": "拼音一", "lessonStart": 1, "lessonEnd": 2}
      ],
      "lessons": [
        {
          "id": 1, "unitId": 1, "title": "a o e", "note": null,
          "letters": [
            {"pinyin": "a", "type": "SINGLE_FINAL", "tones": ["ā","á","ǎ","à"],
             "mnemonic": "张大嘴巴 a a a", "sceneEmoji": "👧", "sceneWord": "啊"},
            {"pinyin": "b", "type": "INITIAL", "tones": [],
             "mnemonic": "听广播 b b b", "sceneEmoji": "📻", "sceneWord": null}
          ],
          "toneDrills": ["ā á ǎ à"],
          "syllables": [],
          "holisticSyllables": []
        }
      ]
    }
    """

    @Test
    fun `解析单元与课程基础字段`() {
        val course = CourseParser.parse(sample)
        assertEquals(1, course.units.size)
        assertEquals("拼音一", course.units[0].title)
        assertEquals(1..2, course.units[0].lessonRange)
        assertEquals(1, course.lessons.size)
        assertEquals("a o e", course.lessons[0].title)
    }

    @Test
    fun `解析字母类型与四声`() {
        val a = CourseParser.parse(sample).lessons[0].letters[0]
        assertEquals("SINGLE_FINAL", a.type.name)
        assertEquals(listOf("ā", "á", "ǎ", "à"), a.tones)
        assertEquals("👧", a.sceneEmoji)
    }

    @Test
    fun `声母无声调与可空字段`() {
        val b = CourseParser.parse(sample).lessons[0].letters[1]
        assertEquals(0, b.tones.size)
        assertEquals(null, b.sceneWord)
    }

    @Test
    fun `lessonsOf 按 unitId 过滤`() {
        val course = CourseParser.parse(sample)
        assertEquals(1, course.lessonsOf(1).size)
        assertTrue(course.lessonsOf(9).isEmpty())
    }

    @Test(expected = CourseParser.ParseException::class)
    fun `非法 type 枚举抛出 ParseException`() {
        CourseParser.parse(sample.replace("\"INITIAL\"", "\"NOT_A_TYPE\""))
    }

    @Test(expected = CourseParser.ParseException::class)
    fun `非法 JSON 抛出 ParseException`() {
        CourseParser.parse("{ 不是 json")
    }
}
