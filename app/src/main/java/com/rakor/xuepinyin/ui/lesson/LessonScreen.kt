package com.rakor.xuepinyin.ui.lesson

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rakor.xuepinyin.domain.StrokeOrderHints
import com.rakor.xuepinyin.domain.model.Lesson
import com.rakor.xuepinyin.domain.model.LetterType
import com.rakor.xuepinyin.domain.model.PinyinLetter
import com.rakor.xuepinyin.domain.model.Syllable
import com.rakor.xuepinyin.domain.progress.LessonProgress
import com.rakor.xuepinyin.domain.progress.LessonStep
import com.rakor.xuepinyin.ui.theme.CandyColors
import com.rakor.xuepinyin.ui.theme.candyFocus

private val STEP_NAMES = listOf("认读", "声调", "笔顺", "拼读", "奖励")

/** 学一学：每课 5 步（认读→声调→笔顺→拼读→奖励）。onProgress 以变换函数上报本课进度 */
@Composable
fun LessonScreen(
    lesson: Lesson,
    initialProgress: LessonProgress,
    totalLessons: Int,
    onProgress: ((LessonProgress) -> LessonProgress) -> Unit,
    onExit: () -> Unit,
    onOpenNext: (Int) -> Unit,
    onPlayAudio: (String) -> Unit = {},
) {
    var stepIndex by remember { mutableIntStateOf(0) }
    var local by remember { mutableStateOf(initialProgress) }
    var letterIndex by remember { mutableIntStateOf(0) }
    var toneLetterIndex by remember { mutableIntStateOf(0) }
    var selectedTone by remember { mutableIntStateOf(0) }
    var strokeIndex by remember { mutableIntStateOf(-1) }
    val syllableAnswers = remember { mutableStateMapOf<Int, Boolean>() }
    var syllableWrongOnce by remember { mutableStateOf(false) }

    fun applyProgress(transform: (LessonProgress) -> LessonProgress) {
        local = transform(local)
        onProgress(transform)
    }

    fun goNextStep() {
        when (stepIndex) {
            0 -> applyProgress { it.markStep(LessonStep.VIEW) }
            1 -> applyProgress { it.markStep(LessonStep.TONES) }
            2 -> applyProgress { it.markStep(LessonStep.STROKE) }
            3 -> applyProgress {
                it.markStep(LessonStep.SYLLABLE)
                    .markSyllables(syllableAnswers.isNotEmpty() && !syllableWrongOnce)
            }
        }
        stepIndex = (stepIndex + 1).coerceAtMost(4)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CandyColors.Background)
            .padding(horizontal = 28.dp, vertical = 12.dp),
    ) {
        LessonHeader(lesson = lesson, stepIndex = stepIndex, onExit = onExit)
        Spacer(Modifier.height(8.dp))
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (stepIndex) {
                0 -> ViewStep(
                    lesson = lesson,
                    letterIndex = letterIndex,
                    onSelectLetter = { letterIndex = it },
                    onPlayAudio = onPlayAudio,
                )
                1 -> TonesStep(
                    lesson = lesson,
                    letterIndex = toneLetterIndex,
                    selectedTone = selectedTone,
                    onSelectLetter = { toneLetterIndex = it; selectedTone = 0 },
                    onSelectTone = { selectedTone = it },
                    onReadAlong = { applyProgress { it.markReadAlong() } },
                    readAlongDone = local.readAlongDone,
                    onPlayAudio = onPlayAudio,
                )
                2 -> StrokeStep(
                    lesson = lesson,
                    letterIndex = letterIndex,
                    strokeIndex = strokeIndex,
                    onSelectLetter = { letterIndex = it; strokeIndex = -1 },
                    onStrokeNext = { strokeIndex = it },
                )
                3 -> SyllableStep(
                    lesson = lesson,
                    answers = syllableAnswers,
                    onAnswer = { idx, correct ->
                        syllableAnswers[idx] = correct
                        if (!correct) syllableWrongOnce = true
                    },
                )
                else -> RewardStep(
                    stars = local.stars(),
                    onRestart = { stepIndex = 0 },
                    onNext = if (lesson.id < totalLessons) {
                        { onOpenNext(lesson.id + 1) }
                    } else null,
                    onExit = onExit,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        if (stepIndex < 4) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (stepIndex > 0) {
                    Button(
                        onClick = { stepIndex -= 1 },
                        modifier = Modifier.candyFocus(),
                        colors = plainButtonColors(),
                    ) { Text("上一步", fontSize = 18.sp) }
                    Spacer(Modifier.width(10.dp))
                }
                Button(
                    onClick = ::goNextStep,
                    modifier = Modifier.candyFocus(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CandyColors.Coral,
                        contentColor = Color.White,
                    ),
                ) { Text(if (stepIndex == 3) "完成拼读" else "下一步", fontSize = 18.sp, fontWeight = FontWeight.Black) }
            }
        }
    }
}

