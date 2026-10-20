# Simple Golems

Small clay helper golems that chop trees, farm, mine, carry and sweep for you.

## Making a golem

| Item | Recipe |
|---|---|
| Golem Core | amethyst, redstone, amethyst / lapis, diamond, lapis / emerald, redstone, emerald |
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

A golem made without a card is a blank golem. It follows its owner around and does no work.

## Using a golem

- Use the golem item on a block to place it. Whoever places it owns it.
- Sneak and use an empty hand on it to pick it up again. It keeps its job, colour, tool, cookies, carried items, area and name.
- Use a tool on it to hand it over. The tool wears like it would for a player. When it breaks the golem stops and waits for a new one.
- Tools work like they do for a player. The tier decides what can be harvested and how fast, and Efficiency, Unbreaking, Fortune and Silk Touch all apply. A miner with a stone pickaxe leaves diamond ore alone, and nothing short of diamond touches obsidian.
- Use clay cookies on it to fill its cookie tin (one stack). One cookie powers about 5 minutes of work. Walking around idle and following you cost nothing.
- Use a dye on it to colour it. All 16 dyes work.
- Use a name tag to name it.
- Use an empty hand on it to take what it is carrying, or to see its status if it carries nothing.
- Sneak and use a stick on it to take its card out. It becomes a blank golem. Use a card on a blank golem to give it a new job.

Only the owner (or someone in creative) can pick a golem up, take its card or change its area.

## Work area

A golem works in an area around the spot where it was placed: 8 blocks each way, 4 down and 8 up.

To set an area, use a Marker Stick on two blocks for opposite corners, then use it on the golem. An area can be at most 32 blocks each way. Holding the stick shows the marked area. Sneak and use the stick in the air to clear it. Sneak and use it on a golem to put the golem back on its default area.

Golems put what they collect in the nearest chest, barrel or shulker box in the area or right next to it (two blocks). Other containers can be added with the `simplegolems:storage` block tag. Without a container, or when every container is full, the golem keeps what it carries and waits.

## Jobs

- **Lumber**: fells trees that grow on dirt and still have natural leaves, so log houses are left alone. Breaks the logs and the leaves of that tree and replants a sapling from the drops or one lying at the stump. If none dropped it uses one it carries or one from storage in the area.
- **Farmer**: harvests fully grown crops and replants them for free. Works with any crop that uses the vanilla crop block or sits in `#minecraft:crops` with an age, so modded crops work too. Stems and tall crops are left alone.
- **Miner**: needs a marked area. Digs it out from the top down and leaves a staircase along the west edge to climb out. If the marked area is flat it digs 8 layers down from it. Skips containers and other block entities, blocks under them, blocks next to water or lava and blocks its pickaxe cannot harvest. Rests while the `mob_griefing` game rule is off.
- **Courier**: needs a marked area. Takes from the container nearest the first corner and fills the container nearest the second corner, one stack at a time.
- **Sweeper**: picks up any dropped item in its area.

Working golems pick up every drop in their area before moving on to the next tree, crop or block, taking one item type per trip to storage. Those drops do not despawn while the golem is at work.

## Thought bubbles

A golem that cannot get on with its job shows a small thought bubble above its head:

- a chest when there is no storage in its area or all of it is full. The golem stops and waits until there is room, so nothing gets spilled.
- a sapling crossed out when a lumber golem had no sapling to replant a stump.
- a clay cookie crossed out when it has run out of cookies.
- its tool crossed out when its tool broke or is missing. A golem first looks for a matching tool in storage in its area and takes the best one (highest tier, so iron beats gold), and only shows the bubble if there is none.

The bubble fades away once the problem is sorted out.

Golems do not despawn, do not attack, take no fall damage and drop their core, card, tool, cookies and carried items if they die.

Minecraft 26.1 to 26.1.2, NeoForge and Fabric.

Build with `./gradlew build`. The entity model is made from `tools/golem_b.json` by `python3 tools/gen_model.py`, item textures by `python3 tools/gen_textures.py`, the thought bubbles by `python3 tools/gen_bubbles.py` and the json files by `python3 tools/gen_resources.py`.
