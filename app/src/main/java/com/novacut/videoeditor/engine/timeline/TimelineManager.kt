package com.novacut.videoeditor.engine.timeline

import android.net.Uri
import com.novacut.videoeditor.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import kotlin.math.abs

/**
 * Production TimelineManager orchestrating multi-track layering, gesture translations,
 * clip trimming, splitting, drag-and-drop reordering, and magnetic snapping.
 */
class TimelineManager {

    private val magneticSnapEngine = MagneticSnapEngine()
    private val timelineOperator = TimelineOperator()

    // Primary Composition Graph
    private val _composition = MutableStateFlow(
        Composition(
            id = UUID.randomUUID().toString(),
            name = "NovaCut Project",
            tracks = listOf(
                CompositionTrack(id = "track_v1", type = TrackType.MAIN_VIDEO, name = "Video 1", zIndex = 0),
                CompositionTrack(id = "track_pip", type = TrackType.OVERLAY_PIP, name = "PiP 1", zIndex = 1),
                CompositionTrack(id = "track_a1", type = TrackType.AUDIO, name = "Audio 1", zIndex = 2),
                CompositionTrack(id = "track_t1", type = TrackType.TEXT_CAPTION, name = "Text 1", zIndex = 3)
            )
        )
    )
    val composition: StateFlow<Composition> = _composition.asStateFlow()

    // Timeline Zoom & Pan Scale (Microseconds to Pixels)
    private val _zoomFactor = MutableStateFlow(1.0f) // 0.25x to 5.0x
    val zoomFactor: StateFlow<Float> = _zoomFactor.asStateFlow()

    private val _currentPlayheadUs = MutableStateFlow(0L)
    val currentPlayheadUs: StateFlow<Long> = _currentPlayheadUs.asStateFlow()

    private val _selectedClipId = MutableStateFlow<String?>(null)
    val selectedClipId: StateFlow<String?> = _selectedClipId.asStateFlow()

    private val _snapGuideUs = MutableStateFlow<Long?>(null)
    val snapGuideUs: StateFlow<Long?> = _snapGuideUs.asStateFlow()

    // Pixels per second at 1.0x zoom
    val basePxPerSecond: Float = 60.0f

    /**
     * Convert an X-coordinate on screen into microsecond timestamp based on scroll offset and zoom.
     */
    fun pxToMicroseconds(px: Float, scrollOffsetPx: Float): Long {
        val totalPx = px + scrollOffsetPx
        val currentPxPerSec = basePxPerSecond * _zoomFactor.value
        val seconds = totalPx / currentPxPerSec
        return (seconds * TimeUtils.US_PER_SECOND).toLong().coerceAtLeast(0L)
    }

    /**
     * Convert a microsecond timestamp to timeline pixel offset.
     */
    fun microsecondsToPx(us: Long): Float {
        val seconds = us.toDouble() / TimeUtils.US_PER_SECOND
        val currentPxPerSec = basePxPerSecond * _zoomFactor.value
        return (seconds * currentPxPerSec).toFloat()
    }

    /**
     * Smooth Pinch-to-Zoom Gesture handler.
     */
    fun applyZoomGesture(zoomRatioDelta: Float) {
        val updated = (_zoomFactor.value * zoomRatioDelta).coerceIn(0.25f, 5.0f)
        _zoomFactor.value = updated
    }

    /**
     * Seek with Magnetic Snapping.
     */
    fun seekTo(requestedUs: Long, enableSnap: Boolean = true) {
        if (!enableSnap) {
            _currentPlayheadUs.value = requestedUs.coerceIn(0L, _composition.value.durationUs)
            _snapGuideUs.value = null
            return
        }

        val snapResult = magneticSnapEngine.findSnapPoint(
            proposedTimeUs = requestedUs,
            composition = _composition.value,
            playheadUs = _currentPlayheadUs.value
        )

        if (snapResult != null) {
            _currentPlayheadUs.value = snapResult.snappedTimeUs
            _snapGuideUs.value = snapResult.snappedTimeUs
        } else {
            _currentPlayheadUs.value = requestedUs.coerceIn(0L, _composition.value.durationUs)
            _snapGuideUs.value = null
        }
    }

    fun selectClip(clipId: String?) {
        _selectedClipId.value = clipId
    }

