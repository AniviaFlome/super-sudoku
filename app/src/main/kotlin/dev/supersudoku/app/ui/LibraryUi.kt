package dev.supersudoku.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.supersudoku.core.MapDifficulty

/**
 * Shared game-library building blocks. Super maps, variant games and Normal
 * games all render the same UI from the same code: difficulty chips, game
 * rows with inline delete-confirm (plus optional restart), New/Random
 * actions, and generating/error states.
 */

/** User-only progress text shared by every games list. */
fun libraryProgress(filled: Int, fillable: Int, done: Int, total: Int, solved: Boolean): String {
    val pct = if (fillable > 0) filled * 100 / fillable else 100
    return if (solved) "Solved · $done/$total grids"
    else "$filled/$fillable · $pct% · $done/$total grids"
}

/** Difficulty chips used wherever games are generated. */
@Composable
fun DifficultyChips(
    selected: MapDifficulty,
    enabled: Boolean,
    onSelect: (MapDifficulty) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
    ) {
        Text(
            "Difficulty:",
            style = MaterialTheme.typography.bodyMedium,
            color = BoardColors.dim,
            modifier = Modifier.padding(end = 8.dp),
        )
        for (d in MapDifficulty.entries) {
            FilterChip(
                selected = selected == d,
                onClick = { onSelect(d) },
                enabled = enabled,
                label = { Text(d.label) },
                modifier = Modifier.padding(end = 8.dp),
            )
        }
    }
}

/**
 * One game row: tap to play, inline delete-confirm, optional restart.
 * Delete state is kept per row instance.
 */
@Composable
fun GameRow(
    title: String,
    subtitle: String,
    solved: Boolean,
    onPlay: () -> Unit,
    onDelete: (() -> Unit)? = null,
    restartLabel: String? = null,
    onRestart: (() -> Unit)? = null,
) {
    var confirmDel by remember(title) { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onPlay).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (solved) BoardColors.complete else BoardColors.given,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = BoardColors.dim,
            )
        }
        if (restartLabel != null && onRestart != null) {
            TextButton(onClick = onRestart) {
                Text(restartLabel, color = BoardColors.dim)
            }
        } else if (onDelete != null) {
            if (confirmDel) {
                TextButton(onClick = onDelete) {
                    Text("Delete", color = BoardColors.conflictFg)
                }
            } else {
                TextButton(onClick = { confirmDel = true }) {
                    Text("✕", color = BoardColors.dim)
                }
            }
        }
    }
}

/** New + Random actions shared by every games list. */
@Composable
fun NewRandomBar(
    newLabel: String,
    onNew: () -> Unit,
    onRandom: () -> Unit,
    generating: Boolean,
    randomEnabled: Boolean = true,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Button(
            onClick = onNew,
            enabled = !generating,
            colors = ButtonDefaults.buttonColors(
                containerColor = BoardColors.entry,
                contentColor = BoardColors.bg,
            ),
            modifier = Modifier.weight(1f),
        ) { Text(if (generating) "Generating…" else newLabel) }
        OutlinedButton(
            onClick = onRandom,
            enabled = !generating && randomEnabled,
        ) { Text("Random") }
    }
}

/** Generating spinner row shared by every games list. */
@Composable
fun GeneratingRow(label: String) {
    Spacer(Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(
            modifier = Modifier.padding(end = 8.dp),
            strokeWidth = 2.dp,
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = BoardColors.dim,
        )
    }
}

/** Generation error text shared by every games list. */
@Composable
fun GenErrorText(msg: String) {
    Spacer(Modifier.height(8.dp))
    Text(msg, color = BoardColors.conflictFg, style = MaterialTheme.typography.bodyMedium)
}
