package com.brandon3055.draconicevolution.client.gui;

import com.brandon3055.brandonscore.client.BCGuiTextures;
import com.brandon3055.brandonscore.client.gui.GuiToolkit;
import com.brandon3055.brandonscore.client.gui.GuiToolkit.Palette;
import com.brandon3055.brandonscore.client.gui.modular.ModularGui;
import com.brandon3055.brandonscore.client.gui.modular.ModularGuiContainer;
import com.brandon3055.brandonscore.client.gui.modular.elements.*;
import com.brandon3055.brandonscore.client.gui.modular.lib.BackgroundRender;
import com.brandon3055.brandonscore.client.gui.modular.lib.Constraints;
import com.brandon3055.brandonscore.client.gui.modular.lib.GuiRender;
import com.brandon3055.brandonscore.client.gui.modular.lib.container.ContainerGuiProvider;
import com.brandon3055.brandonscore.client.gui.modular.lib.container.ContainerScreenAccess;
import com.brandon3055.brandonscore.client.gui.modular.lib.geometry.Align;
import com.brandon3055.brandonscore.client.gui.modular.lib.geometry.Direction;
import com.brandon3055.brandonscore.client.gui.modular.lib.geometry.GuiParent;
import com.brandon3055.brandonscore.client.gui.modulargui.templates.ButtonRow;
import com.brandon3055.draconicevolution.blocks.tileentity.TileCelestialManipulator;
import com.brandon3055.draconicevolution.client.DEGuiTextures;
import com.brandon3055.draconicevolution.inventory.CelestialManipulatorMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static com.brandon3055.brandonscore.client.gui.modular.lib.geometry.Constraint.*;
import static com.brandon3055.brandonscore.client.gui.modular.lib.geometry.GeoParam.*;

/**
 * Celestial Manipulator screen, Fusion Conductor style (restjack fork, unofficial-1.29; mockup
 * https://claude.ai/artifact/Byy4jNVoc84PEuU8fRjXdf). Tall energy bar (left), sky dial (now + skip target), weather row,
 * skip row, toggles column (keep clear, owner only, tell everyone, quiet boom, redstone), status box. Buttons get a red
 * corner when they can't run now; their tooltip says why and what they cost.
 * <p>
 * Created by FoxMcloud5655 on 28/03/2024.
 */
public class CelestialManipulatorGui extends ContainerGuiProvider<CelestialManipulatorMenu> {
    public static final int GUI_WIDTH = 232;
    public static final int GUI_HEIGHT = 206;
    private static final GuiToolkit TOOLKIT = new GuiToolkit("gui.draconicevolution.celestial_manipulator");
    private static final String KEY = "gui.draconicevolution.celestial_manipulator.";

    @Override
    public GuiElement<?> createRootElement(ModularGui gui) {
        GuiManipulable root = new GuiManipulable(gui).addMoveHandle(3).enableCursors(true);
        GuiTexture bg = new GuiTexture(root.getContentElement(), BCGuiTextures.themedGetter("background_dynamic")).dynamicTexture();
        Constraints.bind(bg, root.getContentElement());
        return root;
    }

