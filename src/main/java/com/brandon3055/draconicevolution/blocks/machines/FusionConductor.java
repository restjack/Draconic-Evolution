package com.brandon3055.draconicevolution.blocks.machines;

import com.brandon3055.brandonscore.blocks.EntityBlockBCore;
import com.brandon3055.draconicevolution.blocks.tileentity.TileFusionCraftingCore;
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
 * automates it. Can only be placed directly on a core. Drawn by {@code RenderTileFusionConductor}.
 */
public class FusionConductor extends EntityBlockBCore {
    private static final VoxelShape SHAPE = Shapes.box(0.25, 0.1, 0.25, 0.75, 0.9, 0.75);

    public FusionConductor(Properties properties) {
        super(properties);
        setBlockEntity(DEContent.TILE_FUSION_CONDUCTOR::get, true);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos below = context.getClickedPos().below();
        return context.getLevel().getBlockEntity(below) instanceof TileFusionCraftingCore ? defaultBlockState() : null;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }
}
