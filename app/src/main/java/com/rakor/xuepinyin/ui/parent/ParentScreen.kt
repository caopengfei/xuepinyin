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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rakor.xuepinyin.data.AppSettings
import com.rakor.xuepinyin.data.SettingsRepository
import com.rakor.xuepinyin.domain.model.Course
import com.rakor.xuepinyin.domain.progress.ProgressState
import com.rakor.xuepinyin.ui.theme.CandyColors
import com.rakor.xuepinyin.ui.theme.candyFocus

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
            .background(CandyColors.Background)
            .padding(horizontal = 28.dp, vertical = 12.dp),
    ) {
        val completed = progress.completedLessonIds()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(CandyColors.Card, CandyColors.Ink),
                modifier = Modifier.candyFocus(),
            ) { Text("返回") }
            Spacer(Modifier.width(12.dp))
            Text("家长中心", fontSize = 24.sp, fontWeight = FontWeight.Black, color = CandyColors.Ink)
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
                    Stat("累计星星", "⭐ ${progress.totalStars()}")
                }
                Spacer(Modifier.height(10.dp))
                course.units.forEach { unit ->
                    Text(
                        unit.title,
                        fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CandyColors.InkSoft,
                    )
                    Spacer(Modifier.height(4.dp))
                    unit.lessonRange.mapNotNull { course.lesson(it) }.forEach { lesson ->
                        val p = progress.of(lesson.id)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp),
                        ) {
                            Text(
                                "第 ${lesson.id} 课  ${lesson.title}",
                                fontSize = 16.sp, color = CandyColors.Ink,
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                if (p.isCompleted()) "⭐".repeat(p.stars()) + "☆".repeat(3 - p.stars())
                                else "未完成",
                                fontSize = 15.sp,
                                color = if (p.isCompleted()) CandyColors.Ink else CandyColors.InkSoft,
                            )
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
                    fontSize = 14.sp,
                    color = CandyColors.InkSoft,
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
            .background(CandyColors.Card, RoundedCornerShape(14.dp))
            .padding(16.dp),
    ) {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Black, color = CandyColors.Ink)
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(label, fontSize = 13.sp, color = CandyColors.InkSoft)
        Text(value, fontSize = 26.sp, fontWeight = FontWeight.Black, color = CandyColors.Coral)
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
        Text(label, fontSize = 16.sp, color = CandyColors.Ink)
        Spacer(Modifier.weight(1f))
        Text(value, fontSize = 15.sp, color = CandyColors.InkSoft)
        Spacer(Modifier.width(12.dp))
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(CandyColors.Sun, CandyColors.Ink),
            modifier = Modifier.candyFocus(),
        ) { Text(onClickLabel) }
    }
}
