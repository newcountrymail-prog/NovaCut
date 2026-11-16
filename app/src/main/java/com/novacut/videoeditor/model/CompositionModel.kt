package com.novacut.videoeditor.model

import android.net.Uri

/**
 * High-Precision Time Units:
 * All timeline and media timestamps are represented in Microseconds (1 second = 1,000,000 us).
 * This ensures sub-frame precision at 24fps, 30fps, 60fps, and 120fps.
 */
object TimeUtils {
    const val US_PER_SECOND = 1_000_000L
    const val US_PER_MS = 1_000L

    fun secToUs(seconds: Double): Long = (seconds * US_PER_SECOND).toLong()
    fun msToUs(ms: Long): Long = ms * US_PER_MS
    fun usToMs(us: Long): Long = us / US_PER_MS
    fun usToSec(us: Long): Double = us.toDouble() / US_PER_SECOND

    fun formatTimecode(us: Long): String {
        val totalSec = us / US_PER_SECOND
        val mins = totalSec / 60
        val secs = totalSec % 60
        val frames = ((us % US_PER_SECOND) / (US_PER_SECOND / 30)).coerceIn(0, 29)
        return String.format("%02d:%02d:%02d", mins, secs, frames)
    }
}

data class ClipTransform(
    val scaleX: Float = 1.0f,
    val scaleY: Float = 1.0f,
    val translationX: Float = 0.0f,
    val translationY: Float = 0.0f,
    val rotationDegrees: Float = 0.0f,
    val opacity: Float = 1.0f
)

data class TimelineClip(
    val id: String,
    val sourceUri: Uri?,
    val name: String,
    // Timeline position (when it appears on master composition)
    val timelineStartUs: Long,
    val timelineDurationUs: Long,
    // Source media offsets (in/out points inside the source video)
    val sourceStartOffsetUs: Long = 0L,
    val sourceEndOffsetUs: Long,
    val sourceTotalDurationUs: Long,
    // Speed factor: timelineDurationUs = (sourceEndOffsetUs - sourceStartOffsetUs) / speed
    val speed: Float = 1.0f,
    val volume: Float = 1.0f,
    val isMuted: Boolean = false,
    val isReversed: Boolean = false,
    val transform: ClipTransform = ClipTransform(),
    val filter: FilterPreset = FilterPreset.ORIGINAL,
    val colorAdjustment: ColorAdjustment = ColorAdjustment(),
    val keyframes: List<Keyframe> = emptyList(),
    val isChromaKeyEnabled: Boolean = false,
    val chromaKeyColorHex: String = "#00FF00",
    val isBackgroundCutoutEnabled: Boolean = false,
    val isSmoothSlowMoEnabled: Boolean = false
) {
    val timelineEndUs: Long get() = timelineStartUs + timelineDurationUs

    /**
     * Map a presentation timestamp on the timeline to the source video file timestamp.
     */
    fun mapTimelineToSourceUs(timelinePtsUs: Long): Long? {
        if (timelinePtsUs < timelineStartUs || timelinePtsUs > timelineEndUs) return null
        val offsetFromClipStart = timelinePtsUs - timelineStartUs
        val sourceOffset = (offsetFromClipStart * speed).toLong()
        return if (isReversed) {
            (sourceEndOffsetUs - sourceOffset).coerceIn(sourceStartOffsetUs, sourceEndOffsetUs)
        } else {
            (sourceStartOffsetUs + sourceOffset).coerceIn(sourceStartOffsetUs, sourceEndOffsetUs)
        }
    }
}

data class CompositionTrack(
    val id: String,
    val type: TrackType,
    val name: String,
    val zIndex: Int,
    val isVisible: Boolean = true,
    val isMuted: Boolean = false,
    val isLocked: Boolean = false,
    val clips: List<TimelineClip> = emptyList()
)

data class Composition(
    val id: String,
    val name: String,
    val width: Int = 1080,
    val height: Int = 1920,
    val frameRate: Int = 60,
    val aspectRatio: AspectRatio = AspectRatio.PORTRAIT_9_16,
    val tracks: List<CompositionTrack> = emptyList()
) {
    val durationUs: Long
        get() = tracks.flatMap { it.clips }.maxOfOrNull { it.timelineEndUs } ?: 0L

    fun getClipsAt(timelineUs: Long): List<Pair<CompositionTrack, TimelineClip>> {
        return tracks
            .filter { it.isVisible }
            .sortedBy { it.zIndex }
            .flatMap { track ->
                track.clips
                    .filter { timelineUs >= it.timelineStartUs && timelineUs < it.timelineEndUs }
                    .map { clip -> Pair(track, clip) }
            }
    }
}

data class SnapTarget(
    val timeUs: Long,
    val description: String,
    val trackId: String? = null
)

data class SnapResult(
    val snappedTimeUs: Long,
    val deltaUs: Long,
    val target: SnapTarget
)
