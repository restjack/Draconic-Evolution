package com.brandon3055.draconicevolution.client.gui;

import com.brandon3055.brandonscore.BCConfig;
import com.brandon3055.brandonscore.client.BCGuiTextures;
import com.brandon3055.brandonscore.client.gui.GuiToolkit;
import com.brandon3055.brandonscore.client.gui.GuiToolkit.Palette;
import com.brandon3055.brandonscore.client.gui.modular.ModularGui;
import com.brandon3055.brandonscore.client.gui.modular.ModularGuiContainer;
import com.brandon3055.brandonscore.client.gui.modular.elements.*;
import com.brandon3055.brandonscore.client.gui.modular.lib.Constraints;
import com.brandon3055.brandonscore.client.gui.modular.lib.container.ContainerGuiProvider;
import com.brandon3055.brandonscore.client.gui.modular.lib.container.ContainerScreenAccess;
import com.brandon3055.brandonscore.client.gui.modular.lib.geometry.Align;
import com.brandon3055.brandonscore.client.gui.modular.lib.geometry.Direction;
import com.brandon3055.brandonscore.client.gui.modulargui.templates.ButtonRow;
import com.brandon3055.draconicevolution.blocks.tileentity.TileDislocationInhibitor;
import com.brandon3055.draconicevolution.client.DEGuiTextures;
import com.brandon3055.draconicevolution.inventory.DislocationInhibitorMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.function.Supplier;

import static com.brandon3055.brandonscore.client.gui.modular.lib.geometry.Constraint.*;
import static com.brandon3055.brandonscore.client.gui.modular.lib.geometry.GeoParam.*;

/**
 * Dislocation Normalization Field Projector screen, Fusion Conductor style (restjack fork; mockup
 * https://claude.ai/artifact/QMWoAA3VJbaUbia96fuap3), laid out like the Evo Tweaks Field Crystal screen since
 * unofficial-1.34: area grid (Rad / Off rows, X Y Z columns, -/+ with Shift for 10) on top, toggle row (show area,
 * Draconic-only / all magnets, redstone) under it, status box below. Every click flashes a 2 s line in the status box.
 * 236 x 118, fits 320 x 240.
 * Icons: Nextcloud Projects/Draconic-Evolution/tools/make_inhibitor_icons.py -> textures/gui/dislocation_inhibitor/.
 */
public class DislocationInhibitorGui extends ContainerGuiProvider<DislocationInhibitorMenu> {
    private static final GuiToolkit TOOLKIT = new GuiToolkit("gui.draconicevolution.dislocation_inhibitor");
    private static final int W = 236, H = 118;
    private static final String[] RS_KEYS = {"ignore", "high", "low"};
    private static final String[] AXES = {"X", "Y", "Z"};

    @Override
    public GuiElement<?> createRootElement(ModularGui gui) {
        GuiManipulable root = new GuiManipulable(gui).addMoveHandle(3).enableCursors(true);
        GuiTexture bg = new GuiTexture(root.getContentElement(), BCGuiTextures.themedGetter("background_dynamic")).dynamicTexture();
        Constraints.bind(bg, root.getContentElement());
        return root;
    }

