package com.brandon3055.draconicevolution.blocks.energynet.tileentity;

import com.brandon3055.brandonscore.api.TechLevel;

import com.brandon3055.brandonscore.capability.CapabilityOP;

import com.brandon3055.brandonscore.lib.Vec3D;

import com.brandon3055.brandonscore.lib.datamanager.DataFlags;

import com.brandon3055.brandonscore.lib.datamanager.ManagedBool;

import com.brandon3055.brandonscore.lib.datamanager.ManagedEnum;

import com.brandon3055.brandonscore.utils.EnergyUtils;

import com.brandon3055.draconicevolution.blocks.energynet.EnergyCrystal;

import com.brandon3055.draconicevolution.client.render.effect.CrystalFXBase;


import com.brandon3055.draconicevolution.init.DEContent;

import java.util.List;

import net.minecraft.ChatFormatting;


import net.minecraft.core.BlockPos;

import net.minecraft.core.Direction;

import net.minecraft.core.HolderLookup;

import net.minecraft.nbt.CompoundTag;

import net.minecraft.network.chat.Component;

import net.minecraft.server.level.ServerPlayer;

import net.minecraft.world.InteractionHand;

import net.minecraft.world.InteractionResult;

import net.minecraft.world.entity.player.Player;

import net.minecraft.world.item.context.BlockPlaceContext;

import net.minecraft.world.level.block.entity.BlockEntity;

import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.world.phys.BlockHitResult;



import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;




/**
 * Created by brandon3055 on 19/11/2016.
 */
public class TileCrystalDirectIO extends TileCrystalBase   {

    public final ManagedEnum<Direction> facing = dataManager.register(new ManagedEnum<>("facing", Direction.DOWN, DataFlags.SAVE_NBT_SYNC_TILE));
    public final ManagedBool outputMode = dataManager.register(new ManagedBool("outputMode", DataFlags.SAVE_BOTH_SYNC_TILE));

    public TileCrystalDirectIO(BlockPos pos, BlockState state) {
        super(DEContent.TILE_IO_CRYSTAL.get(), pos, state);
        outputMode.addValueListener(newVal -> opStorage.setIOMode(!newVal));
    }

    public TileCrystalDirectIO(TechLevel techLevel, BlockPos pos, BlockState state) {
        super(DEContent.TILE_IO_CRYSTAL.get(), techLevel, pos, state);
    }

    public static void register(RegisterCapabilitiesEvent event) {
        com.brandon3055.brandonscore.capability.CapabilityOP.registerBlockEntity(event, DEContent.TILE_IO_CRYSTAL.get());
    }

    //region Update Energy IO

    @Override
    public void tick() {
        super.tick();

        if (level.isClientSide()) {
            return;
        }

        BlockEntity tile = level.getBlockEntity(worldPosition.relative(facing.get()));

        if (outputMode.get() && tile != null) {
            opStorage.extractOP(EnergyUtils.insertEnergy(tile, opStorage.extractOP(opStorage.maxExtract(), true), facing.get().getOpposite(), false), false);
        } else if (tile != null && !(tile instanceof TileCrystalBase)) {
            // Input mode also pulls: many FE blocks (cells, batteries, capacitor banks) only push when configured to.
            EnergyUtils.transferEnergy(tile, facing.get().getOpposite(), opStorage);
        }
    }

    //endregion

    @Override
    public InteractionResult onBlockUse(BlockState state, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!player.isShiftKeyDown()) return InteractionResult.PASS;
        // SUCCESS on both sides: a client PASS made vanilla retry with the off hand, so the server got a second click
        // and switched the mode straight back (the chat line then showed the old mode). Only the main hand switches.
        if (player instanceof ServerPlayer && hand == InteractionHand.MAIN_HAND) {
            outputMode.invert();
            updateRotation(facing.get());
            // No other feedback until the HUD box draws text: say which way it now works.
            com.brandon3055.brandonscore.lib.ChatHelper.sendIndexed(player, Component.translatable("gui.draconicevolution.energy_net.io_mode").append(" ")
                    .append(Component.translatable("gui.draconicevolution.energy_net.io_output_" + outputMode.get())
                            .withStyle(outputMode.get() ? ChatFormatting.GOLD : ChatFormatting.DARK_AQUA)), MSG_ID);
        }
        return InteractionResult.SUCCESS;
    }

    //region Rendering

    @Override
    public EnergyCrystal.CrystalType getCrystalType() {
        return EnergyCrystal.CrystalType.CRYSTAL_IO;
    }

    @Override
    public CrystalFXBase createStaticFX() {
        return com.brandon3055.draconicevolution.client.DEParticleEffects.createCrystalEffect(this, true);
    }

    @Override
    public Vec3D getBeamLinkPos(BlockPos linkTo) {
        return Vec3D.getCenter(worldPosition);
    }

    @Override
    public boolean renderBeamTermination() {
        return false;
    }

    @Override
    public void addDisplayData(List<Component> displayList) {
        super.addDisplayData(displayList);
        ChatFormatting colour = outputMode.get() ? ChatFormatting.GOLD : ChatFormatting.DARK_AQUA;
        displayList.add(Component.translatable("gui.draconicevolution.energy_net.io_mode").append(" ").append(Component.translatable("gui.draconicevolution.energy_net.io_output_" + outputMode.get()).withStyle(colour)));
    }

    //endregion

    @Override
    public void onTilePlaced(BlockPlaceContext context, BlockState state) {
        super.onTilePlaced(context, state);
        updateRotation(context.getClickedFace().getOpposite());
    }

    public void updateRotation(Direction newDirection) {
        facing.set(newDirection);
        capManager.remove(CapabilityOP.OP);
        opStorage.setIOMode(!outputMode.get()); //TODO, not sure why I had this commented out... Was there a reason?
        capManager.setSide(CapabilityOP.OP, opStorage, newDirection);
    }

    @Override
    public void readExtraNBT(CompoundTag compound) {
        super.readExtraNBT(compound);
        updateRotation(facing.get());
    }

    @Override
    public void readFromItemStack(CompoundTag compound) {
        super.readFromItemStack(compound);
        updateRotation(facing.get());
    }
}
