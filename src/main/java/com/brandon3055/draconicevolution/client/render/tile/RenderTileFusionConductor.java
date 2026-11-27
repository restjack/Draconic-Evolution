package com.brandon3055.draconicevolution.client.render.tile;

import codechicken.lib.math.MathHelper;
import codechicken.lib.render.CCModel;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.model.OBJParser;
import codechicken.lib.vec.Matrix4;
import codechicken.lib.vec.Vector3;
import com.brandon3055.brandonscore.client.render.ExtractedModel;
import com.brandon3055.draconicevolution.DEConfig;
import com.brandon3055.draconicevolution.DraconicEvolution;
import com.brandon3055.draconicevolution.blocks.tileentity.TileFusionConductor;
import com.brandon3055.draconicevolution.blocks.tileentity.TileFusionCraftingCore;
import com.brandon3055.draconicevolution.client.DEShaders;
import com.brandon3055.draconicevolution.client.handler.ClientEventHandler;
import com.brandon3055.draconicevolution.client.render.DEModelEmission;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

/**
 * Fusion Conductor: purple energy crystal with a gold ring that spins faster while working, a beam down into the core
 * while it crafts, and thin purple beams to each injector it powers.
 */
public class RenderTileFusionConductor implements BlockEntityRenderer<TileFusionConductor, RenderTileFusionConductor.State> {
    private static final RenderType CRYSTAL = RenderTileEnergyCrystal.crystalType;
    private static final RenderType RING = RenderTypes.entitySolid(Identifier.withDefaultNamespace("textures/block/gold_block.png"));
    private static final RenderType BEAM = RenderTypes.entityTranslucentEmissive(Identifier.fromNamespaceAndPath(DraconicEvolution.MODID, "textures/particle/energy_beam_wyvern.png"));
    private static final int LIGHT = 0xF000F0;
    private static final float CX = 0.5F, CY = 0.5F, CZ = 0.5F;

    private final CCModel crystal;

    public RenderTileFusionConductor(BlockEntityRendererProvider.Context context) {
        Map<String, CCModel> map = new OBJParser(Identifier.fromNamespaceAndPath(DraconicEvolution.MODID, "models/block/crystal.obj")).quads().ignoreMtl().parse();
        crystal = CCModel.combine(map.values());
    }

    public static class State extends BlockEntityRenderState {
        ExtractedModel geometry;
    }

    @Override
    public State createRenderState() { return new State(); }

    @Override
    public boolean shouldRenderOffScreen() { return true; }

    @Override
    public AABB getRenderBoundingBox(TileFusionConductor tile) {
        return new AABB(tile.getBlockPos()).inflate(DEConfig.fusionInjectorRange + 1);
    }

    @Override
    public void extractRenderState(TileFusionConductor tile, State state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTicks, cameraPosition, breakProgress);
        ExtractedModel.Capture capture = new ExtractedModel.Capture();
        float time = ClientEventHandler.elapsedTicks + partialTicks;
        float bob = (float) Math.sin(time / 20F) * 0.04F;

        // Crystal (DE energy crystal shader, Wyvern purple)
        CCRenderState ccrs = CCRenderState.instance();
        ccrs.reset();
        ccrs.brightness = 240;
        ccrs.overlay = OverlayTexture.NO_OVERLAY;
        ccrs.baseColour = 0xFFFFFFFF;
        DEShaders.ENERGY_CRYSTAL.setMipmap(0F);
        DEShaders.ENERGY_CRYSTAL.setColour(RenderTileEnergyCrystal.COLOURS[1][0], RenderTileEnergyCrystal.COLOURS[1][1], RenderTileEnergyCrystal.COLOURS[1][2]);
        Matrix4 mat = new Matrix4();
        mat.translate(CX, CY + bob, CZ);
        mat.rotate(180 * MathHelper.torad, new Vector3(1, 0, 0));
        mat.scale(-0.3);
        mat.rotate(time / 200F, new Vector3(0, 1, 0));
        ccrs.bind(CRYSTAL, capture);
        DEModelEmission.render(crystal, ccrs, mat);

        // Gold ring
        ring(capture.getBuffer(RING), tile.ringAngle(partialTicks), CY + bob);

