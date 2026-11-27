package com.brandon3055.draconicevolution.client.render.item;

import codechicken.lib.colour.Colour;
import codechicken.lib.render.CCModel;
import codechicken.lib.render.model.OBJParser;
import com.brandon3055.brandonscore.api.TimeKeeper;
import com.brandon3055.draconicevolution.DraconicEvolution;
import com.brandon3055.draconicevolution.client.render.tile.RenderTileFusionConductor;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Fusion Conductor item: the same ice crystal and steel ring the placed block draws. */
public class RenderItemFusionConductor implements DEItemRenderer {
    private final CCModel crystal = CCModel.combine(new OBJParser(Identifier.fromNamespaceAndPath(DraconicEvolution.MODID, "models/block/crystal.obj")).quads().ignoreMtl().parse().values()).backfacedCopy();

    @Override
    public void renderItem(ItemStack stack, ItemDisplayContext context, PoseStack mStack, MultiBufferSource getter, int packedLight, int packedOverlay) {
        float t = TimeKeeper.getClientTick();
        float[] c = RenderTileFusionConductor.ICE;
        RenderTileFusionConductor.body(crystal, RenderItemEnergyCrystal.CRYSTAL, getter, t, 0.5F, t * 2F, Colour.packRGBA(c[0], c[1], c[2], 1F));
    }

    @Override
    public void getExtents(java.util.function.Consumer<org.joml.Vector3fc> output) {
        for (float x : new float[]{0.18F, 0.82F})
            for (float z : new float[]{0.18F, 0.82F})
                for (float y : new float[]{0.25F, 0.75F})
                    output.accept(new org.joml.Vector3f(x, y, z));
    }
}
