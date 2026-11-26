package com.brandon3055.draconicevolution.blocks.tileentity;

import com.brandon3055.brandonscore.blocks.TileBCore;
import com.brandon3055.brandonscore.lib.datamanager.*;
import com.brandon3055.draconicevolution.DEConfig;
import com.brandon3055.draconicevolution.blocks.machines.CoreMonitor;
import com.brandon3055.draconicevolution.init.DEContent;
import com.brandon3055.draconicevolution.lib.LoadedLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * eli.8: Core Monitor panel. Every second the server copies the linked core's numbers into synced fields, so clients
 * near the monitor see them even when the core is in another dimension. If the core's chunk is not loaded the last
 * values stay and {@link #loaded} goes false; the monitor never loads chunks itself.
 */
public class TileCoreMonitor extends TileBCore {
    public static final int MAX_SIZE = 3;

    public final ManagedString linkDim = register(new ManagedString("link_dim", "", DataFlags.SAVE_NBT_SYNC_TILE));
    public final ManagedPos linkPos = register(new ManagedPos("link_pos", (BlockPos) null, DataFlags.SAVE_NBT_SYNC_TILE));
    public final ManagedLong stored = register(new ManagedLong("stored", DataFlags.SAVE_NBT_SYNC_TILE));
    public final ManagedLong capacity = register(new ManagedLong("capacity", DataFlags.SAVE_NBT_SYNC_TILE));
    public final ManagedLong input = register(new ManagedLong("input", DataFlags.SAVE_NBT_SYNC_TILE));
    public final ManagedLong output = register(new ManagedLong("output", DataFlags.SAVE_NBT_SYNC_TILE));
    public final ManagedByte tier = register(new ManagedByte("tier", DataFlags.SAVE_NBT_SYNC_TILE));
    public final ManagedBool coreActive = register(new ManagedBool("core_active", DataFlags.SAVE_NBT_SYNC_TILE));
    public final ManagedBool stabilizersOk = register(new ManagedBool("stabilizers_ok", DataFlags.SAVE_NBT_SYNC_TILE));
    public final ManagedBool unlimited = register(new ManagedBool("unlimited", DataFlags.SAVE_NBT_SYNC_TILE));
    public final ManagedBool loaded = register(new ManagedBool("loaded", DataFlags.SAVE_NBT_SYNC_TILE));
    public final ManagedLong lastSeen = register(new ManagedLong("last_seen", DataFlags.SAVE_NBT_SYNC_TILE));

    private int tick;

    public TileCoreMonitor(BlockPos pos, BlockState state) {
        super(DEContent.TILE_CORE_MONITOR.get(), pos, state);
    }

    public boolean isLinked() {
        return linkPos.notNull() && !linkDim.get().isEmpty();
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide() || !isLinked() || tick++ % 20 != 0) return;
        if (!DEConfig.extra("coreMonitor")) return;
        TileEnergyCore core = findCore();
        if (core == null) {
            loaded.set(false);
            return;
        }
        loaded.set(true);
        lastSeen.set(level.getGameTime());
        tier.set(core.tier.get());
        coreActive.set(core.active.get());
        stabilizersOk.set(core.stabilizersValid.get());
        unlimited.set(core.energy.isUnlimited());
        stored.set(core.energy.getOPStored());
        capacity.set(core.energy.getMaxOPStored());
        var io = core.energy.getIOInfo();
        input.set(io == null ? 0 : io.currentInput());
        output.set(io == null ? 0 : io.currentOutput());
    }

    /** The linked core if its chunk is loaded right now; never loads anything. */
    @Nullable
    public TileEnergyCore findCore() {
        if (!(level instanceof ServerLevel sl) || !isLinked()) return null;
        ServerLevel target = sl.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, Identifier.parse(linkDim.get())));
        if (target == null || !LoadedLookup.isLoaded(target, linkPos.get())) return null;
        BlockEntity be = LoadedLookup.blockEntity(target, linkPos.get());
        return be instanceof TileEnergyCore core ? core : null;
    }

    /** Links this panel and every connected panel facing the same way (one screen) to the core. */
    public int linkScreen(GlobalPos core) {
        Set<TileCoreMonitor> panels = connectedPanels(level, worldPosition);
        for (TileCoreMonitor p : panels) {
            p.linkDim.set(core.dimension().identifier().toString());
            p.linkPos.set(core.pos());
            p.loaded.set(false);
            p.tick = 0;
        }
        return panels.size();
    }

    public static Set<TileCoreMonitor> connectedPanels(Level level, BlockPos start) {
        Set<TileCoreMonitor> found = new HashSet<>();
        if (!(level.getBlockEntity(start) instanceof TileCoreMonitor first)) return found;
        Direction facing = first.getBlockState().getValue(CoreMonitor.FACING);
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        Set<BlockPos> seen = new HashSet<>();
        while (!queue.isEmpty() && found.size() < MAX_SIZE * MAX_SIZE * 4) {
            BlockPos p = queue.poll();
            if (!seen.add(p)) continue;
            if (!(level.getBlockEntity(p) instanceof TileCoreMonitor m) || m.getBlockState().getValue(CoreMonitor.FACING) != facing) continue;
            found.add(m);
            for (Direction d : new Direction[]{Direction.UP, Direction.DOWN, facing.getClockWise(), facing.getCounterClockWise()}) {
                queue.add(p.relative(d));
            }
        }
        return found;
    }
}
