---
navigation:
  title: "Entity Detector"
  parent: machines/index.md
  icon: draconicevolution:entity_detector
  position: 4
item_ids:
  - draconicevolution:entity_detector
  - draconicevolution:entity_detector_advanced
---

# Entity Detector

The **Entity Detector** scans the area around it at a set interval and gives a redstone signal based on how many matching entities it finds.


- **Basic**: range 1-16 blocks, scans every 1.5 to 60 seconds, filters players and hostile mobs.

<Recipe id="draconicevolution:entity_detector" />

<Recipe id="draconicevolution:entity_detector_advanced" />

- **Advanced**: range up to 64 blocks, scans as often as every 0.25 seconds, and has the full filter: mob types, specific players, items and more.

Both store 512,000 OP and need power.

## Signal and cost

Each scan costs energy, and it rises steeply with range: about 4,000 OP at range 10 and 64,000 OP at range 64. Keep the range as small as the job allows.


- **RS Min / RS Max**: set the entity counts that give the weakest and strongest signal. Set both to the same number for a plain on/off signal once that many are found.

- **Pulse mode**: sends a short pulse after each scan instead of a steady signal.
