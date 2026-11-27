package com.brandon3055.draconicevolution.blocks.machines;

import com.brandon3055.brandonscore.blocks.EntityBlockBCore;
import com.brandon3055.draconicevolution.init.DEContent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Fusion Conductor (restjack fork, Extras switch fusionConductor): floats on top of a Fusion Crafting Core and
 * automates it. Since unofficial-1.35 it can stand anywhere: with no core below it is decorative (crystal idles, no
 * arcs, screen says "Not above a Fusion Crafting Core"). Drawn by {@code RenderTileFusionConductor}.
 */
public class FusionConductor extends EntityBlockBCore {
    private static final VoxelShape SHAPE = Shapes.box(0.25, 0.1, 0.25, 0.75, 0.9, 0.75);

    public FusionConductor(Properties properties) {
        super(properties);
        setBlockEntity(DEContent.TILE_FUSION_CONDUCTOR::get, true);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    public static final net.minecraft.network.chat.Component[] INFO = {
            net.minecraft.network.chat.Component.translatable("fusion_conductor.draconicevolution.info.place").withStyle(net.minecraft.ChatFormatting.GRAY),
            net.minecraft.network.chat.Component.translatable("fusion_conductor.draconicevolution.info.io").withStyle(net.minecraft.ChatFormatting.GRAY),
            net.minecraft.network.chat.Component.translatable("fusion_conductor.draconicevolution.info.capacitor").withStyle(net.minecraft.ChatFormatting.GRAY)
    };

    public static final net.minecraft.network.chat.Component[] JEI_INFO = {
            net.minecraft.network.chat.Component.translatable("fusion_conductor.draconicevolution.jei.1"),
            net.minecraft.network.chat.Component.translatable("fusion_conductor.draconicevolution.jei.2"),
            net.minecraft.network.chat.Component.translatable("fusion_conductor.draconicevolution.jei.3"),
            net.minecraft.network.chat.Component.translatable("fusion_conductor.draconicevolution.jei.4")
    };

    @Override
    public void appendHoverText(net.minecraft.world.item.ItemStack stack, java.util.List<net.minecraft.network.chat.Component> tooltip) {
        java.util.Collections.addAll(tooltip, INFO);
        super.appendHoverText(stack, tooltip);
    }

    /** Placer becomes the owner (owner + team + operators may change settings). */
    @Override
    public void setPlacedBy(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, @Nullable net.minecraft.world.entity.LivingEntity placer, net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && placer instanceof net.minecraft.world.entity.player.Player p
                && level.getBlockEntity(pos) instanceof com.brandon3055.draconicevolution.blocks.tileentity.TileFusionConductor t) t.setOwner(p);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, net.minecraft.core.Direction direction) {
        return level.getBlockEntity(pos) instanceof com.brandon3055.draconicevolution.blocks.tileentity.TileFusionConductor t ? t.getComparatorOutput() : 0;
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable net.minecraft.core.Direction side) {
        return true;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }
}
