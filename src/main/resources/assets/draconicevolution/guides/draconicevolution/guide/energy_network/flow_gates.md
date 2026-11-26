---
navigation:
  title: "Flow Gates"
  parent: energy_network/index.md
  icon: draconicevolution:flux_gate
  position: 5
item_ids:
  - draconicevolution:flux_gate
  - draconicevolution:fluid_gate
---

# Flow Gates

The **Flux Gate** limits how much energy passes through it; the **Fluid Gate** does the same for fluids. Feed the back, take from the front. When you place a gate, its front (output) faces you.

Right-click it to set two rates:
- **Redstone Signal Low**: the flow with no signal.
- **Redstone Signal High**: the flow at signal strength 15.

<Recipe id="draconicevolution:flux_gate" />

<Recipe id="draconicevolution:fluid_gate" />

Signals in between give a rate in between. Type a number and press Apply.

## Notes

- The flux gate counts in RF/t, the same as OP/t or FE/t. The fluid gate counts in mB/t.
- The Flow line shows the rate in use right now.
- High may be lower than Low, so a gate can also close when powered: set High to 0.
- A **Potentiometer** or a comparator makes a handy dial.
- With CC: Tweaked installed a computer can take over the gate; the screen then shows Overridden.
