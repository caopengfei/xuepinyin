package com.rakor.xuepinyin.data

import com.rakor.xuepinyin.domain.progress.LessonProgress
import com.rakor.xuepinyin.domain.progress.ProgressState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 进度仓库：内存 StateFlow 供 UI 观察，写穿到 ProgressStore 持久化 */
class ProgressRepository(private val store: ProgressStore) {

    private val _state = MutableStateFlow(ProgressState())
    val state: StateFlow<ProgressState> = _state.asStateFlow()

    suspend fun initialize() {
        _state.value = store.load()
    }

    suspend fun update(lessonId: Int, transform: (LessonProgress) -> LessonProgress) {
        val next = _state.value.update(lessonId, transform(_state.value.of(lessonId)))
        _state.value = next
        store.save(next)
    }
}
