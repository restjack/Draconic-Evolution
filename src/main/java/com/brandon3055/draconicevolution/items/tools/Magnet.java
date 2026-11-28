package com.brandon3055.draconicevolution.items.tools;

import com.brandon3055.draconicevolution.DEConfig;

import com.brandon3055.draconicevolution.init.DEContent;

import com.brandon3055.draconicevolution.init.ItemData;

import com.brandon3055.draconicevolution.lib.TagUtils26;

import java.util.List;

import net.minecraft.core.BlockPos;

import net.minecraft.nbt.CompoundTag;

import net.minecraft.sounds.SoundEvents;

import net.minecraft.sounds.SoundSource;

import net.minecraft.world.InteractionHand;

import net.minecraft.world.InteractionResult;

import net.minecraft.world.entity.Entity;

import net.minecraft.world.entity.ExperienceOrb;

import net.minecraft.world.entity.item.ItemEntity;

import net.minecraft.world.entity.player.Player;

import net.minecraft.world.item.Item;

import net.minecraft.world.item.ItemStack;

import net.minecraft.world.item.TooltipFlag;

import net.minecraft.world.level.Level;

import net.minecraft.world.phys.AABB;



import net.neoforged.neoforge.common.NeoForge;

import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;






















































/**
 * Created by brandon3055 on 9/3/2016.
 */
//@Optional.Interface(iface = "baubles.api.IBauble", modid = "baubles")
public class Magnet extends Item /*implements IBauble*/ {

    private final int range;

