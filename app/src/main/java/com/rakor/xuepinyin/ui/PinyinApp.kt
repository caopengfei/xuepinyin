package com.rakor.xuepinyin.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import com.rakor.xuepinyin.ui.theme.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.rakor.xuepinyin.ui.theme.AppColors
import com.rakor.xuepinyin.ui.theme.CaptionStyle
import com.rakor.xuepinyin.ui.theme.LocalTvFocusNudge
import com.rakor.xuepinyin.ui.theme.TitleStyle
import com.rakor.xuepinyin.ui.theme.appFocus
import com.rakor.xuepinyin.ui.theme.rememberTvFocus
import com.rakor.xuepinyin.ui.theme.tvFocusTarget
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
    // 护眼弹层关闭会清空焦点，递增 nudge 让被覆盖屏幕的默认焦点重新就位
    var focusNudge by remember { mutableIntStateOf(0) }

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

    CompositionLocalProvider(LocalTvFocusNudge provides focusNudge) {
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
                EyeRestOverlay(onDismiss = {
                    eyeRest = false
                    focusNudge++
                })
            }
        }
    }
}

@Composable
private fun EyeRestOverlay(onDismiss: () -> Unit) {
    // TV：弹层出现时把焦点引导到按钮，否则遥控器按键仍作用于被遮住的底层屏幕；返回键等同「继续学习」
    BackHandler(onBack = onDismiss)
    val tvFocus = rememberTvFocus()
    Box(
        modifier = Modifier
            .fillMaxSize()
            // 80% 暖棕遮罩（ToastBrown 深度，遮罩层需要更强的压暗）
            .background(Color(0xCC5A4634)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("休息一下，保护眼睛", style = TitleStyle, fontSize = 30.sp, color = Color.White)
            Spacer(Modifier.height(8.dp))
            Text(
                "看看远处的绿树，20 分钟后再回来继续吧",
                style = CaptionStyle,
                fontSize = 16.sp,
                color = AppColors.CardFocused,
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(AppColors.Coral, Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.tvFocusTarget(tvFocus).appFocus(),
            ) { Text("继续学习", fontSize = 18.sp) }
        }
    }
}
