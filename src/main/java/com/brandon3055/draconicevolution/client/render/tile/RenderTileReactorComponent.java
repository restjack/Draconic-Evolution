package com.brandon3055.draconicevolution.client.render.tile;

import codechicken.lib.render.CCModel;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.model.OBJParser;
import codechicken.lib.vec.Matrix4;
import codechicken.lib.vec.Rotation;
import codechicken.lib.vec.Vector3;
import com.brandon3055.draconicevolution.blocks.reactor.tileentity.TileReactorComponent;
import com.brandon3055.draconicevolution.blocks.reactor.tileentity.TileReactorInjector;
import com.brandon3055.draconicevolution.blocks.reactor.tileentity.TileReactorStabilizer;
import com.brandon3055.draconicevolution.init.DEContent;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.covers1624.quack.collection.FastStream;
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
import net.minecraft.world.item.Item;

import java.util.Map;

import static codechicken.lib.math.MathHelper.torad;
import static com.brandon3055.draconicevolution.DraconicEvolution.MODID;

/**
 * Created by brandon3055 on 20/01/2017.
 */
public class RenderTileReactorComponent implements BlockEntityRenderer<TileReactorComponent, RenderTileReactorComponent.State> {

    private static final RenderType STAB_FRAME_TYPE = RenderTypes.entitySolid(Identifier.fromNamespaceAndPath(MODID, "textures/block/reactor/reactor_stabilizer.png"));
    private static final RenderType INJECTOR_FRAME_TYPE = RenderTypes.entitySolid(Identifier.fromNamespaceAndPath(MODID, "textures/block/reactor/reactor_injector.png"));

    private static final RenderType STAB_GLOW_TYPE = TileRenderPipelines.STAB_GLOW;
    private static final RenderType INJECTOR_GLOW_TYPE = TileRenderPipelines.INJECTOR_GLOW;

    private static CCModel modelInjectorBase;
    private static CCModel modelInjectorEmitters;

    private static CCModel modelStabFrame;
    private static CCModel modelStabInnerRotor;
    private static CCModel modelStabInnerRotorArm;
    private static CCModel modelStabOuterRotor;
    private static CCModel modelStabOuterRotorArm;
    private static CCModel modelStabRing;
    private static CCModel modelStabRingEmitter;

    private static CCModel modelInnerRotorPart;
    private static CCModel modelOuterRotorPart;

    static { reloadModels(); }

    public static void reloadModels() {
        Map<String, CCModel> map = new OBJParser(Identifier.fromNamespaceAndPath(MODID, "models/block/reactor/reactor_injector.obj")).quads().ignoreMtl().parse();
        modelInjectorBase = CCModel.combine(FastStream.of(map.entrySet())
                .filter(e -> !e.getKey().startsWith("emitter"))
                .map(Map.Entry::getValue)
                .toLinkedList()).backfacedCopy();
        modelInjectorEmitters = CCModel.combine(FastStream.of(map.entrySet())
                .filter(e -> e.getKey().startsWith("emitter"))
                .map(Map.Entry::getValue)
                .toLinkedList()).backfacedCopy();

        Map<String, CCModel> stabMap = new OBJParser(Identifier.fromNamespaceAndPath(MODID, "models/block/reactor/reactor_stabilizer.obj")).quads().ignoreMtl().parse();
        modelStabFrame = CCModel.combine(FastStream.of(stabMap.entrySet())
                .filter(e -> e.getKey().startsWith("frame"))
                .map(Map.Entry::getValue)
                .toLinkedList()).backfacedCopy();

        modelStabInnerRotor = CCModel.combine(FastStream.of(stabMap.entrySet())
                .filter(e -> e.getKey().startsWith("inner_rotor_blade"))
                .map(Map.Entry::getValue)
                .toLinkedList()).backfacedCopy();

        modelStabInnerRotorArm = CCModel.combine(FastStream.of(stabMap.entrySet())
                .filter(e -> e.getKey().startsWith("inner_rotor_arm"))
                .map(Map.Entry::getValue)
                .toLinkedList()).backfacedCopy();

        modelStabOuterRotor = CCModel.combine(FastStream.of(stabMap.entrySet())
                .filter(e -> e.getKey().startsWith("outer_rotor_blade"))
                .map(Map.Entry::getValue)
                .toLinkedList()).backfacedCopy();

        modelStabOuterRotorArm = CCModel.combine(FastStream.of(stabMap.entrySet())
                .filter(e -> e.getKey().startsWith("outer_rotor_arm"))
                .map(Map.Entry::getValue)
                .toLinkedList()).backfacedCopy();

        modelStabRing = CCModel.combine(FastStream.of(stabMap.entrySet())
                .filter(e -> e.getKey().startsWith("ring"))
                .map(Map.Entry::getValue)
                .toLinkedList()).backfacedCopy();

        modelStabRingEmitter = CCModel.combine(FastStream.of(stabMap.entrySet())
                .filter(e -> e.getKey().startsWith("focus_panel"))
                .map(Map.Entry::getValue)
                .toLinkedList()).backfacedCopy();

        modelInnerRotorPart = CCModel.combine(FastStream.of(stabMap.entrySet())
                .filter(e -> e.getKey().startsWith("inner_rotor_blade_a"))
                .map(Map.Entry::getValue)
                .toLinkedList()).backfacedCopy();

        modelOuterRotorPart = CCModel.combine(FastStream.of(stabMap.entrySet())
                .filter(e -> e.getKey().startsWith("outer_rotor_blade_a"))
                .map(Map.Entry::getValue)
                .toLinkedList()).backfacedCopy();
    }

