---
navigation:
  title: "Reading Your Core"
  parent: energy_core/index.md
  icon: minecraft:spyglass
  position: 4
---

# Reading Your Core

The core screen shows stored and maximum OP, energy coming **in** and going **out** per tick, the **net** rate, and how long until the core is full or empty at the current rate.

With **Jade** installed, looking at the core, a pylon sphere or a Stabilizer shows the same numbers without opening anything. Hold Shift for in/out and the time estimate.

## History and problems

The graph button next to the eye opens a panel with the net rate over the last 5 minutes, one bar every 5 seconds: green bars fill the core, red bars drain it.

While a core is not built correctly, the screen says how many blocks are wrong. Hover over that line to see where they are and what each spot needs.

## Effects and safety

The eye button on the core screen hides the core, stabilizer and pylon effects for you only; the blocks keep working. The client config value **blockEffectRenderDistance** limits how far away they are drawn.

**Never move a core with AE2 Spatial IO**: the core, stabilizers and pylons are marked as unmovable for that reason.
