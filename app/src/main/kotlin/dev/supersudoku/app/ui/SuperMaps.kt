package dev.supersudoku.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import dev.supersudoku.app.state.SettingsRepo
import dev.supersudoku.app.state.SuperStore

/**
 * Super maps: thin data loader over the shared games library. The Original
 * plus every generated map, each with user-only progress.
 */
@Composable
fun SuperMapsScreen(
    settings: SettingsRepo,
    onBack: () -> Unit,
    onPlayOriginal: () -> Unit,
    onRestartOriginal: () -> Unit,
    onPlayMap: (id: String) -> Unit,
    onDeleteMap: (id: String) -> Unit,
    onNewMap: () -> Unit,
    generating: Boolean,
    genError: String?,
    refreshTick: Int = 0,
) {
    val context = LocalContext.current
    var maps by remember { mutableStateOf<List<SuperStore.MapEntry>?>(null) }
    var original by remember { mutableStateOf<SuperStore.PuzzleDetail?>(null) }

    LaunchedEffect(refreshTick) {
        maps = SuperStore.listMaps(context)
        original = SuperStore.detail(context, "bundled")
    }

    val orig = original
    val origDone = orig?.grids?.count { it.solved } ?: 0
    val origTotal = orig?.grids?.size ?: 8
    GamesLibraryScreen(
        settings = settings,
        title = "Super maps",
        description = "Each map is a fresh set of givens on the same 8-variant ring. " +
            "Progress saves per map.",
        onBack = onBack,
        originalTitle = "Original",
        originalSubtitle = if (orig == null) "" else libraryProgress(
            orig.userFilled, orig.fillable, origDone, origTotal, origDone == origTotal
        ),
        originalSolved = origDone == origTotal && orig != null,
        onPlayOriginal = onPlayOriginal,
        restartConfirmTitle = "Restart the Original?",
        restartConfirmText = "This erases all entries on the Original map (givens stay).",
        onRestartOriginal = onRestartOriginal,
        games = maps?.map { m ->
            LibraryGame(
                id = m.id,
                title = "${m.name} · ${m.difficultyLabel}",
                subtitle = libraryProgress(
                    m.userFilled, m.fillable, m.doneGrids, m.gridCount, m.solved
                ),
                solved = m.solved,
            )
        },
        newLabel = { "＋ New ${it.label} map" },
        generatingLabel = "Digging a fresh map (unique solution checked)…",
        onNew = onNewMap,
        onPlayGame = onPlayMap,
        onDeleteGame = onDeleteMap,
        generating = generating,
        genError = genError,
    )
}
