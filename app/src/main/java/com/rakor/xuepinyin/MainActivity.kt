package com.rakor.xuepinyin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.rakor.xuepinyin.ui.theme.CandyColors
import com.rakor.xuepinyin.ui.theme.CandyTheme
import com.rakor.xuepinyin.ui.theme.CandyType

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CandyTheme {
                Box(
                    modifier = Modifier.fillMaxSize().background(CandyColors.Background),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("拼音乐园", fontSize = CandyType.heroTitle, color = CandyColors.Ink)
                }
            }
        }
    }
}
