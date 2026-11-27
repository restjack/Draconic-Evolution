package com.brandon3055.draconicevolution.blocks.tileentity;

import com.brandon3055.brandonscore.api.power.OPStorage;
import com.brandon3055.brandonscore.blocks.TileBCore;
import com.brandon3055.brandonscore.capability.CapabilityOP;
import com.brandon3055.brandonscore.lib.IInteractTile;
import com.brandon3055.brandonscore.lib.datamanager.DataFlags;
import com.brandon3055.brandonscore.lib.datamanager.ManagedBool;
import com.brandon3055.brandonscore.lib.datamanager.ManagedByte;
import com.brandon3055.brandonscore.lib.datamanager.ManagedInt;
import com.brandon3055.brandonscore.lib.datamanager.ManagedLong;
import com.brandon3055.brandonscore.lib.datamanager.ManagedString;
import com.brandon3055.brandonscore.lib.datamanager.ManagedUUID;
import com.brandon3055.brandonscore.lib.datamanager.ManagedResource;
import codechicken.lib.data.MCDataInput;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import com.brandon3055.brandonscore.lib.datamanager.ManagedFloat;
import com.brandon3055.brandonscore.lib.datamanager.ManagedTextComponent;
import com.brandon3055.draconicevolution.DEConfig;
import com.brandon3055.draconicevolution.api.DraconicAPI;
import com.brandon3055.draconicevolution.api.crafting.IFusionInjector;
import com.brandon3055.draconicevolution.api.crafting.IFusionRecipe;
import com.brandon3055.draconicevolution.api.crafting.IFusionStateMachine.FusionState;
import com.brandon3055.draconicevolution.api.crafting.StackIngredient;
import com.brandon3055.draconicevolution.init.DEContent;
import com.brandon3055.draconicevolution.inventory.FusionConductorMenu;
import com.brandon3055.draconicevolution.inventory.ManagedItemHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Fusion Conductor (restjack fork): sits on top of a Fusion Crafting Core and automates it.
 * Loads catalyst + ingredients from its input into the core and injectors, powers the injectors from its own
 * buffer (optional), and collects the result. A Draconic capacitor in the upgrade slot speeds charging and the
 * craft phase up at an energy premium (energy = speed ^ exponent, see DEConfig).
 */
public class TileFusionConductor extends TileBCore implements MenuProvider, IInteractTile {

    public static final int INPUT = 18, OUTPUT = 9;
    public static final int OUT_START = INPUT, OUT_END = INPUT + OUTPUT, CAP_SLOT = INPUT + OUTPUT;

    public final ManagedBool powerInjectors = register(new ManagedBool("power_injectors", true, DataFlags.SAVE_NBT_SYNC_TILE));
    /** True while energy flows to injectors (client draws the beams). */
    public final ManagedBool powering = register(new ManagedBool("powering", DataFlags.SYNC_TILE));
    public final ManagedTextComponent status = register(new ManagedTextComponent("status", DataFlags.SAVE_NBT_SYNC_CONTAINER));
    public final ManagedFloat progress = register(new ManagedFloat("progress", -1, DataFlags.SYNC_CONTAINER));
    // Stage 2: lock, batch, redstone, toggles moved over from the core screen.
    public final ManagedResource recipeLock = register(new ManagedResource("recipe_lock", DataFlags.SAVE_NBT_SYNC_TILE));
    public final ManagedInt batch = register(new ManagedInt("batch", DataFlags.SAVE_NBT_SYNC_CONTAINER));
    public final ManagedInt batchLeft = register(new ManagedInt("batch_left", DataFlags.SAVE_NBT_SYNC_CONTAINER));
    // Settings change only through packet 4 (owner check), never CLIENT_CONTROL (that path has no player to check).
    public final ManagedBool autoCraft = register(new ManagedBool("auto_craft", true, DataFlags.SAVE_NBT_SYNC_TILE));
    public final ManagedBool pushOutput = register(new ManagedBool("push_output", DataFlags.SAVE_NBT_SYNC_TILE));
    /** 0 ignore, 1 run while powered, 2 run while unpowered, 3 one craft per pulse. */
    public final ManagedByte redstoneMode = register(new ManagedByte("redstone_mode", DataFlags.SAVE_NBT_SYNC_TILE));
    // Stage 3: owner lock + stats + computer start.
    public final ManagedUUID owner = register(new ManagedUUID("owner", (java.util.UUID) null, DataFlags.SAVE_NBT_SYNC_TILE));
    public final ManagedString ownerName = register(new ManagedString("owner_name", "", DataFlags.SAVE_NBT_SYNC_TILE));
    public final ManagedLong crafts = register(new ManagedLong("crafts", DataFlags.SAVE_NBT_SYNC_CONTAINER));
    public final ManagedLong energyUsed = register(new ManagedLong("energy_used", DataFlags.SAVE_NBT_SYNC_CONTAINER));
    private final ManagedInt oneShot = register(new ManagedInt("one_shot", DataFlags.SAVE_NBT));
    public static final int SET_POWER = 0, SET_AUTO = 1, SET_PUSH = 2, SET_RS = 3;
    private final ManagedBool rsPowered = register(new ManagedBool("rs_powered", DataFlags.SAVE_NBT));
    private final ManagedInt pulses = register(new ManagedInt("pulses", DataFlags.SAVE_NBT));
    /** True from a craft this conductor started until it is collected. */
    private final ManagedBool running = register(new ManagedBool("running", DataFlags.SAVE_NBT));
    public static final int RS_IGNORE = 0, RS_HIGH = 1, RS_LOW = 2, RS_PULSE = 3;