    @Override
    public void buildGui(ModularGui gui, ContainerScreenAccess<DislocationInhibitorMenu> screenAccess) {
        gui.initStandardGui(W, H);
        GuiElement<?> root = gui.getRoot();
        TileDislocationInhibitor tile = screenAccess.getMenu().tile;
        TOOLKIT.createHeading(root, TOOLKIT.translate("title"), true);
        Flash flash = new Flash();

        ButtonRow buttons = ButtonRow.topRightInside(root, Direction.DOWN, 3, 3).setSpacing(1);
        buttons.addButton(TOOLKIT::createThemeButton);
        buttons.addButton(e -> TOOLKIT.createIconButton(e, 12, BCGuiTextures.getter("info_panel"))
                .setTooltip(() -> List.of(TOOLKIT.translate("info"), TOOLKIT.translate("info.1").withStyle(ChatFormatting.GRAY),
                        TOOLKIT.translate("info.2").withStyle(ChatFormatting.GRAY), TOOLKIT.translate("info.3").withStyle(ChatFormatting.GRAY))));

        // Area grid: axis header, Rad row, Off row (Field Crystal layout)
        for (int i = 0; i < 3; i++) {
            place(new GuiText(root, Component.literal(AXES[i])).setTextColour(Palette.BG::text).setShadow(false), root, 28 + i * 56, 16, 52, 8);
        }
        areaRow(root, tile, flash, true, 26);
        areaRow(root, tile, flash, false, 40);

        // Toggle row: show area | Draconic only, all magnets | redstone, then the magnet mode in words
        place(toggle(root, "area", tile.showArea::get, () -> {
            boolean v = !tile.showArea.get();
            set(tile, TileDislocationInhibitor.SET_AREA, v ? 1 : 0);
            flash.show(TOOLKIT.translate(v ? "flash.area_on" : "flash.area_off"));
        }, "area"), root, 6, 56, 16, 16);
        place(toggle(root, "mag_de", () -> !tile.allMagnets.get(), () -> {
            if (!tile.allMagnets.get()) {
                flash.show(TOOLKIT.translate("flash.de_same"));
                return;
            }
            set(tile, TileDislocationInhibitor.SET_ALL, 0);
            flash.show(TOOLKIT.translate("flash.de"));
        }, "de_only"), root, 30, 56, 16, 16);
        place(toggle(root, "mag_all", tile.allMagnets::get, () -> {
            if (tile.allMagnets.get()) {
                flash.show(TOOLKIT.translate("flash.all_same"));
                return;
            }
            set(tile, TileDislocationInhibitor.SET_ALL, 1);
            flash.show(TOOLKIT.translate("flash.all"));
        }, "all"), root, 48, 56, 16, 16);
        GuiButton rs = TOOLKIT.createFlat3DButton(root, null)
                .setTooltip(() -> List.of(TOOLKIT.translate("redstone"),
                        TOOLKIT.translate("redstone." + RS_KEYS[tile.redstone()]).withStyle(ChatFormatting.GOLD),
                        TOOLKIT.translate("redstone.info").withStyle(ChatFormatting.GRAY)))
                .onPress(() -> cycleRs(tile, flash, 1), GuiButton.LEFT_CLICK)
                .onPress(() -> cycleRs(tile, flash, 2), GuiButton.RIGHT_CLICK);
        Constraints.bind(new GuiTexture(rs, DEGuiTextures.getter(() -> "dislocation_inhibitor/red_" + tile.redstone())), rs);
        place(rs, root, 72, 56, 16, 16);
        place(new GuiText(root, () -> TOOLKIT.translate("stops_mode", TOOLKIT.translate(tile.allMagnets.get() ? "mode.all" : "mode.de_only")))
                .setTextColour(Palette.BG::text).setShadow(false).setAlignment(Align.LEFT).setTrim(true)
                .setTooltip(() -> List.of(TOOLKIT.translate(tile.allMagnets.get() ? "toggle.all.info" : "toggle.de_only.info").withStyle(ChatFormatting.GRAY))),
                root, 94, 60, W - 100, 9);

        // Status box: flash or field state, then range + mode, then outline + redstone
        GuiRectangle status = new GuiRectangle(root)
                .shadedRect(Palette.SubItem::accentLight, Palette.SubItem::accentDark, Palette.SubItem::fill)
                .setTooltip(() -> List.of(TOOLKIT.translate("status"), TOOLKIT.translate("status.info").withStyle(ChatFormatting.GRAY)));
        place(status, root, 6, 76, W - 12, 36);
        List<Supplier<Component>> lines = List.of(
                () -> flash.get() != null ? flash.get() : state(tile),
                () -> boxLine(tile),
                () -> TOOLKIT.translate("summary.area", onOff(tile.showArea.get())).append("  ")
                        .append(TOOLKIT.translate("summary.rs", TOOLKIT.translate("redstone.short." + RS_KEYS[tile.redstone()]))));
        for (int i = 0; i < lines.size(); i++) {
            place(new GuiText(status, lines.get(i)).setTextColour(Palette.SubItem::text).setShadow(false)
                    .setAlignment(Align.LEFT).setTrim(true), status, 4, 3 + i * 11, W - 20, 9);
        }
    }

