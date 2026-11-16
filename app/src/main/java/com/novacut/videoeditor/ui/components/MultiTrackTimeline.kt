package com.novacut.videoeditor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novacut.videoeditor.model.*
import com.novacut.videoeditor.ui.theme.*

@Composable
fun MultiTrackTimeline(
    composition: Composition,
    selectedClipId: String?,
    currentPlayheadUs: Long,
    snapGuideTimeUs: Long?,
    onSelectClip: (String) -> Unit,
    onSeekUs: (Long) -> Unit,
    onTrimInPoint: (clipId: String, newStartUs: Long) -> Unit,
    onTrimOutPoint: (clipId: String, newEndUs: Long) -> Unit,
    onToggleTrackMute: (trackId: String) -> Unit,
    onToggleTrackLock: (trackId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var zoomScale by remember { mutableFloatStateOf(1.0f) } // 0.5f to 4.0f
    val basePxPerSecond = 60.dp
    val pxPerSecond = basePxPerSecond * zoomScale

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .border(width = 1.dp, color = SurfaceBorder)
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    zoomScale = (zoomScale * zoom).coerceIn(0.5f, 4.0f)
                }
            }
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Track Control Headers (Left Sidebar)
            Column(
                modifier = Modifier
                    .width(72.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF141418))
                    .border(width = 1.dp, color = SurfaceBorder),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Header spacer for ruler
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .background(Color(0xFF181820)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "TRACKS", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }

                composition.tracks.forEach { track ->
                    TrackHeaderItem(
                        track = track,
                        onToggleMute = { onToggleTrackMute(track.id) },
                        onToggleLock = { onToggleTrackLock(track.id) }
                    )
                }
            }

            // Scrollable Timeline Tracks
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .horizontalScroll(scrollState)
                    .pointerInput(composition.durationUs) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val usPerPx = (TimeUtils.US_PER_SECOND / (60 * zoomScale)).toLong()
                            val deltaUs = (dragAmount.x * usPerPx).toLong()
                            onSeekUs((currentPlayheadUs + deltaUs).coerceIn(0L, composition.durationUs))
                        }
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(vertical = 4.dp)
                ) {
                    // Timecode Ruler
                    TimelineTimecodeRuler(
                        totalDurationUs = composition.durationUs,
                        pxPerSecond = pxPerSecond
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Track lanes
                    composition.tracks.forEach { track ->
                        TimelineTrackLane(
                            track = track,
                            selectedClipId = selectedClipId,
                            pxPerSecond = pxPerSecond,
                            onSelectClip = onSelectClip,
                            onTrimInPoint = onTrimInPoint,
                            onTrimOutPoint = onTrimOutPoint
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                // Magnetic Snapping Guide Line
                if (snapGuideTimeUs != null) {
                    val snapX = (snapGuideTimeUs.toDouble() / TimeUtils.US_PER_SECOND * pxPerSecond.value).dp
                    Box(
                        modifier = Modifier
                            .offset(x = snapX)
                            .fillMaxHeight()
                            .width(2.dp)
                            .background(CyanAccent)
                    )
                }

                // Master Playhead Line with Time Bubble
                val playheadX = (currentPlayheadUs.toDouble() / TimeUtils.US_PER_SECOND * pxPerSecond.value).dp
                Box(
                    modifier = Modifier
                        .offset(x = playheadX)
                        .fillMaxHeight()
                        .width(2.dp)
                        .background(TextWhite)
                ) {
                    // Top Needle Marker
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .align(Alignment.TopCenter)
                            .clip(CircleShape)
                            .background(CyanAccent)
                    )
                }
            }
        }
    }
}

@Composable
fun TrackHeaderItem(
    track: CompositionTrack,
    onToggleMute: () -> Unit,
    onToggleLock: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(SurfaceCard)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = track.name,
                color = when (track.type) {
                    TrackType.MAIN_VIDEO -> CyanAccent
                    TrackType.OVERLAY_PIP -> PinkAccent
                    TrackType.AUDIO -> GreenSuccess
                    TrackType.TEXT_CAPTION -> BeatYellow
                    else -> TextWhite
                },
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (track.isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                    contentDescription = "Mute",
                    tint = if (track.isMuted) PinkAccent else TextMuted,
                    modifier = Modifier.size(14.dp).clickable { onToggleMute() }
                )
                Icon(
                    imageVector = if (track.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = "Lock",
                    tint = if (track.isLocked) BeatYellow else TextMuted,
                    modifier = Modifier.size(14.dp).clickable { onToggleLock() }
                )
            }
        }
    }
}

@Composable
fun TimelineTimecodeRuler(
    totalDurationUs: Long,
    pxPerSecond: androidx.compose.ui.unit.Dp
) {
    val totalSeconds = (totalDurationUs / TimeUtils.US_PER_SECOND + 5).toInt()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp)
            .background(Color(0xFF16161C)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (sec in 0..totalSeconds) {
            Box(
                modifier = Modifier
                    .width(pxPerSecond)
                    .fillMaxHeight(),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = String.format("%02d:%02d", sec / 60, sec % 60),
                    color = TextMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun TimelineTrackLane(
    track: CompositionTrack,
    selectedClipId: String?,
    pxPerSecond: androidx.compose.ui.unit.Dp,
    onSelectClip: (String) -> Unit,
    onTrimInPoint: (clipId: String, newStartUs: Long) -> Unit,
    onTrimOutPoint: (clipId: String, newEndUs: Long) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(Color(0xFF1A1A22))
    ) {
        track.clips.forEach { clip ->
            val isSelected = clip.id == selectedClipId
            val startX = (clip.timelineStartUs.toDouble() / TimeUtils.US_PER_SECOND * pxPerSecond.value).dp
            val clipWidth = (clip.timelineDurationUs.toDouble() / TimeUtils.US_PER_SECOND * pxPerSecond.value).dp

            Box(
                modifier = Modifier
                    .offset(x = startX)
                    .width(clipWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isSelected) SurfaceCard else when (track.type) {
                            TrackType.MAIN_VIDEO -> Color(0xFF202632)
                            TrackType.OVERLAY_PIP -> Color(0xFF32202E)
                            TrackType.AUDIO -> Color(0xFF183224)
                            TrackType.TEXT_CAPTION -> Color(0xFF343018)
                            else -> Color(0xFF22222A)
                        }
                    )
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) CyanAccent else SurfaceBorder,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .clickable { onSelectClip(clip.id) }
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = clip.name,
                        color = TextWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    if (clip.speed != 1.0f) {
                        Text(
                            text = "${clip.speed}x",
                            color = CyanAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Interactive Trimming Handles when Clip is Selected
                if (isSelected && !track.isLocked) {
                    // Left In-Point Handle
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .width(12.dp)
                            .fillMaxHeight()
                            .background(CyanAccent, RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp))
                    )
                    // Right Out-Point Handle
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(12.dp)
                            .fillMaxHeight()
                            .background(CyanAccent, RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp))
                    )
                }
            }
        }
    }
}
