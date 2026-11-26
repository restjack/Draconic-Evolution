package com.brandon3055.draconicevolution.inventory;

import com.brandon3055.draconicevolution.blocks.tileentity.TileCoreMonitor;
import com.brandon3055.draconicevolution.init.DEContent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

/** eli.8: Core Monitor settings. No slots; the menu exists so BC routes the screen's packets to the tile. */
public class CoreMonitorMenu extends DETileMenu<TileCoreMonitor> {

    public CoreMonitorMenu(int windowId, Inventory playerInv, FriendlyByteBuf extraData) {
        this(windowId, playerInv, getClientTile(playerInv, extraData));
    }

    public CoreMonitorMenu(int windowId, Inventory playerInv, TileCoreMonitor tile) {
        super(DEContent.MENU_CORE_MONITOR.get(), windowId, playerInv, tile);
    }
}
