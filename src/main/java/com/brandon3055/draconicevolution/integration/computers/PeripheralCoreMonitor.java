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

    /** The panel that polls the core (only it has live numbers); a computer may touch any panel of the screen. */
    private TileCoreMonitor t() {
        return tile.getLevel() == null ? tile : tile.originPanel();
    }

    private void check() throws LuaException {
        if (!DEConfig.extra("coreMonitor")) throw new LuaException("Core Monitor is disabled in the server config");
        if (!t().isLinked()) throw new LuaException("Monitor is not linked to a core");
    }

    @LuaFunction(mainThread = true)
    public final boolean isLinked() {
        return t().isLinked();
    }

    @LuaFunction(mainThread = true)
    public final boolean isLoaded() {
        return t().loaded.get();
    }

    @LuaFunction(mainThread = true)
    public final long getStored() throws LuaException { check(); return t().stored.get(); }

    @LuaFunction(mainThread = true)
    public final long getCapacity() throws LuaException { check(); return t().unlimited.get() ? -1 : t().capacity.get(); }

    @LuaFunction(mainThread = true)
    public final double getPercent() throws LuaException { check(); return t().fillPercent(); }

    @LuaFunction(mainThread = true)
    public final long getInput() throws LuaException { check(); return t().input.get(); }

    @LuaFunction(mainThread = true)
    public final long getOutput() throws LuaException { check(); return t().output.get(); }

    @LuaFunction(mainThread = true)
    public final long getNet() throws LuaException { check(); return t().input.get() - t().output.get(); }

    @LuaFunction(mainThread = true)
    public final int getTier() throws LuaException { check(); return t().tier.get(); }

    @LuaFunction(mainThread = true)
    public final boolean isActive() throws LuaException { check(); return t().coreActive.get(); }

    @LuaFunction(mainThread = true)
    public final boolean stabilizersValid() throws LuaException { check(); return t().stabilizersOk.get(); }

    /** Seconds until full (net > 0) or empty (net < 0); -1 if not changing or unlimited. */
    @LuaFunction(mainThread = true)
    public final double getEta() throws LuaException {
        check();
        long net = t().input.get() - t().output.get();
        if (net == 0 || t().unlimited.get()) return -1;
        long left = net > 0 ? t().capacity.get() - t().stored.get() : t().stored.get();
        return left / (double) Math.abs(net) / 20D;
    }

    @LuaFunction(mainThread = true)
    public final Map<String, Object> getLimits() throws LuaException {
        check();
        Map<String, Object> m = new HashMap<>();
        m.put("maxInput", t().limIn.get());
        m.put("maxOutput", t().limOut.get());
        m.put("reserve", (int) t().limReserve.get());
        return m;
    }

    /** Turns the core on or off. Returns true if it is now in the requested state. */
    @LuaFunction(mainThread = true)
    public final boolean setActive(boolean active) throws LuaException {
        check();
        TileEnergyCore core = t().findCore();
        if (core == null) throw new LuaException("Core is not loaded");
        if (core.active.get() != active) core.toggleActivation();
        return core.active.get() == active;
    }
}
