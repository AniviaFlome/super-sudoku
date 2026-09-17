#!/usr/bin/env python3
"""Step 3: emit puzzle.json for the app from the decoded Penpa+ data.

Grid layout (canvas 33x33, verified from border runs + element clusters):
  classic    cols 12-20 rows 0-8
  futoshiki  cols  6-14 rows 6-14
  addition   cols 18-26 rows 6-14
  killer     cols  0-8  rows 12-20
  kropki     cols 24-32 rows 12-20
  sudoku_x   cols  6-14 rows 18-26
  irregular  cols 18-26 rows 18-26
  disjoint   cols 12-20 rows 24-32
Ring overlaps (3x3 shared corners, samurai-style): classic-futoshiki,
classic-addition, futoshiki-killer, killer-sudoku_x, sudoku_x-disjoint,
disjoint-irregular, irregular-kropki, kropki-addition.

Addition equations (from surface colors: 11=operand, 10=total):
  H1: (19,11)(20,11) + (19,12)(20,12) = (19,13)(20,13)   "3?+??=8?"
  H2: (24,10)(25,10) + (24,11)(25,11) = (24,12)(25,12)   "??+7?=??"
  V1: (22,6)+(22,7) = (21,8)(22,8)                     REVIEW: 2-cell total
  V2: (26,6)+(26,7) = (26,8)                           "=7"
  V3: (23,11)+(23,12)+(23,13) = (23,14)                REVIEW: triple sum
"""
import json
from collections import defaultdict
from pathlib import Path

HERE = Path(__file__).resolve().parent.parent
OUT = HERE / "app" / "src" / "main" / "assets" / "puzzle.json"

GRIDS = {
    "classic": (12, 0), "futoshiki": (6, 6), "addition": (18, 6),
    "killer": (0, 12), "kropki": (24, 12), "sudoku_x": (6, 18),
    "irregular": (18, 18), "disjoint": (12, 24),
}
NAMES = {
    # Display names use carykh's originals from the puzzle image.
    "classic": "Normal Sudoku", "futoshiki": ">Sudoku<", "addition": "Addition Sudoku",
    "killer": "Killer Sudoku", "kropki": "Consecutive Sudoku", "sudoku_x": "Sudoku X",
    "irregular": "Strange Boxes", "disjoint": "Offset Sudoku",
}
VARIANTS = {
    "classic": "classic", "futoshiki": "futoshiki", "addition": "addition",
    "killer": "killer", "kropki": "kropki", "sudoku_x": "sudoku_x",
    "irregular": "irregular", "disjoint": "disjoint",
}


def in_grid(gid, x, y):
    x0, y0 = GRIDS[gid]
    return x0 <= x < x0 + 9 and y0 <= y < y0 + 9


def grids_of(x, y):
    return [g for g in GRIDS if in_grid(g, x, y)]


