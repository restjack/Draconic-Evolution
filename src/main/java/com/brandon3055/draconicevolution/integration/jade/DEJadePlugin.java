package com.brandon3055.draconicevolution.integration.jade;

import com.brandon3055.brandonscore.api.power.IOInfo;
import com.brandon3055.brandonscore.utils.Utils;
import com.brandon3055.draconicevolution.DraconicEvolution;
import com.brandon3055.draconicevolution.blocks.StructureBlock;
import com.brandon3055.draconicevolution.blocks.machines.EnergyCore;
import com.brandon3055.draconicevolution.blocks.machines.EnergyCoreStabilizer;
import com.brandon3055.draconicevolution.blocks.machines.EnergyPylon;
import com.brandon3055.draconicevolution.blocks.tileentity.TileEnergyCore;
import com.brandon3055.draconicevolution.blocks.tileentity.TileEnergyCoreStabilizer;
import com.brandon3055.draconicevolution.blocks.tileentity.TileEnergyPylon;
import com.brandon3055.draconicevolution.blocks.tileentity.TileStructureBlock;
import com.brandon3055.draconicevolution.client.gui.EnergyCoreGui;
import com.brandon3055.draconicevolution.lib.LoadedLookup;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * Jade tooltips for the energy core multiblock (eli.7, not upstream).
 * Core: tier, state, stored / capacity, net rate; with details (Shift): in / out and time to full or empty.
 * Pylon: mode and core link. Stabilizer: core link. Looking at a pylon sphere or core shell shows its controller.
 * Jade only loads this class when Jade is installed, so the dependency stays optional.
 */
@WailaPlugin(DraconicEvolution.MODID)
public class DEJadePlugin implements IWailaPlugin {
    static final Identifier UID = Identifier.fromNamespaceAndPath(DraconicEvolution.MODID, "energy_core");
    private static final String KEY = "de_core";

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(CoreData.INSTANCE, TileEnergyCore.class);
        registration.registerBlockDataProvider(CoreData.INSTANCE, TileEnergyCoreStabilizer.class);
        registration.registerBlockDataProvider(CoreData.INSTANCE, TileEnergyPylon.class);
        registration.registerBlockDataProvider(CoreData.INSTANCE, TileStructureBlock.class);
        registration.registerBlockDataProvider(FusionData.INSTANCE, com.brandon3055.draconicevolution.blocks.tileentity.TileFusionCraftingCore.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(Tooltip.INSTANCE, EnergyCore.class);
        registration.registerBlockComponent(Tooltip.INSTANCE, EnergyCoreStabilizer.class);
        registration.registerBlockComponent(Tooltip.INSTANCE, EnergyPylon.class);
        registration.registerBlockComponent(Tooltip.INSTANCE, StructureBlock.class);
        registration.registerBlockComponent(Tooltip.INSTANCE, com.brandon3055.draconicevolution.blocks.machines.CraftingInjector.class);
        registration.registerBlockComponent(Tooltip.INSTANCE, com.brandon3055.draconicevolution.blocks.machines.FusionCraftingCore.class);
    }

    /** The block the player looks at, resolved through structure blocks (pylon spheres, core shells, big stabilizers) to its controller. */
    @Nullable
    static BlockEntity resolve(@Nullable BlockEntity tile) {
        if (tile instanceof TileStructureBlock structure && structure.controllerOffset.notNull()) {
            // Not getController(): that reverts the structure block when the controller is missing.
            return lookup(tile.getLevel(), tile.getBlockPos().subtract(structure.controllerOffset.get()));
        }
        return tile;
    }

    /** The energy core a core, stabilizer or pylon belongs to. Read-only: no chunk tickets, no unlinking. */
    @Nullable
    static TileEnergyCore coreOf(@Nullable BlockEntity tile) {
        if (tile instanceof TileEnergyCore core) return core;
        BlockPos offset = tile instanceof TileEnergyPylon pylon ? pylon.coreOffset.get()
                : tile instanceof TileEnergyCoreStabilizer stab ? stab.coreOffset.get() : null;
        if (offset == null) return null;
        return lookup(tile.getLevel(), tile.getBlockPos().subtract(offset)) instanceof TileEnergyCore core ? core : null;
    }

