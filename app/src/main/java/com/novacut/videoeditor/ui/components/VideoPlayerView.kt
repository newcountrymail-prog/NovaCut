package com.novacut.videoeditor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novacut.videoeditor.model.AspectRatio
import com.novacut.videoeditor.model.Clip
import com.novacut.videoeditor.model.FilterPreset
import com.novacut.videoeditor.model.TextClip
import com.novacut.videoeditor.ui.theme.*

@Composable
fun VideoPlayerView(
    currentPlayheadMs: Long,
    totalDurationMs: Long,
    isPlaying: Boolean,
    aspectRatio: AspectRatio,
    activeClip: Clip?,
    overlayClips: List<Clip>,
    textClips: List<TextClip>,
    onTogglePlayPause: () -> Unit,
    onCycleAspectRatio: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(BgDark),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Player Bar (Aspect Ratio & Timecode)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = SurfaceCard,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.clickable { onCycleAspectRatio() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AspectRatio,
                        contentDescription = "Aspect Ratio",
                        tint = CyanAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = aspectRatio.label,
                        color = TextWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Timecode display
            val currentSec = currentPlayheadMs / 1000
            val totalSec = totalDurationMs / 1000
            val timeText = String.format("%02d:%02d / %02d:%02d", currentSec / 60, currentSec % 60, totalSec / 60, totalSec % 60)
            Text(
                text = timeText,
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Viewport Box
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            // Video Frame conforming to Aspect Ratio
            val frameModifier = Modifier
                .aspectRatio(aspectRatio.ratioWidth / aspectRatio.ratioHeight)
                .fillMaxHeight()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF141416))
                .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp))

            Box(
                modifier = frameModifier,
                contentAlignment = Alignment.Center
            ) {
                // Background visual simulation
                val filterColor = when (activeClip?.filter) {
                    FilterPreset.CINEMATIC -> Color(0x3300E5FF)
                    FilterPreset.CYBERPUNK -> Color(0x33FF2A6D)
                    FilterPreset.WARM_GOLD -> Color(0x33FFA000)
                    FilterPreset.COLD_BLUE -> Color(0x330288D1)
                    FilterPreset.MONO_NOIR -> Color(0x66000000)
                    FilterPreset.EMERALD -> Color(0x3300E676)
                    else -> Color.Transparent
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1E1E24))
                ) {
                    // Filter Overlay
                    Box(modifier = Modifier.fillMaxSize().background(filterColor))

                    // Simulated Video Scene Details
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = activeClip?.name ?: "No Video Loaded",
                            color = TextWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (activeClip?.isChromaKeyEnabled == true) {
                            Text(
                                text = "⚡ Chroma Key: ON (${activeClip.chromaKeyColorHex})",
                                color = GreenSuccess,
                                fontSize = 11.sp
                            )
                        }
                        if (activeClip?.isBackgroundCutoutEnabled == true) {
                            Text(
                                text = "✨ AI Cutout: Active",
                                color = CyanAccent,
                                fontSize = 11.sp
                            )
                        }
                        if (activeClip?.isSmoothSlowMoEnabled == true) {
                            Text(
                                text = "🧈 Optical Flow 60fps Slow-Mo",
                                color = BeatYellow,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Picture in Picture (Overlay)
                    if (overlayClips.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .size(width = 80.dp, height = 110.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF2A2A34))
                                .border(1.dp, CyanAccent, RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "PIP Layer",
                                color = CyanAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Render Subtitles / Auto Captions
                    val currentText = textClips.firstOrNull {
                        currentPlayheadMs >= it.startMs && currentPlayheadMs <= (it.startMs + it.durationMs)
                    }
                    if (currentText != null) {
                        Surface(
                            color = Color(0xAA000000),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 24.dp)
                        ) {
                            Text(
                                text = currentText.text,
                                color = Color(android.graphics.Color.parseColor(currentText.textColorHex)),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Play / Pause Overlay Button
                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .size(52.dp)
                        .background(Color(0x66000000), RoundedCornerShape(26.dp))
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = TextWhite,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}