    public Magnet(Properties properties, int range) {
        super(properties);
        this.range = range;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<net.minecraft.network.chat.Component> tooltip, TooltipFlag flag) {
        tooltip.accept(net.minecraft.network.chat.Component.translatable("item.draconicevolution.magnet.range", range).withStyle(net.minecraft.ChatFormatting.GRAY));
        boolean on = isEnabled(stack);
        tooltip.accept(net.minecraft.network.chat.Component.translatable("item.draconicevolution.magnet.status",
                net.minecraft.network.chat.Component.translatable(on ? "item.draconicevolution.magnet.on" : "item.draconicevolution.magnet.off")
                        .withStyle(on ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.RED)).withStyle(net.minecraft.ChatFormatting.GRAY));
        tooltip.accept(net.minecraft.network.chat.Component.translatable("item.draconicevolution.magnet.toggle").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        tooltip.accept(net.minecraft.network.chat.Component.translatable("item.draconicevolution.magnet.sneak").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return isEnabled(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, net.minecraft.server.level.ServerLevel level, Entity entity, net.minecraft.world.entity.EquipmentSlot slot) {
        updateMagnet(stack, entity);
    }

    private void updateMagnet(ItemStack stack, Entity entity) {
        if (!entity.isShiftKeyDown() && isEnabled(stack) && entity instanceof Player player) {
            Level world = entity.level();
            List<ItemEntity> items;
            if (entity.tickCount % 10 == 0) {
                items = world.getEntitiesOfClass(ItemEntity.class, new AABB(entity.getX(), entity.getY(), entity.getZ(), entity.getX(), entity.getY(), entity.getZ()).inflate(range, range, range));
            } else {
                items = world.getEntitiesOfClass(ItemEntity.class, new AABB(entity.getX(), entity.getY(), entity.getZ(), entity.getX(), entity.getY(), entity.getZ()).inflate(5, 5, 5));
            }

            boolean flag = false;
            for (ItemEntity itemEntity : items) {
//                ItemStack item = itemEntity.getItem();

                //For now i think the dislocation inhibitor is a better solution that makes more sense to the user.
//                String name = item.getItem().getRegistryName().toString();
                if (!itemEntity.isAlive() /*|| (DEOldConfig.itemDislocatorBlacklistMap.containsKey(name) && (DEOldConfig.itemDislocatorBlacklistMap.get(name) == -1*//* || DEConfig.itemDislocatorBlacklistMap.get(name) == item.getItemDamage()*/) {
                    continue;
                }

                CompoundTag itemTag = itemEntity.getPersistentData();
                if (itemTag != null && itemTag.contains("PreventRemoteMovement")) {
                    continue;
                }

                if (itemEntity.getOwner() != null && itemEntity.getOwner().equals(entity.getUUID()) && itemEntity.pickupDelay > 0) {
                    continue;
                }

                Player closest = world.getNearestPlayer(itemEntity, 4);
                if (closest != null && closest != entity) {
                    continue;
                }

                // unofficial-1.30: ask the loaded projectors instead of reading 1,331 blocks around every item every tick.
                if (com.brandon3055.draconicevolution.blocks.tileentity.TileDislocationInhibitor.isInhibited(world, itemEntity)) {
                    continue;
                }

                if (entity.distanceToSqr(itemEntity) > 2 * 2) {
                    flag = true;
                }

                if (!world.isClientSide()) {
                    if (itemEntity.pickupDelay > 0) {
                        itemEntity.pickupDelay = 0;
                    }
                    itemEntity.setDeltaMovement(0, 0, 0);
                    itemEntity.fallDistance = 0;
                    itemEntity.setPos(entity.getX() - 0.2 + (world.getRandom().nextDouble() * 0.4), entity.getY() - 0.6, entity.getZ() - 0.2 + (world.getRandom().nextDouble() * 0.4));
                }
            }

            //TODO When loot piles are a thing
//            List<EntityLootCore> cores = world.getEntitiesWithinAABB(EntityLootCore.class, new AxisAlignedBB(entity.getPosX(), entity.getPosY(), entity.getPosZ(), entity.getPosX(), entity.getPosY(), entity.getPosZ()).grow(range, range, range));
//            for (EntityLootCore core : cores) {
//                PlayerEntity closest = world.getClosestPlayer(core, 4);
//                if (closest != null && closest != entity) {
//                    continue;
//                }
//
//                flag = true;
//
//                if (!world.isRemote) {
//                    core.setPosition(entity.getPosX() - 0.2 + (world.rand.nextDouble() * 0.4), entity.getPosY() - 0.6, entity.getPosZ() - 0.2 + (world.rand.nextDouble() * 0.4));
//                }
//            }

            if (flag && DEConfig.itemDislocatorSound) {
                world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.1F, 1F + (world.getRandom().nextFloat() * 0.1F));
            }

            List<ExperienceOrb> xp = world.getEntitiesOfClass(ExperienceOrb.class, new AABB(entity.getX(), entity.getY(), entity.getZ(), entity.getX(), entity.getY(), entity.getZ()).inflate(4, 4, 4));

            for (ExperienceOrb orb : xp) {
                if (!world.isClientSide() && orb.isAlive()) {
                    // unofficial-1.40: projectors with "Stop XP" on keep orbs in their box (and the tag is respected like on items).
                    if (orb.getPersistentData().contains("PreventRemoteMovement")
                            || com.brandon3055.draconicevolution.blocks.tileentity.TileDislocationInhibitor.isInhibited(world, orb)) {
                        continue;
                    }
                    PlayerXpEvent.PickupXp event = NeoForge.EVENT_BUS.post(new PlayerXpEvent.PickupXp(player, orb));
                    if (event.isCanceled()) {
                        continue;
                    }
                    if (DEConfig.itemDislocatorSound) {
                        world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.1F, 0.5F * ((world.getRandom().nextFloat() - world.getRandom().nextFloat()) * 0.7F + 1.8F));
                    }
                    player.take(orb, 1);
                    player.giveExperiencePoints(orb.getValue());
                    orb.discard();
                }
            }
        }
    }

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) { // port-26.1.2: InteractionResultHolder удалён
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            toggleEnabled(stack, player);
        }
        return super.use(world, player, hand);
    }


    public static boolean isEnabled(ItemStack stack) {
        return stack.getOrDefault(ItemData.MAGNET_ACTIVE, false);
    }

    public static void toggleEnabled(ItemStack stack, Player player) {
        stack.set(ItemData.MAGNET_ACTIVE, !isEnabled(stack));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.1F, isEnabled(stack) ? 1F : 0.5F);
    }

//    @Optional.Method(modid = "baubles")
//    @Override
//    public BaubleType getBaubleType(ItemStack itemstack) {
//        return BaubleType.TRINKET;
//    }
//
//    @Override
//    @Optional.Method(modid = "baubles")
//    public void onWornTick(ItemStack itemstack, LivingEntity player) {
//        if (!(player instanceof PlayerEntity)) return;
//        updateMagnet(itemstack, player);
//    }
}
