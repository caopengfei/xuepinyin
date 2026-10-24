package com.rakor.xuepinyin.domain.stroke

/** 一笔中的一段：M 起笔 / L 直线 / C 三次贝塞尔；坐标为字母盒内归一化值（x:0..1，y:0..1 对应四线三格上沿到下沿，中格即 1/3..2/3） */
sealed class Seg {
    abstract val coords: List<Float>

    data class M(val x: Float, val y: Float) : Seg() {
        override val coords = listOf(x, y)
    }

    data class L(val x: Float, val y: Float) : Seg() {
        override val coords = listOf(x, y)
    }

    data class C(
        val c1x: Float, val c1y: Float,
        val c2x: Float, val c2y: Float,
        val x: Float, val y: Float,
    ) : Seg() {
        override val coords = listOf(c1x, c1y, c2x, c2y, x, y)
    }
}

typealias Stroke = List<Seg>

private fun m(x: Float, y: Float) = Seg.M(x, y)
private fun l(x: Float, y: Float) = Seg.L(x, y)
private fun c(c1x: Float, c1y: Float, c2x: Float, c2y: Float, x: Float, y: Float) = Seg.C(c1x, c1y, c2x, c2y, x, y)

/** 29 个基础字母的手工分笔路径；复韵母与 zh/ch/sh 由组件横排拼合 */
object StrokeData {

    private val base: Map<String, List<Stroke>> = mapOf(
        // 单韵母（占中格 y∈[0.333,0.667]）
        "a" to listOf(
            listOf(m(0.58f, 0.375f), c(0.42f, 0.335f, 0.24f, 0.42f, 0.24f, 0.50f), c(0.24f, 0.58f, 0.42f, 0.645f, 0.58f, 0.615f)),
            listOf(m(0.58f, 0.375f), l(0.58f, 0.55f), c(0.58f, 0.65f, 0.74f, 0.65f, 0.78f, 0.55f)),
        ),
        "o" to listOf(
            listOf(m(0.68f, 0.40f), c(0.84f, 0.46f, 0.84f, 0.56f, 0.68f, 0.61f), c(0.40f, 0.68f, 0.20f, 0.60f, 0.20f, 0.50f), c(0.20f, 0.39f, 0.52f, 0.325f, 0.68f, 0.40f)),
        ),
        "e" to listOf(
            listOf(m(0.24f, 0.50f), l(0.70f, 0.50f), c(0.68f, 0.35f, 0.30f, 0.33f, 0.26f, 0.50f), c(0.23f, 0.60f, 0.50f, 0.665f, 0.70f, 0.575f)),
        ),
        "i" to listOf(
            listOf(m(0.50f, 0.375f), l(0.50f, 0.625f)),
            listOf(m(0.50f, 0.20f), l(0.50f, 0.245f)),
        ),
        "u" to listOf(
            listOf(m(0.35f, 0.375f), l(0.35f, 0.56f), c(0.35f, 0.645f, 0.65f, 0.645f, 0.65f, 0.56f)),
            listOf(m(0.65f, 0.375f), l(0.65f, 0.625f)),
        ),
        "ü" to listOf(
            listOf(m(0.35f, 0.375f), l(0.35f, 0.56f), c(0.35f, 0.645f, 0.65f, 0.645f, 0.65f, 0.56f)),
            listOf(m(0.65f, 0.375f), l(0.65f, 0.625f)),
            listOf(m(0.35f, 0.27f), l(0.35f, 0.315f)),
            listOf(m(0.65f, 0.27f), l(0.65f, 0.315f)),
        ),
        // 声母 23
        "b" to listOf(
            listOf(m(0.32f, 0.125f), l(0.32f, 0.625f)),
            listOf(m(0.32f, 0.40f), c(0.72f, 0.34f, 0.72f, 0.63f, 0.32f, 0.585f)),
        ),
        "p" to listOf(
            listOf(m(0.32f, 0.375f), l(0.32f, 0.875f)),
            listOf(m(0.32f, 0.40f), c(0.72f, 0.34f, 0.72f, 0.63f, 0.32f, 0.585f)),
        ),
        "m" to listOf(
            listOf(m(0.18f, 0.375f), l(0.18f, 0.625f)),
            listOf(m(0.18f, 0.425f), c(0.40f, 0.34f, 0.46f, 0.50f, 0.46f, 0.625f)),
            listOf(m(0.46f, 0.425f), c(0.68f, 0.34f, 0.74f, 0.50f, 0.74f, 0.625f)),
        ),
        "f" to listOf(
            listOf(m(0.50f, 0.125f), c(0.38f, 0.20f, 0.30f, 0.30f, 0.30f, 0.625f)),
            listOf(m(0.20f, 0.38f), l(0.58f, 0.38f)),
        ),
        "d" to listOf(
            listOf(m(0.30f, 0.40f), c(0.02f, 0.35f, 0.02f, 0.625f, 0.30f, 0.585f)),
            listOf(m(0.46f, 0.125f), l(0.46f, 0.625f)),
        ),
        "t" to listOf(
            listOf(m(0.44f, 0.125f), l(0.44f, 0.55f), c(0.44f, 0.645f, 0.62f, 0.645f, 0.66f, 0.55f)),
            listOf(m(0.30f, 0.345f), l(0.62f, 0.345f)),
        ),
        "n" to listOf(
            listOf(m(0.30f, 0.375f), l(0.30f, 0.625f)),
            listOf(m(0.30f, 0.425f), c(0.55f, 0.34f, 0.62f, 0.50f, 0.62f, 0.625f)),
        ),
        "l" to listOf(
            listOf(m(0.50f, 0.125f), l(0.50f, 0.625f)),
        ),
        "g" to listOf(
            listOf(m(0.42f, 0.40f), c(0.14f, 0.34f, 0.14f, 0.625f, 0.42f, 0.585f)),
            listOf(m(0.42f, 0.375f), l(0.42f, 0.72f), c(0.42f, 0.865f, 0.24f, 0.865f, 0.16f, 0.78f)),
        ),
        "k" to listOf(
            listOf(m(0.35f, 0.125f), l(0.35f, 0.625f)),
            listOf(m(0.74f, 0.375f), l(0.35f, 0.50f), l(0.78f, 0.625f)),
        ),
        "h" to listOf(
            listOf(m(0.35f, 0.125f), l(0.35f, 0.625f)),
            listOf(m(0.35f, 0.425f), c(0.60f, 0.34f, 0.66f, 0.50f, 0.66f, 0.625f)),
        ),
        "j" to listOf(
            listOf(m(0.52f, 0.125f), l(0.52f, 0.72f), c(0.52f, 0.865f, 0.32f, 0.865f, 0.24f, 0.78f)),
            listOf(m(0.74f, 0.19f), l(0.74f, 0.235f)),
        ),
        "q" to listOf(
            listOf(m(0.30f, 0.40f), c(0.02f, 0.34f, 0.02f, 0.625f, 0.30f, 0.585f)),
            listOf(m(0.46f, 0.375f), l(0.46f, 0.875f)),
        ),
        "x" to listOf(
            listOf(m(0.28f, 0.375f), l(0.72f, 0.625f)),
            listOf(m(0.72f, 0.375f), l(0.28f, 0.625f)),
        ),
        "z" to listOf(
            listOf(m(0.26f, 0.375f), l(0.74f, 0.375f), l(0.26f, 0.625f), l(0.74f, 0.625f)),
        ),
        "c" to listOf(
            listOf(m(0.62f, 0.385f), c(0.30f, 0.33f, 0.18f, 0.58f, 0.60f, 0.625f)),
        ),
        "s" to listOf(
            listOf(m(0.66f, 0.42f), c(0.58f, 0.35f, 0.28f, 0.36f, 0.30f, 0.47f), c(0.32f, 0.565f, 0.70f, 0.545f, 0.68f, 0.60f), c(0.66f, 0.66f, 0.36f, 0.66f, 0.30f, 0.605f)),
        ),
        "r" to listOf(
            listOf(m(0.38f, 0.375f), l(0.38f, 0.625f)),
            listOf(m(0.38f, 0.445f), c(0.58f, 0.35f, 0.68f, 0.42f, 0.70f, 0.50f)),
        ),
        "y" to listOf(
            listOf(m(0.30f, 0.375f), l(0.64f, 0.625f)),
            listOf(m(0.64f, 0.375f), l(0.38f, 0.77f), c(0.34f, 0.86f, 0.26f, 0.87f, 0.20f, 0.83f)),
        ),
        "w" to listOf(
            listOf(m(0.20f, 0.375f), l(0.37f, 0.625f), l(0.55f, 0.375f)),
            listOf(m(0.55f, 0.375f), l(0.73f, 0.625f), l(0.90f, 0.375f)),
        ),
    )

