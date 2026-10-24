package com.rakor.xuepinyin

import com.rakor.xuepinyin.data.AssetsCourseRepository
import com.rakor.xuepinyin.data.CourseParser
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class AssetsCourseRepositoryTest {

    private fun repoOf(path: String) = AssetsCourseRepository { File(path).readText(Charsets.UTF_8) }

    @Test
    fun `加载真实 assets 并解析为 13 课`() {
        val repo = repoOf("src/main/assets/lessons.json")
        assertEquals(13, repo.course.lessons.size)
        assertEquals("a o e", repo.course.lesson(1)!!.title)
    }

    @Test
    fun `course 惰性加载且缓存同一实例`() {
        var reads = 0
        val repo = AssetsCourseRepository {
            reads++
            File("src/main/assets/lessons.json").readText(Charsets.UTF_8)
        }
        repo.course
        repo.course
        assertEquals(1, reads)
    }

    @Test(expected = CourseParser.ParseException::class)
    fun `坏数据抛 ParseException 而非崩溃为其他类型`() {
        AssetsCourseRepository { "{ bad" }.course
    }
}
