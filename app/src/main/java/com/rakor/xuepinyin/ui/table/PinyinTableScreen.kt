package com.rakor.xuepinyin.ui.table

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rakor.xuepinyin.domain.model.Course
import com.rakor.xuepinyin.domain.model.LetterType
import com.rakor.xuepinyin.ui.theme.AppColors
import com.rakor.xuepinyin.ui.theme.CaptionStyle
import com.rakor.xuepinyin.ui.theme.TitleStyle
import com.rakor.xuepinyin.ui.theme.TvFocus
import com.rakor.xuepinyin.ui.theme.appFocus
import com.rakor.xuepinyin.ui.theme.rememberTvFocus
import com.rakor.xuepinyin.ui.theme.tvFocusTarget

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
        modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(AppColors.Card, AppColors.Ink),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.appFocus(),
            ) { Text("返回") }
            Spacer(Modifier.width(12.dp))
            Text("拼音总表 · 温故知新", style = TitleStyle, fontSize = 24.sp)
            Spacer(Modifier.width(18.dp))
            tabs.forEachIndexed { i, label ->
                Button(
                    onClick = { tab = i },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (i == tab) AppColors.Coral else AppColors.Card,
                        contentColor = if (i == tab) Color.White else AppColors.Ink,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(end = 8.dp).appFocus(),
                ) { Text(label, fontSize = 16.sp) }
            }
        }
        Spacer(Modifier.height(14.dp))
        // TV 默认焦点：切页签后落在该页签第一个字母上
        val gridFocus = rememberTvFocus(key = tab)
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
        ) {
            when (tab) {
                0 -> TableGrid(
                    letters = course.allLetters().filter { it.type == LetterType.INITIAL },
                    color = AppColors.InitialBlue,
                    letterLesson = letterLesson,
                    tvFocus = gridFocus,
                    onPlayAudio = onPlayAudio,
                )
                1 -> TableGrid(
                    letters = course.allLetters().filter { it.type != LetterType.INITIAL },
                    color = AppColors.FinalRed,
                    letterLesson = letterLesson,
                    tvFocus = gridFocus,
                    onPlayAudio = onPlayAudio,
                )
                else -> {
                    // 整体认读音节（绿色；暂无专属音频，点按无声）
                    val holistic = remember {
                        course.lessons.flatMap { it.holisticSyllables }.distinct()
                    }
                    Text("整体认读音节：直接整体读，不拼读", style = CaptionStyle, fontSize = 15.sp)
                    Spacer(Modifier.height(4.dp))
                    RowElements(holistic.chunked(6)) { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { h ->
                                Button(
                                    onClick = {},
                                    colors = ButtonDefaults.buttonColors(AppColors.Card, AppColors.HolisticGreen),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .widthIn(min = 110.dp)
                                        .height(64.dp)
                                        .then(if (h === holistic.first()) Modifier.tvFocusTarget(gridFocus) else Modifier)
                                        .appFocus(),
                                ) { Text(h, style = TitleStyle, fontSize = 24.sp) }
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
    tvFocus: TvFocus,
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
                    colors = ButtonDefaults.buttonColors(AppColors.Card, color),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .widthIn(min = 90.dp)
                        .height(64.dp)
                        .then(if (letter === letters.first()) Modifier.tvFocusTarget(tvFocus) else Modifier)
                        .appFocus(),
                ) { Text(letter.pinyin, style = TitleStyle, fontSize = 26.sp, color = color) }
            }
        }
    }
}
