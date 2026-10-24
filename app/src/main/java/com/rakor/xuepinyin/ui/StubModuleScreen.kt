package com.rakor.xuepinyin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.rakor.xuepinyin.ui.theme.CandyColors
import com.rakor.xuepinyin.ui.theme.CandyType

/** 临时占位页：Plan 2-5 会用真实模块页替换 */
@Composable
fun StubModuleScreen(title: String, onBack: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(CandyColors.Background),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontSize = CandyType.title, color = CandyColors.Ink)
            Text("本模块在后续计划中实现", fontSize = CandyType.body, color = CandyColors.InkSoft)
            Button(onClick = onBack) { Text("返回", fontSize = 18.sp) }
        }
    }
}
