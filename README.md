# Simple Golems

Small clay helper golems that chop trees, farm, mine, carry and sweep for you.

## Making a golem

| Item | Recipe |
|---|---|
| Golem Core | redstone, lapis, redstone / lapis, emerald, lapis / redstone, lapis, redstone |
| Golem | clay block on top, clay block, core, clay block in the middle, clay balls in the bottom corners |
| Job golem | the same, with a card between the two clay balls |
| Card | paper and a hint item (shapeless) |
| Clay Cookie | wheat, clay ball, wheat (makes 4) |
| Marker Stick | stick, redstone and a clay ball (shapeless) |

Cards and their hint items:

| Card | Hint | Golem | Tool |
|---|---|---|---|
| Lumber Card | any sapling | fells grown trees and replants | axe |
| Farming Card | wheat | harvests ripe crops and replants | hoe |
| Mining Card | cobblestone | digs out a marked area | pickaxe |
| Carrying Card | chest | moves items from one container to another | none |
| Sweeping Card | feather | picks up dropped items | brush or shovel (optional) |
| Cobblegen Card | stone | mines one block of a generator as it grows back | pickaxe |
| Sifting Card | string mesh (Ex Deorum) | sifts blocks in an Ex Deorum sieve | none |
| Hammering Card | wooden hammer (Ex Deorum) | hammers blocks with an Ex Deorum hammer | Ex Deorum hammer |
| Shepherd Card | any wool | shears sheep | shears |
| Ranching Card | lead | breeds animals up to a set count | none |
| Composting Card | composter | fills a composter, collects the bone meal | none |
| Smelting Card | furnace | runs a furnace, smoker or blast furnace | none |
| Beekeeping Card | honeycomb | harvests full beehives and bee nests | shears (optional) |
| Fishing Card | cod | fishes | fishing rod |
| Lamplighter Card | torch | puts torches in dark spots | none |

A golem made without a card is a blank golem. It follows its owner around and does no work.

## Using a golem

- Use the golem item on a block to place it. Whoever places it owns it.
- Sneak and use an empty hand on it to pick it up again. It keeps its job, colour, tool, cookies, carried items, area and name.
- Use a tool on it to hand it over. The tool wears like it would for a player. When it breaks the golem stops and waits for a new one.
- Tools work like they do for a player. The tier decides what can be harvested and how fast, and Efficiency, Unbreaking, Fortune and Silk Touch all apply. A miner with a stone pickaxe leaves diamond ore alone, and nothing short of diamond touches obsidian.
- Use clay cookies on it to fill its cookie tin (one stack). One cookie powers about 5 minutes of work. Walking around idle and following you cost nothing.
- Use a dye on it to colour it. All 16 dyes work.
- Use a name tag to name it.
- Use an empty hand on it to take everything it is carrying, or to see its status if it carries nothing.
- Sneak and use a stick on it to take its card out. It becomes a blank golem. Use a card on a blank golem to give it a new job.

Only the owner (or someone in creative) can pick a golem up, take its card or change its area.

Hold clay cookies and look at a golem to see how full its cookie tin is on the action bar, for example `Cookies: 12/64`.

## Work area

A golem works in an area around the spot where it was placed: 8 blocks each way, 4 down and 8 up.

To set an area, use a Marker Stick on the golem, then on two blocks for opposite corners; the action bar says which click comes next. Using it on two blocks first and then on the golem works too. An area can be at most 32 blocks across (see `maxAreaWidth` below). Miners and couriers can use an area of any height; other jobs use at most 32 blocks of it, from the bottom. Holding the stick shows the marked area. Sneak and use the stick in the air to clear it. Sneak and use it on a golem to put the golem back on its default area.

Golems put what they collect in the nearest chest, barrel or shulker box in the area or right next to it (two blocks). Storage blocks from other mods work too when they offer an item inventory through the loader's standard API (the NeoForge item handler capability or the Fabric Transfer API), for example Simple Barrels; vanilla furnaces, hoppers and other machines are never used. Without a container, or when every container is full, the golem keeps what it carries and waits.

## Jobs

