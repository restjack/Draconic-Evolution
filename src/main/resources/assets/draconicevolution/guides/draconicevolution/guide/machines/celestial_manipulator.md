---
navigation:
  title: "Celestial Manipulator"
  parent: machines/index.md
  icon: draconicevolution:celestial_manipulator
  position: 6
item_ids:
  - draconicevolution:celestial_manipulator
---

# Celestial Manipulator

Controls the weather and the time of day. It needs power and holds 8 million OP. Power comes in on any side, so an
I/O crystal on its face or a crystal that powers machines in an area can feed it.

**Weather** (power is taken when the weather changes, about 11 seconds after the click):
- Clear skies or start rain: 256,000 OP
- Start a thunderstorm: 384,000 OP

<Recipe id="draconicevolution:celestial_manipulator" />

**Time**: skip to sunrise, noon, sunset, moonrise, midnight or moonset, or skip 24 hours. Time is wound forward
quickly, not jumped, at 320 OP per tick of time, so a full day costs about 7.7 million OP. It needs the full cost
stored before it starts, speeds up and slows down smoothly, and Stop ends it early.

## The screen

The sky dial shows the time, the weather and, during a skip, where it is heading. Buttons with a red corner can't run
right now; hover one to see why and what it costs. Every click's result shows in the box at the bottom.

The column on the right (green = on, red corner = off):
- **Keep skies clear**: clears rain on its own when it starts.
- **Owner only**: only the owner (whoever placed it), their team and operators can use it.
- **Tell everyone**: posts who changed the time or weather in chat.
- **Quiet boom**: keeps the weather sound near the block.
- **Redstone**: which action a redstone signal starts (Off by default). In +24 Hours, the signal going off stops the skip.

## On a server

After each use there is a short cooldown for the whole dimension (60 seconds by default; operators skip it). A comparator
reads 15 while it runs, otherwise its charge.

It only changes the clock; machines and crops do not get extra ticks.

The recipe uses a **Dragon Egg** and a Wyvern Core.
