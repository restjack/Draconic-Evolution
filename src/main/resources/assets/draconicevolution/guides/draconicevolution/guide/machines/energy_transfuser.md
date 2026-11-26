---
navigation:
  title: "Energy Transfuser"
  parent: machines/index.md
  icon: draconicevolution:energy_transfuser
  position: 7
item_ids:
  - draconicevolution:energy_transfuser
---

# Energy Transfuser

A charger with four slots, one on each side (north, east, south and west), for items that hold energy. Right-click a side's slot to put an item in or take it out, or use the screen.

Energy goes in and out through the top and bottom. Each slot has a mode:
- **Charge**: fills the item.

<Recipe id="draconicevolution:machines/energy_transfuser" />

- **Discharge**: drains the item, out of the top and bottom and into charging slots.
- **Buffer**: both.
- **Disabled**: ignored.

## Automation

**Balanced mode** splits incoming energy evenly between the charging slots instead of filling one at a time.

Pipes and hoppers can insert and remove items on each side. A charging item can be pulled out once full, and a discharging one once empty, which makes simple auto-charging lines easy. Redstone control is set in the screen.
