package com.brandon3055.draconicevolution.inventory;

import codechicken.lib.inventory.container.modular.ModularSlot;
import com.brandon3055.brandonscore.client.gui.modular.lib.container.SlotGroup;
import com.brandon3055.draconicevolution.blocks.tileentity.TileFusionConductor;
import com.brandon3055.draconicevolution.init.DEContent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

/** Zones: 0 player, 1 input, 2 output, 3 capacitor. Shift-click from the player tries the capacitor slot first. */
public class FusionConductorMenu extends DETileMenu<TileFusionConductor> {

    public final SlotGroup main = createSlotGroup(0, 3, 1);
    public final SlotGroup hotBar = createSlotGroup(0, 3, 1);
    public final SlotGroup input = createSlotGroup(1, 0);
    public final SlotGroup output = createSlotGroup(2, 0);
    public final SlotGroup capacitor = createSlotGroup(3, 0);

    public FusionConductorMenu(int windowId, Inventory playerInv, FriendlyByteBuf extraData) {
        this(windowId, playerInv, getClientTile(playerInv, extraData));
    }

    public FusionConductorMenu(int windowId, Inventory playerInv, TileFusionConductor tile) {
        super(DEContent.MENU_FUSION_CONDUCTOR.get(), windowId, playerInv, tile);
        main.addPlayerMain(playerInv);
        hotBar.addPlayerBar(playerInv);
        for (int i = 0; i < TileFusionConductor.INPUT; i++) input.addSlot(new ModularSlot(tile.itemHandler, i));
        for (int i = TileFusionConductor.OUT_START; i < TileFusionConductor.OUT_END; i++) output.addSlot(new ModularSlot(tile.itemHandler, i).output());
        capacitor.addSlot(new ModularSlot(tile.itemHandler, TileFusionConductor.CAP_SLOT));
    }
}
