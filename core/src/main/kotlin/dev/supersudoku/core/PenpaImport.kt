package dev.supersudoku.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.Inflater
import kotlin.math.abs

/** Failure to import a Penpa+ URL (unsupported layout/genre or corrupt data). */
class PenpaImportError(message: String) : Exception(message)

/**
 * Importer for Penpa+ (`penpa-edit`) puzzle URLs.
 *
 * Ports scripts/decode_penpa.py + analyze/emit logic: URL-safe base64 →
 * raw-deflate → penpa save text → point-space decoding (stride = cols+4,
 * 2-cell margin) → 9×9 rect fitting → per-grid feature assignment
 * (givens, inequalities, dots, killer cages, equations, diagonals,
 * irregular regions, disjoint groups).
 *
 * Scope (v1): canvases containing one or more 9×9 grids; killer cages via
 * the modern `killercages` list; disjoint sets via position-consistent
 * surface colors; addition-style equations via the operand/total surface
 * pattern. Anything else raises [PenpaImportError] with a reason.
 */
object PenpaImport {
    private val json = Json { ignoreUnknownKeys = true }

    // compression substitutions (applied in reverse, like penpa load())
    private val compressSub = listOf(
        "z" to "zZ", "\"qa\"" to "z9", "\"pu_q\"" to "zQ", "\"pu_a\"" to "zA",
        "\"grid\"" to "zG", "\"edit_mode\"" to "zM", "\"surface\"" to "zS",
        "\"line\"" to "zL", "\"lineE\"" to "zE", "\"wall\"" to "zW",
        "\"cage\"" to "zC", "\"number\"" to "zN", "\"symbol\"" to "zY",
        "\"special\"" to "zP", "\"board\"" to "zB",
        "\"command_redo\"" to "zR", "\"command_undo\"" to "zU",
        "\"command_replay\"" to "z8", "\"numberS\"" to "z1",
        "\"freeline\"" to "zF", "\"freelineE\"" to "z2", "\"thermo\"" to "zT",
        "\"arrows\"" to "z3", "\"direction\"" to "zD",
        "\"squareframe\"" to "z0", "\"polygon\"" to "z5",
        "\"deletelineE\"" to "z4", "\"killercages\"" to "z6",
        "\"nobulbthermo\"" to "z7", "\"__a\"" to "z_", "null" to "zO",
    )

    data class Decoded(
        val cols: Int,
        val rows: Int,
        val title: String,
        val question: JsonObject,
        val answer: JsonObject,
    )

    fun decodeUrl(url: String): Decoded {
        val m = Regex("#m=\\w+&p=([A-Za-z0-9+/=_-]+)").find(url)
            ?: throw PenpaImportError("no penpa puzzle payload (p=) in URL")
        var s = m.groupValues[1].replace('-', '+').replace('_', '/')
        s += "=".repeat((4 - s.length % 4) % 4)
        val raw = try {
            Base64.getDecoder().decode(s)
        } catch (e: IllegalArgumentException) {
            throw PenpaImportError("bad base64 payload: ${e.message}")
        }
        val text = try {
            inflateRaw(raw).toString(Charsets.UTF_8)
        } catch (e: Exception) {
            throw PenpaImportError("zlib inflate failed: ${e.message}")
        }
        var t = text
        for ((orig, sub) in compressSub.asReversed()) {
            t = t.split(sub).joinToString(orig)
        }
        val lines = t.split("\n")
        if (lines.isEmpty()) throw PenpaImportError("empty payload")
        val hdr = lines[0].split(",")
        if (hdr[0] != "square") throw PenpaImportError("only square grids supported (got ${hdr[0]})")
        val cols = hdr.getOrNull(1)?.toIntOrNull()
            ?: throw PenpaImportError("bad header: ${lines[0].take(40)}")
        val rows = hdr.getOrNull(2)?.toIntOrNull()
            ?: throw PenpaImportError("bad header: ${lines[0].take(40)}")
        if (cols > 60 || rows > 60) throw PenpaImportError("canvas too large (${cols}x$rows)")
        val title = hdr.getOrNull(15)?.removePrefix("Title: ")?.trim().orEmpty()
        if (lines.size < 15) throw PenpaImportError("truncated payload (${lines.size} lines)")
        val question = try {
            json.parseToJsonElement(lines[3]).jsonObject
        } catch (e: Exception) {
            throw PenpaImportError("question JSON invalid: ${e.message}")
        }
        val answer = try {
            json.parseToJsonElement(lines[14]).jsonObject
        } catch (e: Exception) {
            JsonObject(emptyMap())
        }
        return Decoded(cols, rows, title, question, answer)
    }

