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
public class TileCoreMonitor extends TileBCore implements IInteractTile, net.minecraft.world.MenuProvider, com.brandon3055.brandonscore.lib.IRedstoneEmitter {
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
    // Round C: look, units, thresholds, ETA target, alerts, redstone.
    public final ManagedString name = register(new ManagedString("name", "", DataFlags.SAVE_NBT_SYNC_TILE));
    public final ManagedByte unit = register(new ManagedByte("unit", DataFlags.SAVE_NBT_SYNC_TILE)); // index into UNITS
    public final ManagedByte warnAt = register(new ManagedByte("warn_at", (byte) 25, DataFlags.SAVE_NBT_SYNC_TILE));
    public final ManagedByte alarmAt = register(new ManagedByte("alarm_at", (byte) 10, DataFlags.SAVE_NBT_SYNC_TILE));
    public final ManagedByte target = register(new ManagedByte("target", DataFlags.SAVE_NBT_SYNC_TILE)); // ETA to this %, 0 = full/empty
    public final ManagedByte bgAlpha = register(new ManagedByte("bg_alpha", DataFlags.SAVE_NBT_SYNC_TILE)); // 0 solid, 1 75%, 2 50%
    public final ManagedInt alerts = register(new ManagedInt("alerts", ALERT_CHAT | ALERT_TOAST, DataFlags.SAVE_NBT_SYNC_TILE));
    public final ManagedByte redstoneMode = register(new ManagedByte("redstone_mode", DataFlags.SAVE_NBT_SYNC_TILE));
    // Screen redo (2026-11-26): graph range on the in-world screen, pylon count, and a one-time upgrade of the show bits.
    public final ManagedByte screenRange = register(new ManagedByte("screen_range", DataFlags.SAVE_NBT_SYNC_TILE)); // 0 5 min, 1 1 h, 2 24 h
    public final ManagedByte pylonCount = register(new ManagedByte("pylon_count", DataFlags.SAVE_NBT_SYNC_TILE));
    public final ManagedByte showVersion = register(new ManagedByte("show_version", DataFlags.SAVE_NBT));
    private int redstoneOut;
    private boolean primed, wasLow, wasStabOk, wasActive;

    public static final String[] UNITS = {"Player setting", "OP", "RF", "FE", "J"};
    public static final String[] REDSTONE_MODES = {"Off", "Fill level (0-15)", "On below alarm %", "On while full", "On while draining"};
    public static final int ALERT_CHAT = 1, ALERT_TOAST = 2, ALERT_SOUND = 4, ALERT_TEAM = 8;
    public static final int MSG_LOOK = 6, MSG_ALERTS = 7, MSG_STATS = 8, MSG_LOCATE = 9, MSG_STATS_DATA = 11;

    /** Client copy of the core's stats, filled when the Stats tab asks. series: 5 min, 1 h, 24 h (oldest first). */
    public long statAvg, statPeakIn, statPeakOut;
    public float statLow = -1;
    public int statMinutes;
    public long[][] statSeries = {new long[0], new long[0], new long[0]};

    public static final int SHOW_HEADER = 1, SHOW_STATUS = 2, SHOW_PERCENT = 4, SHOW_BAR = 8, SHOW_STORED = 16,
            SHOW_INOUT = 32, SHOW_NET = 64, SHOW_ETA = 128, SHOW_FRAME = 256, SHOW_RENDER = 512, SHOW_GRAPH = 1024, SHOW_STATS = 2048, SHOW_ALL = 4095;
    public static final int MSG_SCREEN_RANGE = 12, MSG_ALERT_TEST = 13, MSG_PYLON_LOCATE = 14;
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
        if (level == null || level.isClientSide() || tick++ % 20 != 0) return;
        if (showVersion.get() < 1) { // panels placed before the graph/stats bits existed: switch them on once
            show.set(show.get() | SHOW_GRAPH | SHOW_STATS);
            showVersion.set((byte) 1);
        }
        if (!isLinked()) adoptNeighbour();
        if (!isLinked() || !DEConfig.extra("coreMonitor")) return;
        if (tick % 100 == 1 && isOriginPanel()) pushStatsToNearby();
        TileEnergyCore core = findCore();
        if (core == null) {
            loaded.set(false);
            updateRedstone();
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
        prunePylons(core);
        pylonCount.set((byte) Math.min(127, core.pylons.size()));
        if (isOriginPanel()) checkAlerts(core);
        updateRedstone();
    }

