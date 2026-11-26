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
import java.util.Locale;

/**
 * eli.8 Core Monitor screen, laid out like the mockups (artifact EH52817rQQMr5U8Gxmf923, boards Main and Render).
 * <p>
 * The bottom-left panel draws the whole screen (up to 3x3). Coordinates are "mockup pixels": 180 per block, so a 1x1 is
 * 180x180, a 3x3 540x540, and font sizes are the mockup's CSS sizes. Extract builds a list of draw ops (needs the tile
 * and the font); submit replays them. One bezel is drawn over the whole merged face, so panel seams don't show.
 */
public class RenderTileCoreMonitor implements BlockEntityRenderer<TileCoreMonitor, RenderTileCoreMonitor.State> {
    private static final float B = 180F;
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final int CYAN = 0xFF55FFFF, GOLD = 0xFFFFAA00, GRAY = 0xFFAAAAAA, GREEN = 0xFF55FF55, RED = 0xFFFF5555,
            YELLOW = 0xFFFFFF55, WHITE = 0xFFE8E6F0, PURPLE = 0xFF8800FF, LILAC = 0xFFD9B3FF;
    // Depth layers in canvas units (1/180 block) toward the viewer.
    private static final float Z_BEZEL = 0.4F, Z_SCREEN = 0.8F, Z_BOX = 1.2F, Z_FILL = 1.6F, Z_LINE = 2.0F, Z_CORE = 2.4F, Z_TEXT = 4.0F;
    private static final String[] RANGE_NAMES = {"5 min", "hour", "24 h"}, RANGE_CHIPS = {"5 MIN", "1 H", "24 H"};

    public RenderTileCoreMonitor(BlockEntityRendererProvider.Context context) {
    }

    // ### Draw ops

    private record Rect(float x0, float y0, float x1, float y1, int colour, float z) {}

    private record Txt(String text, float x, float y, float size, int colour) {}

    private record CoreOp(float cx, float cy, float k) {}

    private record GraphOp(float x0, float y0, float x1, float y1, long[] data, boolean line) {}

    public static class State extends BlockEntityRenderState {
        boolean draw;
        Direction facing;
        int w, h;
        float time;
        final List<Object> ops = new ArrayList<>();
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
        state.ops.clear();
        Level level = tile.getLevel();
        if (level == null) { state.draw = false; return; }
        Direction facing = tile.getBlockState().getValue(CoreMonitor.FACING);
        Direction right = facing.getCounterClockWise(); // the viewer's right when looking at the screen
        BlockPos pos = tile.getBlockPos();
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
        state.time = level.getGameTime() + partialTicks;
        state.lightCoords = FULL_BRIGHT;
        new Layout(tile, state, level).build();
    }

    // ### Layout

    /** Builds the op list for one screen. All sizes in mockup pixels. */
    private static class Layout {
        final TileCoreMonitor t;
        final State s;
        final Level level;
        final List<Object> ops;
        final Font font = Minecraft.getInstance().font;
        final float W, H;
        final int m;
        float x0, y0, x1, y1; // content box
        String unit, word, title, eta = "", targetEta = "";
        long stored, cap, in, out, net;
        double pct;
        boolean live;
        int pctColour, wordColour, frame;

        Layout(TileCoreMonitor t, State s, Level level) {
            this.t = t;
            this.s = s;
            this.level = level;
            this.ops = s.ops;
            W = s.w * B;
            H = s.h * B;
            m = Math.min(s.w, s.h);
        }

        // --- primitives ---

        void rect(float a, float b, float c, float d, int colour, float z) {
            ops.add(new Rect(a, b, c, d, colour, z));
        }

        float width(String text, float size) {
            return font.width(text) * size / 12F;
        }

        /** Shrinks the font so the text fits maxW. */
        float fit(String text, float size, float maxW) {
            float wd = width(text, size);
            return wd > maxW && wd > 0 ? size * maxW / wd : size;
        }

        /** Left-aligned text whose line box (height = size) starts at y. Returns the x after it. */
        float text(String text, float x, float y, float size, int colour) {
            ops.add(new Txt(text, x, y, size, colour));
            return x + width(text, size);
        }

        void textRight(String text, float xr, float y, float size, int colour) {
            text(text, xr - width(text, size), y, size, colour);
        }

        void textCentre(String text, float cx, float y, float size, int colour) {
            text(text, cx - width(text, size) / 2, y, size, colour);
        }

        /** Several coloured pieces on one line (text, colour, text, colour...), shrunk together to fit maxW. */
        void pieces(float x, float y, float size, float maxW, Object... textColour) {
            StringBuilder all = new StringBuilder();
            for (int i = 0; i < textColour.length; i += 2) all.append(textColour[i]);
            float sz = fit(all.toString(), size, maxW);
            for (int i = 0; i < textColour.length; i += 2) x = text((String) textColour[i], x, y + (size - sz) / 2, sz, (Integer) textColour[i + 1]);
        }

        void bar(float a, float b, float c, float d) {
            rect(a, b, c, d, 0xFF6A3A90, Z_BOX);
            rect(a + 1, b + 1, c - 1, d - 1, 0xFF2A1A33, Z_FILL);
            if (pct > 0) rect(a + 1, b + 1, a + 1 + (c - a - 2) * (float) Math.min(1, pct / 100), d - 1, pctColour, Z_LINE);
        }

