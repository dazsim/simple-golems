#!/usr/bin/env python3
# writes item models, recipes and lang, run from repo root
import json, os

RES = "common/src/main/resources"
ASSETS = f"{RES}/assets/simplegolems"
DATA = f"{RES}/data/simplegolems"
NS = "simplegolems"

JOBS = ["lumber", "farmer", "miner", "courier", "sweeper"]
HINTS = {"lumber": "#minecraft:saplings", "farmer": "minecraft:wheat", "miner": "minecraft:cobblestone",
         "courier": "minecraft:chest", "sweeper": "minecraft:feather"}
COLOURS = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue",
           "brown", "green", "red", "black"]
# keep in sync with GolemRenderer
UNDYED = 0xBE7858
DYES = [0xFFDEC9, 0xC96A33, 0xBA7088, 0x8F88AC, 0xE7A631, 0x839445, 0xC96464, 0x4B3931,
        0xAA867B, 0x6E7474, 0x955B6E, 0x5F4E74, 0x634431, 0x626A39, 0xB3503E, 0x332019]


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as fh:
        json.dump(obj, fh, indent=2)
        fh.write("\n")


def title(s):
    return " ".join(w.capitalize() for w in s.split("_"))


def flat_model(name, texture=None):
    write(f"{ASSETS}/models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{texture or name}"}})


def plain(name):
    return {"type": "minecraft:model", "model": f"{NS}:item/{name}"}


def items():
    simple = ["golem_core", "clay_cookie", "marker_stick"] + [f"{j}_card" for j in JOBS]
    for n in simple:
        flat_model(n)
        write(f"{ASSETS}/items/{n}.json", {"model": plain(n)})
    for n in ["golem_body", "golem_face"] + [f"golem_accent_{j}" for j in JOBS]:
        flat_model(n)

    def body(colour):
        return {"type": "minecraft:model", "model": f"{NS}:item/golem_body", "tints": [{"type": "minecraft:constant", "value": (0xFF000000 | colour) - (1 << 32)}]}

    write(f"{ASSETS}/items/golem.json", {"model": {"type": "minecraft:composite", "models": [
        {"type": "minecraft:select", "property": "minecraft:component", "component": f"{NS}:colour",
         "cases": [{"when": c, "model": body(DYES[i])} for i, c in enumerate(COLOURS)], "fallback": body(UNDYED)},
        plain("golem_face"),
        {"type": "minecraft:select", "property": "minecraft:component", "component": f"{NS}:job",
         "cases": [{"when": j, "model": plain(f"golem_accent_{j}")} for j in JOBS],
         "fallback": {"type": "minecraft:empty"}}]}})


def shaped(name, pattern, key, result, count=1, components=None, category="misc"):
    r = {"type": "minecraft:crafting_shaped", "category": category, "pattern": pattern, "key": key,
         "result": {"id": f"{NS}:{result}", "count": count}}
    if components:
        r["result"]["components"] = components
    write(f"{DATA}/recipe/{name}.json", r)


def shapeless(name, ingredients, result, count=1):
    write(f"{DATA}/recipe/{name}.json", {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": ingredients,
                                          "result": {"id": f"{NS}:{result}", "count": count}})


def recipes():
    shaped("golem_core", ["RLR", "LEL", "RLR"], {"R": "minecraft:redstone", "L": "minecraft:lapis_lazuli", "E": "minecraft:emerald"}, "golem_core")
    body = {"B": "minecraft:clay", "C": f"{NS}:golem_core", "b": "minecraft:clay_ball"}
    shaped("golem", [" B ", "BCB", "b b"], body, "golem")
    for j in JOBS:
        shaped(f"{j}_golem", [" B ", "BCB", "bKb"], dict(body, K=f"{NS}:{j}_card"), "golem", components={f"{NS}:job": j})
        shapeless(f"{j}_card", ["minecraft:paper", HINTS[j]], f"{j}_card")
    shaped("clay_cookie", ["#X#"], {"#": "minecraft:wheat", "X": "minecraft:clay_ball"}, "clay_cookie", count=4, category="food")
    shapeless("marker_stick", ["minecraft:stick", "minecraft:redstone", "minecraft:clay_ball"], "marker_stick")


