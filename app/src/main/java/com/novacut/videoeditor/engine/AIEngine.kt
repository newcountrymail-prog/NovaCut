package com.novacut.videoeditor.engine

import com.novacut.videoeditor.model.Clip
import com.novacut.videoeditor.model.TextClip
import kotlinx.coroutines.delay
import java.util.UUID

class AIEngine {

    suspend fun generateAutoCaptions(videoDurationMs: Long): List<TextClip> {
        delay(1200) // Simulate fast AI on-device Whisper model transcription

        val sampleTranscripts = listOf(
            "Welcome to NovaCut Studio 🎬",
            "Next-Gen Video & Photo Editing 🔥",
            "Auto Captions, Smooth Slow-Mo & AI Cutout ✨",
            "Engineered with 100% Pro Power 🚀",
            "Export in Crisp 4K 60FPS Quality 💎"
        )

        val captions = mutableListOf<TextClip>()
        val segmentDuration = (videoDurationMs / sampleTranscripts.size).coerceAtLeast(1500L)

        sampleTranscripts.forEachIndexed { index, text ->
            val startMs = index * segmentDuration
            if (startMs < videoDurationMs) {
                captions.add(
                    TextClip(
                        id = UUID.randomUUID().toString(),
                        text = text,
                        startMs = startMs,
                        durationMs = (segmentDuration - 200).coerceAtLeast(1000L),
                        isAutoCaption = true,
                        textColorHex = "#00E5FF",
                        animationType = "PopUp"
                    )
                )
            }
        }
        return captions
    }

    fun toggleChromaKey(clip: Clip, colorHex: String = "#00FF00"): Clip {
        return clip.copy(
            isChromaKeyEnabled = !clip.isChromaKeyEnabled,
            chromaKeyColorHex = colorHex
        )
    }

    fun toggleBackgroundCutout(clip: Clip): Clip {
        return clip.copy(
            isBackgroundCutoutEnabled = !clip.isBackgroundCutoutEnabled
        )
    }

    fun toggleSmoothSlowMo(clip: Clip): Clip {
        return clip.copy(
            isSmoothSlowMoEnabled = !clip.isSmoothSlowMoEnabled
        )
    }
}
