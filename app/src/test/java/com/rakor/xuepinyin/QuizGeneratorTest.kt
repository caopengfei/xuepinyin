package com.rakor.xuepinyin

import com.rakor.xuepinyin.data.CourseParser
import com.rakor.xuepinyin.domain.game.QuizGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random

class QuizGeneratorTest {

    private val course = CourseParser.parse(File("src/main/assets/lessons.json").readText(Charsets.UTF_8))
    private val gen = QuizGenerator(Random(42))

    @Test
    fun `摘糖果题目数量与不重复`() {
        val questions = gen.candyQuestions(course, completedThrough = 6, count = 8)
        assertEquals(8, questions.size)
        assertEquals(8, questions.map { it.answer }.toSet().size)
    }

    @Test
    fun `题目只用已学课程字母`() {
        val questions = gen.candyQuestions(course, completedThrough = 3, count = 20)
        val learned = course.lessons.filter { it.id <= 3 }.flatMap { it.letters }.map { it.pinyin }.toSet()
        questions.forEach { q ->
            assertTrue("未学字母出现在题目: ${q.answer}", q.answer in learned)
            assertTrue("音频 key 指向错误课程: ${q.audioKey}", q.audioKey.startsWith("lesson"))
            q.options.forEach { assertTrue("未学字母出现在选项: $it", it in learned) }
        }
    }

    @Test
    fun `选项含答案且干扰项不重复`() {
        val questions = gen.candyQuestions(course, completedThrough = 13, count = 10)
        questions.forEach { q ->
            assertTrue("答案不在选项中", q.answer in q.options)
            assertEquals("选项重复", q.options.size, q.options.toSet().size)
            assertTrue("至少 3 个选项", q.options.size >= 3)
        }
    }

    @Test
    fun `题库不足时允许重复但不死循环`() {
        val questions = gen.candyQuestions(course, completedThrough = 1, count = 6)
        assertEquals(6, questions.size) // 第 1 课只有 3 个字母，重复出题
    }

    @Test
    fun `声调题选项恰为四声且答案在其中`() {
        val questions = gen.toneQuestions(course, completedThrough = 9, count = 10)
        assertTrue(questions.isNotEmpty())
        questions.forEach { q ->
            assertEquals(4, q.options.size)
            assertTrue(q.answer in q.options)
            assertTrue("声调音频 key 应含 _tone", q.audioKey.contains("_tone"))
        }
    }

    @Test
    fun `声调题只用已学韵母`() {
        val questions = gen.toneQuestions(course, completedThrough = 8, count = 10)
        val learnedFinals = course.lessons.filter { it.id <= 8 }
            .flatMap { it.letters }.filter { it.tones.isNotEmpty() }.map { it.pinyin }.toSet()
        questions.forEach { q -> assertTrue(q.display in learnedFinals) }
    }
}