LANG = {
    "itemGroup.simplegolems": "Simple Golems",
    "entity.simplegolems.golem": "Golem",
    "item.simplegolems.golem_core": "Golem Core",
    "item.simplegolems.golem": "Golem",
    "item.simplegolems.golem.blank": "Blank Golem",
    "item.simplegolems.golem.lumber": "Lumber Golem",
    "item.simplegolems.golem.farmer": "Farmer Golem",
    "item.simplegolems.golem.miner": "Miner Golem",
    "item.simplegolems.golem.courier": "Courier Golem",
    "item.simplegolems.golem.sweeper": "Sweeper Golem",
    "item.simplegolems.clay_cookie": "Clay Cookie",
    "item.simplegolems.marker_stick": "Marker Stick",
    "item.simplegolems.lumber_card": "Lumber Card",
    "item.simplegolems.farmer_card": "Farming Card",
    "item.simplegolems.miner_card": "Mining Card",
    "item.simplegolems.courier_card": "Carrying Card",
    "item.simplegolems.sweeper_card": "Sweeping Card",
    "tooltip.simplegolems.area": "Work area %s x %s x %s",
    "job.simplegolems.blank": "Blank golem",
    "job.simplegolems.lumber": "Lumber golem",
    "job.simplegolems.farmer": "Farmer golem",
    "job.simplegolems.miner": "Miner golem",
    "job.simplegolems.courier": "Courier golem",
    "job.simplegolems.sweeper": "Sweeper golem",
    "message.simplegolems.not_yours": "This golem belongs to someone else",
    "message.simplegolems.has_card": "Take the card out first (sneak and use a stick)",
    "message.simplegolems.programmed": "Now a %s",
    "message.simplegolems.cookies_full": "The cookie tin is full",
    "message.simplegolems.fed": "%s clay cookies in the tin",
    "hud.simplegolems.cookies": "Cookies: %s/%s",
    "message.simplegolems.wrong_tool.blank": "A blank golem has no use for tools, give it a card first",
    "message.simplegolems.wrong_tool.lumber": "A lumber golem works with an axe",
    "message.simplegolems.wrong_tool.farmer": "A farmer golem works with a hoe",
    "message.simplegolems.wrong_tool.miner": "A miner golem works with a pickaxe",
    "message.simplegolems.wrong_tool.courier": "A courier golem needs no tool",
    "message.simplegolems.wrong_tool.sweeper": "A sweeper golem only takes a brush or a shovel",
    "message.simplegolems.status.blank": "Blank golem, following you. Give it a card to put it to work",
    "message.simplegolems.status.no_tool": "%s waiting for a tool",
    "message.simplegolems.status.no_power": "%s out of power, feed it clay cookies",
    "message.simplegolems.status.working": "%s, about %s min of power, area %s x %s x %s",
    "message.simplegolems.status.no_area": "%s needs an area, mark one with a marker stick",
    "message.simplegolems.status.no_griefing": "%s is resting, mob griefing is off",
    "message.simplegolems.status.no_route": "%s needs a container near each corner of its area",
    "message.simplegolems.marker.first": "First corner set at %s %s %s",
    "message.simplegolems.marker.second": "Area %s x %s x %s marked, use it on a golem",
    "message.simplegolems.marker.too_big": "Too big, an area can be at most %s blocks across",
    "message.simplegolems.marker.cleared": "Marker cleared",
    "message.simplegolems.marker.unfinished": "Mark two corners first",
    "message.simplegolems.marker.given": "Area %s x %s x %s given to the golem",
    "message.simplegolems.marker.reset": "Back to the default area around here",
    "tooltip.simplegolems.marker.first": "First corner %s %s %s",
    "tooltip.simplegolems.marker.area": "Area %s x %s x %s",
    "tooltip.simplegolems.marker.help": "Use on two blocks, then on a golem",
}


def lang():
    write(f"{ASSETS}/lang/en_us.json", LANG)


# plain blocks a miner may place for steps and to seal water and lava: no ores, valuables, block entities or falling blocks
BUILDING = ["cobblestone", "cobbled_deepslate", "stone", "deepslate", "andesite", "diorite", "granite", "tuff",
            "dirt", "coarse_dirt", "netherrack", "blackstone", "basalt", "smooth_basalt", "calcite", "end_stone",
            "stone_bricks", "deepslate_bricks", "mossy_cobblestone", "dripstone_block", "sandstone", "red_sandstone",
            "mud_bricks", "packed_mud", "terracotta"]


def tags():
    write(f"{DATA}/tags/item/miner_building_blocks.json", {"values": [f"minecraft:{b}" for b in BUILDING]})


items()
recipes()
lang()
tags()
