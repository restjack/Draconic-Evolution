package com.brandon3055.draconicevolution.blocks.tileentity;

import codechicken.lib.data.MCDataInput;

import com.brandon3055.brandonscore.api.TechLevel;

import com.brandon3055.brandonscore.blocks.TileBCore;

import com.brandon3055.brandonscore.client.particle.IntParticleType.IntParticleData;

import com.brandon3055.brandonscore.inventory.ItemHandlerIOControl;

import com.brandon3055.brandonscore.inventory.TileItemStackHandler;

import com.brandon3055.brandonscore.lib.IChangeListener;

import com.brandon3055.brandonscore.lib.IInteractTile;

import com.brandon3055.brandonscore.lib.Vec3D;

import com.brandon3055.brandonscore.lib.datamanager.*;

import com.brandon3055.brandonscore.utils.FacingUtils;

import com.brandon3055.brandonscore.utils.Utils;

import com.brandon3055.draconicevolution.DEConfig;

import com.brandon3055.draconicevolution.DraconicEvolution;

import com.brandon3055.draconicevolution.api.DraconicAPI;

import com.brandon3055.draconicevolution.api.crafting.IFusionInjector;

import com.brandon3055.draconicevolution.api.crafting.IFusionInventory;

import com.brandon3055.draconicevolution.api.crafting.IFusionRecipe;

import com.brandon3055.draconicevolution.api.crafting.IFusionStateMachine;

import com.brandon3055.draconicevolution.client.DEParticles;

import com.brandon3055.draconicevolution.client.render.tile.fxhandlers.ITileFXHandler;

import com.brandon3055.draconicevolution.handlers.DESounds;

import com.brandon3055.draconicevolution.init.DEContent;

import com.brandon3055.draconicevolution.inventory.FusionCraftingCoreMenu;

import com.google.common.collect.Streams;

import java.util.ArrayList;

import java.util.Arrays;

import java.util.Comparator;

import java.util.List;

import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;

import net.minecraft.core.BlockPos;

import net.minecraft.core.Direction;

import net.minecraft.core.HolderLookup;

import net.minecraft.core.particles.ParticleTypes;

import net.minecraft.nbt.CompoundTag;

import net.minecraft.network.chat.Component;

import net.minecraft.server.level.ServerPlayer;

import net.minecraft.sounds.SoundSource;

import net.minecraft.world.InteractionHand;

import net.minecraft.world.InteractionResult;

import net.minecraft.world.MenuProvider;

import net.minecraft.world.entity.player.Inventory;

import net.minecraft.world.entity.player.Player;

import net.minecraft.world.inventory.AbstractContainerMenu;

import net.minecraft.world.item.ItemStack;

import net.minecraft.world.item.crafting.RecipeHolder;

import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.world.phys.BlockHitResult;

import net.neoforged.neoforge.capabilities.Capabilities;

import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;


























































/**
 * Created by brandon3055 on 11/06/2016.
 */
public class TileFusionCraftingCore extends TileBCore implements IFusionInventory, IFusionStateMachine, MenuProvider, IInteractTile, IChangeListener {

    private final ManagedEnum<FusionState> fusionState = register(new ManagedEnum<>("fusion_state", FusionState.START, DataFlags.SAVE_NBT_SYNC_TILE));
    private final ManagedResource activeRecipe = register(new ManagedResource("active_recipe", DataFlags.SAVE_NBT_SYNC_TILE));
    private final ManagedBool crafting = register(new ManagedBool("is_crafting", DataFlags.SAVE_NBT_SYNC_TILE));
    private final ManagedInt fusionCounter = register(new ManagedInt("fusion_counter", DataFlags.SAVE_NBT));

    public final ManagedTextComponent userStatus = register(new ManagedTextComponent("user_status", DataFlags.SAVE_NBT_SYNC_CONTAINER));
    public final ManagedFloat craftAnimProgress = register(new ManagedFloat("craft_anim_progress", DataFlags.SYNC_TILE));
    public final ManagedShort craftAnimLength = register(new ManagedShort("craft_anim_length", DataFlags.SYNC_TILE));
    public final ManagedFloat progress = register(new ManagedFloat("progress", -1, DataFlags.SAVE_NBT_SYNC_CONTAINER));

