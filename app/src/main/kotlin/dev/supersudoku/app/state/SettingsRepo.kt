package dev.supersudoku.app.state

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsStore by preferencesDataStore("settings")

/** How to flag mistakes. WRONG needs a known solution (falls back to CONFLICTS without one). */
enum class MistakeMode { OFF, CONFLICTS, WRONG }

/** What tapping a board cell does. Ordinals are persisted: do not reorder. */
enum class TapMode {
    /**
     * Cell-first (OpenSudoku default): tap a cell to select it, then the
     * keypad enters into the selection. Keypad does nothing without a
     * selection. Clearing works via the same-digit toggle.
     */
    SELECT,
    /**
     * Digit-first (opt-in): tap a keypad digit to arm it, then tap cells to
     * fill them. Tapping the armed digit again disarms.
     */
    INSERT,
    /** Tap a cell opens the popup editor right there. */
    POPUP,
}

/** App settings. */
class SettingsRepo(private val context: Context) {
    private val mistakeKey = intPreferencesKey("mistake_mode")
    private val sameDigitKey = booleanPreferencesKey("same_digit_highlight")

    val mistakeMode: Flow<MistakeMode> =
        context.settingsStore.data.map {
            MistakeMode.entries.getOrElse(it[mistakeKey] ?: 1) { MistakeMode.CONFLICTS }
        }

    suspend fun setMistakeMode(v: MistakeMode) {
        context.settingsStore.edit { it[mistakeKey] = v.ordinal }
    }

    val sameDigitHighlight: Flow<Boolean> =
        context.settingsStore.data.map { it[sameDigitKey] ?: true }

    suspend fun setSameDigitHighlight(v: Boolean) {
        context.settingsStore.edit { it[sameDigitKey] = v }
    }

    private val tapModeKey = intPreferencesKey("tap_mode")

    val tapMode: Flow<TapMode> =
        context.settingsStore.data.map {
            TapMode.entries.getOrElse(it[tapModeKey] ?: 0) { TapMode.SELECT }
        }

    suspend fun setTapMode(v: TapMode) {
        context.settingsStore.edit { it[tapModeKey] = v.ordinal }
    }

    // ---- OpenSudoku input options ----

    private val bidirKey = booleanPreferencesKey("bidirectional_selection")

    /** Digit-first: tapping a valued cell arms its digit. Default on. */
    val bidirectionalSelection: Flow<Boolean> =
        context.settingsStore.data.map { it[bidirKey] ?: true }

    suspend fun setBidirectionalSelection(v: Boolean) {
        context.settingsStore.edit { it[bidirKey] = v }
    }

    // ---- Overview double-tap ----

    private val doubleTapKey = booleanPreferencesKey("focus_on_double_tap")

    /** Double-tap a grid to focus it. Default off. */
    val focusOnDoubleTap: Flow<Boolean> =
        context.settingsStore.data.map { it[doubleTapKey] ?: false }

    suspend fun setFocusOnDoubleTap(v: Boolean) {
        context.settingsStore.edit { it[doubleTapKey] = v }
    }

    // ---- Generated map difficulty ----

    private val mapDiffKey = stringPreferencesKey("map_difficulty")

    val mapDifficulty: Flow<dev.supersudoku.core.MapDifficulty> =
        context.settingsStore.data.map {
            runCatching {
                dev.supersudoku.core.MapDifficulty.valueOf(
                    it[mapDiffKey] ?: dev.supersudoku.core.MapDifficulty.MEDIUM.name
                )
            }.getOrDefault(dev.supersudoku.core.MapDifficulty.MEDIUM)
        }

    suspend fun setMapDifficulty(v: dev.supersudoku.core.MapDifficulty) {
        context.settingsStore.edit { it[mapDiffKey] = v.name }
    }

    // ---- Pencil-mark assists ----

    private val peerClearKey = booleanPreferencesKey("auto_clear_peer_notes")
    private val dimDoneKey = booleanPreferencesKey("dim_completed_digits")

    /** Entering a digit clears it from peer pencil marks. Default on. */
    val autoClearPeerNotes: Flow<Boolean> =
        context.settingsStore.data.map { it[peerClearKey] ?: true }

    suspend fun setAutoClearPeerNotes(v: Boolean) {
        context.settingsStore.edit { it[peerClearKey] = v }
    }

    /** Dim keypad digits with nothing left to place. Default on. */
    val dimCompletedDigits: Flow<Boolean> =
        context.settingsStore.data.map { it[dimDoneKey] ?: true }

    suspend fun setDimCompletedDigits(v: Boolean) {
        context.settingsStore.edit { it[dimDoneKey] = v }
    }

    // ---- Orientation ----

    /** Screen orientation lock. Default portrait (previous behavior). */
    enum class Orientation { SYSTEM, PORTRAIT, LANDSCAPE }

    private val orientationKey = stringPreferencesKey("orientation_lock")

    val orientation: Flow<Orientation> =
        context.settingsStore.data.map {
            runCatching { Orientation.valueOf(it[orientationKey] ?: Orientation.PORTRAIT.name) }
                .getOrDefault(Orientation.PORTRAIT)
        }

    suspend fun setOrientation(v: Orientation) {
        context.settingsStore.edit { it[orientationKey] = v.name }
    }
}
