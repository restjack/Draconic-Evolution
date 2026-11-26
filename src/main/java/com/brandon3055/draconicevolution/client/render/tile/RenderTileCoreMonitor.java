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

    public record Line(String text, int colour, float scale, boolean bar) {
        Line(String text, int colour, float scale) { this(text, colour, scale, false); }
    }

    public static class State extends BlockEntityRenderState {
        boolean draw;
        Direction facing;
        int w, h;
        int frame, barColour;
        float fill = -1;
        List<Line> lines = new ArrayList<>();
        boolean showFrame = true, renderCore;
        int background = 0xFF0B0710;
        float time;
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
        state.showFrame = tile.isShown(TileCoreMonitor.SHOW_FRAME);
        state.background = (new int[]{0xFF, 0xBF, 0x80}[Math.max(0, Math.min(2, tile.bgAlpha.get()))] << 24) | 0x0B0710;
        state.time = level.getGameTime() + partialTicks;
        buildLines(tile, state, level);
        state.renderCore = w >= 2 && h >= 2 && tile.isShown(TileCoreMonitor.SHOW_RENDER) && tile.isLinked() && tile.loaded.get()
                && DEConfig.extra("coreMonitor");
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
        String unit = t.unitName();
        long stored = t.stored.get(), cap = t.capacity.get(), net = t.input.get() - t.output.get();
        boolean live = t.loaded.get();
        double pct = t.fillPercent();
        int warn = t.warnAt.get(), alarm = t.alarmAt.get();
        int pctColour = !live ? GRAY : pct < 0 ? GOLD : pct < alarm ? RED : pct < warn ? YELLOW : pct >= 100 ? GREEN : GOLD;
        String word = !live ? "NOT LOADED" : !t.coreActive.get() ? "INACTIVE" : !t.stabilizersOk.get() ? "STABILIZER!" : pct >= 100 ? "FULL"
                : pct >= 0 && pct < alarm ? "LOW!" : net < 0 ? "DRAINING" : net > 0 ? "CHARGING" : "ACTIVE";
        int wordColour = !live ? GRAY : !t.coreActive.get() || !t.stabilizersOk.get() || (pct >= 0 && pct < alarm) ? RED
                : net < 0 && pct < warn ? YELLOW : GREEN;
        if (live && pct >= 0 && pct < alarm) s.frame = RED; else if (live && pct >= 0 && pct < warn) s.frame = YELLOW; else if (!live) s.frame = 0xFF55555F;

        boolean small = s.w == 1 && s.h == 1;
        if (!small && t.isShown(TileCoreMonitor.SHOW_HEADER)) s.lines.add(new Line(t.name.get().isEmpty() ? "TIER " + t.tier.get() + " CORE"
                : t.name.get() + " (T" + t.tier.get() + ")", CYAN, 1));
        if (t.isShown(TileCoreMonitor.SHOW_STATUS)) s.lines.add(new Line(word, wordColour, 1));
        if (t.isShown(TileCoreMonitor.SHOW_PERCENT)) s.lines.add(new Line(pct < 0 ? EnergyCoreGui.shortNumber(t.toUnit(stored))
                : String.format(java.util.Locale.ROOT, pct >= 99.95 && pct < 100 ? "%.2f%%" : "%.1f%%", pct), pctColour, big));
        s.fill = pct < 0 ? -1 : (float) Math.min(1, pct / 100);
        s.barColour = pctColour;
        if (s.fill >= 0 && t.isShown(TileCoreMonitor.SHOW_BAR)) s.lines.add(new Line("", 0, 1, true));
        String netText = (net > 0 ? "+" : "") + EnergyCoreGui.shortNumber(t.toUnit(net)) + " " + unit + "/t";
        int netColour = net > 0 ? GREEN : net < 0 ? RED : GRAY;
        if (small) {
            if (t.isShown(TileCoreMonitor.SHOW_NET)) s.lines.add(new Line(netText, netColour, 1));
            if (s.lines.isEmpty()) s.lines.add(new Line(word, wordColour, 1)); // never a blank screen
            return;
        }
        if (pct >= 0 && t.isShown(TileCoreMonitor.SHOW_STORED)) s.lines.add(new Line(EnergyCoreGui.shortNumber(t.toUnit(stored)) + " / " + EnergyCoreGui.shortNumber(t.toUnit(cap)) + " " + unit, GRAY, 1));
        if (t.isShown(TileCoreMonitor.SHOW_INOUT)) s.lines.add(new Line("In +" + EnergyCoreGui.shortNumber(t.toUnit(t.input.get())) + "  Out -" + EnergyCoreGui.shortNumber(t.toUnit(t.output.get())), GRAY, 1));
        if (t.isShown(TileCoreMonitor.SHOW_NET)) s.lines.add(new Line("Net " + netText, netColour, 1));
        if (!t.isShown(TileCoreMonitor.SHOW_ETA)) {
            if (s.lines.isEmpty()) s.lines.add(new Line(word, wordColour, 1));
        } else if (!live) {
            long ago = Math.max(0, (level.getGameTime() - t.lastSeen.get()) / 20);
            s.lines.add(new Line(t.lastSeen.get() == 0 ? "Core not loaded" : "Last seen " + EnergyCoreGui.formatDuration(ago) + " ago", GRAY, 1));
        } else if (t.target.get() > 0 && pct >= 0 && ((net > 0 && pct < t.target.get()) || (net < 0 && pct > t.target.get()))) {
            double toGo = Math.abs(cap * (t.target.get() / 100D) - stored);
            s.lines.add(new Line(t.target.get() + "% in " + EnergyCoreGui.formatDuration(toGo / Math.abs(net) / 20D), WHITE, 1));
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

        if (state.showFrame) quad(pose, collector, 2, 2, W - 2, H - 2, 0.02F, state.frame);
        quad(pose, collector, 4, 4, W - 4, H - 4, 0.06F, state.background);
        if (state.renderCore) {
            if (state.w >= 3 && state.h >= 3) fakeCore(pose, collector, W / 2, H / 2, H * 0.3F, state, 0x40); // faint, behind the text
            else fakeCore(pose, collector, W - 16, 16, 9, state, 0xFF); // small, top-right corner
        }

        Font font = Minecraft.getInstance().font;
        float total = 0;
        for (Line l : state.lines) total += 10 * l.scale();
        float scaleAll = Math.min(1F, (H - 14) / Math.max(1, total)) * (state.h >= 2 ? 1.6F : 1F);
        if (total * scaleAll > H - 14) scaleAll = (H - 14) / total;
        float y = (H - total * scaleAll) / 2;
        for (Line l : state.lines) {
            if (l.bar()) {
                float bh = 6 * scaleAll, bx = 10, bw = W - 20;
                quad(pose, collector, bx, y, bx + bw, y + bh, 0.1F, 0xFF2A1A33);
                quad(pose, collector, bx, y, bx + bw * state.fill, y + bh, 0.14F, state.barColour);
                y += 10 * scaleAll;
                continue;
            }
            float sc = l.scale() * scaleAll;
            int tw = font.width(l.text());
            if (tw * sc > W - 14) sc = (W - 14) / tw;
            pose.pushPose();
            pose.translate((W - tw * sc) / 2, y, 0.3F); // +z = toward the viewer, in front of the background
            pose.scale(sc, sc, sc);
            collector.submitText(pose, 0, 0, Component.literal(l.text()).getVisualOrderText(), false, Font.DisplayMode.POLYGON_OFFSET, l.colour(), FULL_BRIGHT, 0, 0);
            pose.popPose();
            y += 10 * l.scale() * scaleAll;
        }
        pose.popPose();
    }

    /**
     * A flat sketch of the core (Round C): rotating shell outline, centre in the fill colour, four stabilizer orbs with
     * beams. alpha 0x40 for the faint full-size backdrop, 0xFF for the small corner icon.
     */
    private static void fakeCore(PoseStack pose, SubmitNodeCollector c, float cx, float cy, float r, State s, int alpha) {
        int a = alpha << 24;
        float ang = (float) Math.toRadians(s.time * 1.5F);
        float lw = Math.max(0.8F, r / 10);
        int beam = a | 0x00F2F2, orb = (alpha == 0xFF ? 0xFF000000 : a) | 0x55FFFF;
        float or = r * 1.45F;
        float[][] dirs = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
        for (float[] d : dirs) line(pose, c, cx, cy, cx + d[0] * or, cy + d[1] * or, lw * 0.6F, 0.1F, beam);
        float[] px = new float[4], py = new float[4];
        for (int i = 0; i < 4; i++) {
            double t = ang + i * Math.PI / 2;
            px[i] = cx + (float) Math.cos(t) * r;
            py[i] = cy + (float) Math.sin(t) * r;
        }
        int shell = a | 0x8800FF;
        for (int i = 0; i < 4; i++) line(pose, c, px[i], py[i], px[(i + 1) % 4], py[(i + 1) % 4], lw, 0.12F, shell);
        float ir = r * 0.5F;
        int fillColour = (s.fill < 0 ? 0xFFAA00 : s.barColour & 0xFFFFFF) | a;
        quadPts(pose, c, cx, cy - ir, cx + ir, cy, cx, cy + ir, cx - ir, cy, 0.14F, fillColour);
        float os = Math.max(1.2F, r / 6);
        for (float[] d : dirs) {
            float x = cx + d[0] * or, y = cy + d[1] * or;
            quad(pose, c, x - os, y - os, x + os, y + os, 0.16F, orb);
        }
    }

    private static void line(PoseStack pose, SubmitNodeCollector c, float x0, float y0, float x1, float y1, float width, float z, int colour) {
        float dx = x1 - x0, dy = y1 - y0, len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1.0E-4F) return;
        float nx = -dy / len * width / 2, ny = dx / len * width / 2;
        quadPts(pose, c, x0 + nx, y0 + ny, x1 + nx, y1 + ny, x1 - nx, y1 - ny, x0 - nx, y0 - ny, z, colour);
    }

    /** Any four-corner shape, both windings. */
    private static void quadPts(PoseStack pose, SubmitNodeCollector collector, float ax, float ay, float bx, float by, float cx, float cy,
                                float dx, float dy, float z, int colour) {
        collector.submitCustomGeometry(pose, RenderTypes.textBackground(), (p, vc) -> {
            vc.addVertex(p, ax, ay, z).setColor(colour).setLight(FULL_BRIGHT);
            vc.addVertex(p, bx, by, z).setColor(colour).setLight(FULL_BRIGHT);
            vc.addVertex(p, cx, cy, z).setColor(colour).setLight(FULL_BRIGHT);
            vc.addVertex(p, dx, dy, z).setColor(colour).setLight(FULL_BRIGHT);
            vc.addVertex(p, dx, dy, z).setColor(colour).setLight(FULL_BRIGHT);
            vc.addVertex(p, cx, cy, z).setColor(colour).setLight(FULL_BRIGHT);
            vc.addVertex(p, bx, by, z).setColor(colour).setLight(FULL_BRIGHT);
            vc.addVertex(p, ax, ay, z).setColor(colour).setLight(FULL_BRIGHT);
        });
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
