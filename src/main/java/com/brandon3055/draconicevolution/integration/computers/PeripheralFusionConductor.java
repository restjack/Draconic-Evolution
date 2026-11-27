package com.brandon3055.draconicevolution.integration.computers;

import com.brandon3055.draconicevolution.DEConfig;
import com.brandon3055.draconicevolution.api.crafting.IFusionRecipe;
import com.brandon3055.draconicevolution.blocks.tileentity.TileFusionConductor;
import com.brandon3055.draconicevolution.blocks.tileentity.TileFusionCraftingCore;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.HashMap;
import java.util.Map;

/**
 * Fusion Conductor (unofficial-1.17): CC: Tweaked peripheral "fusion_conductor". Status, start one craft, recipe lock,
 * batch, auto-craft, stats. Energy is in OP (= RF = FE). Like the Core Monitor's, computers skip the owner check.
 */
public class PeripheralFusionConductor implements IPeripheral {
    private final TileFusionConductor tile;

    public PeripheralFusionConductor(TileFusionConductor tile) {
        this.tile = tile;
    }

    @Override
    public String getType() {
        return "fusion_conductor";
    }

    @Override
    public boolean equals(IPeripheral other) {
        return other instanceof PeripheralFusionConductor o && o.tile == tile;
    }

    private void check() throws LuaException {
        if (!DEConfig.extra("fusionConductor")) throw new LuaException("Fusion Conductor is disabled in the server config");
    }

    /** {status, crafting, state, progress, energy, capacity, speed, energyFactor, powerInjectors, autoCraft} */
    @LuaFunction(mainThread = true)
    public final Map<String, Object> getStatus() throws LuaException {
        check();
        Map<String, Object> m = new HashMap<>();
        TileFusionCraftingCore core = tile.getCore();
        m.put("status", tile.status.get() == null ? "" : tile.status.get().getString());
        m.put("hasCore", core != null);
        m.put("crafting", core != null && core.isCrafting());
        m.put("state", core != null && core.isCrafting() ? core.getFusionState().name().toLowerCase() : "idle");
        m.put("progress", (double) Math.max(0, tile.progress.get()));
        m.put("energy", tile.opStorage.getOPStored());
        m.put("capacity", tile.opStorage.getMaxOPStored());
        m.put("speed", TileFusionConductor.speed(tile.tier()));
        m.put("energyFactor", TileFusionConductor.energyFactor(tile.tier()));
        m.put("powerInjectors", tile.powerInjectors.get());
        m.put("autoCraft", tile.autoCraft.get());
        return m;
    }

    @LuaFunction(mainThread = true)
    public final boolean isCrafting() throws LuaException {
        check();
        TileFusionCraftingCore core = tile.getCore();
        return core != null && core.isCrafting();
    }

    /** Starts one craft as soon as a full set is in the input, even with auto-craft off or redstone waiting. */
    @LuaFunction(mainThread = true)
    public final void start() throws LuaException {
        check();
        tile.requestCraft();
    }

    /** Recipe id of the lock, or nil. */
    @LuaFunction(mainThread = true)
    public final String getLock() throws LuaException {
        check();
        return tile.recipeLock.get() == null ? null : tile.recipeLock.get().toString();
    }

    /** Lock to a recipe id ("draconicevolution:components/wyvern_core") or a result item id; nil or "" clears. Returns the lock set. */
    @LuaFunction(mainThread = true)
    public final String setLock(java.util.Optional<String> id) throws LuaException {
        check();
        String key = id.orElse("");
        if (key.isEmpty()) {
            tile.setLock(null);
            return null;
        }
        Identifier rid = Identifier.tryParse(key);
        if (rid == null) throw new LuaException("Not an id: " + key);
        for (RecipeHolder<IFusionRecipe> h : TileFusionConductor.recipes(tile.getLevel())) {
            if (h.id().identifier().equals(rid)) { tile.setLock(rid); return key; }
        }
        for (RecipeHolder<IFusionRecipe> h : TileFusionConductor.recipes(tile.getLevel())) {
            var result = h.value().getResultItem(tile.getLevel().registryAccess());
            if (net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(result.getItem()).equals(rid)) {
                tile.setLock(h.id().identifier());
                return h.id().identifier().toString();
            }
        }
        throw new LuaException("No fusion recipe with that id or result: " + key);
    }

    /** {batch, left}; batch 0 = no limit. */
    @LuaFunction(mainThread = true)
    public final Map<String, Object> getBatch() throws LuaException {
        check();
        return Map.of("batch", tile.batch.get(), "left", tile.batchLeft.get());
    }

    @LuaFunction(mainThread = true)
    public final void setBatch(int count) throws LuaException {
        check();
        tile.setBatch(count);
    }

    @LuaFunction(mainThread = true)
    public final void setAutoCraft(boolean on) throws LuaException {
        check();
        tile.autoCraft.set(on);
    }

    @LuaFunction(mainThread = true)
    public final void setPowerInjectors(boolean on) throws LuaException {
        check();
        tile.powerInjectors.set(on);
    }

    /** {crafts, energyUsed} since the block was placed. */
    @LuaFunction(mainThread = true)
    public final Map<String, Object> getStats() throws LuaException {
        check();
        return Map.of("crafts", tile.crafts.get(), "energyUsed", tile.energyUsed.get());
    }
}
