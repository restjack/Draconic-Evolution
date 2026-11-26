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

The **Core Monitor** is a screen that shows an energy core's numbers from anywhere, even another dimension.

Place panels facing the same way next to each other: up to 3 by 3 of them join into one bigger screen with more detail. The bottom-left panel draws it.

<Recipe id="draconicevolution:machines/core_monitor" />

## Linking

Take a **Crystal Binder**, Shift-right-click the core (or one of its stabilizers or pylons), then right-click any panel of the screen. All joined panels link at once.

A core far away only updates while its chunk is loaded; the screen then shows the last values it saw and how long ago.

## What it shows

Fill level with a bar, status (charging, draining, full, low, inactive, stabilizer problem), stored and capacity, energy in and out, the net rate and the time until full or empty. Colours turn yellow below 25% and red below 10%.

Servers can switch the monitor off with **coreMonitor** in the config.