    public final ManagedItemHandler itemHandler = new ManagedItemHandler(this, INPUT + OUTPUT + 1);
    /** Pipes, hoppers, AE2: insert into input only, extract from output only. */
    public final ResourceHandler<ItemResource> automation = itemHandler.view((slot, stack) -> slot < INPUT, (slot, stack) -> slot >= OUT_START && slot < OUT_END);
    public final OPStorage opStorage = new OPStorage(this, bufferSize(0), Long.MAX_VALUE, 0);

    /** Injectors this conductor loaded (anything else holding an item was placed by hand). */
    private final Set<BlockPos> loaded = new LinkedHashSet<>();
    private boolean loadedCatalyst = false;
    private int cooldown = 0;
    private float ring, prevRing;
    private int lastComparator = -1;

    public TileFusionConductor(BlockPos pos, BlockState state) {
        super(DEContent.TILE_FUSION_CONDUCTOR.get(), pos, state);
        capManager.setManaged("energy", CapabilityOP.OP, opStorage).saveBoth().syncContainer();
        // Tile only: breaking the block drops the items, it never packs them into the item.
        capManager.setInternalManaged("inventory", Capabilities.Item.BLOCK, itemHandler).saveTile();
        capManager.set(Capabilities.Item.BLOCK, automation);
        itemHandler.setStackValidator((slot, stack) -> slot < INPUT ? acceptsInput(stack) : slot == CAP_SLOT ? capacitorTier(stack) > 0 : true);
        itemHandler.setContentsChangeListener(slot -> {
            if (slot == CAP_SLOT) updateCapacity();
            setChanged();
        });
    }

    public static void register(RegisterCapabilitiesEvent event) {
        CapabilityOP.registerBlockEntity(event, DEContent.TILE_FUSION_CONDUCTOR.get());
        event.registerBlockEntity(Capabilities.Item.BLOCK, DEContent.TILE_FUSION_CONDUCTOR.get(), (tile, side) -> tile.automation);
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        itemHandler.loadPending();
        updateCapacity();
    }

    //region Speed / energy

    /** 0 = empty, 1 Wyvern, 2 Draconic, 3 Chaotic (creative counts as Chaotic). */
    public static int capacitorTier(ItemStack stack) {
        Item item = stack.getItem();
        if (item == DEContent.CAPACITOR_WYVERN.get()) return 1;
        if (item == DEContent.CAPACITOR_DRACONIC.get()) return 2;
        if (item == DEContent.CAPACITOR_CHAOTIC.get() || item == DEContent.CAPACITOR_CREATIVE.get()) return 3;
        return 0;
    }

    public int tier() {
        return capacitorTier(itemHandler.getStackInSlot(CAP_SLOT));
    }

