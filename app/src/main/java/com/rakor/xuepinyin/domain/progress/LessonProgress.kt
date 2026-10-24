package com.rakor.xuepinyin.domain.progress

import kotlinx.serialization.Serializable

enum class LessonStep { VIEW, TONES, STROKE, SYLLABLE }

/** 一课的进度：4 个学习步骤 + 跟读标记 + 拼读全对标记（星级依据） */
@Serializable
data class LessonProgress(
    val stepsDone: Set<LessonStep> = emptySet(),
    val readAlongDone: Boolean = false,
    val syllablesAllCorrect: Boolean = false,
) {
    fun isCompleted(): Boolean = stepsDone.containsAll(LessonStep.entries)

    /** 1★ 完成 4 步；2★ +跟读；3★ +拼读全对 */
    fun stars(): Int = when {
        isCompleted() && syllablesAllCorrect -> 3
        isCompleted() && readAlongDone -> 2
        isCompleted() -> 1
        else -> 0
    }

    fun markStep(step: LessonStep): LessonProgress = copy(stepsDone = stepsDone + step)

    fun markReadAlong(): LessonProgress = copy(readAlongDone = true)

    fun markSyllables(allCorrect: Boolean): LessonProgress =
        copy(syllablesAllCorrect = syllablesAllCorrect || allCorrect)
}

@Serializable
data class ProgressState(val lessons: Map<Int, LessonProgress> = emptyMap()) {
    fun completedLessonIds(): Set<Int> = lessons.filterValues { it.isCompleted() }.keys

    fun of(lessonId: Int): LessonProgress = lessons[lessonId] ?: LessonProgress()

    fun update(lessonId: Int, progress: LessonProgress): ProgressState =
        copy(lessons = lessons + (lessonId to progress))

    fun totalStars(): Int = lessons.values.sumOf { it.stars() }
}
