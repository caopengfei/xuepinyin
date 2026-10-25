package com.rakor.xuepinyin.ui.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.rakor.xuepinyin.ui.theme.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rakor.xuepinyin.R
import com.rakor.xuepinyin.data.AppSettings
import com.rakor.xuepinyin.data.SettingsRepository
import com.rakor.xuepinyin.domain.model.Course
import com.rakor.xuepinyin.domain.progress.ProgressState
import com.rakor.xuepinyin.ui.theme.AppColors
import com.rakor.xuepinyin.ui.theme.CaptionStyle
import com.rakor.xuepinyin.ui.theme.TitleStyle
import com.rakor.xuepinyin.ui.theme.appFocus

/** 家长中心：进度总览 + 设置 + 关于。onSettingsChange 由上层用协程写穿 SettingsRepository */
@Composable
fun ParentScreen(
    course: Course,
    progress: ProgressState,
    settings: AppSettings,
    onBack: () -> Unit,
    onSettingsChange: ((AppSettings) -> AppSettings) -> Unit,
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 12.dp),
    ) {
        val completed = progress.completedLessonIds()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(AppColors.Card, AppColors.Ink),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.appFocus(),
            ) { Text("返回") }
            Spacer(Modifier.width(12.dp))
            Text("家长中心", style = TitleStyle, fontSize = 24.sp)
        }
        Spacer(Modifier.height(12.dp))
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
        ) {
            // 进度总览
            Section("学习进度") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Stat("已学课程", "${completed.size} / ${course.lessons.size}")
                    Spacer(Modifier.width(18.dp))
                    Stat("累计星星", "${progress.totalStars()} 颗")
                }
                Spacer(Modifier.height(10.dp))
                course.units.forEach { unit ->
                    Text(unit.title, style = CaptionStyle, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    unit.lessonRange.mapNotNull { course.lesson(it) }.forEach { lesson ->
                        val p = progress.of(lesson.id)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp),
                        ) {
                            Text(
                                "第 ${lesson.id} 课  ${lesson.title}",
                                fontSize = 16.sp,
                                color = AppColors.Ink,
                            )
                            Spacer(Modifier.weight(1f))
                            if (p.isCompleted()) {
                                Row {
                                    repeat(3) { i ->
                                        Icon(
                                            painterResource(R.drawable.ic_star),
                                            contentDescription = null,
                                            tint = if (i < p.stars()) AppColors.Honey else AppColors.TrackBrown,
                                            modifier = Modifier
                                                .padding(start = 2.dp)
                                                .size(16.dp),
                                        )
                                    }
                                }
                            } else {
                                Text("未完成", style = CaptionStyle, fontSize = 15.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
            // 设置
            Section("设置") {
                SettingRow(
                    label = "护眼提醒（每 20 分钟）",
                    value = if (settings.eyeRestEnabled) "已开启" else "已关闭",
                    onClickLabel = "切换",
                ) {
                    onSettingsChange { it.copy(eyeRestEnabled = !it.eyeRestEnabled) }
                }
            }
            // 关于
            Section("关于") {
                Text(
                    "拼音乐园 v0.1.0\n依据《义务教育教科书·语文一年级上册》（统编版 2024）汉语拼音单元编制，" +
                        "共 3 个单元 13 课。内容供家庭辅导使用。",
                    style = CaptionStyle,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Card, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Text(title, style = TitleStyle, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(label, style = CaptionStyle, fontSize = 13.sp)
        Text(value, style = TitleStyle, fontSize = 26.sp, color = AppColors.Coral)
    }
}

@Composable
private fun SettingRow(
    label: String,
    value: String,
    onClickLabel: String,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp),
    ) {
        Text(label, fontSize = 16.sp, color = AppColors.Ink)
        Spacer(Modifier.weight(1f))
        Text(value, style = CaptionStyle, fontSize = 15.sp)
        Spacer(Modifier.width(12.dp))
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(AppColors.Card, AppColors.Ink),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.appFocus(),
        ) { Text(onClickLabel) }
    }
}