    /** 组合字母：组件依序横排（每个组件占 1/n 宽） */
    private val compositions: Map<String, String> = mapOf(
        "zh" to "zh", "ch" to "ch", "sh" to "sh", // 显式声明（由 z+h 等拼合）
        "ai" to "ai", "ei" to "ei", "ui" to "ui", "ao" to "ao", "ou" to "ou", "iu" to "iu",
        "ie" to "ie", "üe" to "üe", "er" to "er", "an" to "an", "en" to "en", "in" to "in",
        "un" to "un", "ün" to "ün", "ang" to "ang", "eng" to "eng", "ing" to "ing", "ong" to "ong",
    )

    private fun componentsOf(pinyin: String): List<String> = when (pinyin) {
        "zh" -> listOf("z", "h")
        "ch" -> listOf("c", "h")
        "sh" -> listOf("s", "h")
        "er" -> listOf("e", "r")
        "ai" -> listOf("a", "i"); "ei" -> listOf("e", "i"); "ui" -> listOf("u", "i")
        "ao" -> listOf("a", "o"); "ou" -> listOf("o", "u"); "iu" -> listOf("i", "u")
        "ie" -> listOf("i", "e"); "üe" -> listOf("ü", "e")
        "an" -> listOf("a", "n"); "en" -> listOf("e", "n"); "in" -> listOf("i", "n")
        "un" -> listOf("u", "n"); "ün" -> listOf("ü", "n")
        "ang" -> listOf("a", "n", "g"); "eng" -> listOf("e", "n", "g")
        "ing" -> listOf("i", "n", "g"); "ong" -> listOf("o", "n", "g")
        else -> emptyList()
    }

    private fun shiftScale(stroke: Stroke, dx: Float, width: Float): Stroke = stroke.map { seg ->
        when (seg) {
            is Seg.M -> Seg.M(dx + seg.x * width, seg.y)
            is Seg.L -> Seg.L(dx + seg.x * width, seg.y)
            is Seg.C -> Seg.C(
                dx + seg.c1x * width, seg.c1y,
                dx + seg.c2x * width, seg.c2y,
                dx + seg.x * width, seg.y,
            )
        }
    }

    fun strokesOf(pinyin: String): List<Stroke> {
        base[pinyin]?.let { return it }
        val components = componentsOf(pinyin)
        if (components.isEmpty()) return emptyList()
        val width = 1f / components.size
        return components.flatMapIndexed { i, part ->
            val baseStrokes = base[part] ?: return emptyList()
            baseStrokes.map { shiftScale(it, i * width, width) }
        }
    }
}
