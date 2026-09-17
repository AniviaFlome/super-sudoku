package dev.supersudoku.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.supersudoku.app.state.MistakeMode
import dev.supersudoku.app.state.SettingsRepo
import dev.supersudoku.app.state.AppTheme
import dev.supersudoku.app.state.CatppuccinAccent
import dev.supersudoku.app.state.ThemeRepo
import dev.supersudoku.app.state.supportsAccent
import kotlinx.coroutines.launch

/** Compact settings page: one dropdown row per choice group. */
@Composable
fun SettingsScreen(settings: SettingsRepo, themes: ThemeRepo, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val mistake by settings.mistakeMode.collectAsState(initial = MistakeMode.CONFLICTS)
    val sameDigit by settings.sameDigitHighlight.collectAsState(initial = true)
    val bidir by settings.bidirectionalSelection.collectAsState(initial = true)
    val doubleTap by settings.focusOnDoubleTap.collectAsState(initial = true)
    val peerClear by settings.autoClearPeerNotes.collectAsState(initial = true)
    val dimDone by settings.dimCompletedDigits.collectAsState(initial = true)
    val orientation by settings.orientation.collectAsState(initial = SettingsRepo.Orientation.PORTRAIT)
    val appTheme by themes.theme.collectAsState(initial = AppTheme.MOCHA)
    val accent by themes.accent.collectAsState(initial = CatppuccinAccent.MAUVE)

    Column(Modifier.fillMaxSize().background(BoardColors.bg)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, "Back", tint = BoardColors.given)
            }
            Text("Settings", style = MaterialTheme.typography.titleMedium, color = BoardColors.given)
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        ) {
            SettingHeader("Input")
            SettingSwitch(
                title = "Tap valued cell arms it",
                subtitle = "Digit-first: tapping a filled cell arms its digit.",
                checked = bidir,
                onChange = { scope.launch { settings.setBidirectionalSelection(it) } },
            )
            SettingSwitch(
                title = "Double-tap focuses grid",
                subtitle = "Overview: double-tapping a grid opens it focused.",
                checked = doubleTap,
                onChange = { scope.launch { settings.setFocusOnDoubleTap(it) } },
            )
            Spacer(Modifier.height(8.dp))
            SettingHeader("Pencil marks")
            SettingSwitch(
                title = "Clear peer marks on entry",
                subtitle = "Entering a digit clears it from row, column and box pencil marks.",
                checked = peerClear,
                onChange = { scope.launch { settings.setAutoClearPeerNotes(it) } },
            )
            SettingSwitch(
                title = "Dim completed digits",
                subtitle = "Dim keypad digits with nothing left to place.",
                checked = dimDone,
                onChange = { scope.launch { settings.setDimCompletedDigits(it) } },
            )
            Spacer(Modifier.height(8.dp))
            SettingHeader("Display")
            SettingChoice(
                title = "Orientation",
                currentLabel = orientationName(orientation),
                options = SettingsRepo.Orientation.entries,
                optionLabel = ::orientationName,
                optionSubtitle = ::orientationBlurb,
                isSelected = { it == orientation },
                onSelect = { scope.launch { settings.setOrientation(it) } },
            )
            Spacer(Modifier.height(8.dp))
            SettingHeader("Board")
            SettingChoice(
                title = "Mistakes",
                currentLabel = mistakeName(mistake),
                options = MistakeMode.entries,
                optionLabel = ::mistakeName,
                optionSubtitle = ::mistakeBlurb,
                isSelected = { it == mistake },
                onSelect = { scope.launch { settings.setMistakeMode(it) } },
            )
            SettingSwitch(
                title = "Highlight same digits",
                subtitle = "Dim every cell holding the selected digit.",
                checked = sameDigit,
                onChange = { scope.launch { settings.setSameDigitHighlight(it) } },
            )
            Spacer(Modifier.height(8.dp))
            SettingHeader("Theme")
            SettingChoice(
                title = "Color theme",
                currentLabel = appTheme.label,
                leading = { ThemeSwatch(theme = appTheme, accent = accent) },
                options = AppTheme.entries,
                optionLabel = { it.label },
                optionSubtitle = { it.blurb },
                isSelected = { it == appTheme },
                onSelect = { scope.launch { themes.setTheme(it) } },
            )
            // Accent picker only exists on accent-capable (Catppuccin) themes.
            if (supportsAccent(appTheme)) {
                val accentColor = remember(appTheme, accent) {
                    AppPalettes.accentMap(appTheme)?.get(accent.name.lowercase())
                }
                SettingChoice(
                    title = "Accent",
                    currentLabel = accent.label,
                    leading = {
                        if (accentColor != null) {
                            androidx.compose.foundation.Canvas(Modifier.size(14.dp)) {
                                drawCircle(accentColor)
                            }
                            Spacer(Modifier.padding(end = 8.dp))
                        }
                    },
                    options = CatppuccinAccent.entries,
                    optionLabel = { it.label },
                    optionSubtitle = null,
                    isSelected = { it == accent },
                    onSelect = { scope.launch { themes.setAccent(it) } },
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                // Keep in sync with app/build.gradle.kts versionName.
                "v1.0.0",
                style = MaterialTheme.typography.bodySmall,
                color = BoardColors.dim,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun mistakeName(m: MistakeMode) = when (m) {
    MistakeMode.OFF -> "Don't show"
    MistakeMode.CONFLICTS -> "Board conflicts"
    MistakeMode.WRONG -> "Wrong digits"
}

private fun mistakeBlurb(m: MistakeMode) = when (m) {
    MistakeMode.OFF -> "No mistake highlighting at all."
    MistakeMode.CONFLICTS -> "Flag digits that break a rule (two 3s in a row, wrong cage sum…)."
    MistakeMode.WRONG -> "Flag digits that differ from the solution."
}

private fun orientationName(o: SettingsRepo.Orientation) = when (o) {
    SettingsRepo.Orientation.SYSTEM -> "System"
    SettingsRepo.Orientation.PORTRAIT -> "Portrait"
    SettingsRepo.Orientation.LANDSCAPE -> "Landscape"
}

private fun orientationBlurb(o: SettingsRepo.Orientation) = when (o) {
    SettingsRepo.Orientation.SYSTEM -> "Follow rotation: tablets get side-by-side play."
    SettingsRepo.Orientation.PORTRAIT -> "Always vertical (previous behavior)."
    SettingsRepo.Orientation.LANDSCAPE -> "Always horizontal."
}

/** One compact dropdown row for a choice group. */
@Composable
private fun <T> SettingChoice(
    title: String,
    currentLabel: String,
    options: List<T>,
    optionLabel: (T) -> String,
    optionSubtitle: ((T) -> String)? = null,
    leading: (@Composable () -> Unit)? = null,
    isSelected: (T) -> Boolean,
    onSelect: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .clickable { expanded = true }.padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.padding(4.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = BoardColors.given)
            Text(currentLabel, style = MaterialTheme.typography.bodySmall, color = BoardColors.entry)
        }
        Icon(Icons.Filled.ArrowDropDown, "Change", tint = BoardColors.dim)
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            for (o in options) {
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(optionLabel(o))
                            val sub = optionSubtitle?.invoke(o)
                            if (sub != null) {
                                Text(sub, style = MaterialTheme.typography.bodySmall, color = BoardColors.dim)
                            }
                        }
                    },
                    trailingIcon = if (isSelected(o)) {
                        { Text("✓", color = BoardColors.entry) }
                    } else null,
                    onClick = { expanded = false; onSelect(o) },
                )
            }
        }
    }
}

@Composable
private fun SettingHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = BoardColors.entry,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

@Composable
private fun ThemeSwatch(theme: AppTheme, accent: CatppuccinAccent) {
    val p = remember(theme, accent) { AppPalettes.forTheme(theme, accent) }
    androidx.compose.foundation.layout.Row(
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(3.dp),
        modifier = Modifier.padding(end = 8.dp),
    ) {
        for (c in listOf(p.bg, p.entry, p.complete)) {
            androidx.compose.foundation.Canvas(Modifier.size(14.dp)) {
                drawCircle(c)
                drawCircle(BoardColors.boxLine, style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f))
            }
        }
    }
}

@Composable
private fun SettingSwitch(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .clickable { onChange(!checked) }.padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = BoardColors.given)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = BoardColors.dim)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}
