package com.rakor.xuepinyin.ui.read

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rakor.xuepinyin.domain.model.Course
import com.rakor.xuepinyin.ui.lesson.letterColor
import com.rakor.xuepinyin.ui.theme.CandyColors
import com.rakor.xuepinyin.ui.theme.candyFocus

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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(CandyColors.Background)
            .padding(28.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(CandyColors.Card, CandyColors.Ink),
                modifier = Modifier.candyFocus(),
            ) { Text("返回") }
            Spacer(Modifier.width(12.dp))
            Text("读一读", fontSize = 24.sp, fontWeight = FontWeight.Black, color = CandyColors.Ink)
        }
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            ModeCard("📖", "跟读课本", "按课跟读字母和口诀", CandyColors.Mint) { onPick("textbook") }
            ModeCard("🔤", "词句跟读", "拼读词大声读", CandyColors.Sky) { onPick("words") }
        }
    }
}

@Composable
private fun ModeCard(emoji: String, title: String, sub: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = CandyColors.Card, contentColor = CandyColors.Ink),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .width(260.dp)
            .height(180.dp)
            .candyFocus(RoundedCornerShape(16.dp)),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 44.sp)
            Spacer(Modifier.height(6.dp))
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.Black, color = color)
            Text(sub, fontSize = 14.sp, color = CandyColors.InkSoft)
        }
    }
}

@Composable
private fun LessonPicker(course: Course, currentId: Int, onBack: () -> Unit, onPick: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CandyColors.Background)
            .padding(horizontal = 28.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(CandyColors.Card, CandyColors.Ink),
                modifier = Modifier.candyFocus(),
            ) { Text("返回") }
            Spacer(Modifier.width(12.dp))
            Text("选一课跟读", fontSize = 24.sp, fontWeight = FontWeight.Black, color = CandyColors.Ink)
        }
        Spacer(Modifier.height(10.dp))
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
        ) {
            course.lessons.forEach { lesson ->
                Button(
                    onClick = { onPick(lesson.id) },
                    colors = ButtonDefaults.buttonColors(CandyColors.Card, CandyColors.Ink),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .candyFocus(RoundedCornerShape(10.dp)),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("第 ${lesson.id} 课", fontSize = 16.sp, color = CandyColors.InkSoft)
                        Spacer(Modifier.width(10.dp))
                        Text(lesson.title, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        if (lesson.id == currentId) {
                            Spacer(Modifier.width(10.dp))
                            Text("◀ 继续", fontSize = 14.sp, color = CandyColors.Coral)
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

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(CandyColors.Background)
            .padding(horizontal = 28.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(CandyColors.Card, CandyColors.Ink),
                modifier = Modifier.candyFocus(),
            ) { Text("返回") }
            Spacer(Modifier.width(12.dp))
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.Black, color = CandyColors.Ink)
        }
        Spacer(Modifier.height(8.dp))
        Text("${index + 1} / ${lines.size}", fontSize = 14.sp, color = CandyColors.InkSoft)
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .background(CandyColors.Card, RoundedCornerShape(16.dp))
                .border(3.dp, CandyColors.Ink, RoundedCornerShape(16.dp))
                .padding(vertical = 26.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(line.display, fontSize = 56.sp, fontWeight = FontWeight.Black, color = CandyColors.FinalRed)
                line.sub?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, fontSize = 20.sp, color = CandyColors.InkSoft)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row {
            Button(
                onClick = { if (index > 0) index -= 1 },
                enabled = index > 0,
                colors = ButtonDefaults.buttonColors(CandyColors.Card, CandyColors.Ink),
                modifier = Modifier.candyFocus(),
            ) { Text("◀ 上一句") }
            Spacer(Modifier.width(10.dp))
            Button(
                onClick = { onPlayAudio(line.audioKey) },
                colors = ButtonDefaults.buttonColors(CandyColors.Sun, CandyColors.Ink),
                modifier = Modifier.candyFocus(),
            ) { Text("🔊 再听一遍") }
            Spacer(Modifier.width(10.dp))
            Button(
                onClick = { if (index < lines.size - 1) index += 1 },
                enabled = index < lines.size - 1,
                colors = ButtonDefaults.buttonColors(CandyColors.Coral, Color.White),
                modifier = Modifier.candyFocus(),
            ) { Text("下一句 ▶") }
        }
    }
}
