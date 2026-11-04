#!/usr/bin/env python3
# writes item models, recipes and tags, run from repo root
# en_us.json and lamplighter_lights.json are edited by hand, this leaves them alone
import json, os

RES = "src/main/resources"
ASSETS = f"{RES}/assets/simplegolems"
DATA = f"{RES}/data/simplegolems"
# formats that changed between versions go to src/eras, gen_eras.py writes the older ones from these
ERA = "src/eras/26.1"
NS = "simplegolems"

JOBS = ["lumber", "farmer", "miner", "courier", "sweeper", "cobblegen", "sifter", "hammer", "shepherd", "rancher", "composter",
        "smelter", "beekeeper", "fisher", "lamplighter", "guard"]
HINTS = {"lumber": "#minecraft:saplings", "farmer": "minecraft:wheat", "miner": "minecraft:cobblestone",
         "courier": "minecraft:chest", "sweeper": "minecraft:feather", "cobblegen": "minecraft:stone", "sifter": "exdeorum:string_mesh",
         "hammer": "exdeorum:wooden_hammer", "shepherd": "#minecraft:wool", "rancher": "minecraft:lead", "composter": "minecraft:composter",
         "smelter": "minecraft:furnace", "beekeeper": "minecraft:honeycomb", "fisher": "minecraft:cod", "lamplighter": "minecraft:torch",
         "guard": "minecraft:iron_sword"}
EXDEORUM = ["sifter", "hammer"]
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
        write(f"{ERA}/assets/simplegolems/items/{n}.json", {"model": plain(n)})
    for n in ["golem_body", "golem_face"] + [f"golem_accent_{j}" for j in JOBS]:
        flat_model(n)

    def body(colour):
        return {"type": "minecraft:model", "model": f"{NS}:item/golem_body", "tints": [{"type": "minecraft:constant", "value": (0xFF000000 | colour) - (1 << 32)}]}

    write(f"{ERA}/assets/simplegolems/items/golem.json", {"model": {"type": "minecraft:composite", "models": [
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
    write(f"{ERA}/data/simplegolems/recipe/{name}.json", r)


def shapeless(name, ingredients, result, count=1, mod=None):
    r = {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": ingredients, "result": {"id": f"{NS}:{result}", "count": count}}
    if mod:
        r = dict(conditions(mod), **r)
    write(f"{ERA}/data/simplegolems/recipe/{name}.json", r)


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
    write(f"{DATA}/tags/block/storage.json", {"values": []})
    write(f"{DATA}/tags/entity_type/guard_ignores.json", {"values": []})


items()
recipes()
tags()