    public com.brandon3055.draconicevolution.inventory.FusionItemHandler itemHandler = new com.brandon3055.draconicevolution.inventory.FusionItemHandler(this);
    public ITileFXHandler fxHandler;
    // QoL (our additions): one energy feed for every injector (auto-craft + push output moved to the Fusion Conductor).
    private int lastComparator = -2;

    /** Energy given to the core goes straight into the injectors of the running craft (each still capped by its charge rate). */
    public final com.brandon3055.brandonscore.api.power.OPStorage injectorFeed = new com.brandon3055.brandonscore.api.power.OPStorage(0) {
        private List<com.brandon3055.brandonscore.api.power.IOPStorage> targets() {
            List<com.brandon3055.brandonscore.api.power.IOPStorage> list = new ArrayList<>();
            if (!crafting.get() || level == null) return list;
            for (IFusionInjector inj : getInjectors()) {
                if (inj instanceof TileFusionCraftingInjector tile && inj.validate() && inj.getEnergyRequirement() > 0) {
                    tile.getCapability(com.brandon3055.brandonscore.capability.CapabilityOP.OP, null).ifPresent(list::add);
                }
            }
            return list;
        }

        @Override
        public long receiveOP(long maxReceive, boolean simulate) {
            long got = 0;
            for (var s : targets()) {
                if (got >= maxReceive) break;
                got += s.receiveOP(maxReceive - got, simulate);
            }
            return got;
        }

        @Override
        public int insert(int amount, net.neoforged.neoforge.transfer.transaction.TransactionContext transaction) {
            int got = 0;
            for (var s : targets()) {
                if (got >= amount) break;
                if (s instanceof net.neoforged.neoforge.transfer.energy.EnergyHandler h) got += h.insert(amount - got, transaction);
            }
            return got;
        }

        @Override public long extractOP(long maxExtract, boolean simulate) { return 0; }
        @Override public int extract(int amount, net.neoforged.neoforge.transfer.transaction.TransactionContext transaction) { return 0; }
        @Override public boolean canExtract() { return false; }
        @Override public boolean canReceive() { return true; }
        @Override public long getOPStored() { return level == null ? 0 : getInjectors().stream().mapToLong(IFusionInjector::getInjectorEnergy).sum(); }
        @Override public long getMaxOPStored() { return level == null ? 0 : getInjectors().stream().mapToLong(IFusionInjector::getEnergyRequirement).sum(); }
    };

    private List<IFusionInjector> injectorCache = null;
    private List<BlockPos> injectorPositions = new ArrayList<>();
    private RecipeHolder<IFusionRecipe> recipeCache = null;
    // Vanilla only synchronizes recipe-book displays in 26; the machine needs its real recipe.
    private CompoundTag synchronizedRecipe = new CompoundTag();
    private TechLevel minTierCache = null;

    public TileFusionCraftingCore(BlockPos pos, BlockState state) {
        super(DEContent.TILE_CRAFTING_CORE.get(), pos, state);
        capManager.setInternalManaged("inventory", Capabilities.Item.BLOCK, itemHandler).saveBoth();
        capManager.set(Capabilities.Item.BLOCK, itemHandler.automation);
        capManager.set(com.brandon3055.brandonscore.capability.CapabilityOP.OP, injectorFeed);
        itemHandler.setContentsChangeListener(i -> localInventoryChange());
        itemHandler.setStackValidator((slot, stack) -> slot == 0);
        fxHandler = DraconicEvolution.proxy.createFusionFXHandler(this);
        activeRecipe.addValueListener(e -> recipeCache = null);
    }

