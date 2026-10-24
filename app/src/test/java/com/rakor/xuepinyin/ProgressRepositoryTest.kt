package com.rakor.xuepinyin

import com.rakor.xuepinyin.data.InMemoryProgressStore
import com.rakor.xuepinyin.data.ProgressRepository
import com.rakor.xuepinyin.domain.progress.LessonProgress
import com.rakor.xuepinyin.domain.progress.LessonStep
import com.rakor.xuepinyin.domain.progress.ProgressState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressRepositoryTest {

    @Test
    fun `initialize 从 store 载入状态`() = runTest {
        val saved = ProgressState(lessons = mapOf(1 to LessonProgress(stepsDone = setOf(LessonStep.VIEW))))
        val repo = ProgressRepository(InMemoryProgressStore(saved))
        repo.initialize()
        assertEquals(saved, repo.state.value)
    }

    @Test
    fun `update 变换当前课进度并写穿 store`() = runTest {
        val store = InMemoryProgressStore()
        val repo = ProgressRepository(store)
        repo.initialize()
        repo.update(1) { it.markStep(LessonStep.VIEW) }
        repo.update(1) { it.markStep(LessonStep.TONES) }
        assertEquals(setOf(LessonStep.VIEW, LessonStep.TONES), repo.state.value.of(1).stepsDone)
        assertEquals(repo.state.value, store.load())
    }

    @Test
    fun `多次 update 累加不互相覆盖`() = runTest {
        val repo = ProgressRepository(InMemoryProgressStore())
        repo.initialize()
        repo.update(1) { it.markStep(LessonStep.VIEW) }
        repo.update(2) { it.markReadAlong() }
        repo.update(1) { it.markStep(LessonStep.TONES) }
        assertEquals(setOf(LessonStep.VIEW, LessonStep.TONES), repo.state.value.of(1).stepsDone)
        assertEquals(true, repo.state.value.of(2).readAlongDone)
    }
}
