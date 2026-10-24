package com.rakor.xuepinyin.ui.write

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rakor.xuepinyin.domain.StrokeOrderHints
import com.rakor.xuepinyin.domain.model.Course
import com.rakor.xuepinyin.domain.model.PinyinLetter
import com.rakor.xuepinyin.domain.stroke.Seg
import com.rakor.xuepinyin.domain.stroke.StrokeData
import com.rakor.xuepinyin.ui.lesson.letterColor
import com.rakor.xuepinyin.ui.theme.CandyColors
import com.rakor.xuepinyin.ui.theme.candyFocus
import kotlinx.coroutines.delay

/** 写一写：按课选字母 -> 分笔演示 */
@Composable
fun WriteScreen(course: Course, onBack: () -> Unit) {
    var selected by remember { mutableStateOf<PinyinLetter?>(null) }
    val letter = selected
    if (letter == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CandyColors.Background)
                .padding(horizontal = 28.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onBack, colors = plain(), modifier = Modifier.candyFocus()) { Text("返回") }
                Spacer(Modifier.width(12.dp))
                Text("写一写 · 笔顺练习", fontSize = 24.sp, fontWeight = FontWeight.Black, color = CandyColors.Ink)
            }
            Spacer(Modifier.height(10.dp))
            Column(
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            ) {
                course.units.forEach { unit ->
                    Column {
                        Text(
                            "${unit.title}（第 ${unit.lessonRange.first}–${unit.lessonRange.last} 课）",
                            fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CandyColors.InkSoft,
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                            unit.lessonRange.mapNotNull { course.lesson(it) }.forEach { lesson ->
                                Column {
                                    lesson.letters.forEach { l ->
                                        Button(
                                            onClick = { selected = l },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = CandyColors.Card,
                                                contentColor = letterColor(l),
                                            ),
                                            modifier = Modifier
                                                .padding(bottom = 6.dp)
                                                .width(72.dp)
                                                .candyFocus(),
                                            shape = RoundedCornerShape(10.dp),
                                        ) {
                                            Text(l.pinyin, fontSize = 22.sp, fontWeight = FontWeight.Black)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        StrokeScreen(letter = letter, onBack = { selected = null })
    }
}

@Composable
private fun plain() = ButtonDefaults.buttonColors(CandyColors.Card, CandyColors.Ink)

/** 笔顺演示：四线三格 Canvas + 逐笔揭示动画 */
@Composable
internal fun StrokeScreen(letter: PinyinLetter, onBack: () -> Unit) {
    val strokes = remember(letter.pinyin) { StrokeData.strokesOf(letter.pinyin) }
    var current by remember(letter.pinyin) { mutableIntStateOf(0) } // 已揭示到第几笔（1-based，0=未开始）
    var auto by remember(letter.pinyin) { mutableStateOf(false) }
    val progress = remember(letter.pinyin) { Animatable(0f) }

    LaunchedEffect(current, auto) {
        progress.snapTo(0f)
        if (current > 0) {
            progress.animateTo(1f, animationSpec = tween(durationMillis = 500))
            if (auto && current < strokes.size) {
                delay(350)
                current += 1
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(CandyColors.Background)
            .padding(horizontal = 28.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = onBack, colors = plain(), modifier = Modifier.candyFocus()) { Text("返回") }
            Spacer(Modifier.width(12.dp))
            Text("写一写 · ${letter.pinyin}", fontSize = 24.sp, fontWeight = FontWeight.Black, color = CandyColors.Ink)
            Spacer(Modifier.width(14.dp))
            Text(
                "第 ${current.coerceAtLeast(0)}/${strokes.size} 笔",
                fontSize = 16.sp, color = CandyColors.InkSoft,
            )
        }
        Spacer(Modifier.height(10.dp))
        StrokeCanvas(
            strokes = strokes,
            revealed = current,
            progress = progress.value,
            strokeColor = letterColor(letter),
            modifier = Modifier.fillMaxWidth(0.55f).height(210.dp),
        )
        Spacer(Modifier.height(10.dp))
        Text(StrokeOrderHints.hintOf(letter.pinyin), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CandyColors.Ink)
        Spacer(Modifier.height(12.dp))
        Row {
            Button(
                onClick = { auto = false; if (current > 1) current -= 1 },
                enabled = current > 1,
                colors = plain(), modifier = Modifier.candyFocus(),
            ) { Text("上一笔") }
            Spacer(Modifier.width(10.dp))
            Button(
                onClick = { auto = false; if (current < strokes.size) current += 1 },
                enabled = current < strokes.size,
                colors = ButtonDefaults.buttonColors(CandyColors.Coral, Color.White),
                modifier = Modifier.candyFocus(),
            ) { Text("下一笔 ✍") }
            Spacer(Modifier.width(10.dp))
            Button(
                onClick = { current = 0; auto = true; current = 1 },
                colors = ButtonDefaults.buttonColors(CandyColors.Sky, Color.White),
                modifier = Modifier.candyFocus(),
            ) { Text("自动演示") }
        }
    }
}

private fun buildPath(stroke: List<Seg>, w: Float, h: Float): Path = Path().apply {
    stroke.forEach { seg ->
        when (seg) {
            is Seg.M -> moveTo(seg.x * w, seg.y * h)
            is Seg.L -> lineTo(seg.x * w, seg.y * h)
            is Seg.C -> cubicTo(
                seg.c1x * w, seg.c1y * h,
                seg.c2x * w, seg.c2y * h,
                seg.x * w, seg.y * h,
            )
        }
    }
}

@Composable
internal fun StrokeCanvas(
    strokes: List<List<Seg>>,
    revealed: Int,
    progress: Float,
    strokeColor: Color,
    modifier: Modifier = Modifier,
) {
    val lineColor = Color(0xFFB98F5E)
    val guideColor = Color(0xFF9DB8D9)
    Box(modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // 四线三格
            listOf(0f, h / 3, h * 2 / 3, h).forEach { y ->
                drawLine(
                    color = lineColor,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 2.dp.toPx(),
                )
            }
            val pen = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
            // 已完成的笔画
            strokes.take(revealed.coerceAtMost(strokes.size)).forEachIndexed { i, s ->
                val path = buildPath(s, w, h)
                if (i < revealed - 1) {
                    drawPath(path, color = strokeColor, style = pen)
                } else {
                    // 当前笔：按进度揭示
                    val measure = PathMeasure()
                    measure.setPath(path, false)
                    val partial = Path()
                    measure.getSegment(0f, measure.length * progress.coerceIn(0f, 1f), partial, true)
                    drawPath(partial, color = strokeColor, style = pen)
                }
            }
        }
    }
}
