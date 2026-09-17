"""Shared helpers: parse killercages + numberS (corner totals) from the Penpa+ URL."""
import base64
import json
import re
import zlib
from pathlib import Path

HERE = Path(__file__).resolve().parent.parent

_COMPRESS_SUB = [
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

_pu_q = None


def pu_q():
    global _pu_q
    if _pu_q is None:
        url = (HERE / "docs" / "penpa-url.txt").read_text().strip()
        m = re.search(r"#m=\w+&p=([A-Za-z0-9+/=_-]+)", url)
        s = m.group(1).replace("-", "+").replace("_", "/")
        s += "=" * (-len(s) % 4)
        text = zlib.decompress(base64.b64decode(s), -15).decode("utf-8", "replace")
        for orig, sub in reversed(_COMPRESS_SUB):
            text = orig.join(text.split(sub))
        _pu_q = json.loads(text.split("\n")[3])
    return _pu_q


def corner_cell(k):
    """inner-corner point id -> (canvas cell x, y, corner idx)"""
    t = k - 5476
    cellid, c = divmod(t, 4)
    return cellid % 37 - 2, cellid // 37 - 2, c


def killer_data():
    """Returns (cages, totals): cages = list of cell-id lists (37-stride space),
    totals = list of ((canvas x, y of corner's cell), value)."""
    q = pu_q()
    cages = [c for c in q.get("killercages", []) if c]
    totals = []
    for k, v in q.get("numberS", {}).items():
        x, y, _c = corner_cell(int(k))
        totals.append(((x, y), int(v[0].strip())))
    return cages, totals
