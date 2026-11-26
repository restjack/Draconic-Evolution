---
navigation:
  title: "Fusion Crafting"
  parent: fusion_crafting/index.md
  icon: draconicevolution:crafting_core
  position: 0
item_ids:
  - draconicevolution:crafting_core
---

# Fusion Crafting

**Fusion Crafting** is a multiblock crafter that uses energy to merge items. Most advanced Draconic Evolution items are made this way.

A fusion recipe has three parts:
- a **catalyst**, placed in the **Fusion Crafting Core**
- **ingredients**, one per [Injector](../fusion_crafting/injectors.md)
- **energy**, fed into the injectors

<Recipe id="draconicevolution:machines/crafting_core" />

The Fusion Crafting Core. See [Setup](../fusion_crafting/setup.md) for where the injectors go.

JEI shows every fusion recipe with its tier and energy cost.

## Running a craft

Put the catalyst in the core and the ingredients in the injectors. Open the core: with a valid recipe it shows the result and a start button. A redstone signal on the core also starts the craft.

A craft runs in two stages:
- **Charging**: each filled injector stores its share of the energy.

- **Crafting**: the items fly into the core, and the result appears in the core's output slot.

Every filled injector must be part of the recipe. A spare item in any linked injector means no recipe is found.

## Energy and time

The recipe's energy is split evenly between the ingredients. Each injector charges no faster than its tier allows: about 15 s for Draconium, 11 s for Wyvern, 7 s for Draconic and 3 s for Chaotic injectors. Feeding power faster does not speed it up.

The crafting stage takes as long as the **lowest tier** injector in use: 15, 11, 7 or 3 seconds.

Any OP or FE source works, for example [energy crystals](../energy_network/crystals.md) pointed at the injectors.

## Redstone and automation

A comparator on the core reads its state:
- 0: no recipe, output empty
- 1: valid recipe, ready
- 2 to 3: charging or crafting
- 15: finished item waiting in the output

For automation, set the injectors to single item mode (see [Injectors](../fusion_crafting/injectors.md)), push the catalyst into the core and the ingredients into the injectors, pulse redstone, and pull the result out of the core.
