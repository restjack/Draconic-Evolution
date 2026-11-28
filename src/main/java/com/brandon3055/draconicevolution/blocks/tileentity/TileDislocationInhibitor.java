package com.brandon3055.draconicevolution.blocks.tileentity;

import com.brandon3055.brandonscore.blocks.TileBCore;
import com.brandon3055.brandonscore.lib.IInteractTile;
import com.brandon3055.brandonscore.lib.datamanager.DataFlags;
import com.brandon3055.brandonscore.lib.datamanager.ManagedBool;
import com.brandon3055.brandonscore.lib.datamanager.ManagedByte;
import com.brandon3055.brandonscore.lib.datamanager.ManagedInt;
import codechicken.lib.data.MCDataInput;
import com.brandon3055.draconicevolution.init.DEContent;
import com.brandon3055.draconicevolution.inventory.DislocationInhibitorMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Dislocation Normalization Field Projector (restjack fork, unofficial rework; mockup
 * https://claude.ai/artifact/QMWoAA3VJbaUbia96fuap3). Items inside a box around the block can't be pulled by magnets.
 * <p>
 * The box works like the Evo Tweaks Field Crystal's (unofficial-1.34): per-axis radius r (0..16, 2r+1 blocks wide) and
 * offset (-16..16) of the box centre from the block. Blocks saved before 1.34 had one cube radius ({@link #RANGES}
 * index in "range"); the first load turns that into r/r/r with no offset.
 * <p>
 * Draconic magnets ask {@link #isInhibited} (a per-level list of loaded projectors) instead of scanning blocks around
 * every item. In "all magnets" mode the projector also gives items in its box the standard {@code PreventRemoteMovement}
 * tag that most mods' magnets respect, plus our own marker so we only ever remove tags we added.
 * <p>
 * "Stop XP" (unofficial-1.40, default on): the same for experience orbs. Draconic magnets skip orbs in the box; in
 * "all magnets" mode orbs get the tag too (Sophisticated magnet upgrade reads it) and {@link #onPickupXp} cancels XP
 * pickups that happen from a distance (Simple Magnets, Advanced AE magnet card call {@code playerTouch} from afar).
 */
public class TileDislocationInhibitor extends TileBCore implements MenuProvider, IInteractTile {

    /** Old cube radii (pre-1.34 saves, and shift-right-click on the block still steps through them). */
    public static final int[] RANGES = {3, 5, 8, 12, 16};
    public static final int DEFAULT_RANGE = 1; // 5 blocks: what the old block did
    public static final int RADIUS_MAX = 16, OFFSET_MAX = 16;
    public static final int RS_IGNORE = 0, RS_HIGH = 1, RS_LOW = 2;
    /** SET_RAD + axis (0 x, 1 y, 2 z) and SET_OFF + axis set one value. */
    public static final int SET_ALL = 1, SET_AREA = 2, SET_RS = 3, SET_RAD = 4, SET_OFF = 7, SET_XP = 10;
    public static final String PREVENT = "PreventRemoteMovement", MARK = "draconicevolution:inhibitor";

    /** Pre-1.34 cube size, read once by {@link #migrate}. */
    public final ManagedByte range = register(new ManagedByte("range", DEFAULT_RANGE, DataFlags.SAVE_NBT));
    public final ManagedBool boxSet = register(new ManagedBool("box_v2", false, DataFlags.SAVE_NBT));
    private final ManagedByte[] rad = {
            register(new ManagedByte("rad_x", 5, DataFlags.SAVE_NBT_SYNC_TILE)),
            register(new ManagedByte("rad_y", 5, DataFlags.SAVE_NBT_SYNC_TILE)),
            register(new ManagedByte("rad_z", 5, DataFlags.SAVE_NBT_SYNC_TILE))};
    private final ManagedByte[] off = {
            register(new ManagedByte("off_x", 0, DataFlags.SAVE_NBT_SYNC_TILE)),
            register(new ManagedByte("off_y", 0, DataFlags.SAVE_NBT_SYNC_TILE)),
            register(new ManagedByte("off_z", 0, DataFlags.SAVE_NBT_SYNC_TILE))};
    public final ManagedBool allMagnets = register(new ManagedBool("all_magnets", true, DataFlags.SAVE_NBT_SYNC_TILE));
    public final ManagedBool stopXp = register(new ManagedBool("stop_xp", true, DataFlags.SAVE_NBT_SYNC_TILE));
    public final ManagedBool showArea = register(new ManagedBool("show_area", false, DataFlags.SAVE_NBT_SYNC_TILE));
    /** 0 ignore, 1 needs a signal, 2 needs no signal. */
    public final ManagedByte redstoneMode = register(new ManagedByte("redstone_mode", DataFlags.SAVE_NBT_SYNC_TILE));
    public final ManagedBool signal = register(new ManagedBool("signal", DataFlags.SAVE_NBT_SYNC_TILE));
    /** Items in the box right now (screen only). */
    public final ManagedInt held = register(new ManagedInt("held", DataFlags.SYNC_CONTAINER));

    /** Items and orbs this projector tagged. Identity set: entities have no equals override. */
    private final Set<Entity> tagged = new HashSet<>();

    /** Loaded projectors per level (client and server levels are separate keys). */
    private static final Map<Level, Set<TileDislocationInhibitor>> LOADED = Collections.synchronizedMap(new WeakHashMap<>());

    public TileDislocationInhibitor(BlockPos pos, BlockState state) {
        super(DEContent.TILE_DISLOCATION_INHIBITOR.get(), pos, state);
    }

    //region Registry

    /** True when any active projector's box holds this entity. A few distance checks per call. */
    public static boolean isInhibited(Level level, Entity entity) {
        Set<TileDislocationInhibitor> set = LOADED.get(level);
        if (set == null) return false;
        for (TileDislocationInhibitor t : set) {
            if (t.holds(entity)) return true;
        }
        return false;
    }

    /**
     * All-magnets mode: cancel an XP pickup when the orb sits in a box and isn't touching the player, i.e. a magnet
     * called {@code playerTouch} from a distance. Walking into the orb still collects it. Runs first so listeners
     * like Mystical Agriculture's XP capsule never see a blocked pickup.
     */
    public static void onPickupXp(PlayerXpEvent.PickupXp event) {
        ExperienceOrb orb = event.getOrb();
        if (orb.level().isClientSide() || event.getEntity().getBoundingBox().inflate(1.5, 1.0, 1.5).intersects(orb.getBoundingBox())) return;
        Set<TileDislocationInhibitor> set = LOADED.get(orb.level());
        if (set != null) for (TileDislocationInhibitor t : set) {
            if (t.allMagnets.get() && t.holds(orb)) {
                event.setCanceled(true);
                return;
            }
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide()) migrate();
        if (level != null) LOADED.computeIfAbsent(level, k -> new HashSet<>()).add(this);
    }

    /** Break, replace and chunk unload all end here. */
    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level == null) return;
        Set<TileDislocationInhibitor> set = LOADED.get(level);
        if (set != null) set.remove(this);
        if (!level.isClientSide()) releaseAll();
    }

    /**
     * Item loaded from disk with our marker (left from a restart or an unload): keep the tag only while a loaded,
     * active projector still covers it; otherwise drop it. A projector that loads later tags it again within 0.5 s.
     */
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        Entity item = event.getEntity();
        if (event.getLevel().isClientSide() || !(item instanceof ItemEntity || item instanceof ExperienceOrb)) return;
        CompoundTag data = item.getPersistentData();
        if (!data.contains(MARK)) return;
        long owner = data.getLongOr(MARK, 0L);
        Set<TileDislocationInhibitor> set = LOADED.get(event.getLevel());
        if (set != null) for (TileDislocationInhibitor t : set) {
            if (t.worldPosition.asLong() == owner && t.holds(item) && t.allMagnets.get()) return;
        }
        data.remove(PREVENT);
        data.remove(MARK);
    }

    //endregion

    /** Pre-1.34 save: old cube radius becomes r/r/r, centred on the block. */
    private void migrate() {
        if (boxSet.get()) return;
        setCube(RANGES[Math.floorMod(range.get(), RANGES.length)]);
        boxSet.set(true);
    }

    private void setCube(int r) {
        for (int i = 0; i < 3; i++) {
            rad[i].set((byte) r);
            off[i].set((byte) 0);
        }
    }

    public int radius(int axis) {
        return Math.max(0, Math.min(RADIUS_MAX, rad[axis].get()));
    }

    public int offset(int axis) {
        return Math.max(-OFFSET_MAX, Math.min(OFFSET_MAX, off[axis].get()));
    }

    /** Box relative to the block, inclusive: {minX, minY, minZ, maxX, maxY, maxZ} (Field Crystal's FieldLogic.box). */
    public int[] box() {
        int[] b = new int[6];
        for (int i = 0; i < 3; i++) {
            b[i] = offset(i) - radius(i);
            b[i + 3] = offset(i) + radius(i);
        }
        return b;
    }

    /** Box size in blocks on one axis: 2r+1. */
    public int size(int axis) {
        return 2 * radius(axis) + 1;
    }

    public int redstone() {
        return Math.floorMod(redstoneMode.get(), 3);
    }

    public boolean active() {
        return switch (redstone()) {
            case RS_HIGH -> signal.get();
            case RS_LOW -> !signal.get();
            default -> true;
        };
    }

    /** The field in world space: whole blocks from box min to box max. */
    public AABB area() {
        int[] b = box();
        return new AABB(worldPosition.getX() + b[0], worldPosition.getY() + b[1], worldPosition.getZ() + b[2],
                worldPosition.getX() + b[3] + 1, worldPosition.getY() + b[4] + 1, worldPosition.getZ() + b[5] + 1);
    }

    public boolean covers(Entity e) {
        if (isRemoved() || !active()) return false;
        int[] b = box();
        double x = e.getX() - worldPosition.getX(), y = e.getY() - worldPosition.getY(), z = e.getZ() - worldPosition.getZ();
        return x >= b[0] && x < b[3] + 1 && y >= b[1] && y < b[4] + 1 && z >= b[2] && z < b[5] + 1;
    }

    /** Covers the entity and handles its kind: items always, XP orbs only with "Stop XP" on. */
    public boolean holds(Entity e) {
        return (e instanceof ItemEntity || (e instanceof ExperienceOrb && stopXp.get())) && covers(e);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level.isClientSide() && onInterval(10)) scan();
    }

    /** Count items in the box; in "all magnets" mode tag them (and XP orbs) and untag the ones that left. */
    private void scan() {
        List<Entity> inside = active() ? level.getEntitiesOfClass(Entity.class, area(), this::holds) : List.of();
        held.set((int) inside.stream().filter(e -> e instanceof ItemEntity).count());
        Set<Entity> now = new HashSet<>();
        if (allMagnets.get()) {
            long mine = worldPosition.asLong();
            for (Entity item : inside) {
                CompoundTag data = item.getPersistentData();
                if (!data.contains(PREVENT)) {
                    data.putBoolean(PREVENT, true);
                    data.putLong(MARK, mine);
                    now.add(item);
                } else if (data.contains(MARK) && data.getLongOr(MARK, 0L) == mine) {
                    now.add(item); // ours from before a reload, or still ours
                }
            }
        }
        for (Entity item : tagged) {
            if (!now.contains(item)) release(item);
        }
        tagged.clear();
        tagged.addAll(now);
    }

    private void release(Entity item) {
        CompoundTag data = item.getPersistentData();
        if (data.contains(MARK) && data.getLongOr(MARK, 0L) == worldPosition.asLong()) {
            data.remove(PREVENT);
            data.remove(MARK);
        }
    }

    private void releaseAll() {
        tagged.forEach(this::release);
        tagged.clear();
    }

    @Override
    public void onNeighborChange(BlockPos neighbor) {
        super.onNeighborChange(neighbor);
        if (level == null || level.isClientSide()) return;
        boolean now = level.hasNeighborSignal(worldPosition);
        if (now != signal.get()) {
            signal.set(now);
            scan();
        }
    }

    @Override
    public void receivePacketFromClient(MCDataInput data, ServerPlayer client, int id) {
        if (id != 4) {
            super.receivePacketFromClient(data, client, id);
            return;
        }
        int which = data.readByte(), v = data.readByte();
        switch (which) {
            case SET_ALL -> allMagnets.set(v != 0);
            case SET_AREA -> showArea.set(v != 0);
            case SET_XP -> stopXp.set(v != 0);
            case SET_RS -> redstoneMode.set((byte) Math.floorMod(v, 3));
            case SET_RAD, SET_RAD + 1, SET_RAD + 2 -> rad[which - SET_RAD].set((byte) Math.max(0, Math.min(RADIUS_MAX, v)));
            case SET_OFF, SET_OFF + 1, SET_OFF + 2 -> off[which - SET_OFF].set((byte) Math.max(-OFFSET_MAX, Math.min(OFFSET_MAX, v)));
        }
        scan();
    }

    /** Right-click opens the screen; shift-right-click (empty hands) steps a centred cube through 3/5/8/12/16. */
    @Override
    public InteractionResult onBlockUse(BlockState state, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isShiftKeyDown()) {
            if (player instanceof ServerPlayer sp) {
                int big = Math.max(radius(0), Math.max(radius(1), radius(2))), next = 0; // next old size above the largest axis
                while (next < RANGES.length && RANGES[next] <= big) next++;
                int r = RANGES[next % RANGES.length];
                setCube(r);
                scan();
                sp.sendOverlayMessage(Component.translatable("gui.draconicevolution.dislocation_inhibitor.range_set", r, 2 * r + 1).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer) {
            player.openMenu(this, worldPosition);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new DislocationInhibitorMenu(id, inv, this);
    }
}
