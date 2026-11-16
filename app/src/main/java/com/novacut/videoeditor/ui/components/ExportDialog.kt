package com.novacut.videoeditor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.novacut.videoeditor.model.ExportSettings
import com.novacut.videoeditor.ui.theme.*
import java.io.File

@Composable
fun ExportDialog(
    isExporting: Boolean,
    exportProgress: Float,
    exportedFile: File?,
    onStartExport: (ExportSettings) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedRes by remember { mutableStateOf("1080p FHD") }
    var selectedFps by remember { mutableIntStateOf(60) }

    Dialog(onDismissRequest = { if (!isExporting) onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SurfaceDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (exportedFile != null) {
                    // Export Success View
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = GreenSuccess,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Export Completed!",
                        color = TextWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Saved to:\n${exportedFile.absolutePath}",
                        color = CyanAccent,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Done", color = BgDark, fontWeight = FontWeight.Bold)
                    }
                } else if (isExporting) {
                    // Export Progress View
                    Text(
                        text = "Exporting Video...",
                        color = TextWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { exportProgress },
                        color = CyanAccent,
                        trackColor = SurfaceCard,
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "${(exportProgress * 100).toInt()}% Rendered (GPU Acceleration)",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                } else {
                    // Settings Selection View
                    Text(
                        text = "Export Settings",
                        color = TextWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Resolution Choices
                    Text(
                        text = "Resolution",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("720p HD", "1080p FHD", "4K UHD").forEach { res ->
                            val isSelected = selectedRes == res
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) CyanAccent else SurfaceCard)
                                    .clickable { selectedRes = res }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = res.split(" ").first(),
                                    color = if (isSelected) BgDark else TextWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Frame Rate Choices
                    Text(
                        text = "Frame Rate",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(24, 30, 60).forEach { fps ->
                            val isSelected = selectedFps == fps
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) CyanAccent else SurfaceCard)
                                    .clickable { selectedFps = fps }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${fps} FPS",
                                    color = if (isSelected) BgDark else TextWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                val width = if (selectedRes.contains("4K")) 3840 else if (selectedRes.contains("1080p")) 1080 else 720
                                val height = if (selectedRes.contains("4K")) 2160 else if (selectedRes.contains("1080p")) 1920 else 1280
                                onStartExport(
                                    ExportSettings(
                                        resolutionName = selectedRes,
                                        width = width,
                                        height = height,
                                        frameRate = selectedFps
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Export", color = BgDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