    /**
     * Split selected clip at current playhead position.
     */
    fun splitClipAtPlayhead(): Boolean {
        val clipId = _selectedClipId.value ?: return false
        val currentComp = _composition.value
        val playheadUs = _currentPlayheadUs.value

        for (track in currentComp.tracks) {
            val splitResult = timelineOperator.splitClip(track, clipId, playheadUs)
            if (splitResult != null) {
                val updatedTrack = splitResult.first
                val newClips = splitResult.second
                val updatedTracks = currentComp.tracks.map {
                    if (it.id == track.id) updatedTrack else it
                }
                _composition.value = currentComp.copy(tracks = updatedTracks)
                _selectedClipId.value = newClips.second.id
                return true
            }
        }
        return false
    }

    /**
     * Trim Left Handle (In-Point) with magnetic snapping.
     */
    fun trimClipInPoint(clipId: String, requestedStartUs: Long) {
        val snap = magneticSnapEngine.findSnapPoint(
            proposedTimeUs = requestedStartUs,
            composition = _composition.value,
            excludeClipId = clipId
        )
        val targetStartUs = snap?.snappedTimeUs ?: requestedStartUs

        val currentComp = _composition.value
        val updatedTracks = currentComp.tracks.map { track ->
            timelineOperator.trimInPoint(track, clipId, targetStartUs) ?: track
        }
        _composition.value = currentComp.copy(tracks = updatedTracks)
        _snapGuideUs.value = snap?.snappedTimeUs
    }

    /**
     * Trim Right Handle (Out-Point) with magnetic snapping.
     */
    fun trimClipOutPoint(clipId: String, requestedEndUs: Long) {
        val snap = magneticSnapEngine.findSnapPoint(
            proposedTimeUs = requestedEndUs,
            composition = _composition.value,
            excludeClipId = clipId
        )
        val targetEndUs = snap?.snappedTimeUs ?: requestedEndUs

        val currentComp = _composition.value
        val updatedTracks = currentComp.tracks.map { track ->
            timelineOperator.trimOutPoint(track, clipId, targetEndUs) ?: track
        }
        _composition.value = currentComp.copy(tracks = updatedTracks)
        _snapGuideUs.value = snap?.snappedTimeUs
    }

    /**
     * Drag-and-Drop Clip Translation along timeline.
     */
    fun moveClip(clipId: String, newStartUs: Long) {
        val snap = magneticSnapEngine.findSnapPoint(
            proposedTimeUs = newStartUs,
            composition = _composition.value,
            excludeClipId = clipId
        )
        val targetStartUs = (snap?.snappedTimeUs ?: newStartUs).coerceAtLeast(0L)

        val currentComp = _composition.value
        val updatedTracks = currentComp.tracks.map { track ->
            val clipIndex = track.clips.indexOfFirst { it.id == clipId }
            if (clipIndex != -1) {
                val clip = track.clips[clipIndex]
                val updatedClip = clip.copy(timelineStartUs = targetStartUs)
                val newClips = track.clips.toMutableList().apply { set(clipIndex, updatedClip) }
                track.copy(clips = newClips)
            } else {
                track
            }
        }
        _composition.value = currentComp.copy(tracks = updatedTracks)
        _snapGuideUs.value = snap?.snappedTimeUs
    }

    /**
     * Append a media clip to a designated track.
     */
    fun addClipToTrack(trackId: String, uri: Uri?, name: String, durationUs: Long): TimelineClip {
        val currentComp = _composition.value
        var createdClip: TimelineClip? = null

        val updatedTracks = currentComp.tracks.map { track ->
            if (track.id == trackId) {
                val startUs = track.clips.maxOfOrNull { it.timelineEndUs } ?: 0L
                val clip = TimelineClip(
                    id = UUID.randomUUID().toString(),
                    sourceUri = uri,
                    name = name,
                    timelineStartUs = startUs,
                    timelineDurationUs = durationUs,
                    sourceEndOffsetUs = durationUs,
                    sourceTotalDurationUs = durationUs
                )
                createdClip = clip
                track.copy(clips = track.clips + clip)
            } else {
                track
            }
        }

        _composition.value = currentComp.copy(tracks = updatedTracks)
        _selectedClipId.value = createdClip?.id
        return createdClip!!
    }

    fun removeClip(clipId: String, ripple: Boolean = true) {
        val currentComp = _composition.value
        val updatedTracks = currentComp.tracks.map { track ->
            if (ripple) {
                timelineOperator.rippleDelete(track, clipId)
            } else {
                track.copy(clips = track.clips.filterNot { it.id == clipId })
            }
        }
        _composition.value = currentComp.copy(tracks = updatedTracks)
        if (_selectedClipId.value == clipId) {
            _selectedClipId.value = null
        }
    }
}
