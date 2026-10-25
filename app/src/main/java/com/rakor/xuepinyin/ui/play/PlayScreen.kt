package com.rakor.xuepinyin.ui.play

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.rakor.xuepinyin.domain.game.QuizGenerator
import com.rakor.xuepinyin.domain.model.Course
import com.rakor.xuepinyin.ui.theme.AppColors
import com.rakor.xuepinyin.ui.theme.CaptionStyle
import com.rakor.xuepinyin.ui.theme.TitleStyle
import com.rakor.xuepinyin.ui.theme.appFocus
import com.rakor.xuepinyin.ui.theme.rememberTvFocus
import com.rakor.xuepinyin.ui.theme.tvFocusTarget
import kotlinx.coroutines.delay
import kotlin.random.Random

private const val ROUNDS = 6

/** 玩一玩：找字母 / 选声调 */
@Composable
fun PlayScreen(
    course: Course,
    completedLessonIds: Set<Int>,
    onBack: () -> Unit,
    onPlayAudio: (String) -> Unit,
) {
    val scope = (completedLessonIds.maxOrNull() ?: 0) + 1
    var game by remember { mutableStateOf<String?>(null) }
    // 遥控器返回键：游戏中退回游戏选择，而不是直接退出玩一玩
    BackHandler(enabled = game != null) { game = null }
    val gen = remember { QuizGenerator(Random(System.nanoTime())) }

    when (game) {
        null -> GameSelect(onBack = onBack, onPick = { game = it })
        "candy" -> CandyGame(
            questions = remember { gen.candyQuestions(course, scope, ROUNDS) },
            onExit = { game = null },
            onPlayAudio = onPlayAudio,
        )
        "tone" -> ToneGame(
            questions = remember { gen.toneQuestions(course, scope, ROUNDS) },
            onExit = { game = null },
            onPlayAudio = onPlayAudio,
        )
    }
}

@Composable
private fun GameSelect(onBack: () -> Unit, onPick: (String) -> Unit) {
    // TV 默认焦点落在第一张游戏卡
    val gameFocus = rememberTvFocus()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().padding(28.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(AppColors.Card, AppColors.Ink),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.appFocus(),
            ) { Text("返回") }
            Spacer(Modifier.width(12.dp))
            Text("玩一玩", style = TitleStyle, fontSize = 24.sp)
        }
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            GameCard("找字母", "听一听，选出字母", Modifier.tvFocusTarget(gameFocus)) { onPick("candy") }
            GameCard("选声调", "听读音，选出声调") { onPick("tone") }
        }
    }
}

@Composable
private fun GameCard(title: String, sub: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(AppColors.Card, AppColors.Ink),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.width(260.dp).height(180.dp).appFocus(RoundedCornerShape(16.dp)),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = TitleStyle, fontSize = 22.sp)
            Spacer(Modifier.height(6.dp))
            Text(sub, style = CaptionStyle)
        }
    }
}

@Composable
private fun CandyGame(
    questions: List<QuizGenerator.CandyQuestion>,
    onExit: () -> Unit,
    onPlayAudio: (String) -> Unit,
) {
    GameFrame(
        title = "找字母",
        hint = "听一听，把听到的字母找出来",
        questions = questions,
        onExit = onExit,
        onPlayAudio = onPlayAudio,
    )
}

@Composable
private fun ToneGame(
    questions: List<QuizGenerator.ToneQuestion>,
    onExit: () -> Unit,
    onPlayAudio: (String) -> Unit,
) {
    GameFrame(
        title = "选声调",
        hint = "听读音，选出正确的声调",
        questions = questions,
        onExit = onExit,
        onPlayAudio = onPlayAudio,
    )
}

