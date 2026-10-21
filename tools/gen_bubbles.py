"""Thought bubble textures: a round cloud with two trailing dots and a 10x10 problem icon inside."""
import math, sys
from PIL import Image

OUT = sys.argv[1] if len(sys.argv) > 1 else "common/src/main/resources/assets/simplegolems/textures/entity/golem"
S = 32
EDGE, FILL, SHADE = (48, 44, 52, 255), (255, 255, 255, 255), (214, 216, 228, 255)
RED, DARKRED = (224, 46, 40, 255), (120, 20, 18, 255)

PAL = {
    "o": (42, 30, 22, 255), "l": (186, 128, 64, 255), "b": (150, 98, 46, 255), "d": (106, 68, 32, 255),
    "g": (210, 210, 210, 255), "k": (70, 70, 70, 255),
    "L": (118, 190, 70, 255), "G": (58, 122, 30, 255), "q": (82, 154, 44, 255), "s": (110, 76, 38, 255),
    "h": (220, 220, 222, 255), "H": (140, 140, 146, 255), "w": (150, 104, 52, 255), "W": (92, 62, 28, 255),
    "A": (74, 44, 32, 255), "B": (226, 156, 116, 255), "C": (164, 96, 66, 255), "D": (206, 132, 94, 255),
    "E": (190, 116, 82, 255), "F": (232, 196, 96, 255),
    "c": (150, 150, 150, 255), "m": (112, 112, 112, 255), "n": (84, 84, 86, 255), "v": (178, 178, 180, 255),
    "x": (96, 18, 30, 255), "R": (226, 52, 68, 255), "r": (170, 28, 44, 255), "P": (255, 178, 186, 255),
}

ICONS = {
    "heart": [
        "..........",
        ".xx...xx..",
        "xPRx.xRRx.",
        "xPRRxRRRx.",
        "xRRRRRRrx.",
        ".xRRRRrx..",
        "..xRRrx...",
        "...xrx....",
        "....x.....",
        "..........",
    ],
    "chest": [
        ".oooooooo.",
        "ollllllllo",
        "obbbbbbbbo",
        "obbbbbbbbo",
        "oddddggddo",
        "obbbbkgbbo",
        "obbbbbbbbo",
        "obbbbbbbbo",
        "oddddddddo",
        ".oooooooo.",
    ],
    "sapling": [
        "....LL....",
        "..LLqL.L..",
        ".LqGqLLqL.",
        ".qGqGqGqG.",
        "..GqGsGq..",
        "....sG....",
        "....s.....",
        "....s.....",
        "...sss....",
        "..........",
    ],
    "cookie": [
        "...AAAA...",
        ".AABBBBAA.",
        ".ABBDFBCA.",
        "ABBDDEDDCA",
        "ABFDDEDFCA",
        "ACDDEDFDCA",
        "ACEDDDDECA",
        ".ACDEDDCA.",
        ".AACCCCAA.",
        "...AAAA...",
    ],
    "cobble": [
        "nnnnnnnnnn",
        "nvcmncvcmn",
        "ncmnvcmnnn",
        "nmnvccnvcn",
        "nnvcmmnccn",
        "nvcmnnvcmn",
        "ncmnvcmnmn",
        "nnnvcmnvcn",
        "nvcmnnvcmn",
        "nnnnnnnnnn",
    ],
    "axe": [
        ".....ooo..",
        "....ohhHo.",
        "...ohhhHHo",
        "...oohWHo.",
        "....oWwoo.",
        "...oWwo...",
        "..oWwo....",
        ".oWwo.....",
        "oWwo......",
        ".oo.......",
    ],
    "pickaxe": [
        "..ooooo...",
        ".ohhhhHo..",
        "..oooWhHo.",
        "....oWwoHo",
        "...oWwo.Ho",
        "..oWwo..oo",
        ".oWwo.....",
        "oWwo......",
        "oWo.......",
        ".o........",
    ],
    "hoe": [
        "....oooo..",
        "...ohhhHo.",
        "....ooWHo.",
        "....oWwo..",
        "...oWwo...",
        "..oWwo....",
        ".oWwo.....",
        "oWwo......",
        "oWo.......",
        ".o........",
    ],
    "shears": [
        "......oo..",
        ".....ohho.",
        "....ohhHo.",
        "...ohHHo..",
        "..ooHoo...",
        ".oRRoHo...",
        "oRo.oRRo..",
        "oRo..oRo..",
        ".oRRoRRo..",
        "..oo..oo..",
    ],
    "rod": [
        "........oo",
        ".......oWo",
        "......oWo.",
        ".....oWo.h",
        "....oWo..h",
        "...oWo...h",
        "..oWo....h",
        ".oWo...oRo",
        "oWo......o",
        ".o........",
    ],
    "hammer": [
        "...oooo...",
        "..ohhhHo..",
        ".ohhhhHHo.",
        ".ooohhooo.",
        "....oWo...",
        "...oWo....",
        "..oWo.....",
        ".oWo......",
        "oWo.......",
        ".o........",
    ],
    "sack": [
        "...oooo...",
        "....ob....",
        "...olbo...",
        "..olbbbo..",
        ".olbbbbdo.",
        ".olbbbbdo.",
        "olbbbbbbdo",
        "olbbbbbbdo",
        ".obbbbddo.",
        "..oooooo..",
    ],
}


