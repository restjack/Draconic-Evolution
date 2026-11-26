package com.brandon3055.draconicevolution.client.render.tile;

import com.brandon3055.brandonscore.client.gui.modular.lib.GuiRender;
import codechicken.lib.math.MathHelper;
import codechicken.lib.render.CCModel;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.buffer.TransformingVertexConsumer;
import codechicken.lib.render.model.OBJParser;
import codechicken.lib.vec.Matrix4;
import codechicken.lib.vec.Vector3;
import com.brandon3055.brandonscore.api.TimeKeeper;
import com.brandon3055.brandonscore.client.render.BlockEntityRendererTransparent;
import com.brandon3055.brandonscore.client.render.RenderUtils;
import com.brandon3055.brandonscore.lib.Vec3D;
import com.brandon3055.brandonscore.utils.MathUtils;
import com.brandon3055.draconicevolution.DraconicEvolution;
import com.brandon3055.draconicevolution.blocks.reactor.tileentity.TileReactorComponent;
import com.brandon3055.draconicevolution.blocks.reactor.tileentity.TileReactorCore;
import com.brandon3055.draconicevolution.blocks.reactor.tileentity.TileReactorInjector;
import com.brandon3055.draconicevolution.client.DEShaders;
import com.brandon3055.draconicevolution.client.handler.ClientEventHandler;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import com.brandon3055.brandonscore.client.render.ExtractedModel;
import com.brandon3055.draconicevolution.client.render.DEModelEmission;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;

import java.util.Map;

import static com.brandon3055.draconicevolution.DraconicEvolution.MODID;

/**
 * Created by brandon3055 on 6/11/2016.
 */
public class RenderTileReactorCore implements BlockEntityRenderer<TileReactorCore, RenderTileReactorCore.State> {

    private static CCModel model = null;

    public static final RenderType REACTOR_CORE_TYPE = DEShaders.reactorShader.renderType("reactor_type", null, VertexFormat.Mode.QUADS);
    public static final RenderType REACTOR_SHIELD_TYPE = DEShaders.reactorShieldShader.renderType("shield_type", null, VertexFormat.Mode.QUADS);
    public static final RenderType REACTOR_BEAM_TYPE = DEShaders.reactorBeamShader.renderType("reactor_beam", null, VertexFormat.Mode.TRIANGLE_STRIP);
    private static final RenderType GUI_CORE_TYPE = DEShaders.REACTOR_GUI.renderType("reactor_gui", null, VertexFormat.Mode.QUADS);
    private static final RenderType GUI_SHIELD_TYPE = DEShaders.REACTOR_SHIELD_GUI.renderType("reactor_shield_gui", null, VertexFormat.Mode.QUADS);

    public static final class State extends BlockEntityRenderState {
        ExtractedModel geometry;
    }
    @Override public State createRenderState() { return new State(); }
    @Override
    public void extractRenderState(TileReactorCore tile, State state, float partialTick, net.minecraft.world.phys.Vec3 camera, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(tile, state, breakProgress);
        ExtractedModel.Capture capture = new ExtractedModel.Capture();
        extractGeometry(tile, partialTick, new PoseStack(), capture, state.lightCoords, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
        state.geometry = capture.finish();
    }
    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        state.geometry.submit(pose, collector);
    }

    public RenderTileReactorCore(BlockEntityRendererProvider.Context context) { reloadModel(); }

    public static void reloadModel() {
        Map<String, CCModel> map = new OBJParser(Identifier.fromNamespaceAndPath(DraconicEvolution.MODID, "models/block/reactor/reactor_core.obj")).quads().ignoreMtl().parse();
        model = CCModel.combine(map.values());
    }

    public static void renderCore(Matrix4 mat, CCRenderState ccrs, float animation, double animState, float intensity, float shieldPower, float partialTicks, MultiBufferSource getter) {
        renderCore(mat, ccrs, animation, animState, intensity, shieldPower, partialTicks, getter, false);
    }

