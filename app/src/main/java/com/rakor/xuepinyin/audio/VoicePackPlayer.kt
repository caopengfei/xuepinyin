package com.rakor.xuepinyin.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer

/** 单例式播放器：打断即播（新请求立刻打断旧音频），缺 key 静默降级 */
class VoicePackPlayer(
    private val context: Context,
    private val pack: VoicePack,
) {
    private var player: MediaPlayer? = null

    fun play(key: String) {
        val file = pack.fileOf(key) ?: return
        try {
            val p = player ?: MediaPlayer().also { mp ->
                mp.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build(),
                )
                player = mp
            }
            p.reset()
            context.assets.openFd(file).use { fd ->
                p.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
            }
            p.prepare()
            p.start()
        } catch (_: Exception) {
            // 静默降级：缺资源/解码失败不阻塞学习流程
        }
    }

    fun release() {
        player?.release()
        player = null
    }
}