def main():
    raw = json.loads((HERE / "scripts" / "decode_raw.json").read_text())
    report = []

    def check(cond, msg):
        report.append(("OK " if cond else "FAIL ") + msg)
        if not cond:
            raise AssertionError(msg)

    # ---------- givens ----------
    givens = [(x, y, v) for x, y, v in raw["givens"]]
    # TRANSCRIPTION CORRECTION (verified against carykh's original image):
    # the >Sudoku< (futoshiki) grid has a given 3 at canvas (8,7) that
    # TheSudokuer's Penpa+ transcription missed. Without it the puzzle admits
    # a second solution (2<->3 swap at (6,7),(8,7),(6,9),(8,9)); with it the
    # solution is unique (verified by :core:solveSuper => UNIQUE).
    givens.append((8, 7, 3))
    by_grid = defaultdict(list)
    unassigned = []
    for x, y, v in givens:
        gs = grids_of(x, y)
        if len(gs) == 1:
            by_grid[gs[0]].append([x, y, v])
        elif len(gs) == 2:
            for g in gs:
                by_grid[g].append([x, y, v])
        else:
            unassigned.append([x, y, v])
    check(unassigned == [], f"no unassigned givens, got {unassigned}")
    check(len(givens) == 125, f"125 givens (124 transcribed + 1 corrected), got {len(givens)}")
    check(all(len(grids_of(x, y)) >= 1 for x, y, v in givens),
          "every given is in >=1 grid")

    # ---------- inequalities -> futoshiki ----------
    ineq = []
    for a, b, rel in raw["ineq"]:
        (ax, ay), (bx, by) = tuple(a), tuple(b)
        check(in_grid("futoshiki", ax, ay) and in_grid("futoshiki", bx, by),
              f"ineq {a}{b} inside futoshiki")
        check(abs(ax - bx) + abs(ay - by) == 1, f"ineq {a}{b} adjacent")
        lo, hi = (a, b) if rel == "lt" else (b, a)
        ineq.append({"lo": list(lo), "hi": list(hi)})
    check(len(ineq) == 84, f"84 inequalities, got {len(ineq)}")

    # ---------- dots -> kropki ----------
    dots = []
    for a, b in raw["dots"]:
        (ax, ay), (bx, by) = tuple(a), tuple(b)
        check(in_grid("kropki", ax, ay) or in_grid("kropki", bx, by),
              f"dot {a}{b} touches kropki")
        check(abs(ax - bx) + abs(ay - by) == 1, f"dot {a}{b} adjacent")
        dots.append([list(a), list(b)])
    check(len(dots) == 33, f"33 dots, got {len(dots)}")

    # ---------- killer cages + totals ----------
    # killercages + numberS live in the URL; parse via helper (scripts dir on sys.path)
    import sys
    sys.path.insert(0, str(HERE / "scripts"))
    import scripts_decode_helper as H  # noqa
    cages_raw, totals_raw = H.killer_data()
    cages = []
    for cells in cages_raw:
        pts = sorted((c % 37 - 2, c // 37 - 2) for c in cells)
        check(all(in_grid("killer", x, y) for x, y in pts), f"cage {pts} in killer")
        check(len(pts) == len(set(pts)), "cage cells unique")
        cages.append({"cells": [list(p) for p in pts], "total": None})
    check(len(cages) == 21, f"21 cages, got {len(cages)}")
    used = set()
    for (cx, cy), total in totals_raw:
        match = [i for i, c in enumerate(cages) if [cx, cy] in c["cells"]]
        check(len(match) == 1, f"total {total} at {(cx,cy)} in exactly 1 cage")
        i = match[0]
        check(cages[i]["total"] is None, f"cage {i} single total")
        check((cx, cy) not in used, "total cell unique")
        used.add((cx, cy))
        cages[i]["total"] = total
    check(all(c["total"] is not None for c in cages), "every cage has a total")

    # ---------- addition equations (literal, see module docstring) ----------
    E = lambda ops, tot: {"operands": [[[x, y] for x, y in op] for op in ops],
                           "total": [[x, y] for x, y in tot]}
    equations = [
        E([[(19, 11), (20, 11)], [(19, 12), (20, 12)]], [(19, 13), (20, 13)]),
        E([[(24, 10), (25, 10)], [(24, 11), (25, 11)]], [(24, 12), (25, 12)]),
        E([[(22, 6)], [(22, 7)]], [(21, 8), (22, 8)]),
        E([[(26, 6)], [(26, 7)]], [(26, 8)]),
        E([[(23, 11)], [(23, 12)], [(23, 13)]], [(23, 14)]),
    ]
    # every surfaced addition cell in exactly one equation; every equation cell surfaced
    surf_add = {(int(k) % 37 - 2, int(k) // 37 - 2)
                for k in raw["surfaces"] if in_grid("addition", int(k) % 37 - 2, int(k) // 37 - 2)}
    eq_cells = set()
    for e in equations:
        for op in e["operands"]:
            for c in op:
                check(tuple(c) not in eq_cells, f"equation cell {c} unique")
                eq_cells.add(tuple(c))
        for c in e["total"]:
            check(tuple(c) not in eq_cells, f"equation cell {c} unique")
            eq_cells.add(tuple(c))
    check(eq_cells == surf_add, f"equation cells == surfaced addition cells "
                                f"(eq={len(eq_cells)} surf={len(surf_add)})")

    # ---------- irregular regions from box borders ----------
    # NOTE: in the overlap strips (kropki: x 24-26/y 18-20, disjoint: x 18-20/y 24-26)
    # a drawn line doubles as a boundary for BOTH grids, so every style-2
    # segment between two irregular-internal cells is a region boundary.
    hset = {(x, y) for x, y, s in raw["hlines"] if s == 2}
    vset = {(x, y) for x, y, s in raw["vlines"] if s == 2}
    x0, y0 = GRIDS["irregular"]
    parent = {(x, y): (x, y) for x in range(x0, x0 + 9) for y in range(y0, y0 + 9)}

    def find(p):
        while parent[p] != p:
            parent[p] = parent[parent[p]]
            p = parent[p]
        return p

    def union(a, b):
        parent[find(a)] = find(b)

    for x in range(x0, x0 + 9):
        for y in range(y0, y0 + 9):
            if x + 1 < x0 + 9 and (x + 1, y) not in vset:
                union((x, y), (x + 1, y))
            if y + 1 < y0 + 9 and (x, y + 1) not in hset:
                union((x, y), (x, y + 1))
    regions = defaultdict(list)
    for p in parent:
        regions[find(p)].append(list(p))
    regions = sorted((sorted(r) for r in regions.values()))
    check(len(regions) == 9 and all(len(r) == 9 for r in regions),
          f"9 irregular regions x 9 cells, got {[len(r) for r in regions]}")

    # ---------- disjoint groups (geometric; transcription colors as check) ----------
    groups = defaultdict(list)
    dx0, dy0 = GRIDS["disjoint"]
    for x in range(dx0, dx0 + 9):
        for y in range(dy0, dy0 + 9):
            groups[((x - dx0) % 3, (y - dy0) % 3)].append([x, y])
    check(len(groups) == 9 and all(len(v) == 9 for v in groups.values()),
          "9 disjoint groups x 9 cells")
    surf = {int(k): v for k, v in raw["surfaces"].items()}
    for k, color in surf.items():
        x, y = k % 37 - 2, k // 37 - 2
        if in_grid("disjoint", x, y):
            key = ((x - dx0) % 3, (y - dy0) % 3)
            others = [kk for kk, cc in surf.items()
                      if cc == color and in_grid("disjoint", kk % 37 - 2, kk // 37 - 2)]
            check(all((((kk % 37 - 2) - dx0) % 3, ((kk // 37 - 2) - dy0) % 3) == key
                      for kk in others),
                  f"transcription color {color} position-consistent")

    # ---------- sudoku_x diagonals ----------
    xg, yg = GRIDS["sudoku_x"]
    diag_pts = set()
    for a, b, s in raw["diags"]:
        (ax, ay), (bx, by) = tuple(a), tuple(b)
        diag_pts.add((ax, ay))
        diag_pts.add((bx, by))
    main_d = {(xg + i, yg + i) for i in range(10)}
    anti_d = {(xg + 9 - i, yg + i) for i in range(10)}
    check(diag_pts == main_d | anti_d, "X diagonals are the grid corner-to-corner lines")

    # ---------- labels ----------
    labels = []
    for text, pts in raw["labels"].items():
        for x, y in pts:
            labels.append({"text": text, "x": x, "y": y})

    # ---------- assemble ----------
    grids = []
    for gid, (x0, y0) in GRIDS.items():
        g = {"id": gid, "name": NAMES[gid], "variant": VARIANTS[gid],
             "x": x0, "y": y0, "size": 9,
             "givens": sorted(by_grid[gid])}
        if gid == "futoshiki":
            g["inequalities"] = ineq
        if gid == "kropki":
            g["dots"] = dots
        if gid == "killer":
            g["cages"] = cages
        if gid == "addition":
            g["equations"] = equations
        if gid == "sudoku_x":
            g["diagonals"] = True
        if gid == "irregular":
            g["regions"] = regions
        if gid == "disjoint":
            g["groups"] = [sorted(groups[k]) for k in sorted(groups)]
        grids.append(g)

    puzzle = {
        "meta": {"name": "Carykh's Super Sudoku", "boardCols": 33, "boardRows": 33,
                 "source": "penpa-edit transcription by u/TheSudokuer (tinyurl.com/23v5gqu8) "
                           "+ 1 image-verified correction: given 3 at (8,7)",
                 "notes": "killer cages allow repeated digits; addition reads row/col digit "
                          "strings as multi-digit numbers (blue operands above, red totals beneath)"},
        "grids": grids,
        "labels": labels,
        "unassigned": unassigned,
    }
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(puzzle, indent=1))
    print(f"wrote {OUT} ({OUT.stat().st_size} bytes)")
    print("\n".join(report))


if __name__ == "__main__":
    main()