    /** A panel added to an existing screen takes the screen's link and settings from a linked neighbour. */
    private void adoptNeighbour() {
        Direction facing = getBlockState().getValue(CoreMonitor.FACING);
        for (Direction d : new Direction[]{Direction.UP, Direction.DOWN, facing.getClockWise(), facing.getCounterClockWise()}) {
            if (level.getBlockEntity(worldPosition.relative(d)) instanceof TileCoreMonitor o && o.isLinked()
                    && o.getBlockState().getValue(CoreMonitor.FACING) == facing) {
                copyFrom(o);
                return;
            }
        }
    }

    private void copyFrom(TileCoreMonitor o) {
        linkDim.set(o.linkDim.get());
        linkPos.set(o.linkPos.get());
        owner.set(o.owner.get());
        ownerName.set(o.ownerName.get());
        show.set(o.show.get());
        name.set(o.name.get());
        unit.set(o.unit.get());
        warnAt.set(o.warnAt.get());
        alarmAt.set(o.alarmAt.get());
        target.set(o.target.get());
        bgAlpha.set(o.bgAlpha.get());
        alerts.set(o.alerts.get());
        redstoneMode.set(o.redstoneMode.get());
        screenRange.set(o.screenRange.get());
        tick = 0;
    }

    /** The panel that draws this screen (bottom-left); the settings screen always edits that one. */
    public TileCoreMonitor originPanel() {
        for (TileCoreMonitor p : connectedPanels(level, worldPosition)) if (p.isOriginPanel()) return p;
        return this;
    }

    private static void prunePylons(TileEnergyCore core) {
        Level cl = core.getLevel();
        core.pylons.removeIf(pos -> !(LoadedLookup.isLoaded(cl, pos)
                && LoadedLookup.blockEntity(cl, pos) instanceof TileEnergyPylon p && p.getCore() == core));
    }

    /** Graph and stat cards on the in-world screen: players near the screen get the stats every 5 seconds. */
    private void pushStatsToNearby() {
        for (var p : level.players()) {
            if (p instanceof ServerPlayer sp && p.distanceToSqr(worldPosition.getCenter()) < 64 * 64) sendStats(sp);
        }
    }

    /** Unit label for this monitor. */
    public String unitName() {
        int u = unit.get();
        return u <= 0 || u >= UNITS.length ? DEConfig.energyUnit() : UNITS[u];
    }

    /** OP (= RF = FE) converted to this monitor's unit. */
    public long toUnit(long op) {
        return unit.get() == 4 ? (long) (op * 2.5D) : op;
    }

    public double fillPercent() {
        return unlimited.get() || capacity.get() <= 0 ? -1 : stored.get() * 100D / capacity.get();
    }

    private boolean samePanel(BlockPos pos) {
        var s = level.getBlockState(pos);
        return s.getBlock() instanceof CoreMonitor && s.getValue(CoreMonitor.FACING) == getBlockState().getValue(CoreMonitor.FACING);
    }

    /** The bottom-left panel of a screen; it draws the screen and sends the alerts, so each fires once. */
    public boolean isOriginPanel() {
        Direction right = getBlockState().getValue(CoreMonitor.FACING).getCounterClockWise();
        return !samePanel(worldPosition.relative(right.getOpposite())) && !samePanel(worldPosition.below());
    }

    // ### Redstone

