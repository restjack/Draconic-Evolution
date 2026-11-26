package com.brandon3055.draconicevolution.client.render.tile;

import com.brandon3055.draconicevolution.DEConfig;
import com.brandon3055.draconicevolution.blocks.tileentity.TileStructureBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

/**
 * eli.8: multiblock "structure blocks" (an active energy core's Draconium/Redstone shell, a pylon's glass, reactor parts)
 * are invisible because DE draws its effects over them. With the client's block effects switched off nothing was left,
 * so in that mode each one draws its original block instead. With effects on this draws nothing, as before.
 */
public class RenderTileStructureBlock implements BlockEntityRenderer<TileStructureBlock, RenderTileStructureBlock.State> {
    private static final BlockDisplayContext DISPLAY = BlockDisplayContext.create();

    public RenderTileStructureBlock(BlockEntityRendererProvider.Context context) {
    }

    public static class State extends BlockEntityRenderState {
        BlockModelRenderState model;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(TileStructureBlock tile, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTicks, cameraPosition, breakProgress);
        state.model = null;
        if (DEConfig.blockEffectRendering) return;
        Block original = tile.getOriginalBlock();
        if (original.defaultBlockState().isAir()) return;
        // Resolve the model once per block and keep it on the tile; a tier 8 core has ~1,160 of these.
        // Rebuilt every 10 s as well, so a resource-pack change (e.g. switching the HD packs) is picked up.
        long now = tile.getLevel() == null ? 0 : tile.getLevel().getGameTime();
        if (tile.renderCacheFor != original || !(tile.renderCache instanceof BlockModelRenderState) || now - tile.renderCacheTime > 200 || now < tile.renderCacheTime) {
            BlockModelRenderState model = new BlockModelRenderState();
            Minecraft.getInstance().getBlockModelResolver().update(model, original.defaultBlockState(), DISPLAY);
            tile.renderCache = model;
            tile.renderCacheFor = original;
            tile.renderCacheTime = now;
        }
        state.model = (BlockModelRenderState) tile.renderCache;
    }

    /** With block effects on (the normal case) these blocks draw nothing, so skip them before any per-frame work. */
    @Override
    public boolean shouldRender(TileStructureBlock tile, Vec3 cameraPos) {
        return !DEConfig.blockEffectRendering && BlockEntityRenderer.super.shouldRender(tile, cameraPos);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.model != null) state.model.submitMultiLayer(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
    }
}
