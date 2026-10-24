package com.rakor.xuepinyin

import com.rakor.xuepinyin.data.CourseParser
import com.rakor.xuepinyin.domain.stroke.StrokeData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class StrokeDataTest {

    private val course = CourseParser.parse(File("src/main/assets/lessons.json").readText(Charsets.UTF_8))

    @Test
    fun `课程 47 个字母全部有笔顺数据`() {
        course.allLetters().forEach { letter ->
            assertTrue("字母 ${letter.pinyin} 缺笔顺", StrokeData.strokesOf(letter.pinyin).isNotEmpty())
        }
    }

    @Test
    fun `单字母笔画数与教学规范一致`() {
        assertEquals(2, StrokeData.strokesOf("a").size)
        assertEquals(1, StrokeData.strokesOf("o").size)
        assertEquals(1, StrokeData.strokesOf("e").size)
        assertEquals(2, StrokeData.strokesOf("i").size)
        assertEquals(2, StrokeData.strokesOf("u").size)
        assertEquals(4, StrokeData.strokesOf("ü").size)
        assertEquals(2, StrokeData.strokesOf("b").size)
        assertEquals(3, StrokeData.strokesOf("m").size)
        assertEquals(2, StrokeData.strokesOf("f").size)
        assertEquals(1, StrokeData.strokesOf("l").size)
        assertEquals(2, StrokeData.strokesOf("t").size)
        assertEquals(2, StrokeData.strokesOf("x").size)
        assertEquals(1, StrokeData.strokesOf("z").size)
        assertEquals(1, StrokeData.strokesOf("c").size)
        assertEquals(2, StrokeData.strokesOf("w").size)
    }

    @Test
    fun `组合字母笔画数等于组件之和`() {
        assertEquals(StrokeData.strokesOf("a").size + StrokeData.strokesOf("i").size, StrokeData.strokesOf("ai").size)
        assertEquals(StrokeData.strokesOf("z").size + StrokeData.strokesOf("h").size, StrokeData.strokesOf("zh").size)
        assertEquals(StrokeData.strokesOf("ü").size + StrokeData.strokesOf("e").size, StrokeData.strokesOf("üe").size)
        assertEquals(
            StrokeData.strokesOf("a").size + StrokeData.strokesOf("n").size + StrokeData.strokesOf("g").size,
            StrokeData.strokesOf("ang").size,
        )
    }

    @Test
    fun `所有段坐标在 0 到 1 的归一化范围`() {
        StrokeData.strokesOf("a").forEach { stroke ->
            stroke.forEach { seg ->
                seg.coords.forEach { v -> assertTrue("坐标越界 $v", v in 0f..1f) }
            }
        }
    }
}
