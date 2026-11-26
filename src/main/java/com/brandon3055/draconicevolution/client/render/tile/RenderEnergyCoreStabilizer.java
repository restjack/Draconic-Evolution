package com.brandon3055.draconicevolution.client.render.tile;

import codechicken.lib.math.MathHelper;
import codechicken.lib.render.CCModel;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.model.OBJParser;
import com.brandon3055.draconicevolution.DraconicEvolution;
import com.brandon3055.draconicevolution.blocks.tileentity.TileEnergyCoreStabilizer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
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
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import org.joml.Quaternionf;

import java.util.Map;

/**
 * Created by brandon3055 on 19/4/2016.
 */
public class RenderEnergyCoreStabilizer implements BlockEntityRenderer<TileEnergyCoreStabilizer, RenderEnergyCoreStabilizer.State> {

    private static final RenderType MODEL_TYPE = RenderTypes.entitySolid(Identifier.fromNamespaceAndPath(DraconicEvolution.MODID, "textures/block/energy_core/stabilizer_large.png"));

    private static final RenderType MODEL_TYPE_ACTIVE = TileRenderPipelines.STABILIZER_ACTIVE;

    private CCModel model;

    public RenderEnergyCoreStabilizer(BlockEntityRendererProvider.Context context) {
        Map<String, CCModel> map = new OBJParser(Identifier.fromNamespaceAndPath(DraconicEvolution.MODID, "models/block/energy_core/stabilizer_large.obj")).quads().ignoreMtl().parse();
        model = CCModel.combine(map.values()).backfacedCopy();
    }

//    Ask covers about this...

    public static class State extends BlockEntityRenderState {
        ExtractedModel geometry;
    }

    @Override
    public State createRenderState() { return new State(); }

    @Override
    public void extractRenderState(TileEnergyCoreStabilizer tile, State state, float partialTicks, Vec3 cameraPosition,
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

    private void extractGeometry(TileEnergyCoreStabilizer tile, float partialTicks, PoseStack poseStack, MultiBufferSource getter, int packedLight, int packedOverlay) {
        if (!com.brandon3055.draconicevolution.DEConfig.blockEffectRendering) return; // client safety switch (eli.5)
        if (!tile.isValidMultiBlock.get()) return;

        CCRenderState ccrs = CCRenderState.instance();
        ccrs.reset();
        ccrs.brightness = 240;
        ccrs.overlay = packedOverlay;

        boolean coreActive = tile.isCoreActive.get();
        Direction facing;
        if (coreActive) {
            facing = tile.coreDirection.get();
        } else {
            facing = Direction.get(Direction.AxisDirection.POSITIVE, tile.multiBlockAxis.get());
        }

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        if (facing.getAxis() == Direction.Axis.X || facing.getAxis() == Direction.Axis.Y) {
            poseStack.mulPose(new Quaternionf().rotationXYZ(facing.getStepY() * 90 * (float)MathHelper.torad, facing.getStepX() * -90 * (float)MathHelper.torad, 0));
        } else if (facing == Direction.SOUTH) {
            poseStack.mulPose(Axis.YP.rotationDegrees(180F));
        }
        poseStack.mulPose(Axis.ZP.rotationDegrees(tile.rotation + (coreActive ? partialTicks : 0)));
        poseStack.translate(0, -1.5, 0);
        ccrs.bind(coreActive ? MODEL_TYPE_ACTIVE : MODEL_TYPE, getter, poseStack);
        DEModelEmission.render(model, ccrs);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(TileEnergyCoreStabilizer blockEntity) {
        return BlockEntityRenderer.super.getRenderBoundingBox(blockEntity).inflate(1);
    }
}