    private void updateRedstone() {
        double pct = fillPercent();
        long net = input.get() - output.get();
        int out = switch (redstoneMode.get()) {
            case 1 -> pct < 0 ? 15 : (int) Math.round(Math.min(100, pct) * 15 / 100);
            case 2 -> pct >= 0 && pct < alarmAt.get() ? 15 : 0;
            case 3 -> pct >= 100 ? 15 : 0;
            case 4 -> net < 0 ? 15 : 0;
            default -> 0;
        };
        if (!loaded.get() && redstoneMode.get() != 1) out = 0;
        if (out != redstoneOut) {
            redstoneOut = out;
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
    }

    @Override
    public int getWeakPower(BlockState blockState, Direction side) {
        return DEConfig.extra("coreMonitor") ? redstoneOut : 0;
    }

    @Override
    public int getStrongPower(BlockState blockState, Direction side) {
        return 0;
    }

    // ### Alerts

    private void checkAlerts(TileEnergyCore core) {
        double pct = fillPercent();
        boolean low = pct >= 0 && pct < alarmAt.get(), stabOk = core.stabilizersValid.get(), active = core.active.get();
        if (primed && alerts.get() != 0) {
            if (low && !wasLow) alert("below " + alarmAt.get() + "% (" + String.format(java.util.Locale.ROOT, "%.1f", pct) + "%)");
            if (!stabOk && wasStabOk) alert("stabilizer broken or missing");
            if (!active && wasActive) alert("core deactivated");
        }
        primed = true;
        wasLow = low;
        wasStabOk = stabOk;
        wasActive = active;
    }

    private String label() {
        if (!name.get().isEmpty()) return name.get();
        BlockPos p = linkPos.get();
        return "Core at " + p.getX() + " " + p.getY() + " " + p.getZ();
    }

    private void alert(String what) {
        if (!(level instanceof ServerLevel sl) || owner.isNull()) return;
        Set<ServerPlayer> to = new java.util.LinkedHashSet<>();
        ServerPlayer o = sl.getServer().getPlayerList().getPlayer(owner.get());
        if (o != null) to.add(o);
        if ((alerts.get() & ALERT_TEAM) != 0) to.addAll(teamMembers(sl, owner.get(), o));
        Component chat = Component.literal("[Core Monitor] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(Component.literal(label() + ": " + what).withStyle(ChatFormatting.RED));
        for (ServerPlayer p : to) {
            if ((alerts.get() & ALERT_CHAT) != 0) p.sendSystemMessage(chat);
            if ((alerts.get() & ALERT_TOAST) != 0) com.brandon3055.draconicevolution.network.DraconicNetwork.sendMonitorToast(p, label(), what);
            if ((alerts.get() & ALERT_SOUND) != 0) {
                p.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BELL,
                        net.minecraft.sounds.SoundSource.BLOCKS, p.getX(), p.getY(), p.getZ(), 1F, 0.6F, p.getRandom().nextLong()));
            }
        }
    }

