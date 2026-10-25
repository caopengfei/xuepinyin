package com.rakor.xuepinyin.ui.write

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.rakor.xuepinyin.ui.theme.Button
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rakor.xuepinyin.domain.StrokeOrderHints
import com.rakor.xuepinyin.domain.model.Course
import com.rakor.xuepinyin.domain.model.PinyinLetter
import com.rakor.xuepinyin.domain.stroke.Seg
import com.rakor.xuepinyin.domain.stroke.StrokeData
import com.rakor.xuepinyin.ui.lesson.letterColor
import com.rakor.xuepinyin.ui.theme.AppColors
import com.rakor.xuepinyin.ui.theme.CaptionStyle
import com.rakor.xuepinyin.ui.theme.TitleStyle
import com.rakor.xuepinyin.ui.theme.appFocus
import com.rakor.xuepinyin.ui.theme.rememberTvFocus
import com.rakor.xuepinyin.ui.theme.tvFocusTarget
import kotlinx.coroutines.delay

/** 写一写：按课选字母 -> 分笔演示 */
@Composable
fun WriteScreen(course: Course, onBack: () -> Unit) {
    var selected by remember { mutableStateOf<PinyinLetter?>(null) }
    // 遥控器返回键：笔顺页退回字母列表，而不是直接退出写一写
    BackHandler(enabled = selected != null) { selected = null }
    val letter = selected
    if (letter == null) {
        // TV 默认焦点落在第一个字母
        val letterFocus = rememberTvFocus()
        val firstLetter = course.units.firstOrNull()?.let { u -> course.lesson(u.lessonRange.first) }?.letters?.firstOrNull()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onBack,
                    colors = plain(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.appFocus(),
                ) { Text("返回") }
                Spacer(Modifier.width(12.dp))
                Text("写一写 · 笔顺练习", style = TitleStyle, fontSize = 24.sp)
            }
            Spacer(Modifier.height(10.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            ) {
                // 按单元横向流式排列（每行 8 个），与拼音总表布局一致，不再按课分列留出大片空白
                course.units.forEach { unit ->
                    val unitLetters = unit.lessonRange.mapNotNull { course.lesson(it) }.flatMap { it.letters }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "${unit.title}（第 ${unit.lessonRange.first}–${unit.lessonRange.last} 课）",
                            style = CaptionStyle,
                            fontSize = 15.sp,
                        )
                        Spacer(Modifier.height(6.dp))
                        unitLetters.chunked(8).forEach { rowLetters ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                rowLetters.forEach { l ->
                                    Button(
                                        onClick = { selected = l },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = AppColors.Card,
                                            contentColor = letterColor(l),
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .widthIn(min = 90.dp)
                                            .height(64.dp)
                                            .then(if (l === firstLetter) Modifier.tvFocusTarget(letterFocus) else Modifier)
                                            .appFocus(),
                                    ) {
                                        Text(l.pinyin, style = TitleStyle, fontSize = 26.sp, color = letterColor(l))
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
private fun plain() = ButtonDefaults.buttonColors(AppColors.Card, AppColors.Ink)

/** 笔顺演示：四线三格 Canvas + 逐笔揭示动画 */
@Composable
internal fun StrokeScreen(letter: PinyinLetter, onBack: () -> Unit) {
    val strokes = remember(letter.pinyin) { StrokeData.strokesOf(letter.pinyin) }
    var current by remember(letter.pinyin) { mutableIntStateOf(0) } // 已揭示到第几笔（1-based，0=未开始）
    var auto by remember(letter.pinyin) { mutableStateOf(false) }
    val progress = remember(letter.pinyin) { Animatable(0f) }
    // TV 默认焦点落在“下一笔”，连按 OK 逐笔演示
    val nextStrokeFocus = rememberTvFocus(key = letter.pinyin)

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
            .padding(horizontal = 28.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onBack,
                colors = plain(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.appFocus(),
            ) { Text("返回") }
            Spacer(Modifier.width(12.dp))
            Text("写一写 · ${letter.pinyin}", style = TitleStyle, fontSize = 24.sp)
            Spacer(Modifier.width(14.dp))
            Text("第 ${current.coerceAtLeast(0)}/${strokes.size} 笔", style = CaptionStyle, fontSize = 16.sp)
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
        Text(StrokeOrderHints.hintOf(letter.pinyin), style = TitleStyle, fontSize = 18.sp)
        Spacer(Modifier.height(12.dp))
        Row {
            Button(
                onClick = { auto = false; if (current > 1) current -= 1 },
                enabled = current > 1,
                colors = plain(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.appFocus(),
            ) { Text("上一笔") }
            Spacer(Modifier.width(10.dp))
            Button(
                onClick = { auto = false; if (current < strokes.size) current += 1 },
                enabled = current < strokes.size,
                colors = ButtonDefaults.buttonColors(AppColors.Coral, Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.tvFocusTarget(nextStrokeFocus).appFocus(),
            ) { Text("下一笔") }
            Spacer(Modifier.width(10.dp))
            Button(
                onClick = { current = 0; auto = true; current = 1 },
                colors = plain(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.appFocus(),
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
