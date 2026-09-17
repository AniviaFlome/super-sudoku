package dev.supersudoku.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import dev.supersudoku.app.state.PracticeViewModel
import dev.supersudoku.app.state.SettingsRepo
import dev.supersudoku.app.state.StandaloneViewModel

/** Practice entry game: loads the entry, then shares the single-board screen. */
@Composable
fun PracticeScreen(
    vm: PracticeViewModel,
    settings: SettingsRepo,
    entryId: String,
    onBack: () -> Unit,
) {
    LaunchedEffect(entryId) { vm.ensureEntry(entryId) }
    SingleBoardScreen(
        vm = vm,
        settings = settings,
        title = "Practice",
        subtitle = vm.difficulty.label + " · classic 9×9",
        onBack = onBack,
    )
}

/** Standalone variant game: loads the grid or a generated game, then shares the single-board screen. */
@Composable
fun StandaloneScreen(
    vm: StandaloneViewModel,
    settings: SettingsRepo,
    gridId: String,
    gameId: String?,
    onBack: () -> Unit,
) {
    LaunchedEffect(gridId, gameId) {
        if (gameId != null) vm.ensureGame(gameId) else vm.ensureGrid(gridId)
    }
    SingleBoardScreen(
        vm = vm,
        settings = settings,
        title = vm.title.ifBlank { "Variant" },
        subtitle = "standalone 9×9 · independent save",
        onBack = onBack,
    )
}