    /** Online members of the owner's FTB Teams party if that mod is loaded, else of the owner's scoreboard team. */
    private static List<ServerPlayer> teamMembers(ServerLevel sl, UUID owner, @Nullable ServerPlayer ownerPlayer) {
        List<ServerPlayer> out = new ArrayList<>();
        if (net.neoforged.fml.ModList.get().isLoaded("ftbteams")) {
            try { // reflection: no compile dependency on FTB Teams
                Object api = Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI").getMethod("api").invoke(null);
                Object manager = api.getClass().getMethod("getManager").invoke(api);
                var team = (java.util.Optional<?>) manager.getClass().getMethod("getTeamForPlayerID", UUID.class).invoke(manager, owner);
                if (team.isPresent()) {
                    for (Object p : (java.util.Collection<?>) team.get().getClass().getMethod("getOnlineMembers").invoke(team.get())) {
                        if (p instanceof ServerPlayer sp) out.add(sp);
                    }
                    return out;
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }
        if (ownerPlayer != null) {
            var team = sl.getServer().getScoreboard().getPlayersTeam(ownerPlayer.getScoreboardName());
            if (team != null) {
                for (String n : team.getPlayers()) {
                    ServerPlayer p = sl.getServer().getPlayerList().getPlayerByName(n);
                    if (p != null) out.add(p);
                }
            }
        }
        return out;
    }

    // ### Stats

    private void sendStats(ServerPlayer player) {
        TileEnergyCore core = findCore();
        long[] recent = core == null ? new long[0] : core.recentHistory();
        int n = core == null ? 0 : core.minuteCount;
        long sum = 0, peakIn = 0, peakOut = 0;
        float low = 2;
        long[] hour = new long[Math.min(60, n)], day = new long[0];
        if (core != null) {
            int last = TileEnergyCore.MINUTE_SAMPLES;
            for (int i = 0; i < hour.length; i++) {
                int j = last - hour.length + i;
                hour[i] = core.minuteNet[j];
                sum += core.minuteNet[j];
                peakIn = Math.max(peakIn, core.minutePeakIn[j]);
                peakOut = Math.max(peakOut, core.minutePeakOut[j]);
            }
            for (int i = last - n; i < last; i++) if (core.minuteLowFill[i] >= 0) low = Math.min(low, core.minuteLowFill[i]);
            int bars = Math.min(60, (n + 23) / 24); // 24 minutes per bar, newest on the right
            day = new long[bars];
            for (int b = 0; b < bars; b++) {
                int end = last - (bars - 1 - b) * 24, start = Math.max(last - n, end - 24);
                long s = 0;
                for (int i = start; i < end; i++) s += core.minuteNet[i];
                day[b] = end > start ? s / (end - start) : 0;
            }
        }
        long avg = hour.length == 0 ? 0 : sum / hour.length;
        long pi = peakIn, po = peakOut;
        float lw = low > 1 ? -1 : low * 100;
        long[][] series = {recent, hour, day};
        sendPacketToClient(player, out -> {
            out.writeLong(avg); out.writeLong(pi); out.writeLong(po); out.writeFloat(lw); out.writeVarInt(n);
            for (long[] s : series) { out.writeVarInt(s.length); for (long v : s) out.writeLong(v); }
        }, MSG_STATS_DATA);
    }

    /** A sample alert to only this player, through one channel, marked TEST so nobody mistakes it for a real one. */
    private void sendTestAlert(ServerPlayer p, int channel) {
        String what = "TEST - this is how an alert will look";
        if ((channel & ALERT_CHAT) != 0) p.sendSystemMessage(Component.literal("[Core Monitor] [TEST] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(Component.literal(label() + ": below " + alarmAt.get() + "% (example)").withStyle(ChatFormatting.RED)));
        if ((channel & ALERT_TOAST) != 0) com.brandon3055.draconicevolution.network.DraconicNetwork.sendMonitorToast(p, "[TEST] " + label(), what);
        if ((channel & ALERT_SOUND) != 0) {
            p.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BELL,
                    net.minecraft.sounds.SoundSource.BLOCKS, p.getX(), p.getY(), p.getZ(), 1F, 0.6F, p.getRandom().nextLong()));
            p.sendOverlayMessage(Component.literal("[TEST] alert sound").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    /** Pylon position in chat (JourneyMap waypoint form) plus a column of particles at the pylon. */
    private void locatePylon(ServerPlayer player, BlockPos pos) {
        TileEnergyCore core = findCore();
        if (core == null || !core.pylons.contains(pos)) return;
        player.sendSystemMessage(Component.literal("Pylon: ").withStyle(ChatFormatting.LIGHT_PURPLE).append(Component.literal(
                "[name:\"Pylon\", x:" + pos.getX() + ", y:" + pos.getY() + ", z:" + pos.getZ() + ", dim:" + linkDim.get() + "]").withStyle(ChatFormatting.AQUA)));
        if (core.getLevel() instanceof ServerLevel sl) {
            for (int i = 0; i < 12; i++) {
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1 + i * 0.6, pos.getZ() + 0.5, 4, 0.15, 0.15, 0.15, 0.01);
            }
        }
    }

    /** JourneyMap turns a chat line in this form into a clickable "add waypoint". */
    private void sendLocate(ServerPlayer player) {
        BlockPos p = linkPos.get();
        String wp = "[name:\"" + label().replace("\"", "'") + "\", x:" + p.getX() + ", y:" + p.getY() + ", z:" + p.getZ() + ", dim:" + linkDim.get() + "]";
        player.sendSystemMessage(Component.literal("Core: ").withStyle(ChatFormatting.LIGHT_PURPLE).append(Component.literal(wp).withStyle(ChatFormatting.AQUA)));
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
        if (id == MSG_STATS) {
            sendStats(player);
            return;
        }
        if (id == MSG_LOCATE) {
            if (isLinked()) sendLocate(player);
            return;
        }
        if (id == MSG_ALERT_TEST) {
            if (isLinked()) sendTestAlert(player, data.readByte());
            return;
        }
        if (id == MSG_PYLON_LOCATE) {
            locatePylon(player, data.readPos());
            return;
        }
        if (!DEConfig.extra("coreMonitor")) return;
        if (!canControl(player)) {
            player.sendOverlayMessage(msg("Only " + (ownerName.get().isEmpty() ? "the owner" : ownerName.get()) + " or an operator can change this monitor", ChatFormatting.RED));
            return;
        }
        switch (id) {
            case MSG_LOOK -> {
                String n = data.readString();
                byte u = data.readByte(), w = data.readByte(), a = data.readByte(), tg = data.readByte(), bg = data.readByte();
                for (TileCoreMonitor p : connectedPanels(level, worldPosition)) {
                    p.name.set(n.length() > 32 ? n.substring(0, 32) : n);
                    p.unit.set((byte) Math.max(0, Math.min(UNITS.length - 1, u)));
                    p.warnAt.set((byte) Math.max(0, Math.min(100, w)));
                    p.alarmAt.set((byte) Math.max(0, Math.min(100, a)));
                    p.target.set((byte) Math.max(0, Math.min(100, tg)));
                    p.bgAlpha.set((byte) Math.max(0, Math.min(2, bg)));
                }
                player.sendOverlayMessage(msg("Monitor look saved", ChatFormatting.GREEN));
            }
            case MSG_ALERTS -> {
                int al = data.readInt() & 15;
                byte rm = (byte) Math.max(0, Math.min(REDSTONE_MODES.length - 1, data.readByte()));
                for (TileCoreMonitor p : connectedPanels(level, worldPosition)) {
                    p.alerts.set(al);
                    p.redstoneMode.set(rm);
                }
            }
            case MSG_SCREEN_RANGE -> {
                byte r = (byte) Math.max(0, Math.min(2, data.readByte()));
                for (TileCoreMonitor p : connectedPanels(level, worldPosition)) p.screenRange.set(r);
            }
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
            prunePylons(core);
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
        } else if (id == MSG_STATS_DATA) {
            statAvg = data.readLong(); statPeakIn = data.readLong(); statPeakOut = data.readLong();
            statLow = data.readFloat(); statMinutes = data.readVarInt();
            for (int s = 0; s < 3; s++) {
                long[] a = new long[data.readVarInt()];
                for (int i = 0; i < a.length; i++) a[i] = data.readLong();
                statSeries[s] = a;
            }
        } else {
            super.receivePacketFromServer(data, id);
        }
    }

    @Override
    public InteractionResult onBlockUse(BlockState state, Player player, InteractionHand hand, BlockHitResult hit) {
        // Holding a block: place it (building a bigger screen against this one), don't open the settings.
        if (hand != InteractionHand.MAIN_HAND || player.getItemInHand(hand).getItem() instanceof net.minecraft.world.item.BlockItem) return InteractionResult.PASS;
        if (player instanceof ServerPlayer sp) {
            TileCoreMonitor target = originPanel();
            sp.openMenu(target, target.getBlockPos());
        }
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
