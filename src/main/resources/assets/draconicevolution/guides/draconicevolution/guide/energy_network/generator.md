---
navigation:
  title: "Generator"
  parent: energy_network/index.md
  icon: draconicevolution:generator
  position: 0
item_ids:
  - draconicevolution:generator
---

# Generator

The **Generator** burns anything that works as furnace fuel and turns it into OP (1 OP = 1 FE). It has three fuel slots and one slot that charges an item.

<Recipe id="draconicevolution:machines/generator" />

It pushes energy into every block touching it, up to 32,000 OP/t, from a 100,000 OP buffer. When nothing takes the energy the buffer fills and burning stops, so no fuel is wasted. Output also tapers off once the buffer is about three quarters full.

## Modes

Pick a mode in the generator screen. Efficiency is how much energy each fuel item gives compared to Normal.
- Eco Plus: 5 OP/t, 500%
- Eco: 20 OP/t, 150%
- Normal: 40 OP/t, 100%
- Performance: 80 OP/t, 80%
- Overdrive: 300 OP/t, 50%

Example: one coal gives 16,000 OP in Normal and 80,000 OP in Eco Plus.

## Automation

Hoppers and pipes can insert fuel, and can pull out empty buckets and fully charged items. The redstone button in the screen sets how the generator reacts to a redstone signal.

In this port the generator can also push into cables that have no buffer of their own, such as Powah's. See [What's Different](../this_port/whats_different.md).