    @Override
    public void buildGui(ModularGui gui, ContainerScreenAccess<CelestialManipulatorMenu> screenAccess) {
        gui.initStandardGui(GUI_WIDTH, GUI_HEIGHT);
        GuiElement<?> root = gui.getRoot();
        TileCelestialManipulator tile = screenAccess.getMenu().tile;
        TOOLKIT.createHeading(root, gui.getGuiTitle(), true);
        Flash flash = new Flash();

        com.brandon3055.brandonscore.client.gui.InfoPanel info = com.brandon3055.brandonscore.client.gui.InfoPanel.create(root);
        info.labeledValue(TOOLKIT.translate("info.owner").withStyle(ChatFormatting.GOLD), () -> Component.literal(tile.ownerName.get().isEmpty() ? "-" : tile.ownerName.get()).withStyle(ChatFormatting.GRAY));
        info.labeledValue(TOOLKIT.translate("info.feed").withStyle(ChatFormatting.GOLD), () -> tile.feed.get() <= 0 ? TOOLKIT.translate("no_power").withStyle(ChatFormatting.RED) : Component.literal(TileCelestialManipulator.fmt(tile.feed.get()) + " OP/t").withStyle(ChatFormatting.GRAY));
        info.labeledValue(TOOLKIT.translate("info.last").withStyle(ChatFormatting.GOLD), () -> lastRun(tile));
        ButtonRow buttons = ButtonRow.topRightInside(root, Direction.DOWN, 3, 3).setSpacing(1);
        buttons.addButton(TOOLKIT::createThemeButton);
        buttons.addButton(e -> TOOLKIT.createInfoButton(e, info));

        // Energy bar down the left side
        var energyBar = TOOLKIT.createEnergyBar(root, tile.opStorage);
        energyBar.container()
                .constrain(TOP, relative(root.get(TOP), 18))
                .constrain(LEFT, relative(root.get(LEFT), 5))
                .constrain(HEIGHT, literal(130))
                .constrain(WIDTH, literal(18));

        // Sky dial
        new SkyDial(root, tile)
                .constrain(LEFT, relative(root.get(LEFT), 27))
                .constrain(TOP, relative(root.get(TOP), 18))
                .constrain(WIDTH, literal(178))
                .constrain(HEIGHT, literal(64));

        // Toggles column (right)
        GuiButton[] column = {
                DEToggles.toggle(root, "celestial_manipulator/keep_clear", () -> flash.set(tile, TileCelestialManipulator.SET_KEEP_CLEAR, !tile.keepClear.get(), "keep_clear"), tile.keepClear::get, KEY + "keep_clear"),
                DEToggles.toggle(root, "celestial_manipulator/owner_only", () -> flash.set(tile, TileCelestialManipulator.SET_LOCK, !tile.ownerOnly.get(), "owner_only"), tile.ownerOnly::get, KEY + "owner_only"),
                DEToggles.toggle(root, "celestial_manipulator/notice", () -> flash.set(tile, TileCelestialManipulator.SET_NOTICE, !tile.notice.get(), "notice"), tile.notice::get, KEY + "notice"),
                DEToggles.toggle(root, "celestial_manipulator/quiet", () -> flash.set(tile, TileCelestialManipulator.SET_QUIET, !tile.quiet.get(), "quiet"), tile.quiet::get, KEY + "quiet"),
                redstoneButton(root, tile, flash),
        };
        for (int i = 0; i < column.length; i++) {
            column[i].constrain(LEFT, relative(root.get(LEFT), 210)).constrain(TOP, relative(root.get(TOP), 18 + i * 18));
        }

        // Weather row
        label(root, "weather", 27, 86);
        for (int i = 0; i < 3; i++) actionButton(root, tile, flash, i, 27 + i * 20, 96);
        // Skip row: six times, +24, stop
        label(root, "skipTo", 27, 118);
        for (int i = 3; i < 9; i++) actionButton(root, tile, flash, i, 27 + (i - 3) * 20, 128);
        actionButton(root, tile, flash, 9, 147, 128);
        GuiButton stop = TOOLKIT.createFlat3DButton(root, () -> TOOLKIT.translate("stop_short"))
                .setTooltipSingle(TOOLKIT.translate("stop"))
                .onPress(() -> {
                    flash.clear();
                    tile.sendPacketToServer(out -> out.writeString("STOP"), 0);
                })
                .constrain(LEFT, relative(root.get(LEFT), 167)).constrain(TOP, relative(root.get(TOP), 128))
                .constrain(WIDTH, literal(38)).constrain(HEIGHT, literal(18));
        new GuiRectangle(stop).fill(0x90202020).setEnabled(() -> !tile.timeWarpRunning.get())
                .constrain(LEFT, relative(stop.get(LEFT), 1)).constrain(TOP, relative(stop.get(TOP), 1))
                .constrain(RIGHT, relative(stop.get(RIGHT), -1)).constrain(BOTTOM, relative(stop.get(BOTTOM), -1));

        // Status box: a click's message for 2 s, else the server's last reply (5 s), else "Ready"; two summary lines.
        GuiRectangle statusBG = new GuiRectangle(root)
                .shadedRect(Palette.SubItem::accentLight, Palette.SubItem::accentDark, Palette.SubItem::fill)
                .constrain(LEFT, relative(root.get(LEFT), 5))
                .constrain(RIGHT, relative(root.get(RIGHT), -5))
                .constrain(TOP, relative(root.get(TOP), 152))
                .constrain(BOTTOM, relative(root.get(BOTTOM), -5));
        GuiText modes = new GuiText(statusBG, () -> summaryModes(tile))
                .setShadow(false).setTextColour(Palette.SubItem::text).setAlignment(Align.LEFT)
                .constrain(LEFT, relative(statusBG.get(LEFT), 3))
                .constrain(RIGHT, relative(statusBG.get(RIGHT), -3))
                .constrain(BOTTOM, relative(statusBG.get(BOTTOM), -3))
                .constrain(HEIGHT, literal(9));
        GuiText toggles = new GuiText(statusBG, () -> summaryToggles(tile))
                .setShadow(false).setTextColour(Palette.SubItem::text).setAlignment(Align.LEFT)
                .constrain(LEFT, relative(statusBG.get(LEFT), 3))
                .constrain(RIGHT, relative(statusBG.get(RIGHT), -3))
                .constrain(BOTTOM, match(modes.get(TOP)))
                .constrain(HEIGHT, literal(9));
        new GuiText(statusBG, () -> flash.get() != null ? flash.get() : tile.status.get() != null ? tile.status.get() : ready(tile))
                .setWrap(true).setShadow(false).setTextColour(Palette.SubItem::text).setAlignment(Align.LEFT)
                .constrain(LEFT, relative(statusBG.get(LEFT), 3))
                .constrain(RIGHT, relative(statusBG.get(RIGHT), -3))
                .constrain(TOP, relative(statusBG.get(TOP), 3))
                .constrain(BOTTOM, relative(toggles.get(TOP), -1));
    }