        String num(long op) {
            return EnergyCoreGui.shortNumber(t.toUnit(op));
        }

        String signed(long op) {
            return (op > 0 ? "+" : "") + num(op);
        }

        int signColour(long v) {
            return v > 0 ? GREEN : v < 0 ? RED : GRAY;
        }

        String percent() {
            if (pct < 0) return num(stored);
            return String.format(Locale.ROOT, pct >= 99.95 && pct < 100 ? "%.2f%%" : "%.1f%%", pct);
        }

        boolean show(int flag) {
            return t.isShown(flag);
        }

        int range() {
            return Math.max(0, Math.min(2, t.screenRange.get()));
        }

        long max(long[] d) {
            long mx = 0;
            for (long v : d) mx = Math.max(mx, Math.abs(v));
            return mx;
        }

        // --- build ---

        void build() {
            float pad = m >= 3 ? 16 : m == 2 ? 14 : 12;
            bezel();
            readData();
            int bg = (new int[]{0xFF, 0xBF, 0x80}[Math.max(0, Math.min(2, t.bgAlpha.get()))] << 24) | 0x0B0710;
            if (show(TileCoreMonitor.SHOW_FRAME)) {
                rect(pad, pad, W - pad, H - pad, frame, Z_SCREEN);
                rect(pad + 2, pad + 2, W - pad - 2, H - pad - 2, bg, Z_SCREEN + 0.2F);
            } else {
                rect(pad, pad, W - pad, H - pad, bg, Z_SCREEN);
            }
            float px = m >= 3 ? 26 : m == 2 ? 20 : 10, py = m >= 3 ? 22 : m == 2 ? 18 : 10;
            x0 = pad + 2 + px;
            y0 = pad + 2 + py;
            x1 = W - pad - 2 - px;
            y1 = H - pad - 2 - py;

            if (!DEConfig.extra("coreMonitor")) { message(new String[]{"Core Monitor", "disabled by", "server config"}, new int[]{CYAN, GRAY, GRAY}); return; }
            if (!t.isLinked()) {
                message(new String[]{"NOT LINKED", "Shift-click core", "with Crystal Binder,", "then this screen"}, new int[]{RED, GRAY, GRAY, GRAY});
                return;
            }
            if (m >= 3 && show(TileCoreMonitor.SHOW_RENDER)) renderBoard();
            else if (m >= 3) large();
            else if (m == 2) medium();
            else if (s.h == 1 && s.w >= 2) wide();
            else small();
        }

        /** Grey block bezel with a light top-left and dark bottom-right edge, over the whole merged face. */
        void bezel() {
            rect(0, 0, W, H, 0xFF55555F, Z_BEZEL - 0.2F);
            rect(3, 3, W - 3, H - 3, 0xFF3A3A44, Z_BEZEL);
            rect(3, 3, W - 3, 6, 0xFF6C6C78, Z_BEZEL + 0.1F);
            rect(3, 6, 6, H - 3, 0xFF6C6C78, Z_BEZEL + 0.1F);
            rect(6, H - 6, W - 3, H - 3, 0xFF26262E, Z_BEZEL + 0.1F);
            rect(W - 6, 6, W - 3, H - 6, 0xFF26262E, Z_BEZEL + 0.1F);
        }

        void readData() {
            unit = t.unitName();
            stored = t.stored.get();
            cap = t.capacity.get();
            in = t.input.get();
            out = t.output.get();
            net = in - out;
            live = t.loaded.get();
            pct = t.fillPercent();
            int warn = t.warnAt.get(), alarm = t.alarmAt.get();
            boolean alarmNow = live && pct >= 0 && pct < alarm;
            pctColour = !live ? GRAY : pct < 0 ? GOLD : alarmNow ? RED : pct < warn ? YELLOW : pct >= 100 ? GREEN : GOLD;
            word = !live ? "NOT LOADED" : !t.coreActive.get() ? "INACTIVE" : !t.stabilizersOk.get() ? "STABILIZER!" : pct >= 100 ? "FULL"
                    : alarmNow ? "LOW!" : net < 0 ? "DRAINING" : net > 0 ? "CHARGING" : "ACTIVE";
            wordColour = !live ? GRAY : !t.coreActive.get() || !t.stabilizersOk.get() || alarmNow ? RED : net < 0 && pct < warn ? YELLOW : GREEN;
            boolean flash = ((long) (s.time / 10)) % 2 == 0;
            frame = !live ? 0xFF55555F : alarmNow ? (flash ? RED : 0xFF7A2020) : pct >= 0 && pct < warn ? YELLOW : PURPLE;
            title = t.name.get().isEmpty() ? "TIER " + t.tier.get() + (m >= 3 ? " ENERGY CORE" : " CORE") : t.name.get().toUpperCase(Locale.ROOT);
            if (!live) {
                long ago = Math.max(0, (level.getGameTime() - t.lastSeen.get()) / 20);
                eta = t.lastSeen.get() == 0 ? "Core not loaded" : "Last seen " + EnergyCoreGui.formatDuration(ago) + " ago";
            } else if (net > 0 && pct >= 0 && pct < 100) {
                eta = "Full in " + EnergyCoreGui.formatDuration((cap - stored) / (double) net / 20D);
            } else if (net < 0 && stored > 0) {
                eta = "Empty in " + EnergyCoreGui.formatDuration(stored / (double) -net / 20D);
            }
            int tg = t.target.get();
            if (live && tg > 0 && pct >= 0 && ((net > 0 && pct < tg) || (net < 0 && pct > tg))) {
                targetEta = tg + "% in " + EnergyCoreGui.formatDuration(Math.abs(cap * (tg / 100D) - stored) / Math.abs(net) / 20D);
            }
        }

