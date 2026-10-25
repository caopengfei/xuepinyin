package com.rakor.xuepinyin.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rakor.xuepinyin.R
import com.rakor.xuepinyin.ui.InputMode
import com.rakor.xuepinyin.ui.rememberInputMode

/** 配色「童趣暖阳 · 奶油圆角」：奶油渐变底 + 白色圆角卡 + 珊瑚橙主色 + 暖棕文字（不用纯黑） */
object AppColors {
    // 奶油渐变背景（AppTheme 全屏铺底，屏幕自身不再设背景色）
    val CreamTop = Color(0xFFFFF8EC)
    val CreamMid = Color(0xFFFFEFD8)
    val CreamBottom = Color(0xFFFFE6C7)

    // 珊瑚橙主色系
    val Coral = Color(0xFFFF8A65)
    val CoralDeep = Color(0xFFF07048)
    val CoralTrack = Color(0x22FF8A65)
    val Honey = Color(0xFFD4954A)

    // 暖棕墨色文字
    val Ink = Color(0xFF5A4634)
    val InkSoft = Color(0xFF8A6D4E)

    // 卡片与功能色
    val Card = Color(0xFFFFFFFF)
    val CardFocused = Color(0xFFFFF2E2)
    val ScrimBrown = Color(0x665A4634)
    val TrackBrown = Color(0x1A5A4634)
    val DividerBrown = Color(0x33D4954A)

    // 拼音教学功能色（沿用课本配色惯例：声母蓝 / 韵母红 / 整体认读绿）
    val FinalRed = Color(0xFFD9455F)
    val InitialBlue = Color(0xFF2D6CDF)
    val HolisticGreen = Color(0xFF3F8F6B)
}

// 课本式字排：标题/大字用文楷 Medium，小字拼音用文楷 Regular，正文用系统黑体
val WenKai = FontFamily(Font(R.font.wenkai_subset, FontWeight.Normal))
val WenKaiMedium = FontFamily(Font(R.font.wenkai_medium_subset, FontWeight.Medium))

/** 页面标题 / 卡片标题 / 大字展示（字号、颜色就地覆盖） */
val TitleStyle = TextStyle(fontFamily = WenKaiMedium, fontWeight = FontWeight.Medium, color = AppColors.Ink)

/** 小节标签（珊瑚深色 + 大字距） */
val SectionLabelStyle = TextStyle(
    fontFamily = WenKaiMedium,
    fontWeight = FontWeight.Medium,
    fontSize = 15.sp,
    letterSpacing = 2.sp,
    color = AppColors.CoralDeep,
)

/** 拼音文本（文楷 Regular） */
val PinyinStyle = TextStyle(fontFamily = WenKai, fontWeight = FontWeight.Normal, fontSize = 18.sp, color = AppColors.Ink)

/** 正文（系统黑体） */
val BodyStyle = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 16.sp, color = AppColors.Ink)

/** 辅助说明（系统黑体） */
val CaptionStyle = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 14.sp, color = AppColors.InkSoft)

private val AppScheme = lightColorScheme(
    primary = AppColors.Coral,
    onPrimary = Color.White,
    secondary = AppColors.Honey,
    onSecondary = Color.White,
    background = AppColors.CreamTop,
    onBackground = AppColors.Ink,
    surface = AppColors.Card,
    onSurface = AppColors.Ink,
)

/** 主题 + 设备档位：TV 基准字号；平板横屏整体下调一档（fontScale × 0.85，触控焦点态自动弱化） */
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val inputMode = rememberInputMode()
    val density = LocalDensity.current
    val tierDensity = if (inputMode == InputMode.TOUCH) {
        Density(density.density, density.fontScale * 0.85f)
    } else {
        density
    }
    CompositionLocalProvider(LocalDensity provides tierDensity) {
        MaterialTheme(colorScheme = AppScheme) {
            // 全屏奶油渐变铺底，各屏幕保持透明
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(AppColors.CreamTop, AppColors.CreamMid, AppColors.CreamBottom),
                        ),
                    ),
            ) {
                content()
            }
        }
    }
}

/** TV 焦点态：放大 1.06 + 蜜橙暖染 + 珊瑚色细描边（不用投影——黑色阴影在奶油底/半透明底上会糊成脏光晕；平板触控下不触发） */
fun Modifier.appFocus(shape: RoundedCornerShape = RoundedCornerShape(12.dp)): Modifier = composed {
    var focused by remember { mutableStateOf(false) }
    this
        .onFocusChanged { focused = it.isFocused || it.hasFocus }
        .graphicsLayer {
            val scale = if (focused) 1.06f else 1f
            scaleX = scale
            scaleY = scale
        }
        .drawWithContent {
            drawContent()
            // 聚焦暖染对齐参考风格的 CardFocused 暖底语言
            if (focused) drawRect(AppColors.Honey.copy(alpha = 0.12f))
        }
        // 未聚焦不挂 border：border(0.dp) 在 Android 会画 1px 发丝线而非不画
        .then(if (focused) Modifier.border(2.dp, AppColors.CoralDeep, shape) else Modifier)
}
