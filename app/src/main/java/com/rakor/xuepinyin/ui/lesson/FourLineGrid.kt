package com.rakor.xuepinyin.ui.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 四线三格：四条横线构成上格/中格/下格；bandContent 槽位对齐中格
 * （供学一学认读/声调、写一写笔顺、拼音总表复用）。
 */
@Composable
fun FourLineGrid(
    modifier: Modifier = Modifier,
    height: Dp = 160.dp,
    lineColor: Color = Color(0xFFB98F5E),
    cardColor: Color = Color.White,
    bandContent: @Composable BoxScope.() -> Unit = {},
) {
    Surface(
        modifier = modifier.fillMaxWidth().height(height),
        shape = RoundedCornerShape(12.dp),
        color = cardColor,
        border = androidx.compose.foundation.BorderStroke(2.dp, lineColor.copy(alpha = 0.4f)),
    ) {
        Box {
            Column(Modifier.fillMaxHeight()) {
                repeat(3) {
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .border(1.dp, lineColor)
                            .background(Color.Transparent),
                    )
                }
            }
            // 中格槽位：内容不受中格 1/3 高度约束（大字母按四线格本义溢出到上/下格），垂直居中
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .fillMaxHeight(1f / 3f),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(align = Alignment.CenterVertically, unbounded = true),
                    content = bandContent,
                )
            }
        }
    }
}
