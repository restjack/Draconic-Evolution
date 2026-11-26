---
navigation:
  title: "Reactor Safety"
  parent: endgame/index.md
  icon: minecraft:tnt
  position: 4
---

# Reactor Safety

## What makes it explode

- The containment field reaches zero while the core is above 2,000 °C.
- A stabilizer is broken while the core is above 2,000 °C.
- Fuel conversion reaches 100%. With no draconium left the temperature climbs by itself and the reactor can no longer shut down, so the field fails in the end.

Once the status reads **Explosion Imminent**, nothing can stop it. Leave.

## The explosion

The stabilizers blow out first. The reactor then works out the blast and counts down: at least a minute, often a few. The crater radius depends on the fuel, from about 50 blocks for one ingot to about 350 for a full load.

Server owners can shrink it with **reactorExplosionScale** or replace it with a small blast using **disableLargeReactorBoom**. Ask the server admin before counting on either.

## Taking it apart

Only take apart a reactor that is **Offline**. Breaking a stabilizer while the core is between 350 and 2,000 °C sets off a smaller explosion that sprays lava. Below 350 °C the reactor just becomes invalid. Breaking an injector only unlinks it.

Above 2,000 °C, breaking a stabilizer counts as a field failure.

## Shutting down

Press **Shutdown**. Output stops at once, but the reaction keeps going until saturation passes 99%; then the temperature falls. Below 2,000 °C the status becomes **Cooling Down**, and about three minutes later, below 100 °C, **Offline**.

Keep feeding the field until fusion stops: a field that runs dry during shutdown still explodes. While it is still stopping you can press Activate to restart it.

## SAS (fail-safe)

The **SAS** button on the reactor screen is a semi-automated shutdown. While it is on and the reactor is online, it starts a shutdown by itself as soon as saturation reaches 99% with the core below 2,500 °C.

Pair it with redstone: when a comparator in Conversion mode shows the fuel is nearly spent, or anything else looks wrong, close the output Flow Gate. Saturation fills up and SAS shuts the reactor down for you.

## Good habits

- Keep a large buffer so the field never runs short, even during shutdown.
- Never let your base draw more from that buffer than the reactor makes; a third Flow Gate on the base supply helps.
- Refuel well before conversion reaches 100%.
- Keep the core at or below 8,000 °C.
- Test the whole setup in a copy of the world first.
