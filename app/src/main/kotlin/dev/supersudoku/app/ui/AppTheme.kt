package dev.supersudoku.app.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import dev.supersudoku.app.state.AppTheme
import dev.supersudoku.app.state.CatppuccinAccent

/** Full board palette for one theme (mirrors BoardColors fields). */
data class BoardPalette(
    val bg: Color,
    val cellLine: Color,
    val boxLine: Color,
    val gridOutline: Color,
    val given: Color,
    val entry: Color,
    val note: Color,
    val selection: Color,
    val sameDigit: Color,
    val conflictBg: Color,
    val conflictFg: Color,
    val complete: Color,
    val operandTint: Color,
    val totalTint: Color,
    val dim: Color,
    val dotWhite: Color,
    val bridgePink: Color,
    val diagonal: Color,
    val xWash: Color,
    val cageLine: Color,
    val disjoint: List<Color>,
    val material: ColorScheme,
    val isLight: Boolean,
)

private fun darkMaterial(bg: Color, text: Color, accent: Color) = darkColorScheme(
    background = bg,
    surface = bg,
    surfaceVariant = bg,
    primary = accent,
    onBackground = text,
    onSurface = text,
    onPrimary = bg,
)

/** Official Catppuccin hexes mapped onto the board roles. */
object AppPalettes {
    val DefaultDark = BoardPalette(
        bg = Color(0xFF101418),
        cellLine = Color(0xFF2A3138),
        boxLine = Color(0xFF5A6672),
        gridOutline = Color(0xFF9AA6B2),
        given = Color(0xFFE8EAED),
        entry = Color(0xFF7FB4FF),
        note = Color(0xFF8A94A0),
        selection = Color(0xFF2E4A6B),
        sameDigit = Color(0xFF274264),
        conflictBg = Color(0xFF5C1F1F),
        conflictFg = Color(0xFFFF7B72),
        complete = Color(0xFF3FB950),
        operandTint = Color(0xFF1F6FEB).copy(alpha = 0.16f),
        totalTint = Color(0xFF3FB950).copy(alpha = 0.18f),
        dim = Color(0xFF8A94A0),
        dotWhite = Color(0xFFF0F0F0),
        bridgePink = Color(0xFFF032E6),
        diagonal = Color(0xFFE6194B).copy(alpha = 0.75f),
        xWash = Color(0xFF4363D8).copy(alpha = 0.10f),
        cageLine = Color(0xFF8A94A0),
        disjoint = listOf(
            Color(0xFFE6194B), Color(0xFFF58231), Color(0xFFFFE119),
            Color(0xFF3CB44B), Color(0xFF42D4F4), Color(0xFF4363D8),
            Color(0xFFF032E6), Color(0xFF9A6324), Color(0xFFA9A9A9),
        ).map { it.copy(alpha = 0.22f) },
        material = darkColorScheme(background = Color(0xFF101418)),
        isLight = false,
    )

    // Catppuccin Mocha (base #1e1e2e, text #cdd6f4, blue #89b4fa …)
    val Mocha = catppuccin(
        isLight = false,
        base = Color(0xFF1E1E2E),
        mantle = Color(0xFF181825),
        surface0 = Color(0xFF313244),
        surface2 = Color(0xFF585B70),
        overlay0 = Color(0xFF6C7086),
        overlay1 = Color(0xFF7F849C),
        overlay2 = Color(0xFF9399B2),
        text = Color(0xFFCDD6F4),
        subtext0 = Color(0xFFA6ADC8),
        blue = Color(0xFF89B4FA),
        sapphire = Color(0xFF74C7EC),
        sky = Color(0xFF89DCEB),
        teal = Color(0xFF94E2D5),
        green = Color(0xFFA6E3A1),
        yellow = Color(0xFFF9E2AF),
        peach = Color(0xFFFAB387),
        maroon = Color(0xFFEBA0AC),
        red = Color(0xFFF38BA8),
        pink = Color(0xFFF5C2E7),
        mauve = Color(0xFFCBA6F7),
        lavender = Color(0xFFB4BEFE),
        flamingo = Color(0xFFF2CDCD),
        rosewater = Color(0xFFF5E0DC),
    )

