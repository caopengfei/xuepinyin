package com.rakor.xuepinyin.ui.read

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rakor.xuepinyin.R
import com.rakor.xuepinyin.domain.model.Course
import com.rakor.xuepinyin.ui.theme.AppColors
import com.rakor.xuepinyin.ui.theme.CaptionStyle
import com.rakor.xuepinyin.ui.theme.TitleStyle
import com.rakor.xuepinyin.ui.theme.appFocus
import com.rakor.xuepinyin.ui.theme.rememberTvFocus
import com.rakor.xuepinyin.ui.theme.tvFocusTarget

private data class ReadLine(val display: String, val audioKey: String, val sub: String?)

/** 读一读：跟读课本（按课）/ 词句跟读（拼读词） */
@Composable
fun ReadScreen(
    course: Course,
    completedLessonIds: Set<Int>,
    onBack: () -> Unit,
    onPlayAudio: (String) -> Unit,
) {
    var mode by remember { mutableStateOf<String?>(null) }
    var lessonId by remember { mutableIntStateOf(completedLessonIds.maxOrNull()?.plus(1)?.coerceAtMost(13) ?: 1) }

    // 遥控器返回键逐级退回：跟读课本→选课→模式选择→退出，而不是直接退出读一读
    BackHandler(enabled = mode != null) {
        mode = if (mode == "reader") "textbook" else null
    }

    when (mode) {
        null -> ModeSelect(onBack = onBack, onPick = { mode = it })
        "textbook" -> {
            LessonPicker(
                course = course,
                currentId = lessonId,
                onBack = { mode = null },
                onPick = { lessonId = it; mode = "reader" },
            )
        }
        "reader" -> {
            val lesson = course.lesson(lessonId) ?: course.lesson(1)!!
            val lines = remember(lessonId) {
                buildList {
                    lesson.letters.forEach { l ->
                        add(ReadLine(l.pinyin, "lesson${lesson.id}/${l.pinyin}", l.mnemonic))
                    }
                }
            }
            Reader(
                title = "跟读课本 · 第 ${lesson.id} 课 ${lesson.title}",
                lines = lines,
                onBack = { mode = "textbook" },
                onPlayAudio = onPlayAudio,
            )
        }
        "words" -> {
            val words = remember {
                course.lessons.flatMap { it.syllables }
                    .mapIndexed { i, s -> ReadLine("${s.initial}＋${s.finalPinyin} → ${s.result}", "read/${i + 1}", s.word) }
            }
            Reader(
                title = "词句跟读 · 拼读词",
                lines = words,
                onBack = { mode = null },
                onPlayAudio = onPlayAudio,
            )
        }
        else -> mode = null
    }
}

@Composable
private fun ModeSelect(onBack: () -> Unit, onPick: (String) -> Unit) {
    // TV 默认焦点落在第一张模式卡
    val modeFocus = rememberTvFocus()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(AppColors.Card, AppColors.Ink),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.appFocus(),
            ) { Text("返回") }
            Spacer(Modifier.width(12.dp))
            Text("读一读", style = TitleStyle, fontSize = 24.sp)
        }
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            ModeCard("跟读课本", "按课跟读字母和口诀", Modifier.tvFocusTarget(modeFocus)) { onPick("textbook") }
            ModeCard("词句跟读", "拼读词大声读") { onPick("words") }
        }
    }
}

@Composable
private fun ModeCard(title: String, sub: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(AppColors.Card, AppColors.Ink),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .width(260.dp)
            .height(180.dp)
            .appFocus(RoundedCornerShape(16.dp)),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = TitleStyle, fontSize = 22.sp)
            Spacer(Modifier.height(6.dp))
            Text(sub, style = CaptionStyle)
        }
    }
}

@Composable
private fun LessonPicker(course: Course, currentId: Int, onBack: () -> Unit, onPick: (Int) -> Unit) {
    // TV 默认焦点落在当前要继续的课
    val pickerFocus = rememberTvFocus()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(AppColors.Card, AppColors.Ink),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.appFocus(),
            ) { Text("返回") }
            Spacer(Modifier.width(12.dp))
            Text("选一课跟读", style = TitleStyle, fontSize = 24.sp)
        }
        Spacer(Modifier.height(10.dp))
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
        ) {
            course.lessons.forEach { lesson ->
                Button(
                    onClick = { onPick(lesson.id) },
                    colors = ButtonDefaults.buttonColors(AppColors.Card, AppColors.Ink),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .then(if (lesson.id == currentId) Modifier.tvFocusTarget(pickerFocus) else Modifier)
                        .appFocus(),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("第 ${lesson.id} 课", fontSize = 16.sp, color = AppColors.InkSoft)
                        Spacer(Modifier.width(10.dp))
                        Text(lesson.title, style = TitleStyle, fontSize = 20.sp)
                        if (lesson.id == currentId) {
                            Spacer(Modifier.width(10.dp))
                            Text("继续", fontSize = 14.sp, color = AppColors.CoralDeep)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Reader(
    title: String,
    lines: List<ReadLine>,
    onBack: () -> Unit,
    onPlayAudio: (String) -> Unit,
) {
    var index by remember { mutableIntStateOf(0) }
    val line = lines[index.coerceIn(lines.indices)]
    // 切行自动朗读
    LaunchedEffect(index, lines.size) { onPlayAudio(line.audioKey) }
    // TV 默认焦点落在“下一句”，连按 OK 逐句前进
    val nextFocus = rememberTvFocus()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(AppColors.Card, AppColors.Ink),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.appFocus(),
            ) { Text("返回") }
            Spacer(Modifier.width(12.dp))
            Text(title, style = TitleStyle, fontSize = 22.sp)
        }
        Spacer(Modifier.height(8.dp))
        Text("${index + 1} / ${lines.size}", style = CaptionStyle)
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .background(AppColors.Card, RoundedCornerShape(16.dp))
                .padding(vertical = 26.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(line.display, style = TitleStyle, fontSize = 56.sp)
                line.sub?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, style = CaptionStyle, fontSize = 20.sp)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row {
            Button(
                onClick = { if (index > 0) index -= 1 },
                enabled = index > 0,
                colors = ButtonDefaults.buttonColors(AppColors.Card, AppColors.Ink),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.appFocus(),
            ) { Text("上一句") }
            Spacer(Modifier.width(10.dp))
            Button(
                onClick = { onPlayAudio(line.audioKey) },
                colors = ButtonDefaults.buttonColors(AppColors.Honey, Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.appFocus(),
            ) {
                Icon(painterResource(R.drawable.ic_music), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("再听一遍")
            }
            Spacer(Modifier.width(10.dp))
            Button(
                onClick = { if (index < lines.size - 1) index += 1 },
                enabled = index < lines.size - 1,
                colors = ButtonDefaults.buttonColors(AppColors.Coral, Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.tvFocusTarget(nextFocus).appFocus(),
            ) { Text("下一句") }
        }
    }
}
