package com.rakor.xuepinyin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.rakor.xuepinyin.audio.VoicePack
import com.rakor.xuepinyin.audio.VoicePackPlayer
import com.rakor.xuepinyin.data.AssetsCourseRepository
import com.rakor.xuepinyin.data.DataStoreProgressStore
import com.rakor.xuepinyin.data.DataStoreSettingsStore
import com.rakor.xuepinyin.data.ProgressRepository
import com.rakor.xuepinyin.data.SettingsRepository
import com.rakor.xuepinyin.ui.PinyinApp
import com.rakor.xuepinyin.ui.theme.AppTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val courseRepository = AssetsCourseRepository.fromAssets(applicationContext)
        val progressRepository = ProgressRepository(DataStoreProgressStore(applicationContext))
        val settingsRepository = SettingsRepository(DataStoreSettingsStore(applicationContext))
        val audioPlayer = VoicePackPlayer(applicationContext, VoicePack.fromAssets(applicationContext))
        lifecycleScope.launch {
            progressRepository.initialize()
            settingsRepository.initialize()
        }
        setContent {
            AppTheme {
                PinyinApp(
                    repository = courseRepository,
                    progressRepository = progressRepository,
                    settingsRepository = settingsRepository,
                    audioPlayer = audioPlayer,
                )
            }
        }
    }
}
