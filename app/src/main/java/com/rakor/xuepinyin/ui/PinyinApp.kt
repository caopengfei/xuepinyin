package com.rakor.xuepinyin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rakor.xuepinyin.audio.VoicePackPlayer
import com.rakor.xuepinyin.data.AssetsCourseRepository
import com.rakor.xuepinyin.data.ProgressRepository
import com.rakor.xuepinyin.data.SettingsRepository
import com.rakor.xuepinyin.domain.nextLessonId
import com.rakor.xuepinyin.ui.home.HomeScreen
import com.rakor.xuepinyin.ui.lesson.LessonScreen
import com.rakor.xuepinyin.ui.parent.ParentScreen
import com.rakor.xuepinyin.ui.play.PlayScreen
import com.rakor.xuepinyin.ui.read.ReadScreen
import com.rakor.xuepinyin.ui.table.PinyinTableScreen
import com.rakor.xuepinyin.ui.theme.CandyColors
import com.rakor.xuepinyin.ui.theme.candyFocus
import com.rakor.xuepinyin.ui.write.WriteScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val EYE_REST_MINUTES = 20L

@Composable
fun PinyinApp(
    repository: AssetsCourseRepository,
    progressRepository: ProgressRepository,
    settingsRepository: SettingsRepository,
    audioPlayer: VoicePackPlayer? = null,
) {
    val nav = rememberNavController()
    val scope = rememberCoroutineScope()
    val progress by progressRepository.state.collectAsState()
    val settings by settingsRepository.state.collectAsState()
    val course = repository.course
    var eyeRest by remember { mutableStateOf(false) }

    // 语速设置同步到播放器
    LaunchedEffect(settings.voiceSpeed) { audioPlayer?.speed = settings.voiceSpeed }

    // 护眼提醒：开启时每 20 分钟弹出一次
    LaunchedEffect(settings.eyeRestEnabled) {
        if (settings.eyeRestEnabled) {
            while (true) {
                delay(EYE_REST_MINUTES * 60 * 1000)
                eyeRest = true
                audioPlayer?.play("common/6")
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            HomeScreen(
                course = course,
                completedLessonIds = progress.completedLessonIds(),
                onOpenLesson = { id -> nav.navigate("lesson/$id") },
                onOpenModule = { route -> nav.navigate(route) },
                onOpenTable = { nav.navigate("table") },
                onOpenParent = { nav.navigate("parent") },
            )
        }
        composable(
            route = "lesson/{id}",
            arguments = listOf(navArgument("id") { type = NavType.IntType }),
        ) { entry ->
            val id = entry.arguments?.getInt("id") ?: 1
            val lesson = course.lesson(id) ?: return@composable
            LessonScreen(
                lesson = lesson,
                initialProgress = progress.of(id),
                totalLessons = course.lessons.size,
                onProgress = { transform ->
                    scope.launch { progressRepository.update(id, transform) }
                },
                onExit = { nav.popBackStack("home", inclusive = false) },
                onOpenNext = { nextId -> nav.navigate("lesson/$nextId") },
                onPlayAudio = { key -> audioPlayer?.play(key) },
            )
        }
        composable("read") {
            ReadScreen(
                course = course,
                completedLessonIds = progress.completedLessonIds(),
                onBack = { nav.popBackStack() },
                onPlayAudio = { key -> audioPlayer?.play(key) },
            )
        }
        composable("write") {
            WriteScreen(course = course, onBack = { nav.popBackStack() })
        }
        composable("play") {
            PlayScreen(
                course = course,
                completedLessonIds = progress.completedLessonIds(),
                onBack = { nav.popBackStack() },
                onPlayAudio = { key -> audioPlayer?.play(key) },
            )
        }
        composable("table") {
            PinyinTableScreen(
                course = course,
                onBack = { nav.popBackStack() },
                onPlayAudio = { key -> audioPlayer?.play(key) },
            )
        }
        composable("parent") {
            ParentScreen(
                course = course,
                progress = progress,
                settings = settings,
                onBack = { nav.popBackStack() },
                onSettingsChange = { transform ->
                    scope.launch { settingsRepository.update(transform) }
                },
            )
        }
        }
        if (eyeRest) {
            EyeRestOverlay(onDismiss = { eyeRest = false })
        }
    }
}

@Composable
private fun EyeRestOverlay(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC3A2E24)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("👀", fontSize = 56.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                "休息一下，保护眼睛",
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "看看远处的绿树，20 分钟后再回来继续吧",
                fontSize = 16.sp,
                color = Color(0xFFE8DCC8),
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(CandyColors.Sun, CandyColors.Ink),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.candyFocus(RoundedCornerShape(14.dp)),
            ) { Text("继续学习", fontSize = 18.sp, fontWeight = FontWeight.Black) }
        }
    }
}
