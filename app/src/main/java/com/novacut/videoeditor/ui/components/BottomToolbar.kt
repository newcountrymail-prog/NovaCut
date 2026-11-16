package com.novacut.videoeditor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novacut.videoeditor.ui.theme.*
import com.novacut.videoeditor.viewmodel.ToolTab

data class ToolItem(
    val tab: ToolTab,
    val title: String,
    val icon: ImageVector
)

@Composable
fun BottomToolbar(
    activeTab: ToolTab,
    onSelectTab: (ToolTab) -> Unit,
    onQuickSplit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val tools = listOf(
        ToolItem(ToolTab.SPLIT, "Split", Icons.Default.ContentCut),
        ToolItem(ToolTab.SPEED, "Speed", Icons.Default.Speed),
        ToolItem(ToolTab.AUDIO, "Audio", Icons.Default.Audiotrack),
        ToolItem(ToolTab.TEXT, "Text", Icons.Default.TextFields),
        ToolItem(ToolTab.OVERLAY, "Overlay", Icons.Default.Layers),
        ToolItem(ToolTab.EFFECTS, "Effects", Icons.Default.AutoAwesome),
        ToolItem(ToolTab.FILTERS, "Filters", Icons.Default.ColorLens),
        ToolItem(ToolTab.ADJUST, "Adjust", Icons.Default.Tune),
        ToolItem(ToolTab.AI_TOOLS, "AI Magic", Icons.Default.Psychology),
        ToolItem(ToolTab.CANVAS, "Canvas", Icons.Default.Crop)
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(SurfaceDark)
            .horizontalScroll(scrollState)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tools.forEach { item ->
            val isActive = activeTab == item.tab
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        if (item.tab == ToolTab.SPLIT) {
                            onQuickSplit()
                        } else {
                            onSelectTab(item.tab)
                        }
                    }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = if (isActive) CyanAccent else TextWhite,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.title,
                    color = if (isActive) CyanAccent else TextMuted,
                    fontSize = 11.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