    @Nullable
    private static BlockEntity lookup(@Nullable Level level, BlockPos pos) {
        if (level == null || !LoadedLookup.isLoaded(level, pos)) return null;
        return LoadedLookup.blockEntity(level, pos);
    }

    static final String FUSION_KEY = "de_fusion";

    enum FusionData implements IServerDataProvider<BlockAccessor> {
        INSTANCE;

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof com.brandon3055.draconicevolution.blocks.tileentity.TileFusionCraftingCore core)) return;
            CompoundTag tag = new CompoundTag();
            tag.putBoolean("crafting", core.isCrafting());
            tag.putInt("state", core.getFusionState().ordinal());
            tag.putFloat("progress", Math.max(0, core.progress.get()));
            tag.putLong("stored", core.injectorFeed.getOPStored());
            tag.putLong("needed", core.injectorFeed.getMaxOPStored());
            tag.putBoolean("output", !core.getOutputStack().isEmpty());
            data.put(FUSION_KEY, tag);
        }

        @Override
        public Identifier getUid() {
            return FUSION_UID;
        }
    }

    static final Identifier FUSION_UID = Identifier.fromNamespaceAndPath(DraconicEvolution.MODID, "fusion_core");

    enum CoreData implements IServerDataProvider<BlockAccessor> {
        INSTANCE;

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            TileEnergyCore core = coreOf(resolve(accessor.getBlockEntity()));
            if (core == null) return;
            CompoundTag tag = new CompoundTag();
            tag.putInt("tier", core.tier.get());
            tag.putBoolean("active", core.active.get());
            tag.putBoolean("unlimited", core.energy.isUnlimited());
            tag.putLong("stored", core.energy.getOPStored());
            tag.putLong("max", core.energy.getMaxOPStored());
            IOInfo io = core.energy.getIOInfo();
            tag.putLong("in", io == null ? 0 : io.currentInput());
            tag.putLong("out", io == null ? 0 : io.currentOutput());
            data.put(KEY, tag);
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }

    enum Tooltip implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            BlockEntity tile = resolve(accessor.getBlockEntity());
            if (tile instanceof com.brandon3055.draconicevolution.blocks.tileentity.TileFusionCraftingCore) {
                CompoundTag f = accessor.getServerData().getCompoundOrEmpty(FUSION_KEY);
                if (f.isEmpty()) return;
                String unit = " " + com.brandon3055.draconicevolution.DEConfig.energyUnit();
                if (f.getBooleanOr("crafting", false)) {
                    boolean charging = f.getIntOr("state", 0) < 2;
                    tooltip.add(Component.literal((charging ? "Charging " : "Crafting ") + Math.round(f.getFloatOr("progress", 0) * 1000) / 10D + "%").withStyle(ChatFormatting.GREEN));
                    if (charging) tooltip.add(Component.literal(Utils.formatNumber(f.getLongOr("stored", 0)) + " / " + Utils.formatNumber(f.getLongOr("needed", 0)) + unit).withStyle(ChatFormatting.GOLD));
                } else {
                    tooltip.add(Component.literal(f.getBooleanOr("output", false) ? "Output ready" : "Idle").withStyle(ChatFormatting.GRAY));
                }
                if (((com.brandon3055.draconicevolution.blocks.tileentity.TileFusionCraftingCore) tile).hasConductor()) tooltip.add(Component.literal("Run by the Fusion Conductor above").withStyle(ChatFormatting.LIGHT_PURPLE));
                return;
            }
            if (tile instanceof com.brandon3055.draconicevolution.blocks.tileentity.TileFusionCraftingInjector inj) {
                tooltip.add(com.brandon3055.draconicevolution.blocks.machines.CraftingInjector.modeText(inj.singleItem.get()));
                var held = inj.getInjectorStack();
                if (!held.isEmpty()) tooltip.add(Component.literal(held.getCount() + "x ").append(held.getHoverName()).withStyle(ChatFormatting.WHITE));
                long need = inj.energyRequired.get();
                if (need > 0) tooltip.add(Component.literal("Charge: " + Math.min(100, Math.round(inj.energy.get() * 1000D / need) / 10D) + "%").withStyle(ChatFormatting.GOLD));
                if (accessor.showDetails()) tooltip.add(com.brandon3055.draconicevolution.blocks.machines.CraftingInjector.MODE_INFO[2]);
                return;
            }
            if (tile instanceof TileEnergyPylon pylon) {
                tooltip.add(TileEnergyPylon.modeText(pylon.ioMode.get()));
                if (pylon.coreOffset.isNull()) tooltip.add(Component.literal("Not linked to a core").withStyle(ChatFormatting.RED));
            } else if (tile instanceof TileEnergyCoreStabilizer stab) {
                tooltip.add(stab.coreOffset.isNull()
                        ? Component.literal("Not linked to a core").withStyle(ChatFormatting.RED)
                        : Component.literal("Linked to core").withStyle(ChatFormatting.GREEN));
            } else if (!(tile instanceof TileEnergyCore)) {
                return; // a structure block of some other multiblock
            }

            CompoundTag data = accessor.getServerData();
            if (!data.contains(KEY)) return;
            CompoundTag c = data.getCompoundOrEmpty(KEY);
            long stored = c.getLongOr("stored", 0), max = c.getLongOr("max", 0);
            long in = c.getLongOr("in", 0), out = c.getLongOr("out", 0), net = in - out;
            boolean unlimited = c.getBooleanOr("unlimited", false);

            tooltip.add(Component.literal("Tier " + c.getIntOr("tier", 0) + " core, ")
                    .append(c.getBooleanOr("active", false)
                            ? Component.literal("active").withStyle(ChatFormatting.GREEN)
                            : Component.literal("inactive").withStyle(ChatFormatting.RED))
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal(unlimited
                    ? "Stored: " + Utils.formatNumber(stored) + " " + com.brandon3055.draconicevolution.DEConfig.energyUnit()
                    : "Stored: " + Utils.formatNumber(stored) + " / " + Utils.formatNumber(max) + " " + com.brandon3055.draconicevolution.DEConfig.energyUnit() + " ("
                      + (max > 0 ? Math.round(stored * 1000D / max) / 10D : 0) + "%)").withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.literal("Net: " + (net > 0 ? "+" : "") + Utils.formatNumber(net) + " " + com.brandon3055.draconicevolution.DEConfig.energyUnit() + "/t")
                    .withStyle(net > 0 ? ChatFormatting.GREEN : net < 0 ? ChatFormatting.RED : ChatFormatting.GRAY));
            if (accessor.showDetails()) {
                tooltip.add(Component.literal("In ").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal("+" + Utils.formatNumber(in)).withStyle(ChatFormatting.GREEN))
                        .append(" / Out ")
                        .append(Component.literal("-" + Utils.formatNumber(out)).withStyle(ChatFormatting.RED))
                        .append(" " + com.brandon3055.draconicevolution.DEConfig.energyUnit() + "/t"));
                if (net > 0 && !unlimited && stored < max) {
                    tooltip.add(Component.literal("Full in " + EnergyCoreGui.formatDuration((max - stored) / (double) net / 20D)).withStyle(ChatFormatting.GRAY));
                } else if (net < 0 && stored > 0) {
                    tooltip.add(Component.literal("Empty in " + EnergyCoreGui.formatDuration(stored / (double) -net / 20D)).withStyle(ChatFormatting.GRAY));
                }
            }
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }
}