    private static void label(GuiElement<?> root, String key, int x, int y) {
        new GuiText(root, TOOLKIT.translate(key)).setTextColour(Palette.BG::text).setShadow(false).setAlignment(Align.LEFT)
                .constrain(LEFT, relative(root.get(LEFT), x)).constrain(TOP, relative(root.get(TOP), y))
                .constrain(WIDTH, literal(80)).constrain(HEIGHT, literal(9));
    }

    /** 18 px action button: icon, blue while it runs, red corner when it can't run now. */
    private static void actionButton(GuiElement<?> root, TileCelestialManipulator tile, Flash flash, int action, int x, int y) {
        String key = TileCelestialManipulator.KEYS[action];
        GuiButton b = TOOLKIT.createIconButton(root, 18, DEGuiTextures.getter("celestial_manipulator/" + key));
        b.setTooltip(() -> tooltip(tile, action))
                .setTooltipDelay(0)
                // Always ask the server (operators skip the cooldown); its reply shows in the status box.
                .onPress(() -> {
                    flash.clear();
                    tile.sendPacketToServer(out -> out.writeString(TileCelestialManipulator.ACTIONS[action]), 0);
                })
                .constrain(LEFT, relative(root.get(LEFT), x)).constrain(TOP, relative(root.get(TOP), y))
                .constrain(WIDTH, literal(18)).constrain(HEIGHT, literal(18));
        new GuiRectangle(b).fill(0x7040A0FF).setEnabled(() -> tile.active.get() && tile.runAction.get() == action)
                .constrain(LEFT, relative(b.get(LEFT), 1)).constrain(TOP, relative(b.get(TOP), 1))
                .constrain(RIGHT, relative(b.get(RIGHT), -1)).constrain(BOTTOM, relative(b.get(BOTTOM), -1));
        new GuiRectangle(b).fill(0x90202020).setEnabled(() -> !tile.active.get() && why(tile, action) != null)
                .constrain(LEFT, relative(b.get(LEFT), 1)).constrain(TOP, relative(b.get(TOP), 1))
                .constrain(RIGHT, relative(b.get(RIGHT), -1)).constrain(BOTTOM, relative(b.get(BOTTOM), -1));
        new GuiRectangle(b).fill(0xFFD03030).setEnabled(() -> why(tile, action) != null && !(tile.active.get() && tile.runAction.get() == action))
                .constrain(RIGHT, relative(b.get(RIGHT), -1)).constrain(BOTTOM, relative(b.get(BOTTOM), -1))
                .constrain(WIDTH, literal(4)).constrain(HEIGHT, literal(4));
    }

