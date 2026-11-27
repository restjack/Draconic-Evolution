package com.brandon3055.draconicevolution.items.tools;

import com.brandon3055.draconicevolution.api.energy.ICrystalBinder;

import net.minecraft.core.BlockPos;

import net.minecraft.world.InteractionResult;

import net.minecraft.world.item.Item;

import net.minecraft.world.item.ItemStack;

import net.minecraft.world.item.context.UseOnContext;

import net.minecraft.world.level.Level;

import net.minecraft.world.level.block.Rotation;

import net.minecraft.world.level.block.state.BlockState;



/**
 * Created by brandon3055 on 25/11/2016.
 */
public class CrystalBinder extends Item implements ICrystalBinder {
    public CrystalBinder(Properties properties) {
        super(properties.enchantable(1));
    }



    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        net.minecraft.core.GlobalPos bound = stack.get(com.brandon3055.draconicevolution.init.ItemData.BINDER_POS);
        if (bound != null) {
            BlockPos p = bound.pos();
            tooltip.accept(net.minecraft.network.chat.Component.translatable("item.draconicevolution.crystal_binder.bound",
                    p.getX() + " " + p.getY() + " " + p.getZ(), bound.dimension().identifier().toString()).withStyle(net.minecraft.ChatFormatting.GREEN));
        }
        for (String k : new String[]{"link", "monitor", "rotate"}) {
            tooltip.accept(net.minecraft.network.chat.Component.translatable("item.draconicevolution.crystal_binder." + k).withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        BlockState state = context.getLevel().getBlockState(context.getClickedPos());
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();

        BlockState rotated = state.rotate(world, pos, Rotation.CLOCKWISE_90);
        if (!rotated.equals(state)) {
            world.setBlockAndUpdate(pos, rotated);
            return world.isClientSide() ? InteractionResult.PASS : InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }
}
