package com.rakor.xuepinyin.data

import android.content.Context
import com.rakor.xuepinyin.domain.model.Course

/** 课程数据仓库：生产环境从 assets 读取，测试注入任意 JSON 提供者 */
class AssetsCourseRepository(private val jsonProvider: () -> String) {

    val course: Course by lazy { CourseParser.parse(jsonProvider()) }

    companion object {
        fun fromAssets(context: Context): AssetsCourseRepository =
            AssetsCourseRepository {
                context.assets.open("lessons.json").bufferedReader().use { it.readText() }
            }
    }
}
