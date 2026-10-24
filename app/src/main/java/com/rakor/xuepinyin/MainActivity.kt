package com.rakor.xuepinyin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.rakor.xuepinyin.audio.VoicePack
import com.rakor.xuepinyin.audio.VoicePackPlayer
import com.rakor.xuepinyin.data.AssetsCourseRepository
import com.rakor.xuepinyin.data.DataStoreProgressStore
import com.rakor.xuepinyin.data.ProgressRepository
import com.rakor.xuepinyin.ui.PinyinApp
import com.rakor.xuepinyin.ui.theme.CandyTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val courseRepository = AssetsCourseRepository.fromAssets(applicationContext)
        val progressRepository = ProgressRepository(DataStoreProgressStore(applicationContext))
        val audioPlayer = VoicePackPlayer(applicationContext, VoicePack.fromAssets(applicationContext))
        lifecycleScope.launch { progressRepository.initialize() }
        setContent {
            CandyTheme {
                PinyinApp(
                    repository = courseRepository,
                    progressRepository = progressRepository,
                    audioPlayer = audioPlayer,
                )
            }
        }
    }
}