        void message(String[] lines, int[] colours) {
            float size = m >= 3 ? 30 : m == 2 ? 26 : 20, gap = size * 0.35F;
            float total = lines.length * size + (lines.length - 1) * gap, y = (y0 + y1 - total) / 2;
            for (int i = 0; i < lines.length; i++) {
                float sz = fit(lines[i], size, x1 - x0);
                textCentre(lines[i], (x0 + x1) / 2, y + (size - sz) / 2, sz, colours[i]);
                y += size + gap;
            }
        }

        void header(float y, float size, String left, String right, int leftColour) {
            float mw = x1 - x0;
            float rw = right.isEmpty() ? 0 : Math.min(width(right, size), mw * 0.6F);
            if (!right.isEmpty()) { float sz = fit(right, size, mw * 0.6F); textRight(right, x1, y + (size - sz) / 2, sz, wordColour); }
            if (!left.isEmpty()) { float sz = fit(left, size, mw - rw - 10); text(left, x0, y + (size - sz) / 2, sz, leftColour); }
        }

        /** 1x1 (and 1x2/1x3): title, big percent, bar, net; taller ones add stored, time and the core. */
        void small() {
            List<Runnable> rows = new ArrayList<>();
            List<Float> heights = new ArrayList<>();
            float cx = (x0 + x1) / 2, mw = x1 - x0;
            float[] y = new float[1];
            boolean bad = !word.equals("ACTIVE") && !word.equals("CHARGING") && !word.equals("DRAINING");
            boolean badShown = bad && show(TileCoreMonitor.SHOW_STATUS);
            String head = badShown ? word : t.name.get().isEmpty() ? "CORE" : t.name.get().toUpperCase(Locale.ROOT);
            int headColour = badShown ? wordColour : CYAN;
            if (show(TileCoreMonitor.SHOW_HEADER) || badShown) { heights.add(20F); rows.add(() -> textCentre(head, cx, y[0], fit(head, 20, mw), headColour)); }
            if (show(TileCoreMonitor.SHOW_PERCENT)) {
                heights.add(46F);
                rows.add(() -> { String p = percent(); float sz = fit(p, 52, mw); textCentre(p, cx, y[0] + (46 - sz) / 2, sz, pctColour); });
            }
            if (show(TileCoreMonitor.SHOW_BAR) && pct >= 0) { heights.add(10F); rows.add(() -> bar(x0, y[0], x1, y[0] + 10)); }
            if (s.h >= 2 && show(TileCoreMonitor.SHOW_STORED) && pct >= 0) {
                heights.add(20F);
                rows.add(() -> { String v = num(stored) + "/" + num(cap); textCentre(v, cx, y[0], fit(v, 20, mw), GOLD); });
            }
            if (show(TileCoreMonitor.SHOW_NET)) {
                heights.add(20F);
                rows.add(() -> { String v = signed(net) + " " + unit + "/t"; textCentre(v, cx, y[0], fit(v, 20, mw), signColour(net)); });
            }
            if (s.h >= 2 && show(TileCoreMonitor.SHOW_ETA) && !eta.isEmpty()) { heights.add(20F); rows.add(() -> textCentre(eta, cx, y[0], fit(eta, 20, mw), WHITE)); }
            if (s.h >= 2 && show(TileCoreMonitor.SHOW_RENDER)) { heights.add(64F); rows.add(() -> ops.add(new CoreOp(cx, y[0] + 32, 0.22F))); }
            if (rows.isEmpty()) { heights.add(20F); rows.add(() -> textCentre(word, cx, y[0], fit(word, 20, mw), wordColour)); }
            spread(rows, heights, y, s.h == 1);
        }

        /** Places rows top to bottom; spaceBetween spreads them like the 1x1 mockup, else gap 8, centred. */
        void spread(List<Runnable> rows, List<Float> heights, float[] y, boolean spaceBetween) {
            float total = 0;
            for (float hgt : heights) total += hgt;
            float avail = y1 - y0;
            int n = rows.size();
            float gap = n > 1 ? (spaceBetween ? Math.max(4, (avail - total) / (n - 1)) : 8) : 0;
            y[0] = n == 1 ? y0 + (avail - total) / 2 : spaceBetween ? y0 : y0 + Math.max(0, (avail - total - gap * (n - 1)) / 2);
            for (int i = 0; i < n; i++) {
                if (y[0] + heights.get(i) > y1 + 1) break;
                rows.get(i).run();
                y[0] += heights.get(i) + gap;
            }
        }