    private static void renderCore(Matrix4 mat, CCRenderState ccrs, float animation, double animState, float intensity, float shieldPower, float partialTicks, MultiBufferSource getter, boolean gui) {
        //The model should be generated during startup, unless Embeddium is installed... STOP BREAKING MY SHIT!!!!
        if (model == null) {
            Map<String, CCModel> map = new OBJParser(Identifier.fromNamespaceAndPath(DraconicEvolution.MODID, "models/block/reactor/reactor_core.obj")).quads().ignoreMtl().parse();
            model = CCModel.combine(map.values());
        }
        var core = gui ? DEShaders.REACTOR_GUI : DEShaders.reactorShader;
        core.uniform(96).glUniform1f(animation);
        core.uniform(100).glUniform1f(intensity);
        ccrs.bind(gui ? GUI_CORE_TYPE : REACTOR_CORE_TYPE, getter);
        DEModelEmission.render(model, ccrs, mat);
        RenderUtils.endBatch(getter);

        mat.scale(1.05);

        var shield = gui ? DEShaders.REACTOR_SHIELD_GUI : DEShaders.reactorShieldShader;
        shield.uniform(96).glUniform1f(animation);
        shield.uniform(100).glUniform1f((0.7F * shieldPower) - (float) (1 - animState));
        ccrs.bind(gui ? GUI_SHIELD_TYPE : REACTOR_SHIELD_TYPE, getter);
        DEModelEmission.render(model, ccrs, mat);
        RenderUtils.endBatch(getter);
    }

    public static void renderGUI(GuiRender render, TileReactorCore te) {
        double diameter = 100;
        float t = (float) (te.temperature.get() / TileReactorCore.MAX_TEMPERATURE);
        float intensity = t <= 0.2 ? (float) MathUtils.map(t, 0, 0.2, 0, 0.3) : t <= 0.8 ? (float) MathUtils.map(t, 0.2, 0.8, 0.3, 1) : (float) MathUtils.map(t, 0.8, 1, 1, 1.3);
        float animation = (te.coreAnimation + (0 * (float) te.shaderAnimationState.get())) / 20F;
        float shieldPower = (float) (te.maxShieldCharge.get() > 0 ? te.shieldCharge.get() / te.maxShieldCharge.get() : 0);
        Minecraft mc = Minecraft.getInstance();
        Matrix4 mat = new Matrix4();
        mat.scale(diameter);
        float partialTicks = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        mat.rotate((TimeKeeper.getClientTick() + partialTicks) / 400F, Vector3.Y_POS);
        CCRenderState ccrs = CCRenderState.instance();
        ccrs.reset();
        ExtractedModel.Capture capture = new ExtractedModel.Capture();
        renderCore(mat, ccrs, animation, te.shaderAnimationState.get(), intensity, shieldPower, partialTicks, capture, true);
        ExtractedModel geometry = capture.finish();
        render.withExtraction(() -> {
            var graphics = render.guiGraphicsWrapper();
            graphics.submitPictureInPictureRenderState(new com.brandon3055.draconicevolution.client.shader.ReactorGuiState(
                    -64, -64, 64, 64, geometry, new org.joml.Matrix3x2f(graphics.pose()), graphics.peekScissorStack()));
        });
    }

