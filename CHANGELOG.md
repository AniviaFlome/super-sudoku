# Changelog

## 1.0.3 — 2026-09-17

- Fixed the super-board timer being stuck at 0:00 (game clock now runs and saves on exit).
- Fixed overlapping overview labels ("Sudoku X", "White Kropki"): kept every word, stacked as compass captions, centered, with an overlap guard.
- Settings dropdown menus now follow the board theme.

## 1.0.2 — 2026-09-17

- Release APKs are now signed with a stable release key, so updates install over previous releases (1.0.0/1.0.1 debug-signed builds must be uninstalled once).
- CI actions moved to Node 24 runtimes.

## 1.0.1 — 2026-09-17

- System back button follows on-screen navigation instead of quitting the app (unfocuses a focused super grid first).
- System bars filled with the board background: no more transparent nav bar showing the board.
- Numpad background extends into the navigation area (gap filled).
- In-game top bar (timer/settings) background fills the status area.

## 1.0.0 — 2026-09-17

First release.