        /** 2x1 / 3x1: header row, big percent with stored and net beside it, bar. */
        void wide() {
            float y = y0, mw = x1 - x0;
            if (show(TileCoreMonitor.SHOW_HEADER) || show(TileCoreMonitor.SHOW_STATUS)) {
                header(y, 20, show(TileCoreMonitor.SHOW_HEADER) ? title : "", show(TileCoreMonitor.SHOW_STATUS) ? word : "", CYAN);
                y += 28;
            }
            float barH = show(TileCoreMonitor.SHOW_BAR) && pct >= 0 ? 10 : 0;
            float rowH = y1 - y - (barH > 0 ? barH + 8 : 0);
            float bx = x0;
            if (show(TileCoreMonitor.SHOW_PERCENT)) {
                String p = percent();
                float sz = fit(p, Math.min(52, rowH), mw * 0.5F);
                text(p, x0, y + (rowH - sz) / 2, sz, pctColour);
                bx = x0 + width(p, sz) + 14;
            }
            float rw = x1 - bx, ly = y + rowH / 2 - 22;
            if (show(TileCoreMonitor.SHOW_STORED) && pct >= 0) pieces(bx, ly, 20, rw, num(stored), GOLD, " / " + num(cap) + " " + unit, GRAY);
            if (show(TileCoreMonitor.SHOW_NET)) pieces(bx, ly + 24, 20, rw, signed(net), signColour(net), " " + unit + "/t", GRAY);
            if (barH > 0) bar(x0, y1 - barH, x1, y1);
        }

        /** 2x2 (and 3x2/2x3): mockup "2x2 - all lines, no graph"; a small core next to the percent if enabled.
         * 3x2 puts the graph in a column on the right; 2x3 puts it under the lines. */
        void medium() {
            boolean sideGraph = s.w >= 3 && show(TileCoreMonitor.SHOW_GRAPH);
            float fullX1 = x1, y = y0, gap = 8;
            if (sideGraph) {
                float split = x0 + (x1 - x0) * 0.55F;
                long[] data = t.statSeries[range()];
                float gy = y0 + (show(TileCoreMonitor.SHOW_HEADER) || show(TileCoreMonitor.SHOW_STATUS) ? 24 + gap : 0);
                text("Net " + unit + "/t \u00B7 " + RANGE_NAMES[range()], split + 18, gy, fit("Net " + unit + "/t \u00B7 " + RANGE_NAMES[range()], 20, x1 - split - 18), CYAN);
                ops.add(new GraphOp(split + 18, gy + 24, x1, y1 - 24, data, false));
                textRight("\u00B1" + num(max(data)), x1, y1 - 20, 20, GRAY);
            }
            if (show(TileCoreMonitor.SHOW_HEADER) || show(TileCoreMonitor.SHOW_STATUS)) {
                header(y, 24, show(TileCoreMonitor.SHOW_HEADER) ? title : "", show(TileCoreMonitor.SHOW_STATUS) ? word : "", CYAN);
                y += 24 + gap;
            }
            if (sideGraph) x1 = x0 + (x1 - x0) * 0.55F;
            float mw = x1 - x0;
            if (show(TileCoreMonitor.SHOW_HEADER) || show(TileCoreMonitor.SHOW_STATUS)) {
                header(y, 24, show(TileCoreMonitor.SHOW_HEADER) ? title : "", show(TileCoreMonitor.SHOW_STATUS) ? word : "", CYAN);
                y += 24 + gap;
            }
            boolean core = show(TileCoreMonitor.SHOW_RENDER);
            if (show(TileCoreMonitor.SHOW_PERCENT) || core) {
                float rowH = 64;
                if (show(TileCoreMonitor.SHOW_PERCENT)) {
                    String p = percent();
                    float sz = fit(p, 72, mw - (core ? 80 : 0));
                    text(p, x0, y + (rowH - sz) / 2, sz, pctColour);
                }
                if (core) ops.add(new CoreOp(x1 - 34, y + rowH / 2, 0.21F));
                y += rowH + gap;
            }
            if (show(TileCoreMonitor.SHOW_BAR) && pct >= 0) { bar(x0, y, x1, y + 14); y += 14 + gap; }
            List<Object[]> lines = new ArrayList<>();
            if (show(TileCoreMonitor.SHOW_STORED) && pct >= 0) lines.add(new Object[]{num(stored), GOLD, " / " + num(cap) + " " + unit, GRAY});
            if (show(TileCoreMonitor.SHOW_INOUT)) lines.add(new Object[]{"+" + num(in), GREEN, " / ", GRAY, "-" + num(out), RED, " " + unit + "/t", GRAY});
            if (show(TileCoreMonitor.SHOW_NET)) lines.add(new Object[]{"Net: ", GRAY, signed(net) + " " + unit + "/t", signColour(net)});
            if (show(TileCoreMonitor.SHOW_ETA) && !eta.isEmpty()) lines.add(new Object[]{eta, WHITE});
            if (show(TileCoreMonitor.SHOW_ETA) && !targetEta.isEmpty()) lines.add(new Object[]{targetEta, YELLOW});
            for (Object[] l : lines) {
                if (y + 26 > y1 + 1) break;
                pieces(x0, y, 26, mw, l);
                y += 26 + gap;
            }
            x1 = fullX1;
            if (!sideGraph && s.h >= 3 && show(TileCoreMonitor.SHOW_GRAPH) && y1 - y > 70) {
                long[] data = t.statSeries[range()];
                text("Net " + unit + "/t \u00B7 last " + RANGE_NAMES[range()], x0, y, 22, CYAN);
                ops.add(new GraphOp(x0, y + 26, x1, y1, data, false));
            }
        }

