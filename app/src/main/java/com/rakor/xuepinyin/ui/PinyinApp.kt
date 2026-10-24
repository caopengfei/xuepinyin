package com.rakor.xuepinyin.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rakor.xuepinyin.data.AssetsCourseRepository
import com.rakor.xuepinyin.data.ProgressRepository
import com.rakor.xuepinyin.domain.nextLessonId
import com.rakor.xuepinyin.ui.home.HomeScreen
import com.rakor.xuepinyin.ui.lesson.LessonScreen
import kotlinx.coroutines.launch

@Composable
fun PinyinApp(
    repository: AssetsCourseRepository,
    progressRepository: ProgressRepository,
) {
    val nav = rememberNavController()
    val scope = rememberCoroutineScope()
    val progress by progressRepository.state.collectAsState()
    val course = repository.course

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
            )
        }
        composable("read") { StubModuleScreen("读一读", onBack = { nav.popBackStack() }) }
        composable("write") { StubModuleScreen("写一写", onBack = { nav.popBackStack() }) }
        composable("play") { StubModuleScreen("玩一玩", onBack = { nav.popBackStack() }) }
        composable("table") { StubModuleScreen("拼音总表", onBack = { nav.popBackStack() }) }
        composable("parent") { StubModuleScreen("家长中心", onBack = { nav.popBackStack() }) }
    }
}
