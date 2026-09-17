package dev.supersudoku.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import dev.supersudoku.app.state.SettingsRepo
import dev.supersudoku.app.state.StandaloneStore
import dev.supersudoku.app.state.VariantGames

/**
 * Games list for one standalone variant: thin data loader over the shared
 * games library. The minted Original plus generated games.
 */
@Composable
fun VariantGamesScreen(
    settings: SettingsRepo,
    gridId: String,
    onBack: () -> Unit,
    onPlayOriginal: () -> Unit,
    onPlayGame: (id: String) -> Unit,
    onDeleteGame: (id: String) -> Unit,
    onNewGame: () -> Unit,
    generating: Boolean,
    genError: String?,
    refreshTick: Int = 0,
) {
    val context = LocalContext.current
    var games by remember { mutableStateOf<List<VariantGames.Entry>?>(null) }
    var original by remember { mutableStateOf<StandaloneStore.Entry?>(null) }

    LaunchedEffect(gridId, refreshTick) {
        games = VariantGames.list(context, gridId)
        original = StandaloneStore.list(context).firstOrNull { it.gridId == gridId }
    }

    fun progressText(filled: Int, fillable: Int, solved: Boolean): String {
        val pct = if (fillable > 0) filled * 100 / fillable else 100
        return if (solved) "Solved" else "$filled/$fillable · $pct% filled"
    }

    GamesLibraryScreen(
        settings = settings,
        title = original?.name ?: "Variant",
        description = "Fresh 9×9 games dug from the solution. Progress saves per game.",
        onBack = onBack,
        originalTitle = original?.let { "Original" },
        originalSubtitle = original?.let {
            progressText(it.userFilled, it.fillable, it.solved)
        } ?: "",
        originalSolved = original?.solved == true,
        onPlayOriginal = onPlayOriginal,
        restartConfirmTitle = null,
        restartConfirmText = null,
        onRestartOriginal = {},
        games = games?.map { m ->
            LibraryGame(
                id = m.id,
                title = m.name,
                subtitle = "${m.difficultyLabel} · " +
                    progressText(m.userFilled, m.fillable, m.solved),
                solved = m.solved,
            )
        },
        newLabel = { "＋ New ${it.label} game" },
        generatingLabel = "Digging a fresh game (unique solution checked)…",
        onNew = onNewGame,
        onPlayGame = onPlayGame,
        onDeleteGame = onDeleteGame,
        generating = generating,
        genError = genError,
    )
}
