package com.rakor.xuepinyin.ui

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** 输入模式：TV 遥控器焦点 / 平板触控 */
enum class InputMode { FOCUS, TOUCH }

fun resolveInputMode(context: Context): InputMode {
    val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
    val isTv = uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    val hasTouch = context.packageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)
    return if (isTv || !hasTouch) InputMode.FOCUS else InputMode.TOUCH
}

@Composable
fun rememberInputMode(): InputMode {
    val context = LocalContext.current
    return remember { resolveInputMode(context) }
}
