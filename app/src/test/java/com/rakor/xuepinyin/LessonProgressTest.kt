package com.rakor.xuepinyin

import com.rakor.xuepinyin.domain.progress.LessonProgress
import com.rakor.xuepinyin.domain.progress.LessonStep
import com.rakor.xuepinyin.domain.progress.ProgressState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonProgressTest {

    private fun allStepsDone() = LessonProgress(
        stepsDone = LessonStep.entries.toSet(),
    )

    @Test
    fun `未开始为 0 星且未完成`() {
        val p = LessonProgress()
        assertEquals(0, p.stars())
        assertFalse(p.isCompleted())
    }

    @Test
    fun `四步完成得 1 星`() {
        val p = allStepsDone()
        assertTrue(p.isCompleted())
        assertEquals(1, p.stars())
    }

    @Test
    fun `四步加跟读得 2 星`() {
        val p = allStepsDone().markReadAlong()
        assertEquals(2, p.stars())
    }

    @Test
    fun `四步跟读拼读全对得 3 星`() {
        val p = allStepsDone().markReadAlong().markSyllables(true)
        assertEquals(3, p.stars())
    }

    @Test
    fun `缺一步不算完成`() {
        val p = LessonProgress(stepsDone = setOf(LessonStep.VIEW, LessonStep.TONES, LessonStep.STROKE))
        assertFalse(p.isCompleted())
        assertEquals(0, p.stars())
    }

    @Test
    fun `markStep 幂等`() {
        val once = LessonProgress().markStep(LessonStep.VIEW)
        val twice = once.markStep(LessonStep.VIEW)
        assertEquals(once, twice)
    }

    @Test
    fun `拼读全对一旦置位不会被 false 覆盖`() {
        val p = allStepsDone().markSyllables(true).markSyllables(false)
        assertTrue(p.syllablesAllCorrect)
    }

    @Test
    fun `ProgressState 聚合完成课与总星`() {
        var state = ProgressState()
        state = state.update(1, allStepsDone().markReadAlong().markSyllables(true))
        state = state.update(2, allStepsDone())
        state = state.update(3, LessonProgress().markStep(LessonStep.VIEW))
        assertEquals(setOf(1, 2), state.completedLessonIds())
        assertEquals(3 + 1, state.totalStars())
        assertEquals(LessonProgress(), state.of(4))
    }
}