    /** Why this action can't start now (client view; the server checks again), or null. */
    @Nullable
    static Component why(TileCelestialManipulator tile, int action) {
        if (tile.active.get()) return TOOLKIT.translate("why.busy");
        if (tile.cooldown.get() > 0) return TOOLKIT.translate("why.cooldown", tile.cooldown.get());
        if (action == 0 && !tile.raining.get()) return TOOLKIT.translate("why.clear");
        if (action == 1 && tile.raining.get()) return TOOLKIT.translate("why.raining");
        if (action == 2 && tile.raining.get() && tile.thundering.get()) return TOOLKIT.translate("why.storming");
        long cost = tile.costOf(action), have = tile.opStorage.getOPStored();
        if (have < cost) return TOOLKIT.translate("why.power", TileCelestialManipulator.fmt(cost), TileCelestialManipulator.fmt(have));
        return null;
    }

    private static List<Component> tooltip(TileCelestialManipulator tile, int action) {
        List<Component> list = new ArrayList<>();
        list.add(TileCelestialManipulator.label(action));
        list.add(TOOLKIT.translate("cost", TileCelestialManipulator.fmt(tile.costOf(action))).withStyle(ChatFormatting.GRAY));
        // weather: 11.5 s; skip: 5 s wind-up + 50 game ticks per tick, plus ~4 s easing
        long secs = action < 3 ? 12 : 5 + tile.skipLength(action) / (TileCelestialManipulator.WARP_RATE * 20L) + 4;
        list.add(TOOLKIT.translate("takes", secs).withStyle(ChatFormatting.GRAY));
        Component no = why(tile, action);
        if (no != null) list.add(no.copy().withStyle(ChatFormatting.RED));
        return list;
    }

    private static Component ready(TileCelestialManipulator tile) {
        return TOOLKIT.translate("ready", TileCelestialManipulator.clock(tile.getLevel().getOverworldClockTime()), weatherWord(tile));
    }

    private static Component weatherWord(TileCelestialManipulator tile) {
        return TOOLKIT.translate(tile.thundering.get() && tile.raining.get() ? "word.storm" : tile.raining.get() ? "word.rain" : "word.clear");
    }

    private static Component lastRun(TileCelestialManipulator tile) {
        String by = tile.runBy.get();
        if (by.isEmpty() || tile.runAction.get() < 0) return Component.literal("-").withStyle(ChatFormatting.GRAY);
        String who = by.startsWith("p:") ? by.substring(2) : by.equals("auto") ? "Keep clear" : "Redstone";
        return Component.literal(who + ": ").append(TileCelestialManipulator.label(tile.runAction.get())).withStyle(ChatFormatting.GRAY);
    }

    /** "Clear Off  Lock Off  Notify Off  Quiet Off": plain label, On green / Off red (fits 216 px at default scale). */
    private static Component summaryToggles(TileCelestialManipulator tile) {
        return Component.empty().append(word("keep", tile.keepClear.get())).append("  ")
                .append(word("lock", tile.ownerOnly.get())).append("  ")
                .append(word("notice", tile.notice.get())).append("  ")
                .append(word("quiet", tile.quiet.get()));
    }

