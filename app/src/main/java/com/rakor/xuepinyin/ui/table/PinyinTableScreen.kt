package com.rakor.xuepinyin.ui.table

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rakor.xuepinyin.domain.model.Course
import com.rakor.xuepinyin.domain.model.LetterType
import com.rakor.xuepinyin.ui.theme.CandyColors
import com.rakor.xuepinyin.ui.theme.candyFocus

/** 拼音总表：声母 23 / 韵母 24 / 整体认读 16，点读发音 */
@Composable
fun PinyinTableScreen(
    course: Course,
    onBack: () -> Unit,
    onPlayAudio: (String) -> Unit,
) {
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("声母 23", "韵母 24", "整体认读 16")
    // 字母 -> 所在课（用于音频 key）
    val letterLesson = remember {
        buildMap {
            course.lessons.forEach { lesson ->
                lesson.letters.forEach { put(it.pinyin, lesson.id) }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(CandyColors.Background).padding(horizontal = 28.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(CandyColors.Card, CandyColors.Ink),
                modifier = Modifier.candyFocus(),
            ) { Text("返回") }
            Spacer(Modifier.width(12.dp))
            Text("拼音总表 · 温故知新", fontSize = 24.sp, fontWeight = FontWeight.Black, color = CandyColors.Ink)
            Spacer(Modifier.width(18.dp))
            tabs.forEachIndexed { i, label ->
                Button(
                    onClick = { tab = i },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (i == tab) CandyColors.Coral else CandyColors.Card,
                        contentColor = if (i == tab) Color.White else CandyColors.Ink,
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.padding(end = 8.dp).candyFocus(RoundedCornerShape(18.dp)),
                ) { Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            }
        }
        Spacer(Modifier.height(14.dp))
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
        ) {
            when (tab) {
                0 -> TableGrid(
                    letters = course.allLetters().filter { it.type == LetterType.INITIAL },
                    color = CandyColors.InitialBlue,
                    letterLesson = letterLesson,
                    onPlayAudio = onPlayAudio,
                )
                1 -> TableGrid(
                    letters = course.allLetters().filter { it.type != LetterType.INITIAL },
                    color = CandyColors.FinalRed,
                    letterLesson = letterLesson,
                    onPlayAudio = onPlayAudio,
                )
                else -> {
                    // 整体认读音节（绿色；暂无专属音频，点按无声）
                    val holistic = remember {
                        course.lessons.flatMap { it.holisticSyllables }.distinct()
                    }
                    Text(
                        "整体认读音节：直接整体读，不拼读",
                        fontSize = 15.sp, color = CandyColors.InkSoft,
                    )
                    Spacer(Modifier.height(4.dp))
                    RowElements(holistic.chunked(6)) { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { h ->
                                Button(
                                    onClick = {},
                                    colors = ButtonDefaults.buttonColors(CandyColors.Card, CandyColors.HolisticGreen),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.width(110.dp).height(64.dp).candyFocus(RoundedCornerShape(12.dp)),
                                ) { Text(h, fontSize = 24.sp, fontWeight = FontWeight.Black) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> RowElements(rows: List<List<T>>, content: @Composable (List<T>) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { content(it) }
    }
}

@Composable
private fun TableGrid(
    letters: List<com.rakor.xuepinyin.domain.model.PinyinLetter>,
    color: Color,
    letterLesson: Map<String, Int>,
    onPlayAudio: (String) -> Unit,
) {
    RowElements(letters.chunked(8)) { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { letter ->
                Button(
                    onClick = {
                        letterLesson[letter.pinyin]?.let { lessonId ->
                            onPlayAudio("lesson$lessonId/${letter.pinyin}")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(CandyColors.Card, color),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.width(90.dp).height(64.dp).candyFocus(RoundedCornerShape(12.dp)),
                ) { Text(letter.pinyin, fontSize = 26.sp, fontWeight = FontWeight.Black) }
            }
        }
    }
}
