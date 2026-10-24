package com.rakor.xuepinyin.domain.game

import com.rakor.xuepinyin.domain.model.Course
import com.rakor.xuepinyin.domain.model.Lesson
import com.rakor.xuepinyin.domain.model.PinyinLetter
import kotlin.random.Random

/** 玩一玩出题器：题目仅出自已学课程（completedThrough 及之前的课） */
class QuizGenerator(private val random: Random = Random.Default) {

    data class CandyQuestion(
        val audioKey: String,
        val answer: String,
        val options: List<String>,
    )

    data class ToneQuestion(
        val audioKey: String,
        val display: String,
        val answer: String,
        val options: List<String>,
    )

    private fun lessonsInScope(course: Course, completedThrough: Int): List<Lesson> =
        course.lessons.filter { it.id <= completedThrough }

    /** 摘糖果：听发音选字母。题池充足时不重复出题，不足时循环。 */
    fun candyQuestions(course: Course, completedThrough: Int, count: Int): List<CandyQuestion> {
        val lessons = lessonsInScope(course, completedThrough)
        val pool = lessons.flatMap { lesson -> lesson.letters.map { it to lesson.id } }
        if (pool.isEmpty()) return emptyList()
        val picks = if (pool.size >= count) {
            pool.shuffled(random).take(count)
        } else {
            List(count) { pool[random.nextInt(pool.size)] }
        }
        return picks.map { (letter, lessonId) ->
            val distractors = pool
                .map { it.first.pinyin }
                .filter { it != letter.pinyin }
                .distinct()
                .shuffled(random)
                .take(3)
            CandyQuestion(
                audioKey = "lesson$lessonId/${letter.pinyin}",
                answer = letter.pinyin,
                options = (distractors + letter.pinyin).shuffled(random),
            )
        }
    }

    /** 声调跳跳：听带调读音，选出正确的声调形式。 */
    fun toneQuestions(course: Course, completedThrough: Int, count: Int): List<ToneQuestion> {
        val finals: List<Pair<PinyinLetter, Int>> = lessonsInScope(course, completedThrough)
            .flatMap { lesson -> lesson.letters.filter { it.tones.isNotEmpty() }.map { it to lesson.id } }
        if (finals.isEmpty()) return emptyList()
        return List(count) {
            val (letter, lessonId) = finals[random.nextInt(finals.size)]
            val toneIndex = random.nextInt(letter.tones.size)
            ToneQuestion(
                audioKey = "lesson$lessonId/${letter.pinyin}_tone$toneIndex",
                display = letter.pinyin,
                answer = letter.tones[toneIndex],
                options = letter.tones,
            )
        }
    }
}
