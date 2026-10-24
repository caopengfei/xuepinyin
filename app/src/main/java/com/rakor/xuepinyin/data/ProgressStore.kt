package com.rakor.xuepinyin.data

import com.rakor.xuepinyin.domain.progress.ProgressState

interface ProgressStore {
    suspend fun load(): ProgressState
    suspend fun save(state: ProgressState)
}

class InMemoryProgressStore(initial: ProgressState = ProgressState()) : ProgressStore {
    private var state = initial

    override suspend fun load(): ProgressState = state

    override suspend fun save(state: ProgressState) {
        this.state = state
    }
}
