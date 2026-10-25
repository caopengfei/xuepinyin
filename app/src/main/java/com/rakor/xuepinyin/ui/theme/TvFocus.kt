package com.rakor.xuepinyin.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import com.rakor.xuepinyin.ui.InputMode
import com.rakor.xuepinyin.ui.rememberInputMode
import kotlinx.coroutines.delay

/**
 * TV 默认焦点助手：屏幕/步骤进入时把焦点引导到主操作按钮（触控模式不生效）。
 * 用法：val f = rememberTvFocus(key)；目标按钮 modifier 加 .tvFocusTarget(f)。
 * key 变化（如切换字母/翻页）时重新引导；导航过渡期间请求可能被丢，重试至多约 1.5s。
 * 全屏覆盖物关闭后 LocalTvFocusNudge 递增，被覆盖屏幕的默认焦点随之重新引导。
 */
class TvFocus {
    val requester = FocusRequester()
    var targetFocused by mutableStateOf(false)
}

/** 全屏覆盖物（如护眼弹层）关闭时递增：覆盖物移除会清空焦点，各屏幕默认焦点据此重新就位 */
val LocalTvFocusNudge = compositionLocalOf { 0 }

@Composable
fun rememberTvFocus(key: Any? = Unit): TvFocus {
    val nudge = LocalTvFocusNudge.current
    val tvFocus = remember { TvFocus() }
    val inputMode = rememberInputMode()
    LaunchedEffect(tvFocus, key, nudge) {
        if (inputMode != InputMode.FOCUS) return@LaunchedEffect
        tvFocus.requester.requestFocus()
        repeat(6) {
            delay(250)
            if (tvFocus.targetFocused) return@LaunchedEffect
            tvFocus.requester.requestFocus()
        }
    }
    return tvFocus
}

/** 挂在默认焦点目标按钮上（配合 rememberTvFocus 使用） */
fun Modifier.tvFocusTarget(tvFocus: TvFocus): Modifier = composed {
    onFocusChanged { tvFocus.targetFocused = it.isFocused }
        .focusRequester(tvFocus.requester)
}
