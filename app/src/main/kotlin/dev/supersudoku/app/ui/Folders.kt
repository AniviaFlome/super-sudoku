package dev.supersudoku.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Shared OpenSudoku-style folder row. */
@Composable
fun FolderRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    dotColorIndex: Int? = null,
    titleColor: Color = BoardColors.given,
    /** 0..1 progress bar under the subtitle, or null for none. */
    progress: Float? = null,
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dotColorIndex != null) {
            val c = BoardColors.disjoint[dotColorIndex % BoardColors.disjoint.size]
            Canvas(Modifier.size(14.dp)) { drawCircle(c) }
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = titleColor)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = BoardColors.dim)
            if (progress != null) {
                Spacer(Modifier.height(6.dp))
                LinearProgress(
                    progress = progress.coerceIn(0f, 1f),
                )
            }
        }
        Icon(Icons.Filled.ChevronRight, null, tint = BoardColors.dim)
    }
}

@Composable
private fun LinearProgress(progress: Float) {
    androidx.compose.material3.LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
        color = BoardColors.entry,
        trackColor = BoardColors.cellLine.copy(alpha = 0.6f),
    )
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = BoardColors.entry,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
    )
}
