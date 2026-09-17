package dev.supersudoku.app.ui

import dev.supersudoku.core.Variant

/** Short titles for the rules screen. */
data class VariantRule(val variant: Variant, val name: String, val rule: String, val tip: String)

/**
 * Rule texts are carykh's originals from the puzzle image; tips are ours.
 * Display names match the original too (Normal, >Sudoku<, Consecutive,
 * Strange Boxes, Offset).
 */
val VARIANT_RULES: List<VariantRule> = listOf(
    VariantRule(
        Variant.CLASSIC, "Normal Sudoku",
        "Nothing special. Every row, column and 3×3 box contains the digits 1–9 exactly once.",
        "Start here — the normal grid has the most givens.",
    ),
    VariantRule(
        Variant.FUTOSHIKI, ">Sudoku<",
        "When solved, the greater than and less than signs must be true. A chevron points at the smaller digit: 3 < 5.",
        "Chains of chevrons force an order — follow them to the extremes (1 and 9).",
    ),
    VariantRule(
        Variant.ADDITION, "Addition Sudoku",
        "When solved, the numbers highlighted in blue will add up to the numbers in red beneath them — e.g. 13 + 78 = 91.",
        "Work out the possible splits of each total first (91 = 13+78 = 24+67 …).",
    ),
    VariantRule(
        Variant.KILLER, "Killer Sudoku",
        "The small numbers are equal to the sums of the numbers within the region. Digits may repeat inside a region.",
        "Small cages pin down combos: a 2-cell cage totalling 17 must be 8+9.",
    ),
    VariantRule(
        Variant.KROPKI, "Consecutive Sudoku",
        "If two numbers next to each other are also consecutive, there is a pink bridge between them. Otherwise, there is no pink bridge.",
        "A bridge rules out 1 and 9 on one side (nothing is consecutive with them there).",
    ),
    VariantRule(
        Variant.SUDOKU_X, "Sudoku X",
        "The two diagonals marked in red must contain the numbers 1–9.",
        "The shared corner cells with neighbours are great starting points.",
    ),
    VariantRule(
        Variant.IRREGULAR, "Strange Boxes",
        "Different shapes, same rules: rows and columns hold 1–9, and each bold jigsaw region does too.",
        "Trace each region's outline before placing digits.",
    ),
    VariantRule(
        Variant.DISJOINT, "Offset Sudoku",
        "In addition to each row, column and box having numbers 1–9, each square colour also has the numbers 1–9: a digit in one box's top-left corner bans it from all other top-left corners.",
        "Colours are a visual aid only — the rule is positional.",
    ),
)

fun ruleFor(variant: Variant): VariantRule =
    VARIANT_RULES.first { it.variant == variant }
