#!/usr/bin/env python3
"""Craft a minimal 9x9 Penpa+ URL for testing the Kotlin importer.

Writes /tmp/mini_penpa_url.txt. Contents:
- 9x9 canvas, givens 5@(0,0), 3@(1,1)
- inequality (2,2) < (3,2)  [vertical edge, rotation 5]
- white dot between (5,3)-(5,4)  [horizontal edge]
- killer cage [(6,6),(7,6)] total 11 (corner total)
- standard 3x3 box lines (style 2)
"""
import base64
import json
import zlib

NX = NY = 9
S = NX + 4  # stride 13
B = S * S  # 169


def cell(x, y):
    return (y + 2) * S + (x + 2)


def vert(X, Y):  # vertex intersection
    return B + S * (Y + 1) + (X + 1)


def hedge(X, Y):  # horizontal edge: line Y, column X
    return 2 * B + S * (Y + 1) + (X + 2)


def vedge(X, Y):  # vertical edge: line X, row Y
    return 3 * B + S * (Y + 2) + (X + 1)


def corner(x, y, c):
    return 4 * B + 4 * cell(x, y) + c


# box lines
lineE = {}
for X in (3, 6):
    for Y in range(0, 9):
        a, b = vert(X, Y), vert(X, Y + 1)
        lineE[f"{a},{b}"] = 2
for Y in (3, 6):
    for X in range(0, 9):
        a, b = vert(X, Y), vert(X + 1, Y)
        lineE[f"{a},{b}"] = 2

pu_q = {
    "command_redo": {"__a": []},
    "command_undo": {"__a": []},
    "command_replay": {"__a": []},
    "surface": {},
    "number": {str(cell(0, 0)): ["5", 1, "1"], str(cell(1, 1)): ["3", 1, "1"]},
    "numberS": {str(corner(6, 6, 0)): [" 11", 1]},
    "symbol": {
        str(vedge(3, 2)): [5, "inequality", 2],
        str(hedge(5, 4)): [1, "circle_SS", 1],
    },
    "freeline": {},
    "freelineE": {},
    "thermo": [],
    "arrows": [],
    "direction": [],
    "squareframe": {},
    "polygon": {},
    "line": {},
    "lineE": lineE,
    "wall": {},
    "cage": {},
    "deletelineE": {},
    "killercages": [[cell(6, 6), cell(7, 6)]],
    "nobulbthermo": {},
}

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

header = "square,9,9,38,0,1,1,342,342,171,171,0,0,0,0,Title: Mini Test,Author: ,,ON,false,"
lines = [header, "[0,0,0,0]", '["1","2","1"]~"surface"~["",1]',
         json.dumps(pu_q, separators=(",", ":")),
         "", "[88]", "[]",
         '{"sol_surface_exact":false}', '"x"', '"x"', "[3,2,1]",
         '{"qa":"pu_q"}', '"x"', "0", "{}", "x", "{}", "[]", "false"]
text = "\n".join(lines)
for orig, sub in COMPRESS_SUB:
    text = text.replace(orig, sub)
c = zlib.compressobj(9, zlib.DEFLATED, -15)
payload = c.compress(text.encode()) + c.flush()
b64 = base64.urlsafe_b64encode(payload).decode().rstrip("=")
url = "https://swaroopg92.github.io/penpa-edit/#m=solve&p=" + b64
open("/tmp/mini_penpa_url.txt", "w").write(url)
print("chars:", len(url))
print(url[:120])
