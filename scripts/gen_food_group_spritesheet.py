#!/usr/bin/env python3
"""Generate 96x16 RGBA food-group spritesheet (6 x 16px icons)."""
from __future__ import annotations

from pathlib import Path

from PIL import Image

T = (0, 0, 0, 0)
K = (45, 35, 30, 255)
O1, O2, O3 = (255, 152, 40, 255), (230, 110, 20, 255), (255, 210, 120, 255)
G1, G2, G3 = (70, 200, 70, 255), (35, 130, 45, 255), (140, 235, 100, 255)
M1, M2, M3 = (160, 95, 55, 255), (120, 65, 40, 255), (90, 50, 35, 255)
X = (35, 25, 20, 255)
B1, B2, B3 = (230, 185, 90, 255), (200, 150, 60, 255), (255, 220, 140, 255)
C1, C2 = (255, 248, 220, 255), (240, 220, 180, 255)
R = (220, 50, 50, 255)
D1, D2, D3 = (200, 205, 215, 255), (160, 165, 175, 255), (245, 248, 255, 255)

CMAP: dict[str, tuple[int, int, int, int]] = {
    ".": T,
    "#": K,
    "o": O1,
    "O": O2,
    "h": O3,
    "g": G1,
    "G": G2,
    "L": G3,  # leaf highlight (was 'l' — avoid ambiguous l/1)
    "m": M1,
    "M": M2,
    "b": M3,
    "x": X,
    "w": B1,
    "W": B2,
    "y": B3,
    "c": C1,
    "C": C2,
    "r": R,
    "d": D1,
    "D": D2,
    "i": D3,
}


def blit_cell(sheet: Image.Image, cx: int, rows: list[str]) -> None:
    assert len(rows) == 16, (cx, len(rows))
    ox = cx * 16
    for y, row in enumerate(rows):
        if len(row) != 16:
            raise ValueError(f"cell {cx} row {y} len={len(row)!r} {row!r}")
        for x, ch in enumerate(row):
            if ch not in CMAP:
                raise ValueError(f"cell {cx} row {y} bad {ch!r}")
            sheet.putpixel((ox + x, y), CMAP[ch])


def main() -> None:
    fruit = [
        "................",
        "......##........",
        ".....#GG#.......",
        "....#oooo#......",
        "...#oooooo#.....",
        "..#oooooooo#....",
        "..#oooooooo#....",
        ".#oooooooooo#...",
        ".#ooOooooOoo#...",
        ".#oooooooooo#...",
        ".#oooooooooo#...",
        "..#oooooooo#....",
        "..#oooooooo#....",
        "...#oooooo#.....",
        "....######......",
        "................",
    ]
    veg = [
        "................",
        ".....###........",
        "....#GLG#.......",
        "...#GLGLG#......",
        "..#GGLLLGG#.....",
        ".#GLLLLLLLG#....",
        ".#GLLgggLLG#....",
        "#GLLLgggLLLG#...",
        "#GLLgggggLLG#...",
        "#GLLgggggLLG#...",
        ".#GLLgggLLG#....",
        ".#GGLLLLLGG#....",
        "..#GGGGGG#......",
        "...######.......",
        "................",
        "................",
    ]
    meat = [
        "................",
        "................",
        "....########....",
        "...#mmmmmmMM#...",
        "..#MmmmmmmMM#...",
        ".#MMmxxxxmmMM#..",
        ".#MmmxxxxmmMM#..",
        ".#MmmmmmmmmMM#..",
        ".#MMmmmmmmMM#...",
        "..#MmmmmmmM#....",
        "...#MMMMMM#.....",
        "....######......",
        "................",
        "................",
        "................",
        "................",
    ]
    bread = [
        "................",
        "................",
        "................",
        "......######....",
        ".....#yyyyWW#...",
        "....#WyyyyyyW#..",
        "...#WWyyyyyyW#..",
        "..#WyyWyyyyyWW#.",
        ".#WyyyyyyyyyyW#.",
        ".#WWWWWWWWWWWW#.",
        "..############..",
        "................",
        "................",
        "................",
        "................",
        "................",
    ]
    cake = [
        "................",
        "........#r#.....",
        ".......#rrr#....",
        ".......#.C.#....",
        "......#cccc#....",
        ".....#cccccc#...",
        "....#cccccccc#..",
        "....#cccccccc#..",
        "...#cccccccccc#.",
        "...#CCCCCCCCCC#.",
        "...#cccccccccc#.",
        "....##########..",
        "................",
        "................",
        "................",
        "................",
    ]
    bucket = [
        "................",
        "......####......",
        ".....#dddd#.....",
        "....#dddddd#....",
        "...#dddddddd#...",
        "...#iiiiiiii#...",
        "...#iiiiiiii#...",
        "...#iiiiiiii#...",
        "...#DDDDDDDD#...",
        "...#DDDDDDDD#...",
        "...#DDDDDDDD#...",
        "....#DDDDDD#....",
        ".....######.....",
        "................",
        "................",
        "................",
    ]

    img = Image.new("RGBA", (96, 16), T)
    for i, grid in enumerate([fruit, veg, meat, bread, cake, bucket]):
        blit_cell(img, i, grid)

    out = Path(__file__).resolve().parents[1] / "src/main/resources/assets/nourished/textures/gui/food_groups_spritesheet.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    img.save(out)
    print("wrote", out, img.size, img.mode)


if __name__ == "__main__":
    main()
