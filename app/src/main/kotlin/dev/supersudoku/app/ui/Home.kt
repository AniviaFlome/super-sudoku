package dev.supersudoku.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.supersudoku.app.state.CustomStore
import dev.supersudoku.app.state.SuperStore

/**
 * Main menu: text header plus independent mode entries — Super Sudoku
 * (the ring), Variants (separate single-variant games), Normal Sudoku
 * (classic 9x9 + library). Variant levels and map generation live inside
 * their categories, not here.
 */
@Composable
fun HomeScreen(
    currentMapId: String?,
    onSuperHome: () -> Unit,
    onVariants: () -> Unit,
    onNormal: () -> Unit,
    onRules: () -> Unit,
    onImport: () -> Unit,
    customs: List<CustomStore.Entry>,
    onPlayCustom: (String) -> Unit,
    onDeleteCustom: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val key = if (currentMapId == null) "bundled" else "map:$currentMapId"
    var detail by remember(key) { mutableStateOf<SuperStore.PuzzleDetail?>(null) }
    var mapLabel by remember(key) { mutableStateOf("Original") }

    LaunchedEffect(key) {
        detail = SuperStore.detail(context, key)
        mapLabel = if (currentMapId == null) "Original"
        else SuperStore.mapName(context, currentMapId) ?: "Map"
    }

    Column(Modifier.fillMaxSize().background(BoardColors.bg)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onImport) {
                Icon(Icons.Filled.FileUpload, "Import puzzle", tint = BoardColors.dim)
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Filled.Settings, "Settings", tint = BoardColors.dim)
            }
        }
        Column(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            RingMark(Modifier.size(88.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                "Super Sudoku",
                style = MaterialTheme.typography.headlineLarge,
                color = BoardColors.given,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
        androidx.compose.foundation.layout.Box(
            Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp),
            ) {
                val d = detail
            val done = d?.grids?.count { it.solved } ?: 0
            val total = d?.grids?.size ?: 8
            val pct = if (d != null && d.fillable > 0) d.userFilled * 100 / d.fillable else 0
            val superSubtitle = if (d != null) {
                "$mapLabel · $pct% filled · $done/$total grids"
            } else {
                "carykh's 8-in-1 variant ring"
            }
            FolderRow(
                title = "Super Sudoku",
                subtitle = superSubtitle,
                onClick = onSuperHome,
                progress = if (d != null && d.fillable > 0) d.userFilled.toFloat() / d.fillable else null,
            )
            FolderRow(
                title = "Variants",
                subtitle = "Each variant as its own 9×9 game",
                onClick = onVariants,
            )
            FolderRow(
                title = "Normal Sudoku",
                subtitle = "Easy · Medium · Hard",
                onClick = onNormal,
            )
            if (customs.isNotEmpty()) {
                SectionLabel("My puzzles")
                for (e in customs) {
                    var confirmDel by remember(e.id) { mutableStateOf(false) }
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .clickable { onPlayCustom(e.id) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(e.name, style = MaterialTheme.typography.bodyLarge, color = BoardColors.entry)
                            Text(
                                "${e.gridCount} grid${if (e.gridCount == 1) "" else "s"}" +
                                    if (e.hasSolution) " · solution known" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = BoardColors.dim,
                            )
                        }
                        if (confirmDel) {
                            TextButton(onClick = {
                                onDeleteCustom(e.id)
                                confirmDel = false
                            }) { Text("Delete", color = BoardColors.conflictFg) }
                        } else {
                            TextButton(onClick = { confirmDel = true }) {
                                Text("✕", color = BoardColors.dim)
                            }
                        }
                    }
                }
            }
            FolderRow(
                title = "How to play",
                subtitle = "Rules for all 8 variants",
                onClick = onRules,
            )
            Spacer(Modifier.height(12.dp))
            }
        }
    }
}

/** App logo: ring of 8 variant dots echoing the puzzle layout. */
@Composable
private fun RingMark(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val n = 8
        val r = size.minDimension / 2f
        val dot = size.minDimension / 5.5f
        for (i in 0 until n) {
            val a = i * (Math.PI * 2 / n) - Math.PI / 2
            val cx = size.width / 2f + kotlin.math.cos(a).toFloat() * (r - dot / 2f)
            val cy = size.height / 2f + kotlin.math.sin(a).toFloat() * (r - dot / 2f)
            drawCircle(
                BoardColors.disjoint[i % BoardColors.disjoint.size].copy(alpha = 0.9f),
                radius = dot / 2f,
                center = Offset(cx, cy),
            )
        }
    }
}