    private fun inflateRaw(data: ByteArray): ByteArray {
        val inf = Inflater(true)
        inf.setInput(data)
        val out = ByteArrayOutputStream()
        val buf = ByteArray(65536)
        while (!inf.finished()) {
            val n = inf.inflate(buf)
            if (n == 0) {
                if (inf.needsInput()) break
                throw IllegalStateException("inflate stalled")
            }
            out.write(buf, 0, n)
        }
        inf.end()
        return out.toByteArray()
    }

    // ---------- point-space helpers (stride = cols+4, 2-cell margin) ----------

    private class Space(val cols: Int, val rows: Int) {
        val stride = cols + 4
        private val block = stride * (rows + 4)

        fun cell(k: Int): Pos? {
            val x = k % stride - 2
            val y = k / stride - 2
            return if (x in 0 until cols && y in 0 until rows) Pos(x, y) else null
        }

        fun vertex(k: Int): Pos? {
            val t = k - block
            val x = t % stride - 1
            val y = t / stride - 1
            return if (x in 0..cols && y in 0..rows) Pos(x, y) else null
        }

        /** Returns (orientation H/V, line coordinate, cross coordinate). */
        fun edge(k: Int): Triple<Char, Int, Int>? {
            // h-edge block: id = 2*block + 37*j + i -> line Y=j-1, column X=i-2
            if (k in 2 * block until 3 * block) {
                val t = k - 2 * block
                return Triple('H', t % stride - 2, t / stride - 1)
            }
            // v-edge block: id = 3*block + 37*j + i -> line X=i-1, row Y=j-2
            if (k in 3 * block until 4 * block) {
                val t = k - 3 * block
                return Triple('V', t % stride - 1, t / stride - 2)
            }
            return null
        }

        fun cornerCell(k: Int): Pos? {
            val t = k - 4 * block
            if (t < 0) return null
            return cell(t / 4)
        }
    }

    // ---------- conversion to SuperPuzzle ----------

    data class Imported(val puzzle: SuperPuzzle, val solution: Map<Pos, Int>?)

    internal data class Rect(val x: Int, val y: Int, val score: Int) {
        fun contains(p: Pos) = p.x in x until x + 9 && p.y in y until y + 9
        fun sharedCells(o: Rect): Int {
            val x0 = maxOf(x, o.x)
            val x1 = minOf(x + 9, o.x + 9)
            val y0 = maxOf(y, o.y)
            val y1 = minOf(y + 9, o.y + 9)
            return if (x1 > x0 && y1 > y0) (x1 - x0) * (y1 - y0) else 0
        }
    }

    internal data class Seg(val ax: Int, val ay: Int, val bx: Int, val by: Int, val style: Int)
    private data class Ineq(val a: Pos, val b: Pos, val rel: String) // rel: "lt" => a<b

