package com.brandon3055.draconicevolution.blocks;

import com.brandon3055.brandonscore.blocks.EntityBlockBCore;
import com.brandon3055.draconicevolution.init.DEContent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Created by brandon3055 on 12/09/2017.
 * <p>
 * restjack fork: now has a tile ({@code TileDislocationInhibitor}) with adjustable range, "all magnets" mode,
 * area outline and redstone control. Blocks placed before this build get their tile the first time they are used.
 */
public class DislocationInhibitor extends EntityBlockBCore {

    public DislocationInhibitor(Properties properties) {
        super(properties);
        setBlockEntity(DEContent.TILE_DISLOCATION_INHIBITOR::get, true);
    }

    @Override
    public void appendHoverText(ItemStack stack, List<Component> tooltip) {
        tooltip.add(Component.translatable("tile.draconicevolution.dislocation_inhibitor.info"));
        tooltip.add(Component.translatable("tile.draconicevolution.dislocation_inhibitor.info2").withStyle(net.minecraft.ChatFormatting.GRAY));
        super.appendHoverText(stack, tooltip);
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable net.minecraft.core.Direction side) {
        return true;
    }
}
