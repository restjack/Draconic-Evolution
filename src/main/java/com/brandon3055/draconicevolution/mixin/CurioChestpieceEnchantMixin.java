package com.brandon3055.draconicevolution.mixin;

import com.brandon3055.draconicevolution.integration.equipment.EquipmentManager;
import com.brandon3055.draconicevolution.items.equipment.ModularChestpiece;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A Draconic chestpiece worn in the Curios "body" slot counts for enchantments (Protection, Thorns, ...) as if it were
 * in the chest armor slot. Vanilla only walks the real equipment slots. It stacks with a chestplate in the armor slot
 * (Eli's choice); vanilla's protection cap still applies. EquipmentManager.findItem searches curios only, so a
 * chestpiece in the armor slot is never counted twice.
 */
@Mixin(EnchantmentHelper.class)
public abstract class CurioChestpieceEnchantMixin {
    @Inject(method = "runIterationOnEquipment(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/enchantment/EnchantmentHelper$EnchantmentInSlotVisitor;)V",
            at = @At("TAIL"))
    private static void draconicevolution$curioChestpiece(LivingEntity entity, EnchantmentHelper.EnchantmentInSlotVisitor visitor, CallbackInfo ci) {
        if (!(entity instanceof Player)) return; // only players get the body curio slot
        ItemStack chest = EquipmentManager.findItem(s -> s.getItem() instanceof ModularChestpiece, entity);
        if (!chest.isEmpty()) EnchantmentHelper.runIterationOnItem(chest, EquipmentSlot.CHEST, entity, visitor);
    }
}
