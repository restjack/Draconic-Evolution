package com.brandon3055.draconicevolution;

import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import com.brandon3055.brandonscore.utils.Utils;
import com.brandon3055.draconicevolution.api.DraconicAPI;
import com.brandon3055.draconicevolution.client.ClientProxy;
import com.brandon3055.draconicevolution.client.DEParticles;
import com.brandon3055.draconicevolution.command.DECommands;
import com.brandon3055.draconicevolution.handlers.*;
import com.brandon3055.draconicevolution.init.*;
import com.brandon3055.draconicevolution.integration.computers.ComputerCraftCompatEventHandler;
import com.brandon3055.draconicevolution.integration.equipment.EquipmentManager;
import com.brandon3055.draconicevolution.network.DraconicNetwork;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


@Mod (DraconicEvolution.MODID)
public class DraconicEvolution {
    public static final Logger LOGGER = LogManager.getLogger();
    public static final String MODID = "draconicevolution";
    public static final String MODNAME = "Draconic Evolution";

    public static CommonProxy proxy;

    public DraconicEvolution(IEventBus modBus) {
        // port-26.1.2: BC Utils.unsafeRunForDist удалён -> FMLEnvironment.getDist()
        proxy = FMLEnvironment.getDist() == Dist.CLIENT ? new ClientProxy() : new CommonProxy();

        DEConfig.load();
        DETags.init();
        ItemData.init(modBus);
        DEContent.init(modBus);
        DEModules.init(modBus);
        DESounds.init(modBus);
        DEParticles.init(modBus);
        DECreativeTabs.init(modBus);
        EquipmentManager.initialize(modBus);
        DECommands.init();
        CapabilityData.init(modBus);
        LootEventHandler.init();
        ModuleEventHandler.init(modBus);
        ModularArmorEventHandler.init();
        DraconicNetwork.init(modBus);
        DEEventHandler.init(modBus);
        com.brandon3055.draconicevolution.lib.ConfigCondition.init(modBus); // eli.8: recipes that follow an Extras switch
        // eli.7: since MC 1.21.2 clients only get recipe types the server sends; JEI needs the fusion recipes.
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.OnDatapackSyncEvent e) ->
                e.sendRecipes(DraconicAPI.FUSION_RECIPE_TYPE.get()));
        // unofficial-1.30: drop stale "don't pull" tags our Dislocation Inhibitor left on items saved to disk.
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(com.brandon3055.draconicevolution.blocks.tileentity.TileDislocationInhibitor::onEntityJoin);

        // port-26.1.2: BC Utils.loadOptionalMod удалён -> ModList.get().isLoaded
        if (ModList.get().isLoaded("computercraft")) {
            modBus.register(new ComputerCraftCompatEventHandler());
        }

        // port-26.1.2: BC Utils.unsafeRunWhenOn удалён
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            DEClient.init(modBus);
        }
        DraconicAPI.addModuleProvider(MODID);
    }
}