    private static Component word(String key, boolean on) {
        return TOOLKIT.translate("summary." + key).append(" ").append(Component.translatable(on ? "gui.draconicevolution.toggle.on" : "gui.draconicevolution.toggle.off")
                .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED));
    }

    /** "RS: Off  Fed: 32k OP/t", or "No power" in red while nothing feeds it. */
    private static Component summaryModes(TileCelestialManipulator tile) {
        return TOOLKIT.translate("summary.rs", rsName(tile.rsAction.get())).append("  ")
                .append(fed(tile));
    }

    private static Component fed(TileCelestialManipulator tile) {
        return tile.feed.get() <= 0 ? TOOLKIT.translate("no_power").withStyle(ChatFormatting.RED)
                : TOOLKIT.translate("summary.fed", TileCelestialManipulator.fmt(tile.feed.get()));
    }

    private static Component rsName(int a) {
        return a < 0 ? TOOLKIT.translate("redstone.off") : TileCelestialManipulator.label(a);
    }

    /** Redstone: off, or which action a rising signal starts. Left click forward, right click back. */
    private static GuiButton redstoneButton(GuiElement<?> parent, TileCelestialManipulator tile, Flash flash) {
        GuiButton b = TOOLKIT.createFlat3DButton(parent, null)
                .setTooltip(() -> List.of(TOOLKIT.translate("redstone"),
                        rsName(tile.rsAction.get()).copy().withStyle(ChatFormatting.GOLD),
                        TOOLKIT.translate("redstone.info").withStyle(ChatFormatting.GRAY)))
                .onPress(() -> cycleRs(tile, flash, 1), GuiButton.LEFT_CLICK)
                .onPress(() -> cycleRs(tile, flash, -1), GuiButton.RIGHT_CLICK);
        Constraints.size(b, 16, 16);
        GuiTexture icon = new GuiTexture(b, DEGuiTextures.getter(() -> "celestial_manipulator/" + (tile.rsAction.get() < 0 ? "redstone" : TileCelestialManipulator.KEYS[Math.min(9, tile.rsAction.get())])));
        Constraints.bind(icon, b, 0);
        new GuiRectangle(b).fill(0x90202020).setEnabled(() -> tile.rsAction.get() < 0)
                .constrain(LEFT, relative(b.get(LEFT), 1)).constrain(TOP, relative(b.get(TOP), 1))
                .constrain(RIGHT, relative(b.get(RIGHT), -1)).constrain(BOTTOM, relative(b.get(BOTTOM), -1));
        new GuiRectangle(b).fill(0xFFD03030).setEnabled(() -> tile.rsAction.get() < 0)
                .constrain(RIGHT, relative(b.get(RIGHT), -1)).constrain(BOTTOM, relative(b.get(BOTTOM), -1))
                .constrain(WIDTH, literal(4)).constrain(HEIGHT, literal(4));
        return b;
    }

    private static void cycleRs(TileCelestialManipulator tile, Flash flash, int step) {
        int n = TileCelestialManipulator.ACTIONS.length + 1; // -1..9
        int next = Math.floorMod(tile.rsAction.get() + 1 + step, n) - 1;
        tile.sendPacketToServer(out -> out.writeInt(next), 1);
        flash.show(TOOLKIT.translate("redstone").append(": ").append(rsName(next).copy().withStyle(ChatFormatting.GOLD)));
    }

    /** Short "Keep skies clear: On" line in the status box after a click. */
    private static class Flash {
        private Component text;
        private long until;

        void show(Component c) {
            text = c;
            until = System.currentTimeMillis() + 2000;
        }

        Component get() {
            return System.currentTimeMillis() < until ? text : null;
        }

        void clear() {
            until = 0;
        }

        void set(TileCelestialManipulator tile, int which, boolean on, String key) {
            tile.sendPacketToServer(out -> out.writeByte(which).writeBoolean(on), 2);
            show(DEToggles.onOff(KEY + key, on));
        }
    }

    /**
     * Day arc with the sun and moon where they are now; while a skip runs, a dotted line to the target and progress.
     * Sky colour follows the time of day and darkens in rain. Top-left: clock, weather, run progress or cooldown.
     */
    private static class SkyDial extends GuiElement<SkyDial> implements BackgroundRender {
        private final TileCelestialManipulator tile;

        SkyDial(@NotNull GuiParent<?> parent, TileCelestialManipulator tile) {
            super(parent);
            this.tile = tile;
        }

        @Override
        public double getBackgroundDepth() {
            return 0.035;
        }

        @Override
        public void renderBackground(GuiRender render, double mouseX, double mouseY, float partialTicks) {
            long now = tile.getLevel().getOverworldClockTime();
            double x = xMin(), y = yMin(), w = xSize(), h = ySize();
            render.borderRect(x, y, w, h, 1, sky(now), 0xFF555555);
            double cx = x + w * 0.62, hz = y + h * 0.8, r = h * 0.62;
            for (int i = 0; i <= 32; i++) { // arc
                double a = Math.PI * i / 32;
                render.rect(cx - Math.cos(a) * r - 0.5, hz - Math.sin(a) * r - 0.5, 1, 1, 0x60FFFFFF);
            }
            if (tile.timeWarpRunning.get()) { // dotted line to the target
                double[] t = point(tile.targetTime.get(), cx, hz, r);
                for (int i = 1; i <= 10; i++) if (hz + (t[1] - hz) * i / 10 < hz) render.rect(cx + (t[0] - cx) * i / 10 - 0.5, hz + (t[1] - hz) * i / 10 - 0.5, 1, 1, 0xFF8FB8FF);
            }
            double[] s = point(now, cx, hz, r), m = point(now + 12000, cx, hz, r);
            if (s[1] < hz) render.rect(s[0] - 3, s[1] - 3, 6, Math.min(6, hz - s[1] + 3), 0xFFFFE58A);
            if (m[1] < hz) render.rect(m[0] - 2, m[1] - 2, 4, Math.min(4, hz - m[1] + 2), 0xFFDFE6F0);
            if (tile.raining.get()) {
                long tick = tile.getLevel().getGameTime();
                int spanX = Math.max(1, (int) (w - 4)), spanY = Math.max(1, (int) (h * 0.8 - 4));
                for (int i = 0; i < 18; i++) {
                    double rx = x + 2 + (i * 37 % spanX), ry = y + 1 + ((i * 11 + tick) % spanY);
                    render.rect(rx, ry, 1, 3, 0x80AABEE6);
                }
            }
            render.rect(x + 1, hz, w - 2, y + h - 1 - hz, 0xFF1D2A1D); // ground covers what is below the horizon
            render.drawString(TileCelestialManipulator.clock(now), x + 4, y + 4, 0xFFFFFFFF, true);
            render.drawString(weatherWord(tile), x + 4, y + 14, tile.raining.get() ? 0xFF9FB6E0 : 0xFFFFE58A, true);
            Component line = progress();
            if (line != null) render.drawString(line, x + 4, y + 24, 0xFFC8D0D8, true);
        }

        @Nullable
        private Component progress() {
            int a = tile.runAction.get();
            if (tile.weatherToggleRunning.get()) {
                return TileCelestialManipulator.label(a).copy().append(" " + Math.min(99, tile.timer * 100 / 230) + "%");
            }
            if (tile.timeWarpRunning.get()) {
                if (tile.timer <= 100) return TOOLKIT.translate("spinning");
                long len = Math.max(1, tile.runLength.get()), left = tile.targetTime.get() - tile.getLevel().getOverworldClockTime();
                return TileCelestialManipulator.label(a).copy().append(" " + Math.max(0, Math.min(99, (int) (100 - left * 100 / len))) + "%");
            }
            if (tile.cooldown.get() > 0) return TOOLKIT.translate("why.cooldown", tile.cooldown.get());
            return null;
        }

        /** Sun position: tick 0 (sunrise) on the left horizon, 6000 at the top. */
        private static double[] point(long time, double cx, double hz, double r) {
            double a = Math.floorMod(time, 24000L) / 24000D * Math.PI * 2;
            return new double[]{cx - Math.cos(a) * r, hz - Math.sin(a) * r};
        }

        private int sky(long time) {
            double a = Math.floorMod(time, 24000L) / 24000D - 0.25;
            double b = Math.max(0, Math.min(1, Math.cos(a * Math.PI * 2) * 2 + 0.5));
            double dusk = 1 - Math.abs(b - 0.5) * 2;
            int[] night = {10, 14, 34}, day = {90, 135, 220}, warm = {200, 110, 60};
            int[] c = new int[3];
            for (int i = 0; i < 3; i++) {
                double v = night[i] + (day[i] - night[i]) * b;
                v += (warm[i] - v) * dusk * 0.5;
                if (tile.raining.get()) v = v * (tile.thundering.get() ? 0.45 : 0.65) + 15;
                c[i] = (int) Math.max(0, Math.min(255, v));
            }
            return 0xFF000000 | c[0] << 16 | c[1] << 8 | c[2];
        }
    }

    public static class Screen extends ModularGuiContainer<CelestialManipulatorMenu> {
        public Screen(CelestialManipulatorMenu menu, Inventory inv, Component title) {
            super(menu, inv, new CelestialManipulatorGui());
            getModularGui().setGuiTitle(title);
        }
    }
}
