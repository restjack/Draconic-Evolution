package com.brandon3055.draconicevolution.client.render.tile;

import codechicken.lib.colour.Colour;
import codechicken.lib.math.MathHelper;
import codechicken.lib.render.CCModel;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.model.OBJParser;
import codechicken.lib.vec.Matrix4;
import codechicken.lib.vec.Scale;
import codechicken.lib.vec.Vector3;
import com.brandon3055.brandonscore.client.shader.BCShaders;
import com.brandon3055.draconicevolution.DraconicEvolution;
import com.brandon3055.draconicevolution.blocks.tileentity.TileEnergyPylon;
import com.brandon3055.draconicevolution.client.handler.ClientEventHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
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
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;

import java.util.Map;

/**
 * Created by brandon3055 on 20/05/2016.
 */
public class RenderTileEnergyPylon implements BlockEntityRenderer<TileEnergyPylon, RenderTileEnergyPylon.State> {

    private static RenderType modelType = RenderTypes.entitySolid(Identifier.fromNamespaceAndPath(DraconicEvolution.MODID, "textures/block/pylon_sphere_texture.png"));

    private static final RenderType shellType = TileRenderPipelines.PYLON_SHELL;
    private final CCModel model;

    public RenderTileEnergyPylon(BlockEntityRendererProvider.Context context) {
        Map<String, CCModel> map = new OBJParser(Identifier.fromNamespaceAndPath(DraconicEvolution.MODID, "models/pylon_sphere.obj")).quads().ignoreMtl().parse();
        model = CCModel.combine(map.values());
        model.apply(new Scale(-0.35, -0.35, -0.35));
        model.computeNormals();
    }

    public static class State extends BlockEntityRenderState {
        ExtractedModel geometry;
    }

    @Override
    public State createRenderState() { return new State(); }

    @Override
    public void extractRenderState(TileEnergyPylon tile, State state, float partialTicks, Vec3 cameraPosition,
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

    private void extractGeometry(TileEnergyPylon te, float partialTicks, PoseStack mStack, MultiBufferSource getter, int packedLight, int packedOverlay) {
        if (!com.brandon3055.draconicevolution.DEConfig.blockEffectRendering) return; // client safety switch (eli.5)
        if (!te.structureValid.get()) return;

        Matrix4 mat = new Matrix4(mStack);
        CCRenderState ccrs = CCRenderState.instance();
        ccrs.reset();
        ccrs.brightness = 240;
        ccrs.overlay = packedOverlay;

        ccrs.baseColour = 0x005efaFF;
        if (te.colour.notNull()) {
            ccrs.baseColour = te.colour.get().rgba();
        }

        ccrs.bind(modelType, getter);
        mat.translate(te.direction.get().getStepX(), te.direction.get().getStepY(), te.direction.get().getStepZ());
        mat.translate(0.5, 0.5, 0.5);
        mat.rotate(((ClientEventHandler.elapsedTicks + partialTicks) * 2F) * MathHelper.torad, new Vector3(0, 1, 0.5).normalize());
        DEModelEmission.render(model, ccrs, mat);

        float f = MathHelper.clip(((ClientEventHandler.elapsedTicks + partialTicks) % 35F) / 30F, 0, 1);
        if (te.ioMode.get().canExtract()) {
            f = 1F - f;
        }

        ccrs.baseColour = Colour.packRGBA(1F - f, 0xF5 / 255F, 0xfa / 255F, 1F - f);
        if (te.colour.notNull()) {
            ccrs.baseColour = Colour.packRGBA(te.colour.get().rF() * (1F - f), te.colour.get().gF(), te.colour.get().bF(), 1F - (f * f));
        }

        ccrs.bind(shellType, getter);
        mat.scale(1 + f);
        DEModelEmission.render(model, ccrs, mat);
    }

    @Override
    public int getViewDistance() {
        return com.brandon3055.draconicevolution.DEConfig.effectViewDistance(64); // eli.7
    }

    @Override
    public AABB getRenderBoundingBox(TileEnergyPylon blockEntity) {
        return BlockEntityRenderer.super.getRenderBoundingBox(blockEntity).inflate(1);
    }
}
