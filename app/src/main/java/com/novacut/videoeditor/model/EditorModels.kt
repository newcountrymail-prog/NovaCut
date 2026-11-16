package com.novacut.videoeditor.model

import android.net.Uri

enum class AspectRatio(val label: String, val ratioWidth: Float, val ratioHeight: Float) {
    PORTRAIT_9_16("9:16 (Reels)", 9f, 16f),
    LANDSCAPE_16_9("16:9 (YouTube)", 16f, 9f),
    SQUARE_1_1("1:1 (Post)", 1f, 1f),
    PORTRAIT_4_5("4:5 (Feed)", 4f, 5f)
}

enum class TrackType {
    MAIN_VIDEO,
    OVERLAY_PIP,
    AUDIO,
    TEXT_CAPTION,
    EFFECTS
}

enum class FilterPreset(val displayName: String) {
    ORIGINAL("Original"),
    CINEMATIC("Cinematic"),
    CYBERPUNK("Cyberpunk"),
    VINTAGE("Vintage 90s"),
    WARM_GOLD("Warm Gold"),
    COLD_BLUE("Cold Blue"),
    MONO_NOIR("Film Noir"),
    EMERALD("Emerald Pop")
}

enum class VoiceEffect(val title: String) {
    NONE("Original"),
    DEEP("Deep Bass"),
    ROBOT("Cyborg"),
    CHIPMUNK("Chipmunk"),
    ECHO("Auditorium Echo"),
    MIC("Studio Mic")
}

data class Keyframe(
    val timeMs: Long,
    val scale: Float = 1.0f,
    val rotation: Float = 0.0f,
    val translationX: Float = 0.0f,
    val translationY: Float = 0.0f,
    val opacity: Float = 1.0f
)

data class ColorAdjustment(
    val brightness: Float = 0.0f,   // -1.0 to 1.0
    val contrast: Float = 1.0f,     // 0.0 to 2.0
    val saturation: Float = 1.0f,   // 0.0 to 2.0
    val temperature: Float = 0.0f,  // -1.0 to 1.0
    val vignette: Float = 0.0f,     // 0.0 to 1.0
    val sharpen: Float = 0.0f       // 0.0 to 1.0
)

data class Clip(
    val id: String,
    val uri: Uri?,
    val name: String,
    val startTrimMs: Long = 0L,
    val endTrimMs: Long,
    val durationMs: Long,
    val speed: Float = 1.0f,
    val volume: Float = 1.0f,
    val filter: FilterPreset = FilterPreset.ORIGINAL,
    val adjustment: ColorAdjustment = ColorAdjustment(),
    val keyframes: List<Keyframe> = emptyList(),
    val isChromaKeyEnabled: Boolean = false,
    val chromaKeyColorHex: String = "#00FF00",
    val isBackgroundCutoutEnabled: Boolean = false,
    val isSmoothSlowMoEnabled: Boolean = false,
    val isMuted: Boolean = false,
    val isReversed: Boolean = false
)

data class TextClip(
    val id: String,
    val text: String,
    val startMs: Long,
    val durationMs: Long = 3000L,
    val textColorHex: String = "#FFFFFF",
    val backgroundColorHex: String = "#00000000",
    val isAutoCaption: Boolean = false,
    val animationType: String = "Typewriter",
    val scale: Float = 1.0f,
    val posX: Float = 0f,
    val posY: Float = 0f
)

data class AudioClip(
    val id: String,
    val title: String,
    val uri: Uri?,
    val startMs: Long,
    val durationMs: Long,
    val volume: Float = 1.0f,
    val voiceEffect: VoiceEffect = VoiceEffect.NONE,
    val isNoiseReduced: Boolean = false
)

data class Track(
    val id: String,
    val type: TrackType,
    val name: String,
    val isMuted: Boolean = false,
    val isLocked: Boolean = false
)

data class ExportSettings(
    val resolutionName: String = "1080p FHD",
    val width: Int = 1080,
    val height: Int = 1920,
    val frameRate: Int = 60,
    val bitrateMbps: Int = 16,
    val format: String = "MP4 (H.264)"
)
