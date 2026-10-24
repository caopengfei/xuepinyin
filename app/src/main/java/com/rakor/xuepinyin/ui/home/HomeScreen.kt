package com.rakor.xuepinyin.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rakor.xuepinyin.domain.model.Course
import com.rakor.xuepinyin.domain.model.Lesson
import com.rakor.xuepinyin.domain.nextLessonId
import com.rakor.xuepinyin.ui.theme.CandyColors
import com.rakor.xuepinyin.ui.theme.candyFocus

@Composable
fun HomeScreen(
    course: Course,
    completedLessonIds: Set<Int>,
    onOpenLesson: (Int) -> Unit,
    onOpenModule: (String) -> Unit,
    onOpenTable: () -> Unit,
    onOpenParent: () -> Unit,
) {
    val currentId = nextLessonId(completedLessonIds, course.lessons.size)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CandyColors.Background)
            .padding(horizontal = 28.dp, vertical = 12.dp),
    ) {
        HomeTopBar(onOpenModule, onOpenTable, onOpenParent)
        Spacer(Modifier.height(10.dp))
        HeroCard(
            lesson = course.lesson(currentId)!!,
            completed = completedLessonIds.size,
            total = course.lessons.size,
            onStart = { onOpenLesson(currentId) },
        )
        Spacer(Modifier.height(12.dp))
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
        ) {
            course.units.forEach { unit ->
                UnitSection(
                    title = unit.title,
                    rangeText = "第 ${unit.lessonRange.first}–${unit.lessonRange.last} 课",
                    lessons = unit.lessonRange.mapNotNull { course.lesson(it) },
                    completedIds = completedLessonIds,
                    currentId = currentId,
                    onOpenLesson = onOpenLesson,
                )
            }
        }
        HomeFooter(Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun HomeTopBar(
    onOpenModule: (String) -> Unit,
    onOpenTable: () -> Unit,
    onOpenParent: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(CandyColors.Coral, RoundedCornerShape(10.dp))
                    .border(2.dp, CandyColors.Ink, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("拼", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(8.dp))
            Text("拼音乐园", fontSize = 24.sp, fontWeight = FontWeight.Black, color = CandyColors.Ink)
        }
        Spacer(Modifier.width(20.dp))
        listOf("学一学" to "learn", "读一读" to "read", "写一写" to "write", "玩一玩" to "play").forEach { (label, route) ->
            Button(
                onClick = { onOpenModule(route) },
                modifier = Modifier.padding(end = 6.dp).candyFocus(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CandyColors.Sun,
                    contentColor = CandyColors.Ink,
                ),
                shape = RoundedCornerShape(20.dp),
            ) { Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onOpenTable,
            modifier = Modifier.padding(end = 6.dp).candyFocus(),
            colors = ButtonDefaults.buttonColors(
                containerColor = CandyColors.Mint,
                contentColor = Color.White,
            ),
        ) { Text("拼音总表", fontSize = 16.sp) }
        Button(
            onClick = onOpenParent,
            modifier = Modifier.candyFocus(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = CandyColors.InkSoft,
            ),
        ) { Text("家长中心", fontSize = 16.sp) }
    }
}

@Composable
private fun HeroCard(lesson: Lesson, completed: Int, total: Int, onStart: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFE9B8), RoundedCornerShape(14.dp))
            .border(3.dp, CandyColors.Ink, RoundedCornerShape(14.dp))
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                if (completed == 0) "开始学习 · 第 ${lesson.id} 课" else "继续学习 · 第 ${lesson.id} 课",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFA4602A),
            )
            Text(lesson.title, fontSize = 32.sp, fontWeight = FontWeight.Black, color = CandyColors.Ink)
            Text("已学 $completed / $total 课 · 跟着熊猫奇奇学发音", fontSize = 14.sp, color = CandyColors.InkSoft)
        }
        Text("🐼", fontSize = 52.sp)
        Spacer(Modifier.width(14.dp))
        Button(
            onClick = onStart,
            modifier = Modifier.candyFocus(),
            colors = ButtonDefaults.buttonColors(
                containerColor = CandyColors.Coral,
                contentColor = Color.White,
            ),
            shape = RoundedCornerShape(14.dp),
        ) { Text("开始学习", fontSize = 20.sp, fontWeight = FontWeight.Black) }
    }
}

@Composable
private fun UnitSection(
    title: String,
    rangeText: String,
    lessons: List<Lesson>,
    completedIds: Set<Int>,
    currentId: Int,
    onOpenLesson: (Int) -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .background(CandyColors.Coral, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 2.dp),
            ) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(Modifier.width(8.dp))
            Text(rangeText, fontSize = 13.sp, color = CandyColors.InkSoft)
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            lessons.forEach { lesson ->
                LessonChip(
                    lesson = lesson,
                    state = when {
                        lesson.id in completedIds -> ChipState.DONE
                        lesson.id == currentId -> ChipState.NOW
                        else -> ChipState.TODO
                    },
                    onOpen = { onOpenLesson(lesson.id) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private enum class ChipState { DONE, NOW, TODO }

@Composable
private fun LessonChip(
    lesson: Lesson,
    state: ChipState,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (bg, content) = when (state) {
        ChipState.DONE -> Color(0xFFE2F7F2) to CandyColors.Ink
        ChipState.NOW -> CandyColors.Coral to Color.White
        ChipState.TODO -> CandyColors.Card to CandyColors.InkSoft
    }
    Button(
        onClick = onOpen,
        modifier = modifier.fillMaxWidth().height(56.dp).candyFocus(),
        colors = ButtonDefaults.buttonColors(containerColor = bg, contentColor = content),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                when (state) {
                    ChipState.DONE -> "✓ 第 ${lesson.id} 课"
                    ChipState.NOW -> "第 ${lesson.id} 课"
                    ChipState.TODO -> "第 ${lesson.id} 课"
                },
                fontSize = 11.sp,
            )
            Text(lesson.title, fontSize = 18.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun HomeFooter(modifier: Modifier = Modifier) {
    Text(
        "◀ ▲ ▼ ▶ 方向键选择 · OK 确认 · 返回退出 · 统编版语文一年级上册",
        fontSize = 13.sp,
        color = CandyColors.InkSoft,
        modifier = modifier.padding(top = 4.dp),
    )
}
