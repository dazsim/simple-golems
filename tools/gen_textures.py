#!/usr/bin/env python3
# draws the item textures, run from repo root (the golem entity textures come from the art folder)
import os, random
from PIL import Image

OUT = "common/src/main/resources/assets/simplegolems/textures/item"
GEM = {"A": (178, 104, 222), "R": (222, 48, 38), "L": (52, 92, 214), "D": (110, 236, 228), "E": (52, 206, 98)}
RECIPE = ["ARA", "LDL", "ERE"]
JOBS = {"lumber": (88, 170, 64), "farmer": (236, 192, 58), "miner": (206, 70, 58), "courier": (62, 120, 214), "sweeper": (166, 92, 196)}
CLAY = [(226, 156, 116), (206, 132, 94), (190, 116, 82), (164, 96, 66), (128, 72, 50)]
GREY = [(255, 255, 255), (240, 240, 240), (224, 224, 224), (200, 200, 200), (166, 166, 166)]
EYE = (40, 26, 24)
CHEEK = (236, 132, 120)
OUTLINE = (74, 44, 32)


def img():
    return Image.new("RGBA", (16, 16))


def put(im, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        im.putpixel((x, y), tuple(c) + (255,) if len(c) == 3 else tuple(c))


def rect(im, x0, y0, x1, y1, c):
    for x in range(x0, x1 + 1):
        for y in range(y0, y1 + 1):
            put(im, x, y, c)


def shaded(im, x0, y0, x1, y1, pal, rng):
    for x in range(x0, x1 + 1):
        for y in range(y0, y1 + 1):
            i = 1 + (rng.random() < 0.3)
            if y == y0 or x == x0:
                i = 0
            if y == y1 or x == x1:
                i = 3
            put(im, x, y, pal[i])


def scale(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c)


def outline(im, colour=OUTLINE):
    px = im.load()
    solid = {(x, y) for x in range(16) for y in range(16) if px[x, y][3] > 0}
    for x, y in [(x, y) for x in range(16) for y in range(16) if (x, y) not in solid]:
        if any((x + dx, y + dy) in solid for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            px[x, y] = colour + (255,)


def gem(im, x0, y0):
    for j in range(3):
        for i in range(3):
            put(im, x0 + i, y0 + j, GEM[RECIPE[j][i]])


def golem_core():
    im = img()
    rng = random.Random(1)
    # clay puck with the recipe gem set in it
    for x in range(3, 13):
        for y in range(3, 13):
            if (x in (3, 12)) and (y in (3, 12)):
                continue
            put(im, x, y, CLAY[1 + (rng.random() < 0.3)] if 3 < x < 12 and 3 < y < 12 else CLAY[3])
    rect(im, 4, 4, 11, 4, CLAY[0])
    rect(im, 5, 5, 10, 10, scale(CLAY[3], 0.8))
    for j in range(3):
        for i in range(3):
            rect(im, 5 + 2 * i, 5 + 2 * j, 6 + 2 * i, 6 + 2 * j, GEM[RECIPE[j][i]])
    put(im, 7, 7, (196, 252, 248))
    put(im, 5, 5, scale(GEM["A"], 1.25))
    put(im, 9, 5, scale(GEM["A"], 1.25))
    outline(im)
    return im


def clay_cookie():
    im = img()
    rng = random.Random(2)
    for x in range(16):
        for y in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d < 5.6:
                c = CLAY[1 + (rng.random() < 0.35)]
                if d > 4.6:
                    c = CLAY[3]
                if x + y < 12 and d > 3.6:
                    c = CLAY[0]
                put(im, x, y, c)
    for x, y in ((6, 6), (9, 7), (7, 9), (10, 10), (5, 9)):
        put(im, x, y, (232, 196, 96))
        put(im, x + 1, y, (198, 158, 64))
    outline(im)
    return im


def card(colour, icon):
    im = img()
    rng = random.Random(3)
    for x in range(2, 14):
        for y in range(3, 13):
            put(im, x, y, (236, 230, 216) if rng.random() < 0.8 else (226, 220, 204))
    rect(im, 2, 12, 13, 12, (196, 188, 172))
    rect(im, 2, 3, 13, 5, colour)
    rect(im, 2, 3, 13, 3, scale(colour, 1.15))
    rect(im, 3, 4, 3, 4, (255, 255, 255))
    icon(im, scale(colour, 0.8))
    outline(im, (70, 60, 52))
    return im


def icon_leaf(im, c):
    for x, y in ((7, 11), (7, 10), (7, 9), (6, 8), (5, 8), (5, 7), (6, 7), (8, 8), (9, 8), (9, 7), (10, 7), (8, 7), (6, 9), (9, 9)):
        put(im, x, y, c)


def icon_wheat(im, c):
    for y in range(7, 12):
        put(im, 7, y, c)
    for x, y in ((6, 7), (8, 7), (6, 9), (8, 9), (7, 6)):
        put(im, x, y, c)


def icon_pick(im, c):
    for x, y in ((5, 7), (6, 7), (7, 7), (8, 7), (9, 7), (4, 8), (10, 8), (7, 8), (7, 9), (7, 10), (7, 11)):
        put(im, x, y, c)


def icon_chest(im, c):
    rect(im, 5, 7, 10, 11, c)
    rect(im, 5, 9, 10, 9, (236, 230, 216))
    put(im, 7, 9, (60, 50, 40))
    put(im, 8, 9, (60, 50, 40))


def icon_brush(im, c):
    for x, y in ((10, 7), (9, 8), (8, 9)):
        put(im, x, y, (150, 110, 70))
    rect(im, 5, 9, 7, 11, c)
    put(im, 8, 10, c)


def marker_stick():
    im = img()
    stick = [(137, 103, 39), (104, 78, 30), (73, 54, 21)]
    for i in range(9):
        x, y = 3 + i, 12 - i
        put(im, x, y, stick[0])
        put(im, x + 1, y, stick[1])
    put(im, 3, 13, stick[2])
    rect(im, 11, 2, 13, 4, CLAY[1])
    put(im, 11, 2, CLAY[0])
    put(im, 13, 4, CLAY[3])
    put(im, 12, 3, (230, 40, 30))
    put(im, 12, 2, (255, 120, 100))
    put(im, 13, 3, (170, 20, 16))
    outline(im, (40, 28, 18))
    return im


# golem icon: front view, centred on column 7
HEAD = (3, 2, 11, 8)
CHEST = (4, 9, 10, 13)
ARMS = ((1, 9, 3, 15), (11, 9, 13, 15))
LEGS = ((4, 14, 5, 15), (9, 14, 10, 15))


def golem_body():
    im = img()
    rng = random.Random(4)
    for box in (HEAD, CHEST) + ARMS:
        shaded(im, *box, GREY, rng)
    for box in LEGS:
        shaded(im, *box, [scale(c, 0.82) for c in GREY], rng)
    rect(im, 3, 7, 11, 7, GREY[2])
    rect(im, 5, 9, 9, 13, GREY[3])
    put(im, 7, 7, GREY[3])
    put(im, 7, 6, GREY[2])
    return im


def golem_face():
    im = img()
    body = golem_body()
    outline(body, (60, 40, 32))
    for x in range(16):
        for y in range(16):
            if body.getpixel((x, y))[:3] == (60, 40, 32):
                put(im, x, y, (60, 40, 32))
    for x0 in (4, 9):
        rect(im, x0, 4, x0 + 1, 6, EYE)
        put(im, x0, 4, (255, 255, 255))
    put(im, 3, 7, CHEEK)
    put(im, 11, 7, CHEEK)
    rect(im, 5, 9, 9, 13, (0, 0, 0, 0))
    gem(im, 6, 10)
    rect(im, 11, 11, 13, 12, (104, 66, 36))
    return im


def accent(job):
    im = img()
    c = JOBS[job]
    put(im, 12, 11, c)
    put(im, 12, 12, scale(c, 0.8))
    if job == "lumber":
        rect(im, 7, 0, 7, 1, (92, 140, 50))
        rect(im, 4, 0, 6, 0, (88, 160, 52))
        rect(im, 8, 0, 10, 0, (72, 138, 42))
        put(im, 6, 1, (58, 116, 34))
        put(im, 8, 1, (104, 178, 64))
    elif job == "farmer":
        rect(im, 0, 2, 14, 2, (220, 186, 98))
        rect(im, 1, 2, 13, 2, (236, 206, 120))
        rect(im, 4, 0, 10, 1, (236, 206, 120))
        rect(im, 4, 1, 10, 1, (186, 58, 48))
        put(im, 5, 0, (246, 222, 146))
    elif job == "miner":
        rect(im, 3, 0, 11, 2, (246, 196, 40))
        rect(im, 3, 2, 11, 2, (212, 160, 26))
        rect(im, 6, 0, 8, 0, (255, 216, 80))
        rect(im, 6, 1, 8, 2, (255, 244, 170))
    elif job == "courier":
        rect(im, 9, 12, 12, 14, (150, 96, 54))
        rect(im, 9, 12, 12, 12, (112, 70, 36))
        put(im, 10, 13, (246, 240, 226))
        put(im, 11, 13, (206, 60, 50))
        rect(im, 10, 9, 10, 11, (104, 66, 36))
    elif job == "sweeper":
        rect(im, 4, 9, 10, 9, (242, 238, 226))
        rect(im, 5, 10, 6, 10, (242, 238, 226))
        put(im, 5, 11, (242, 238, 226))
        put(im, 5, 9, (44, 62, 120))
        put(im, 8, 9, (44, 62, 120))
    return im


def main():
    os.makedirs(OUT, exist_ok=True)
    golem_core().save(f"{OUT}/golem_core.png")
    clay_cookie().save(f"{OUT}/clay_cookie.png")
    marker_stick().save(f"{OUT}/marker_stick.png")
    icons = {"lumber": icon_leaf, "farmer": icon_wheat, "miner": icon_pick, "courier": icon_chest, "sweeper": icon_brush}
    for job, colour in JOBS.items():
        card(colour, icons[job]).save(f"{OUT}/{job}_card.png")
        accent(job).save(f"{OUT}/golem_accent_{job}.png")
    golem_body().save(f"{OUT}/golem_body.png")
    golem_face().save(f"{OUT}/golem_face.png")


main()
