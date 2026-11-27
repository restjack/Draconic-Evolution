package com.brandon3055.draconicevolution.items.equipment;

import net.minecraft.world.item.equipment.ArmorType;
import com.brandon3055.brandonscore.api.TechLevel;
import com.brandon3055.brandonscore.client.model.DummyHumanoidModel;
import com.brandon3055.brandonscore.client.render.EquippedItemModel;
import com.brandon3055.brandonscore.items.EquippedModelItem;
import com.brandon3055.draconicevolution.DEConfig;
import com.brandon3055.draconicevolution.api.capability.ModuleHost;
import com.brandon3055.draconicevolution.api.config.ConfigProperty;
import com.brandon3055.draconicevolution.api.config.DecimalProperty;
import com.brandon3055.draconicevolution.api.modules.ModuleCategory;
import com.brandon3055.draconicevolution.api.modules.ModuleTypes;
import com.brandon3055.draconicevolution.api.modules.data.JumpData;
import com.brandon3055.draconicevolution.api.modules.data.SpeedData;
import com.brandon3055.draconicevolution.api.modules.lib.ModularOPStorage;
import com.brandon3055.draconicevolution.api.modules.lib.ModuleHostImpl;
import com.brandon3055.draconicevolution.client.model.ModularChestpieceModel;
import com.brandon3055.draconicevolution.init.EquipCfg;
import com.brandon3055.draconicevolution.init.ModuleCfg;
import com.brandon3055.draconicevolution.init.TechProperties;
import com.brandon3055.draconicevolution.integration.equipment.EquipmentManager;
import com.brandon3055.draconicevolution.integration.equipment.IDEEquipment;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by brandon3055 on 21/5/20.
 */
public class ModularChestpiece extends Item implements IModularArmor, IDEEquipment, EquippedModelItem, IModularEnergyItem {
    private final TechLevel techLevel;

    public ModularChestpiece(TechProperties props) {
        super(DEArmorMaterials.apply(DEArmorMaterials.MODULAR_CHESTPIECE, props, net.minecraft.world.item.equipment.ArmorType.CHESTPLATE)); // port-26.1.2: ArmorItem удалён -> Item.Properties.humanoidArmor (эталон Items26)
        this.techLevel = props.getTechLevel();
    }

    @Override
    public boolean canEquip(ItemStack stack, EquipmentSlot armorType, LivingEntity entity) {
        if (entity instanceof LivingEntity && !EquipmentManager.findItem(e -> e.getItem() instanceof ModularChestpiece, (LivingEntity) entity).isEmpty()) {
            return false;
        }
        return entity.getEquipmentSlotForItem(stack) == armorType;
    }

    @Override
    public boolean canEquip(ItemStack stack, LivingEntity livingEntity, String slotID) {
        if (!slotID.equals("body") || !EquipmentManager.findItem(e -> e.getItem() instanceof ModularChestpiece, livingEntity).isEmpty()) {
            return false;
        }

        return !(livingEntity.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof ModularChestpiece);
    }

    @Override
    public boolean isCombineRepairable(ItemStack stack) {
        return false; // port-26.1.2: neoforge-26 abstract в IItemExtension; DE-ремонт через энергию
    }

    @Override
    public TechLevel getTechLevel() {
        return techLevel;
    }