    public static int speed(int tier) {
        return Math.max(1, DEConfig.conductorSpeed.get(tier));
    }

    /** Energy multiplier: the configured value, or speed ^ exponent when that is 0. */
    public static double energyFactor(int tier) {
        double set = DEConfig.conductorEnergy.get(tier);
        return set > 0 ? set : Math.pow(speed(tier), DEConfig.conductorEnergyExponent);
    }

    public static long bufferSize(int tier) {
        return Math.max(1, DEConfig.conductorBuffer.get(tier));
    }

    private void updateCapacity() {
        opStorage.setCapacity(bufferSize(tier()));
        if (opStorage.getOPStored() > opStorage.getMaxOPStored()) opStorage.modifyEnergyStored(opStorage.getMaxOPStored() - opStorage.getOPStored());
    }

    //endregion

    @Nullable
    public TileFusionCraftingCore getCore() {
        return level != null && level.getBlockEntity(worldPosition.below()) instanceof TileFusionCraftingCore core ? core : null;
    }

    @Override
    public void tick() {
        super.tick();
        if (level.isClientSide()) {
            prevRing = ring;
            TileFusionCraftingCore c = getCore();
            ring += c != null && c.isCrafting() ? 12 : 2;
            return;
        }
        if (!DEConfig.extra("fusionConductor")) {
            setStatus(Component.translatable("fusion_conductor.draconicevolution.disabled").withStyle(ChatFormatting.RED), -1);
            powering.set(false);
            return;
        }
        if (onInterval(10)) {
            int key = getComparatorOutput();
            if (key != lastComparator) {
                lastComparator = key;
                level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            }
        }
        TileFusionCraftingCore core = getCore();
        if (core == null) {
            setStatus(Component.translatable("fusion_conductor.draconicevolution.no_core").withStyle(ChatFormatting.RED), -1);
            powering.set(false);
            return;
        }
        if (core.isCrafting()) {
            feed(core);
            if (onInterval(10)) setStatus(craftStatus(core), core.progress.get());
            return;
        }
        powering.set(false);
        if (cooldown > 0) cooldown--;
        if (!onInterval(10)) return;
        boolean finished = running.get() && !core.getOutputStack().isEmpty();
        if (!collect(core)) return;
        if (running.get()) {
            running.set(false);
            if (finished) {
                crafts.add(1);
                if (batch.get() > 0 && batchLeft.get() > 0) batchLeft.subtract(1);
            }
        }
        if (pushOutput.get()) pushOutputItems();
        Component wait = waitReason();
        if (wait != null) {
            setStatus(wait, -1);
            return;
        }
        if (cooldown <= 0) tryStart(core);
    }

    /** Why the conductor may not start a craft right now (auto-craft, batch, redstone), or null. */
    @Nullable
    private Component waitReason() {
        if (oneShot.get() > 0) return null;
        if (!autoCraft.get()) return Component.translatable("fusion_conductor.draconicevolution.paused").withStyle(ChatFormatting.GRAY);
        if (batch.get() > 0 && batchLeft.get() <= 0) return Component.translatable("fusion_conductor.draconicevolution.batch_done", batch.get()).withStyle(ChatFormatting.GREEN);
        boolean powered = rsPowered.get();
        return switch (redstoneMode.get()) {
            case RS_HIGH -> powered ? null : Component.translatable("fusion_conductor.draconicevolution.rs_wait_high").withStyle(ChatFormatting.GRAY);
            case RS_LOW -> powered ? Component.translatable("fusion_conductor.draconicevolution.rs_wait_low").withStyle(ChatFormatting.GRAY) : null;
            case RS_PULSE -> pulses.get() > 0 ? null : Component.translatable("fusion_conductor.draconicevolution.rs_wait_pulse").withStyle(ChatFormatting.GRAY);
            default -> null;
        };
    }

    @Override
    public void onNeighborChange(BlockPos neighbor) {
        super.onNeighborChange(neighbor);
        if (level == null || level.isClientSide()) return;
        boolean now = level.hasNeighborSignal(worldPosition);
        if (now && !rsPowered.get() && redstoneMode.get() == RS_PULSE) pulses.set(Math.min(64, pulses.get() + 1));
        rsPowered.set(now);
    }

