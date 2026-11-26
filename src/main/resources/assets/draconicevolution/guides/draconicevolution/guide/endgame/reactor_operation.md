---
navigation:
  title: "Running the Reactor"
  parent: endgame/index.md
  icon: draconicevolution:awakened_draconium_block
  position: 3
item_ids:
  - draconicevolution:awakened_draconium_block
---

# Running the Reactor

## Fuel

While the reactor is **Offline**, open its screen and click fuel into the left slots: **Awakened Draconium** blocks, ingots or nuggets. It needs at least one ingot's worth and holds up to 8 blocks. More fuel means a bigger core, more output and a bigger explosion.

As fuel burns it turns into chaos, shown in the right slots as Chaos Fragments. Take those out when you refuel. You can also load fragments to start partly converted.

## Starting up

Press **Charge**, then feed energy into the injector. It fills the containment field to 50%, then the core's energy saturation to 50%, then heats the core to 2,000 °C. Once all three are reached, press **Activate**.

Start-up costs a lot: roughly 100 million OP for one block of fuel and about 760 million OP for a full 8-block load, most of it going into saturation.

## Online

While **Online**, the reactor generates into its own saturation and you pull energy out of the back of a stabilizer. The lower the saturation, the more it generates and the hotter it runs. Pull a little and it stays cool with low output; pull hard and both climb.

The containment field drains all the time, faster the harder you push. Keep it topped up through the injector. The screen's **Field Input Rate** is the exact OP/t needed to hold the field where it is now.

## Temperature

From 2,000 to 8,000 °C the field drain depends only on how hard you push. Above 8,000 °C it rises steeply with every extra degree, wasting energy and risking failure. Keep the core at or below 8,000 °C.

Fuel burns faster the hotter and less saturated the core is. As fuel converts, the maximum generation rate rises, from 0.4 times the base rate on fresh fuel to 3 times at full conversion. It also changes the heat balance, so keep watching the temperature and adjust.

## Regulating

Use two [Flow Gates](../energy_network/flow_gates.md):
- **Input**: from your energy store into the back of the injector, set to at least the Field Input Rate. Everything sent in is used, so do not overfeed.
- **Output**: from the back of a stabilizer to your store. A running stabilizer pushes all the energy the next block accepts, so without a gate it drains the reactor flat and it overheats.

Your profit is generation minus field input. Change output in small steps and let things settle.

## Redstone and computers

Holding a higher field strength costs more: the input needed rises sharply as the field nears 100%.

A comparator next to a stabilizer or injector reads the reactor. Open the screen from that part and pick its mode: temperature, field, saturation or fuel conversion, each with an inverted version. Use them to drive Flow Gates or shutdown logic.

With CC: Tweaked installed, stabilizers and injectors are **draconic_reactor** peripherals: getReactorInfo(), chargeReactor(), activateReactor(), stopReactor() and toggleFailSafe().
