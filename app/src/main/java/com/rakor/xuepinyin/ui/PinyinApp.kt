package com.rakor.xuepinyin.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
import com.rakor.xuepinyin.ui.write.WriteScreen
import kotlinx.coroutines.launch

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

    // 语速设置同步到播放器
    LaunchedEffect(settings.voiceSpeed) { audioPlayer?.speed = settings.voiceSpeed }

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
}
