package com.brandon3055.draconicevolution.blocks.machines;

import com.brandon3055.brandonscore.blocks.EntityBlockBCore;
import com.brandon3055.draconicevolution.blocks.tileentity.TileCelestialManipulator;
import com.brandon3055.draconicevolution.init.DEContent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * Created by brandon3055 on 25/09/2016.
 */
public class CelestialManipulator extends EntityBlockBCore {

	protected static final VoxelShape SHAPE = Block.box(1.0, 0, 1.0, 15.0, 8.5, 15.0);

    public CelestialManipulator(Properties properties) {
        super(properties);
        canProvidePower = true;
        setBlockEntity(DEContent.TILE_CELESTIAL_MANIPULATOR::get, true);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** Placer becomes the owner (used by the owner-only lock). */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && placer instanceof Player p && level.getBlockEntity(pos) instanceof TileCelestialManipulator t) t.setOwner(p);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, net.minecraft.core.Direction direction) {
        return level.getBlockEntity(pos) instanceof TileCelestialManipulator t ? t.getComparatorOutput() : 0;
    }
}