        /** 3x3 without the core picture: mockup "3x3 - all lines + 5-minute graph". */
        void large() {
            float y = y0, mw = x1 - x0, gap = 10;
            if (show(TileCoreMonitor.SHOW_HEADER) || show(TileCoreMonitor.SHOW_STATUS)) {
                header(y, 28, show(TileCoreMonitor.SHOW_HEADER) ? title : "", show(TileCoreMonitor.SHOW_STATUS) ? word : "", CYAN);
                y += 28 + gap;
            }
            if (show(TileCoreMonitor.SHOW_PERCENT)) {
                String p = percent();
                float sz = fit(p, 96, mw * 0.62F), rowH = sz * 0.85F;
                text(p, x0, y - sz * 0.08F, sz, pctColour);
                if (show(TileCoreMonitor.SHOW_STORED) && pct >= 0) {
                    float sx = x0 + width(p, sz) + 22, sw = x1 - sx;
                    String a = num(stored), b = "of " + num(cap) + " " + unit;
                    text(a, sx, y + rowH - 58, fit(a, 28, sw), GOLD);
                    text(b, sx, y + rowH - 30, fit(b, 28, sw), GRAY);
                }
                y += rowH + gap;
            } else if (show(TileCoreMonitor.SHOW_STORED) && pct >= 0) {
                pieces(x0, y, 28, mw, num(stored), GOLD, " / " + num(cap) + " " + unit, GRAY);
                y += 28 + gap;
            }
            if (show(TileCoreMonitor.SHOW_BAR) && pct >= 0) { bar(x0, y, x1, y + 18); y += 18 + gap; }
            if (show(TileCoreMonitor.SHOW_INOUT) || show(TileCoreMonitor.SHOW_NET)) {
                float cw = (mw - 20) / 3;
                String[] labels = {"IN", "OUT", "NET " + unit + "/t"};
                String[] values = {"+" + num(in), "-" + num(out), signed(net)};
                int[] colours = {GREEN, RED, signColour(net)};
                for (int i = 0; i < 3; i++) {
                    if ((i < 2 && !show(TileCoreMonitor.SHOW_INOUT)) || (i == 2 && !show(TileCoreMonitor.SHOW_NET))) continue;
                    float cx = x0 + i * (cw + 10);
                    text(labels[i], cx, y, fit(labels[i], 26, cw), GRAY);
                    text(values[i], cx, y + 26, fit(values[i], 26, cw), colours[i]);
                }
                y += 52 + gap;
            }
            if (show(TileCoreMonitor.SHOW_STATS)) y = cards(y, 22, gap);
            boolean footer = show(TileCoreMonitor.SHOW_ETA) && !(eta.isEmpty() && targetEta.isEmpty());
            float footY = y1 - 26;
            long[] data = t.statSeries[range()];
            if (show(TileCoreMonitor.SHOW_GRAPH) && (footer ? footY - gap : y1) - y > 50) {
                text("Net " + unit + "/t · last " + RANGE_NAMES[range()], x0, y, 22, CYAN);
                ops.add(new GraphOp(x0, y + 26, x1, footer ? footY - gap : y1, data, false));
            }
            if (footer) {
                String f = eta.isEmpty() ? targetEta : eta;
                text(f, x0, footY, fit(f, 26, mw * 0.65F), eta.isEmpty() ? YELLOW : WHITE);
                if (show(TileCoreMonitor.SHOW_GRAPH)) textRight("±" + num(max(data)), x1, footY, 26, GRAY);
            }
        }

        /** 3x3 with the core picture: mockup board "Render". */
        void renderBoard() {
            float y = y0, gap = 10;
            if (show(TileCoreMonitor.SHOW_HEADER) || show(TileCoreMonitor.SHOW_STATUS)) {
                String right = show(TileCoreMonitor.SHOW_STATUS) ? word + (live ? " · " + (t.stabilizersOk.get() ? "STABILIZERS OK" : "STABILIZER MISSING") : "") : "";
                String left = show(TileCoreMonitor.SHOW_HEADER) ? (t.name.get().isEmpty() ? "ENERGY CORE" : t.name.get().toUpperCase(Locale.ROOT)) : "";
                header(y, 26, left, right, LILAC);
                y += 26 + gap;
            }
            float pic = 210;
            ops.add(new CoreOp(x0 + pic / 2, y + pic / 2, pic / 300F));
            float cx = x0 + pic + 14, cw = x1 - cx, ly = y + 4;
            String tier = "TIER " + t.tier.get() + " ENERGY CORE";
            text(tier, cx, ly, fit(tier, 19.5F, cw), CYAN);
            ly += 22;
            if (show(TileCoreMonitor.SHOW_PERCENT)) {
                String p = percent();
                float sz = fit(p, 66, cw);
                text(p, cx, ly - sz * 0.06F, sz, pctColour);
                ly += sz * 0.85F + 6;
            }
            if (show(TileCoreMonitor.SHOW_STORED) && pct >= 0) { pieces(cx, ly, 21, cw, num(stored), GOLD, " / " + num(cap) + " " + unit, GRAY); ly += 25; }
            if (show(TileCoreMonitor.SHOW_BAR) && pct >= 0) { bar(cx, ly, x1, ly + 12); ly += 18; }
            if (show(TileCoreMonitor.SHOW_NET)) {
                int n = t.pylonCount.get();
                pieces(cx, ly, 19.5F, cw, "Net ", GRAY, signed(net) + " " + unit + "/t", signColour(net), " · " + n + (n == 1 ? " pylon" : " pylons"), GRAY);
                ly += 23;
            } else if (show(TileCoreMonitor.SHOW_INOUT)) {
                pieces(cx, ly, 19.5F, cw, "+" + num(in), GREEN, " / ", GRAY, "-" + num(out), RED, " " + unit + "/t", GRAY);
                ly += 23;
            }
            if (show(TileCoreMonitor.SHOW_ETA) && !eta.isEmpty()) { text(eta, cx, ly, fit(eta, 19.5F, cw), WHITE); ly += 23; }
            if (show(TileCoreMonitor.SHOW_ETA) && !targetEta.isEmpty() && ly + 20 < y + pic) text(targetEta, cx, ly, fit(targetEta, 19.5F, cw), YELLOW);
            y += pic + gap;
            if (show(TileCoreMonitor.SHOW_STATS)) y = cards(y, 18, gap);
            if (show(TileCoreMonitor.SHOW_GRAPH) && y1 - y > 40) {
                text("Net " + unit + "/t", x0, y, 16.5F, CYAN);
                float rx = x1;
                for (int i = 2; i >= 0; i--) {
                    String chip = "[" + RANGE_CHIPS[i] + "]";
                    textRight(chip, rx, y, 16.5F, i == range() ? WHITE : 0xFF6A6A78);
                    rx -= width(chip, 16.5F) + 6;
                }
                ops.add(new GraphOp(x0, y + 20, x1, y1, t.statSeries[range()], true));
            }
        }

