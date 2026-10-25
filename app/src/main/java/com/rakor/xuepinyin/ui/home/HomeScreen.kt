package com.rakor.xuepinyin.ui.home

import androidx.compose.foundation.background
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
import com.rakor.xuepinyin.ui.theme.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rakor.xuepinyin.domain.model.Course
import com.rakor.xuepinyin.domain.model.Lesson
import com.rakor.xuepinyin.domain.nextLessonId
import com.rakor.xuepinyin.ui.theme.AppColors
import com.rakor.xuepinyin.ui.theme.CaptionStyle
import com.rakor.xuepinyin.ui.theme.SectionLabelStyle
import com.rakor.xuepinyin.ui.theme.TitleStyle
import com.rakor.xuepinyin.ui.theme.appFocus
import com.rakor.xuepinyin.ui.theme.rememberTvFocus
import com.rakor.xuepinyin.ui.theme.tvFocusTarget

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
            .padding(horizontal = 28.dp, vertical = 12.dp),
    ) {
        HomeTopBar(onOpenModule, onOpenTable, onOpenParent, onStartLearn = { onOpenLesson(currentId) })
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
    onStartLearn: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(AppColors.Coral, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("拼", style = TitleStyle, fontSize = 20.sp, color = Color.White)
            }
            Spacer(Modifier.width(8.dp))
            Text("拼音乐园", style = TitleStyle, fontSize = 24.sp)
        }
        Spacer(Modifier.width(20.dp))
        // 学一学直接打开当前课程（导航图里没有 learn 路由，走 onOpenModule 会崩）
        Button(
            onClick = onStartLearn,
            modifier = Modifier.padding(end = 6.dp).appFocus(),
            colors = ButtonDefaults.buttonColors(AppColors.Card, AppColors.Ink),
            shape = RoundedCornerShape(12.dp),
        ) { Text("学一学", fontSize = 16.sp) }
        listOf("读一读" to "read", "写一写" to "write", "玩一玩" to "play").forEach { (label, route) ->
            Button(
                onClick = { onOpenModule(route) },
                modifier = Modifier.padding(end = 6.dp).appFocus(),
                colors = ButtonDefaults.buttonColors(AppColors.Card, AppColors.Ink),
                shape = RoundedCornerShape(12.dp),
            ) { Text(label, fontSize = 16.sp) }
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onOpenTable,
            modifier = Modifier.padding(end = 6.dp).appFocus(),
            colors = ButtonDefaults.buttonColors(AppColors.Card, AppColors.Ink),
            shape = RoundedCornerShape(12.dp),
        ) { Text("拼音总表", fontSize = 16.sp) }
        Button(
            onClick = onOpenParent,
            modifier = Modifier.appFocus(),
            colors = ButtonDefaults.buttonColors(AppColors.Card, AppColors.InkSoft),
            shape = RoundedCornerShape(12.dp),
        ) { Text("家长中心", fontSize = 16.sp) }
    }
}

@Composable
private fun HeroCard(lesson: Lesson, completed: Int, total: Int, onStart: () -> Unit) {
    // TV 默认焦点：进首页直接落在“开始学习”，OK 即继续学习
    val startFocus = rememberTvFocus()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Card, RoundedCornerShape(16.dp))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                if (completed == 0) "开始学习 · 第 ${lesson.id} 课" else "继续学习 · 第 ${lesson.id} 课",
                style = SectionLabelStyle,
            )
            Text(lesson.title, style = TitleStyle, fontSize = 32.sp)
            Text("已学 $completed / $total 课 · 每天进步一点点", style = CaptionStyle)
        }
        Spacer(Modifier.width(14.dp))
        Button(
            onClick = onStart,
            modifier = Modifier.tvFocusTarget(startFocus).appFocus(),
            colors = ButtonDefaults.buttonColors(AppColors.Coral, Color.White),
            shape = RoundedCornerShape(12.dp),
        ) { Text("开始学习", fontSize = 20.sp) }
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
            Text(title, style = SectionLabelStyle)
            Spacer(Modifier.width(8.dp))
            Text(rangeText, style = CaptionStyle, fontSize = 13.sp)
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
        ChipState.DONE -> AppColors.CoralTrack to AppColors.CoralDeep
        ChipState.NOW -> AppColors.Coral to Color.White
        ChipState.TODO -> AppColors.Card to AppColors.Ink
    }
    Button(
        onClick = onOpen,
        modifier = modifier.fillMaxWidth().height(56.dp).appFocus(),
        colors = ButtonDefaults.buttonColors(bg, content),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                when (state) {
                    ChipState.DONE -> "已学 · 第 ${lesson.id} 课"
                    ChipState.NOW -> "第 ${lesson.id} 课"
                    ChipState.TODO -> "第 ${lesson.id} 课"
                },
                // 显式行高：默认继承 bodyLarge 的 24sp 会让两行内容撑破 56dp 的 chip，压缩后裁掉文楷下降部
                lineHeight = 14.sp,
                fontSize = 11.sp,
            )
            Text(lesson.title, style = TitleStyle, fontSize = 18.sp, lineHeight = 21.5.sp, color = content)
        }
    }
}

@Composable
private fun HomeFooter(modifier: Modifier = Modifier) {
    Text(
        "方向键选择 · OK 确认 · 返回退出 · 统编版语文一年级上册",
        style = CaptionStyle,
        fontSize = 13.sp,
        modifier = modifier.padding(top = 4.dp),
    )
}
