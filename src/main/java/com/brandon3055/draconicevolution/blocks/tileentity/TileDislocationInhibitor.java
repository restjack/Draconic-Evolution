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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

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
 * Draconic magnets ask {@link #isInhibited} (a per-level list of loaded projectors) instead of scanning blocks around
 * every item. In "all magnets" mode the projector also gives items in its box the standard {@code PreventRemoteMovement}
 * tag that most mods' magnets respect, plus our own marker so we only ever remove tags we added.
 */
public class TileDislocationInhibitor extends TileBCore implements MenuProvider, IInteractTile {

    public static final int[] RANGES = {3, 5, 8, 12, 16};
    public static final int DEFAULT_RANGE = 1; // 5 blocks: what the old block did
    public static final int RS_IGNORE = 0, RS_HIGH = 1, RS_LOW = 2;
    public static final int SET_RANGE = 0, SET_ALL = 1, SET_AREA = 2, SET_RS = 3;
    public static final String PREVENT = "PreventRemoteMovement", MARK = "draconicevolution:inhibitor";

    public final ManagedByte range = register(new ManagedByte("range", DEFAULT_RANGE, DataFlags.SAVE_NBT_SYNC_TILE));
    public final ManagedBool allMagnets = register(new ManagedBool("all_magnets", true, DataFlags.SAVE_NBT_SYNC_TILE));
    public final ManagedBool showArea = register(new ManagedBool("show_area", false, DataFlags.SAVE_NBT_SYNC_TILE));
    /** 0 ignore, 1 needs a signal, 2 needs no signal. */
    public final ManagedByte redstoneMode = register(new ManagedByte("redstone_mode", DataFlags.SAVE_NBT_SYNC_TILE));
    public final ManagedBool signal = register(new ManagedBool("signal", DataFlags.SAVE_NBT_SYNC_TILE));
    /** Items in the box right now (screen only). */
    public final ManagedInt held = register(new ManagedInt("held", DataFlags.SYNC_CONTAINER));

    /** Items this projector tagged. Identity set: ItemEntity has no equals override. */
    private final Set<ItemEntity> tagged = new HashSet<>();

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
            if (t.covers(entity)) return true;
        }
        return false;
    }

    @Override
    public void onLoad() {
        super.onLoad();
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
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ItemEntity item)) return;
        CompoundTag data = item.getPersistentData();
        if (!data.contains(MARK)) return;
        long owner = data.getLongOr(MARK, 0L);
        Set<TileDislocationInhibitor> set = LOADED.get(event.getLevel());
        if (set != null) for (TileDislocationInhibitor t : set) {
            if (t.worldPosition.asLong() == owner && t.covers(item) && t.allMagnets.get()) return;
        }
        data.remove(PREVENT);
        data.remove(MARK);
    }

    //endregion

    public int radius() {
        return RANGES[Math.floorMod(range.get(), RANGES.length)];
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

    /** The field: a box of whole blocks, radius blocks out from this one on every side. */
    public AABB area() {
        return new AABB(worldPosition).inflate(radius());
    }

    public boolean covers(Entity e) {
        if (isRemoved() || !active()) return false;
        int r = radius();
        double x = e.getX() - worldPosition.getX(), y = e.getY() - worldPosition.getY(), z = e.getZ() - worldPosition.getZ();
        return x >= -r && x < r + 1 && y >= -r && y < r + 1 && z >= -r && z < r + 1;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level.isClientSide() && onInterval(10)) scan();
    }

    /** Count items in the box; in "all magnets" mode tag them and untag the ones that left. */
    private void scan() {
        List<ItemEntity> inside = active() ? level.getEntitiesOfClass(ItemEntity.class, area(), this::covers) : List.of();
        held.set(inside.size());
        Set<ItemEntity> now = new HashSet<>();
        if (allMagnets.get()) {
            long mine = worldPosition.asLong();
            for (ItemEntity item : inside) {
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
        for (ItemEntity item : tagged) {
            if (!now.contains(item)) release(item);
        }
        tagged.clear();
        tagged.addAll(now);
    }

    private void release(ItemEntity item) {
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
            case SET_RANGE -> range.set((byte) Math.floorMod(v, RANGES.length));
            case SET_ALL -> allMagnets.set(v != 0);
            case SET_AREA -> showArea.set(v != 0);
            case SET_RS -> redstoneMode.set((byte) Math.floorMod(v, 3));
        }
        scan();
    }

    /** Right-click opens the screen; shift-right-click (empty hands) cycles the range. */
    @Override
    public InteractionResult onBlockUse(BlockState state, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isShiftKeyDown()) {
            if (player instanceof ServerPlayer sp) {
                range.set((byte) ((Math.floorMod(range.get(), RANGES.length) + 1) % RANGES.length));
                scan();
                sp.sendOverlayMessage(Component.translatable("gui.draconicevolution.dislocation_inhibitor.range_set", radius()).withStyle(ChatFormatting.LIGHT_PURPLE));
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