- **Lumber**: fells trees that grow on dirt, grass, moss or mud and still have natural leaves, so log houses are left alone. It walks to the ground beside the stump, under low leaves if needed, and fells the whole tree from there; one that ends up on top of a tree chops its way down. Breaks the logs and the leaves of that tree and replants a sapling from the drops or one lying at the stump. If none dropped it uses one it carries or one from storage in the area. When a tree stands too high to reach and there is no way up, it builds steps of logs it carries (or fetches from storage in the area) with two blocks of headroom, climbs them, fells the tree, then walks back down breaking and collecting only the logs it placed. It never builds in another golem's area and only does this while `mob_griefing` is on.
- **Farmer**: harvests fully grown crops and replants them for free. Works with any crop that uses the vanilla crop block or sits in `simplegolems:farmer_crops` (by default `#minecraft:crops`) with an age, so modded crops work too. Stems and tall crops are left alone.
- **Miner**: needs a marked area. Digs it out from the top down and leaves a staircase that winds round the edge of the pit (straight down the west edge if the area is less than 3 blocks wide), with two blocks of headroom over every step. If the marked area is flat it digs all the way down to bedrock (bedrock itself stays); an area marked with a height keeps to that box. A chest at the top, or anywhere else in or next to the pit, counts as its storage at any depth, and it walks its own staircase to get there a few steps at a time. When it digs into a cave or ravine it builds the missing steps, and it seals water and lava in the pit, in its walls and at its rim as soon as they show up, using plain blocks such as cobblestone, cobbled deepslate, stone, dirt or netherrack (the `simplegolems:miner_building_blocks` item tag; never ores, valuables, block entities or falling blocks). It keeps a full stack of those blocks back (`blockReserve`) instead of putting it in storage, and fetches a stack from storage if it runs out. It only steps down one block at a time and keeps out of water, so it does not get stuck in a cave. If it ends up somewhere in the pit with no way back to its staircase, such as at the bottom of a hole or down a cave, it builds steps up to the staircase with those blocks, digging through the pit where it has to but never through the staircase or the pit walls. Skips containers and other block entities, blocks under them and blocks its pickaxe cannot harvest, so a chest in a niche in the pit stays put. Rests while the `mob_griefing` game rule is off.
- **Courier**: needs a marked area. Takes from the container nearest the first corner and fills the container nearest the second corner, up to four stacks per trip, and finds its way up or down a long staircase between them.
- **Sweeper**: picks up any dropped item in its area.

Golems have four carry slots plus their cookie jar. Working golems pick up every drop in their area before moving on to the next tree, crop or block, gather several kinds of item at once, and only head to storage when all four slots are full or there is nothing left to pick up or work on. Those drops do not despawn while the golem is at work. If the way to storage is blocked the golem keeps trying the path rather than giving up. Golems keep out of lava and fire and get out if they end up in it. A golem that ends up somewhere with no way back to its home or area, such as a sealed cave, digs a staircase back out (with its pickaxe if it is a miner, by hand otherwise), building missing steps and sealing water and lava on the way; it never breaks bedrock, containers or anything inside another golem's area, and stops digging as soon as it can walk back. A golem that falls into the void is put back on the nearest safe ground, or at home if there is none.

## Programs

Cobblegen, sifter, hammer, composter and smelter golems work between blocks you click with a Marker Stick instead of in an area. Use the stick on the golem, then click the blocks it asks for in the action bar, sneaking or not; chests and furnaces are selected, not opened, and a wrong kind of block is refused. The last click finishes the setup. Sneak and use the stick in the air to cancel.

