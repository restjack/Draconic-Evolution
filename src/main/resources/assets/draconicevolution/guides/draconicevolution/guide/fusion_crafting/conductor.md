---
navigation:
  title: "Fusion Conductor"
  parent: fusion_crafting/index.md
  icon: draconicevolution:fusion_conductor
  position: 3
item_ids:
  - draconicevolution:fusion_conductor
---

# Fusion Conductor

The **Fusion Conductor** automates a fusion setup. Place it directly on top of a **Fusion Crafting Core**.

Pipes, hoppers, AE2 or Refined Storage put items into its input; finished items come out of its output. Without a conductor the core still works by hand.

## Fusion Conductor

### Fusion: <ItemLink id="draconicevolution:fusion_conductor" />

<Row><ItemImage id="draconicevolution:wyvern_relay_crystal" scale="2" /><ItemImage id="draconicevolution:fusion_conductor" scale="2" /></Row>

Catalyst (core): <ItemLink id="draconicevolution:wyvern_relay_crystal" />  
Injectors:

<ItemGrid><ItemIcon id="draconicevolution:wyvern_core" /><ItemIcon id="minecraft:gold_block" /><ItemIcon id="draconicevolution:basic_crafting_injector" /><ItemIcon id="draconicevolution:wyvern_core" /><ItemIcon id="minecraft:gold_block" /><ItemIcon id="draconicevolution:basic_crafting_injector" /></ItemGrid>

Wyvern injectors, 32M OP

## How it crafts

It picks the first recipe it can fully load: catalyst, every ingredient, and enough empty injectors of a high enough tier. It uses the lowest tier that works, so Chaotic injectors stay free for Chaotic recipes.

When the craft is done the result goes to the output, and leftovers go back.

## Power

With **Power injectors** on (the lightning button) it charges the injectors itself from its energy buffer, with purple beams. Feed it OP, RF or FE from any side.

Turn it off to use your own crystals or pylons on the injectors instead.

## Capacitor upgrade

Put a Draconic capacitor in the slot under the energy bar:
- Wyvern: 2x speed, 4x energy
- Draconic: 4x speed, 16x energy
- Chaotic: 8x speed, 64x energy

The capacitor is not drained. Low power only slows the craft down; it never cancels.

## Controls

- **Recipe lock**: click the slot holding the item you want, or use JEI's +. Right-click clears.
- **Batch**: craft this many, then stop.
- **Redstone**: ignore, run while powered, while not powered, or one craft per pulse.

## Live view

The map beside the status shows the core and its injectors from above. Green: loaded by the conductor. Red: an item put there by hand, which stops the conductor until you take it out. Grey: empty.

The core's auto-craft, push output, show injectors and 1/64 buttons move to the conductor.

## More

A comparator reads 1-14 while it crafts. With CC: Tweaked it is the peripheral **fusion_conductor**: status, start, lock, batch, stats.

Only the player who placed it, their team and operators can change its settings. Breaking it drops everything, including a craft in progress.
