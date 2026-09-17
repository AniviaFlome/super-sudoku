#!/usr/bin/env python3
"""Step 2: fit 9x9 grid rects and assign every decoded element to a rect."""
import json
from collections import Counter, defaultdict
from pathlib import Path

HERE = Path(__file__).resolve().parent.parent
raw = json.loads((HERE / "scripts" / "decode_raw.json").read_text())

# ---- merged border runs (style 2 only = internal box lines, plus style 80) ----
H2 = defaultdict(list)   # y -> [x starts]
V2 = defaultdict(list)   # x -> [y starts]
H80, V80 = [], []
for x, y, s in raw["hlines"]:
    if s == 2:
        H2[y].append(x)
    else:
        H80.append((x, y))
for x, y, s in raw["vlines"]:
    if s == 2:
        V2[x].append(y)
    else:
        V80.append((x, y))


def merge_runs(segs):
    """segs: (fixed, pos) -> merged (fixed, start, end) runs"""
    by = defaultdict(list)
    for f, p in segs:
        by[f].append(p)
    out = []
    for f, ps in sorted(by.items()):
        ps = sorted(ps)
        s = e = ps[0]
        for p in ps[1:]:
            if p == e + 1:
                e = p
            else:
                out.append((f, s, e))
                s = e = p
        out.append((f, s, e))
    return out


print("== style-2 horizontal lines (y: x0..x1, segments->span) ==")
hpairs = [(y, x) for y, xs in H2.items() for x in xs]
for y, a, b in merge_runs(hpairs):
    print(f"  y={y:2d}  x={a:2d}..{b:2d}  (span {b - a + 1})")
print("== style-2 vertical lines (x: y0..y1) ==")
vpairs = [(x, y) for x, ys in V2.items() for y in ys]
for x, a, b in merge_runs(vpairs):
    print(f"  x={x:2d}  y={a:2d}..{b:2d}  (span {b - a + 1})")
print("style-80 straight:", H80, V80)
print("style-80 diagonals:", raw["diags"])

# ---- element density by rect candidate ----
print("\n== label positions ==")
for k, v in sorted(raw["labels"].items()):
    print(f"  {k}: {v}")

print("\n== surfaces by row band ==")
bands = Counter()
for k in raw["surfaces"]:
    x, y = int(k) % 37 - 2, int(k) // 37 - 2
    bands[(y // 9 * 9, x // 9 * 9)] += 1
print("  (rowband9, colband9): count ->", dict(sorted(bands.items())))

print("\n== kropki dots bounding ==")
dx = [c[0] for d in raw["dots"] for c in d]
dy = [c[1] for d in raw["dots"] for c in d]
print(f"  x {min(dx)}..{max(dx)}  y {min(dy)}..{max(dy)}")

print("\n== inequality bounding ==")
ix = [c[0] for cells, _ in raw["ineq"] for c in cells]
iy = [c[1] for cells, _ in raw["ineq"] for c in cells]
print(f"  x {min(ix)}..{max(ix)}  y {min(iy)}..{max(iy)}")

print("\n== givens bounding ==")
gx = [x for x, y, v in raw["givens"]]
gy = [y for x, y, v in raw["givens"]]
print(f"  x {min(gx)}..{max(gx)}  y {min(gy)}..{max(gy)}")
