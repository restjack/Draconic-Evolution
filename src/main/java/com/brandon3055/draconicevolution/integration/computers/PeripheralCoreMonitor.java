package com.brandon3055.draconicevolution.integration.computers;

import com.brandon3055.draconicevolution.DEConfig;
import com.brandon3055.draconicevolution.blocks.tileentity.TileCoreMonitor;
import com.brandon3055.draconicevolution.blocks.tileentity.TileEnergyCore;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;

import java.util.HashMap;
import java.util.Map;

/**
 * eli.8 Core Monitor (Round C): CC: Tweaked peripheral "draconic_monitor". Values are the monitor's last copy of the
 * core (updated every second while the core's chunk is loaded); isLoaded() says whether they are live. Energy is in OP
 * (= RF = FE). setActive needs the core loaded and works like the core's own button; computers skip the owner check.
 */
public class PeripheralCoreMonitor implements IPeripheral {
    private final TileCoreMonitor tile;

    public PeripheralCoreMonitor(TileCoreMonitor tile) {
        this.tile = tile;
    }

    @Override
    public String getType() {
        return "draconic_monitor";
    }

    @Override
    public boolean equals(IPeripheral other) {
        return other instanceof PeripheralCoreMonitor o && o.tile == tile;
    }

    private void check() throws LuaException {
        if (!DEConfig.extra("coreMonitor")) throw new LuaException("Core Monitor is disabled in the server config");
        if (!tile.isLinked()) throw new LuaException("Monitor is not linked to a core");
    }

    @LuaFunction(mainThread = true)
    public final boolean isLinked() {
        return tile.isLinked();
    }

    @LuaFunction(mainThread = true)
    public final boolean isLoaded() {
        return tile.loaded.get();
    }

    @LuaFunction(mainThread = true)
    public final long getStored() throws LuaException { check(); return tile.stored.get(); }

    @LuaFunction(mainThread = true)
    public final long getCapacity() throws LuaException { check(); return tile.unlimited.get() ? -1 : tile.capacity.get(); }

    @LuaFunction(mainThread = true)
    public final double getPercent() throws LuaException { check(); return tile.fillPercent(); }

    @LuaFunction(mainThread = true)
    public final long getInput() throws LuaException { check(); return tile.input.get(); }

    @LuaFunction(mainThread = true)
    public final long getOutput() throws LuaException { check(); return tile.output.get(); }

    @LuaFunction(mainThread = true)
    public final long getNet() throws LuaException { check(); return tile.input.get() - tile.output.get(); }

    @LuaFunction(mainThread = true)
    public final int getTier() throws LuaException { check(); return tile.tier.get(); }

    @LuaFunction(mainThread = true)
    public final boolean isActive() throws LuaException { check(); return tile.coreActive.get(); }

    @LuaFunction(mainThread = true)
    public final boolean stabilizersValid() throws LuaException { check(); return tile.stabilizersOk.get(); }

    /** Seconds until full (net > 0) or empty (net < 0); -1 if not changing or unlimited. */
    @LuaFunction(mainThread = true)
    public final double getEta() throws LuaException {
        check();
        long net = tile.input.get() - tile.output.get();
        if (net == 0 || tile.unlimited.get()) return -1;
        long left = net > 0 ? tile.capacity.get() - tile.stored.get() : tile.stored.get();
        return left / (double) Math.abs(net) / 20D;
    }

    @LuaFunction(mainThread = true)
    public final Map<String, Object> getLimits() throws LuaException {
        check();
        Map<String, Object> m = new HashMap<>();
        m.put("maxInput", tile.limIn.get());
        m.put("maxOutput", tile.limOut.get());
        m.put("reserve", (int) tile.limReserve.get());
        return m;
    }

    /** Turns the core on or off. Returns true if it is now in the requested state. */
    @LuaFunction(mainThread = true)
    public final boolean setActive(boolean active) throws LuaException {
        check();
        TileEnergyCore core = tile.findCore();
        if (core == null) throw new LuaException("Core is not loaded");
        if (core.active.get() != active) core.toggleActivation();
        return core.active.get() == active;
    }
}