    // Catppuccin Macchiato (base #24273a …)
    val Macchiato = catppuccin(
        isLight = false,
        base = Color(0xFF24273A),
        mantle = Color(0xFF1E2030),
        surface0 = Color(0xFF363A4F),
        surface2 = Color(0xFF5B6078),
        overlay0 = Color(0xFF6E738D),
        overlay1 = Color(0xFF8087A2),
        overlay2 = Color(0xFF939AB8),
        text = Color(0xFFCAD3F5),
        subtext0 = Color(0xFFA5ADCB),
        blue = Color(0xFF8AADF4),
        sapphire = Color(0xFF7DC4E4),
        sky = Color(0xFF91D7E3),
        teal = Color(0xFF8BD5CA),
        green = Color(0xFFA6DA95),
        yellow = Color(0xFFEED49F),
        peach = Color(0xFFF5A97F),
        maroon = Color(0xFFEE99A0),
        red = Color(0xFFED8796),
        pink = Color(0xFFF5BDE6),
        mauve = Color(0xFFC6A0F6),
        lavender = Color(0xFFB7BDF8),
        flamingo = Color(0xFFF0C6C6),
        rosewater = Color(0xFFF4DBD6),
    )

    // Catppuccin Frappé (base #303446 …)
    val Frappe = catppuccin(
        isLight = false,
        base = Color(0xFF303446),
        mantle = Color(0xFF292C3C),
        surface0 = Color(0xFF414559),
        surface2 = Color(0xFF626880),
        overlay0 = Color(0xFF737994),
        overlay1 = Color(0xFF838BA7),
        overlay2 = Color(0xFF949CBB),
        text = Color(0xFFC6D0F5),
        subtext0 = Color(0xFFA5ADCB),
        blue = Color(0xFF8CAAEE),
        sapphire = Color(0xFF85C1DC),
        sky = Color(0xFF99D1DB),
        teal = Color(0xFF81C8BE),
        green = Color(0xFFA6D189),
        yellow = Color(0xFFE5C890),
        peach = Color(0xFFEF9F76),
        maroon = Color(0xFFEA999C),
        red = Color(0xFFE78284),
        pink = Color(0xFFF4B8E4),
        mauve = Color(0xFFCA9EE6),
        lavender = Color(0xFFBABBF1),
        flamingo = Color(0xFFEEBEBE),
        rosewater = Color(0xFFF2D5CF),
    )

    // Catppuccin Latte (base #eff1f5, light)
    val Latte = catppuccin(
        isLight = true,
        base = Color(0xFFEFF1F5),
        mantle = Color(0xFFE6E9EF),
        surface0 = Color(0xFFCCD0DA),
        surface2 = Color(0xFFACB0BE),
        overlay0 = Color(0xFF9CA0B0),
        overlay1 = Color(0xFF8C8FA1),
        overlay2 = Color(0xFF7C7F93),
        text = Color(0xFF4C4F69),
        subtext0 = Color(0xFF6C6F85),
        blue = Color(0xFF1E66F5),
        sapphire = Color(0xFF209FB5),
        sky = Color(0xFF04A5E5),
        teal = Color(0xFF179299),
        green = Color(0xFF40A02B),
        yellow = Color(0xFFDF8E1D),
        peach = Color(0xFFFE640B),
        maroon = Color(0xFFE64553),
        red = Color(0xFFD20F39),
        pink = Color(0xFFEA76CB),
        mauve = Color(0xFF8839EF),
        lavender = Color(0xFF7287FD),
        flamingo = Color(0xFFDD7878),
        rosewater = Color(0xFFDC8A78),
    )

    fun forTheme(t: AppTheme): BoardPalette = forTheme(t, CatppuccinAccent.BLUE)

    // ---------- Fixed palettes (no accent support) ----------