    fun import(url: String): Imported {
        val d = decodeUrl(url)
        val sp = Space(d.cols, d.rows)
        val q = d.question

        fun obj(name: String): JsonObject = q[name] as? JsonObject ?: JsonObject(emptyMap())
        fun intKey(k: String): Int? = k.toIntOrNull()

        // numbers: givens + labels
        val givens = mutableListOf<Triple<Int, Int, Int>>()
        val labels = mutableListOf<Triple<String, Int, Int>>()
        for ((k, v) in obj("number")) {
            val p = sp.cell(intKey(k) ?: continue) ?: continue
            val arr = v as? JsonArray ?: continue
            if (arr.isEmpty()) continue
            val text = arr[0].jsonPrimitive.content
            when {
                text.length == 1 && text[0].isDigit() -> givens.add(Triple(p.x, p.y, text.toInt()))
                text.trim().any { it.isLetter() } -> labels.add(Triple(text.trim(), p.x, p.y))
            }
        }

        // symbols: inequalities + kropki dots
        val ineqs = mutableListOf<Ineq>()
        val dots = mutableListOf<Pair<Pos, Pos>>()
        for ((k, v) in obj("symbol")) {
            val arr = v as? JsonArray ?: continue
            if (arr.size < 3) continue
            val rot = arr[0].jsonPrimitive.content.toIntOrNull() ?: continue
            val name = arr[1].jsonPrimitive.content
            val e = sp.edge(intKey(k) ?: continue) ?: continue
            val (ori, x, y) = e
            if (name == "inequality") {
                if (ori == 'V') {
                    val rel = when (rot) {
                        5 -> "lt"
                        7 -> "gt"
                        else -> throw PenpaImportError("bad inequality rotation $rot")
                    }
                    if (x - 1 >= 0 && x < d.cols && y in 0 until d.rows) {
                        ineqs.add(Ineq(Pos(x - 1, y), Pos(x, y), rel))
                    }
                } else {
                    val rel = when (rot) {
                        6 -> "lt"
                        8 -> "gt"
                        else -> throw PenpaImportError("bad inequality rotation $rot")
                    }
                    if (y - 1 >= 0 && y < d.rows && x in 0 until d.cols) {
                        ineqs.add(Ineq(Pos(x, y - 1), Pos(x, y), rel))
                    }
                }
            } else if (name == "circle_SS") {
                val cells = if (ori == 'H') listOf(Pos(x, y - 1), Pos(x, y))
                else listOf(Pos(x - 1, y), Pos(x, y))
                if (cells.all { it.x in 0 until d.cols && it.y in 0 until d.rows }) {
                    dots.add(cells[0] to cells[1])
                }
            }
        }

        // surfaces: cell -> color id
        val surfaces = mutableMapOf<Pos, Int>()
        for ((k, v) in obj("surface")) {
            val p = sp.cell(intKey(k) ?: continue) ?: continue
            surfaces[p] = v.toString().toIntOrNull() ?: continue
        }

        // borders: vertex-pair segments with styles; diagonals separated
        val borders = mutableListOf<Seg>()
        val diags = mutableListOf<Seg>()
        for ((k, v) in obj("lineE")) {
            val parts = k.split(",")
            if (parts.size != 2) continue
            val a = sp.vertex(parts[0].toIntOrNull() ?: continue) ?: continue
            val b = sp.vertex(parts[1].toIntOrNull() ?: continue) ?: continue
            val style = v.toString().toIntOrNull() ?: continue
            val dx = abs(a.x - b.x)
            val dy = abs(a.y - b.y)
            if (dx == 1 && dy == 1) diags.add(Seg(a.x, a.y, b.x, b.y, style))
            else if ((dx == 1 && dy == 0) || (dx == 0 && dy == 1)) {
                borders.add(Seg(a.x, a.y, b.x, b.y, style))
            }
        }

        // killer cages: modern list + corner totals
        val cageLists = (q["killercages"] as? JsonArray)
            ?.mapNotNull { it as? JsonArray }
            ?.map { arr -> arr.mapNotNull { sp.cell(it.toString().toIntOrNull() ?: -1) } }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()
        val hasCageBorders = (q["cage"] as? JsonObject)?.isNotEmpty() == true
        if (cageLists.isEmpty() && hasCageBorders) {
            throw PenpaImportError(
                "killer cages stored as borders only (old penpa); " +
                    "re-save with a current penpa-edit to get the cage list",
            )
        }
        val totals = mutableMapOf<Pos, Int>()
        for ((k, v) in obj("numberS")) {
            val p = sp.cornerCell(intKey(k) ?: continue) ?: continue
            val arr = v as? JsonArray ?: continue
            if (arr.isEmpty()) continue
            totals[p] = arr[0].jsonPrimitive.content.trim().toIntOrNull() ?: continue
        }

        // ---- fit 9x9 rects from internal box lines (style 2) ----
        val hSet = borders.filter { it.style == 2 && it.ay == it.by }
            .map { it.ay to minOf(it.ax, it.bx) }.toSet()
        val vSet = borders.filter { it.style == 2 && it.ax == it.bx }
            .map { it.ax to minOf(it.ay, it.by) }.toSet()
        val candidates = mutableListOf<Rect>()
        for (y0 in 0..d.rows - 9) for (x0 in 0..d.cols - 9) {
            var score = 0
            for (x in x0 until x0 + 9) {
                if ((y0 + 3 to x) in hSet) score++
                if ((y0 + 6 to x) in hSet) score++
            }
            for (y in y0 until y0 + 9) {
                if ((x0 + 3 to y) in vSet) score++
                if ((x0 + 6 to y) in vSet) score++
            }
            if (score >= 18) candidates.add(Rect(x0, y0, score))
        }
        if (candidates.isEmpty()) throw PenpaImportError("no 9x9 grid with box lines found")
        val rects = mutableListOf<Rect>()
        for (c in candidates.sortedByDescending { it.score }) {
            if (rects.none { it.sharedCells(c) > 9 }) rects.add(c)
        }

        // ---- per-rect content (pass 1: collect + detect variant) ----
        data class Built(
            val rect: Rect,
            val givens: Map<Pos, Int>,
            val ineqs: List<Ineq>,
            val dots: List<Pair<Pos, Pos>>,
            val cages: List<Cage>,
            val equations: List<Equation>,
            val groups: List<Set<Pos>>,
            val regions: List<Set<Pos>>,
            val hasDiags: Boolean,
            val variant: Variant,
        )
        // cages are owned by the single rect containing all their cells
        val cageOwner = mutableMapOf<Int, Rect>()
        cageLists.forEachIndexed { ci, cl ->
            cageOwner[ci] = rects.firstOrNull { r -> cl.all { r.contains(it) } }
                ?: throw PenpaImportError("a killer cage crosses grid boundaries")
        }
        val built = rects.sortedWith(compareBy({ it.y }, { it.x })).map { r ->
            val rg = givens.filter { (x, y, _) -> r.contains(Pos(x, y)) }
                .associate { (x, y, v) -> Pos(x, y) to v }
            val ri = ineqs.filter { r.contains(it.a) && r.contains(it.b) }
            val rd = dots.filter { (a, b) -> r.contains(a) && r.contains(b) }
            val rcages = cageLists.withIndex()
                .filter { (ci, _) -> cageOwner[ci] == r }
                .map { (_, cl) ->
                    val set = cl.toSet()
                    val tot = totals.entries.firstOrNull { (pp, _) -> pp in set }?.value
                        ?: throw PenpaImportError("killer cage without a total near ${cl.first()}")
                    Cage(set, tot)
                }
            val surfHere = surfaces.filter { (pp, _) -> r.contains(pp) }
            val equations = parseEquations(surfHere)
            val usedSurf = equations.flatMap { e -> e.operands.flatten() + e.total }.toSet()
            val leftover = surfHere.filter { (pp, _) -> pp !in usedSurf }
            val groups = deriveDisjoint(r, leftover)
            val regions = traceRegions(r, borders)
            val standard = standardBoxes(r).map { it.toSet() }.toSet()
            val irregular = regions != null && regions.toSet() != standard
            val hasDiags = hasDiagonals(r, diags)
            val variant = when {
                irregular -> Variant.IRREGULAR
                groups != null -> Variant.DISJOINT
                rcages.isNotEmpty() -> Variant.KILLER
                equations.isNotEmpty() -> Variant.ADDITION
                ri.isNotEmpty() -> Variant.FUTOSHIKI
                rd.isNotEmpty() -> Variant.KROPKI
                hasDiags -> Variant.SUDOKU_X
                else -> Variant.CLASSIC
            }
            Built(r, rg, ri, rd, rcages, equations, groups ?: emptyList(),
                if (irregular) regions!! else emptyList(), hasDiags, variant)
        }
        // pass 2: re-home shared inequalities/dots to matching-variant rects
        // (e.g. overlap dots belong to the Consecutive grid, not its neighbours)
        fun rehome(
            items: List<Pair<Set<Rect>, Any>>,
            want: Variant,
        ): Map<Rect, List<Any>> {
            val out = mutableMapOf<Rect, MutableList<Any>>()
            for ((rectsOf, item) in items) {
                val keep = rectsOf.filter { r ->
                    built.first { it.rect == r }.variant == want
                }.ifEmpty { rectsOf.toList() }
                for (r in keep) out.getOrPut(r) { mutableListOf() }.add(item)
            }
            return out
        }
        val ineqHome = rehome(
            ineqs.map { e ->
                built.map { it.rect }.filter { r -> r.contains(e.a) && r.contains(e.b) }.toSet() to (e as Any)
            },
            Variant.FUTOSHIKI,
        )
        val dotHome = rehome(
            dots.map { (a, b) ->
                built.map { it.rect }.filter { r -> r.contains(a) && r.contains(b) }.toSet() to (Pair(a, b) as Any)
            },
            Variant.KROPKI,
        )
        val grids = mutableListOf<GridDef>()
        built.forEachIndexed { gi, b ->
            @Suppress("UNCHECKED_CAST")
            val fi = (ineqHome[b.rect] ?: emptyList()).map { it as Ineq }
            @Suppress("UNCHECKED_CAST")
            val fd = (dotHome[b.rect] ?: emptyList()).map { it as Pair<Pos, Pos> }
            grids.add(
                GridDef(
                    id = "g$gi", name = "Grid ${gi + 1}", variant = b.variant,
                    x = b.rect.x, y = b.rect.y, givens = b.givens,
                    inequalities = fi.map { (a, bb, rel) -> if (rel == "lt") a to bb else bb to a },
                    dots = fd,
                    cages = b.cages,
                    equations = b.equations,
                    diagonals = b.hasDiags,
                    regions = b.regions,
                    groups = b.groups,
                ),
            )
        }
        if (grids.isEmpty()) throw PenpaImportError("no playable grid detected")

        // solution from the answer section, if it fills every grid cell
        var solution: Map<Pos, Int>? = null
        val ans = d.answer["number"] as? JsonObject
        if (ans != null && ans.isNotEmpty()) {
            val sol = mutableMapOf<Pos, Int>()
            for ((k, v) in ans) {
                val p = sp.cell(intKey(k) ?: continue) ?: continue
                val arr = v as? JsonArray ?: continue
                if (arr.isEmpty()) continue
                val t = arr[0].jsonPrimitive.content
                if (t.length == 1 && t[0].isDigit()) sol[p] = t.toInt()
            }
            val allCells = grids.flatMap { it.cells() }.toSet()
            if (allCells.isNotEmpty() && allCells.all { it in sol }) {
                solution = sol
            }
        }

        val title = d.title.ifBlank { "Imported puzzle" }
        if (grids.size == 1) {
            grids[0] = grids[0].copy(name = title)
        }
        return Imported(SuperPuzzle(grids, labels.map { (t, x, y) -> BoardLabel(t, x, y) }), solution)
    }

