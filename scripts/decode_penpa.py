#!/usr/bin/env python3
"""Decode carykh's Super Sudoku from the Penpa+ transcription URL.

Reads docs/penpa-url.txt (full penpa-edit #m=solve URL saved from
https://tinyurl.com/23v5gqu8), decodes the zlib+base64 payload using the
documented penpa-edit save format, and emits puzzle.json for the app.

Coordinate systems (from penpa-edit docs/js/class_square.js):
  - canvas is 33x33 cells; point ids use stride 37 with 2-cell margin
  - cell center point id: k = 37*(y+2) + (x+2), x,y in [0,32]
  - vertices:      k = 1369 + 37*(Y+1) + (X+1) -> canvas intersection (X,Y)
  - horiz edges:   k = 2777 + 37*Y + X         -> grid line Y, column X
                   (inequality between cells (X,Y-1) and (X,Y))
  - vert edges:    k = 4182 + 37*Y + X         -> grid line X, row Y
                   (inequality between cells (X-1,Y) and (X,Y))
  - inner corners (killer cages): k = 5476 + 4*cellid + c,
    cellid = 37*(y+2)+(x+2), c in {0:NW,1:NE,2:SW,3:SE}
  - inequality rotations: V-edge 5=left<right, 7=left>right;
    H-edge 6=upper<lower, 8=upper>lower
"""
import base64
import json
import re
import sys
import zlib
from collections import Counter, defaultdict
from pathlib import Path

HERE = Path(__file__).resolve().parent.parent
NX = NY = 33
SID = 37  # stride

COMPRESS_SUB = [
    ("z", "zZ"), ('"qa"', "z9"), ('"pu_q"', "zQ"), ('"pu_a"', "zA"),
    ('"grid"', "zG"), ('"edit_mode"', "zM"), ('"surface"', "zS"),
    ('"line"', "zL"), ('"lineE"', "zE"), ('"wall"', "zW"),
    ('"cage"', "zC"), ('"number"', "zN"), ('"symbol"', "zY"),
    ('"special"', "zP"), ('"board"', "zB"),
    ('"command_redo"', "zR"), ('"command_undo"', "zU"),
    ('"command_replay"', "z8"), ('"numberS"', "z1"),
    ('"freeline"', "zF"), ('"freelineE"', "z2"), ('"thermo"', "zT"),
    ('"arrows"', "z3"), ('"direction"', "zD"),
    ('"squareframe"', "z0"), ('"polygon"', "z5"),
    ('"deletelineE"', "z4"), ('"killercages"', "z6"),
    ('"nobulbthermo"', "z7"), ('"__a"', "z_"), ("null", "zO"),
]


def cell_of(k):
    return k % SID - 2, k // SID - 2


def in_canvas(x, y):
    return 0 <= x < NX and 0 <= y < NY


def vertex_of(k):
    t = k - 1369
    return t % SID - 1, t // SID - 1


