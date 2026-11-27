package com.brandon3055.draconicevolution.inventory;

import com.brandon3055.draconicevolution.blocks.tileentity.TileDislocationInhibitor;
import com.brandon3055.draconicevolution.init.DEContent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

/** No slots: the menu only syncs the projector's data to the screen. */
public class DislocationInhibitorMenu extends DETileMenu<TileDislocationInhibitor> {
    public DislocationInhibitorMenu(int windowId, Inventory playerInv, FriendlyByteBuf extraData) {
        this(windowId, playerInv, getClientTile(playerInv, extraData));
    }

    public DislocationInhibitorMenu(int windowId, Inventory playerInv, TileDislocationInhibitor tile) {
        super(DEContent.MENU_DISLOCATION_INHIBITOR.get(), windowId, playerInv, tile);
    }
}