    /** Diagonals present iff both corner-to-corner lines are (nearly) fully drawn. */
    private fun hasDiagonals(r: Rect, diags: List<Seg>): Boolean {
        fun lineCover(pts: List<Pair<Int, Int>>): Boolean {
            var have = 0
            for (i in 0 until pts.size - 1) {
                val (x1, y1) = pts[i]
                val (x2, y2) = pts[i + 1]
                if (diags.any { s ->
                    setOf(s.ax to s.ay, s.bx to s.by) == setOf(x1 to y1, x2 to y2)
                }) have++
            }
            return have >= 8
        }
        val main = (0..9).map { (r.x + it) to (r.y + it) }
        val anti = (0..9).map { (r.x + 9 - it) to (r.y + it) }
        return lineCover(main) && lineCover(anti)
    }

    /**
     * Equation parse: edge-connected surface components; the bottom row's
     * color is the total, other rows are operands (adjacent runs join into
     * multi-digit operands). Touching equations (e.g. sharing an edge) are
     * split apart by trying edge cuts whose sides both parse cleanly.
     */
    fun parseEquations(surf: Map<Pos, Int>): List<Equation> {
        if (surf.isEmpty()) return emptyList()
        val remaining = surf.keys.toMutableSet()
        val out = mutableListOf<Equation>()
        while (remaining.isNotEmpty()) {
            val comp = mutableSetOf<Pos>()
            val stack = ArrayDeque<Pos>()
            stack.add(remaining.first())
            while (stack.isNotEmpty()) {
                val p = stack.removeLast()
                if (!comp.add(p)) continue
                remaining.remove(p)
                for (q in listOf(Pos(p.x + 1, p.y), Pos(p.x - 1, p.y), Pos(p.x, p.y + 1), Pos(p.x, p.y - 1))) {
                    if (q in remaining) stack.add(q)
                }
            }
            out.addAll(splitComponent(comp, surf))
        }
        return out
    }

