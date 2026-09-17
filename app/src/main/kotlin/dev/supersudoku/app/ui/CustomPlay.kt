package dev.supersudoku.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.supersudoku.app.state.CustomStore
import dev.supersudoku.app.state.PuzzleSource
import dev.supersudoku.app.state.SettingsRepo
import dev.supersudoku.app.state.SuperViewModel
import dev.supersudoku.core.PenpaImport
import dev.supersudoku.core.PenpaImportError
import dev.supersudoku.core.Pos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Play an imported custom puzzle (own view-model instance + save file). */
@Composable
fun CustomPlayScreen(
    vm: SuperViewModel,
    settings: SettingsRepo,
    customId: String,
    initialFocusGridId: String? = null,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var missing by remember(customId) { mutableStateOf(false) }
    LaunchedEffect(customId) {
        val entry = withContext(Dispatchers.IO) {
            CustomStore.load(vm.getApplication(), customId)
        }
        if (entry == null) missing = true
        else vm.ensureLoaded(PuzzleSource.Custom(customId))
    }
    LaunchedEffect(customId, initialFocusGridId) {
        if (initialFocusGridId != null) vm.focusGridId = initialFocusGridId
    }
    if (missing) {
        Box(Modifier.fillMaxSize().background(BoardColors.bg), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Puzzle not found", color = BoardColors.given)
                Spacer(Modifier.height(12.dp))
                Button(onClick = onBack) { Text("Back") }
            }
        }
    } else {
        SuperBoardContent(vm, settings, onBack, onOpenSettings)
    }
}

/** Shared back + title header row. */
@Composable
fun ScreenHeader(title: String, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Filled.ArrowBack, "Back", tint = BoardColors.given)
        }
        Text(title, style = MaterialTheme.typography.titleMedium, color = BoardColors.given)
    }
}

/** Paste a Penpa+ URL (or full link) to import a puzzle. */
@Composable
fun ImportScreen(onBack: () -> Unit, onImported: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var url by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun doImport() {
        val text = url.trim()
        if (text.isEmpty() || busy) return
        busy = true
        error = null
        scope.launch(Dispatchers.Default) {
            try {
                val imported = PenpaImport.import(text)
                val puzzle = imported.puzzle.normalized()
                val dx = imported.puzzle.grids.minOf { it.x }
                val dy = imported.puzzle.grids.minOf { it.y }
                val sol = imported.solution?.mapKeys { (p, _) -> Pos(p.x - dx, p.y - dy) }
                val name = if (puzzle.grids.size == 1) puzzle.grids[0].name
                else "Imported (${puzzle.grids.size} grids)"
                val id = CustomStore.save(context, puzzle, name, sol)
                withContext(kotlinx.coroutines.Dispatchers.Main) { onImported(id) }
            } catch (e: PenpaImportError) {
                error = e.message
            } catch (e: Exception) {
                error = "Import failed: ${e.message}"
            } finally {
                busy = false
            }
        }
    }

    Column(
        Modifier.fillMaxSize().background(BoardColors.bg).padding(20.dp)
            .imePadding()
            .verticalScroll(rememberScrollState()),
    ) {
        ScreenHeader(title = "Import puzzle", onBack = onBack)
        Spacer(Modifier.height(12.dp))
        Text(
            "Paste a Penpa+ link (penpa-edit #m=solve URL). " +
                "Supports 9×9 classic, > <, killer (modern penpa), consecutive dots, " +
                "diagonals, jigsaw regions, offset colours and blue/red addition blocks.",
            style = MaterialTheme.typography.bodyMedium,
            color = BoardColors.dim,
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = url,
            onValueChange = { url = it; error = null },
            label = { Text("Penpa+ URL") },
            modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp),
            minLines = 3,
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = ::doImport,
            enabled = !busy && url.isNotBlank(),
            colors = ButtonDefaults.buttonColors(
                containerColor = BoardColors.entry,
                contentColor = BoardColors.bg,
            ),
        ) {
            Text(if (busy) "Importing…" else "Import")
        }
        if (error != null) {
            Spacer(Modifier.height(12.dp))
            Text(error!!, color = BoardColors.conflictFg, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
