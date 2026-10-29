#!/usr/bin/env python3
# writes item models, recipes and lang, run from repo root
import json, os

RES = "common/src/main/resources"
ASSETS = f"{RES}/assets/simplegolems"
DATA = f"{RES}/data/simplegolems"
NS = "simplegolems"

JOBS = ["lumber", "farmer", "miner", "courier", "sweeper", "cobblegen", "sifter", "hammer", "shepherd", "rancher", "composter",
        "smelter", "beekeeper", "fisher", "lamplighter", "guard"]
HINTS = {"lumber": "#minecraft:saplings", "farmer": "minecraft:wheat", "miner": "minecraft:cobblestone",
         "courier": "minecraft:chest", "sweeper": "minecraft:feather", "cobblegen": "minecraft:stone", "sifter": "exdeorum:string_mesh",
         "hammer": "exdeorum:wooden_hammer", "shepherd": "#minecraft:wool", "rancher": "minecraft:lead", "composter": "minecraft:composter",
         "smelter": "minecraft:furnace", "beekeeper": "minecraft:honeycomb", "fisher": "minecraft:cod", "lamplighter": "minecraft:torch",
         "guard": "minecraft:iron_sword"}
EXDEORUM = ["sifter", "hammer"]
# name, card, golem, job, tool message
NAMES = {
    "lumber": ("Lumber Card", "Lumber Golem", "Lumber golem", "A lumber golem works with an axe"),
    "farmer": ("Farming Card", "Farmer Golem", "Farmer golem", "A farmer golem works with a hoe"),
    "miner": ("Mining Card", "Miner Golem", "Miner golem", "A miner golem works with a pickaxe"),
    "courier": ("Carrying Card", "Courier Golem", "Courier golem", "A courier golem needs no tool"),
    "sweeper": ("Sweeping Card", "Sweeper Golem", "Sweeper golem", "A sweeper golem only takes a brush or a shovel"),
    "cobblegen": ("Cobblegen Card", "Cobblegen Golem", "Cobblegen golem", "A cobblegen golem works with a pickaxe"),
    "sifter": ("Sifting Card", "Sifter Golem", "Sifter golem", "A sifter golem needs no tool, put a mesh in the sieve"),
    "hammer": ("Hammering Card", "Hammer Golem", "Hammer golem", "A hammer golem works with an Ex Deorum hammer"),
    "shepherd": ("Shepherd Card", "Shepherd Golem", "Shepherd golem", "A shepherd golem works with shears"),
    "rancher": ("Ranching Card", "Rancher Golem", "Rancher golem", "A rancher golem needs no tool, just feed in storage"),
    "composter": ("Composting Card", "Composter Golem", "Composter golem", "A composter golem needs no tool"),
    "smelter": ("Smelting Card", "Smelter Golem", "Smelter golem", "A smelter golem needs no tool"),
    "beekeeper": ("Beekeeping Card", "Beekeeper Golem", "Beekeeper golem", "A beekeeper golem only takes shears"),
    "fisher": ("Fishing Card", "Fisher Golem", "Fisher golem", "A fisher golem works with a fishing rod"),
    "lamplighter": ("Lamplighter Card", "Lamplighter Golem", "Lamplighter golem", "A lamplighter golem needs no tool, just torches in storage"),
    "guard": ("Guarding Card", "Guard Golem", "Guard golem", "A guard golem fights with a sword or an axe"),
}
POINTS = {
    "cobblegen": "Click the block to mine, then its chest, then use the marker on the golem",
    "sifter": "Click the input chest, the sieve, then the output chest (it can be the same one)",
    "hammer": "Click the input chest, the spot to hammer on, then the output chest",
    "composter": "Click the input chest, the composter, then the output chest",
    "smelter": "Click the input chest, the fuel chest, the furnace, then the output chest",
}
NEEDS = {
    "sieve": "%s cannot find its sieve, mark it again", "mesh": "%s needs a mesh in the sieve",
    "spot": "%s needs its hammering spot clear", "composter": "%s cannot find its composter, mark it again",
    "furnace": "%s cannot find its furnace, mark it again", "fuel": "%s needs fuel in the fuel chest",
    "feed": "%s needs feed for its animals in storage", "bottles": "%s needs glass bottles in storage or shears",
    "torches": "%s needs torches in storage",
}
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