    @Override
    public @NotNull ModuleHostImpl instantiateHost(ItemStack stack) {
        ModuleHostImpl host = new ModuleHostImpl(techLevel, ModuleCfg.chestpieceWidth(techLevel), ModuleCfg.chestpieceHeight(techLevel), "chestpiece", ModuleCfg.removeInvalidModules);
        host.addCategories(ModuleCategory.CHESTPIECE);
        host.addPropertyBuilder(props -> {
            SpeedData speed = host.getModuleData(ModuleTypes.SPEED);
            if (speed != null) {
                SpeedData data = host.getModuleData(ModuleTypes.SPEED);
                double maxSpeed = data == null ? 0 : data.speedMultiplier();
                if (DEConfig.armorSpeedLimit != -1) {
                    maxSpeed = Math.min(maxSpeed, DEConfig.armorSpeedLimit);
                }

                //TODO Do these still work properly now that we are not using dynamic getters? Props are regenerated on data load so it should be fine?
                props.add(new DecimalProperty("walk_speed", 0).min(0).max(maxSpeed).setFormatter(ConfigProperty.DecimalFormatter.PLUS_PERCENT_0));
                props.add(new DecimalProperty("run_speed", maxSpeed).min(0).max(maxSpeed).setFormatter(ConfigProperty.DecimalFormatter.PLUS_PERCENT_0));
            }

            JumpData jump = host.getModuleData(ModuleTypes.JUMP_BOOST);
            if (jump != null) {
                JumpData data = host.getModuleData(ModuleTypes.JUMP_BOOST);
                double jumpSetting =  data == null ? 0 : data.multiplier();

                //TODO Do these still work properly now that we are not using dynamic getters? Props are regenerated on data load so it should be fine?
                props.add(new DecimalProperty("jump_boost_run", jumpSetting).min(0).max(jumpSetting).setFormatter(ConfigProperty.DecimalFormatter.PLUS_PERCENT_0));
                props.add(new DecimalProperty("jump_boost", jumpSetting).min(0).max(jumpSetting).setFormatter(ConfigProperty.DecimalFormatter.PLUS_PERCENT_0));
            }
        });
        return host;
    }

    @Override
    public @NotNull ModularOPStorage instantiateOPStorage(ItemStack stack, Supplier<ModuleHost> hostSupplier) {
        return new ModularOPStorage(hostSupplier, EquipCfg.getBaseChestpieceEnergy(techLevel), EquipCfg.getBaseChestpieceTransfer(techLevel));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<Component> builder, TooltipFlag flagIn) { // port-26.1.2: сигнатура 26 (TooltipDisplay+Consumer)
        List<Component> tooltip = new java.util.ArrayList<>();
        addModularItemInformation(stack, context, tooltip, flagIn);
        tooltip.forEach(builder);
    }

    private ModularChestpieceModel<?> model;

    private ModularChestpieceModel<?> model_on_armor;

    /** Worn as a curio (slot == null): respect that Curios slot's render toggle. */
    @Override
    public boolean shouldRenderInSlot(LivingEntity entity, ItemStack stack, @Nullable EquipmentSlot slot) {
        return slot != null || EquipmentManager.isCurioVisible(e -> e.getItem() instanceof ModularChestpiece, entity);
    }

    @Override
    public EquippedItemModel getExtendedModel(LivingEntity entity, ItemStack stack, @Nullable EquipmentSlot slot, HumanoidModel<?> parentModel, boolean slim) {
        return ClientRendering.getModel(this, entity, slot, parentModel);
    }

    @Override
    public boolean canBeHurtBy(ItemStack stack, DamageSource source) {
        return source.is(DamageTypes.FELL_OUT_OF_WORLD);
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (entity.getAge() >= 0 && entity.pickupDelay != 32767) {
            entity.setExtendedLifetime();
        }
        return super.onEntityItemUpdate(stack, entity);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return damageBarVisible(stack);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return damageBarWidth(stack);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return damageBarColour(stack);
    }
    private static final class ClientRendering {
        static EquippedItemModel getModel(ModularChestpiece chestpiece, LivingEntity entity, @Nullable EquipmentSlot slot, HumanoidModel<?> parentModel) {
            ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
            boolean onArmor = slot == null && !chest.isEmpty() && chest.has(net.minecraft.core.component.DataComponents.EQUIPPABLE); // port-26.1.2: ArmorItem удалён -> EQUIPPABLE-компонент
            if (chestpiece.model == null || chestpiece.model_on_armor == null) {
                chestpiece.model = new ModularChestpieceModel<>(chestpiece.techLevel, false);
                chestpiece.model_on_armor = new ModularChestpieceModel<>(chestpiece.techLevel, true);
            }
            ModularChestpieceModel<?> activeModel = onArmor ? chestpiece.model_on_armor : chestpiece.model;
            ClientHooks.copyModelProperties(parentModel, activeModel);
            return activeModel;
        }
    }
}
