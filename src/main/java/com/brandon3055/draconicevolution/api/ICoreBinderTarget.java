package com.brandon3055.draconicevolution.api;

import net.minecraft.core.GlobalPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * A block entity that takes an energy core from the Crystal Binder (restjack fork, unofficial-1.36; used by the Core
 * Monitor). The player shift-clicks an energy core (or one of its stabilizers or pylons) to save it on the binder, then
 * clicks this block. Lets add-ons hook the binder without the fork knowing their classes.
 */
public interface ICoreBinderTarget {

    /**
     * Called on both sides when a player clicks this block entity with a Crystal Binder (not sneaking at a core).
     *
     * @param core the energy core saved on the binder, or null when the binder holds none
     * @return true when handled; the click is then cancelled
     */
    boolean onCoreBinderUse(Player player, InteractionHand hand, @Nullable GlobalPos core);
}
