package com.rakor.xuepinyin

import com.rakor.xuepinyin.domain.nextLessonId
import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressRuleTest {
    @Test fun `无进度时从第 1 课开始`() {
        assertEquals(1, nextLessonId(emptySet(), 13))
    }

    @Test fun `学到第 4 课时下一课是第 5 课`() {
        assertEquals(5, nextLessonId(setOf(1, 2, 3, 4), 13))
    }

    @Test fun `乱序完成同样取最小未完成`() {
        assertEquals(2, nextLessonId(setOf(1, 3), 13))
    }

    @Test fun `全部完成停在第 13 课`() {
        assertEquals(13, nextLessonId((1..13).toSet(), 13))
    }
}
