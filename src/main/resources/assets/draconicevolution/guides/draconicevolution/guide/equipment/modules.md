---
navigation:
  title: "The Module System"
  parent: equipment/index.md
  icon: draconicevolution:module_core
  position: 0
item_ids:
  - draconicevolution:module_core
  - draconicevolution:item_draconium_energy
---

# The Module System

Draconic tools, weapons, armour and capacitors are modular. Freshly crafted, they store no energy, and a tool without energy digs no faster than a bare hand. Upgrades are **modules** that you place in the item's grid.

<Recipe id="draconicevolution:modules/module_core" />

<Recipe id="draconicevolution:modules/item_draconium_energy" />

Every module starts from a Module Core. Look the rest up in JEI.

Press **Shift+C** (the Tool Modules key) to open the module screen for the modular items in your inventory. **C** (Tool Config) opens each item's settings, such as mining speed and AOE size.

## Installing

Drag a module from your inventory onto free space in the grid. Most modules are 1x1; some are 2x1, 2x2 or 3x3, and the energy link is 4x4.

A module only goes in if:
- its tier is the same as or lower than the item's,
- the item supports that type,
- there is room where you drop it, and

- the item is under that type's install limit (for example one AOE module, up to eight Speed modules).

## Tiers and grids

Modules come in four tiers: Draconium (no prefix in the name), Wyvern, Draconic and Chaotic. Gear comes in Wyvern, Draconic and Chaotic. Grid sizes by tier:
- Tools and bows: 4x4, 6x5, 8x6
- Staff of Power: 8x6, 10x8 (Draconic, Chaotic only)
- Chestpiece: 6x5, 8x6, 10x8
- Capacitor: 4x4, 5x5, 8x6

Server owners can change these in the DE config.

## Energy first

Install an energy module first: 1M, 4M, 16M or 64M OP from Draconium to Chaotic, and they add up. Tools use 256 OP per block mined and 1,024 OP per point of melee damage.

Charge gear in the [Generator](../energy_network/generator.md)'s charge slot, any FE charger, or a capacitor. The full module list is in [Module Types](../equipment/module_types.md).
