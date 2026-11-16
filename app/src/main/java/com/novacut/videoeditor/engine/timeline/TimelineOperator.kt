package com.novacut.videoeditor.engine.timeline

import com.novacut.videoeditor.model.CompositionTrack
import com.novacut.videoeditor.model.TimelineClip
import java.util.UUID

class TimelineOperator {

    /**
     * Frame-accurate non-destructive clip split at given master composition time.
     */
    fun splitClip(
        track: CompositionTrack,
        clipId: String,
        splitTimelineUs: Long
    ): Pair<CompositionTrack, Pair<TimelineClip, TimelineClip>>? {
        val clipIndex = track.clips.indexOfFirst { it.id == clipId }
        if (clipIndex == -1) return null
        val clip = track.clips[clipIndex]

        if (splitTimelineUs <= clip.timelineStartUs || splitTimelineUs >= clip.timelineEndUs) {
            return null
        }

        // Map timeline split timestamp to source offset
        val sourceSplitUs = clip.mapTimelineToSourceUs(splitTimelineUs) ?: return null
        val firstPartDurationUs = splitTimelineUs - clip.timelineStartUs
        val secondPartDurationUs = clip.timelineEndUs - splitTimelineUs

        val partA = clip.copy(
            id = UUID.randomUUID().toString(),
            timelineDurationUs = firstPartDurationUs,
            sourceEndOffsetUs = sourceSplitUs
        )

        val partB = clip.copy(
            id = UUID.randomUUID().toString(),
            timelineStartUs = splitTimelineUs,
            timelineDurationUs = secondPartDurationUs,
            sourceStartOffsetUs = sourceSplitUs
        )

        val updatedClips = track.clips.toMutableList().apply {
            removeAt(clipIndex)
            add(clipIndex, partA)
            add(clipIndex + 1, partB)
        }

        return Pair(track.copy(clips = updatedClips), Pair(partA, partB))
    }

    /**
     * Trim clip In-Point (Left Handle).
     */
    fun trimInPoint(
        track: CompositionTrack,
        clipId: String,
        newStartTimelineUs: Long
    ): CompositionTrack? {
        val clipIndex = track.clips.indexOfFirst { it.id == clipId }
        if (clipIndex == -1) return null
        val clip = track.clips[clipIndex]

        if (newStartTimelineUs >= clip.timelineEndUs - 100_000L) return null // Minimum 100ms clip

        val deltaTimelineUs = newStartTimelineUs - clip.timelineStartUs
        val deltaSourceUs = (deltaTimelineUs * clip.speed).toLong()
        val newSourceStartUs = clip.sourceStartOffsetUs + deltaSourceUs

        if (newSourceStartUs < 0 || newSourceStartUs >= clip.sourceEndOffsetUs) return null

        val updatedClip = clip.copy(
            timelineStartUs = newStartTimelineUs,
            timelineDurationUs = clip.timelineEndUs - newStartTimelineUs,
            sourceStartOffsetUs = newSourceStartUs
        )

        val updatedClips = track.clips.toMutableList().apply {
            set(clipIndex, updatedClip)
        }
        return track.copy(clips = updatedClips)
    }

    /**
     * Trim clip Out-Point (Right Handle).
     */
    fun trimOutPoint(
        track: CompositionTrack,
        clipId: String,
        newEndTimelineUs: Long
    ): CompositionTrack? {
        val clipIndex = track.clips.indexOfFirst { it.id == clipId }
        if (clipIndex == -1) return null
        val clip = track.clips[clipIndex]

        if (newEndTimelineUs <= clip.timelineStartUs + 100_000L) return null // Minimum 100ms clip

        val newTimelineDurationUs = newEndTimelineUs - clip.timelineStartUs
        val newSourceDurationUs = (newTimelineDurationUs * clip.speed).toLong()
        val newSourceEndUs = clip.sourceStartOffsetUs + newSourceDurationUs

        if (newSourceEndUs > clip.sourceTotalDurationUs) return null

        val updatedClip = clip.copy(
            timelineDurationUs = newTimelineDurationUs,
            sourceEndOffsetUs = newSourceEndUs
        )

        val updatedClips = track.clips.toMutableList().apply {
            set(clipIndex, updatedClip)
        }
        return track.copy(clips = updatedClips)
    }

    /**
     * Ripple Deletion (CapCut Magnetic Track):
     * Deletes clip and automatically pulls all subsequent clips left to close the gap.
     */
    fun rippleDelete(
        track: CompositionTrack,
        clipId: String
    ): CompositionTrack {
        val clipIndex = track.clips.indexOfFirst { it.id == clipId }
        if (clipIndex == -1) return track
        val removedClip = track.clips[clipIndex]
        val gapDurationUs = removedClip.timelineDurationUs

        val updatedClips = mutableListOf<TimelineClip>()
        for (i in track.clips.indices) {
            if (i < clipIndex) {
                updatedClips.add(track.clips[i])
            } else if (i > clipIndex) {
                val clip = track.clips[i]
                updatedClips.add(clip.copy(timelineStartUs = clip.timelineStartUs - gapDurationUs))
            }
        }
        return track.copy(clips = updatedClips)
    }

    /**
     * Speed Adjuster: Recalculates timeline duration according to new playback speed.
     */
    fun setSpeed(
        track: CompositionTrack,
        clipId: String,
        newSpeed: Float
    ): CompositionTrack? {
        val clipIndex = track.clips.indexOfFirst { it.id == clipId }
        if (clipIndex == -1 || newSpeed <= 0f) return null
        val clip = track.clips[clipIndex]

        val sourceDurationUs = clip.sourceEndOffsetUs - clip.sourceStartOffsetUs
        val newTimelineDurationUs = (sourceDurationUs / newSpeed).toLong()

        val updatedClip = clip.copy(
            speed = newSpeed,
            timelineDurationUs = newTimelineDurationUs
        )

        val updatedClips = track.clips.toMutableList().apply {
            set(clipIndex, updatedClip)
        }
        return track.copy(clips = updatedClips)
    }
}