def cloud_mask():
    blobs = [(13, 12, 5.6), (18, 8.5, 5.8), (24, 10.5, 5.2), (21, 15, 5.0), (15, 16, 4.4), (26, 15.5, 3.6)]
    dots = [(8.5, 23.5, 2.1), (4.5, 28.0, 1.4)]
    m = set()
    for x in range(S):
        for y in range(S):
            px, py = x + 0.5, y + 0.5
            if any((px - cx) ** 2 + (py - cy) ** 2 <= r * r for cx, cy, r in blobs + dots):
                m.add((x, y))
    return m


def bubble():
    m = cloud_mask()
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    for x, y in m:
        im.putpixel((x, y), SHADE if (x, y + 1) not in m or (x, y + 2) not in m else FILL)
    for x in range(S):
        for y in range(S):
            if (x, y) in m:
                continue
            if any((x + dx, y + dy) in m for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                im.putpixel((x, y), EDGE)
    return im


def cross(im, ox, oy, n=10):
    for i in range(-1, n + 1):
        im.putpixel((ox + i, oy + i), RED)
        if i < n:
            im.putpixel((ox + i, oy + i + 1), DARKRED)


def badge(im, ox, oy):
    for x in range(ox - 1, ox + 4):
        for y in range(oy - 1, oy + 4):
            im.putpixel((x, y), FILL)
    for i in range(3):
        im.putpixel((ox + i, oy + i), RED)
        im.putpixel((ox + 2 - i, oy + i), RED)


def make(icon, crossed=False, marked=False):
    im = bubble()
    ox, oy = (15, 7) if icon == "heart" else (14, 7)
    for y, row in enumerate(ICONS[icon]):
        for x, c in enumerate(row[:10]):
            if c != ".":
                im.putpixel((ox + x, oy + y), PAL[c])
    if crossed:
        cross(im, ox, oy)
    if marked:
        badge(im, ox + 8, oy + 8)
    return im


if __name__ == "__main__":
    out = {
        "bubble_storage": make("chest", marked=True),
        "bubble_sapling": make("sapling", crossed=True),
        "bubble_food": make("cookie", crossed=True),
        "bubble_axe": make("axe", crossed=True),
        "bubble_pickaxe": make("pickaxe", crossed=True),
        "bubble_hoe": make("hoe", crossed=True),
        "bubble_blocks": make("cobble", crossed=True),
        "bubble_shears": make("shears", crossed=True),
        "bubble_rod": make("rod", crossed=True),
        "bubble_hammer": make("hammer", crossed=True),
        "bubble_supplies": make("sack", crossed=True),
        "bubble_thanks": make("heart"),
    }
    for k, v in out.items():
        v.save(f"{OUT}/{k}.png")
    if len(sys.argv) > 2:
        sheet = Image.new("RGBA", (len(out) * 36 * 8, 36 * 8), (120, 170, 230, 255))
        for i, v in enumerate(out.values()):
            sheet.alpha_composite(v.resize((S * 8, S * 8), Image.NEAREST), (i * 288 + 16, 16))
        sheet.save(sys.argv[2])