- **Cobblegen** (2 clicks: the block to mine, then its chest): mines that one block each time it grows back, such as the cobblestone of a lava and water generator, and never breaks anything around it. Works with any block a pickaxe can take, so stone and basalt generators work too.
- **Sifter** (3 clicks: input chest, sieve, output chest; the output may be the same chest): needs Ex Deorum. Takes only blocks the sieve's mesh can sift, works the sieve like a player and carries the results to the output chest. It never puts in or takes out the mesh: with no mesh it waits and shows the supplies bubble, and the status says it needs a mesh. Works with sieves and compressed sieves, one at a time; Ex Deorum's `automated_sieves` setting is for machines and does not stop it.
- **Hammer** (3 clicks: input chest, the block to hammer on top of, output chest): needs Ex Deorum and one of its hammers (a compressed hammer for compressed blocks). Only takes blocks that have an Ex Deorum hammer recipe for that hammer, such as cobblestone, gravel, sand and dirt, places one on the spot and breaks it, so the results are exactly Ex Deorum's hammer drops.
- **Composter** (3 clicks: input chest, composter, output chest): fills the composter with anything that composts and takes the bone meal out.
- **Smelter** (4 clicks: input chest, fuel chest, furnace, output chest): keeps a furnace, smoker or blast furnace fed with things it can cook and with fuel, and empties the output. The fuel chest may be the input chest too; then burnable things such as logs are only used as fuel.
- **Shepherd**: shears grown sheep in its area and catches the wool as it falls, so none ends up over a fence; the wool goes to storage.
- **Rancher**: feeds pairs of grown animals in its area with feed from storage (wheat, seeds, carrots, whatever that animal eats) until there are `rancherMaxAnimals` of that kind.
- **Beekeeper**: harvests full beehives and bee nests in its area without letting out or angering the bees, as if they were smoked. With glass bottles from storage it fills honey bottles; with shears in hand it takes honeycomb.
- **Fisher**: stands at water in its area and fishes with its rod (the normal fishing loot, without treasure), taking every few catches to storage.
- **Lamplighter**: places torches from storage (and More Lighting's table lamps, floor lamps and floor lights when that mod is installed) on dark ground in its area (block light 0, where monsters can spawn) until every spot is lit.

A program that is missing what it works with (torches, feed, bottles, fuel, a mesh, its furnace or composter) shows a sack crossed out and says what it needs in its status. Cobblegen, sifter and hammer are the skyblock programs and can be turned off together with `skyblockPrograms`. The Ex Deorum cards and recipes only show when Ex Deorum is installed (it is NeoForge only for 26.1.2); without it everything else works as normal.

## Thought bubbles

A golem that cannot get on with its job shows a small thought bubble above its head:

- a chest when there is no storage in its area or all of it is full. The golem stops and waits until there is room, so nothing gets spilled.
- a sapling crossed out when a lumber golem had no sapling to replant a stump.
- a clay cookie crossed out when it has run out of cookies.
- a cobblestone crossed out when a miner has steps to build or water or lava to seal and no building blocks with it or in storage.
- a sack crossed out when a program has run out of what it works with, or when a lumber golem needs logs to build steps up to a tree and has none with it or in storage.
- its tool crossed out when its tool broke or is missing. A golem first looks for a matching tool in storage in its area and takes the best one (highest tier, so iron beats gold), and only shows the bubble if there is none.

Once the problem is sorted out, whether you handed it over or the golem fetched it, the bubble shows a little heart as a thank-you for about two seconds and then fades away.

## Config

The server settings are in `config/simplegolems.toml` (the same file on NeoForge and Fabric; clients do not need it). NeoForge reloads it while the game runs, Fabric reads it when the server starts. An older config file keeps working: missing settings are added with their defaults. The defaults are:

```
maxAreaWidth = 32        # largest marked area, in blocks across (4 to 128)
workSpeed = 1.0          # 2.0 digs, chops and harvests twice as fast
cookieTicks = 6000       # how long one clay cookie lasts (20 ticks = 1 second)
blockReserve = 64        # building blocks a miner keeps back (0 to 64)
enabledJobs = ["lumber", "farmer", "miner", "courier", "sweeper"]   # a golem with a job not listed waits
enabledPrograms = ["cobblegen", "sifter", "hammer", "shepherd", "rancher", "composter",
                   "smelter", "beekeeper", "fisher", "lamplighter"]   # the same for programs
voidRescue = true        # golems that fall into the void are put back on safe ground
digOut = true            # stuck golems dig their way back to their area
moddedStorage = true     # use other mods' storage blocks
skyblockPrograms = true  # cobblegen, sifter and hammer as a group
rancherMaxAnimals = 8    # a rancher stops breeding a kind of animal at this many in its area (2 to 64)
```

## Tags for data packs

- `simplegolems:storage` (block): extra blocks golems treat as storage, for a mod block that is a plain container.
- `simplegolems:not_storage` (block): blocks golems never put items in or take them from, even if they offer an item inventory (by default Ex Deorum's barrels and mechanical sieve and hammer).
- `simplegolems:miner_building_blocks` (item): blocks a miner builds steps and seals water and lava with.
- `simplegolems:miner_never_dig` (block): blocks a miner leaves alone (empty by default).
- `simplegolems:lumber_logs` (block and item): what a lumber golem fells and collects (defaults to `#minecraft:logs`).
- `simplegolems:lumber_saplings` (item): what it replants (defaults to `#minecraft:saplings`).
- `simplegolems:hammers` (item): what a hammer golem uses (defaults to Ex Deorum's hammers and compressed hammers).
- `simplegolems:lamplighter_lights` (item): what a lamplighter places (defaults to torches, plus More Lighting's floor-standing lights when installed). Anything in it is placed as a player would place it on the floor.
- `simplegolems:farmer_crops` (block): crops a farmer harvests besides the vanilla crop block types (defaults to `#minecraft:crops`).

All recipes are normal data pack recipes and show in any recipe viewer.

Golems do not despawn, do not attack, take no fall damage and drop their core, card, tool, cookies and all four carry slots if they die.

Minecraft 26.1 to 26.1.2, NeoForge and Fabric.

Build with `./gradlew build`. The entity model is made from `tools/golem_b.json` by `python3 tools/gen_model.py`, item textures by `python3 tools/gen_textures.py`, the thought bubbles by `python3 tools/gen_bubbles.py` and the json files by `python3 tools/gen_resources.py`.

## Changelog

- **1.1.3**: a miner never places a block in the headroom of its own staircase, and digs out any block blocking its way back up instead of spinning in place. Fixed a golem climbing out of a hole sometimes never noticing it was out, and a miner standing half over the top step it needs to build never moving aside to build it (both seen mostly on NeoForge).