    /** Parse one connected set, splitting at bridge edges when both sides parse. */
    private fun splitComponent(comp: Set<Pos>, surf: Map<Pos, Int>): List<Equation> {
        parseComponent(comp, surf)?.let { return listOf(it) }
        // touching equations can share 2+ edges; try single cuts, then pairs.
        // Depth-1 only (parts must parse directly): avoids combinatorial blowup
        // and is exact for pairwise touches.
        val edges = mutableListOf<Pair<Pos, Pos>>()
        for (p in comp) {
            for (q in listOf(Pos(p.x + 1, p.y), Pos(p.x, p.y + 1))) {
                if (q in comp) edges.add(p to q)
            }
        }
        for (e in edges) {
            tryCut(comp, surf, setOf(e))?.let { return it }
        }
        for (i in edges.indices) for (j in i + 1 until edges.size) {
            tryCut(comp, surf, setOf(edges[i], edges[j]))?.let { return it }
        }
        return emptyList()
    }

    /**
     * Remove [cut] edges; if every resulting part parses directly as an
     * equation, return all of them.
     */
    private fun tryCut(
        comp: Set<Pos>,
        surf: Map<Pos, Int>,
        cut: Set<Pair<Pos, Pos>>,
    ): List<Equation>? {
        fun norm(a: Pos, b: Pos) =
            if (a.x < b.x || (a.x == b.x && a.y < b.y)) a to b else b to a
        val cutN = cut.map { (a, b) -> norm(a, b) }.toSet()
        val seen = mutableSetOf<Pos>()
        val parts = mutableListOf<Set<Pos>>()
        for (start in comp) {
            if (start in seen) continue
            val part = mutableSetOf<Pos>()
            val stack = ArrayDeque<Pos>()
            stack.add(start)
            while (stack.isNotEmpty()) {
                val p = stack.removeLast()
                if (!part.add(p)) continue
                seen.add(p)
                for (q in listOf(Pos(p.x + 1, p.y), Pos(p.x - 1, p.y), Pos(p.x, p.y + 1), Pos(p.x, p.y - 1))) {
                    if (q in comp && norm(p, q) !in cutN) stack.add(q)
                }
            }
            parts.add(part)
        }
        if (parts.size < 2) return null
        val out = mutableListOf<Equation>()
        for (part in parts) {
            out.add(parseComponent(part, surf) ?: return null)
        }
        return out
    }

