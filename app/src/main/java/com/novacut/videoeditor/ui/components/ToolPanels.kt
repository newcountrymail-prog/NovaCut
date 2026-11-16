package com.novacut.videoeditor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novacut.videoeditor.model.AspectRatio
import com.novacut.videoeditor.model.ColorAdjustment
import com.novacut.videoeditor.model.FilterPreset
import com.novacut.videoeditor.model.TimelineClip
import com.novacut.videoeditor.ui.theme.*
import com.novacut.videoeditor.viewmodel.EditorViewModel
import com.novacut.videoeditor.viewmodel.ToolTab

@Composable
fun ToolPanelContainer(
    activeTab: ToolTab,
    activeClip: TimelineClip?,
    viewModel: EditorViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (activeTab == ToolTab.NONE) return

    Surface(
        color = SurfaceDark,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (activeTab) {
                        ToolTab.SPEED -> "Speed Ramping & Slow-Mo"
                        ToolTab.AUDIO -> "Audio Studio & Voice FX"
                        ToolTab.TEXT -> "Text & AI Auto-Captions"
                        ToolTab.OVERLAY -> "Overlay & Picture-in-Picture (PiP)"
                        ToolTab.FILTERS -> "Cinematic Filters & LUTs"
                        ToolTab.ADJUST -> "Color Grading & Adjust"
                        ToolTab.AI_TOOLS -> "AI Magic Tools"
                        ToolTab.CANVAS -> "Canvas & Aspect Ratio"
                        else -> "Tool"
                    },
                    color = TextWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (activeTab) {
                ToolTab.SPEED -> {
                    var speedVal by remember { mutableFloatStateOf(activeClip?.speed ?: 1.0f) }
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Clip Speed", color = TextSecondary, fontSize = 12.sp)
                            Text(text = String.format("%.1fx", speedVal), color = CyanAccent, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = speedVal,
                            onValueChange = {
                                speedVal = it
                                viewModel.setSpeed(it)
                            },
                            valueRange = 0.2f..4.0f,
                            steps = 18,
                            colors = SliderDefaults.colors(thumbColor = CyanAccent, activeTrackColor = CyanAccent)
                        )
                        Button(
                            onClick = { viewModel.toggleSmoothSlowMo() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (activeClip?.isSmoothSlowMoEnabled == true) BeatYellow else SurfaceCard
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (activeClip?.isSmoothSlowMoEnabled == true) "✓ AI Optical Flow Slow-Mo (ON)" else "Enable AI Smooth Slow-Mo",
                                color = if (activeClip?.isSmoothSlowMoEnabled == true) BgDark else TextWhite,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                ToolTab.FILTERS -> {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(FilterPreset.values()) { preset ->
                            val isSelected = activeClip?.filter == preset
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) CyanAccent else SurfaceCard)
                                    .clickable { viewModel.setFilter(preset) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = preset.displayName,
                                    color = if (isSelected) BgDark else TextWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                ToolTab.TEXT -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.addTextOverlay() },
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("+ Add Text", color = TextWhite)
                        }
                        Button(
                            onClick = { viewModel.generateAutoCaptions() },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("⚡ Auto Captions", color = BgDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                ToolTab.OVERLAY -> {
                    Button(
                        onClick = { viewModel.addOverlayClip() },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("+ Add Video/Photo Overlay (PiP)", color = BgDark, fontWeight = FontWeight.Bold)
                    }
                }

                ToolTab.AUDIO -> {
                    Button(
                        onClick = { viewModel.addAudioTrack() },
                        colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("+ Add Music / Audio Track", color = BgDark, fontWeight = FontWeight.Bold)
                    }
                }

                ToolTab.AI_TOOLS -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Chroma Key (Green Screen Removal)", color = TextWhite, fontSize = 13.sp)
                            Switch(
                                checked = activeClip?.isChromaKeyEnabled == true,
                                onCheckedChange = { viewModel.toggleChromaKey() },
                                colors = SwitchDefaults.colors(checkedThumbColor = CyanAccent, checkedTrackColor = SurfaceCard)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "AI Cutout (No Green Screen Body Matting)", color = TextWhite, fontSize = 13.sp)
                            Switch(
                                checked = activeClip?.isBackgroundCutoutEnabled == true,
                                onCheckedChange = { viewModel.toggleBackgroundCutout() },
                                colors = SwitchDefaults.colors(checkedThumbColor = CyanAccent, checkedTrackColor = SurfaceCard)
                            )
                        }
                    }
                }

                ToolTab.ADJUST -> {
                    var brightness by remember { mutableFloatStateOf(activeClip?.colorAdjustment?.brightness ?: 0f) }
                    var contrast by remember { mutableFloatStateOf(activeClip?.colorAdjustment?.contrast ?: 1f) }
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Brightness", color = TextSecondary, fontSize = 12.sp)
                            Text(String.format("%.1f", brightness), color = CyanAccent, fontSize = 12.sp)
                        }
                        Slider(
                            value = brightness,
                            onValueChange = {
                                brightness = it
                                viewModel.updateAdjustment(activeClip?.colorAdjustment?.copy(brightness = it) ?: ColorAdjustment(brightness = it))
                            },
                            valueRange = -0.5f..0.5f
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Contrast", color = TextSecondary, fontSize = 12.sp)
                            Text(String.format("%.1f", contrast), color = CyanAccent, fontSize = 12.sp)
                        }
                        Slider(
                            value = contrast,
                            onValueChange = {
                                contrast = it
                                viewModel.updateAdjustment(activeClip?.colorAdjustment?.copy(contrast = it) ?: ColorAdjustment(contrast = it))
                            },
                            valueRange = 0.5f..1.5f
                        )
                    }
                }

                ToolTab.CANVAS -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val currentRatio = viewModel.composition.collectAsState().value.aspectRatio
                        AspectRatio.values().forEach { ratio ->
                            val isSelected = currentRatio == ratio
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) CyanAccent else SurfaceCard)
                                    .clickable { viewModel.setAspectRatio(ratio) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = ratio.label.split(" ").first(),
                                    color = if (isSelected) BgDark else TextWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                else -> {}
            }
        }
    }
}