@Composable
private fun plainButtonColors() = ButtonDefaults.buttonColors(
    containerColor = CandyColors.Card,
    contentColor = CandyColors.Ink,
)

@Composable
private fun LessonHeader(lesson: Lesson, stepIndex: Int, onExit: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = onExit, colors = plainButtonColors(), modifier = Modifier.candyFocus()) {
            Text("返回", fontSize = 16.sp)
        }
        Spacer(Modifier.width(12.dp))
        Text(
            "第 ${lesson.id} 课 · ${lesson.title}",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = CandyColors.Ink,
        )
        Spacer(Modifier.width(16.dp))
        STEP_NAMES.forEachIndexed { i, name ->
            val active = i == stepIndex
            Box(
                modifier = Modifier
                    .padding(end = 6.dp)
                    .background(
                        if (active) CandyColors.Coral else CandyColors.Card,
                        RoundedCornerShape(8.dp),
                    )
                    .border(1.dp, CandyColors.CardBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text("${i + 1} $name", fontSize = 12.sp, color = if (active) Color.White else CandyColors.InkSoft)
            }
        }
    }
}

internal fun letterColor(letter: PinyinLetter): Color = when (letter.type) {
    LetterType.INITIAL -> CandyColors.InitialBlue
    else -> CandyColors.FinalRed
}

internal fun audioKey(lessonId: Int, pinyin: String): String = "lesson$lessonId/$pinyin"

