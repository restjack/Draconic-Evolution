---
navigation:
  title: "Core Monitor"
  parent: energy_core/index.md
  icon: draconicevolution:core_monitor
  position: 5
item_ids:
  - draconicevolution:core_monitor
---

# Core Monitor

The **Core Monitor** shows an energy core's numbers from anywhere, even another dimension.

Panels facing the same way join into one screen, up to 4 by 4. Bigger screens show more: a graph from 3 by 2, stat cards and a live picture of the core on 3 by 3 and 4 by 4.

<Recipe id="draconicevolution:machines/core_monitor" />

## Linking

Take a **Crystal Binder**, Shift-right-click the core (or a stabilizer or pylon), then right-click any panel. Panels added later join on their own.

A far core only updates while its chunk is loaded; the screen then shows the last values and how long ago.

## Settings

Right-click a panel with an empty hand.
- **Display**: what the screen shows, unit, warning and alarm levels, graph range, background.
- **Control**: turn the core on or off; preview the next tier.
- **Pylons**: each pylon, its rate and mode, and rate limits.

## More settings

- **Options**: name, ETA target, alerts (chat, toast, sound, team; switching one on sends a TEST), redstone output, locate.
- **Stats**: averages, peaks and a graph over 5 minutes, 1 hour or 24 hours.

Only the player who linked it and operators can change settings.

## Finding things

The pin next to a pylon, and **Locate core**, outline that block through walls for 30 seconds (click again to stop) and put its position in chat as a JourneyMap waypoint.

With CC: Tweaked the monitor is the peripheral **draconic_monitor**.

## Redstone

Each panel can give a signal: the fill level (0-15), on below the alarm level, on while full, or on while draining.
