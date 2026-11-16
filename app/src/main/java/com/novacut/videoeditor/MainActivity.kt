package com.novacut.videoeditor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novacut.videoeditor.model.AspectRatio
import com.novacut.videoeditor.ui.components.*
import com.novacut.videoeditor.ui.theme.*
import com.novacut.videoeditor.viewmodel.EditorViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: EditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NovaCutTheme {
                MainEditorScreen(viewModel)
            }
        }
    }
}

@Composable
fun MainEditorScreen(viewModel: EditorViewModel) {
    val clips by viewModel.clips.collectAsState()
    val overlayClips by viewModel.overlayClips.collectAsState()
    val audioClips by viewModel.audioClips.collectAsState()
    val textClips by viewModel.textClips.collectAsState()
    val selectedClipId by viewModel.selectedClipId.collectAsState()
    val currentPlayheadMs by viewModel.currentPlayheadMs.collectAsState()
    val totalDurationMs by viewModel.totalDurationMs.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val aspectRatio by viewModel.aspectRatio.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()

    val isExporting by viewModel.isExporting.collectAsState()
    val exportProgress by viewModel.exportProgress.collectAsState()
    val exportedFile by viewModel.exportedFile.collectAsState()

    var showExportDialog by remember { mutableStateOf(false) }

    val activeClip = clips.firstOrNull { it.id == selectedClipId }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BgDark)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "NovaCut",
                            color = CyanAccent,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Studio",
                            color = TextWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                    Text(
                        text = "© 2026 Master H. All Rights Reserved.",
                        color = TextMuted,
                        fontSize = 9.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Undo, contentDescription = "Undo", tint = TextMuted)
                    }
                    IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Redo, contentDescription = "Redo", tint = TextMuted)
                    }
                    Button(
                        onClick = { showExportDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = "Export",
                            tint = BgDark,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Export",
                            color = BgDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth().background(SurfaceDark)) {
                // Contextual Tool Panel
                ToolPanelContainer(
                    activeTab = activeTab,
                    activeClip = activeClip,
                    viewModel = viewModel,
                    onClose = { viewModel.setToolTab(com.novacut.videoeditor.viewmodel.ToolTab.NONE) }
                )

                // Bottom Tools Navigation Bar
                BottomToolbar(
                    activeTab = activeTab,
                    onSelectTab = { viewModel.setToolTab(it) },
                    onQuickSplit = { viewModel.splitCurrentClip() }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BgDark)
        ) {
            // Video Player Viewport (Conforms to Aspect Ratio)
            VideoPlayerView(
                currentPlayheadMs = currentPlayheadMs,
                totalDurationMs = totalDurationMs,
                isPlaying = isPlaying,
                aspectRatio = aspectRatio,
                activeClip = activeClip,
                overlayClips = overlayClips,
                textClips = textClips,
                onTogglePlayPause = { viewModel.togglePlayPause() },
                onCycleAspectRatio = {
                    val nextRatio = when (aspectRatio) {
                        AspectRatio.PORTRAIT_9_16 -> AspectRatio.LANDSCAPE_16_9
                        AspectRatio.LANDSCAPE_16_9 -> AspectRatio.SQUARE_1_1
                        AspectRatio.SQUARE_1_1 -> AspectRatio.PORTRAIT_4_5
                        AspectRatio.PORTRAIT_4_5 -> AspectRatio.PORTRAIT_9_16
                    }
                    viewModel.setAspectRatio(nextRatio)
                },
                modifier = Modifier.weight(1.3f)
            )

            // Multi-Track Timeline (Scrubbable with needle & layers)
            MultiTrackTimeline(
                clips = clips,
                overlayClips = overlayClips,
                audioClips = audioClips,
                textClips = textClips,
                selectedClipId = selectedClipId,
                currentPlayheadMs = currentPlayheadMs,
                totalDurationMs = totalDurationMs,
                onSelectClip = { viewModel.selectClip(it) },
                onSeek = { viewModel.seekTo(it) },
                modifier = Modifier.weight(1.0f)
            )
        }
    }

    if (showExportDialog || isExporting || exportedFile != null) {
        ExportDialog(
            isExporting = isExporting,
            exportProgress = exportProgress,
            exportedFile = exportedFile,
            onStartExport = { settings ->
                viewModel.exportVideo(settings)
            },
            onDismiss = {
                showExportDialog = false
                viewModel.dismissExport()
            }
        )
    }
}
