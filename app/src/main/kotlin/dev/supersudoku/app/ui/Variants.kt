package dev.supersudoku.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.supersudoku.app.state.StandaloneStore

/**
 * Variants category: each variant as its own 9x9 game family with an
 * independent save — separate versions just like Normal Sudoku.
 * Tapping a variant opens its games list (Original + generated).
 */
@Composable
fun VariantsScreen(
    onBack: () -> Unit,
    onOpenGames: (gridId: String) -> Unit,
) {
    Column(Modifier.fillMaxSize().background(BoardColors.bg)) {
        ScreenHeader(title = "Variants", onBack = onBack)
        StandaloneLevelList(onPlayStandalone = onOpenGames)
    }
}

/** Shared standalone level list with per-variant progress. */
@Composable
fun StandaloneLevelList(onPlayStandalone: (gridId: String) -> Unit) {
    val context = LocalContext.current
    var standalone by remember { mutableStateOf<List<StandaloneStore.Entry>?>(null) }

    LaunchedEffect(Unit) {
        standalone = StandaloneStore.list(context)
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 8.dp),
    ) {
        Text(
            "Each variant as its own 9×9 with an independent save.",
            style = MaterialTheme.typography.bodySmall,
            color = BoardColors.dim,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Spacer(Modifier.height(4.dp))
        val levels = standalone
        if (levels == null) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Loading levels…", color = BoardColors.dim, style = MaterialTheme.typography.bodySmall)
            }
        } else {
            for ((i, e) in levels.withIndex()) {
                val pct = if (e.fillable > 0) e.userFilled * 100 / e.fillable else 100
                FolderRow(
                    title = e.name,
                    subtitle = if (e.solved) "Solved · ${e.variantLabel}"
                    else "${e.userFilled}/${e.fillable} · $pct% · ${e.variantLabel}",
                    dotColorIndex = i,
                    titleColor = if (e.solved) BoardColors.complete else BoardColors.given,
                    progress = if (e.fillable > 0) e.userFilled.toFloat() / e.fillable else null,
                    onClick = { onPlayStandalone(e.gridId) },
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
