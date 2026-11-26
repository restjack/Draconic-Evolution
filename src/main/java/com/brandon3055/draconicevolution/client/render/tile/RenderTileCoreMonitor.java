package com.brandon3055.draconicevolution.client.render.tile;

import com.brandon3055.draconicevolution.DEConfig;
import com.brandon3055.draconicevolution.blocks.machines.CoreMonitor;
import com.brandon3055.draconicevolution.blocks.tileentity.TileCoreMonitor;
import com.brandon3055.draconicevolution.client.gui.EnergyCoreGui;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * eli.8: draws a Core Monitor screen. The bottom-left panel of a group draws the whole screen (up to 3x3 blocks);
 * the other panels draw nothing. 64 canvas units per block.
 */
public class RenderTileCoreMonitor implements BlockEntityRenderer<TileCoreMonitor, RenderTileCoreMonitor.State> {
    private static final int PX = 64, FULL_BRIGHT = 0xF000F0;
    private static final int CYAN = 0xFF55FFFF, GOLD = 0xFFFFAA00, GRAY = 0xFFAAAAAA, GREEN = 0xFF55FF55, RED = 0xFFFF5555,
            YELLOW = 0xFFFFFF55, WHITE = 0xFFE8E6F0, PURPLE = 0xFF8800FF;

    public RenderTileCoreMonitor(BlockEntityRendererProvider.Context context) {
    }

    public record Line(String text, int colour, float scale) {}

    public static class State extends BlockEntityRenderState {
        boolean draw;
        Direction facing;
        int w, h;
        int frame, barColour;
        float fill = -1;
        List<Line> lines = new ArrayList<>();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public int getViewDistance() {
        return 48;
    }

    private static boolean isPanel(Level level, BlockPos pos, Direction facing) {
        var s = level.getBlockState(pos);
        return s.getBlock() instanceof CoreMonitor && s.getValue(CoreMonitor.FACING) == facing;
    }

    @Override
    public void extractRenderState(TileCoreMonitor tile, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTicks, cameraPosition, breakProgress);
        state.lines.clear();
        state.fill = -1;
        Level level = tile.getLevel();
        if (level == null) { state.draw = false; return; }
        Direction facing = tile.getBlockState().getValue(CoreMonitor.FACING);
        Direction right = facing.getCounterClockWise(); // the viewer's right when looking at the screen
        BlockPos pos = tile.getBlockPos();
        // Only the bottom-left panel draws; size = filled rectangle up to 3x3.
        if (isPanel(level, pos.relative(right.getOpposite()), facing) || isPanel(level, pos.below(), facing)) { state.draw = false; return; }
        int w = 1, h = 1;
        while (w < TileCoreMonitor.MAX_SIZE && isPanel(level, pos.relative(right, w), facing)) w++;
        while (h < TileCoreMonitor.MAX_SIZE && isPanel(level, pos.above(h), facing)) h++;
        rows:
        for (int y = 1; y < h; y++) {
            for (int x = 1; x < w; x++) {
                if (!isPanel(level, pos.relative(right, x).above(y), facing)) { h = y; break rows; }
            }
        }
        state.draw = true;
        state.facing = facing;
        state.w = w;
        state.h = h;
        state.lightCoords = FULL_BRIGHT;
        buildLines(tile, state, level);
    }

