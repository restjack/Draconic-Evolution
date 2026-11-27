package com.brandon3055.draconicevolution.client.render.tile;

import com.brandon3055.draconicevolution.blocks.tileentity.TileDislocationInhibitor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

/**
 * The block itself is a normal model; this only draws the field outline while "Show area" is on (copy of the Evo Tweaks
 * Field Crystal box, same teal). Renders off screen (global list): blocks placed before unofficial-1.30 had no tile, so
 * the client makes one only when the screen opens, after the chunk section was built without it, and a section-listed
 * renderer then never runs (the 1.30-1.33 "Show area does nothing" bug). The global list is filled on add, so it also
 * keeps the box visible while the block's own section is culled.
 */
public class RenderTileDislocationInhibitor implements BlockEntityRenderer<TileDislocationInhibitor, RenderTileDislocationInhibitor.State> {

    public RenderTileDislocationInhibitor(BlockEntityRendererProvider.Context context) {}

    public static class State extends BlockEntityRenderState {
        AABB box;
        int colour;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(TileDislocationInhibitor tile, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTicks, cameraPosition, breakProgress);
        int[] b = tile.box();
        state.box = tile.showArea.get() ? new AABB(b[0], b[1], b[2], b[3] + 1, b[4] + 1, b[5] + 1).deflate(0.01) : null;
        state.colour = tile.active() ? 0xFF2FD6C8 : 0xFF6A6A6A; // Field Crystal teal, grey while redstone holds it off
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        AABB box = state.box;
        if (box == null) return;
        int colour = state.colour;
        collector.submitCustomGeometry(poseStack, RenderTypes.lines(), (pose, vc) ->
                Shapes.create(box).forAllEdges((x1, y1, z1, x2, y2, z2) ->
                        line(vc, pose, (float) x1, (float) y1, (float) z1, (float) x2, (float) y2, (float) z2, colour)));
    }

    private static void line(VertexConsumer vc, PoseStack.Pose pose, float x1, float y1, float z1, float x2, float y2, float z2, int argb) {
        float nx = x2 - x1, ny = y2 - y1, nz = z2 - z1, len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1e-4F) return;
        nx /= len; ny /= len; nz /= len;
        vc.addVertex(pose, x1, y1, z1).setColor(argb).setNormal(pose, nx, ny, nz).setLineWidth(2.5F);
        vc.addVertex(pose, x2, y2, z2).setColor(argb).setNormal(pose, nx, ny, nz).setLineWidth(2.5F);
    }

    @Override
    public AABB getRenderBoundingBox(TileDislocationInhibitor tile) {
        AABB own = new AABB(tile.getBlockPos());
        return tile.showArea.get() ? own.minmax(tile.area()) : own;
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
