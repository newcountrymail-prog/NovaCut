package com.novacut.videoeditor.engine.timeline

import com.novacut.videoeditor.model.Composition
import com.novacut.videoeditor.model.SnapResult
import com.novacut.videoeditor.model.SnapTarget
import kotlin.math.abs

class MagneticSnapEngine {

    /**
     * Default snapping threshold: 120 milliseconds in microseconds.
     */
    var defaultThresholdUs: Long = 120_000L

    /**
     * Finds the closest magnetic snap target within threshold.
     *
     * @param proposedTimeUs The requested drag position.
     * @param composition The active multi-track composition graph.
     * @param playheadUs The current playhead position.
     * @param excludeClipId The clip currently being dragged (so it doesn't snap to its own prior state).
     * @param customThresholdUs Optional threshold override based on timeline zoom level.
     */
    fun findSnapPoint(
        proposedTimeUs: Long,
        composition: Composition,
        playheadUs: Long? = null,
        beatMarkersUs: List<Long> = emptyList(),
        excludeClipId: String? = null,
        customThresholdUs: Long? = null
    ): SnapResult? {
        val threshold = customThresholdUs ?: defaultThresholdUs
        val targets = mutableListOf<SnapTarget>()

        // 1. Zero point (timeline origin)
        targets.add(SnapTarget(0L, "Timeline Origin"))

        // 2. Playhead target
        playheadUs?.let {
            targets.add(SnapTarget(it, "Playhead"))
        }

        // 3. Audio Beat Markers
        beatMarkersUs.forEach { beatUs ->
            targets.add(SnapTarget(beatUs, "Audio Beat"))
        }

        // 4. Clip in-points and out-points across all visible tracks
        composition.tracks.filter { it.isVisible }.forEach { track ->
            track.clips.forEach { clip ->
                if (clip.id != excludeClipId) {
                    targets.add(SnapTarget(clip.timelineStartUs, "In-Point: ${clip.name}", track.id))
                    targets.add(SnapTarget(clip.timelineEndUs, "Out-Point: ${clip.name}", track.id))
                }
            }
        }

        // Find nearest target within threshold
        var closestTarget: SnapTarget? = null
        var minDeltaUs = Long.MAX_VALUE

        for (target in targets) {
            val delta = abs(proposedTimeUs - target.timeUs)
            if (delta <= threshold && delta < minDeltaUs) {
                minDeltaUs = delta
                closestTarget = target
            }
        }

        return closestTarget?.let {
            SnapResult(
                snappedTimeUs = it.timeUs,
                deltaUs = proposedTimeUs - it.timeUs,
                target = it
            )
        }
    }
}
