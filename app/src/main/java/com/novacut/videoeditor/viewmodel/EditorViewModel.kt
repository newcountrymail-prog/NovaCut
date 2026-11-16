package com.novacut.videoeditor.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.novacut.videoeditor.engine.AIEngine
import com.novacut.videoeditor.engine.AudioProcessor
import com.novacut.videoeditor.engine.VideoEngine
import com.novacut.videoeditor.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

enum class ToolTab {
    NONE,
    SPLIT,
    SPEED,
    AUDIO,
    TEXT,
    OVERLAY,
    EFFECTS,
    FILTERS,
    ADJUST,
    AI_TOOLS,
    CANVAS
}

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val videoEngine = VideoEngine(application)
    private val audioProcessor = AudioProcessor()
    private val aiEngine = AIEngine()

    // Timeline Clips
    private val _clips = MutableStateFlow<List<Clip>>(emptyList())
    val clips: StateFlow<List<Clip>> = _clips.asStateFlow()

    private val _overlayClips = MutableStateFlow<List<Clip>>(emptyList())
    val overlayClips: StateFlow<List<Clip>> = _overlayClips.asStateFlow()

    private val _audioClips = MutableStateFlow<List<AudioClip>>(emptyList())
    val audioClips: StateFlow<List<AudioClip>> = _audioClips.asStateFlow()

    private val _textClips = MutableStateFlow<List<TextClip>>(emptyList())
    val textClips: StateFlow<List<TextClip>> = _textClips.asStateFlow()

    // Editor Playhead & State
    private val _currentPlayheadMs = MutableStateFlow(0L)
    val currentPlayheadMs: StateFlow<Long> = _currentPlayheadMs.asStateFlow()

    private val _totalDurationMs = MutableStateFlow(10000L)
    val totalDurationMs: StateFlow<Long> = _totalDurationMs.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _selectedClipId = MutableStateFlow<String?>("clip-1")
    val selectedClipId: StateFlow<String?> = _selectedClipId.asStateFlow()

    private val _aspectRatio = MutableStateFlow(AspectRatio.PORTRAIT_9_16)
    val aspectRatio: StateFlow<AspectRatio> = _aspectRatio.asStateFlow()

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

    init {
        // Initialize default sample multi-layer project
        val defaultClip1 = Clip(
            id = "clip-1",
            uri = null,
            name = "Intro Scene.mp4",
            startTrimMs = 0L,
            endTrimMs = 5000L,
            durationMs = 5000L,
            filter = FilterPreset.CINEMATIC
        )
        val defaultClip2 = Clip(
            id = "clip-2",
            uri = null,
            name = "Action Shot.mp4",
            startTrimMs = 0L,
            endTrimMs = 5000L,
            durationMs = 5000L,
            filter = FilterPreset.WARM_GOLD
        )
        _clips.value = listOf(defaultClip1, defaultClip2)

        _audioClips.value = listOf(
            audioProcessor.createSampleAudio("Cyberpunk Beat.mp3", 10000L)
        )

        _textClips.value = listOf(
            TextClip(
                id = UUID.randomUUID().toString(),
                text = "NovaCut Studio",
                startMs = 500L,
                durationMs = 3500L,
                textColorHex = "#00E5FF"
            )
        )

        updateTotalDuration()
    }

    private fun updateTotalDuration() {
        val duration = videoEngine.calculateTotalDurationMs(_clips.value)
        _totalDurationMs.value = if (duration > 0) duration else 10000L
    }

    fun selectClip(clipId: String?) {
        _selectedClipId.value = clipId
    }

    fun setToolTab(tab: ToolTab) {
        _activeTab.value = if (_activeTab.value == tab) ToolTab.NONE else tab
    }

    fun setAspectRatio(ratio: AspectRatio) {
        _aspectRatio.value = ratio
    }

    fun seekTo(timeMs: Long) {
        _currentPlayheadMs.value = timeMs.coerceIn(0L, _totalDurationMs.value)
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            play()
        }
    }

    private fun play() {
        _isPlaying.value = true
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            while (_isPlaying.value) {
                delay(33) // ~30 fps update
                val next = _currentPlayheadMs.value + 33
                if (next >= _totalDurationMs.value) {
                    _currentPlayheadMs.value = 0L
                    _isPlaying.value = false
                    break
                } else {
                    _currentPlayheadMs.value = next
                }
            }
        }
    }

    private fun pause() {
        _isPlaying.value = false
        playbackJob?.cancel()
    }

    fun splitCurrentClip() {
        val currentId = _selectedClipId.value ?: return
        val currentList = _clips.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == currentId }
        if (index != -1) {
            val clip = currentList[index]
            val splitAt = clip.startTrimMs + ((clip.endTrimMs - clip.startTrimMs) / 2)
            val parts = videoEngine.splitClip(clip, splitAt)
            if (parts != null) {
                currentList.removeAt(index)
                currentList.add(index, parts.first)
                currentList.add(index + 1, parts.second)
                _clips.value = currentList
                _selectedClipId.value = parts.second.id
                updateTotalDuration()
            }
        }
    }

    fun deleteCurrentClip() {
        val currentId = _selectedClipId.value ?: return
        val currentList = _clips.value.filterNot { it.id == currentId }
        _clips.value = currentList
        _selectedClipId.value = currentList.firstOrNull()?.id
        updateTotalDuration()
    }

    fun duplicateCurrentClip() {
        val currentId = _selectedClipId.value ?: return
        val clip = _clips.value.find { it.id == currentId } ?: return
        val newClip = clip.copy(id = UUID.randomUUID().toString())
        _clips.value = _clips.value + newClip
        _selectedClipId.value = newClip.id
        updateTotalDuration()
    }

    fun setSpeed(speed: Float) {
        val currentId = _selectedClipId.value ?: return
        _clips.value = _clips.value.map {
            if (it.id == currentId) it.copy(speed = speed) else it
        }
        updateTotalDuration()
    }

    fun setFilter(filter: FilterPreset) {
        val currentId = _selectedClipId.value ?: return
        _clips.value = _clips.value.map {
            if (it.id == currentId) it.copy(filter = filter) else it
        }
    }

    fun updateAdjustment(adjustment: ColorAdjustment) {
        val currentId = _selectedClipId.value ?: return
        _clips.value = _clips.value.map {
            if (it.id == currentId) it.copy(adjustment = adjustment) else it
        }
    }

    fun toggleMute() {
        val currentId = _selectedClipId.value ?: return
        _clips.value = _clips.value.map {
            if (it.id == currentId) it.copy(isMuted = !it.isMuted) else it
        }
    }

    fun toggleReverse() {
        val currentId = _selectedClipId.value ?: return
        _clips.value = _clips.value.map {
            if (it.id == currentId) it.copy(isReversed = !it.isReversed) else it
        }
    }

    fun toggleChromaKey() {
        val currentId = _selectedClipId.value ?: return
        _clips.value = _clips.value.map {
            if (it.id == currentId) aiEngine.toggleChromaKey(it) else it
        }
    }

    fun toggleBackgroundCutout() {
        val currentId = _selectedClipId.value ?: return
        _clips.value = _clips.value.map {
            if (it.id == currentId) aiEngine.toggleBackgroundCutout(it) else it
        }
    }

    fun toggleSmoothSlowMo() {
        val currentId = _selectedClipId.value ?: return
        _clips.value = _clips.value.map {
            if (it.id == currentId) aiEngine.toggleSmoothSlowMo(it) else it
        }
    }

    fun addOverlayClip(name: String = "Overlay PIP Clip.mp4") {
        val newOverlay = Clip(
            id = UUID.randomUUID().toString(),
            uri = null,
            name = name,
            startTrimMs = 0L,
            endTrimMs = 4000L,
            durationMs = 4000L
        )
        _overlayClips.value = _overlayClips.value + newOverlay
    }

    fun addTextOverlay(text: String = "Double Tap to Edit") {
        val newText = TextClip(
            id = UUID.randomUUID().toString(),
            text = text,
            startMs = _currentPlayheadMs.value,
            durationMs = 3000L
        )
        _textClips.value = _textClips.value + newText
    }

    fun generateAutoCaptions() {
        viewModelScope.launch {
            val captions = aiEngine.generateAutoCaptions(_totalDurationMs.value)
            _textClips.value = _textClips.value + captions
        }
    }

    fun addAudioTrack(title: String = "Soundtrack Track 2") {
        val newAudio = audioProcessor.createSampleAudio(title, _totalDurationMs.value)
        _audioClips.value = _audioClips.value + newAudio
    }

    fun exportVideo(settings: ExportSettings) {
        viewModelScope.launch {
            pause()
            _isExporting.value = true
            _exportProgress.value = 0f
            val file = videoEngine.exportProject(
                clips = _clips.value,
                texts = _textClips.value,
                audios = _audioClips.value,
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