        // Beams
        VertexConsumer beam = capture.getBuffer(BEAM);
        TileFusionCraftingCore core = tile.getCore();
        BlockPos pos = tile.getBlockPos();
        if (core != null && core.isCrafting()) {
            beam(beam, new Vector3(CX, CY + bob - 0.2, CZ), new Vector3(0.5, -0.5, 0.5), 0.09F, time);
        }
        for (BlockPos target : tile.beamTargets()) {
            beam(beam, new Vector3(CX, CY + bob, CZ), new Vector3(target.getX() - pos.getX() + 0.5, target.getY() - pos.getY() + 0.5, target.getZ() - pos.getZ() + 0.5), 0.035F, time);
        }
        state.geometry = capture.finish();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.geometry != null) state.geometry.submit(poseStack, collector);
    }

    /** Flat gold band, 24 segments, radius 0.3, tilted a little so it reads as a ring from the side. */
    private static void ring(VertexConsumer b, float angleDeg, float y) {
        int seg = 24;
        float rOut = 0.3F, rIn = 0.26F, h = 0.025F;
        float tilt = 0.25F;
        for (int i = 0; i < seg; i++) {
            double a0 = Math.toRadians(angleDeg) + i * Math.PI * 2 / seg, a1 = a0 + Math.PI * 2 / seg;
            float[] o0 = p(a0, rOut, y, tilt), o1 = p(a1, rOut, y, tilt), i0 = p(a0, rIn, y, tilt), i1 = p(a1, rIn, y, tilt);
            quad(b, o0, o1, i1, i0, h, 0, 1, 0);          // top
            quad(b, i0, i1, o1, o0, -h, 0, -1, 0);        // bottom
            side(b, o0, o1, h, (float) Math.cos(a0), (float) Math.sin(a0));
            side(b, i1, i0, h, (float) -Math.cos(a0), (float) -Math.sin(a0));
        }
    }

    private static float[] p(double a, float r, float y, float tilt) {
        float x = (float) Math.cos(a) * r, z = (float) Math.sin(a) * r;
        return new float[]{CX + x, y + z * tilt, CZ + z};
    }

    private static void quad(VertexConsumer b, float[] a, float[] c, float[] d, float[] e, float dy, float nx, float ny, float nz) {
        v(b, a[0], a[1] + dy, a[2], 0, 0, nx, ny, nz);
        v(b, c[0], c[1] + dy, c[2], 1, 0, nx, ny, nz);
        v(b, d[0], d[1] + dy, d[2], 1, 1, nx, ny, nz);
        v(b, e[0], e[1] + dy, e[2], 0, 1, nx, ny, nz);
    }

    private static void side(VertexConsumer b, float[] a, float[] c, float h, float nx, float nz) {
        v(b, a[0], a[1] + h, a[2], 0, 0, nx, 0, nz);
        v(b, a[0], a[1] - h, a[2], 0, 1, nx, 0, nz);
        v(b, c[0], c[1] - h, c[2], 1, 1, nx, 0, nz);
        v(b, c[0], c[1] + h, c[2], 1, 0, nx, 0, nz);
    }

    private static void v(VertexConsumer b, float x, float y, float z, float u, float vv, float nx, float ny, float nz) {
        b.addVertex(x, y, z).setColor(255, 255, 255, 255).setUv(u, vv).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LIGHT).setNormal(nx, ny, nz);
    }

    /** Two crossed quads from a to b with a scrolling beam texture (both sides drawn). */
    private static void beam(VertexConsumer b, Vector3 from, Vector3 to, float width, float time) {
        Vector3 dir = to.copy().subtract(from);
        double len = dir.mag();
        if (len < 0.01) return;
        dir.normalize();
        Vector3 pa = dir.copy().perpendicular().normalize().multiply(width);
        Vector3 pb = dir.copy().crossProduct(pa).normalize().multiply(width);
        float v0 = time / -15F, v1 = v0 + (float) len * 0.2F;
        for (Vector3 off : new Vector3[]{pa, pb}) {
            Vector3 a1 = from.copy().add(off), a2 = from.copy().subtract(off), b1 = to.copy().add(off), b2 = to.copy().subtract(off);
            bv(b, a1, 0, v0); bv(b, a2, 1, v0); bv(b, b2, 1, v1); bv(b, b1, 0, v1);
            bv(b, b1, 0, v1); bv(b, b2, 1, v1); bv(b, a2, 1, v0); bv(b, a1, 0, v0);
        }
    }

    private static void bv(VertexConsumer b, Vector3 p, float u, float v) {
        b.addVertex((float) p.x, (float) p.y, (float) p.z).setColor(200, 120, 255, 220).setUv(u * 0.53F, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LIGHT).setNormal(0, 1, 0);
    }
}
