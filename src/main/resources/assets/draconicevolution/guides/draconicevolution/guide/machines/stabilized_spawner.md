---
navigation:
  title: "Stabilized Spawner"
  parent: machines/index.md
  icon: draconicevolution:stabilized_spawner
  position: 1
item_ids:
  - draconicevolution:stabilized_spawner
  - draconicevolution:mob_soul
---

# Stabilized Spawner

Right-click a vanilla mob spawner with any of the four [Cores](../getting_started/cores.md). The core is used up, and the spawner pops out as a **Stabilized Mob Spawner** of the same tier, set to the same mob.

To change the tier, right-click it with a different core; the old core drops out. To change the mob, right-click it with a **Soul** or a spawn egg.

A redstone signal turns it off. Breaking a placed spawner loses its mob.

## Tiers

- **Draconium**: up to 4 mobs every 10-40 s; needs a player within 24 blocks; normal spawn rules apply.
- **Wyvern**: up to 6 every 5-20 s; no player needed.
- **Draconic**: up to 8 every 2.5-10 s; ignores light and ground rules.
- **Chaotic**: up to 12 every 1.25-5 s; ignores spawn rules.

Mobs appear within 4 blocks. From Wyvern up they never despawn. A spawner stops while too many of its mob are nearby.

## Souls

<ItemImage id="draconicevolution:mob_soul" scale="2" />

A **Soul** sometimes drops when a player kills a mob with a **Reaper** weapon. The base chance is 1 in 1000 (1 in 800 for animals), divided by the weapon's Reaper level. Bosses drop no souls.

## Using souls

The **Reaper** enchantment goes up to V. Wyvern, Draconic and Chaotic swords, axes, bows and staffs count as Reaper 1, 2 and 3, added to any enchantment.

Example: a Draconic sword (Reaper 2) with Reaper V is level 7, so about 1 in 142 for monsters.

Besides setting a spawner, a Soul can spawn its mob directly: shift-right-click a block with it. This uses up the Soul.

Server owners can block mobs from dropping souls and from spawners with the **spawnerList** config.