        /** Four stat boxes: AVG 1 H, PEAK DRAW, PEAK IN, LOW 24 H. Returns the y below them. */
        float cards(float y, float size, float gap) {
            float cw = (x1 - x0 - 3 * 8) / 4, ch = size * 2 + 12;
            boolean none = t.statMinutes == 0;
            String[] labels = {"AVG 1 H", "PEAK DRAW", "PEAK IN", "LOW 24 H"};
            String[] values = {none ? "-" : signed(t.statAvg) + " " + unit + "/t", none ? "-" : "-" + num(t.statPeakOut),
                    none ? "-" : "+" + num(t.statPeakIn), t.statLow < 0 ? "-" : String.format(Locale.ROOT, "%.1f%%", t.statLow)};
            int[] colours = {signColour(t.statAvg), RED, GREEN, GOLD};
            for (int i = 0; i < 4; i++) {
                float cx = x0 + i * (cw + 8);
                rect(cx, y, cx + cw, y + ch, 0xFF3A2450, Z_BOX);
                rect(cx + 1, y + 1, cx + cw - 1, y + ch - 1, 0xFF120A18, Z_FILL);
                text(labels[i], cx + 8, y + 6, fit(labels[i], size, cw - 16), GRAY);
                text(values[i], cx + 8, y + 6 + size, fit(values[i], size, cw - 16), none && i < 3 ? GRAY : colours[i]);
            }
            return y + ch + gap;
        }
    }