@Composable
private fun LetterCard(letter: PinyinLetter, bigText: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FourLineGrid(
            modifier = Modifier.width(320.dp),
            height = 150.dp,
        ) {
            Text(
                bigText,
                fontSize = 84.sp,
                fontWeight = FontWeight.Black,
                color = letterColor(letter),
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun ViewStep(
    lesson: Lesson,
    letterIndex: Int,
    onSelectLetter: (Int) -> Unit,
    onPlayAudio: (String) -> Unit,
) {
    val letters = lesson.letters
    val letter = letters[letterIndex.coerceIn(letters.indices)]
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.height(6.dp))
        Text("认读字母（${letterIndex + 1}/${letters.size}）", fontSize = 14.sp, color = CandyColors.InkSoft)
        Spacer(Modifier.height(8.dp))
        LetterCard(letter, letter.pinyin)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text((letter.sceneEmoji ?: ""), fontSize = 44.sp)
            Spacer(Modifier.width(14.dp))
            Column {
                letter.sceneWord?.let {
                    Text("$it —— ${letter.pinyin}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = CandyColors.Ink)
                }
                Text(letter.mnemonic ?: "", fontSize = 20.sp, color = Color(0xFFA4602A), fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row {
            Button(
                onClick = { if (letterIndex > 0) onSelectLetter(letterIndex - 1) },
                enabled = letterIndex > 0,
                colors = plainButtonColors(),
                modifier = Modifier.candyFocus(),
            ) { Text("◀ 上一字母", fontSize = 16.sp) }
            Spacer(Modifier.width(10.dp))
            Button(
                onClick = { onPlayAudio(audioKey(lesson.id, letter.pinyin)) },
                colors = ButtonDefaults.buttonColors(CandyColors.Sun, CandyColors.Ink),
                modifier = Modifier.candyFocus(),
            ) { Text("🔊 听一听", fontSize = 16.sp) }
            Spacer(Modifier.width(10.dp))
            Button(
                onClick = { if (letterIndex < letters.size - 1) onSelectLetter(letterIndex + 1) },
                enabled = letterIndex < letters.size - 1,
                colors = plainButtonColors(),
                modifier = Modifier.candyFocus(),
            ) { Text("下一字母 ▶", fontSize = 16.sp) }
        }
        lesson.note?.let {
            Spacer(Modifier.height(10.dp))
            Text("💡 $it", fontSize = 15.sp, color = CandyColors.InkSoft)
        }
    }
}

@Composable
private fun TonesStep(
    lesson: Lesson,
    letterIndex: Int,
    selectedTone: Int,
    onSelectLetter: (Int) -> Unit,
    onSelectTone: (Int) -> Unit,
    onReadAlong: () -> Unit,
    readAlongDone: Boolean,
    onPlayAudio: (String) -> Unit,
) {
    val toned = lesson.letters.filter { it.tones.isNotEmpty() }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    ) {
        if (toned.isNotEmpty()) {
            val letter = toned[letterIndex.coerceIn(toned.indices)]
            Text("声调练习（${letterIndex + 1}/${toned.size}）", fontSize = 14.sp, color = CandyColors.InkSoft)
            Spacer(Modifier.height(8.dp))
            LetterCard(letter, letter.tones[selectedTone.coerceIn(letter.tones.indices)])
            Spacer(Modifier.height(10.dp))
            Row {
                letter.tones.forEachIndexed { i, tone ->
                    Button(
                        onClick = {
                            onSelectTone(i)
                            onPlayAudio(audioKey(lesson.id, "${letter.pinyin}_tone$i"))
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (i == selectedTone) CandyColors.Sun else CandyColors.Card,
                            contentColor = CandyColors.Ink,
                        ),
                        modifier = Modifier.padding(end = 6.dp).candyFocus(),
                    ) { Text(tone, fontSize = 22.sp, fontWeight = FontWeight.Black) }
                }
            }
        } else {
            Text("声调操练", fontSize = 14.sp, color = CandyColors.InkSoft)
            Spacer(Modifier.height(10.dp))
            lesson.toneDrills.forEach { drill ->
                Box(
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .fillMaxWidth(0.8f)
                        .background(CandyColors.Card, RoundedCornerShape(10.dp))
                        .border(2.dp, CandyColors.CardBorder, RoundedCornerShape(10.dp))
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                ) {
                    Text(drill, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = CandyColors.FinalRed)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row {
            Button(
                onClick = onReadAlong,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (readAlongDone) CandyColors.Mint else CandyColors.Coral,
                    contentColor = Color.White,
                ),
                modifier = Modifier.candyFocus(),
            ) { Text(if (readAlongDone) "✓ 已跟读" else "🎤 跟读", fontSize = 16.sp) }
            if (toned.size > 1) {
                Spacer(Modifier.width(10.dp))
                Button(
                    onClick = { onSelectLetter((letterIndex + 1) % toned.size) },
                    colors = plainButtonColors(),
                    modifier = Modifier.candyFocus(),
                ) { Text("换一个字母 ▶", fontSize = 16.sp) }
            }
        }
        Text("跟读完成可多得一颗星 ⭐", fontSize = 13.sp, color = CandyColors.InkSoft)
    }
}

@Composable
private fun StrokeStep(
    lesson: Lesson,
    letterIndex: Int,
    strokeIndex: Int,
    onSelectLetter: (Int) -> Unit,
    onStrokeNext: (Int) -> Unit,
) {
    val letters = lesson.letters
    val letter = letters[letterIndex.coerceIn(letters.indices)]
    val hint = StrokeOrderHints.hintOf(letter.pinyin)
    val strokeCount = Regex("([一两二三四五])笔").find(hint)?.groupValues?.get(1)?.let {
        when (it) { "一" -> 1; "两" -> 2; "三" -> 3; "四" -> 4; else -> 0 }
    } ?: 0
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    ) {
        Text("笔顺演示", fontSize = 14.sp, color = CandyColors.InkSoft)
        Spacer(Modifier.height(8.dp))
        LetterCard(letter, letter.pinyin)
        Spacer(Modifier.height(10.dp))
        Text(hint, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CandyColors.Ink)
        Spacer(Modifier.height(10.dp))
        if (strokeCount > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                repeat(strokeCount) { i ->
                    Box(
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .size(if (i <= strokeIndex) 22.dp else 16.dp)
                            .background(
                                if (i <= strokeIndex) CandyColors.Coral else CandyColors.CardBorder,
                                CircleShape,
                            ),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = { onStrokeNext((strokeIndex + 1).coerceAtMost(strokeCount - 1)) },
                    enabled = strokeIndex < strokeCount - 1,
                    colors = ButtonDefaults.buttonColors(CandyColors.Sky, Color.White),
                    modifier = Modifier.candyFocus(),
                ) { Text("下一笔 ✍", fontSize = 16.sp) }
            }
        } else {
            Text("多字母组合：跟随写一写模块练习", fontSize = 14.sp, color = CandyColors.InkSoft)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            letters.forEachIndexed { i, l ->
                Button(
                    onClick = { onSelectLetter(i) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (i == letterIndex) CandyColors.Sun else CandyColors.Card,
                        contentColor = CandyColors.Ink,
                    ),
                    modifier = Modifier.candyFocus(),
                ) { Text(l.pinyin, fontSize = 18.sp, fontWeight = FontWeight.Black) }
            }
        }
    }
}

/** 拼读练习内容：优先音节拼读 → 整体认读 → 四声复习 */
private fun practiceLines(lesson: Lesson): List<String> {
    if (lesson.syllables.isNotEmpty()) return emptyList()
    if (lesson.holisticSyllables.isNotEmpty()) return lesson.holisticSyllables
    return lesson.toneDrills
}

@Composable
private fun SyllableStep(
    lesson: Lesson,
    answers: Map<Int, Boolean>,
    onAnswer: (Int, Boolean) -> Unit,
) {
    // 非测验内容（整体认读 / 四声复习）：浏览确认即计为完成
    if (lesson.syllables.isEmpty()) {
        val lines = practiceLines(lesson)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        ) {
            Text(
                if (lesson.holisticSyllables.isNotEmpty()) "整体认读音节（直接整体读）" else "四声复习",
                fontSize = 14.sp, color = CandyColors.InkSoft,
            )
            Spacer(Modifier.height(10.dp))
            lines.forEach { line ->
                Box(
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .fillMaxWidth(0.8f)
                        .background(CandyColors.Card, RoundedCornerShape(10.dp))
                        .border(2.dp, CandyColors.HolisticGreen.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                ) {
                    Text(line, fontSize = 26.sp, fontWeight = FontWeight.Black, color = CandyColors.HolisticGreen)
                }
            }
            Button(
                onClick = { onAnswer(0, true) },
                colors = ButtonDefaults.buttonColors(CandyColors.Mint, Color.White),
                modifier = Modifier.padding(top = 8.dp).candyFocus(),
                enabled = answers[0] != true,
            ) { Text(if (answers[0] == true) "✓ 已完成" else "我读完了 ✓", fontSize = 16.sp) }
        }
        return
    }

    val items = lesson.syllables
    val distractorPool = remember(lesson.id) {
        listOf("le", "he", "qu", "xi", "zhu", "che", "shu", "bao", "tou", "men", "hai", "kou")
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    ) {
        Text("拼一拼：声母 ＋ 韵母 → 音节", fontSize = 14.sp, color = CandyColors.InkSoft)
        Spacer(Modifier.height(8.dp))
        items.forEachIndexed { idx, item ->
            val answered = answers[idx]
            val options = remember(item.result, lesson.id) {
                val others = items.map { it.result }.filter { it != item.result }.shuffled()
                val extras = distractorPool
                    .filter { it != item.result && it !in items.map { s -> s.result } }
                    .shuffled()
                ((others + extras).distinct().take(3) + item.result).shuffled()
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(vertical = 6.dp)
                    .fillMaxWidth(0.9f)
                    .background(CandyColors.Card, RoundedCornerShape(12.dp))
                    .border(2.dp, CandyColors.CardBorder, RoundedCornerShape(12.dp))
                    .padding(vertical = 8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.initial, fontSize = 30.sp, fontWeight = FontWeight.Black, color = CandyColors.InitialBlue)
                    Text(" ＋ ", fontSize = 22.sp, color = CandyColors.InkSoft)
                    Text(item.finalPinyin, fontSize = 30.sp, fontWeight = FontWeight.Black, color = CandyColors.FinalRed)
                    Text(" ＝ ？", fontSize = 22.sp, color = CandyColors.InkSoft)
                    item.word?.let {
                        Spacer(Modifier.width(6.dp))
                        Text("（$it）", fontSize = 16.sp, color = CandyColors.InkSoft)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEach { opt ->
                        val isAnswer = opt == item.result
                        Button(
                            onClick = { onAnswer(idx, opt == item.result) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when {
                                    answered == true && isAnswer -> CandyColors.Mint
                                    else -> CandyColors.Card
                                },
                                contentColor = if (answered == true && isAnswer) Color.White else CandyColors.Ink,
                            ),
                            modifier = Modifier.candyFocus(),
                        ) { Text(opt, fontSize = 20.sp, fontWeight = FontWeight.Black) }
                    }
                }
            }
        }
        Text("全部选对可得第三颗星 ⭐", fontSize = 13.sp, color = CandyColors.InkSoft)
    }
}

@Composable
private fun RewardStep(
    stars: Int,
    onRestart: () -> Unit,
    onNext: (() -> Unit)?,
    onExit: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize(),
    ) {
        Text("🍭", fontSize = 64.sp)
        Spacer(Modifier.height(6.dp))
        Text("本课完成！", fontSize = 34.sp, fontWeight = FontWeight.Black, color = CandyColors.Ink)
        Spacer(Modifier.height(8.dp))
        Row {
            repeat(3) { i ->
                Text(if (i < stars) "⭐" else "☆", fontSize = 44.sp)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            when {
                stars >= 3 -> "太棒了，三颗星！"
                stars == 2 -> "很好！拼读再练练拿三星"
                stars == 1 -> "完成啦！记得跟读和拼读哦"
                else -> "继续加油！"
            },
            fontSize = 16.sp, color = CandyColors.InkSoft,
        )
        Spacer(Modifier.height(14.dp))
        Row {
            Button(onClick = onRestart, colors = plainButtonColors(), modifier = Modifier.candyFocus()) {
                Text("再学一遍", fontSize = 16.sp)
            }
            Spacer(Modifier.width(10.dp))
            if (onNext != null) {
                Button(
                    onClick = onNext,
                    colors = ButtonDefaults.buttonColors(CandyColors.Coral, Color.White),
                    modifier = Modifier.candyFocus(),
                ) { Text("下一课 ▶", fontSize = 16.sp, fontWeight = FontWeight.Black) }
                Spacer(Modifier.width(10.dp))
            }
            Button(onClick = onExit, colors = plainButtonColors(), modifier = Modifier.candyFocus()) {
                Text("返回首页", fontSize = 16.sp)
            }
        }
    }
}