    // Dracula (bg #282a36, fg #f8f8f2, purple #bd93f9 …)
    val Dracula = BoardPalette(
        bg = Color(0xFF282A36),
        cellLine = Color(0xFF44475A),
        boxLine = Color(0xFF6272A4),
        gridOutline = Color(0xFFF8F8F2).copy(alpha = 0.6f),
        given = Color(0xFFF8F8F2),
        entry = Color(0xFFBD93F9),
        note = Color(0xFF6272A4),
        selection = Color(0xFFBD93F9).copy(alpha = 0.30f),
        sameDigit = Color(0xFF8BE9FD).copy(alpha = 0.22f),
        conflictBg = Color(0xFFFF5555).copy(alpha = 0.25f),
        conflictFg = Color(0xFFFF5555),
        complete = Color(0xFF50FA7B),
        operandTint = Color(0xFFBD93F9).copy(alpha = 0.16f),
        totalTint = Color(0xFF50FA7B).copy(alpha = 0.18f),
        dim = Color(0xFF6272A4),
        dotWhite = Color(0xFFF8F8F2),
        bridgePink = Color(0xFFFF79C6),
        diagonal = Color(0xFFFF5555).copy(alpha = 0.75f),
        xWash = Color(0xFFBD93F9).copy(alpha = 0.10f),
        cageLine = Color(0xFF6272A4),
        disjoint = listOf(
            Color(0xFFFF5555), Color(0xFFFFB86C), Color(0xFFF1FA8C),
            Color(0xFF50FA7B), Color(0xFF8BE9FD), Color(0xFFBD93F9),
            Color(0xFFFF79C6), Color(0xFF6272A4), Color(0xFFF8F8F2),
        ).map { it.copy(alpha = 0.22f) },
        material = darkMaterial(Color(0xFF282A36), Color(0xFFF8F8F2), Color(0xFFBD93F9)),
        isLight = false,
    )

    // Tokyo Night (bg #1a1b26, fg #c0caf5, blue #7aa2f7 …)
    val TokyoNight = BoardPalette(
        bg = Color(0xFF1A1B26),
        cellLine = Color(0xFF292E42),
        boxLine = Color(0xFF565F89),
        gridOutline = Color(0xFFC0CAF5).copy(alpha = 0.6f),
        given = Color(0xFFC0CAF5),
        entry = Color(0xFF7AA2F7),
        note = Color(0xFF565F89),
        selection = Color(0xFF7AA2F7).copy(alpha = 0.30f),
        sameDigit = Color(0xFF7DCFFF).copy(alpha = 0.22f),
        conflictBg = Color(0xFFF7768E).copy(alpha = 0.25f),
        conflictFg = Color(0xFFF7768E),
        complete = Color(0xFF9ECE6A),
        operandTint = Color(0xFF7AA2F7).copy(alpha = 0.16f),
        totalTint = Color(0xFF9ECE6A).copy(alpha = 0.18f),
        dim = Color(0xFF565F89),
        dotWhite = Color(0xFFC0CAF5),
        bridgePink = Color(0xFFBB9AF7),
        diagonal = Color(0xFFF7768E).copy(alpha = 0.75f),
        xWash = Color(0xFF7AA2F7).copy(alpha = 0.10f),
        cageLine = Color(0xFF565F89),
        disjoint = listOf(
            Color(0xFFF7768E), Color(0xFFFF9E64), Color(0xFFE0AF68),
            Color(0xFF9ECE6A), Color(0xFF7DCFFF), Color(0xFF7AA2F7),
            Color(0xFFBB9AF7), Color(0xFF565F89), Color(0xFFC0CAF5),
        ).map { it.copy(alpha = 0.22f) },
        material = darkMaterial(Color(0xFF1A1B26), Color(0xFFC0CAF5), Color(0xFF7AA2F7)),
        isLight = false,
    )

    // Gruvbox Dark (bg #282828, fg #ebdbb2, blue #83a598 …)
    val GruvboxDark = BoardPalette(
        bg = Color(0xFF282828),
        cellLine = Color(0xFF3C3836),
        boxLine = Color(0xFF928374),
        gridOutline = Color(0xFFEBDBB2).copy(alpha = 0.6f),
        given = Color(0xFFEBDBB2),
        entry = Color(0xFF83A598),
        note = Color(0xFF928374),
        selection = Color(0xFF83A598).copy(alpha = 0.30f),
        sameDigit = Color(0xFF8EC07C).copy(alpha = 0.22f),
        conflictBg = Color(0xFFFB4934).copy(alpha = 0.25f),
        conflictFg = Color(0xFFFB4934),
        complete = Color(0xFFB8BB26),
        operandTint = Color(0xFF83A598).copy(alpha = 0.16f),
        totalTint = Color(0xFFB8BB26).copy(alpha = 0.18f),
        dim = Color(0xFF928374),
        dotWhite = Color(0xFFEBDBB2),
        bridgePink = Color(0xFFD3869B),
        diagonal = Color(0xFFFB4934).copy(alpha = 0.75f),
        xWash = Color(0xFF83A598).copy(alpha = 0.10f),
        cageLine = Color(0xFF928374),
        disjoint = listOf(
            Color(0xFFFB4934), Color(0xFFFE8019), Color(0xFFFABD2F),
            Color(0xFFB8BB26), Color(0xFF8EC07C), Color(0xFF83A598),
            Color(0xFFD3869B), Color(0xFF928374), Color(0xFFEBDBB2),
        ).map { it.copy(alpha = 0.22f) },
        material = darkMaterial(Color(0xFF282828), Color(0xFFEBDBB2), Color(0xFF83A598)),
        isLight = false,
    )