    private fun parseComponent(comp: Set<Pos>, surf: Map<Pos, Int>): Equation? {
        val maxY = comp.maxOf { it.y }
        val bottomColor = comp.filter { it.y == maxY }.map { surf.getValue(it) }.toSet()
        val otherColors = comp.filter { it.y != maxY }.map { surf.getValue(it) }.toSet()
        if (bottomColor.size != 1 || otherColors.size != 1) return null
        if (bottomColor == otherColors) return null
        val total = comp.filter { it.y == maxY }.sortedWith(compareBy({ it.y }, { it.x }))
        val operands = mutableListOf<List<Pos>>()
        for (y in comp.filter { it.y != maxY }.map { it.y }.toSortedSet()) {
            val xs = comp.filter { it.y == y }.map { it.x }.sorted()
            var run = mutableListOf<Pos>()
            for (x in xs) {
                if (run.isNotEmpty() && x != run.last().x + 1) {
                    operands.add(run)
                    run = mutableListOf()
                }
                run.add(Pos(x, y))
            }
            if (run.isNotEmpty()) operands.add(run)
        }
        if (operands.isEmpty() || total.isEmpty()) return null
        return Equation(operands, total)
    }

    /** Disjoint groups if leftover surfaces validate (each color ×9, position-consistent). */
    internal fun deriveDisjoint(r: Rect, leftover: Map<Pos, Int>): List<Set<Pos>>? {
        if (leftover.isEmpty()) return null
        if (leftover.keys.any { it.x !in r.x until r.x + 9 || it.y !in r.y until r.y + 9 }) return null
        val byColor = leftover.entries.groupBy({ it.value }, { it.key })
        if (byColor.values.any { it.size != 9 }) return null
        for ((_, cells) in byColor) {
            val positions = cells.map { ((it.x - r.x) % 3) to ((it.y - r.y) % 3) }.toSet()
            if (positions.size != 1) return null
        }
        return (0 until 3).flatMap { oy ->
            (0 until 3).map { ox ->
                (0 until 3).flatMap { by ->
                    (0 until 3).map { bx -> Pos(r.x + bx * 3 + ox, r.y + by * 3 + oy) }
                }.toSet()
            }
        }
    }

