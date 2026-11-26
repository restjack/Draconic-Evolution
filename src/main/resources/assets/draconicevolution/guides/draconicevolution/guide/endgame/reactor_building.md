---
navigation:
  title: "Building the Reactor"
  parent: endgame/index.md
  icon: draconicevolution:reactor_stabilizer
  position: 2
item_ids:
  - draconicevolution:reactor_stabilizer
  - draconicevolution:reactor_prt_stab_frame
  - draconicevolution:reactor_prt_focus_ring
  - draconicevolution:reactor_prt_in_rotor
  - draconicevolution:reactor_prt_out_rotor
  - draconicevolution:reactor_prt_rotor_full
  - draconicevolution:reactor_injector
---

# Building the Reactor

## Layout

Place the **Draconic Reactor Core** first. Then put four **Reactor Stabilizers** in a ring around it, all on one plane: north, south, east and west, or above, below and two sides. Each one must be in a straight line with the core, 4 to 7 blocks away, with nothing in between.

One of the two sides that are left takes a **Reactor Energy Injector**. You can put a second injector on the last side.

## Facing and linking

Reactor parts face the player who places them, like a piston. Stand on the core side of the spot when you place each one, so its front points at the core. A part that faces the core links by itself; right-click any linked part to open the reactor screen.

The screen shows **Invalid Setup** until all four stabilizers face the core. Energy goes into the back of the injector and comes out of the back of each stabilizer.

## Keep it clear

The core swells as it heats, to about 6 blocks across with a full load. Any block that ends up inside it slowly burns away. The 4-block minimum keeps the stabilizers and injectors outside that sphere; do not put anything else closer.

Plan the wiring for big numbers: a full-size reactor can move hundreds of thousands to millions of OP per tick. Use high-tier [Energy Crystals](../energy_network/crystals.md) or cables that can carry that much.

<Recipe id="draconicevolution:machines/reactor_prt_stab_frame" />

<Recipe id="draconicevolution:machines/reactor_prt_focus_ring" />

Stabilizer parts. JEI shows every recipe here too.

<Recipe id="draconicevolution:machines/reactor_prt_in_rotor" />

<Recipe id="draconicevolution:machines/reactor_prt_out_rotor" />

<Recipe id="draconicevolution:machines/reactor_prt_rotor_full" />

The rotor assembly combines both rotors.

## Reactor Stabilizer

### Fusion: <ItemLink id="draconicevolution:reactor_stabilizer" />

<Row><ItemImage id="draconicevolution:reactor_prt_stab_frame" scale="2" /><ItemImage id="draconicevolution:reactor_stabilizer" scale="2" /></Row>

Catalyst (core): <ItemLink id="draconicevolution:reactor_prt_stab_frame" />  
Injectors:

<ItemGrid><ItemIcon id="draconicevolution:draconic_energy_core" /><ItemIcon id="draconicevolution:reactor_prt_rotor_full" /><ItemIcon id="draconicevolution:reactor_prt_focus_ring" /><ItemIcon id="draconicevolution:awakened_draconium_ingot" /><ItemIcon id="draconicevolution:awakened_draconium_ingot" /><ItemIcon id="draconicevolution:chaotic_core" /><ItemIcon id="draconicevolution:awakened_draconium_ingot" /><ItemIcon id="draconicevolution:large_chaos_frag" /></ItemGrid>

Chaotic injectors, 16M OP

## Reactor Energy Injector

### Fusion: <ItemLink id="draconicevolution:reactor_injector" />

<Row><ItemImage id="draconicevolution:wyvern_core" scale="2" /><ItemImage id="draconicevolution:reactor_injector" scale="2" /></Row>

Catalyst (core): <ItemLink id="draconicevolution:wyvern_core" />  
Injectors:

<ItemGrid><ItemIcon id="draconicevolution:draconium_ingot" /><ItemIcon id="draconicevolution:reactor_prt_in_rotor" /><ItemIcon id="draconicevolution:reactor_prt_in_rotor" /><ItemIcon id="draconicevolution:reactor_prt_in_rotor" /><ItemIcon id="draconicevolution:draconium_ingot" /><ItemIcon id="draconicevolution:draconium_ingot" /><ItemIcon id="minecraft:iron_ingot" /><ItemIcon id="draconicevolution:reactor_prt_in_rotor" /><ItemIcon id="minecraft:iron_ingot" /><ItemIcon id="draconicevolution:draconium_ingot" /></ItemGrid>

Chaotic injectors, 16M OP
