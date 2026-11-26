---
navigation:
  title: "Energy Crystals"
  parent: energy_network/index.md
  icon: draconicevolution:basic_relay_crystal
  position: 1
item_ids:
  - draconicevolution:basic_relay_crystal
  - draconicevolution:crystal_binder
---

# Energy Crystals

Energy crystals move OP without cables. Each crystal has its own buffer, and linked crystals keep sharing energy until they are all at the same charge level, so energy flows from full crystals towards empty ones.

There are three kinds:
- [I/O crystals](../energy_network/io_crystals.md) sit on a block and move energy in or out.
- [Relay crystals](../energy_network/relay_crystals.md) are hubs with many links.

- [Wireless crystals](../energy_network/wireless_crystals.md) power machines from a distance.

## Linking

Hold a **Crystal Binder**. **Shift-right-click** a crystal to save it to the binder, then right-click another crystal to link the two. Doing the same on an already linked pair breaks the link. Shift-right-click the air to clear the binder.

Both crystals need a free link, the same dimension, and each must be within the other's range. Beams between crystals show energy moving; looking at a crystal shows its charge, links and mode.

## Tiers

- Basic: 4M OP buffer, 32 block range
- Wyvern: 16M OP, 64 blocks
- Draconic: 64M OP, 127 blocks

Range is a sphere around each crystal, and the shorter range of the two counts. A crystal can pass on its whole buffer every tick, so links are rarely the bottleneck. Link counts are on each crystal's page; higher tiers are crafted from lower ones (see JEI).

## Putting it together

A typical setup: an I/O crystal on each generator or on an [Energy Pylon](../energy_core/pylons.md), all linked to a relay crystal, and the relay linked to wireless crystals near your machines.

DE blocks that push energy, including crystals, can feed cables with no buffer (such as Powah's) in this port; older Brandon's Core jars could not.

<Recipe id="draconicevolution:tools/crystal_binder" />
