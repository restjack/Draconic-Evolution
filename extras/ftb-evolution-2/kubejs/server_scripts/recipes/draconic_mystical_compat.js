// priority: 0
// Added 2026-11-25 for the unofficial Draconic Evolution 26.1.2 port.
//
// Mystical Agriculture 9.0.11 ships ore_infusion/draconicevolution/draconium_ore with the
// result "draconicevolution:draconium_ore", an item ID Draconic Evolution no longer has,
// so the recipe fails to load. The current overworld ore is "overworld_draconium_ore".
// This is the same recipe (1 stone + 8 Draconium Essence) with the correct result.
// Remove this file once Mystical Agriculture fixes its recipe.

if (Platform.isLoaded("draconicevolution") && Platform.isLoaded("mysticalagriculture")) {
  ServerEvents.recipes((event) => {
    event
      .custom({
        type: "mysticalagriculture:ore_infusion",
        ingredients: [
          { ingredient: "minecraft:stone", count: 1 },
          { ingredient: "mysticalagriculture:draconium_essence", count: 8 },
        ],
        result: { id: "draconicevolution:overworld_draconium_ore", count: 1 },
      })
      .id("kubejs:mysticalagriculture/ore_infusion/draconium_ore");
  });
}