    // Gruvbox Light (bg #fbf1c7, fg #3c3836, blue #458588 …)
    val GruvboxLight = BoardPalette(
        bg = Color(0xFFFBF1C7),
        cellLine = Color(0xFFEBDBB2),
        boxLine = Color(0xFF7C6F64),
        gridOutline = Color(0xFF3C3836).copy(alpha = 0.6f),
        given = Color(0xFF3C3836),
        entry = Color(0xFF458588),
        note = Color(0xFF7C6F64),
        selection = Color(0xFF458588).copy(alpha = 0.30f),
        sameDigit = Color(0xFF689D6A).copy(alpha = 0.22f),
        conflictBg = Color(0xFFCC241D).copy(alpha = 0.25f),
        conflictFg = Color(0xFF9D0006),
        complete = Color(0xFF98971A),
        operandTint = Color(0xFF458588).copy(alpha = 0.16f),
        totalTint = Color(0xFF98971A).copy(alpha = 0.18f),
        dim = Color(0xFF7C6F64),
        dotWhite = Color(0xFF3C3836),
        bridgePink = Color(0xFFB16286),
        diagonal = Color(0xFFCC241D).copy(alpha = 0.75f),
        xWash = Color(0xFF458588).copy(alpha = 0.10f),
        cageLine = Color(0xFF7C6F64),
        disjoint = listOf(
            Color(0xFFCC241D), Color(0xFFD65D0E), Color(0xFFD79921),
            Color(0xFF98971A), Color(0xFF689D6A), Color(0xFF458588),
            Color(0xFFB16286), Color(0xFF7C6F64), Color(0xFF3C3836),
        ).map { it.copy(alpha = 0.22f) },
        material = lightColorScheme(
            background = Color(0xFFFBF1C7),
            surface = Color(0xFFFBF1C7),
            surfaceVariant = Color(0xFFEBDBB2),
            primary = Color(0xFF458588),
            onBackground = Color(0xFF3C3836),
            onSurface = Color(0xFF3C3836),
            onPrimary = Color(0xFFFBF1C7),
        ),
        isLight = true,
    )

    /**
     * Palette for a theme with a Catppuccin accent applied. Non-accent themes
     * ignore [accent]. Default accent everywhere is mauve (see ThemeRepo).
     */
    fun forTheme(t: AppTheme, accent: CatppuccinAccent): BoardPalette {
        val base = when (t) {
            AppTheme.DEFAULT_DARK -> DefaultDark
            AppTheme.LATTE -> Latte
            AppTheme.FRAPPE -> Frappe
            AppTheme.MACCHIATO -> Macchiato
            AppTheme.MOCHA -> Mocha
            AppTheme.DRACULA -> Dracula
            AppTheme.TOKYO_NIGHT -> TokyoNight
            AppTheme.GRUVBOX_DARK -> GruvboxDark
            AppTheme.GRUVBOX_LIGHT -> GruvboxLight
        }
        val color = accentMap(t)?.get(accent.name.lowercase()) ?: return base
        return base.copy(
            entry = color,
            selection = color.copy(alpha = 0.30f),
            operandTint = color.copy(alpha = 0.16f),
            xWash = color.copy(alpha = 0.10f),
            material = base.material.copy(primary = color),
        )
    }