    public RenderTileReactorComponent(BlockEntityRendererProvider.Context context) {
        reloadModels();
    }

    public static class State extends BlockEntityRenderState {
        ExtractedModel geometry;
    }

    @Override
    public State createRenderState() { return new State(); }

    @Override
    public void extractRenderState(TileReactorComponent tile, State state, float partialTicks, Vec3 cameraPosition,
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

    private void extractGeometry(TileReactorComponent te, float partialTicks, PoseStack mStack, MultiBufferSource getter, int packedLight, int packedOverlay) {
        if (!com.brandon3055.draconicevolution.DEConfig.blockEffectRendering) return; // client safety switch (eli.5)
        Matrix4 mat = new Matrix4(mStack);
        mat.translate(0.5, 0, 0.5);

        CCRenderState ccrs = CCRenderState.instance();
        ccrs.reset();
        ccrs.brightness = packedLight;
        ccrs.overlay = packedOverlay;

        switch (te.facing.get()) {
            case SOUTH -> mat.rotate(180 * torad, Vector3.Y_POS);
            case EAST -> mat.rotate(-90 * torad, Vector3.Y_POS);
            case WEST -> mat.rotate(90 * torad, Vector3.Y_POS);
            case UP -> mat.apply(new Rotation(90 * torad, Vector3.X_POS).at(new Vector3(0, 0.5, 0)));
            case DOWN -> mat.apply(new Rotation(-90 * torad, Vector3.X_POS).at(new Vector3(0, 0.5, 0)));
        }

        if (te instanceof TileReactorStabilizer) {
            float coreRotation = te.animRotation + (partialTicks * te.animRotationSpeed);//Remember Partial Ticks here
            renderStabilizer(ccrs, mat, getter, coreRotation, te.animRotationSpeed / 15F, packedLight, packedOverlay);
        } else if (te instanceof TileReactorInjector) {
            renderInjector(ccrs, mat, getter, te.animRotationSpeed / 15F, packedLight, packedOverlay);
        }

        ccrs.reset();
    }

    public static void renderStabilizer(CCRenderState ccrs, Matrix4 mat, MultiBufferSource getter, float coreRotation, float brightness, int packedLight, int packedOverlay) {
        float ringRotation = coreRotation * -0.5F;//Remember Partial Ticks here

        Matrix4 innerRotorMat = mat.copy();
        innerRotorMat.apply(new Rotation(coreRotation * torad, Vector3.Z_POS).at(new Vector3(0, 0.5, 0)));
        Matrix4 outerRotorMat = mat.copy();
        outerRotorMat.apply(new Rotation(coreRotation * torad * -2, Vector3.Z_POS).at(new Vector3(0, 0.5, 0)));

        ccrs.bind(STAB_FRAME_TYPE, getter);
        DEModelEmission.render(modelStabFrame, ccrs, mat);
        DEModelEmission.render(modelStabInnerRotorArm, ccrs, innerRotorMat);
        DEModelEmission.render(modelStabOuterRotorArm, ccrs, outerRotorMat);

        for (int i = 0; i < 4; i++) {
            ccrs.brightness = packedLight;
            Matrix4 ringMat = mat.copy();
            ringMat.apply(new Rotation(((90 * i) + ringRotation) * torad, Vector3.Z_POS).at(new Vector3(0, 0.5, 0)));
            DEModelEmission.render(modelStabRing, ccrs, ringMat);

            ccrs.brightness = (int) (brightness * 240);
            Matrix4 emitterMat = ringMat.copy();
            emitterMat.apply(new Rotation(45 * torad, Vector3.X_NEG).at(new Vector3(0, 15 / 16F, -((8 + 1.5) / 16F))));
            DEModelEmission.render(modelStabRingEmitter, ccrs, emitterMat);
        }

        ccrs.brightness = (int) (brightness * 240);
        DEModelEmission.render(modelStabInnerRotor, ccrs, innerRotorMat);
        DEModelEmission.render(modelStabOuterRotor, ccrs, outerRotorMat);

        if (brightness >= 1) {
            ccrs.bind(STAB_GLOW_TYPE, getter);
            DEModelEmission.render(modelStabInnerRotor, ccrs, innerRotorMat);
            DEModelEmission.render(modelStabOuterRotor, ccrs, outerRotorMat);

            for (int i = 0; i < 4; i++) {
                Matrix4 emitterMat = mat.copy();
                emitterMat.apply(new Rotation(((90 * i) + ringRotation) * torad, Vector3.Z_POS).at(new Vector3(0, 0.5, 0)));
                emitterMat.apply(new Rotation(45 * torad, Vector3.X_NEG).at(new Vector3(0, 15 / 16F, -((8 + 1.5) / 16F))));
                DEModelEmission.render(modelStabRingEmitter, ccrs, emitterMat);
            }
        }
    }

    public static void renderInjector(CCRenderState ccrs, Matrix4 mat, MultiBufferSource getter, float brightness, int packedLight, int packedOverlay) {
        ccrs.bind(INJECTOR_FRAME_TYPE, getter);
        DEModelEmission.render(modelInjectorBase, ccrs, mat);

        ccrs.brightness = (int) (brightness * 240);
        DEModelEmission.render(modelInjectorEmitters, ccrs, mat);

        if (brightness >= 1) {
            ccrs.bind(INJECTOR_GLOW_TYPE, getter);
            DEModelEmission.render(modelInjectorEmitters, ccrs, mat);
        }
    }

    public static void renderComponent(Item item, CCRenderState ccrs, Matrix4 mat, MultiBufferSource getter, int packedLight, int packedOverlay) {
        if (item == DEContent.REACTOR_PRT_STAB_FRAME.get()) {
            ccrs.bind(STAB_FRAME_TYPE, getter);
            mat.translate(0.5, 0, 0.5);
            DEModelEmission.render(modelStabFrame, ccrs, mat);

        } else if (item == DEContent.REACTOR_PRT_IN_ROTOR.get()) {
            ccrs.bind(STAB_FRAME_TYPE, getter);
            mat.translate(0.3, 0, 0.5);
            mat.scale(1.5F, 1.5F, 1.5F);
            DEModelEmission.render(modelInnerRotorPart, ccrs, mat);

        } else if (item == DEContent.REACTOR_PRT_OUT_ROTOR.get()) {
            ccrs.bind(STAB_FRAME_TYPE, getter);
            mat.translate(0.3, 0, 0.5);
            mat.scale(1.5F, 1.5F, 1.5F);
            DEModelEmission.render(modelOuterRotorPart, ccrs, mat);

        } else if (item == DEContent.REACTOR_PRT_ROTOR_FULL.get()) {
            ccrs.bind(STAB_FRAME_TYPE, getter);
            mat.translate(0.5, -0.2, 0.5);
            mat.scale(1.5F, 1.5F, 1.5F);

            DEModelEmission.render(modelStabInnerRotor, ccrs, mat);
            DEModelEmission.render(modelStabInnerRotorArm, ccrs, mat);
            mat.apply(new Rotation(60 * torad, Vector3.Z_NEG).at(new Vector3(0, 0.5, 0)));
            DEModelEmission.render(modelStabOuterRotor, ccrs, mat);
            DEModelEmission.render(modelStabOuterRotorArm, ccrs, mat);

        } else if (item == DEContent.REACTOR_PRT_FOCUS_RING.get()) {
            ccrs.bind(STAB_FRAME_TYPE, getter);
            mat.translate(0.5, -0.2, 1.25);
            mat.scale(1.5F, 1.5F, 1.5F);

            for (int i = 0; i < 4; i++) {
                Matrix4 ringMat = mat.copy();
                ringMat.apply(new Rotation((90 * i) * torad, Vector3.Z_POS).at(new Vector3(0, 0.5, 0)));
                DEModelEmission.render(modelStabRing, ccrs, ringMat);

                Matrix4 emitterMat = ringMat.copy();
                emitterMat.apply(new Rotation(45 * torad, Vector3.X_NEG).at(new Vector3(0, 15 / 16F, -((8 + 1.5) / 16F))));
                DEModelEmission.render(modelStabRingEmitter, ccrs, emitterMat);
            }
        }
    }
}