    private static void buildLines(TileCoreMonitor t, State s, Level level) {
        float big = Math.min(s.w, s.h) >= 2 ? (Math.min(s.w, s.h) >= 3 ? 4F : 3F) : 2F;
        s.frame = PURPLE;
        if (!DEConfig.extra("coreMonitor")) {
            s.lines.add(new Line("Core Monitor", CYAN, 1));
            s.lines.add(new Line("disabled by", GRAY, 1));
            s.lines.add(new Line("server config", GRAY, 1));
            return;
        }
        if (!t.isLinked()) {
            s.lines.add(new Line("NOT LINKED", RED, 1));
            s.lines.add(new Line("Shift-click core", GRAY, 1));
            s.lines.add(new Line("with Crystal Binder,", GRAY, 1));
            s.lines.add(new Line("then this screen", GRAY, 1));
            return;
        }
        String unit = DEConfig.energyUnit();
        long stored = t.stored.get(), cap = t.capacity.get(), net = t.input.get() - t.output.get();
        boolean live = t.loaded.get();
        double pct = t.unlimited.get() || cap <= 0 ? -1 : stored * 100D / cap;
        int pctColour = !live ? GRAY : pct < 0 ? GOLD : pct < 10 ? RED : pct < 25 ? YELLOW : pct >= 100 ? GREEN : GOLD;
        String word = !live ? "NOT LOADED" : !t.coreActive.get() ? "INACTIVE" : !t.stabilizersOk.get() ? "STABILIZER!" : pct >= 100 ? "FULL"
                : pct >= 0 && pct < 10 ? "LOW!" : net < 0 ? "DRAINING" : net > 0 ? "CHARGING" : "ACTIVE";
        int wordColour = !live ? GRAY : !t.coreActive.get() || !t.stabilizersOk.get() || (pct >= 0 && pct < 10) ? RED
                : net < 0 && pct < 25 ? YELLOW : GREEN;
        if (live && pct >= 0 && pct < 10) s.frame = RED; else if (live && pct >= 0 && pct < 25) s.frame = YELLOW; else if (!live) s.frame = 0xFF55555F;

        if (s.w == 1 && s.h == 1) {
            s.lines.add(new Line(word, wordColour, 1));
        } else {
            s.lines.add(new Line("TIER " + t.tier.get() + " CORE", CYAN, 1));
            s.lines.add(new Line(word, wordColour, 1));
        }
        s.lines.add(new Line(pct < 0 ? EnergyCoreGui.shortNumber(stored) : String.format(java.util.Locale.ROOT, pct >= 99.95 && pct < 100 ? "%.2f%%" : "%.1f%%", pct), pctColour, big));
        s.fill = pct < 0 ? -1 : (float) Math.min(1, pct / 100);
        s.barColour = pctColour;
        if (s.w == 1 && s.h == 1) {
            s.lines.add(new Line((net > 0 ? "+" : "") + EnergyCoreGui.shortNumber(net) + " " + unit + "/t", net > 0 ? GREEN : net < 0 ? RED : GRAY, 1));
            return;
        }
        if (pct >= 0) s.lines.add(new Line(EnergyCoreGui.shortNumber(stored) + " / " + EnergyCoreGui.shortNumber(cap) + " " + unit, GRAY, 1));
        s.lines.add(new Line("In +" + EnergyCoreGui.shortNumber(t.input.get()) + "  Out -" + EnergyCoreGui.shortNumber(t.output.get()), GRAY, 1));
        s.lines.add(new Line("Net " + (net > 0 ? "+" : "") + EnergyCoreGui.shortNumber(net) + " " + unit + "/t", net > 0 ? GREEN : net < 0 ? RED : GRAY, 1));
        if (!live) {
            long ago = Math.max(0, (level.getGameTime() - t.lastSeen.get()) / 20);
            s.lines.add(new Line(t.lastSeen.get() == 0 ? "Core not loaded" : "Last seen " + EnergyCoreGui.formatDuration(ago) + " ago", GRAY, 1));
        } else if (net > 0 && pct >= 0 && pct < 100) {
            s.lines.add(new Line("Full in " + EnergyCoreGui.formatDuration((cap - stored) / (double) net / 20D), WHITE, 1));
        } else if (net < 0 && stored > 0) {
            s.lines.add(new Line("Empty in " + EnergyCoreGui.formatDuration(stored / (double) -net / 20D), WHITE, 1));
        }
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.draw) return;
        float W = state.w * PX, H = state.h * PX;
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-state.facing.toYRot())); // local +Z = facing, +X = viewer's right
        pose.translate(-0.5, state.h - 0.5, 0.5015);
        pose.scale(1F / PX, -1F / PX, 1F / PX); // canvas: x right, y down, in 1/64 block

        quad(pose, collector, 2, 2, W - 2, H - 2, 0.02F, state.frame);
        quad(pose, collector, 4, 4, W - 4, H - 4, 0.06F, 0xFF0B0710);

        Font font = Minecraft.getInstance().font;
        float total = 0;
        for (Line l : state.lines) total += 10 * l.scale();
        if (state.fill >= 0) total += 10;
        float scaleAll = Math.min(1F, (H - 14) / Math.max(1, total)) * (state.h >= 2 ? 1.6F : 1F);
        if (total * scaleAll > H - 14) scaleAll = (H - 14) / total;
        float y = (H - total * scaleAll) / 2;
        int i = 0;
        for (Line l : state.lines) {
            float sc = l.scale() * scaleAll;
            int tw = font.width(l.text());
            if (tw * sc > W - 14) sc = (W - 14) / tw;
            pose.pushPose();
            pose.translate((W - tw * sc) / 2, y, 0.3F); // +z = toward the viewer, in front of the background
            pose.scale(sc, sc, sc);
            collector.submitText(pose, 0, 0, Component.literal(l.text()).getVisualOrderText(), false, Font.DisplayMode.POLYGON_OFFSET, l.colour(), FULL_BRIGHT, 0, 0);
            pose.popPose();
            y += 10 * l.scale() * scaleAll;
            // The fill bar sits right under the big percent line.
            if (state.fill >= 0 && l.scale() > 1 && i++ == 0) {
                float bh = 6 * scaleAll, bx = 10, bw = W - 20;
                quad(pose, collector, bx, y, bx + bw, y + bh, 0.1F, 0xFF2A1A33);
                quad(pose, collector, bx, y, bx + bw * state.fill, y + bh, 0.14F, state.barColour);
                y += 10 * scaleAll;
            }
        }
        pose.popPose();
    }

    /** A flat colour rectangle at canvas depth z (+z = toward the viewer). Both windings, so face culling can't hide it. */
    private static void quad(PoseStack pose, SubmitNodeCollector collector, float x0, float y0, float x1, float y1, float z, int colour) {
        collector.submitCustomGeometry(pose, RenderTypes.textBackground(), (p, vc) -> {
            vc.addVertex(p, x0, y0, z).setColor(colour).setLight(FULL_BRIGHT);
            vc.addVertex(p, x0, y1, z).setColor(colour).setLight(FULL_BRIGHT);
            vc.addVertex(p, x1, y1, z).setColor(colour).setLight(FULL_BRIGHT);
            vc.addVertex(p, x1, y0, z).setColor(colour).setLight(FULL_BRIGHT);
            vc.addVertex(p, x1, y0, z).setColor(colour).setLight(FULL_BRIGHT);
            vc.addVertex(p, x1, y1, z).setColor(colour).setLight(FULL_BRIGHT);
            vc.addVertex(p, x0, y1, z).setColor(colour).setLight(FULL_BRIGHT);
            vc.addVertex(p, x0, y0, z).setColor(colour).setLight(FULL_BRIGHT);
        });
    }
}
