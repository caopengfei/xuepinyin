package com.rakor.xuepinyin.ui.theme

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object CandyColors {
    val Background = Color(0xFFFFF6E9)
    val Ink = Color(0xFF4A3B32)
    val InkSoft = Color(0xFF8A7A6D)
    val Coral = Color(0xFFFF6B6B)
    val Sun = Color(0xFFFFC94D)
    val Mint = Color(0xFF43C8B0)
    val Sky = Color(0xFF54A0FF)
    val Grape = Color(0xFF9B7EDE)
    val Card = Color(0xFFFFFFFF)
    val CardBorder = Color(0xFFECDCC8)
    val FinalRed = Color(0xFFD9455F)
    val InitialBlue = Color(0xFF2D6CDF)
    val HolisticGreen = Color(0xFF3F8F6B)
}

object CandyType {
    val caption = 16.sp
    val body = 20.sp
    val title = 28.sp
    val heroTitle = 56.sp
    val letter = 30.sp
    val bigLetter = 120.sp
}

private val CandyScheme = lightColorScheme(
    primary = CandyColors.Coral,
    onPrimary = Color.White,
    background = CandyColors.Background,
    onBackground = CandyColors.Ink,
    surface = CandyColors.Card,
    onSurface = CandyColors.Ink,
    secondary = CandyColors.Sun,
    onSecondary = CandyColors.Ink,
)

@Composable
fun CandyTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = CandyScheme, content = content)
}

/** TV 焦点态：放大 1.06 + 深可可描边 + 硬阴影（平板触控下不触发，视觉自动弱化） */
fun Modifier.candyFocus(shape: RoundedCornerShape = RoundedCornerShape(12.dp)): Modifier = composed {
    var focused by remember { mutableStateOf(false) }
    this
        .onFocusChanged { focused = it.isFocused || it.hasFocus }
        .graphicsLayer {
            val scale = if (focused) 1.06f else 1f
            scaleX = scale
            scaleY = scale
        }
        .border(
            width = if (focused) 3.dp else 0.dp,
            color = if (focused) CandyColors.Ink else Color.Transparent,
            shape = shape,
        )
        .shadow(elevation = if (focused) 8.dp else 0.dp, shape = shape)
}