@Composable
private fun <Q> GameFrame(
    title: String,
    hint: String,
    questions: List<Q>,
    onExit: () -> Unit,
    onPlayAudio: (String) -> Unit,
) where Q : Any {
    var index by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var picked by remember { mutableStateOf<String?>(null) }
    var finished by remember { mutableStateOf(false) }
    // TV 默认焦点：每轮落在第一个选项（随轮次自动回焦），结算时落在“再玩一次”
    val optionFocus = rememberTvFocus(key = index)

    fun question(i: Int): QuizGenerator.CandyQuestion? = questions.getOrNull(i) as? QuizGenerator.CandyQuestion
    fun toneQuestion(i: Int): QuizGenerator.ToneQuestion? = questions.getOrNull(i) as? QuizGenerator.ToneQuestion
    val audioKey = question(index)?.audioKey ?: toneQuestion(index)?.audioKey
    val answer = question(index)?.answer ?: toneQuestion(index)?.answer
    val options = question(index)?.options ?: toneQuestion(index)?.options ?: emptyList()

    LaunchedEffect(index, finished) {
        if (!finished && audioKey != null) {
            picked = null
            onPlayAudio(audioKey)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onExit,
                colors = ButtonDefaults.buttonColors(AppColors.Card, AppColors.Ink),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.appFocus(),
            ) { Text("返回") }
            Spacer(Modifier.width(12.dp))
            Text(title, style = TitleStyle, fontSize = 24.sp)
            Spacer(Modifier.width(16.dp))
            Text(hint, style = CaptionStyle)
            Spacer(Modifier.weight(1f))
            Text("得分 $score", style = TitleStyle, fontSize = 20.sp)
            Spacer(Modifier.width(10.dp))
            Text("${index + 1}/$ROUNDS", style = CaptionStyle, fontSize = 16.sp)
        }
        Spacer(Modifier.height(14.dp))
        if (finished) {
            // TV 默认焦点落在“再玩一次”
            val doneFocus = rememberTvFocus()
            Text("本局完成！", style = TitleStyle, fontSize = 30.sp)
            Spacer(Modifier.height(6.dp))
            Text("答对 $score / $ROUNDS 题", style = TitleStyle, fontSize = 20.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                if (score >= ROUNDS - 1) "太厉害了！" else "再多练一练，会越来越棒",
                style = CaptionStyle,
                fontSize = 16.sp,
            )
            Spacer(Modifier.height(16.dp))
            Row {
                Button(
                    onClick = { index = 0; score = 0; finished = false },
                    colors = ButtonDefaults.buttonColors(AppColors.Card, AppColors.Ink),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.tvFocusTarget(doneFocus).appFocus(),
                ) { Text("再玩一次") }
                Spacer(Modifier.width(10.dp))
                Button(
                    onClick = onExit,
                    colors = ButtonDefaults.buttonColors(AppColors.Coral, Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.appFocus(),
                ) { Text("返回") }
            }
        } else {
            Button(
                onClick = { audioKey?.let(onPlayAudio) },
                colors = ButtonDefaults.buttonColors(AppColors.Honey, Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(72.dp).width(200.dp).appFocus(),
            ) {
                Icon(painterResource(R.drawable.ic_music), contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text("再听一遍", fontSize = 20.sp)
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                options.forEachIndexed { optIdx, opt ->
                    val isAnswer = opt == answer
                    val (bg, content) = when {
                        picked == null -> AppColors.Card to AppColors.Ink
                        picked == opt && isAnswer -> AppColors.HolisticGreen to Color.White
                        picked == opt -> AppColors.CoralDeep to Color.White
                        else -> AppColors.Card to AppColors.Ink
                    }
                    Button(
                        onClick = {
                            if (picked == null) {
                                picked = opt
                                val correct = opt == answer
                                if (correct) score += 1
                                onPlayAudio(if (correct) "common/4" else "common/5")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(bg, content),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .width(150.dp)
                            .height(110.dp)
                            .then(if (optIdx == 0) Modifier.tvFocusTarget(optionFocus) else Modifier)
                            .appFocus(RoundedCornerShape(16.dp)),
                    ) {
                        Text(opt, style = TitleStyle, fontSize = 40.sp, color = content)
                    }
                }
            }
            LaunchedEffect(picked) {
                if (picked != null) {
                    delay(1100)
                    if (index + 1 >= questions.size) finished = true else index += 1
                }
            }
        }
    }
}
