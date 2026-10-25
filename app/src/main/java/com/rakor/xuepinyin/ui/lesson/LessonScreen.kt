package com.rakor.xuepinyin.ui.lesson

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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.mutableStateMapOf
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
import com.rakor.xuepinyin.domain.StrokeOrderHints
import com.rakor.xuepinyin.domain.model.Lesson
import com.rakor.xuepinyin.domain.model.LetterType
import com.rakor.xuepinyin.domain.model.PinyinLetter
import com.rakor.xuepinyin.domain.progress.LessonProgress
import com.rakor.xuepinyin.domain.progress.LessonStep
import com.rakor.xuepinyin.ui.theme.AppColors
import com.rakor.xuepinyin.ui.theme.CaptionStyle
import com.rakor.xuepinyin.ui.theme.TitleStyle
import com.rakor.xuepinyin.ui.theme.appFocus
import com.rakor.xuepinyin.ui.theme.rememberTvFocus
import com.rakor.xuepinyin.ui.theme.tvFocusTarget

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

    // 进入课程播欢迎语
    LaunchedEffect(lesson.id) { onPlayAudio("common/1") }

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
                    onPlayAudio = onPlayAudio,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        if (stepIndex < 4) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (stepIndex > 0) {
                    Button(
                        onClick = { stepIndex -= 1 },
                        modifier = Modifier.appFocus(),
                        colors = plainButtonColors(),
                        shape = RoundedCornerShape(12.dp),
                    ) { Text("上一步", fontSize = 18.sp) }
                    Spacer(Modifier.width(10.dp))
                }
                Button(
                    onClick = ::goNextStep,
                    modifier = Modifier.appFocus(),
                    colors = ButtonDefaults.buttonColors(AppColors.Coral, Color.White),
                    shape = RoundedCornerShape(12.dp),
                ) { Text(if (stepIndex == 3) "完成拼读" else "下一步", fontSize = 18.sp) }
            }
        }
    }
}

@Composable
private fun plainButtonColors() = ButtonDefaults.buttonColors(
    containerColor = AppColors.Card,
    contentColor = AppColors.Ink,
)

@Composable
private fun LessonHeader(lesson: Lesson, stepIndex: Int, onExit: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Button(
            onClick = onExit,
            colors = plainButtonColors(),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.appFocus(),
        ) {
            Text("返回", fontSize = 16.sp)
        }
        Spacer(Modifier.width(12.dp))
        Text("第 ${lesson.id} 课 · ${lesson.title}", style = TitleStyle, fontSize = 24.sp)
        Spacer(Modifier.width(16.dp))
        STEP_NAMES.forEachIndexed { i, name ->
            val active = i == stepIndex
            Box(
                modifier = Modifier
                    .padding(end = 6.dp)
                    .background(
                        if (active) AppColors.Coral else AppColors.Card,
                        RoundedCornerShape(8.dp),
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text("${i + 1} $name", fontSize = 12.sp, color = if (active) Color.White else AppColors.InkSoft)
            }
        }
    }
}

