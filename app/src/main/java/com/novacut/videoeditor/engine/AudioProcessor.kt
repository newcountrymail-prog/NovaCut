package com.novacut.videoeditor.engine

import com.novacut.videoeditor.model.AudioClip
import com.novacut.videoeditor.model.VoiceEffect
import java.util.UUID

class AudioProcessor {

    fun generateBeatMarkers(durationMs: Long, bpm: Int = 128): List<Long> {
        val beatIntervalMs = (60_000L / bpm)
        val beats = mutableListOf<Long>()
        var current = beatIntervalMs
        while (current < durationMs) {
            beats.add(current)
            current += beatIntervalMs
        }
        return beats
    }

    fun applyVoiceEffect(clip: AudioClip, effect: VoiceEffect): AudioClip {
        return clip.copy(voiceEffect = effect)
    }

    fun toggleNoiseReduction(clip: AudioClip): AudioClip {
        return clip.copy(isNoiseReduced = !clip.isNoiseReduced)
    }

    fun createSampleAudio(title: String, durationMs: Long): AudioClip {
        return AudioClip(
            id = UUID.randomUUID().toString(),
            title = title,
            uri = null,
            startMs = 0L,
            durationMs = durationMs,
            volume = 0.8f
        )
    }
}
