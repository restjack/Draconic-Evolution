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

## Settings

Right-click a panel with an empty hand for its settings.
- **Show**: which lines the screen shows.
- **Core**: turn the core on or off, preview the next tier.
- **Pylons**: each pylon and its rate; click a mode to switch it.
- **Limits**: max in/out per tick and a reserve %.

## Look

Name the monitor, pick OP, RF, FE or J, set the warning and alarm colours, a target % for the timer, the background, the frame, and a small picture of the core on 2x2 and bigger screens.

Only the player who linked it (and operators) can change settings.

## Alerts

When the core drops below the alarm %, a stabilizer breaks or the core turns off, the owner gets a chat line, a toast and/or a sound; optionally their FTB Teams party too.

**Locate** puts the core's position in chat as a JourneyMap waypoint.

## Redstone

Each panel can give a signal: the fill level (0-15), on below the alarm %, on while full, or on while draining.

**Stats** shows the last hour's average and peaks, the lowest fill in 24 hours, and a graph over 5 minutes, 1 hour or 24 hours.

## Computers

With CC: Tweaked the monitor is a peripheral, **draconic_monitor**: getStored, getCapacity, getPercent, getInput, getOutput, getNet, getEta, getTier, isActive, isLoaded, getLimits and setActive(true/false).
