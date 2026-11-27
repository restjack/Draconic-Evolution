"""Draw the Celestial Manipulator screen's toggle and skip icons (16x16, transparent, Draconic palette, dark outline).

Same style as Evo Tweaks' tools/make_field_icons.py: a 12x12 pixel map placed at (2, 2), then a 1 px dark outline so
it reads on the green "on" and grey "off" button faces.

Usage: python -I gradle/make_celestial_icons.py [out dir]
Default out dir: src/main/resources/assets/draconicevolution/textures/gui/celestial_manipulator
Set ICON_SHEET=<png> to also write an 8x contact sheet (on/off faces).
"""
import os
import sys
from PIL import Image

PAL = {
    "O": (0xFF, 0x9A, 0x1F), "Y": (0xFF, 0xE0, 0x66),  # Draconic orange / hot yellow
    "R": (0xE8, 0x2A, 0x2A), "r": (0x6A, 0x10, 0x10),  # red, unlit red
    "P": (0xA8, 0x50, 0xFF), "p": (0x5A, 0x2A, 0x9A),  # chaotic purple
    "B": (0x8F, 0xB8, 0xFF), "b": (0x4A, 0x6A, 0xB0),  # sky blue
    "W": (0xF2, 0xF2, 0xF6), "M": (0xB8, 0xB8, 0xC6), "g": (0x60, 0x60, 0x6C),  # white, metal, dark metal
}
OUTLINE = (0x14, 0x14, 0x1A)

ICONS = {
    # Keep skies clear: sun, with a small cloud struck through
    "keep_clear": """
....Y..Y....
..Y.OOOO.Y..
...OYYYYO...
..OYYYYYYO..
Y.OYYYYYYO.Y
..OYYYYYYO..
...OYYYYO..R
..Y.OOOO..R.
....Y..YMR..
......MMRMM.
.....MMRMMMM
....RMMMMMM.""",
    # Owner only: padlock
    "owner_only": """
....MMMM....
...M....M...
..M......M..
..M......M..
..M......M..
.OOOOOOOOOO.
.OYYYYYYYYO.
.OYYYggYYYO.
.OYYYggYYYO.
.OYYYYgYYYO.
.OYYYYYYYYO.
.OOOOOOOOOO.""",
    # Tell everyone: bell with sound marks
    "notice": """
.....OO.....
....OYYO....
...OYYYYO...
..OYYYYYYO..
..OYYYYYYO..
..OYYYYYYO..
.OYYYYYYYYO.
.OYYYYYYYYO.
OOOOOOOOOOOO
.....YY.....
B..........B
.B........B.""",
    # Quiet boom: speaker with a single small wave
    "quiet": """
............
.....M......
....MM......
...MWM......
MMMWWM...B..
MWWWWM....B.
MWWWWM....B.
MMMWWM...B..
...MWM......
....MM......
.....M......
............""",
    # Redstone: lit dust (shown when redstone does nothing)
    "redstone": """
............
.....rr.....
....rRRr....
...rRYYRr...
..rRYWWYRr..
..rRYWWYRr..
...rRYYRr...
....rRRr....
.....rr.....
.r........r.
rRr......rRr
.r........r.""",
    # Skip a full day: clock face with a +1 day arrow
    "skip24": """
...MMMMMM...
..M.WWWW.M..
.M.WWWWWW.M.
M.WWWWgWWW.M
M.WWWWgWWW.M
M.WWWWggggOM
M.WWWWWWWOYO
.M.WWWWWOYYO
..M.WWWOOOOO
...MMMM..YO.
.........O..
............""",
}


def grid(art):
    rows = art.strip("\n").split("\n")
    assert all(len(r) == len(rows[0]) for r in rows), "ragged rows"
    return rows


def draw(art, size=16, at=2):
    rows = grid(art)
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    filled = set()
    for y, row in enumerate(rows):
        for x, c in enumerate(row):
            if c != ".":
                img.putpixel((x + at, y + at), PAL[c] + (255,))
                filled.add((x + at, y + at))
    for (x, y) in filled:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            p = (x + dx, y + dy)
            if p not in filled and 0 <= p[0] < size and 0 <= p[1] < size:
                img.putpixel(p, OUTLINE + (255,))
    return img


def main(out):
    os.makedirs(out, exist_ok=True)
    for name, art in ICONS.items():
        assert len(grid(art)) == 12 and len(grid(art)[0]) == 12, name
        draw(art).save(os.path.join(out, name + ".png"))
    sheet_path = os.environ.get("ICON_SHEET")
    if sheet_path:
        names = list(ICONS)
        sheet = Image.new("RGBA", (len(names) * 144, 2 * 144), (0x2A, 0x2A, 0x2A, 255))
        for i, n in enumerate(names):
            icon = Image.open(os.path.join(out, n + ".png"))
            for j, bg in enumerate(((0x40, 0xC0, 0x40, 255), (0x6A, 0x6A, 0x6A, 255))):
                tile = Image.new("RGBA", (16, 16), bg)
                tile.alpha_composite(icon)
                sheet.paste(tile.resize((128, 128), Image.NEAREST), (i * 144 + 8, j * 144 + 8))
        sheet.save(sheet_path)
    print("wrote", len(ICONS), "icons to", out)


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources",
                                                            "assets", "draconicevolution", "textures", "gui", "celestial_manipulator"))