    // ### Submit

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.draw) return;
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-state.facing.toYRot())); // local +Z = facing, +X = viewer's right
        pose.translate(-0.5, state.h - 0.5, 0.5015);
        pose.scale(1F / B, -1F / B, 1F / B); // canvas: x right, y down, mockup pixels
        for (Object op : state.ops) {
            if (op instanceof Rect r) {
                quad(pose, collector, r.x0, r.y0, r.x1, r.y1, r.z, r.colour);
            } else if (op instanceof Txt tx) {
                float sc = tx.size / 12F; // MC glyphs are 8 px tall; size/12 matches the mockup font's visible height
                pose.pushPose();
                pose.translate(tx.x, tx.y + tx.size * 0.17F, Z_TEXT);
                pose.scale(sc, sc, sc);
                // Argument order: ..., displayMode, light, colour, background, outline (light first; swapped = invisible text).
                collector.submitText(pose, 0, 0, Component.literal(tx.text).getVisualOrderText(), false, Font.DisplayMode.POLYGON_OFFSET,
                        FULL_BRIGHT, tx.colour, 0, 0);
                pose.popPose();
            } else if (op instanceof CoreOp c) {
                drawCore(pose, collector, c.cx, c.cy, c.k, state.time);
            } else if (op instanceof GraphOp g) {
                drawGraph(pose, collector, g);
            }
        }
        pose.popPose();
    }

    // ### Graph

    private static void drawGraph(PoseStack pose, SubmitNodeCollector c, GraphOp g) {
        quad(pose, c, g.x0, g.y0, g.x1, g.y1, Z_BOX, 0xFF3A2450);
        quad(pose, c, g.x0 + 1, g.y0 + 1, g.x1 - 1, g.y1 - 1, Z_FILL, 0xFF120A18);
        long[] d = g.data;
        long maxPos = 0, maxNeg = 0;
        for (long v : d) { maxPos = Math.max(maxPos, v); maxNeg = Math.max(maxNeg, -v); }
        float top = g.y0 + 3, bottom = g.y1 - 3, hgt = bottom - top;
        float zero = maxPos + maxNeg == 0 ? (top + bottom) / 2 : top + hgt * maxPos / (float) (maxPos + maxNeg);
        quad(pose, c, g.x0 + 1, zero, g.x1 - 1, zero + 1, Z_CORE - 0.2F, 0xFF5A5A66);
        if (d.length == 0) return;
        long scale = Math.max(1, maxPos + maxNeg);
        int slots = 60;
        float bw = (g.x1 - g.x0 - 4) / slots;
        int n = Math.min(slots, d.length), off = slots - n;
        float prevX = 0, prevY = 0;
        for (int i = 0; i < n; i++) {
            long v = d[d.length - n + i];
            float bx = g.x0 + 2 + (off + i) * bw;
            float vy = zero - v / (float) scale * hgt;
            if (!g.line) {
                if (v > 0) quad(pose, c, bx, vy, bx + bw * 0.7F, zero, Z_CORE, 0xFF55FF55);
                else if (v < 0) quad(pose, c, bx, zero + 1, bx + bw * 0.7F, vy + 1, Z_CORE, 0xFFFF5555);
            } else {
                float mx = bx + bw / 2;
                quad(pose, c, bx, Math.min(vy, zero), bx + bw, Math.max(vy, zero), Z_LINE, mix(0x120A18, v >= 0 ? 0x55FF55 : 0xFF5555, 0.35F));
                if (i > 0) line(pose, c, prevX, prevY, mx, vy, 2F, Z_CORE, 0xFFFFAA00);
                prevX = mx;
                prevY = vy;
            }
        }
    }

    // ### Core picture (mockup board "Render": svg 300x300, sphere r70, halo r110, orbs r13 at 134, beams 130 -> 80)

    private static final float[][] ICO_V;
    private static final int[][] ICO_E;

    static {
        float p = (float) ((1 + Math.sqrt(5)) / 2);
        float[][] v = {{-1, p, 0}, {1, p, 0}, {-1, -p, 0}, {1, -p, 0}, {0, -1, p}, {0, 1, p}, {0, -1, -p}, {0, 1, -p},
                {p, 0, -1}, {p, 0, 1}, {-p, 0, -1}, {-p, 0, 1}};
        float len = (float) Math.sqrt(1 + p * p);
        for (float[] a : v) for (int i = 0; i < 3; i++) a[i] /= len;
        ICO_V = v;
        List<int[]> edges = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            for (int j = i + 1; j < 12; j++) {
                float dx = v[i][0] - v[j][0], dy = v[i][1] - v[j][1], dz = v[i][2] - v[j][2];
                if (Math.abs(dx * dx + dy * dy + dz * dz - 1.1056F) < 0.05F) edges.add(new int[]{i, j}); // neighbours (edge 1.0515)
            }
        }
        ICO_E = edges.toArray(new int[0][]);
    }

    /**
     * Every layer is opaque on purpose: the screen's quads write depth, so a see-through piece drawn before the shape
     * behind it leaves a hole (the dark smears and block-texture holes Eli saw). Glows are blended into BG by hand.
     */
    private static void drawCore(PoseStack pose, SubmitNodeCollector c, float cx, float cy, float k, float time) {
        float pulse = 0.775F + 0.225F * (float) Math.sin(time * Math.PI / 30); // 3 s cycle
        for (int[] d : new int[][]{{0, -1}, {0, 1}, {-1, 0}, {1, 0}}) {
            line(pose, c, cx + d[0] * 130 * k, cy + d[1] * 130 * k, cx + d[0] * 72 * k, cy + d[1] * 72 * k, 3 * k + 0.5F, Z_FILL + 0.2F,
                    mix(BG, 0x55FFFF, 0.8F * pulse));
        }
        // halo: a ring outside the sphere only, fading into the background
        disc(pose, c, cx, cy, 71 * k, 110 * k, mix(BG, 0x8800FF, 0.45F * pulse), 0xFF000000 | BG, Z_FILL);
        // sphere with a baked-in highlight: fan from the hot spot to the rim, orange half way
        float hx = cx - 14 * k, hy = cy - 18 * k, r = 70 * k;
        int seg = 40;
        c.submitCustomGeometry(pose, RenderTypes.textBackground(), (p, vc) -> {
            for (int i = 0; i < seg; i++) {
                double a0 = i * Math.PI * 2 / seg, a1 = (i + 1) * Math.PI * 2 / seg;
                float rx0 = cx + (float) Math.cos(a0) * r, ry0 = cy + (float) Math.sin(a0) * r;
                float rx1 = cx + (float) Math.cos(a1) * r, ry1 = cy + (float) Math.sin(a1) * r;
                float mx0 = hx + (rx0 - hx) * 0.55F, my0 = hy + (ry0 - hy) * 0.55F, mx1 = hx + (rx1 - hx) * 0.55F, my1 = hy + (ry1 - hy) * 0.55F;
                quad4(p, vc, Z_LINE, hx, hy, 0xFFFFD27A, mx0, my0, 0xFFFF7A1A, mx1, my1, 0xFFFF7A1A, hx, hy, 0xFFFFD27A);
                quad4(p, vc, Z_LINE, mx0, my0, 0xFFFF7A1A, rx0, ry0, 0xFF7A1200, rx1, ry1, 0xFF7A1200, mx1, my1, 0xFFFF7A1A);
            }
        });
        // Shell: icosahedron wireframe, one turn per 18 s, tilted 25 degrees toward the viewer; back edges dimmer.
        double a = time / 360D * Math.PI * 2, tilt = Math.toRadians(25);
        float[][] pr = new float[12][3];
        for (int i = 0; i < 12; i++) {
            float x = ICO_V[i][0], y = ICO_V[i][1], z = ICO_V[i][2];
            float x1 = (float) (x * Math.cos(a) + z * Math.sin(a)), z1 = (float) (-x * Math.sin(a) + z * Math.cos(a));
            float y2 = (float) (y * Math.cos(tilt) - z1 * Math.sin(tilt)), z2 = (float) (y * Math.sin(tilt) + z1 * Math.cos(tilt));
            pr[i] = new float[]{cx + x1 * r * 0.96F, cy - y2 * r * 0.96F, z2}; // just inside the rim
        }
        // Every edge is drawn; its colour slides smoothly from faint purple-on-orange at the back to full purple at the
        // front, so edges never pop in or out as they turn past the rim. Depth also orders them (front edges on top).
        for (int[] e : ICO_E) {
            float depth = (pr[e[0]][2] + pr[e[1]][2]) / 2; // -1 back .. 1 front
            float t = Math.max(0, Math.min(1, (depth + 0.35F) / 0.9F));
            t = t * t * (3 - 2 * t);
            line(pose, c, pr[e[0]][0], pr[e[0]][1], pr[e[1]][0], pr[e[1]][1], 1.6F * k + 0.4F, Z_CORE + 0.15F * t,
                    mix(0xFF7A1A, 0xB066FF, 0.22F + 0.78F * t));
        }
        disc(pose, c, cx, cy, 69.5F * k, 71.5F * k + 0.3F, 0xFFD9B3FF, 0xFFD9B3FF, Z_CORE + 0.2F);
        for (int[] d : new int[][]{{0, -1}, {0, 1}, {-1, 0}, {1, 0}}) {
            float ox = cx + d[0] * 134 * k, oy = cy + d[1] * 134 * k;
            disc(pose, c, ox, oy, 0, 7.8F * k, 0xFFE8FFFF, 0xFF3FD4FF, Z_CORE);
            disc(pose, c, ox, oy, 7.8F * k, 13 * k, 0xFF3FD4FF, 0xFF0A5A7A, Z_CORE);
        }
    }

    private static final int BG = 0x0B0710;

    /** colour blended over a background by alpha a, as an opaque colour. */
    private static int mix(int bg, int colour, float a) {
        a = Math.max(0, Math.min(1, a));
        int r = (int) (((bg >> 16) & 255) * (1 - a) + ((colour >> 16) & 255) * a);
        int g = (int) (((bg >> 8) & 255) * (1 - a) + ((colour >> 8) & 255) * a);
        int b = (int) ((bg & 255) * (1 - a) + (colour & 255) * a);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    /** One quad with a colour per corner, both windings. */
    private static void quad4(PoseStack.Pose p, com.mojang.blaze3d.vertex.VertexConsumer vc, float z, float ax, float ay, int ac, float bx, float by, int bc,
                              float cx, float cy, int cc, float dx, float dy, int dc) {
        vc.addVertex(p, ax, ay, z).setColor(ac).setLight(FULL_BRIGHT);
        vc.addVertex(p, bx, by, z).setColor(bc).setLight(FULL_BRIGHT);
        vc.addVertex(p, cx, cy, z).setColor(cc).setLight(FULL_BRIGHT);
        vc.addVertex(p, dx, dy, z).setColor(dc).setLight(FULL_BRIGHT);
        vc.addVertex(p, dx, dy, z).setColor(dc).setLight(FULL_BRIGHT);
        vc.addVertex(p, cx, cy, z).setColor(cc).setLight(FULL_BRIGHT);
        vc.addVertex(p, bx, by, z).setColor(bc).setLight(FULL_BRIGHT);
        vc.addVertex(p, ax, ay, z).setColor(ac).setLight(FULL_BRIGHT);
    }

    /** A ring (or a disc when r0 = 0) from r0 to r1, colour fading from c0 inside to c1 outside. */
    private static void disc(PoseStack pose, SubmitNodeCollector col, float cx, float cy, float r0, float r1, int c0, int c1, float z) {
        int seg = 32;
        col.submitCustomGeometry(pose, RenderTypes.textBackground(), (p, vc) -> {
            for (int i = 0; i < seg; i++) {
                double a0 = i * Math.PI * 2 / seg, a1 = (i + 1) * Math.PI * 2 / seg;
                float ax = (float) Math.cos(a0), ay = (float) Math.sin(a0), bx = (float) Math.cos(a1), by = (float) Math.sin(a1);
                float[] xs = {cx + ax * r0, cx + ax * r1, cx + bx * r1, cx + bx * r0};
                float[] ys = {cy + ay * r0, cy + ay * r1, cy + by * r1, cy + by * r0};
                int[] cs = {c0, c1, c1, c0};
                for (int v = 0; v < 4; v++) vc.addVertex(p, xs[v], ys[v], z).setColor(cs[v]).setLight(FULL_BRIGHT);
                for (int v = 3; v >= 0; v--) vc.addVertex(p, xs[v], ys[v], z).setColor(cs[v]).setLight(FULL_BRIGHT);
            }
        });
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

    /** A flat colour rectangle at canvas depth z. Both windings, so face culling can't hide it. */
    private static void quad(PoseStack pose, SubmitNodeCollector collector, float x0, float y0, float x1, float y1, float z, int colour) {
        quadPts(pose, collector, x0, y0, x0, y1, x1, y1, x1, y0, z, colour);
    }
}
