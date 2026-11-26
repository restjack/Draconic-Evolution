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

import codechicken.lib.data.MCDataInput;
import com.brandon3055.brandonscore.lib.IInteractTile;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;

/**
 * eli.8: Core Monitor panel. Every second the server copies the linked core's numbers into synced fields, so clients
 * near the monitor see them even when the core is in another dimension. If the core's chunk is not loaded the last
 * values stay and {@link #loaded} goes false; the monitor never loads chunks itself.
 */
public class TileCoreMonitor extends TileBCore implements IInteractTile, net.minecraft.world.MenuProvider {
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
    // Round B: copies of the core's limits, the player who linked it, and which lines the screen shows.
    public final ManagedLong limIn = register(new ManagedLong("lim_in", DataFlags.SAVE_NBT_SYNC_TILE));
    public final ManagedLong limOut = register(new ManagedLong("lim_out", DataFlags.SAVE_NBT_SYNC_TILE));
    public final ManagedByte limReserve = register(new ManagedByte("lim_reserve", DataFlags.SAVE_NBT_SYNC_TILE));
    public final ManagedUUID owner = register(new ManagedUUID("owner", (UUID) null, DataFlags.SAVE_NBT_SYNC_TILE));
    public final ManagedString ownerName = register(new ManagedString("owner_name", "", DataFlags.SAVE_NBT_SYNC_TILE));
    public final ManagedInt show = register(new ManagedInt("show", SHOW_ALL, DataFlags.SAVE_NBT_SYNC_TILE));

    public static final int SHOW_HEADER = 1, SHOW_STATUS = 2, SHOW_PERCENT = 4, SHOW_BAR = 8, SHOW_STORED = 16,
            SHOW_INOUT = 32, SHOW_NET = 64, SHOW_ETA = 128, SHOW_ALL = 255;
    public static final int MSG_SHOW = 1, MSG_TOGGLE_CORE = 2, MSG_PYLONS = 3, MSG_PYLON_FLIP = 4, MSG_LIMITS = 5, MSG_PYLON_LIST = 10;

    /** Client copy of the linked core's pylons, filled when the settings screen asks. */
    public record PylonRow(BlockPos pos, boolean input, long rate) {}
    public List<PylonRow> clientPylons = List.of();

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
        limIn.set(core.limitIn.get());
        limOut.set(core.limitOut.get());
        limReserve.set(core.reserve.get());
    }

    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int windowId, net.minecraft.world.entity.player.Inventory inv, Player player) {
        return new com.brandon3055.draconicevolution.inventory.CoreMonitorMenu(windowId, inv, this);
    }

    public boolean isShown(int flag) {
        return (show.get() & flag) != 0;
    }

    /** The linking player and server operators may change settings; anyone may if nobody is recorded. */
    public boolean canControl(ServerPlayer player) {
        return owner.isNull() || player.getUUID().equals(owner.get())
                || Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(player.createCommandSourceStack());
    }

    private static Component msg(String text, ChatFormatting colour) {
        return Component.literal(text).withStyle(colour);
    }

    @Override
    public void receivePacketFromClient(MCDataInput data, ServerPlayer player, int id) {
        super.receivePacketFromClient(data, player, id);
        if (id == MSG_PYLONS) {
            sendPylons(player);
            return;
        }
        if (!DEConfig.extra("coreMonitor")) return;
        if (!canControl(player)) {
            player.sendOverlayMessage(msg("Only " + (ownerName.get().isEmpty() ? "the owner" : ownerName.get()) + " or an operator can change this monitor", ChatFormatting.RED));
            return;
        }
        switch (id) {
            case MSG_SHOW -> {
                int v = data.readInt() & SHOW_ALL;
                for (TileCoreMonitor p : connectedPanels(level, worldPosition)) p.show.set(v);
            }
            case MSG_TOGGLE_CORE -> {
                TileEnergyCore core = findCore();
                if (core == null) { player.sendOverlayMessage(msg("Core is not loaded", ChatFormatting.RED)); return; }
                boolean was = core.active.get();
                core.toggleActivation();
                player.sendOverlayMessage(core.active.get() != was ? msg(core.active.get() ? "Core activated" : "Core deactivated", ChatFormatting.GREEN)
                        : msg("Core can not activate: structure or stabilizers not valid", ChatFormatting.RED));
                tick = 0;
            }
            case MSG_PYLON_FLIP -> {
                BlockPos pos = data.readPos();
                TileEnergyCore core = findCore();
                if (core != null && core.pylons.contains(pos) && LoadedLookup.blockEntity(core.getLevel(), pos) instanceof TileEnergyPylon pylon && pylon.getCore() == core) {
                    pylon.toggleMode();
                    player.sendOverlayMessage(TileEnergyPylon.modeText(pylon.ioMode.get()));
                }
                sendPylons(player);
            }
            case MSG_LIMITS -> {
                long in = Math.max(0, data.readLong()), out = Math.max(0, data.readLong());
                byte res = (byte) Math.max(0, Math.min(100, data.readByte()));
                TileEnergyCore core = findCore();
                if (core == null) { player.sendOverlayMessage(msg("Core is not loaded", ChatFormatting.RED)); return; }
                core.limitIn.set(in);
                core.limitOut.set(out);
                core.reserve.set(res);
                player.sendOverlayMessage(msg("Core limits saved", ChatFormatting.GREEN));
                tick = 0;
            }
        }
    }

    private void sendPylons(ServerPlayer player) {
        TileEnergyCore core = findCore();
        List<PylonRow> rows = new ArrayList<>();
        if (core != null) {
            Level cl = core.getLevel();
            core.pylons.removeIf(pos -> !(LoadedLookup.isLoaded(cl, pos)
                    && LoadedLookup.blockEntity(cl, pos) instanceof TileEnergyPylon p && p.getCore() == core));
            for (BlockPos pos : core.pylons) {
                if (LoadedLookup.blockEntity(cl, pos) instanceof TileEnergyPylon p) {
                    rows.add(new PylonRow(pos, p.ioMode.get().canReceive(), p.lastTickRate()));
                }
            }
            rows.sort((a, b) -> a.pos().compareTo(b.pos()));
        }
        sendPacketToClient(player, out -> {
            out.writeVarInt(rows.size());
            for (PylonRow r : rows) { out.writePos(r.pos()); out.writeBoolean(r.input()); out.writeLong(r.rate()); }
        }, MSG_PYLON_LIST);
    }

    @Override
    public void receivePacketFromServer(MCDataInput data, int id) {
        if (id == MSG_PYLON_LIST) {
            List<PylonRow> rows = new ArrayList<>();
            int n = data.readVarInt();
            for (int i = 0; i < n; i++) rows.add(new PylonRow(data.readPos(), data.readBoolean(), data.readLong()));
            clientPylons = rows;
        } else {
            super.receivePacketFromServer(data, id);
        }
    }

    @Override
    public InteractionResult onBlockUse(BlockState state, Player player, InteractionHand hand, BlockHitResult hit) {
        // Holding a block: place it (building a bigger screen against this one), don't open the settings.
        if (hand != InteractionHand.MAIN_HAND || player.getItemInHand(hand).getItem() instanceof net.minecraft.world.item.BlockItem) return InteractionResult.PASS;
        if (player instanceof ServerPlayer sp) sp.openMenu(this, worldPosition);
        return InteractionResult.SUCCESS;
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
    public int linkScreen(GlobalPos core, Player player) {
        Set<TileCoreMonitor> panels = connectedPanels(level, worldPosition);
        for (TileCoreMonitor p : panels) {
            p.owner.set(player.getUUID());
            p.ownerName.set(player.getName().getString());
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
