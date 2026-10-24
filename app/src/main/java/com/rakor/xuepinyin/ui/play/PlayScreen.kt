package com.rakor.xuepinyin.ui.play

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.rakor.xuepinyin.domain.game.QuizGenerator
import com.rakor.xuepinyin.domain.model.Course
import com.rakor.xuepinyin.ui.theme.CandyColors
import com.rakor.xuepinyin.ui.theme.candyFocus
import kotlinx.coroutines.delay
import kotlin.random.Random

private const val ROUNDS = 6

/** 玩一玩：摘糖果·找字母 / 声调跳跳 */
@Composable
fun PlayScreen(
    course: Course,
    completedLessonIds: Set<Int>,
    onBack: () -> Unit,
    onPlayAudio: (String) -> Unit,
) {
    val scope = (completedLessonIds.maxOrNull() ?: 0) + 1
    var game by remember { mutableStateOf<String?>(null) }
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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().background(CandyColors.Background).padding(28.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(CandyColors.Card, CandyColors.Ink),
                modifier = Modifier.candyFocus(),
            ) { Text("返回") }
            Spacer(Modifier.width(12.dp))
            Text("玩一玩", fontSize = 24.sp, fontWeight = FontWeight.Black, color = CandyColors.Ink)
        }
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            GameCard("🍬", "摘糖果", "听一听，找字母", CandyColors.Sun) { onPick("candy") }
            GameCard("🎵", "声调跳跳", "听读音，选声调", CandyColors.Mint) { onPick("tone") }
        }
    }
}

@Composable
private fun GameCard(emoji: String, title: String, sub: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(CandyColors.Card, CandyColors.Ink),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.width(260.dp).height(180.dp).candyFocus(RoundedCornerShape(16.dp)),
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
private fun CandyGame(
    questions: List<QuizGenerator.CandyQuestion>,
    onExit: () -> Unit,
    onPlayAudio: (String) -> Unit,
) {
    GameFrame(
        title = "摘糖果 · 找字母",
        hint = "听一听，把听到的字母摘下来",
        questions = questions,
        onExit = onExit,
        onPlayAudio = onPlayAudio,
        optionStyle = { it },
    )
}

@Composable
private fun ToneGame(
    questions: List<QuizGenerator.ToneQuestion>,
    onExit: () -> Unit,
    onPlayAudio: (String) -> Unit,
) {
    GameFrame(
        title = "声调跳跳",
        hint = "听读音，选出正确的声调",
        questions = questions,
        onExit = onExit,
        onPlayAudio = onPlayAudio,
        optionStyle = { it },
    )
}

@Composable
private fun <Q> GameFrame(
    title: String,
    hint: String,
    questions: List<Q>,
    onExit: () -> Unit,
    onPlayAudio: (String) -> Unit,
    optionStyle: @Composable (String) -> Unit,
) where Q : Any {
    var index by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var picked by remember { mutableStateOf<String?>(null) }
    var finished by remember { mutableStateOf(false) }

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
        modifier = Modifier.fillMaxSize().background(CandyColors.Background).padding(horizontal = 28.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onExit,
                colors = ButtonDefaults.buttonColors(CandyColors.Card, CandyColors.Ink),
                modifier = Modifier.candyFocus(),
            ) { Text("返回") }
            Spacer(Modifier.width(12.dp))
            Text(title, fontSize = 24.sp, fontWeight = FontWeight.Black, color = CandyColors.Ink)
            Spacer(Modifier.width(16.dp))
            Text(hint, fontSize = 14.sp, color = CandyColors.InkSoft)
            Spacer(Modifier.weight(1f))
            Text("🍬 $score", fontSize = 20.sp, fontWeight = FontWeight.Black, color = CandyColors.Ink)
            Spacer(Modifier.width(10.dp))
            Text("${index + 1}/$ROUNDS", fontSize = 16.sp, color = CandyColors.InkSoft)
        }
        Spacer(Modifier.height(14.dp))
        if (finished) {
            Text(if (score >= ROUNDS - 1) "🌟" else "🍬", fontSize = 64.sp)
            Spacer(Modifier.height(6.dp))
            Text("得了 $score 颗糖果！", fontSize = 30.sp, fontWeight = FontWeight.Black, color = CandyColors.Ink)
            Spacer(Modifier.height(6.dp))
            Text(
                if (score >= ROUNDS - 1) "太厉害了！" else "再多练一练，糖果会更多哦",
                fontSize = 16.sp, color = CandyColors.InkSoft,
            )
            Spacer(Modifier.height(14.dp))
            Row {
                Button(
                    onClick = { index = 0; score = 0; finished = false },
                    colors = ButtonDefaults.buttonColors(CandyColors.Card, CandyColors.Ink),
                    modifier = Modifier.candyFocus(),
                ) { Text("再玩一次") }
                Spacer(Modifier.width(10.dp))
                Button(
                    onClick = onExit,
                    colors = ButtonDefaults.buttonColors(CandyColors.Coral, Color.White),
                    modifier = Modifier.candyFocus(),
                ) { Text("返回") }
            }
        } else {
            Button(
                onClick = { audioKey?.let(onPlayAudio) },
                colors = ButtonDefaults.buttonColors(CandyColors.Sun, CandyColors.Ink),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.height(72.dp).width(200.dp).candyFocus(RoundedCornerShape(24.dp)),
            ) {
                Text("🔊 再听一遍", fontSize = 20.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                options.forEach { opt ->
                    val isAnswer = opt == answer
                    val state = when {
                        picked == null -> CandyColors.Card
                        picked == opt && isAnswer -> CandyColors.Mint
                        picked == opt -> CandyColors.Coral
                        else -> CandyColors.Card
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
                        colors = ButtonDefaults.buttonColors(state, if (picked != null && picked == opt && isAnswer) Color.White else CandyColors.Ink),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .width(150.dp)
                            .height(110.dp)
                            .candyFocus(RoundedCornerShape(18.dp)),
                    ) {
                        Text(
                            opt,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Black,
                            color = if (picked != null && picked == opt && isAnswer) Color.White else CandyColors.Ink,
                        )
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
