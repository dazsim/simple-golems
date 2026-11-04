#!/usr/bin/env python3
# writes the 1.21.1 and 1.20.1 json formats into src/eras from src/eras/26.1, run from repo root after gen_resources.py
import json, os, shutil

MOD = "simplegolems"
SRC = "src/main/resources"
NEW = "src/eras/26.1"
JOBS = ["blank", "lumber", "farmer", "miner", "courier", "sweeper", "cobblegen", "sifter", "hammer", "shepherd",
        "rancher", "composter", "smelter", "beekeeper", "fisher", "lamplighter", "guard"]


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as fh:
        json.dump(obj, fh, indent=2)
        fh.write("\n")


def ing(x):
    if isinstance(x, list):
        return [ing(v) for v in x]
    return {"tag": x[1:]} if x.startswith("#") else {"item": x}


def recipe(r):
    r = dict(r)
    conds = r.pop("neoforge:conditions", None)
    if conds:
        r["conditions"] = [{"type": "forge:mod_loaded", "modid": c["modid"]} for c in conds]
    if "key" in r:
        r["key"] = {k: ing(v) for k, v in r["key"].items()}
    if "ingredients" in r:
        r["ingredients"] = [ing(v) for v in r["ingredients"]]
    res = r["result"]
    out = {"item": res["id"], "count": res.get("count", 1)}
    comps = res.get("components")
    if comps:
        # vanilla 1.20.1 recipes drop result nbt, so these use our own serializer
        r["type"] = f"{MOD}:shaped_nbt"
        out["nbt"] = {MOD: {k.split(":", 1)[1]: v for k, v in comps.items()}}
    r["result"] = out
    return r


# 1.21.1 keeps the 26.1 results and conditions but wants ingredient objects
def recipe_121(r):
    r = dict(r)
    if "key" in r:
        r["key"] = {k: ing(v) for k, v in r["key"].items()}
    if "ingredients" in r:
        r["ingredients"] = [ing(v) for v in r["ingredients"]]
    return r


# golem item: layer0 body tinted by colour, layer1 face, layer2 job accent picked by the job property
def golem(job):
    t = {"layer0": f"{MOD}:item/golem_body", "layer1": f"{MOD}:item/golem_face"}
    if job != "blank":
        t["layer2"] = f"{MOD}:item/golem_accent_{job}"
    return {"parent": "minecraft:item/generated", "textures": t}


def golem_models(out):
    a = f"{out}/assets/{MOD}/models/item"
    m = golem("blank")
    m["overrides"] = [{"predicate": {f"{MOD}:job": round(i / 100, 2)}, "model": f"{MOD}:item/golem_{j}"} for i, j in enumerate(JOBS) if j != "blank"]
    write(f"{a}/golem.json", m)
    for j in JOBS[1:]:
        write(f"{a}/golem_{j}.json", golem(j))


def pack(out):
    with open(f"{out}/pack.mcmeta", "w") as fh:
        fh.write('{\n    "pack": {\n        "description": "${name}",\n        "pack_format": ${pack_format}\n    }\n}\n')


R = f"{NEW}/data/{MOD}/recipe"
T = f"{SRC}/data/{MOD}/tags"

OUT = "src/eras/1.20.1"
shutil.rmtree(OUT, ignore_errors=True)
for f in sorted(os.listdir(R)):
    write(f"{OUT}/data/{MOD}/recipes/{f}", recipe(json.load(open(f"{R}/{f}"))))
for kind, plural in [("block", "blocks"), ("item", "items"), ("entity_type", "entity_types")]:
    for f in sorted(os.listdir(f"{T}/{kind}")):
        write(f"{OUT}/data/{MOD}/tags/{plural}/{f}", json.load(open(f"{T}/{kind}/{f}")))
golem_models(OUT)
pack(OUT)

OUT = "src/eras/1.21.1"
shutil.rmtree(OUT, ignore_errors=True)
for f in sorted(os.listdir(R)):
    write(f"{OUT}/data/{MOD}/recipe/{f}", recipe_121(json.load(open(f"{R}/{f}"))))
golem_models(OUT)
pack(OUT)
