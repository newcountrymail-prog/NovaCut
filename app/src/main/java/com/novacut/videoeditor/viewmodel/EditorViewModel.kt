package com.novacut.videoeditor.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.novacut.videoeditor.engine.AIEngine
import com.novacut.videoeditor.engine.AudioProcessor
import com.novacut.videoeditor.engine.VideoEngine
import com.novacut.videoeditor.engine.timeline.MagneticSnapEngine
import com.novacut.videoeditor.engine.timeline.TimelineOperator
import com.novacut.videoeditor.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val videoEngine = VideoEngine(application)
    private val audioProcessor = AudioProcessor()
    private val aiEngine = AIEngine()
    private val magneticSnapEngine = MagneticSnapEngine()
    private val timelineOperator = TimelineOperator()

    // Master Composition Graph
    private val _composition = MutableStateFlow(
        Composition(
            id = UUID.randomUUID().toString(),
            name = "Project 1",
            tracks = listOf(
                CompositionTrack(
                    id = "track-v1",
                    type = TrackType.MAIN_VIDEO,
                    name = "V1 (Main)",
                    zIndex = 0,
                    clips = listOf(
                        TimelineClip(
                            id = "clip-1",
                            sourceUri = null,
                            name = "Intro Scene.mp4",
                            timelineStartUs = 0L,
                            timelineDurationUs = 5_000_000L,
                            sourceEndOffsetUs = 5_000_000L,
                            sourceTotalDurationUs = 10_000_000L,
                            filter = FilterPreset.CINEMATIC
                        ),
                        TimelineClip(
                            id = "clip-2",
                            sourceUri = null,
                            name = "Action Shot.mp4",
                            timelineStartUs = 5_000_000L,
                            timelineDurationUs = 5_000_000L,
                            sourceEndOffsetUs = 5_000_000L,
                            sourceTotalDurationUs = 10_000_000L,
                            filter = FilterPreset.WARM_GOLD
                        )
                    )
                ),
                CompositionTrack(
                    id = "track-v2",
                    type = TrackType.OVERLAY_PIP,
                    name = "V2 (PiP)",
                    zIndex = 1,
                    clips = emptyList()
                ),
                CompositionTrack(
                    id = "track-a1",
                    type = TrackType.AUDIO,
                    name = "A1 (Music)",
                    zIndex = 2,
                    clips = listOf(
                        TimelineClip(
                            id = "audio-1",
                            sourceUri = null,
                            name = "Cyberpunk Beat.mp3",
                            timelineStartUs = 0L,
                            timelineDurationUs = 10_000_000L,
                            sourceEndOffsetUs = 10_000_000L,
                            sourceTotalDurationUs = 10_000_000L
                        )
                    )
                ),
                CompositionTrack(
                    id = "track-t1",
                    type = TrackType.TEXT_CAPTION,
                    name = "T1 (Text)",
                    zIndex = 3,
                    clips = listOf(
                        TimelineClip(
                            id = "text-1",
                            sourceUri = null,
                            name = "NovaCut Studio",
                            timelineStartUs = 500_000L,
                            timelineDurationUs = 3_500_000L,
                            sourceEndOffsetUs = 3_500_000L,
                            sourceTotalDurationUs = 3_500_000L
                        )
                    )
                )
            )
        )
    )
    val composition: StateFlow<Composition> = _composition.asStateFlow()

    // Precision Playhead in Microseconds
    private val _currentPlayheadUs = MutableStateFlow(0L)
    val currentPlayheadUs: StateFlow<Long> = _currentPlayheadUs.asStateFlow()

    // Magnetic Snapping Guide line indicator (null when not snapping)
    private val _snapGuideUs = MutableStateFlow<Long?>(null)
    val snapGuideUs: StateFlow<Long?> = _snapGuideUs.asStateFlow()

    private val _selectedClipId = MutableStateFlow<String?>("clip-1")
    val selectedClipId: StateFlow<String?> = _selectedClipId.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _activeTab = MutableStateFlow(ToolTab.NONE)
    val activeTab: StateFlow<ToolTab> = _activeTab.asStateFlow()

    // Export State
    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _exportProgress = MutableStateFlow(0f)
    val exportProgress: StateFlow<Float> = _exportProgress.asStateFlow()

    private val _exportedFile = MutableStateFlow<File?>(null)
    val exportedFile: StateFlow<File?> = _exportedFile.asStateFlow()

    private var playbackJob: Job? = null

    fun selectClip(clipId: String?) {
        _selectedClipId.value = clipId
    }

    fun setToolTab(tab: ToolTab) {
        _activeTab.value = if (_activeTab.value == tab) ToolTab.NONE else tab
    }

    fun setAspectRatio(ratio: AspectRatio) {
        _composition.value = _composition.value.copy(aspectRatio = ratio)
    }

    /**
     * Seek with Magnetic Snapping:
     * When user drags near clip edges or beat markers, it snaps smoothly.
     */
    fun seekToUs(requestedUs: Long) {
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

    fun togglePlayPause() {
        if (_isPlaying.value) pause() else play()
    }

    private fun play() {
        _isPlaying.value = true
        _snapGuideUs.value = null
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            val stepUs = (TimeUtils.US_PER_SECOND / 30) // 30 FPS update interval
            while (_isPlaying.value) {
                delay(33)
                val nextUs = _currentPlayheadUs.value + stepUs
                if (nextUs >= _composition.value.durationUs) {
                    _currentPlayheadUs.value = 0L
                    _isPlaying.value = false
                    break
                } else {
                    _currentPlayheadUs.value = nextUs
                }
            }
        }
    }

    private fun pause() {
        _isPlaying.value = false
        playbackJob?.cancel()
    }

    /**
     * Module 1 Operator: Split Clip
     */
    fun splitCurrentClip() {
        val clipId = _selectedClipId.value ?: return
        val currentComp = _composition.value
        for (track in currentComp.tracks) {
            val splitResult = timelineOperator.splitClip(track, clipId, _currentPlayheadUs.value)
            if (splitResult != null) {
                val updatedTrack = splitResult.first
                val newClips = splitResult.second
                val updatedTracks = currentComp.tracks.map {
                    if (it.id == track.id) updatedTrack else it
                }
                _composition.value = currentComp.copy(tracks = updatedTracks)
                _selectedClipId.value = newClips.second.id
                break
            }
        }
    }

    /**
     * Module 1 Operator: Left Trim (In-Point)
     */
    fun trimInPoint(clipId: String, newStartUs: Long) {
        val currentComp = _composition.value
        val updatedTracks = currentComp.tracks.map { track ->
            timelineOperator.trimInPoint(track, clipId, newStartUs) ?: track
        }
        _composition.value = currentComp.copy(tracks = updatedTracks)
    }

    /**
     * Module 1 Operator: Right Trim (Out-Point)
     */
    fun trimOutPoint(clipId: String, newEndUs: Long) {
        val currentComp = _composition.value
        val updatedTracks = currentComp.tracks.map { track ->
            timelineOperator.trimOutPoint(track, clipId, newEndUs) ?: track
        }
        _composition.value = currentComp.copy(tracks = updatedTracks)
    }

    /**
     * Module 1 Operator: Ripple Delete
     */
    fun deleteCurrentClip() {
        val clipId = _selectedClipId.value ?: return
        val currentComp = _composition.value
        val updatedTracks = currentComp.tracks.map { track ->
            timelineOperator.rippleDelete(track, clipId)
        }
        _composition.value = currentComp.copy(tracks = updatedTracks)
        _selectedClipId.value = null
    }

    /**
     * Module 1 Operator: Speed Scaling
     */
    fun setSpeed(newSpeed: Float) {
        val clipId = _selectedClipId.value ?: return
        val currentComp = _composition.value
        val updatedTracks = currentComp.tracks.map { track ->
            timelineOperator.setSpeed(track, clipId, newSpeed) ?: track
        }
        _composition.value = currentComp.copy(tracks = updatedTracks)
    }

    fun toggleTrackMute(trackId: String) {
        _composition.value = _composition.value.copy(
            tracks = _composition.value.tracks.map {
                if (it.id == trackId) it.copy(isMuted = !it.isMuted) else it
            }
        )
    }

    fun toggleTrackLock(trackId: String) {
        _composition.value = _composition.value.copy(
            tracks = _composition.value.tracks.map {
                if (it.id == trackId) it.copy(isLocked = !it.isLocked) else it
            }
        )
    }

    fun getActiveClip(): TimelineClip? {
        val clipId = _selectedClipId.value ?: return null
        return _composition.value.tracks.flatMap { it.clips }.firstOrNull { it.id == clipId }
    }

    fun setFilter(filter: FilterPreset) {
        val clipId = _selectedClipId.value ?: return
        updateClip(clipId) { it.copy(filter = filter) }
    }

    fun updateAdjustment(adjustment: ColorAdjustment) {
        val clipId = _selectedClipId.value ?: return
        updateClip(clipId) { it.copy(colorAdjustment = adjustment) }
    }

    fun toggleChromaKey() {
        val clipId = _selectedClipId.value ?: return
        updateClip(clipId) { it.copy(isChromaKeyEnabled = !it.isChromaKeyEnabled) }
    }

    fun toggleBackgroundCutout() {
        val clipId = _selectedClipId.value ?: return
        updateClip(clipId) { it.copy(isBackgroundCutoutEnabled = !it.isBackgroundCutoutEnabled) }
    }

    fun toggleSmoothSlowMo() {
        val clipId = _selectedClipId.value ?: return
        updateClip(clipId) { it.copy(isSmoothSlowMoEnabled = !it.isSmoothSlowMoEnabled) }
    }

    private fun updateClip(clipId: String, transform: (TimelineClip) -> TimelineClip) {
        val currentComp = _composition.value
        val updatedTracks = currentComp.tracks.map { track ->
            val updatedClips = track.clips.map { clip ->
                if (clip.id == clipId) transform(clip) else clip
            }
            track.copy(clips = updatedClips)
        }
        _composition.value = currentComp.copy(tracks = updatedTracks)
    }

    fun addOverlayClip(name: String = "PiP Layer.mp4") {
        val currentComp = _composition.value
        val newClip = TimelineClip(
            id = UUID.randomUUID().toString(),
            sourceUri = null,
            name = name,
            timelineStartUs = _currentPlayheadUs.value,
            timelineDurationUs = 4_000_000L,
            sourceEndOffsetUs = 4_000_000L,
            sourceTotalDurationUs = 4_000_000L
        )
        val updatedTracks = currentComp.tracks.map { track ->
            if (track.type == TrackType.OVERLAY_PIP) {
                track.copy(clips = track.clips + newClip)
            } else track
        }
        _composition.value = currentComp.copy(tracks = updatedTracks)
        _selectedClipId.value = newClip.id
    }

    fun addTextOverlay(text: String = "Double Tap to Edit") {
        val currentComp = _composition.value
        val newClip = TimelineClip(
            id = UUID.randomUUID().toString(),
            sourceUri = null,
            name = text,
            timelineStartUs = _currentPlayheadUs.value,
            timelineDurationUs = 3_000_000L,
            sourceEndOffsetUs = 3_000_000L,
            sourceTotalDurationUs = 3_000_000L
        )
        val updatedTracks = currentComp.tracks.map { track ->
            if (track.type == TrackType.TEXT_CAPTION) {
                track.copy(clips = track.clips + newClip)
            } else track
        }
        _composition.value = currentComp.copy(tracks = updatedTracks)
    }

    fun generateAutoCaptions() {
        viewModelScope.launch {
            val captions = aiEngine.generateAutoCaptions(TimeUtils.usToMs(_composition.value.durationUs))
            val currentComp = _composition.value
            val captionClips = captions.map {
                TimelineClip(
                    id = it.id,
                    sourceUri = null,
                    name = it.text,
                    timelineStartUs = TimeUtils.msToUs(it.startMs),
                    timelineDurationUs = TimeUtils.msToUs(it.durationMs),
                    sourceEndOffsetUs = TimeUtils.msToUs(it.durationMs),
                    sourceTotalDurationUs = TimeUtils.msToUs(it.durationMs)
                )
            }
            val updatedTracks = currentComp.tracks.map { track ->
                if (track.type == TrackType.TEXT_CAPTION) {
                    track.copy(clips = track.clips + captionClips)
                } else track
            }
            _composition.value = currentComp.copy(tracks = updatedTracks)
        }
    }

    fun addAudioTrack(title: String = "Soundtrack Track 2") {
        val currentComp = _composition.value
        val newClip = TimelineClip(
            id = UUID.randomUUID().toString(),
            sourceUri = null,
            name = title,
            timelineStartUs = 0L,
            timelineDurationUs = currentComp.durationUs.coerceAtLeast(8_000_000L),
            sourceEndOffsetUs = currentComp.durationUs.coerceAtLeast(8_000_000L),
            sourceTotalDurationUs = currentComp.durationUs.coerceAtLeast(8_000_000L)
        )
        val updatedTracks = currentComp.tracks.map { track ->
            if (track.type == TrackType.AUDIO) {
                track.copy(clips = track.clips + newClip)
            } else track
        }
        _composition.value = currentComp.copy(tracks = updatedTracks)
    }

    fun exportVideo(settings: ExportSettings) {
        viewModelScope.launch {
            pause()
            _isExporting.value = true
            _exportProgress.value = 0f
            val file = videoEngine.exportProject(
                clips = emptyList(),
                texts = emptyList(),
                audios = emptyList(),
                settings = settings,
                onProgress = { _exportProgress.value = it }
            )
            _exportedFile.value = file
            _isExporting.value = false
        }
    }

    fun dismissExport() {
        _exportedFile.value = null
        _isExporting.value = false
        _exportProgress.value = 0f
    }
}