    @Override
    public void setLevel(net.minecraft.world.level.Level level) {
        super.setLevel(level);
        itemHandler.loadPending();
    }

    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Item.BLOCK, DEContent.TILE_CRAFTING_CORE.get(),
                (tile, side) -> tile.itemHandler.automation);
        com.brandon3055.brandonscore.capability.CapabilityOP.registerBlockEntity(event, DEContent.TILE_CRAFTING_CORE.get());
    }

    public void startCraft() {
        if (isCrafting()) {
            inventoryChanged();
            return;
        }

        updateInjectors();
        RecipeHolder<IFusionRecipe> holder = findRecipe();
        IFusionRecipe recipe = holder == null ? null : holder.value();
        setActiveRecipe(holder);
        if (recipe == null || !recipe.canStartCraft(this, level, null)) {
            return;
        }

        setCounter(0);
        setFusionState(FusionState.START);
        crafting.set(true);
    }

    @Override
    public void receivePacketFromClient(MCDataInput data, ServerPlayer client, int id) {
        if (id == 3) { // set every linked injector to single item (true) or stack (false) mode
            boolean single = data.readBoolean();
            for (IFusionInjector inj : getInjectors()) {
                if (inj instanceof TileFusionCraftingInjector tile) tile.singleItem.set(single);
            }
            return;
        }
        startCraft();
    }

    @Override
    public void receivePacketFromServer(MCDataInput data, int id) {
        if (id == 2) {
            synchronizedRecipe = data.readCompoundNBT();
            recipeCache = null;
        } else if (id == 0) {
            injectorPositions.clear();
            int count = data.readShort();
            for (int i = 0; i < count; i++) {
                injectorPositions.add(data.readPos());
            }
            injectorCache = null;
        } else if (id == 1) {// Craft Complete
            level.addParticle(ParticleTypes.EXPLOSION, getBlockPos().getX() + 0.5, getBlockPos().getY() + 0.5, getBlockPos().getZ() + 0.5, 1.0D, 0.0D, 0.0D);
//            level.playLocalSound(getBlockPos().getX() + 0.5, getBlockPos().getY() + 0.5, getBlockPos().getZ() + 0.5, SoundEvents.GENERIC_EXPLODE, SoundCategory.BLOCKS, 4.0F, (1.0F + (this.level.getRandom().nextFloat() - this.level.getRandom().nextFloat()) * 0.2F) * 0.7F, false);
            level.playLocalSound(getBlockPos().getX() + 0.5, getBlockPos().getY() + 0.5, getBlockPos().getZ() + 0.5, DESounds.FUSION_COMPLETE.get(), SoundSource.BLOCKS, 4.0F, (1.0F + (this.level.getRandom().nextFloat() - this.level.getRandom().nextFloat()) * 0.2F) * 0.7F, false);

            for (int i = 0; i < 100; i++) {
                double velX = (level.getRandom().nextDouble() - 0.5) * 0.1;
                double velY = (level.getRandom().nextDouble() - 0.5) * 0.1;
                double velZ = (level.getRandom().nextDouble() - 0.5) * 0.1;
                level.addParticle(new IntParticleData(DEParticles.ENERGY_BASIC.get(), 0, 255, 255, 64), getBlockPos().getX() + 0.5, getBlockPos().getY() + 0.5, getBlockPos().getZ() + 0.5, velX, velY, velZ);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level.isClientSide()) {
            fxHandler.tick();
        }

        if (crafting.get() && !level.isClientSide()) {
            RecipeHolder<IFusionRecipe> recipe = getActiveRecipe();
            if (recipe != null) {
                recipe.value().tickFusionState(this, this, level);
            } else {
                cancelCraft();
            }
        }
        if (!level.isClientSide()) {
            long t = level.getGameTime();
            if (t % 10 == 0) {
                // Progress does not mark the block changed, so tell comparators ourselves.
                int key = crafting.get() ? getComparatorOutput() : getOutputStack().isEmpty() ? -1 : 15;
                if (key != lastComparator) {
                    lastComparator = key;
                    level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
                }
            }
        }
    }

    public boolean updateInjectors() {
        minTierCache = null;
        if (isCrafting() || level.isClientSide()) {
            return true;
        }

        if (injectorPositions.isEmpty()) {
            setFusionStatus(-1, null);
        }

        injectorCache = null;
        List<BlockPos> oldPositions = new ArrayList<>(injectorPositions);
        injectorPositions.clear();
        int range = DEConfig.fusionInjectorRange;
        int radius = 1;
        List<TileFusionCraftingInjector> searchTiles = Streams.concat(
                        BlockPos.betweenClosedStream(worldPosition.offset(-range, -radius, -radius), worldPosition.offset(range, radius, radius)), //X
                        BlockPos.betweenClosedStream(worldPosition.offset(-radius, -range, -radius), worldPosition.offset(radius, range, radius)), //Y
                        BlockPos.betweenClosedStream(worldPosition.offset(-radius, -radius, -range), worldPosition.offset(radius, radius, range))  //Z
                )
                .map(level::getBlockEntity)
                .filter(e -> e instanceof TileFusionCraftingInjector)
                .map(e -> (TileFusionCraftingInjector) e)
                .collect(Collectors.toList());

        for (TileFusionCraftingInjector tile : searchTiles) {
            Vec3D dirVec = new Vec3D(tile.getBlockPos()).subtract(worldPosition);
            double dist = Utils.getCardinalDistance(tile.getBlockPos(), worldPosition);

            if (dist <= DEConfig.fusionInjectorMinDist) {
                setFusionStatus(-1, Component.translatable("fusion_status.draconicevolution.injector_close").withStyle(ChatFormatting.RED));
                injectorPositions.clear();
                return false;
            }

            if (Direction.getNearest((int) dirVec.x, (int) dirVec.y, (int) dirVec.z, Direction.NORTH) == tile.getRotation().getOpposite()) {
                BlockPos pos = tile.getBlockPos();
                Direction facing = tile.getRotation();
                boolean obstructed = false;
                for (BlockPos bp : BlockPos.betweenClosed(pos.relative(facing), pos.relative(facing, FacingUtils.distanceInDirection(pos, worldPosition, facing) - 1))) {
                    if (!level.isEmptyBlock(bp) && (level.getBlockState(bp).canOcclude() || level.getBlockEntity(bp) instanceof TileFusionCraftingInjector)) {
                        obstructed = true;
                        tile.setCore(null);
                        break;
                    }
                }
                if (!obstructed && tile.setCore(this)) {
                    injectorPositions.add(tile.getBlockPos());
                }
            }
        }

        dirtyBlock();
        if (!oldPositions.equals(injectorPositions)) {
            sendPacketToChunk(e -> {
                e.writeShort(injectorPositions.size());
                injectorPositions.forEach(e::writePos);
            }, 0);
        }
        return true;
    }

    public void inventoryChanged() {
        setChanged();
        if (!updateInjectors()) {
            return;
        }
        if (isCrafting()) {
            RecipeHolder<IFusionRecipe> recipe = getActiveRecipe();
            if (recipe == null || !recipe.value().matches(this, level)) {
                cancelCraft();
            }
        } else if (!level.isClientSide()) {
            RecipeHolder<IFusionRecipe> recipe = findRecipe();
            if (recipe != null) {
                recipe.value().canStartCraft(this, level, e -> setFusionStatus(-1, e));
            } else {
                setFusionStatus(-1, Component.translatable("fusion_status.draconicevolution.no_recipe").append(missingHint()));
            }
            setActiveRecipe(recipe);
        }
    }

    private void localInventoryChange() {
        updateBlock();
        inventoryChanged();
    }

    @Override
    public InteractionResult onBlockUse(BlockState state, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (player instanceof ServerPlayer) {
            updateInjectors(); //TODO just have the injectors poke the core when placed so i dont need this
            player.openMenu(this, worldPosition);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int currentWindowIndex, Inventory playerInventory, Player player) {
        return new FusionCraftingCoreMenu(currentWindowIndex, player.getInventory(), this);
    }

    @Nonnull
    @Override
    public ItemStack getCatalystStack() {
        return itemHandler.getStackInSlot(0);
    }

    @Nonnull
    @Override
    public ItemStack getOutputStack() {
        return itemHandler.getStackInSlot(1);
    }

    @Override
    public void setCatalystStack(@Nonnull ItemStack stack) {
        itemHandler.setStackInSlot(0, stack);
    }

    @Override
    public void setOutputStack(@Nonnull ItemStack stack) {
        itemHandler.setStackInSlot(1, stack);
    }

    @Override
    public List<IFusionInjector> getInjectors() {
        if (injectorCache == null) {
            injectorCache = injectorPositions.stream()
                    .map(level::getBlockEntity)
                    .filter(e -> e instanceof IFusionInjector)
                    .map(e -> (IFusionInjector) e)
                    .collect(Collectors.toList());
        }
        return injectorCache;
    }

    @Override
    public net.minecraft.core.HolderLookup.Provider registryAccess() {
        return java.util.Objects.requireNonNull(level, "Fusion core is not attached to a level").registryAccess();
    }

    @Override
    public ItemStack getItem(int index) {
        if (index <= 0) return itemHandler.getStackInSlot(0);
        index--;
        List<IFusionInjector> injectors = getInjectors();
        return index >= injectors.size() ? ItemStack.EMPTY : injectors.get(index).getInjectorStack();
    }

    @Override
    public int size() {
        return getInjectors().size() + 1;
    }

    @Override
    public TechLevel getMinimumTier() {
        if (minTierCache == null) {
            minTierCache = getInjectors().stream()
                    .filter(e -> !e.getInjectorStack().isEmpty())
                    .sorted(Comparator.comparing(e -> e.getInjectorTier().index))
                    .map(IFusionInjector::getInjectorTier)
                    .findFirst()
                    .orElse(TechLevel.DRACONIUM);
        }

        return minTierCache;
    }

    @Override
    public void onNeighborChange(BlockPos neighbor) {
        super.onNeighborChange(neighbor);
        updateInjectors();
    }

    @Override
    public FusionState getFusionState() {
        return fusionState.get();
    }

    @Override
    public void setFusionState(FusionState state) {
        fusionState.set(state);
        setChanged();
    }

    @Override
    public void completeCraft() {
        crafting.set(false);
        inventoryChanged();
        sendPacketToChunk(e -> {}, 1);
    }

    @Override
    public void cancelCraft() {
        crafting.set(false);
        getInjectors().forEach(e -> e.setEnergyRequirement(0, 0));
        setFusionStatus(-1, Component.translatable("fusion_status.draconicevolution.canceled"));
        level.playSound(null, getBlockPos(), DESounds.FUSION_COMPLETE.get(), SoundSource.BLOCKS, 4.0F, (1.0F + (this.level.getRandom().nextFloat() - this.level.getRandom().nextFloat()) * 0.2F) * 0.7F);
        inventoryChanged();
    }

    @Override
    public int getCounter() {
        return fusionCounter.get();
    }

    @Override
    public void setCounter(int count) {
        fusionCounter.set(count);
    }

    @Override
    public void setFusionStatus(double progress, Component stateText) {
        this.progress.set((float) progress);
        userStatus.set(stateText);
    }

    @Override
    public void setCraftAnimation(float progress, int length) {
        craftAnimProgress.set(progress);
        craftAnimLength.set((short) length);
    }

    /** A Fusion Conductor on top runs this core (auto-craft, output, injector toggles live on its screen). */
    public boolean hasConductor() {
        return level != null && level.getBlockEntity(worldPosition.above()) instanceof TileFusionConductor;
    }

    /** Read-only view of the linked injector positions (client uses it for the highlight button). */
    public List<BlockPos> getInjectorPositions() {
        return java.util.Collections.unmodifiableList(injectorPositions);
    }

    /** "Missing: A, B +2" for the closest recipe with this catalyst, or nothing. */
    private Component missingHint() {
        ItemStack catalyst = getCatalystStack();
        if (catalyst.isEmpty() || !(level instanceof net.minecraft.server.level.ServerLevel server)) return Component.empty();
        List<IFusionRecipe.IFusionIngredient> best = null;
        for (var holder : server.recipeAccess().recipeMap().byType(DraconicAPI.FUSION_RECIPE_TYPE.get())) {
            IFusionRecipe recipe = holder.value();
            if (!recipe.getCatalyst().test(catalyst)) continue;
            List<ItemStack> have = new ArrayList<>();
            getInjectors().forEach(e -> { if (!e.getInjectorStack().isEmpty()) have.add(e.getInjectorStack()); });
            List<IFusionRecipe.IFusionIngredient> missing = new ArrayList<>();
            for (var ing : recipe.fusionIngredients()) {
                ItemStack hit = have.stream().filter(s -> ing.get().test(s)).findFirst().orElse(null);
                if (hit != null) have.remove(hit); else missing.add(ing);
            }
            if (!missing.isEmpty() && (best == null || missing.size() < best.size())) best = missing;
        }
        if (best == null) return Component.empty();
        StringBuilder names = new StringBuilder();
        for (int i = 0; i < Math.min(2, best.size()); i++) {
            if (i > 0) names.append(", ");
            names.append(best.get(i).get().items().findFirst().map(h -> new ItemStack(h).getHoverName().getString()).orElse("?"));
        }
        if (best.size() > 2) names.append(" +").append(best.size() - 2);
        return Component.literal("\nMissing: " + names).withStyle(ChatFormatting.GRAY);
    }

    public boolean isCrafting() {
        return crafting.get();
    }

    @Nullable
    private RecipeHolder<IFusionRecipe> findRecipe() {
        return level instanceof net.minecraft.server.level.ServerLevel server
                ? server.recipeAccess().getRecipeFor(DraconicAPI.FUSION_RECIPE_TYPE.get(), this, server).orElse(null) : null;
    }

    @Nullable
    public RecipeHolder<IFusionRecipe> getActiveRecipe() {
        if (level == null || activeRecipe.get() == null) return null;
        var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, activeRecipe.get());
        if (level instanceof net.minecraft.server.level.ServerLevel server) {
            // Resolve against the current manager so /reload cannot leave a stale cached recipe.
            var holder = server.recipeAccess().byKey(key).orElse(null);
            if (holder == null || !(holder.value() instanceof IFusionRecipe fusion)) return null;
            var resolved = new RecipeHolder<IFusionRecipe>(key, fusion);
            if (recipeCache == null || recipeCache.value() != fusion) setActiveRecipe(resolved);
            return resolved;
        }
        if (recipeCache == null && synchronizedRecipe.getStringOr("id", "").equals(activeRecipe.get().toString())) {
            var ops = registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
            net.minecraft.world.item.crafting.Recipe.CODEC.parse(ops, synchronizedRecipe.getCompoundOrEmpty("recipe"))
                    .result().filter(IFusionRecipe.class::isInstance).map(IFusionRecipe.class::cast)
                    .ifPresent(recipe -> recipeCache = new RecipeHolder<>(key, recipe));
        }
        return recipeCache;
    }

    public void setActiveRecipe(@Nullable RecipeHolder<IFusionRecipe> recipe) {
        activeRecipe.set(recipe == null ? null : recipe.id().identifier());
        recipeCache = recipe;
        synchronizedRecipe = new CompoundTag();
        if (recipe != null && level != null) {
            var ops = registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
            synchronizedRecipe.putString("id", recipe.id().identifier().toString());
            synchronizedRecipe.put("recipe", net.minecraft.world.item.crafting.Recipe.CODEC.encodeStart(ops, recipe.value()).getOrThrow());
        }
        if (level != null && !level.isClientSide()) {
            sendPacketToChunk(data -> data.writeCompoundNBT(synchronizedRecipe), 2);
        }
    }

    public int getComparatorOutput() {
        if (!getOutputStack().isEmpty()) {
            return 15;
        } else if (crafting.get()) {
            // 1-7 while charging, 8-14 while crafting, 15 = output ready.
            int step = Math.round(net.minecraft.util.Mth.clamp(progress.get(), 0F, 1F) * 6);
            return (getFusionState() == FusionState.CRAFTING ? 8 : 1) + step;
        } else {
            RecipeHolder<IFusionRecipe> recipe = findRecipe();
            if (recipe != null && recipe.value().canStartCraft(this, level, null)) {
                return 1;
            }
            return 0;
        }
    }

    @Override
    public void writeExtraNBT(CompoundTag compound) {
        super.writeExtraNBT(compound);
        if (level instanceof net.minecraft.server.level.ServerLevel && activeRecipe.get() != null) getActiveRecipe();
        compound.put("fusion_recipe_sync", synchronizedRecipe.copy());
        compound.putLongArray("injector_positions", injectorPositions.stream().mapToLong(BlockPos::asLong).toArray());
    }

    @Override
    public void readExtraNBT(CompoundTag compound) {
        super.readExtraNBT(compound);
        synchronizedRecipe = compound.getCompoundOrEmpty("fusion_recipe_sync").copy();
        recipeCache = null;
        injectorPositions = Arrays.stream(compound.getLongArray("injector_positions").orElseGet(() -> new long[0])).mapToObj(BlockPos::of).collect(Collectors.toList());
        injectorCache = null;
    }
}