    /** Accent name (lowercase) -> color for accent-capable themes. */
    fun accentMap(t: AppTheme): Map<String, Color>? = when (t) {
        AppTheme.MOCHA -> mapOf(
            "rosewater" to Color(0xFFF5E0DC), "flamingo" to Color(0xFFF2CDCD),
            "pink" to Color(0xFFF5C2E7), "mauve" to Color(0xFFCBA6F7),
            "red" to Color(0xFFF38BA8), "maroon" to Color(0xFFEBA0AC),
            "peach" to Color(0xFFFAB387), "yellow" to Color(0xFFF9E2AF),
            "green" to Color(0xFFA6E3A1), "teal" to Color(0xFF94E2D5),
            "sky" to Color(0xFF89DCEB), "sapphire" to Color(0xFF74C7EC),
            "blue" to Color(0xFF89B4FA), "lavender" to Color(0xFFB4BEFE),
        )
        AppTheme.MACCHIATO -> mapOf(
            "rosewater" to Color(0xFFF4DBD6), "flamingo" to Color(0xFFF0C6C6),
            "pink" to Color(0xFFF5BDE6), "mauve" to Color(0xFFC6A0F6),
            "red" to Color(0xFFED8796), "maroon" to Color(0xFFEE99A0),
            "peach" to Color(0xFFF5A97F), "yellow" to Color(0xFFEED49F),
            "green" to Color(0xFFA6DA95), "teal" to Color(0xFF8BD5CA),
            "sky" to Color(0xFF91D7E3), "sapphire" to Color(0xFF7DC4E4),
            "blue" to Color(0xFF8AADF4), "lavender" to Color(0xFFB7BDF8),
        )
        AppTheme.FRAPPE -> mapOf(
            "rosewater" to Color(0xFFF2D5CF), "flamingo" to Color(0xFFEEBEBE),
            "pink" to Color(0xFFF4B8E4), "mauve" to Color(0xFFCA9EE6),
            "red" to Color(0xFFE78284), "maroon" to Color(0xFFEA999C),
            "peach" to Color(0xFFEF9F76), "yellow" to Color(0xFFE5C890),
            "green" to Color(0xFFA6D189), "teal" to Color(0xFF81C8BE),
            "sky" to Color(0xFF99D1DB), "sapphire" to Color(0xFF85C1DC),
            "blue" to Color(0xFF8CAAEE), "lavender" to Color(0xFFBABBF1),
        )
        AppTheme.LATTE -> mapOf(
            "rosewater" to Color(0xFFDC8A78), "flamingo" to Color(0xFFDD7878),
            "pink" to Color(0xFFEA76CB), "mauve" to Color(0xFF8839EF),
            "red" to Color(0xFFD20F39), "maroon" to Color(0xFFE64553),
            "peach" to Color(0xFFFE640B), "yellow" to Color(0xFFDF8E1D),
            "green" to Color(0xFF40A02B), "teal" to Color(0xFF179299),
            "sky" to Color(0xFF04A5E5), "sapphire" to Color(0xFF209FB5),
            "blue" to Color(0xFF1E66F5), "lavender" to Color(0xFF7287FD),
        )
        else -> null
    }

    @Suppress("LongParameterList")
    private fun catppuccin(
        isLight: Boolean,
        base: Color,
        mantle: Color,
        surface0: Color,
        surface2: Color,
        overlay0: Color,
        overlay1: Color,
        overlay2: Color,
        text: Color,
        subtext0: Color,
        blue: Color,
        sapphire: Color,
        sky: Color,
        teal: Color,
        green: Color,
        yellow: Color,
        peach: Color,
        maroon: Color,
        red: Color,
        pink: Color,
        mauve: Color,
        lavender: Color,
        flamingo: Color,
        rosewater: Color,
    ): BoardPalette {
        val material = if (isLight) {
            lightColorScheme(
                background = base,
                surface = base,
                surfaceVariant = mantle,
                primary = blue,
                onBackground = text,
                onSurface = text,
                onPrimary = base,
            )
        } else {
            darkMaterial(base, text, blue)
        }
        return BoardPalette(
            bg = base,
            cellLine = surface0,
            boxLine = overlay0,
            gridOutline = overlay2,
            given = text,
            entry = blue,
            note = subtext0,
            selection = blue.copy(alpha = 0.30f),
            sameDigit = sapphire.copy(alpha = 0.22f),
            conflictBg = red.copy(alpha = 0.25f),
            conflictFg = if (isLight) maroon else red,
            complete = green,
            operandTint = blue.copy(alpha = 0.16f),
            totalTint = green.copy(alpha = 0.18f),
            dim = subtext0,
            dotWhite = text,
            bridgePink = pink,
            diagonal = red.copy(alpha = 0.75f),
            xWash = blue.copy(alpha = 0.10f),
            cageLine = overlay1,
            disjoint = listOf(red, peach, yellow, green, teal, sky, blue, mauve, pink)
                .map { it.copy(alpha = 0.22f) },
            material = material,
            isLight = isLight,
        )
    }
}
