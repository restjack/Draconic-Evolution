package com.brandon3055.draconicevolution.integration.patchouli;

import com.brandon3055.draconicevolution.DraconicEvolution;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import vazkii.patchouli.api.PatchouliAPI;

/** eli.7: the in-game guide (Patchouli book draconicevolution:guide). Only touches Patchouli classes when it is loaded. */
public class PatchouliCompat {
    public static final Identifier GUIDE = Identifier.fromNamespaceAndPath(DraconicEvolution.MODID, "guide");

    public static boolean isLoaded() {
        return ModList.get().isLoaded("patchouli");
    }

    /** Call only when {@link #isLoaded()}. */
    public static void openGuide(ServerPlayer player) {
        PatchouliAPI.get().openBookGUI(player, GUIDE);
    }
}
