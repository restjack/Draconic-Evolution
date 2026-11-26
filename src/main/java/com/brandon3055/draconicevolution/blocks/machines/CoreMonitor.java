package com.brandon3055.draconicevolution.blocks.machines;

import com.brandon3055.brandonscore.blocks.EntityBlockBCore;
import com.brandon3055.draconicevolution.init.DEContent;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;

/**
 * eli.8 (restjack fork, Extras switch coreMonitor): a display panel linked to an energy core. Panels facing the same way
 * next to each other form one screen up to 4x4. Link: Shift-right-click the core (or a stabilizer/pylon) with a Crystal
 * Binder, then right-click the monitor with it.
 */
public class CoreMonitor extends EntityBlockBCore {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public CoreMonitor(Block.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        setBlockEntity(DEContent.TILE_CORE_MONITOR::get, true);
        canProvidePower = true; // Round C redstone modes
    }

    /** No black selection box over the screen itself; the sides and back still show it. */
    @Override
    public boolean renderSelectionBox(net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent event, net.minecraft.world.level.Level level) {
        BlockState state = level.getBlockState(event.getHitResult().getBlockPos());
        return !state.hasProperty(FACING) || event.getHitResult().getDirection() != state.getValue(FACING);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
}
