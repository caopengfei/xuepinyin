package com.rakor.xuepinyin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.rakor.xuepinyin.data.AssetsCourseRepository
import com.rakor.xuepinyin.ui.PinyinApp
import com.rakor.xuepinyin.ui.theme.CandyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CandyTheme {
                PinyinApp(repository = AssetsCourseRepository.fromAssets(applicationContext))
            }
        }
    }
}
