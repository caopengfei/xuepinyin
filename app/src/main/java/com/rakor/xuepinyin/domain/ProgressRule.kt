package com.rakor.xuepinyin.domain

/** 当前应学课程 = 最小未完成课号；全部完成则停在最后一课（复习态由调用方判断） */
fun nextLessonId(completedLessonIds: Set<Int>, totalLessons: Int): Int {
    for (id in 1..totalLessons) if (id !in completedLessonIds) return id
    return totalLessons
}
