package com.brandon3055.draconicevolution.client.render.tile;

import codechicken.lib.colour.Colour;
import codechicken.lib.math.MathHelper;
import codechicken.lib.render.CCModel;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.model.OBJParser;
import codechicken.lib.vec.Matrix4;
import codechicken.lib.vec.Rotation;
import codechicken.lib.vec.Vector3;
import com.brandon3055.brandonscore.client.render.RenderUtils;
import com.brandon3055.brandonscore.lib.Vec3D;
import com.brandon3055.brandonscore.utils.Utils;
import com.brandon3055.draconicevolution.DraconicEvolution;
import com.brandon3055.draconicevolution.blocks.energynet.tileentity.TileCrystalBase;
import com.brandon3055.draconicevolution.blocks.energynet.tileentity.TileCrystalDirectIO;
import com.brandon3055.draconicevolution.client.DEShaders;
import com.brandon3055.draconicevolution.client.handler.ClientEventHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import com.brandon3055.brandonscore.client.render.ExtractedModel;
import com.brandon3055.draconicevolution.client.render.DEModelEmission;
import net.minecraft.client.renderer.rendertype.RenderTypes;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import java.util.Map;

/**
 * Created by brandon3055 on 6/11/2016.
 */
public class RenderTileEnergyCrystal implements BlockEntityRenderer<TileCrystalBase, RenderTileEnergyCrystal.State> {

    public static float[][] COLOURS = {{0.0F, 0.2F, 0.3F}, {0.47F, 0.0F, 0.58F}, {1.0F, 0.4F, 0.1F}, {0.62F, 0.03F, 0.05F}};

    private static final RenderType fallBackType = RenderTypes.entityTranslucent(Identifier.fromNamespaceAndPath(DraconicEvolution.MODID, "textures/models/crystal_no_shader.png"));
    private static final RenderType fallBackOverlayType = RenderTypes.entityTranslucent(Identifier.fromNamespaceAndPath(DraconicEvolution.MODID, "textures/models/crystal_base.png"));
    private static final RenderType crystalBaseType = RenderTypes.entitySolid(Identifier.fromNamespaceAndPath(DraconicEvolution.MODID, "textures/models/crystal_base.png"));

    public static final RenderType crystalType = DEShaders.ENERGY_CRYSTAL.renderType("de_energy_crystal_block", null, VertexFormat.Mode.QUADS);

    private final CCModel crystalFull;
    private final CCModel crystalHalf;
    private final CCModel crystalBase;

    public RenderTileEnergyCrystal(BlockEntityRendererProvider.Context context) {
        Map<String, CCModel> map = new OBJParser(Identifier.fromNamespaceAndPath(DraconicEvolution.MODID, "models/block/crystal.obj")).quads().ignoreMtl().parse();
        crystalFull = CCModel.combine(map.values());
        map = new OBJParser(Identifier.fromNamespaceAndPath(DraconicEvolution.MODID, "models/block/crystal_half.obj")).quads().ignoreMtl().parse();
        crystalHalf = map.get("Crystal");
        crystalBase = map.get("Base");
    }

    public static class State extends BlockEntityRenderState {
        ExtractedModel geometry;
    }

    @Override
    public State createRenderState() { return new State(); }