    /** "Box 11 x 11 x 11" plus ", offset 0 2 0" when the box is moved. */
    private static Component boxLine(TileDislocationInhibitor tile) {
        MutableComponent c = TOOLKIT.translate("summary.box", tile.size(0), tile.size(1), tile.size(2));
        if (tile.offset(0) != 0 || tile.offset(1) != 0 || tile.offset(2) != 0) {
            c.append(TOOLKIT.translate("summary.offset", tile.offset(0), tile.offset(1), tile.offset(2)));
        }
        return c;
    }

    /** Label, then per axis: - [value] +. Click 1, Shift-click 10; radius 0..16, offset -16..16. */
    private static void areaRow(GuiElement<?> root, TileDislocationInhibitor tile, Flash flash, boolean rad, int y) {
        String which = rad ? "rad" : "off";
        place(new GuiText(root, TOOLKIT.translate(which)).setTextColour(Palette.BG::text).setShadow(false).setAlignment(Align.LEFT),
                root, 6, y + 2, 20, 9);
        Supplier<List<Component>> tip = () -> List.of(TOOLKIT.translate("tip.area_" + which),
                TOOLKIT.translate("tip.step").withStyle(ChatFormatting.GRAY));
        for (int i = 0; i < 3; i++) {
            int axis = i, x = 28 + i * 56;
            place(TOOLKIT.createFlat3DButton(root, () -> Component.literal("-")).setTooltip(tip)
                    .onPress(() -> areaStep(tile, flash, rad, axis, -1)), root, x, y, 12, 12);
            GuiRectangle box = TOOLKIT.shadedBorder(root).fill(Palette.Slot::fill).setTooltip(tip);
            place(box, root, x + 13, y, 26, 12);
            place(new GuiText(box, () -> Component.literal(Integer.toString(rad ? tile.radius(axis) : tile.offset(axis))))
                    .setTextColour(Palette.Slot::text).setShadow(false), box, 0, 2, 26, 9);
            place(TOOLKIT.createFlat3DButton(root, () -> Component.literal("+")).setTooltip(tip)
                    .onPress(() -> areaStep(tile, flash, rad, axis, 1)), root, x + 40, y, 12, 12);
        }
    }

    private static void areaStep(TileDislocationInhibitor tile, Flash flash, boolean rad, int axis, int sign) {
        int lim = rad ? TileDislocationInhibitor.RADIUS_MAX : TileDislocationInhibitor.OFFSET_MAX;
        int before = rad ? tile.radius(axis) : tile.offset(axis);
        int after = Math.max(rad ? 0 : -lim, Math.min(lim, before + sign * (net.minecraft.client.Minecraft.getInstance().hasShiftDown() ? 10 : 1)));
        if (after == before) {
            flash.show(TOOLKIT.translate(rad ? "flash.radius_limit" : "flash.offset_limit", AXES[axis], lim));
            return;
        }
        set(tile, (rad ? TileDislocationInhibitor.SET_RAD : TileDislocationInhibitor.SET_OFF) + axis, after);
        flash.show(rad ? TOOLKIT.translate(after < before ? "flash.radius_smaller" : "flash.radius_set", AXES[axis], after, 2 * after + 1)
                : TOOLKIT.translate("flash.offset_set", AXES[axis], after));
    }

    private static Component state(TileDislocationInhibitor tile) {
        if (!tile.active()) return TOOLKIT.translate(tile.redstone() == TileDislocationInhibitor.RS_HIGH ? "state.wait_high" : "state.wait_low").withStyle(good(false));
        return TOOLKIT.translate("state.holding", tile.held.get()).withStyle(good(true));
    }