    /** Push output into adjacent inventories (never into the core below). */
    private void pushOutputItems() {
        for (Direction dir : Direction.values()) {
            if (dir == Direction.DOWN) continue;
            var handler = level.getCapability(Capabilities.Item.BLOCK, worldPosition.relative(dir), dir.getOpposite());
            if (handler == null) continue;
            for (int i = OUT_START; i < OUT_END; i++) {
                ItemStack out = itemHandler.getStackInSlot(i);
                if (out.isEmpty()) continue;
                try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                    int moved = handler.insert(ItemResource.of(out), out.getCount(), tx);
                    if (moved > 0) {
                        tx.commit();
                        itemHandler.setStackInSlot(i, out.copyWithCount(out.getCount() - moved));
                    }
                }
            }
        }
    }

    private void setStatus(Component text, float prog) {
        status.set(text);
        progress.set(prog);
    }

    private Component craftStatus(TileFusionCraftingCore core) {
        Component base = core.userStatus.get() == null ? Component.empty() : core.userStatus.get();
        int tier = tier();
        if (tier == 0 || !powerInjectors.get()) return base;
        return Component.empty().append(base).append(Component.literal("\nSpeed " + speed(tier) + "x, energy " + trim(energyFactor(tier)) + "x").withStyle(ChatFormatting.DARK_PURPLE));
    }

    public static String trim(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.format("%.1f", v);
    }

    /** Powers the running craft's injectors (charge phase) and speeds the craft phase up. */
    private void feed(TileFusionCraftingCore core) {
        if (!powerInjectors.get()) {
            powering.set(false);
            return;
        }
        int tier = tier();
        int speed = speed(tier);
        if (core.getFusionState() == FusionState.CHARGING) {
            double factor = energyFactor(tier);
            boolean needed = false;
            for (IFusionInjector inj : core.getInjectors()) {
                if (!(inj instanceof TileFusionCraftingInjector tile) || tile.getEnergyRequirement() <= 0) continue;
                long need = tile.getEnergyRequirement() - tile.getInjectorEnergy();
                if (need <= 0) continue;
                needed = true;
                long want = Math.min(need, Math.max(1, tile.chargeRate.get()) * speed);
                long give = Math.min(want, (long) (opStorage.getOPStored() / factor));
                if (give <= 0) continue; // low power: wait, never cancel
                tile.energy.add(give);
                energyUsed.add(opStorage.modifyEnergyStored(-Math.min(opStorage.getOPStored(), (long) Math.ceil(give * factor))));
            }
            powering.set(needed);
        } else {
            powering.set(false);
            if (core.getFusionState() == FusionState.CRAFTING && speed > 1) core.setCounter(core.getCounter() + speed - 1);
        }
    }

    //region Collect / load

    /** Takes the result and anything this conductor loaded back out of the core. False = blocked. */
    private boolean collect(TileFusionCraftingCore core) {
        ItemStack out = core.getOutputStack();
        if (!out.isEmpty()) {
            ItemStack rest = insert(out.copy(), OUT_START, OUT_END);
            core.setOutputStack(rest);
            if (!rest.isEmpty()) {
                setStatus(Component.translatable("fusion_conductor.draconicevolution.output_full").withStyle(ChatFormatting.RED), -1);
                return false;
            }
        }
        if (loadedCatalyst) {
            ItemStack cat = core.getCatalystStack();
            if (!cat.isEmpty()) core.setCatalystStack(giveBack(cat.copy()));
            if (!core.getCatalystStack().isEmpty()) return blockedReturn();
            loadedCatalyst = false;
        }
        for (BlockPos pos : new ArrayList<>(loaded)) {
            if (level.getBlockEntity(pos) instanceof TileFusionCraftingInjector inj && !inj.getInjectorStack().isEmpty()) {
                inj.setInjectorStack(giveBack(inj.getInjectorStack().copy()));
                if (!inj.getInjectorStack().isEmpty()) return blockedReturn();
            }
            loaded.remove(pos);
            updateBlock();
        }
        setChanged();
        return true;
    }

    private boolean blockedReturn() {
        setStatus(Component.translatable("fusion_conductor.draconicevolution.return_full").withStyle(ChatFormatting.RED), -1);
        return false;
    }

    /** Leftovers go back to input if fusion can use them, else (bucket remainders etc.) to output. */
    private ItemStack giveBack(ItemStack stack) {
        if (usedByFusion(level, stack)) stack = insert(stack, 0, INPUT);
        return insert(stack, OUT_START, OUT_END);
    }

    /** Raw insert into [from, to), skipping validators. Returns what did not fit. */
    private ItemStack insert(ItemStack stack, int from, int to) {
        for (int pass = 0; pass < 2 && !stack.isEmpty(); pass++) {
            for (int i = from; i < to && !stack.isEmpty(); i++) {
                ItemStack have = itemHandler.getStackInSlot(i);
                if (pass == 0 ? have.isEmpty() || !ItemStack.isSameItemSameComponents(have, stack) : !have.isEmpty()) continue;
                int room = stack.getMaxStackSize() - have.getCount();
                int move = Math.min(room, stack.getCount());
                if (move <= 0) continue;
                itemHandler.setStackInSlot(i, stack.copyWithCount(have.getCount() + move));
                stack = stack.copyWithCount(stack.getCount() - move);
            }
        }
        return stack;
    }

    private record Use(int slot, int count) {}

    private record Plan(RecipeHolder<IFusionRecipe> recipe, Use catalyst, List<Use> ingredients, List<TileFusionCraftingInjector> injectors) {}

    private static int count(Ingredient ing) {
        return ing.getCustomIngredient() instanceof StackIngredient s ? Math.max(1, s.getCount()) : 1;
    }

    /** Finds an input slot holding enough of an ingredient (one slot per ingredient). */
    private int find(Ingredient ing, int need, int[] avail) {
        for (int i = 0; i < INPUT; i++) {
            if (avail[i] >= need && ing.test(itemHandler.getStackInSlot(i).copyWithCount(need))) return i;
        }
        return -1;
    }

    private void tryStart(TileFusionCraftingCore core) {
        if (!core.getCatalystStack().isEmpty()) {
            setStatus(Component.translatable("fusion_conductor.draconicevolution.hand_catalyst").withStyle(ChatFormatting.RED), -1);
            return;
        }
        if (isInputEmpty()) {
            setStatus(Component.translatable("fusion_conductor.draconicevolution.waiting").withStyle(ChatFormatting.GRAY), -1);
            return;
        }
        core.updateInjectors();
        List<TileFusionCraftingInjector> free = new ArrayList<>();
        for (IFusionInjector inj : core.getInjectors()) {
            if (!(inj instanceof TileFusionCraftingInjector tile)) continue;
            if (!tile.getInjectorStack().isEmpty()) {
                // Fusion needs every linked injector empty except the recipe's, so a hand-placed item blocks it.
                BlockPos p = tile.getBlockPos();
                setStatus(Component.translatable("fusion_conductor.draconicevolution.foreign", p.getX(), p.getY(), p.getZ()).withStyle(ChatFormatting.RED), -1);
                return;
            }
            free.add(tile);
        }
        free.sort(Comparator.comparingInt(t -> t.getInjectorTier().index));

        Component reason = null;
        int bestMissing = Integer.MAX_VALUE;
        for (RecipeHolder<IFusionRecipe> holder : recipes(level)) {
            if (recipeLock.get() != null && !recipeLock.get().equals(holder.id().identifier())) continue;
            IFusionRecipe recipe = holder.value();
            int[] avail = new int[INPUT];
            for (int i = 0; i < INPUT; i++) avail[i] = itemHandler.getStackInSlot(i).getCount();
            int catNeed = count(recipe.getCatalyst());
            int catSlot = find(recipe.getCatalyst(), catNeed, avail);
            if (catSlot < 0) continue;
            avail[catSlot] -= catNeed;
            List<Use> uses = new ArrayList<>();
            List<String> missing = new ArrayList<>();
            for (IFusionRecipe.IFusionIngredient ing : recipe.fusionIngredients()) {
                int need = count(ing.get());
                int slot = find(ing.get(), need, avail);
                if (slot < 0) {
                    missing.add(ing.get().items().findFirst().map(h -> new ItemStack(h).getHoverName().getString()).orElse("?"));
                } else {
                    avail[slot] -= need;
                    uses.add(new Use(slot, need));
                }
            }
            if (!missing.isEmpty()) {
                if (missing.size() < bestMissing) {
                    bestMissing = missing.size();
                    String names = String.join(", ", missing.subList(0, Math.min(2, missing.size()))) + (missing.size() > 2 ? " +" + (missing.size() - 2) : "");
                    reason = Component.translatable("fusion_conductor.draconicevolution.missing", recipe.getResultItem(level.registryAccess()).getHoverName(), names).withStyle(ChatFormatting.GRAY);
                }
                continue;
            }
            int tierIdx = recipe.getRecipeTier().index;
            List<TileFusionCraftingInjector> usable = free.stream().filter(t -> t.getInjectorTier().index >= tierIdx).toList();
            if (usable.size() < uses.size()) {
                if (bestMissing > 0) {
                    bestMissing = 0;
                    reason = Component.translatable("fusion_conductor.draconicevolution.injectors", uses.size(), Component.translatable("tech_level.draconicevolution." + recipe.getRecipeTier().name().toLowerCase()), usable.size()).withStyle(ChatFormatting.RED);
                }
                continue;
            }
            load(core, new Plan(holder, new Use(catSlot, catNeed), uses, usable.subList(0, uses.size())));
            return;
        }
        setStatus(reason != null ? reason : Component.translatable("fusion_conductor.draconicevolution.no_recipe").withStyle(ChatFormatting.GRAY), -1);
    }

    private void load(TileFusionCraftingCore core, Plan plan) {
        core.setCatalystStack(take(plan.catalyst()));
        loadedCatalyst = true;
        for (int i = 0; i < plan.ingredients().size(); i++) {
            TileFusionCraftingInjector inj = plan.injectors().get(i);
            inj.setInjectorStack(take(plan.ingredients().get(i)));
            loaded.add(inj.getBlockPos().immutable());
        }
        setChanged();
        updateBlock();
        core.startCraft();
        if (core.isCrafting()) {
            running.set(true);
            if (oneShot.get() > 0) oneShot.subtract(1);
            else if (redstoneMode.get() == RS_PULSE && pulses.get() > 0) pulses.subtract(1);
        } else {
            // Next collect puts everything back; wait a bit so we don't load/unload every half second.
            cooldown = 100;
            setStatus(core.userStatus.get() == null ? Component.translatable("fusion_conductor.draconicevolution.no_recipe") : core.userStatus.get(), -1);
        }
    }

    private ItemStack take(Use use) {
        ItemStack have = itemHandler.getStackInSlot(use.slot());
        ItemStack taken = have.copyWithCount(use.count());
        itemHandler.setStackInSlot(use.slot(), have.copyWithCount(have.getCount() - use.count()));
        return taken;
    }

    private boolean isInputEmpty() {
        for (int i = 0; i < INPUT; i++) if (!itemHandler.getStackInSlot(i).isEmpty()) return false;
        return true;
    }

    //endregion

    //region Recipes / input filter

    /** Input filter: the locked recipe's items, or any item some fusion recipe uses. */
    public boolean acceptsInput(ItemStack stack) {
        RecipeHolder<IFusionRecipe> lock = lockedRecipe();
        if (lock == null) return usedByFusion(level, stack);
        IFusionRecipe r = lock.value();
        return r.getCatalyst().test(stack.copyWithCount(count(r.getCatalyst()))) || r.fusionIngredients().stream().anyMatch(i -> i.get().test(stack.copyWithCount(count(i.get()))));
    }

    @Nullable
    public RecipeHolder<IFusionRecipe> lockedRecipe() {
        Identifier id = recipeLock.get();
        if (id == null || level == null) return null;
        for (RecipeHolder<IFusionRecipe> h : recipes(level)) if (h.id().identifier().equals(id)) return h;
        return null;
    }

    /** First fusion recipe whose result is this item (client side helper for the ghost slot). */
    @Nullable
    public static RecipeHolder<IFusionRecipe> recipeFor(Level level, ItemStack result) {
        for (RecipeHolder<IFusionRecipe> h : recipes(level)) {
            if (ItemStack.isSameItem(h.value().getResultItem(level.registryAccess()), result)) return h;
        }
        return null;
    }

    /** Owner, the owner's FTB Teams party (or vanilla scoreboard team), or an operator. Unowned = anyone. */
    public boolean canControl(ServerPlayer player) {
        if (owner.isNull() || player.getUUID().equals(owner.get())) return true;
        if (net.minecraft.commands.Commands.hasPermission(net.minecraft.commands.Commands.LEVEL_GAMEMASTERS).test(player.createCommandSourceStack())) return true;
        if (level instanceof ServerLevel sl) {
            ServerPlayer o = sl.getServer().getPlayerList().getPlayer(owner.get());
            return TileCoreMonitor.teamMembers(sl, owner.get(), o).contains(player)
                    || (o == null && sameScoreboardTeam(sl, player));
        }
        return false;
    }

    /** Owner offline: compare scoreboard teams by name (FTB Teams lists online members only). */
    private boolean sameScoreboardTeam(ServerLevel sl, ServerPlayer player) {
        var team = sl.getServer().getScoreboard().getPlayersTeam(player.getScoreboardName());
        return team != null && !ownerName.get().isEmpty() && team.getPlayers().contains(ownerName.get());
    }

    public void setOwner(@Nullable Player player) {
        owner.set(player == null ? null : player.getUUID());
        ownerName.set(player == null ? "" : player.getName().getString());
    }

    /** Computer / API: start one craft now, even with auto-craft off or the redstone mode waiting. */
    public void requestCraft() {
        oneShot.set(Math.min(64, oneShot.get() + 1));
    }

    public void setLock(@Nullable Identifier rid) {
        recipeLock.set(rid != null && recipes(level).stream().anyMatch(h -> h.id().identifier().equals(rid)) ? rid : null);
    }

    public void setBatch(int v) {
        v = Math.max(0, Math.min(9999, v));
        batch.set(v);
        batchLeft.set(v);
    }

    @Override
    public void receivePacketFromClient(MCDataInput data, ServerPlayer client, int id) {
        if (id >= 1 && id <= 4 && !canControl(client)) {
            client.sendOverlayMessage(Component.translatable("fusion_conductor.draconicevolution.not_owner", ownerName.get().isEmpty() ? "the owner" : ownerName.get()).withStyle(ChatFormatting.RED));
            return;
        }
        switch (id) {
            case 4 -> { // setting: byte which, byte value
                int which = data.readByte(), v = data.readByte();
                switch (which) {
                    case SET_POWER -> powerInjectors.set(v != 0);
                    case SET_AUTO -> autoCraft.set(v != 0);
                    case SET_PUSH -> pushOutput.set(v != 0);
                    case SET_RS -> redstoneMode.set((byte) Math.floorMod(v, 4));
                }
            }
            case 1 -> { // set recipe lock ("" = clear)
                String key = data.readString();
                setLock(key.isEmpty() ? null : Identifier.tryParse(key));
            }
            case 2 -> setBatch(batch.get() + data.readInt()); // batch delta; resets the count left
            case 3 -> { // every injector of the core: single item (true) / stack
                boolean single = data.readBoolean();
                TileFusionCraftingCore core = getCore();
                if (core != null) for (IFusionInjector inj : core.getInjectors()) if (inj instanceof TileFusionCraftingInjector t) t.singleItem.set(single);
            }
            default -> super.receivePacketFromClient(data, client, id);
        }
    }

    /** Comparator: the core's 1-14 while crafting, 0 otherwise. */
    public int getComparatorOutput() {
        TileFusionCraftingCore core = getCore();
        return core != null && core.isCrafting() ? Math.min(14, core.getComparatorOutput()) : 0;
    }

    public boolean isLoaded(BlockPos pos) {
        return loaded.contains(pos);
    }

    public static Collection<RecipeHolder<IFusionRecipe>> recipes(Level level) {
        if (level instanceof ServerLevel server) return server.recipeAccess().recipeMap().byType(DraconicAPI.FUSION_RECIPE_TYPE.get());
        return level != null && level.isClientSide() ? com.brandon3055.draconicevolution.client.handler.ClientEventHandler.fusionRecipes : List.of();
    }

    // [0] server, [1] client: singleplayer runs both threads, so each side keeps its own cache.
    @SuppressWarnings("unchecked")
    private static final Collection<RecipeHolder<IFusionRecipe>>[] filterSource = new Collection[2];
    @SuppressWarnings("unchecked")
    private static final Set<Item>[] filterItems = new Set[]{Set.of(), Set.of()};

    /** Input filter: only items some fusion recipe uses (item-level, ignores components). */
    public static boolean usedByFusion(@Nullable Level level, ItemStack stack) {
        if (level == null || stack.isEmpty()) return level == null;
        Collection<RecipeHolder<IFusionRecipe>> source = recipes(level);
        int side = level.isClientSide() ? 1 : 0;
        if (source != filterSource[side]) {
            // ponytail: rebuilt when the recipe collection object changes (reload / join); one shared cache for both sides.
            Set<Item> items = new HashSet<>();
            for (RecipeHolder<IFusionRecipe> holder : source) {
                holder.value().getCatalyst().items().forEach(h -> items.add(h.value()));
                holder.value().fusionIngredients().forEach(i -> i.get().items().forEach(h -> items.add(h.value())));
            }
            filterItems[side] = items;
            filterSource[side] = source;
        }
        return filterItems[side].contains(stack.getItem());
    }

    //endregion

    /** Break: cancel a running craft, take back what we loaded, drop everything. */
    public void dropContents() {
        if (level == null || level.isClientSide()) return;
        List<ItemStack> drops = new ArrayList<>();
        TileFusionCraftingCore core = getCore();
        if (core != null) {
            if (core.isCrafting() && (loadedCatalyst || !loaded.isEmpty())) core.cancelCraft();
            if (!core.isCrafting()) {
                if (loadedCatalyst && !core.getCatalystStack().isEmpty()) {
                    drops.add(core.getCatalystStack().copy());
                    core.setCatalystStack(ItemStack.EMPTY);
                }
                for (BlockPos pos : loaded) {
                    if (level.getBlockEntity(pos) instanceof TileFusionCraftingInjector inj && !inj.getInjectorStack().isEmpty()) {
                        drops.add(inj.getInjectorStack().copy());
                        inj.setInjectorStack(ItemStack.EMPTY);
                    }
                }
            }
        }
        loaded.clear();
        loadedCatalyst = false;
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            drops.add(itemHandler.getStackInSlot(i).copy());
            itemHandler.setStackInSlot(i, ItemStack.EMPTY);
        }
        drops.forEach(s -> { if (!s.isEmpty()) Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, s); });
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        dropContents();
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    public InteractionResult onBlockUse(BlockState state, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (player instanceof ServerPlayer) {
            player.openMenu(this, worldPosition);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new FusionConductorMenu(id, inv, this);
    }

    public float ringAngle(float partialTicks) {
        return prevRing + (ring - prevRing) * partialTicks;
    }

    /** Client: injectors to draw beams to (the core below syncs its linked positions). */
    public List<BlockPos> beamTargets() {
        TileFusionCraftingCore core = getCore();
        if (core == null || !powering.get()) return List.of();
        List<BlockPos> list = new ArrayList<>();
        for (BlockPos p : core.getInjectorPositions()) {
            if (level.getBlockEntity(p) instanceof TileFusionCraftingInjector inj && !inj.getInjectorStack().isEmpty()) list.add(p);
        }
        return list;
    }

    @Override
    public void writeExtraNBT(CompoundTag nbt) {
        super.writeExtraNBT(nbt);
        nbt.putLongArray("loaded", loaded.stream().mapToLong(BlockPos::asLong).toArray());
        nbt.putBoolean("loaded_catalyst", loadedCatalyst);
    }

    @Override
    public void readExtraNBT(CompoundTag nbt) {
        super.readExtraNBT(nbt);
        loaded.clear();
        Arrays.stream(nbt.getLongArray("loaded").orElseGet(() -> new long[0])).mapToObj(BlockPos::of).forEach(loaded::add);
        loadedCatalyst = nbt.getBooleanOr("loaded_catalyst", false);
    }
}