def edge_of(k):
    if 2738 <= k <= 4106:
        return ("H", (k - 2777) % SID, (k - 2777) // SID)
    if 4107 <= k <= 5475:
        return ("V", (k - 4182) % SID, (k - 4182) // SID)
    return ("?", None, None)


def corner_of(k):
    t = k - 5476
    cellid, c = divmod(t, 4)
    return cell_of(cellid) + (c,)


def decode_payload(url):
    m = re.search(r"#m=\w+&p=([A-Za-z0-9+/=_-]+)", url)
    assert m, "no p param found"
    s = m.group(1).replace("-", "+").replace("_", "/")
    s += "=" * (-len(s) % 4)
    text = zlib.decompress(base64.b64decode(s), -15).decode("utf-8", "replace")
    for orig, sub in reversed(COMPRESS_SUB):
        text = orig.join(text.split(sub))
    return text


def main():
    url = (HERE / "docs" / "penpa-url.txt").read_text().strip()
    lines = decode_payload(url).split("\n")
    hdr = lines[0].split(",")
    assert hdr[0] == "square" and hdr[1] == "33" and hdr[2] == "33", hdr[:3]
    print("header ok: square 33x33")

    pu_q = json.loads(lines[3])
    pu_a = json.loads(lines[14])
    print("pu_q keys:", {k: (len(v) if isinstance(v, (dict, list)) else v)
                         for k, v in pu_q.items()})
    print("pu_a keys:", {k: (len(v) if isinstance(v, (dict, list)) else v)
                         for k, v in pu_a.items()})

    nums = pu_q.get("number", {})
    syms = pu_q.get("symbol", {})
    surfs = pu_q.get("surface", {})
    edges = pu_q.get("lineE", {})
    cages = pu_q.get("cage", {})
    for name, d in [("numberS", pu_q.get("numberS", {})), ("line", pu_q.get("line", {})),
                    ("wall", pu_q.get("wall", {})), ("freeline", pu_q.get("freeline", {})),
                    ("freelineE", pu_q.get("freelineE", {})),
                    ("deletelineE", pu_q.get("deletelineE", {}))]:
        print(f"{name}: {len(d)}", ("SAMPLE " + str(list(d.items())[:2])) if d else "")

    # ---- numbers: givens + labels ----
    labels, givens, other = {}, [], []
    for k, v in nums.items():
        x, y = cell_of(int(k))
        ok = in_canvas(x, y)
        val = v[0]
        if val.isdigit() and len(val) == 1 and ok:
            givens.append((x, y, int(val)))
        elif val.replace(" ", "").isalpha() and ok:
            labels.setdefault(val.strip(), []).append((x, y))
        else:
            other.append((k, (x, y), val, v[1:]))
    print(f"\ngivens: {len(givens)}  labels: {sorted(labels)}")
    print("other numbers:", other)

    # ---- symbols ----
    ineq, dots, sym_other = [], [], []
    for k, v in syms.items():
        ori, X, Y = edge_of(int(k))
        if v[1] == "inequality":
            rot = v[0]
            if ori == "V":
                rel = "lt" if rot == 5 else ("gt" if rot == 7 else f"BAD{rot}")
                cells = ((X - 1, Y), (X, Y))
            elif ori == "H":
                rel = "lt" if rot == 6 else ("gt" if rot == 8 else f"BAD{rot}")
                cells = ((X, Y - 1), (X, Y))
            else:
                cells, rel = None, f"BADedge{k}"
            ineq.append((cells, rel))
        elif v[1] == "circle_SS":
            dots.append(edge_cells(ori, X, Y))
        else:
            sym_other.append((k, v))
    print(f"inequalities: {len(ineq)}  kropki dots: {len(dots)}  other syms: {sym_other}")

    # ---- surfaces ----
    print("surface colors:", Counter(surfs.values()))

    # ---- borders ----
    vlines, hlines, diags, emisc = [], [], [], []
    for k, v in edges.items():
        a, b = (int(t) for t in k.split(","))
        xa, ya = vertex_of(a)
        xb, yb = vertex_of(b)
        if xa == xb and abs(ya - yb) == 1:
            vlines.append((xa, min(ya, yb), v))
        elif ya == yb and abs(xa - xb) == 1:
            hlines.append((min(xa, xb), ya, v))
        else:
            (diags if abs(xa - xb) == 1 and abs(ya - yb) == 1 else emisc).append(
                ((xa, ya), (xb, yb), v))
    print(f"borders: V={len(vlines)} H={len(hlines)} diag={len(diags)} misc={emisc}")
    print("border styles:", Counter([s for _, _, s in vlines + hlines]),
          "diag styles:", Counter([s for _, _, s in diags]))

    # ---- cages ----
    cage_segs = []
    for k, v in cages.items():
        a, b = (int(t) for t in k.split(","))
        cage_segs.append((corner_of(a), corner_of(b), v))
    print(f"cage segments: {len(cage_segs)} styles:",
          Counter([s for _, _, s in cage_segs]))

    # save raw decode for the next step
    raw = {
        "labels": {k: v for k, v in labels.items()},
        "givens": givens,
        "ineq": [list(c) + [r] for c, r in ineq],
        "dots": [list(c) for c in dots],
        "surfaces": {k: v for k, v in surfs.items()},
        "vlines": vlines, "hlines": hlines, "diags": diags,
        "cage_segs": [[list(a), list(b), s] for a, b, s in cage_segs],
        "other_numbers": other,
    }
    (HERE / "scripts" / "decode_raw.json").write_text(json.dumps(raw))
    print("wrote scripts/decode_raw.json")


def edge_cells(ori, X, Y):
    if ori == "H":
        return [(X, Y - 1), (X, Y)]
    if ori == "V":
        return [(X - 1, Y), (X, Y)]
    return None


if __name__ == "__main__":
    main()