    @Override
    public void extractRenderState(TileCrystalBase tile, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTicks, cameraPosition, breakProgress);
        ExtractedModel.Capture capture = new ExtractedModel.Capture();
        extractGeometry(tile, partialTicks, new PoseStack(), capture, state.lightCoords,
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
        state.geometry = capture.finish();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.geometry != null) state.geometry.submit(poseStack, collector);
    }

    private void extractGeometry(TileCrystalBase te, float partialTicks, PoseStack mStack, MultiBufferSource getter, int packedLight, int packedOverlay) {
        int tier = te.getTier();

        Matrix4 mat = new Matrix4(mStack);
        CCRenderState ccrs = CCRenderState.instance();
        ccrs.reset();
        ccrs.brightness = 240;//packedLight;
        ccrs.overlay = packedOverlay;

        Player player = Minecraft.getInstance().player;
        BlockPos pos = te.getBlockPos();
        double x = (player == null ? pos.getX() + 0.5 : player.getX()) - (pos.getX() + 0.5);
        double y = (player == null ? pos.getY() + 0.5 : player.getY()) - (pos.getY() + 0.5);
        double z = (player == null ? pos.getZ() + 0.5 : player.getZ()) - (pos.getZ() + 0.5);
        double mm = MathHelper.clip((((x * x) + (y * y) + (z * z) - 5) / 4096), 0, 1);
//        float xrot = (float) Math.atan2(x + 0.5, z + 0.5);
//        float dist = (float) Utils.getDistanceAtoB(Vec3D.getCenter(pos).x, Vec3D.getCenter(pos).z, Minecraft.getInstance().player.getX(), Minecraft.getInstance().player.getZ());
//        float yrot = (float) net.minecraft.util.Mth.atan2(dist, y + 0.5);
        ccrs.baseColour = 0xFFFFFFFF;
        DEShaders.ENERGY_CRYSTAL.setMipmap((float) mm);
        DEShaders.ENERGY_CRYSTAL.setColour(COLOURS[tier][0], COLOURS[tier][1], COLOURS[tier][2]);
        if (te instanceof TileCrystalDirectIO io) { // Eli's pick (option A): shader flow shows Input vs Output
            net.minecraft.core.Direction f = io.facing.get();
            DEShaders.ENERGY_CRYSTAL.setFlow(io.outputMode.get() ? -1F : 1F, f.getStepX(), f.getStepY(), f.getStepZ());
        } else {
            DEShaders.ENERGY_CRYSTAL.setFlow(0F, 0F, 1F, 0F);
        }
//        DEShaders.energyCrystalAngle.glUniform2f(xrot / -3.125F, yrot / 3.125F);

        if (te instanceof TileCrystalDirectIO) {
            ccrs.bind(crystalBaseType, getter);
            mat.translate(0.5, 1, 0.5);
            mat.scale(-0.5);
            mat.apply(Rotation.sideOrientation(((TileCrystalDirectIO) te).facing.get().getOpposite().get3DDataValue(), 0).at(new Vector3(0, 1, 0)));
            DEModelEmission.render(crystalBase, ccrs, mat);
            mat.rotate((ClientEventHandler.elapsedTicks + partialTicks) / 400F, new Vector3(0, 1, 0));
            if (mm < 1) {
                ccrs.bind(crystalType, getter);
                DEModelEmission.render(crystalHalf, ccrs, mat);
            } else {
                ccrs.baseColour = Colour.packRGBA(r[tier], g[tier], b[tier], 1F);
                ccrs.bind(fallBackType, getter);
                DEModelEmission.render(crystalHalf, ccrs, mat);
                ccrs.baseColour = -1;
            }
        } else {
            mat.translate(Vector3.CENTER);
            mat.rotate(180 * MathHelper.torad, new Vector3(1, 0, 0));
            mat.scale(-0.5);
            mat.rotate((ClientEventHandler.elapsedTicks + partialTicks) / 400F, new Vector3(0, 1, 0));
            if (mm < 1) {
                ccrs.bind(crystalType, getter);
                DEModelEmission.render(crystalFull, ccrs, mat);
            } else {
                ccrs.baseColour = Colour.packRGBA(r[tier], g[tier], b[tier], 1F);
                ccrs.bind(fallBackType, getter);
                DEModelEmission.render(crystalFull, ccrs, mat);
                ccrs.baseColour = -1;
            }
        }
        // Reset, so other users of this shader (item renderer, Conductor, add-ons) never inherit an I/O flow.
        DEShaders.ENERGY_CRYSTAL.setFlow(0F, 0F, 1F, 0F);
    }

    private static float[] r = {0.0F, 0.55F, 1.0F, 0.75F};
    private static float[] g = {0.35F, 0.3F, 0.572F, 0.06F};
    private static float[] b = {0.65F, 0.9F, 0.172F, 0.08F};
}