internal fun letterColor(letter: PinyinLetter): Color = when (letter.type) {
    LetterType.INITIAL -> AppColors.InitialBlue
    else -> AppColors.FinalRed
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
                style = TitleStyle,
                fontSize = 84.sp,
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
    // 切换字母自动播呼读音
    LaunchedEffect(lesson.id, letterIndex) {
        onPlayAudio(audioKey(lesson.id, letter.pinyin))
    }
    // TV 默认焦点落在“听一听”：进入认读步骤、点上一/下一字母切换后都回到这里（触控模式不抢焦点）
    val listenFocus = rememberTvFocus(key = letterIndex)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.height(6.dp))
        Text("认读字母（${letterIndex + 1}/${letters.size}）", style = CaptionStyle)
        Spacer(Modifier.height(8.dp))
        LetterCard(letter, letter.pinyin)
        Spacer(Modifier.height(10.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            letter.sceneWord?.let {
                Text("$it —— ${letter.pinyin}", style = TitleStyle, fontSize = 22.sp)
            }
            letter.mnemonic?.let {
                Spacer(Modifier.height(2.dp))
                Text(it, style = TitleStyle, fontSize = 20.sp, color = AppColors.InkSoft)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row {
            Button(
                onClick = { if (letterIndex > 0) onSelectLetter(letterIndex - 1) },
                enabled = letterIndex > 0,
                colors = plainButtonColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.appFocus(),
            ) { Text("上一字母", fontSize = 16.sp) }
            Spacer(Modifier.width(10.dp))
            Button(
                onClick = { onPlayAudio(audioKey(lesson.id, letter.pinyin)) },
                colors = ButtonDefaults.buttonColors(AppColors.Honey, Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.tvFocusTarget(listenFocus).appFocus(),
            ) {
                Icon(painterResource(R.drawable.ic_music), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("听一听", fontSize = 16.sp)
            }
            Spacer(Modifier.width(10.dp))
            Button(
                onClick = { if (letterIndex < letters.size - 1) onSelectLetter(letterIndex + 1) },
                enabled = letterIndex < letters.size - 1,
                colors = plainButtonColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.appFocus(),
            ) { Text("下一字母", fontSize = 16.sp) }
        }
        lesson.note?.let {
            Spacer(Modifier.height(10.dp))
            Text("小提示：$it", style = CaptionStyle, fontSize = 15.sp)
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
    // 默认焦点：有声调时落在第一个声调按钮，纯口诀操练时落在跟读
    val tonesFocus = rememberTvFocus(key = letterIndex)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    ) {
        if (toned.isNotEmpty()) {
            val letter = toned[letterIndex.coerceIn(toned.indices)]
            Text("声调练习（${letterIndex + 1}/${toned.size}）", style = CaptionStyle)
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
                            containerColor = if (i == selectedTone) AppColors.Coral else AppColors.Card,
                            contentColor = if (i == selectedTone) Color.White else AppColors.Ink,
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .then(if (i == 0) Modifier.tvFocusTarget(tonesFocus) else Modifier)
                            .appFocus(),
                    ) { Text(tone, style = TitleStyle, fontSize = 22.sp) }
                }
            }
        } else {
            Text("声调操练", style = CaptionStyle)
            Spacer(Modifier.height(10.dp))
            lesson.toneDrills.forEach { drill ->
                Box(
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .fillMaxWidth(0.8f)
                        .background(AppColors.Card, RoundedCornerShape(10.dp))
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                ) {
                    Text(drill, style = TitleStyle, fontSize = 26.sp, color = AppColors.FinalRed)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row {
            Button(
                onClick = onReadAlong,
                colors = if (readAlongDone) plainButtonColors() else ButtonDefaults.buttonColors(AppColors.Coral, Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .then(if (toned.isEmpty()) Modifier.tvFocusTarget(tonesFocus) else Modifier)
                    .appFocus(),
            ) {
                Icon(
                    painterResource(if (readAlongDone) R.drawable.ic_check else R.drawable.ic_mic),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(if (readAlongDone) "已跟读" else "跟读", fontSize = 16.sp)
            }
            if (toned.size > 1) {
                Spacer(Modifier.width(10.dp))
                Button(
                    onClick = { onSelectLetter((letterIndex + 1) % toned.size) },
                    colors = plainButtonColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.appFocus(),
                ) { Text("换一个字母", fontSize = 16.sp) }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("跟读完成可多得一颗星", style = CaptionStyle, fontSize = 13.sp)
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
    // 默认焦点落在“下一笔”，连按 OK 逐笔演示
    val strokeFocus = rememberTvFocus()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    ) {
        Text("笔顺演示", style = CaptionStyle)
        Spacer(Modifier.height(8.dp))
        LetterCard(letter, letter.pinyin)
        Spacer(Modifier.height(10.dp))
        Text(hint, style = TitleStyle, fontSize = 20.sp)
        Spacer(Modifier.height(10.dp))
        if (strokeCount > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                repeat(strokeCount) { i ->
                    Box(
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .size(if (i <= strokeIndex) 22.dp else 16.dp)
                            .background(
                                if (i <= strokeIndex) AppColors.Coral else AppColors.TrackBrown,
                                CircleShape,
                            ),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = { onStrokeNext((strokeIndex + 1).coerceAtMost(strokeCount - 1)) },
                    enabled = strokeIndex < strokeCount - 1,
                    colors = ButtonDefaults.buttonColors(AppColors.Coral, Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.tvFocusTarget(strokeFocus).appFocus(),
                ) { Text("下一笔", fontSize = 16.sp) }
            }
        } else {
            Text("多字母组合：跟随写一写模块练习", style = CaptionStyle)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            letters.forEachIndexed { i, l ->
                Button(
                    onClick = { onSelectLetter(i) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (i == letterIndex) AppColors.Coral else AppColors.Card,
                        contentColor = if (i == letterIndex) Color.White else AppColors.Ink,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.appFocus(),
                ) { Text(l.pinyin, style = TitleStyle, fontSize = 18.sp) }
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
        // 默认焦点落在“我读完了”
        val readFocus = rememberTvFocus()
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        ) {
            Text(
                if (lesson.holisticSyllables.isNotEmpty()) "整体认读音节（直接整体读）" else "四声复习",
                style = CaptionStyle,
            )
            Spacer(Modifier.height(10.dp))
            lines.forEach { line ->
                Box(
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .fillMaxWidth(0.8f)
                        .background(AppColors.Card, RoundedCornerShape(10.dp))
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                ) {
                    Text(line, style = TitleStyle, fontSize = 26.sp, color = AppColors.HolisticGreen)
                }
            }
            Button(
                onClick = { onAnswer(0, true) },
                colors = if (answers[0] == true) plainButtonColors() else ButtonDefaults.buttonColors(AppColors.Coral, Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(top = 8.dp).tvFocusTarget(readFocus).appFocus(),
                enabled = answers[0] != true,
            ) { Text(if (answers[0] == true) "已完成" else "我读完了", fontSize = 16.sp) }
        }
        return
    }

    val items = lesson.syllables
    val distractorPool = remember(lesson.id) {
        listOf("le", "he", "qu", "xi", "zhu", "che", "shu", "bao", "tou", "men", "hai", "kou")
    }
    // 默认焦点落在第一道题的第一个选项
    val quizFocus = rememberTvFocus()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    ) {
        Text("拼一拼：声母 ＋ 韵母 → 音节", style = CaptionStyle)
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
                    .background(AppColors.Card, RoundedCornerShape(12.dp))
                    .padding(vertical = 8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.initial, style = TitleStyle, fontSize = 30.sp, color = AppColors.InitialBlue)
                    Text(" ＋ ", fontSize = 22.sp, color = AppColors.InkSoft)
                    Text(item.finalPinyin, style = TitleStyle, fontSize = 30.sp, color = AppColors.FinalRed)
                    Text(" ＝ ？", fontSize = 22.sp, color = AppColors.InkSoft)
                    item.word?.let {
                        Spacer(Modifier.width(6.dp))
                        Text("（$it）", style = CaptionStyle, fontSize = 16.sp)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEachIndexed { optIdx, opt ->
                        val isAnswer = opt == item.result
                        Button(
                            onClick = { onAnswer(idx, opt == item.result) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when {
                                    answered == true && isAnswer -> AppColors.HolisticGreen
                                    else -> AppColors.Card
                                },
                                contentColor = if (answered == true && isAnswer) Color.White else AppColors.Ink,
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .then(if (idx == 0 && optIdx == 0) Modifier.tvFocusTarget(quizFocus) else Modifier)
                                .appFocus(),
                        ) { Text(opt, style = TitleStyle, fontSize = 20.sp) }
                    }
                }
            }
        }
        Spacer(Modifier.height(2.dp))
        Text("全部选对可得第三颗星", style = CaptionStyle, fontSize = 13.sp)
    }
}

@Composable
private fun RewardStep(
    stars: Int,
    onRestart: () -> Unit,
    onNext: (() -> Unit)?,
    onExit: () -> Unit,
    onPlayAudio: (String) -> Unit = {},
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize(),
    ) {
        // 结算播奖励语（1~3 星对应 reward/1~3）
        LaunchedEffect(stars) {
            if (stars > 0) onPlayAudio("reward/${stars.coerceIn(1, 3)}")
        }
        // 默认焦点：有下一课时落在“下一课”，否则“再学一遍”
        val rewardFocus = rememberTvFocus()
        Text("本课完成！", style = TitleStyle, fontSize = 34.sp)
        Spacer(Modifier.height(10.dp))
        Row {
            repeat(3) { i ->
                Icon(
                    painterResource(R.drawable.ic_star),
                    contentDescription = null,
                    tint = if (i < stars) AppColors.Honey else AppColors.TrackBrown,
                    modifier = Modifier.size(44.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            when {
                stars >= 3 -> "太棒了，三颗星！"
                stars == 2 -> "很好！拼读再练练拿三星"
                stars == 1 -> "完成啦！记得跟读和拼读哦"
                else -> "继续加油！"
            },
            style = CaptionStyle,
            fontSize = 16.sp,
        )
        Spacer(Modifier.height(16.dp))
        Row {
            Button(
                onClick = onRestart,
                colors = plainButtonColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .then(if (onNext == null) Modifier.tvFocusTarget(rewardFocus) else Modifier)
                    .appFocus(),
            ) { Text("再学一遍", fontSize = 16.sp) }
            Spacer(Modifier.width(10.dp))
            if (onNext != null) {
                Button(
                    onClick = onNext,
                    colors = ButtonDefaults.buttonColors(AppColors.Coral, Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.tvFocusTarget(rewardFocus).appFocus(),
                ) { Text("下一课", fontSize = 16.sp) }
                Spacer(Modifier.width(10.dp))
            }
            Button(
                onClick = onExit,
                colors = plainButtonColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.appFocus(),
            ) { Text("返回首页", fontSize = 16.sp) }
        }
    }
}
