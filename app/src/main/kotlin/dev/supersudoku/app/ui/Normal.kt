package dev.supersudoku.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import dev.supersudoku.app.state.PracticeStore
import dev.supersudoku.app.state.SettingsRepo
import dev.supersudoku.core.Difficulty
import dev.supersudoku.core.MapDifficulty
import kotlinx.coroutines.launch

/**
 * Normal Sudoku: thin data loader over the shared games library. Classic
 * generated puzzles, one flat list with the difficulty in each subtitle.
 */
@Composable
fun NormalScreen(
    settings: SettingsRepo,
    onBack: () -> Unit,
    onPlay: (id: String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val chipDifficulty by settings.mapDifficulty.collectAsState(initial = MapDifficulty.MEDIUM)
    var entries by remember { mutableStateOf<List<PracticeStore.Entry>?>(null) }
    var creating by remember { mutableStateOf(false) }
    var genError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { entries = PracticeStore.list(context) }
    fun refresh() {
        scope.launch { entries = PracticeStore.list(context) }
    }

    // Per-difficulty numbering ("#1" restarts in each difficulty).
    val counters = HashMap<Difficulty, Int>()
    GamesLibraryScreen(
        settings = settings,
        title = "Normal Sudoku",
        description = "Classic generated puzzles. Progress saves per game.",
        onBack = onBack,
        originalTitle = null,
        originalSubtitle = "",
        originalSolved = false,
        onPlayOriginal = {},
        restartConfirmTitle = null,
        restartConfirmText = null,
        onRestartOriginal = {},
        games = entries?.map { e ->
            val n = (counters[e.difficulty] ?: 0) + 1
            counters[e.difficulty] = n
            val pct = if (e.fillable > 0) e.filledTotal * 100 / e.fillable else 100
            LibraryGame(
                id = e.id,
                title = "#$n",
                subtitle = "${e.difficulty.label} · " + if (e.solved) "Solved"
                else "${e.filledTotal}/${e.fillable} · $pct% filled",
                solved = e.solved,
            )
        },
        newLabel = { "＋ New ${it.label} game" },
        generatingLabel = "Generating…",
        onNew = {
            if (creating) return@GamesLibraryScreen
            creating = true
            genError = null
            scope.launch {
                try {
                    val classic = Difficulty.valueOf(chipDifficulty.name)
                    val loaded = PracticeStore.create(context, classic)
                    refresh()
                    onPlay(loaded.id)
                } catch (_: Exception) {
                    genError = "Couldn't generate a puzzle. Try again."
                } finally {
                    creating = false
                }
            }
        },
        onPlayGame = onPlay,
        onDeleteGame = {
            scope.launch {
                PracticeStore.delete(context, it)
                refresh()
            }
        },
        generating = creating,
        genError = genError,
    )
}