    private void extractGeometry(TileReactorCore te, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        if (!com.brandon3055.draconicevolution.DEConfig.blockEffectRendering) return; // client safety switch (eli.5)
        Matrix4 mat = new Matrix4(poseStack);
        CCRenderState ccrs = CCRenderState.instance();
        ccrs.reset();
        ccrs.brightness = packedLight;
        ccrs.overlay = packedOverlay;

        double diameter = te.getCoreDiameter();
        float t = (float) (te.temperature.get() / TileReactorCore.MAX_TEMPERATURE);
        float intensity = t <= 0.2 ? (float) MathUtils.map(t, 0, 0.2, 0, 0.3) : t <= 0.8 ? (float) MathUtils.map(t, 0.2, 0.8, 0.3, 1) : (float) MathUtils.map(t, 0.8, 1, 1, 1.3);
        float shieldPower = (float) (te.maxShieldCharge.get() > 0 ? te.shieldCharge.get() / te.maxShieldCharge.get() : 0);
        float animation = (te.coreAnimation + (partialTicks * (float) te.shaderAnimationState.get())) / 20F;

        mat.translate(0.5, 0.5, 0.5);
        mat.scale(diameter);
        mat.rotate((ClientEventHandler.elapsedTicks + partialTicks) / 400F, Vector3.Y_POS);

        renderCore(mat, ccrs, animation, te.shaderAnimationState.get(), intensity, shieldPower, partialTicks, buffers);

        float coreSize = (float) te.getCoreDiameter() / 2.3F;
        float fxState = te.shieldAnimationState;

        for (Direction direction : Direction.values()) {
            TileReactorComponent component = te.getComponent(direction.getOpposite());
            if (component == null) continue;
            Direction facing = component.facing.get();
            float dist = (float) Math.sqrt(component.getBlockPos().distSqr(te.getBlockPos()));

            Vec3D pos1 = Vec3D.getCenter(component.getBlockPos()).subtract(new Vec3D(te.getBlockPos())).offset(facing, -0.35D);

            if (component instanceof TileReactorInjector) {
                Vec3D pos2 = pos1.copy().offset(facing, 0.6D);

                DEShaders.reactorBeamType.glUniform1i(2);
                DEShaders.reactorBeamFade.glUniform1f(1F);
                DEShaders.reactorBeamPower.glUniform1f(fxState);
                DEShaders.reactorBeamStartup.glUniform1f(fxState);
                renderShaderBeam(ccrs, facing, fxState, buffers, poseStack, pos1, 0.1F, 0.1F, 0.6F, true, false);

                DEShaders.reactorBeamFade.glUniform1f(0F);
                renderShaderBeam(ccrs, facing, fxState, buffers, poseStack, pos2, 0.1F, coreSize / 1.5, dist - (coreSize * 1.3F), false, false);
            } else {
                Vec3D pos2 = pos1.copy().offset(facing, 0.8D);

                //Inner Inner
                DEShaders.reactorBeamType.glUniform1i(1);
                DEShaders.reactorBeamFade.glUniform1f(1F);
                DEShaders.reactorBeamPower.glUniform1f((float) te.animExtractState.get());
                DEShaders.reactorBeamStartup.glUniform1f((float) te.animExtractState.get());
                renderShaderBeam(ccrs, facing, fxState, buffers, poseStack, pos1, 0.263F, 0.263F, 0.8F, true, false);

                DEShaders.reactorBeamFade.glUniform1f(0F);
                renderShaderBeam(ccrs, facing, fxState, buffers, poseStack, pos2, 0.263F, coreSize / 2, dist - (coreSize * 1.3F), false, false);

                //Draw Outer
                DEShaders.reactorBeamType.glUniform1i(0);
                DEShaders.reactorBeamFade.glUniform1f(1F);
                DEShaders.reactorBeamPower.glUniform1f(fxState);
                DEShaders.reactorBeamStartup.glUniform1f(fxState);
                renderShaderBeam(ccrs, facing, fxState, buffers, poseStack, pos1, 0.355F, 0.355F, 0.8F, true, false);

                DEShaders.reactorBeamFade.glUniform1f(0F);
                renderShaderBeam(ccrs, facing, fxState, buffers, poseStack, pos2, 0.355F, coreSize, dist - coreSize, false, true);
            }

            ccrs.reset();
        }
    }

    public void renderShaderBeam(CCRenderState ccrs, Direction facing, float fxState, MultiBufferSource buffers, PoseStack poseStack, Vec3D pos, double widthStart, double widthEnd, float length, boolean fadeReverse, boolean highRes) {
        VertexConsumer buffer = new TransformingVertexConsumer(buffers.getBuffer(REACTOR_BEAM_TYPE), poseStack);
        ccrs.bind(buffer, DefaultVertexFormat.POSITION_TEX_COLOR);
//        ccrs.startDrawing(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_TEX_COLOR);
        
        float sides = highRes ? 599 : 99;
        for (int i = 0; i < sides + 1; i++) {
            double angle = (i / sides) * (Math.PI * 2);// + beamRotation;
            float sin = (float) MathHelper.sin(angle);
            float cos = (float) MathHelper.cos(angle);
            float texX = i / sides;
            Vec3D point = pos.copy().radialOffset(facing.getAxis(), sin, cos, widthStart);
            buffer.addVertex((float) point.x, (float) point.y, (float) point.z).setColor(1F, 1F, 1F, fadeReverse ? 0F : fxState).setUv(texX, (fadeReverse ? 0.1F : 1F));
            point.offset(facing, length);
            point.radialOffset(facing.getAxis(), sin, cos, widthEnd - widthStart);
            buffer.addVertex((float) point.x, (float) point.y, (float) point.z).setColor(1F, 1F, 1F, fadeReverse ? fxState : 0F).setUv(texX, 0);
        }
//        ccrs.draw();
        RenderUtils.endBatch(buffers);
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public AABB getRenderBoundingBox(TileReactorCore blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        return new AABB(pos.getX() - 14, pos.getY() - 14, pos.getZ() - 14, pos.getX() + 15, pos.getY() + 15, pos.getZ() + 15);
    }
}
