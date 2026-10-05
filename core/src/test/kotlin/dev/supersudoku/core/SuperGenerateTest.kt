package dev.supersudoku.core

import java.io.File
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SuperGenerateTest {
    private fun asset(name: String): String {
        val f = File("../app/src/main/assets/$name")
        assertTrue(f.exists(), "missing ${f.absolutePath}")
        return f.readText()
    }

    @Test
    fun templateGivensAreUniquelySolvable() {
        val puzzle = PuzzleLoader.load(asset("puzzle.json"))
        val base = HashMap<Pos, Int>()
        for (g in puzzle.grids) for ((p, v) in g.givens) base[p] = v
        assertEquals(1, SuperGenerator.countSolutions(puzzle.grids, base, 2))
    }

    @Test
    fun diggingKeepsUniqueSolutionAndSubsetOfIt() {
        val puzzle = PuzzleLoader.load(asset("puzzle.json"))
        val solution = PuzzleLoader.loadSolution(asset("solution.json"))
        val out = SuperGenerator.generate(
            template = puzzle,
            solution = solution,
            random = Random(7),
            maxRemove = 4,
            timeBudgetMs = 120_000,
        )
        // Result is a strict subset of the template givens and of the solution.
        val base = HashMap<Pos, Int>()
        for (g in puzzle.grids) for ((p, v) in g.givens) base[p] = v
        assertTrue(out.givens.size <= base.size)
        for ((p, v) in out.givens) {
            assertEquals(base[p], v, "generated given differs at $p")
            assertEquals(solution[p], v, "generated given disagrees with solution at $p")
        }
        // Still exactly one solution.
        assertEquals(1, SuperGenerator.countSolutions(puzzle.grids, out.givens, 2))
    }

    @Test
    fun difficultyMinimumsAreReachableNoveltyFloors() {
        for (d in MapDifficulty.entries) {
            assertTrue(d.minRemove > 0, "${d.name}: minRemove must reject duplicates")
            assertTrue(d.minRemove <= d.maxRemove, "${d.name}: min above max")
        }
    }

    @Test
    fun easyAndMediumHitCapsWithNovelUniqueOutput() {
        val puzzle = PuzzleLoader.load(asset("puzzle.json"))
        val solution = PuzzleLoader.loadSolution(asset("solution.json"))
        val base = HashMap<Pos, Int>()
        for (g in puzzle.grids) for ((p, v) in g.givens) base[p] = v
        val easy = SuperGenerator.generate(
            template = puzzle, solution = solution, random = Random(1),
            maxRemove = MapDifficulty.EASY.maxRemove,
            timeBudgetMs = MapDifficulty.EASY.timeBudgetMs,
        )
        val medium = SuperGenerator.generate(
            template = puzzle, solution = solution, random = Random(2),
            maxRemove = MapDifficulty.MEDIUM.maxRemove,
            timeBudgetMs = MapDifficulty.MEDIUM.timeBudgetMs,
        )
        assertEquals(MapDifficulty.EASY.maxRemove, easy.removedCount)
        assertEquals(MapDifficulty.MEDIUM.maxRemove, medium.removedCount)
        // Novel: strict subsets of the template, distinct from each other.
        assertTrue(easy.givens.size == base.size - easy.removedCount)
        assertTrue(medium.givens.size == base.size - medium.removedCount)
        assertTrue(easy.givens != base, "easy map duplicates the Original")
        assertTrue(medium.givens != base, "medium map duplicates the Original")
        assertTrue(easy.givens != medium.givens, "easy and medium coincide")
        assertEquals(1, SuperGenerator.countSolutions(puzzle.grids, easy.givens, 2))
        assertEquals(1, SuperGenerator.countSolutions(puzzle.grids, medium.givens, 2))
    }

    @Test
    fun distinctSeedsYieldDistinctMaps() {
        // Guards the same-millisecond duplicate bug: the shuffle order (not
        // the seed source) must drive variety, so different seeds must give
        // different maps.
        val puzzle = PuzzleLoader.load(asset("puzzle.json"))
        val solution = PuzzleLoader.loadSolution(asset("solution.json"))
        val seen = HashSet<Map<Pos, Int>>()
        for (seed in 1..6) {
            val out = SuperGenerator.generate(
                template = puzzle, solution = solution, random = Random(seed),
                maxRemove = MapDifficulty.EASY.maxRemove,
                timeBudgetMs = MapDifficulty.EASY.timeBudgetMs,
            )
            assertTrue(
                out.removedCount >= MapDifficulty.EASY.minRemove,
                "seed $seed dug only ${out.removedCount}",
            )
            seen.add(out.givens)
        }
        assertTrue(seen.size > 1, "6 seeds produced a single map")
    }

    @Test
    fun sameSeedIsDeterministic() {
        // Methodology guard: identical RNG streams must give identical maps,
        // proving variety comes from the RNG stream itself.
        val puzzle = PuzzleLoader.load(asset("puzzle.json"))
        val solution = PuzzleLoader.loadSolution(asset("solution.json"))
        fun once() = SuperGenerator.generate(
            template = puzzle, solution = solution, random = Random(42),
            maxRemove = MapDifficulty.EASY.maxRemove,
            timeBudgetMs = MapDifficulty.EASY.timeBudgetMs,
        ).givens
        assertEquals(once(), once())
    }

    @Test
    fun rapidSuccessiveGenerationsAreDistinct() {
        // The reported bug: rapid taps minted identical maps because every
        // shuffle was seeded with the current millisecond. Production now
        // uses Random.Default, so back-to-back generations must differ.
        val puzzle = PuzzleLoader.load(asset("puzzle.json"))
        val solution = PuzzleLoader.loadSolution(asset("solution.json"))
        val seen = HashSet<Map<Pos, Int>>()
        repeat(6) {
            val out = SuperGenerator.generate(
                template = puzzle, solution = solution, random = Random.Default,
                maxRemove = MapDifficulty.EASY.maxRemove,
                timeBudgetMs = MapDifficulty.EASY.timeBudgetMs,
            )
            assertTrue(
                out.removedCount >= MapDifficulty.EASY.minRemove,
                "rapid generation dug only ${out.removedCount}",
            )
            assertEquals(1, SuperGenerator.countSolutions(puzzle.grids, out.givens, 2))
            seen.add(out.givens)
        }
        assertEquals(6, seen.size, "rapid taps produced duplicate maps")
    }

    /** Local-coords template givens, solution and bare grid for a variant. */
    private data class VariantSetup(
        val templateLocal: Map<Pos, Int>,
        val solutionLocal: Map<Pos, Int>,
        val bare: GridDef,
    )

    private fun variantSetup(gridId: String): VariantSetup {
        val puzzle = PuzzleLoader.load(asset("puzzle.json"))
        val solution = PuzzleLoader.loadSolution(asset("solution.json"))
        val standalone = StandaloneLoader.load(asset("standalone.json"))
        val g = puzzle.grids.first { it.id == gridId }
        val templateLocal = standalone.getValue(gridId)
            .mapKeys { (p, _) -> Pos(p.x - g.x, p.y - g.y) }
        val solutionLocal = HashMap<Pos, Int>()
        for (dy in 0 until 9) for (dx in 0 until 9) {
            solutionLocal[Pos(dx, dy)] = solution.getValue(Pos(g.x + dx, g.y + dy))
        }
        return VariantSetup(templateLocal, solutionLocal, localGrid(g).copy(givens = emptyMap()))
    }

    @Test
    fun rapidSprinkleExtrasAreDistinct() {
        // The microsecond worst case of the same-millisecond bug: sprinkle
        // takes no measurable time, so time-seeded shuffles collided on
        // almost every rapid tap. Must now differ tap to tap.
        val (templateLocal, solutionLocal, bare) = variantSetup("futoshiki")
        val seen = HashSet<Map<Pos, Int>>()
        repeat(6) {
            val givens = SuperGenerator.sprinkleExtras(templateLocal, solutionLocal, 8, Random.Default)
            assertEquals(templateLocal.size + 8, givens.size)
            assertEquals(1, SuperGenerator.countSolutions(listOf(bare), givens, 2))
            seen.add(givens)
        }
        assertEquals(6, seen.size, "rapid Easy taps produced duplicate games")
    }

    @Test
    fun rapidDigDownIsDistinct() {
        // The dig-down path (non-futoshiki/kropki variants) under rapid taps.
        val (templateLocal, solutionLocal, bare) = variantSetup("classic")
        val seen = HashSet<Map<Pos, Int>>()
        repeat(3) {
            val dug = SuperGenerator.digDown(
                grids = listOf(bare),
                solution = solutionLocal,
                targetGivens = templateLocal.size + 8,
                random = Random.Default,
                timeBudgetMs = 60_000,
            )
            assertEquals(1, SuperGenerator.countSolutions(listOf(bare), dug.givens, 2))
            assertTrue(dug.givens != templateLocal, "dug game duplicates the Original")
            seen.add(dug.givens)
        }
        assertEquals(3, seen.size, "rapid taps produced duplicate dug games")
    }

    @Test
    fun rapidIdsAreUnique() {
        // The compounding half of the bug: ids derived from the creation
        // millisecond overwrote each other's files. UUIDs must never collide.
        val seen = HashSet<String>()
        repeat(1_000) { seen.add(java.util.UUID.randomUUID().toString()) }
        assertEquals(1_000, seen.size)
    }

    /** Shift a grid's geometry to local 0..8 coordinates (mirrors StandaloneStore.localGrid). */
    private fun localGrid(g: GridDef): GridDef {
        fun shift(p: Pos) = Pos(p.x - g.x, p.y - g.y)
        return g.copy(
            x = 0, y = 0,
            givens = g.givens.mapKeys { (p, _) -> shift(p) },
            inequalities = g.inequalities.map { (a, b) -> shift(a) to shift(b) },
            dots = g.dots.map { (a, b) -> shift(a) to shift(b) },
            cages = g.cages.map { c -> Cage(c.cells.map(::shift).toSet(), c.total) },
            equations = g.equations.map { e ->
                Equation(e.operands.map { op -> op.map(::shift) }, e.total.map(::shift))
            },
            regions = g.regions.map { r -> r.map(::shift).toSet() },
            groups = g.groups.map { r -> r.map(::shift).toSet() },
        )
    }

    @Test
    fun templateKeptVariantsSprinkleNovelUniqueExtras() {
        val puzzle = PuzzleLoader.load(asset("puzzle.json"))
        val solution = PuzzleLoader.loadSolution(asset("solution.json"))
        val standalone = StandaloneLoader.load(asset("standalone.json"))
        for (gridId in listOf("futoshiki", "kropki")) {
            val g = puzzle.grids.first { it.id == gridId }
            val templateLocal = standalone.getValue(gridId)
                .mapKeys { (p, _) -> Pos(p.x - g.x, p.y - g.y) }
            val solutionLocal = HashMap<Pos, Int>()
            for (dy in 0 until 9) for (dx in 0 until 9) {
                solutionLocal[Pos(dx, dy)] = solution.getValue(Pos(g.x + dx, g.y + dy))
            }
            val bare = localGrid(g).copy(givens = emptyMap())
            val easy = SuperGenerator.sprinkleExtras(templateLocal, solutionLocal, 8, Random(11))
            val medium = SuperGenerator.sprinkleExtras(templateLocal, solutionLocal, 4, Random(11))
            // Novelty: strict supersets with distinct counts (never the Original).
            assertEquals(templateLocal.size + 8, easy.size, "$gridId easy")
            assertEquals(templateLocal.size + 4, medium.size, "$gridId medium")
            assertTrue(easy != templateLocal && medium != templateLocal, "$gridId duplicates Original")
            assertTrue(easy != medium, "$gridId easy and medium coincide")
            for ((p, v) in easy) assertEquals(solutionLocal[p], v, "$gridId easy disagrees at $p")
            assertEquals(1, SuperGenerator.countSolutions(listOf(bare), easy, 2), "$gridId easy")
            assertEquals(1, SuperGenerator.countSolutions(listOf(bare), medium, 2), "$gridId medium")
            // Hard digs a few away through the shared generator: novel + unique.
            val pseudo = SuperPuzzle(listOf(bare.copy(givens = templateLocal)), emptyList())
            val hard = SuperGenerator.generate(
                template = pseudo, solution = solutionLocal, random = Random(13),
                maxRemove = 4, timeBudgetMs = 30_000,
            )
            assertTrue(hard.removedCount in 1..4, "$gridId hard removed=${hard.removedCount}")
            assertTrue(hard.givens != templateLocal, "$gridId hard duplicates Original")
            assertEquals(1, SuperGenerator.countSolutions(listOf(bare), hard.givens, 2), "$gridId hard")
        }
    }
}