    private static void cycleRs(TileDislocationInhibitor tile, Flash flash, int step) {
        int next = (tile.redstone() + step) % 3;
        set(tile, TileDislocationInhibitor.SET_RS, next);
        flash.show(TOOLKIT.translate("redstone").append(": ").append(TOOLKIT.translate("redstone." + RS_KEYS[next])));
    }

    private static void set(TileDislocationInhibitor tile, int which, int value) {
        tile.sendPacketToServer(out -> out.writeByte(which).writeByte(value), 4);
    }

    /** Green / red that stays readable on both themes' status box. */
    private static ChatFormatting good(boolean on) {
        return on ? (BCConfig.darkMode ? ChatFormatting.GREEN : ChatFormatting.DARK_GREEN)
                : (BCConfig.darkMode ? ChatFormatting.RED : ChatFormatting.DARK_RED);
    }

    private static MutableComponent onOff(boolean on) {
        return Component.translatable(on ? "gui.draconicevolution.toggle.on" : "gui.draconicevolution.toggle.off").withStyle(good(on));
    }

    private static void label(GuiElement<?> root, String key, int x, int y, int w) {
        place(new GuiText(root, TOOLKIT.translate(key)).setTextColour(Palette.BG::text).setShadow(false)
                .setAlignment(Align.LEFT).setTrim(true), root, x, y, w, 9);
    }

    private static <E extends GuiElement<?>> E place(E e, GuiElement<?> parent, double x, double y, double w, double h) {
        e.constrain(LEFT, relative(parent.get(LEFT), x)).constrain(TOP, relative(parent.get(TOP), y))
                .constrain(WIDTH, literal(w)).constrain(HEIGHT, literal(h));
        return e;
    }

    /** 16 px toggle like the Conductor's: green with the icon when on; dimmed icon plus a red corner when off. */
    private static GuiButton toggle(GuiElement<?> parent, String icon, Supplier<Boolean> state, Runnable press, String key) {
        GuiButton button = TOOLKIT.createFlat3DButton(parent, null).onPress(press).setToggleMode(state)
                .setTooltip(() -> List.of(TOOLKIT.translate("toggle." + key), onOff(state.get()),
                        TOOLKIT.translate("toggle." + key + ".info").withStyle(ChatFormatting.GRAY)));
        new GuiRectangle(button).fill(0xB040C040).setEnabled(state)
                .constrain(LEFT, relative(button.get(LEFT), 1)).constrain(TOP, relative(button.get(TOP), 1))
                .constrain(RIGHT, relative(button.get(RIGHT), -1)).constrain(BOTTOM, relative(button.get(BOTTOM), -1));
        Constraints.bind(new GuiTexture(button, DEGuiTextures.getter("dislocation_inhibitor/" + icon)), button);
        new GuiRectangle(button).fill(0x90202020).setEnabled(() -> !state.get())
                .constrain(LEFT, relative(button.get(LEFT), 1)).constrain(TOP, relative(button.get(TOP), 1))
                .constrain(RIGHT, relative(button.get(RIGHT), -1)).constrain(BOTTOM, relative(button.get(BOTTOM), -1));
        new GuiRectangle(button).fill(0xFFD03030).setEnabled(() -> !state.get())
                .constrain(RIGHT, relative(button.get(RIGHT), -1)).constrain(BOTTOM, relative(button.get(BOTTOM), -1))
                .constrain(WIDTH, literal(4)).constrain(HEIGHT, literal(4));
        return button;
    }

    /** Short line in the status box after a click, for 2 s. */
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
    }

    public static class Screen extends ModularGuiContainer<DislocationInhibitorMenu> {
        public Screen(DislocationInhibitorMenu menu, Inventory inv, Component title) {
            super(menu, inv, new DislocationInhibitorGui());
            getModularGui().setGuiTitle(title);
        }
    }
}
