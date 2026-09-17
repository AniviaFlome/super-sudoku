package dev.supersudoku.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.supersudoku.app.state.SettingsRepo
import dev.supersudoku.core.MapDifficulty
import kotlinx.coroutines.launch

/** One row in a games library (map, variant game, practice puzzle). */
data class LibraryGame(
    val id: String,
    val title: String,
    val subtitle: String,
    val solved: Boolean,
)

/**
 * The single shared games-library screen. Super maps, variant games and
 * Normal games all render this exact UI from their own data: description,
 * difficulty chips (drive what New creates), New + Random actions,
 * optional Original row (with optional Restart), then the game rows.
 */
@Composable
fun GamesLibraryScreen(
    settings: SettingsRepo,
    title: String,
    description: String,
    onBack: () -> Unit,
    originalTitle: String?,
    originalSubtitle: String,
    originalSolved: Boolean,
    onPlayOriginal: () -> Unit,
    restartConfirmTitle: String?,
    restartConfirmText: String?,
    onRestartOriginal: () -> Unit,
    games: List<LibraryGame>?,
    newLabel: (MapDifficulty) -> String,
    generatingLabel: String,
    onNew: () -> Unit,
    onPlayGame: (String) -> Unit,
    onDeleteGame: (String) -> Unit,
    generating: Boolean,
    genError: String?,
) {
    val scope = rememberCoroutineScope()
    val difficulty by settings.mapDifficulty.collectAsState(initial = MapDifficulty.MEDIUM)
    var confirmRestart by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(BoardColors.bg)) {
        ScreenHeader(title = title, onBack = onBack)
        val list = games
        if (list == null) {
            androidx.compose.foundation.layout.Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
        } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            ) {
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = BoardColors.dim,
                )
                Spacer(Modifier.height(12.dp))
                DifficultyChips(
                    selected = difficulty,
                    enabled = !generating,
                    onSelect = { scope.launch { settings.setMapDifficulty(it) } },
                )
                Spacer(Modifier.height(8.dp))
                NewRandomBar(
                    newLabel = newLabel(difficulty),
                    onNew = onNew,
                    onRandom = {
                        val all = (if (originalTitle != null) listOf("original") else emptyList()) +
                            list.map { it.id }
                        val pick = all.randomOrNull() ?: return@NewRandomBar
                        if (pick == "original") onPlayOriginal() else onPlayGame(pick)
                    },
                    generating = generating,
                    randomEnabled = originalTitle != null || list.isNotEmpty(),
                )
                if (generating) {
                    GeneratingRow(generatingLabel)
                }
                if (genError != null) {
                    GenErrorText(genError)
                }
                Spacer(Modifier.height(12.dp))
                if (originalTitle != null) {
                    GameRow(
                        title = originalTitle,
                        subtitle = originalSubtitle,
                        solved = originalSolved,
                        onPlay = onPlayOriginal,
                        restartLabel = if (restartConfirmTitle != null) "Restart" else null,
                        onRestart = if (restartConfirmTitle != null) {
                            { confirmRestart = true }
                        } else null,
                    )
                }
                for (m in list) {
                    GameRow(
                        title = m.title,
                        subtitle = m.subtitle,
                        solved = m.solved,
                        onPlay = { onPlayGame(m.id) },
                        onDelete = { onDeleteGame(m.id) },
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
    if (confirmRestart && restartConfirmTitle != null && restartConfirmText != null) {
        AlertDialog(
            onDismissRequest = { confirmRestart = false },
            confirmButton = {
                TextButton(onClick = { confirmRestart = false; onRestartOriginal() }) {
                    Text("Erase & restart", color = BoardColors.entry)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRestart = false }) {
                    Text("Cancel", color = BoardColors.entry)
                }
            },
            title = { Text(restartConfirmTitle) },
            text = { Text(restartConfirmText) },
            containerColor = BoardColors.bg,
            titleContentColor = BoardColors.given,
            textContentColor = BoardColors.given,
        )
    }
}