    private fun standardBoxes(r: Rect): List<List<Pos>> {
        return (0 until 3).flatMap { by ->
            (0 until 3).map { bx ->
                (0 until 3).flatMap { dy ->
                    (0 until 3).map { dx -> Pos(r.x + bx * 3 + dx, r.y + by * 3 + dy) }
                }
            }
        }
    }

    /**
     * Trace jigsaw regions from internal style-2 borders. Returns null unless
     * the result is exactly 9 regions of 9 cells (else the caller falls back
     * to standard boxes).
     */
    internal fun traceRegions(r: Rect, borders: List<Seg>): List<Set<Pos>>? {
        val hSet = borders.filter { it.style == 2 && it.ay == it.by }
            .map { it.ay to minOf(it.ax, it.bx) }.toSet()
        val vSet = borders.filter { it.style == 2 && it.ax == it.bx }
            .map { it.ax to minOf(it.ay, it.by) }.toSet()
        val parent = HashMap<Pos, Pos>()
        for (dx in 0 until 9) for (dy in 0 until 9) {
            parent[Pos(r.x + dx, r.y + dy)] = Pos(r.x + dx, r.y + dy)
        }
        fun find(p: Pos): Pos {
            var c = p
            while (parent.getValue(c) != c) {
                parent[c] = parent.getValue(parent.getValue(c))
                c = parent.getValue(c)
            }
            return c
        }
        for (dx in 0 until 9) for (dy in 0 until 9) {
            val p = Pos(r.x + dx, r.y + dy)
            if (dx + 1 < 9 && (r.x + dx + 1 to r.y + dy) !in vSet) {
                val q = Pos(p.x + 1, p.y)
                parent[find(p)] = find(q)
            }
            if (dy + 1 < 9 && (r.y + dy + 1 to r.x + dx) !in hSet) {
                val q = Pos(p.x, p.y + 1)
                parent[find(p)] = find(q)
            }
        }
        val regions = parent.keys.groupBy { find(it) }.values
            .map { it.sortedWith(compareBy({ p: Pos -> p.y }, { p: Pos -> p.x })).toSet() }
        if (regions.size != 9 || regions.any { it.size != 9 }) return null
        return regions.sortedWith(
            compareBy(
                { it.minOf { p -> p.y } },
                { it.minOf { p -> p.x } },
            ),
        )
    }
}
