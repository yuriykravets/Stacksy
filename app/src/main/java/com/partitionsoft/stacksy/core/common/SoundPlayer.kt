package com.partitionsoft.stacksy.core.common

import android.media.AudioManager
import android.media.ToneGenerator
import com.partitionsoft.stacksy.game.domain.GameEffect

class SoundPlayer : AutoCloseable {
    private val toneGenerator = runCatching {
        ToneGenerator(AudioManager.STREAM_MUSIC, 55)
    }.getOrNull()

    fun play(effect: GameEffect) {
        val tone = when (effect) {
            GameEffect.Drop -> ToneGenerator.TONE_PROP_BEEP
            GameEffect.Perfect -> ToneGenerator.TONE_PROP_ACK
            GameEffect.GameOver -> ToneGenerator.TONE_PROP_NACK
        }
        val duration = if (effect == GameEffect.GameOver) 220 else 70
        toneGenerator?.startTone(tone, duration)
    }

    override fun close() {
        toneGenerator?.release()
    }
}
