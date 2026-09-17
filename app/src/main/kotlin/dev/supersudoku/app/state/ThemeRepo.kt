package dev.supersudoku.app.state

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeStore by preferencesDataStore("theme")

/** App color theme. DEFAULT_DARK is the original Super Sudoku palette. */
enum class AppTheme(val label: String, val blurb: String) {
    DEFAULT_DARK("Super Dark", "Original palette, dark background."),
    LATTE("Catppuccin Latte", "Light theme, dark text on cream."),
    FRAPPE("Catppuccin Frappé", "Soft dark theme, low contrast."),
    MACCHIATO("Catppuccin Macchiato", "Mid-contrast dark theme."),
    MOCHA("Catppuccin Mocha", "High-contrast dark theme."),
    DRACULA("Dracula", "Dark purple-gray, fixed palette."),
    TOKYO_NIGHT("Tokyo Night", "Blue-tinted dark, fixed palette."),
    GRUVBOX_DARK("Gruvbox Dark", "Warm retro dark, fixed palette."),
    GRUVBOX_LIGHT("Gruvbox Light", "Warm retro light, fixed palette."),
}

/** Whether a theme supports a custom Catppuccin accent. */
fun supportsAccent(t: AppTheme): Boolean = when (t) {
    AppTheme.LATTE, AppTheme.FRAPPE, AppTheme.MACCHIATO, AppTheme.MOCHA -> true
    AppTheme.DEFAULT_DARK, AppTheme.DRACULA, AppTheme.TOKYO_NIGHT,
    AppTheme.GRUVBOX_DARK, AppTheme.GRUVBOX_LIGHT -> false
}

/** Catppuccin accent color choices (names match upstream). */
enum class CatppuccinAccent {
    ROSEWATER, FLAMINGO, PINK, MAUVE, RED, MAROON, PEACH, YELLOW,
    GREEN, TEAL, SKY, SAPPHIRE, BLUE, LAVENDER;

    val label: String get() = name.lowercase().replaceFirstChar { it.uppercase() }
}

class ThemeRepo(private val context: Context) {
    private val key = stringPreferencesKey("app_theme")
    private val accentKey = stringPreferencesKey("app_accent")

    val theme: Flow<AppTheme> =
        context.themeStore.data.map { prefs ->
            runCatching { AppTheme.valueOf(prefs[key] ?: AppTheme.MOCHA.name) }
                .getOrDefault(AppTheme.MOCHA)
        }

    suspend fun setTheme(v: AppTheme) {
        context.themeStore.edit { it[key] = v.name }
    }

    val accent: Flow<CatppuccinAccent> =
        context.themeStore.data.map { prefs ->
            runCatching { CatppuccinAccent.valueOf(prefs[accentKey] ?: CatppuccinAccent.MAUVE.name) }
                .getOrDefault(CatppuccinAccent.MAUVE)
        }

    suspend fun setAccent(v: CatppuccinAccent) {
        context.themeStore.edit { it[accentKey] = v.name }
    }
}
