#!/usr/bin/env python3
# writes the 1.20.1 json formats into src/legacy/1.20.1 from src/main/resources, run from repo root
import json, os, shutil

MOD = "simplegolems"
SRC = "src/main/resources"
OUT = "src/legacy/1.20.1"
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
        r["fabric:load_conditions"] = [{"condition": "fabric:all_mods_loaded", "values": [c["modid"] for c in conds]}]
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


shutil.rmtree(OUT, ignore_errors=True)

D = f"{SRC}/data/{MOD}"
for f in sorted(os.listdir(f"{D}/recipe")):
    write(f"{OUT}/data/{MOD}/recipes/{f}", recipe(json.load(open(f"{D}/recipe/{f}"))))
for kind, plural in [("block", "blocks"), ("item", "items"), ("entity_type", "entity_types")]:
    for f in sorted(os.listdir(f"{D}/tags/{kind}")):
        write(f"{OUT}/data/{MOD}/tags/{plural}/{f}", json.load(open(f"{D}/tags/{kind}/{f}")))

# golem item: layer0 body tinted by colour, layer1 face, layer2 job accent picked by the job property
A = f"{OUT}/assets/{MOD}/models/item"
def golem(job):
    t = {"layer0": f"{MOD}:item/golem_body", "layer1": f"{MOD}:item/golem_face"}
    if job != "blank":
        t["layer2"] = f"{MOD}:item/golem_accent_{job}"
    return {"parent": "minecraft:item/generated", "textures": t}
m = golem("blank")
m["overrides"] = [{"predicate": {f"{MOD}:job": round(i / 100, 2)}, "model": f"{MOD}:item/golem_{j}"} for i, j in enumerate(JOBS) if j != "blank"]
write(f"{A}/golem.json", m)
for j in JOBS[1:]:
    write(f"{A}/golem_{j}.json", golem(j))
