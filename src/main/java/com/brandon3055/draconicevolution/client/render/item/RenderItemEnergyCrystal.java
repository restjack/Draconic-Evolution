package com.brandon3055.draconicevolution.client.render.item;

import codechicken.lib.colour.Colour;
import codechicken.lib.render.CCModel;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.model.OBJParser;
import codechicken.lib.vec.Matrix4;
import codechicken.lib.vec.Rotation;
import com.brandon3055.brandonscore.api.TechLevel;
import com.brandon3055.brandonscore.api.TimeKeeper;
import com.brandon3055.draconicevolution.DraconicEvolution;
import com.brandon3055.draconicevolution.blocks.energynet.EnergyCrystal;
import com.brandon3055.draconicevolution.blocks.energynet.EnergyCrystal.CrystalType;
import com.brandon3055.draconicevolution.client.DEShaders;
import com.brandon3055.draconicevolution.client.handler.ClientEventHandler;
import com.brandon3055.draconicevolution.client.render.tile.RenderTileEnergyCrystal;
import com.brandon3055.draconicevolution.init.DEContent;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static com.brandon3055.draconicevolution.client.render.tile.RenderTileEnergyCrystal.COLOURS;

/**
 * Created by brandon3055 on 21/11/2016.
 */
public class RenderItemEnergyCrystal implements DEItemRenderer {
    public static final RenderType crystalBaseType = net.minecraft.client.renderer.rendertype.RenderTypes.entitySolid(Identifier.fromNamespaceAndPath(DraconicEvolution.MODID, "textures/models/crystal_base.png"));

    public static final RenderType CRYSTAL = DEShaders.ENERGY_CRYSTAL.renderType("de_energy_crystal_item", null, com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS);

    private final CrystalType type;
    private final TechLevel techLevel;
    private final CCModel crystalFull;
    private final CCModel crystalHalf;
    private final CCModel crystalBase;

    public RenderItemEnergyCrystal(CrystalType type, TechLevel techLevel) {
        this.type = type;
        this.techLevel = techLevel;
        Map<String, CCModel> map = new OBJParser(Identifier.fromNamespaceAndPath(DraconicEvolution.MODID, "models/block/crystal.obj")).quads().ignoreMtl().parse();
        crystalFull = CCModel.combine(map.values()).backfacedCopy();
        map = new OBJParser(Identifier.fromNamespaceAndPath(DraconicEvolution.MODID, "models/block/crystal_half.obj")).quads().ignoreMtl().parse();
        crystalHalf = map.get("Crystal").backfacedCopy();
        crystalBase = map.get("Base").backfacedCopy();
    }

    @Override
    public void renderItem(ItemStack stack, ItemDisplayContext context, PoseStack mStack, MultiBufferSource getter, int packedLight, int packedOverlay) {
        int tier = techLevel.index;
        Matrix4 mat = new Matrix4(mStack);
        CCRenderState ccrs = CCRenderState.instance();
        ccrs.reset();
        ccrs.brightness = packedLight;
        ccrs.overlay = packedOverlay;
        mat.translate(0.5, type == CrystalType.CRYSTAL_IO ? 0 : 0.5, 0.5);
        DEShaders.ENERGY_CRYSTAL.setMipmap(0);
        DEShaders.ENERGY_CRYSTAL.setColour(COLOURS[tier][0], COLOURS[tier][1], COLOURS[tier][2]);

        if (type == CrystalType.CRYSTAL_IO) {
            ccrs.bind(crystalBaseType, getter);
            com.brandon3055.draconicevolution.client.render.DEModelEmission.render(crystalBase, ccrs, mat);
            mat.apply(new Rotation(TimeKeeper.getClientTick() / 400F, 0, 1, 0));
            ccrs.baseColour = Colour.packRGBA(r[tier], g[tier], b[tier], 1F);
            ccrs.bind(CRYSTAL, getter);
            com.brandon3055.draconicevolution.client.render.DEModelEmission.render(crystalHalf, ccrs, mat);
        } else {
            ccrs.baseColour = Colour.packRGBA(r[tier], g[tier], b[tier], 1F);
            mat.apply(new Rotation(TimeKeeper.getClientTick() / 400F, 0, 1, 0));
            ccrs.bind(CRYSTAL, getter);
            com.brandon3055.draconicevolution.client.render.DEModelEmission.render(crystalFull, ccrs, mat);
        }
    }

