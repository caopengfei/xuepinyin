package com.rakor.xuepinyin.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rakor.xuepinyin.data.AssetsCourseRepository
import com.rakor.xuepinyin.ui.home.HomeScreen

@Composable
fun PinyinApp(repository: AssetsCourseRepository) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            HomeScreen(
                course = repository.course,
                completedLessonIds = emptySet(), // Plan 2 接入真实进度
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
            StubModuleScreen("第 $id 课 · 学一学", onBack = { nav.popBackStack() })
        }
        composable("read") { StubModuleScreen("读一读", onBack = { nav.popBackStack() }) }
        composable("write") { StubModuleScreen("写一写", onBack = { nav.popBackStack() }) }
        composable("play") { StubModuleScreen("玩一玩", onBack = { nav.popBackStack() }) }
        composable("table") { StubModuleScreen("拼音总表", onBack = { nav.popBackStack() }) }
        composable("parent") { StubModuleScreen("家长中心", onBack = { nav.popBackStack() }) }
    }
}