def shapeless(name, ingredients, result, count=1, mod=None):
    r = {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": ingredients, "result": {"id": f"{NS}:{result}", "count": count}}
    if mod:
        r = dict(conditions(mod), **r)
    write(f"{DATA}/recipe/{name}.json", r)


# only loaded with the other mod, on either loader
def conditions(mod):
    return {"neoforge:conditions": [{"type": "neoforge:mod_loaded", "modid": mod}],
            "fabric:load_conditions": [{"condition": "fabric:all_mods_loaded", "values": [mod]}]}


def recipes():
    shaped("golem_core", ["RLR", "LEL", "RLR"], {"R": "minecraft:redstone", "L": "minecraft:lapis_lazuli", "E": "minecraft:emerald"}, "golem_core")
    body = {"B": "minecraft:clay", "C": f"{NS}:golem_core", "b": "minecraft:clay_ball"}
    shaped("golem", [" B ", "BCB", "b b"], body, "golem")
    for j in JOBS:
        shaped(f"{j}_golem", [" B ", "BCB", "bKb"], dict(body, K=f"{NS}:{j}_card"), "golem", components={f"{NS}:job": j})
        shapeless(f"{j}_card", ["minecraft:paper", HINTS[j]], f"{j}_card", mod="exdeorum" if j in EXDEORUM else None)
    shaped("clay_cookie", ["#X#"], {"#": "minecraft:wheat", "X": "minecraft:clay_ball"}, "clay_cookie", count=4, category="food")
    shapeless("marker_stick", ["minecraft:stick", "minecraft:redstone", "minecraft:clay_ball"], "marker_stick")


LANG = {
    "itemGroup.simplegolems": "Simple Golems",
    "entity.simplegolems.golem": "Golem",
    "item.simplegolems.golem_core": "Golem Core",
    "item.simplegolems.golem": "Golem",
    "item.simplegolems.golem.blank": "Blank Golem",
    "item.simplegolems.clay_cookie": "Clay Cookie",
    "item.simplegolems.marker_stick": "Marker Stick",
    "tooltip.simplegolems.area": "Work area %s x %s x %s",
    "job.simplegolems.blank": "Blank golem",
    "message.simplegolems.not_yours": "This golem belongs to someone else",
    "message.simplegolems.has_card": "Take the card out first (sneak and use a stick)",
    "message.simplegolems.programmed": "Now a %s",
    "message.simplegolems.cookies_full": "The cookie tin is full",
    "message.simplegolems.fed": "%s clay cookies in the tin",
    "hud.simplegolems.cookies": "Cookies: %s/%s",
    "message.simplegolems.wrong_tool.blank": "A blank golem has no use for tools, give it a card first",
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
    "tooltip.simplegolems.marker.points": "%s blocks clicked",
    "message.simplegolems.marker.point": "Block %s marked at %s %s %s (sneak and use in the air to start again)",
    "message.simplegolems.marker.points_given": "%s blocks given to the golem",
    "message.simplegolems.status.no_exdeorum": "%s needs Ex Deorum installed",
    "message.simplegolems.status.disabled": "%s is turned off in the server config",
    "message.simplegolems.status.guard": "%s, guarding its area, health %s/%s, %s mobs beaten, about %s min of fighting power",
    "message.simplegolems.status.guard_follow": "%s, following you, health %s/%s, %s mobs beaten, about %s min of fighting power",
    "message.simplegolems.status.guard_down": "%s is down, back up in %s s",
    "message.simplegolems.guard.follow": "Guard golem following you",
    "message.simplegolems.guard.stay": "Guard golem guarding its area",
}
for j, (card, golem, job, tool) in NAMES.items():
    LANG[f"item.simplegolems.golem.{j}"] = golem
    LANG[f"item.simplegolems.{j}_card"] = card
    LANG[f"job.simplegolems.{j}"] = job
    LANG[f"message.simplegolems.wrong_tool.{j}"] = tool
for j, text in POINTS.items():
    LANG[f"message.simplegolems.marker.points_needed.{j}"] = text
for k, text in NEEDS.items():
    LANG[f"message.simplegolems.status.needs.{k}"] = text


def lang():
    write(f"{ASSETS}/lang/en_us.json", LANG)


# plain blocks a miner may place for steps and to seal water and lava: no ores, valuables, block entities or falling blocks
BUILDING = ["cobblestone", "cobbled_deepslate", "stone", "deepslate", "andesite", "diorite", "granite", "tuff",
            "dirt", "coarse_dirt", "netherrack", "blackstone", "basalt", "smooth_basalt", "calcite", "end_stone",
            "stone_bricks", "deepslate_bricks", "mossy_cobblestone", "dripstone_block", "sandstone", "red_sandstone",
            "mud_bricks", "packed_mud", "terracotta"]


EXD_BARRELS = ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "pale_oak", "bamboo", "crimson", "warped",
               "fir", "redwood", "mahogany", "jacaranda", "palm", "willow", "dead", "magic", "umbran", "hellbark", "archwood", "skyroot",
               "bluebright", "starlit", "frostbright", "comet", "lunar", "dusk", "maple", "stone", "crystallized"]


def tags():
    write(f"{DATA}/tags/item/miner_building_blocks.json", {"values": [f"minecraft:{b}" for b in BUILDING]})
    write(f"{DATA}/tags/block/lumber_logs.json", {"values": ["#minecraft:logs"]})
    write(f"{DATA}/tags/item/lumber_logs.json", {"values": ["#minecraft:logs"]})
    write(f"{DATA}/tags/item/lumber_saplings.json", {"values": ["#minecraft:saplings"]})
    write(f"{DATA}/tags/block/farmer_crops.json", {"values": ["#minecraft:crops"]})
    write(f"{DATA}/tags/block/miner_never_dig.json", {"values": []})
    # Ex Deorum barrels and machines offer an item inventory but are not storage
    exd = [f"exdeorum:{w}_barrel" for w in EXD_BARRELS] + ["exdeorum:mechanical_sieve", "exdeorum:mechanical_hammer"]
    write(f"{DATA}/tags/block/not_storage.json", {"values": [{"id": b, "required": False} for b in exd]})
    write(f"{DATA}/tags/item/hammers.json", {"values": [{"id": "#exdeorum:hammers", "required": False}, {"id": "#exdeorum:compressed_hammers", "required": False}]})
    write(f"{DATA}/tags/item/lamplighter_lights.json", {"values": ["minecraft:torch"]})
    write(f"{DATA}/tags/block/storage.json", {"values": []})
    write(f"{DATA}/tags/entity_type/guard_ignores.json", {"values": []})


items()
recipes()
lang()
tags()