    @Override
    public void getExtents(java.util.function.Consumer<org.joml.Vector3fc> output) {
        CCModel model = type == CrystalType.CRYSTAL_IO ? crystalHalf : crystalFull;
        // Bound all animation angles, rather than a single frame, using the OBJ's swept radius.
        double radius = 0, minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        for (var vertex : model.verts) {
            var v = vertex.vec;
            radius = Math.max(radius, Math.hypot(v.x, v.z));
            minY = Math.min(minY, v.y); maxY = Math.max(maxY, v.y);
        }
        double yOffset = type == CrystalType.CRYSTAL_IO ? 0 : 0.5;
        for (double x : new double[]{0.5 - radius, 0.5 + radius})
            for (double z : new double[]{0.5 - radius, 0.5 + radius})
                for (double y : new double[]{minY + yOffset, maxY + yOffset})
                    output.accept(new org.joml.Vector3f((float)x, (float)y, (float)z));
        if (type == CrystalType.CRYSTAL_IO) for (var vertex : crystalBase.verts) {
            var v = vertex.vec;
            output.accept(new org.joml.Vector3f((float)v.x + 0.5F, (float)v.y, (float)v.z + 0.5F));
        }
    }

    private static float[] r = {0.0F, 0.47F, 1.0F, 0.62F};
    private static float[] g = {0.2F, 0.0F, 0.4F, 0.03F};
    private static float[] b = {0.3F, 0.58F, 0.1F, 0.05F};

    //@formatter:off //This is not cursed at all! idk what your talking about!
    public static class ITEM_BASIC_IO_CRYSTAL extends RenderItemEnergyCrystal { public ITEM_BASIC_IO_CRYSTAL() { super(EnergyCrystal.CrystalType.CRYSTAL_IO, TechLevel.DRACONIUM);}}
    public static class ITEM_WYVERN_IO_CRYSTAL extends RenderItemEnergyCrystal { public ITEM_WYVERN_IO_CRYSTAL() { super(EnergyCrystal.CrystalType.CRYSTAL_IO, TechLevel.WYVERN);}}
    public static class ITEM_DRACONIC_IO_CRYSTAL extends RenderItemEnergyCrystal { public ITEM_DRACONIC_IO_CRYSTAL() { super(EnergyCrystal.CrystalType.CRYSTAL_IO, TechLevel.DRACONIC);}}
    public static class ITEM_BASIC_RELAY_CRYSTAL extends RenderItemEnergyCrystal { public ITEM_BASIC_RELAY_CRYSTAL() { super(EnergyCrystal.CrystalType.RELAY, TechLevel.DRACONIUM);}}
    public static class ITEM_WYVERN_RELAY_CRYSTAL extends RenderItemEnergyCrystal { public ITEM_WYVERN_RELAY_CRYSTAL() { super(EnergyCrystal.CrystalType.RELAY, TechLevel.WYVERN);}}
    public static class ITEM_DRACONIC_RELAY_CRYSTAL extends RenderItemEnergyCrystal { public ITEM_DRACONIC_RELAY_CRYSTAL() { super(EnergyCrystal.CrystalType.RELAY, TechLevel.DRACONIC);}}
    public static class ITEM_BASIC_WIRELESS_CRYSTAL extends RenderItemEnergyCrystal { public ITEM_BASIC_WIRELESS_CRYSTAL() { super(EnergyCrystal.CrystalType.WIRELESS, TechLevel.DRACONIUM);}}
    public static class ITEM_WYVERN_WIRELESS_CRYSTAL extends RenderItemEnergyCrystal { public ITEM_WYVERN_WIRELESS_CRYSTAL() { super(EnergyCrystal.CrystalType.WIRELESS, TechLevel.WYVERN);}}
    public static class ITEM_DRACONIC_WIRELESS_CRYSTAL extends RenderItemEnergyCrystal { public ITEM_DRACONIC_WIRELESS_CRYSTAL() { super(EnergyCrystal.CrystalType.WIRELESS, TechLevel.DRACONIC);}}
    public static class ITEM_CHAOTIC_IO_CRYSTAL extends RenderItemEnergyCrystal { public ITEM_CHAOTIC_IO_CRYSTAL() { super(EnergyCrystal.CrystalType.CRYSTAL_IO, TechLevel.CHAOTIC);}}
    public static class ITEM_CHAOTIC_RELAY_CRYSTAL extends RenderItemEnergyCrystal { public ITEM_CHAOTIC_RELAY_CRYSTAL() { super(EnergyCrystal.CrystalType.RELAY, TechLevel.CHAOTIC);}}
    public static class ITEM_CHAOTIC_WIRELESS_CRYSTAL extends RenderItemEnergyCrystal { public ITEM_CHAOTIC_WIRELESS_CRYSTAL() { super(EnergyCrystal.CrystalType.WIRELESS, TechLevel.CHAOTIC);}}
    //@formatter:on
}
