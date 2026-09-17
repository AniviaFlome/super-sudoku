package dev.supersudoku.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.supersudoku.app.state.AppTheme
import dev.supersudoku.app.state.CustomStore
import dev.supersudoku.app.state.PracticeViewModel
import dev.supersudoku.app.state.PuzzleSource
import dev.supersudoku.app.state.SettingsRepo
import dev.supersudoku.app.state.StandaloneViewModel
import dev.supersudoku.app.state.SuperStore
import dev.supersudoku.app.state.SuperViewModel
import dev.supersudoku.app.state.ThemeRepo
import dev.supersudoku.app.state.VariantGames
import dev.supersudoku.app.ui.AppPalettes
import dev.supersudoku.app.ui.BoardColors
import dev.supersudoku.app.ui.CustomPlayScreen
import dev.supersudoku.app.ui.HomeScreen
import dev.supersudoku.app.ui.ImportScreen
import dev.supersudoku.app.ui.NormalScreen
import dev.supersudoku.app.ui.PracticeScreen
import dev.supersudoku.app.ui.RulesScreen
import dev.supersudoku.app.ui.SettingsScreen
import dev.supersudoku.app.ui.StandaloneScreen
import dev.supersudoku.app.ui.SuperMapsScreen
import dev.supersudoku.app.ui.SuperScreen
import dev.supersudoku.app.ui.VariantGamesScreen
import dev.supersudoku.app.ui.VariantsScreen
import dev.supersudoku.core.MapDifficulty
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // single shared instance (DataStore must not be opened twice)
            val settings = remember { SettingsRepo(applicationContext) }
            val themes = remember { ThemeRepo(applicationContext) }
            val appTheme by themes.theme.collectAsState(initial = AppTheme.MOCHA)
            val accent by themes.accent.collectAsState(initial = dev.supersudoku.app.state.CatppuccinAccent.MAUVE)
            val palette = AppPalettes.forTheme(appTheme, accent)
            val orientation by settings.orientation.collectAsState(initial = SettingsRepo.Orientation.PORTRAIT)
            // Apply the orientation lock (manifest leaves it unspecified).
            androidx.compose.runtime.LaunchedEffect(orientation) {
                requestedOrientation = when (orientation) {
                    SettingsRepo.Orientation.SYSTEM -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    SettingsRepo.Orientation.PORTRAIT -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    SettingsRepo.Orientation.LANDSCAPE -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                }
            }
            // Push the palette into the board renderer before first draw.
            BoardColors.applyPalette(palette)
            MaterialTheme(colorScheme = palette.material) {
                var screenTag by rememberSaveable { mutableStateOf("home") }
                var practiceEntry by rememberSaveable { mutableStateOf("") }
                var customId by rememberSaveable { mutableStateOf("") }
                var customFocus by remember { mutableStateOf<String?>(null) }
                var customReturn by rememberSaveable { mutableStateOf("home") }
                var settingsReturn by rememberSaveable { mutableStateOf("home") }
                var customsTick by remember { mutableStateOf(0) }
                var mapsTick by remember { mutableStateOf(0) }
                var superFocus by remember { mutableStateOf<String?>(null) }
                var superFocusNonce by remember { mutableStateOf(0) }
                var standaloneId by rememberSaveable { mutableStateOf("") }
                var standaloneGame by remember { mutableStateOf<String?>(null) }
                var standaloneReturn by rememberSaveable { mutableStateOf("home") }
                var variantGrid by rememberSaveable { mutableStateOf("") }
                var vgTick by remember { mutableStateOf(0) }
                var generating by remember { mutableStateOf(false) }
                var genError by remember { mutableStateOf<String?>(null) }
                val mapDifficulty by settings.mapDifficulty.collectAsState(initial = MapDifficulty.MEDIUM)
                // activity-scoped: same instances on every screen, state survives navigation
                val superVm: SuperViewModel = viewModel()
                val scope = rememberCoroutineScope()
                // Current generated map (null = Original); re-read on navigation.
                val currentMapId = remember(screenTag, mapsTick) {
                    SuperStore.currentMapId(applicationContext)
                }
                val customs = remember(screenTag, customsTick) {
                    CustomStore.list(applicationContext)
                }
                fun openSettings(from: String) {
                    settingsReturn = from
                    screenTag = "settings"
                }
                /** Mint a fresh map at the chosen difficulty, then run [after]. */
                fun generateNewMap(after: (String) -> Unit) {
                    if (generating) return
                    generating = true
                    genError = null
                    scope.launch {
                        val entry = SuperStore.createMap(applicationContext, mapDifficulty)
                        generating = false
                        if (entry == null) {
                            genError = "Couldn't mint a fresh map — every safe removal broke " +
                                "uniqueness. Try again, or Restart the Original."
                        } else {
                            mapsTick++
                            superFocus = null
                            superFocusNonce++
                            after(entry.id)
                        }
                    }
                }
                when (screenTag) {
                    "home" -> HomeScreen(
                        currentMapId = currentMapId,
                        onSuperHome = { screenTag = "maps" },
                        onVariants = { screenTag = "variants" },
                        onNormal = { screenTag = "normal" },
                        onRules = { screenTag = "rules" },
                        onImport = { screenTag = "import" },
                        customs = customs,
                        onPlayCustom = {
                            customId = it
                            customFocus = null
                            customReturn = "home"
                            screenTag = "custom"
                        },
                        onDeleteCustom = {
                            scope.launch {
                                CustomStore.delete(applicationContext, it)
                                customsTick++
                            }
                        },
                        onOpenSettings = { openSettings("home") },
                    )
                    "variants" -> VariantsScreen(
                        onBack = { screenTag = "home" },
                        onOpenGames = {
                            variantGrid = it
                            screenTag = "variantgames"
                        },
                    )
                    "variantgames" -> VariantGamesScreen(
                        settings = settings,
                        gridId = variantGrid,
                        onBack = { screenTag = "variants" },
                        onPlayOriginal = {
                            standaloneId = variantGrid
                            standaloneGame = null
                            standaloneReturn = "variantgames"
                            screenTag = "standalone"
                        },
                        onPlayGame = {
                            standaloneId = variantGrid
                            standaloneGame = it
                            standaloneReturn = "variantgames"
                            screenTag = "standalone"
                        },
                        onDeleteGame = {
                            scope.launch {
                                VariantGames.delete(applicationContext, it)
                                vgTick++
                            }
                        },
                        onNewGame = {
                            if (!generating) {
                                generating = true
                                genError = null
                                scope.launch {
                                    val entry = VariantGames.create(
                                        applicationContext, variantGrid, mapDifficulty
                                    )
                                    generating = false
                                    if (entry == null) {
                                        genError = "Couldn't mint a fresh game — every safe removal broke " +
                                            "uniqueness. Try again."
                                    } else {
                                        vgTick++
                                        standaloneId = variantGrid
                                        standaloneGame = entry.id
                                        standaloneReturn = "variantgames"
                                        screenTag = "standalone"
                                    }
                                }
                            }
                        },
                        generating = generating,
                        genError = genError,
                        refreshTick = vgTick,
                    )
                    "normal" -> NormalScreen(
                        settings = settings,
                        onBack = { screenTag = "home" },
                        onPlay = {
                            practiceEntry = it
                            screenTag = "practicePlay"
                        },
                    )
                    "super" -> {
                        val mapId = currentMapId
                        if (mapId == null) {
                            SuperScreen(
                                vm = superVm,
                                settings = settings,
                                source = PuzzleSource.Bundled,
                                initialFocusGridId = superFocus,
                                focusNonce = superFocusNonce,
                                onBack = { screenTag = "maps" },
                                onOpenSettings = { openSettings("super") },
                            )
                        } else {
                            val vm: SuperViewModel = viewModel(key = "map:$mapId")
                            SuperScreen(
                                vm = vm,
                                settings = settings,
                                source = PuzzleSource.SuperMap(mapId),
                                initialFocusGridId = superFocus,
                                focusNonce = superFocusNonce,
                                onBack = { screenTag = "maps" },
                                onOpenSettings = { openSettings("super") },
                            )
                        }
                    }
                    "standalone" -> {
                        val game = standaloneGame
                        val vm: StandaloneViewModel = viewModel(
                            key = if (game == null) "standalone:$standaloneId" else "svgame:$game"
                        )
                        StandaloneScreen(
                            vm = vm,
                            settings = settings,
                            gridId = standaloneId,
                            gameId = game,
                            onBack = { screenTag = standaloneReturn },
                        )
                    }
                    "maps" -> SuperMapsScreen(
                        settings = settings,
                        onBack = { screenTag = "home" },
                        onPlayOriginal = {
                            SuperStore.setCurrentMapId(applicationContext, null)
                            superFocus = null
                            superFocusNonce++
                            screenTag = "super"
                        },
                        onRestartOriginal = {
                            superVm.whenReady(PuzzleSource.Bundled) { superVm.resetBoard() }
                        },
                        onPlayMap = { id ->
                            SuperStore.setCurrentMapId(applicationContext, id)
                            superFocus = null
                            superFocusNonce++
                            screenTag = "super"
                        },
                        onDeleteMap = {
                            scope.launch {
                                SuperStore.deleteMap(applicationContext, it)
                                mapsTick++
                            }
                        },
                        onNewMap = { generateNewMap { screenTag = "super" } },
                        generating = generating,
                        genError = genError,
                        refreshTick = mapsTick,
                    )
                    "custom" -> {
                        val vm: SuperViewModel = viewModel(key = "custom:$customId")
                        CustomPlayScreen(
                            vm = vm,
                            settings = settings,
                            customId = customId,
                            initialFocusGridId = customFocus,
                            onBack = { screenTag = customReturn },
                            onOpenSettings = { openSettings("custom") },
                        )
                    }
                    "practicePlay" -> {
                        val vm: PracticeViewModel = viewModel(key = "practice:$practiceEntry")
                        PracticeScreen(
                            vm,
                            settings,
                            entryId = practiceEntry,
                            onBack = { screenTag = "normal" },
                        )
                    }
                    "rules" -> RulesScreen(onBack = { screenTag = "home" })
                    "import" -> ImportScreen(
                        onBack = { screenTag = "home" },
                        onImported = {
                            customId = it
                            customFocus = null
                            customReturn = "home"
                            screenTag = "custom"
                        },
                    )
                    else -> SettingsScreen(settings, themes, onBack = { screenTag = settingsReturn })
                }
            }
        }
    }
}
