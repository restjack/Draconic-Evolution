---
navigation:
  title: "Chaos Guardian"
  parent: endgame/index.md
  icon: draconicevolution:chaos_shard
  position: 0
item_ids:
  - draconicevolution:chaos_shard
  - draconicevolution:large_chaos_frag
  - draconicevolution:medium_chaos_frag
---

# Chaos Guardian

Chaos Islands are floating islands of end stone and obsidian out in the End. Each hides a **Chaos Crystal** in an obsidian chamber at its heart, and each is guarded by the **Chaos Guardian**, a far tougher cousin of the Ender Dragon. Beating it is the only way to get **Chaos Shards**, which every chaotic-tier item and the [Draconic Reactor](../endgame/reactor.md) need.

**Untested in this port.** Upstream has open guardian bugs, such as a crash when right-clicking a Guardian Crystal (reported from FTB Evolution). Back up the world before you fight.

## Where to find one

Islands sit on a grid: every point where both X and Z are multiples of the config value **chaosIslandSeparation** (default 10,000), except the main island at 0, 0. Each island is centred on its grid point with the crystal at Y 80, and normal End terrain is cleared for about 770 blocks around it.

On this server the value is **2000**, which puts eight islands inside the ±2,500 world border: (±2000, 0), (0, ±2000) and the four corners (±2000, ±2000).

## Starting the fight

Nothing happens until a player walks to the middle of the island, just above the Chaos Crystal. Then 14 obelisks rise in a ring about 90 blocks out and 40 blocks above the crystal, each topped with a **Guardian Crystal**, and the Guardian appears high above the centre.

The boss bar shows its health, its shield (the bar is purple while the shield holds) and how many crystals are left. Everyone within about 200 blocks of the centre is in the fight.

The Chaos Guardian. It is immune to fire and to harmful potion effects.

## Guardian Crystals

While any crystal survives, the Guardian takes no damage and its shield refills within seconds. Normal attacks do nothing to the crystals either.

A crystal becomes **unstable** for 10 seconds when one of the Guardian's own attacks hits it, and only then do your hits count. Lure its fireballs and beam into a crystal, then strike. Chaotic-tier weapons destabilise crystals by themselves (server config). Each crystal has a 512-point shield that refills over a minute, and it explodes when broken.

## Shield and health

Once every crystal is gone, the Guardian's 16,000-point shield stops refilling. Break it, then wear down 1,000 health. One hit removes at most 500 shield or 100 health, and a hit no bigger than the last one is ignored for a quarter of a second, so heavy, steady hits beat many small ones. Head hits do full damage; body hits about a quarter. Only player and explosion damage reaches its health.

The longer the fight and the more damage it takes, the angrier it gets, and it uses its beam and bombardments more often.

## Its attacks

- Its fireballs, beam and charge ignore normal armour and resistance. A charge hits for 150.
- The laser beam is stopped by solid blocks. At full power it kills outright; only the protection window of a triggered Undying module saves you.

- A long charge-up summons 8 to 13 **Guardian Withers**, pulls players back toward the centre and switches off flight and Elytra gliding high above the island. Kill the withers to cut it short.
- It heals slowly while near a living crystal.

## What to bring

- Draconic armour with a shield. It is the only protection that works here, though guardian damage drains it three times faster than normal hits.
- Lots of energy to keep that shield and your gear charged.
- A strong bow or weapon: aim for the head.
- Blocks to hide behind while the beam charges.
- An Undying module.

See [Modules](../equipment/modules.md). Upstream reports stuck or doubled guardians after a server restart mid-fight, so finish a fight once you start it.

## Rewards

The Guardian drops about 24,000 experience and a **Dragon Heart**, which floats 20 blocks above the Chaos Crystal. Only after the Guardian dies can the **Chaos Crystal** be broken; it drops 5 **Chaos Shards** (server config). Until you break it, lightning strikes around it.

Older versions made the island collapse when the crystal broke. That implosion is not implemented in 3.1.4, so the island stays.

<Recipe id="draconicevolution:decompress/large_chaos_frag" />

<Recipe id="draconicevolution:decompress/medium_chaos_frag" />

A shard splits into 9 Large Chaos Fragments, each of those into 9 Small, then 9 Tiny. Recipes use every size.

## Fighting again

Each island has one Guardian, and it does not come back by itself. An operator can run **/respawn_draconic_guardian** within 60 blocks of an island's Chaos Crystal, then walk to the centre to start a new fight. The crystal has to still be there, so a second fight is only possible before it is broken.
