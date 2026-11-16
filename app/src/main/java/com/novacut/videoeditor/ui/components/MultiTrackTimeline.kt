package com.novacut.videoeditor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novacut.videoeditor.model.AudioClip
import com.novacut.videoeditor.model.Clip
import com.novacut.videoeditor.model.TextClip
import com.novacut.videoeditor.ui.theme.*

@Composable
fun MultiTrackTimeline(
    clips: List<Clip>,
    overlayClips: List<Clip>,
    audioClips: List<AudioClip>,
    textClips: List<TextClip>,
    selectedClipId: String?,
    currentPlayheadMs: Long,
    totalDurationMs: Long,
    onSelectClip: (String) -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val pxPerSecond = 50.dp // Scale for timeline width

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .border(width = 1.dp, color = SurfaceBorder)
            .pointerInput(totalDurationMs) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val deltaMs = (dragAmount.x * 20).toLong()
                    onSeek((currentPlayheadMs + deltaMs).coerceIn(0L, totalDurationMs))
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(scrollState)
                .padding(vertical = 6.dp)
        ) {
            // 1. Timecode Ruler
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val secondsCount = ((totalDurationMs / 1000) + 5).toInt()
                for (sec in 0..secondsCount) {
                    Box(
                        modifier = Modifier
                            .width(pxPerSecond)
                            .height(20.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = String.format("%02d:%02d", sec / 60, sec % 60),
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 2. Track 1: Main Video Clips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                clips.forEach { clip ->
                    val isSelected = clip.id == selectedClipId
                    val clipDurationSec = ((clip.endTrimMs - clip.startTrimMs) / 1000f).coerceAtLeast(1f)
                    val clipWidth = pxPerSecond * clipDurationSec

                    Box(
                        modifier = Modifier
                            .width(clipWidth)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) SurfaceCard else Color(0xFF1E1E24))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) TextWhite else SurfaceBorder,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable { onSelectClip(clip.id) }
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = clip.name,
                                color = if (isSelected) TextWhite else TextSecondary,
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
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 3. Track 2: Overlay PIP Clips
            if (overlayClips.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    overlayClips.forEach { overlay ->
                        Box(
                            modifier = Modifier
                                .width(pxPerSecond * 4)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF263238))
                                .border(1.dp, CyanAccent, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = "PIP: ${overlay.name}",
                                color = CyanAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // 4. Track 3: Audio Tracks & Beat Markers
            if (audioClips.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    audioClips.forEach { audio ->
                        Box(
                            modifier = Modifier
                                .width(pxPerSecond * 10)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF1B2E24))
                                .border(1.dp, GreenSuccess, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "🎵 ${audio.title}",
                                    color = GreenSuccess,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                // Yellow Beat Dot Marker
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(BeatYellow)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // 5. Track 4: Text & Auto Captions
            if (textClips.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    textClips.forEach { text ->
                        Box(
                            modifier = Modifier
                                .width(pxPerSecond * 3)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF382333))
                                .border(1.dp, PinkAccent, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = "📝 ${text.text}",
                                color = PinkAccent,
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // Center Playhead Needle
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxHeight()
                .width(2.dp)
                .background(TextWhite)
        )
    }
}
