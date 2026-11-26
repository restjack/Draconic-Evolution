package com.brandon3055.draconicevolution.client.gui;

import com.brandon3055.brandonscore.client.gui.modular.ModularGui;
import com.brandon3055.brandonscore.client.gui.modular.ModularGuiContainer;
import com.brandon3055.brandonscore.client.gui.modular.elements.GuiButton;
import com.brandon3055.brandonscore.client.gui.modular.elements.GuiElement;
import com.brandon3055.brandonscore.client.gui.modular.elements.GuiManipulable;
import com.brandon3055.brandonscore.client.gui.modular.elements.GuiRectangle;
import com.brandon3055.brandonscore.client.gui.modular.elements.GuiTextField;
import com.brandon3055.brandonscore.client.gui.modular.lib.Constraints;
import com.brandon3055.brandonscore.client.gui.modular.lib.GuiRender;
import com.brandon3055.brandonscore.client.gui.modular.lib.TextState;
import com.brandon3055.brandonscore.client.gui.modular.lib.container.ContainerGuiProvider;
import com.brandon3055.brandonscore.client.gui.modular.lib.container.ContainerScreenAccess;
import com.brandon3055.brandonscore.multiblock.EmptyPart;
import com.brandon3055.brandonscore.multiblock.MultiBlockDefinition;
import com.brandon3055.brandonscore.multiblock.MultiBlockManager;
import com.brandon3055.brandonscore.multiblock.MultiBlockPart;
import com.brandon3055.brandonscore.utils.Utils;
import com.brandon3055.draconicevolution.DEConfig;
import com.brandon3055.draconicevolution.DraconicEvolution;
import com.brandon3055.draconicevolution.blocks.machines.CoreMonitor;
import com.brandon3055.draconicevolution.blocks.tileentity.TileCoreMonitor;
import com.brandon3055.draconicevolution.blocks.tileentity.TileEnergyCore;
import com.brandon3055.draconicevolution.init.DEContent;
import com.brandon3055.draconicevolution.client.render.MonitorHighlight;
import com.brandon3055.draconicevolution.inventory.CoreMonitorMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import static com.brandon3055.brandonscore.BCConfig.darkMode;

/**
 * eli.8 Core Monitor settings window, drawn like the mockups (artifact EH52817rQQMr5U8Gxmf923, boards Settings,
 * Controls, Pylons): grey panel with title and Done, a dark status strip, tab chips, boxed sections with titles,
 * checkboxes, radio buttons and sliders. Stats is drawn like Logistic Networks' I/O Monitor. Follows Brandon's Core's
 * light/dark theme. Every change goes to the server, which checks the player is the owner or an operator.
 * <p>
 * Everything is painted here (no BC textures): {@link #paint} adds a drawn element, {@link #click} a clickable one.
 */
public class CoreMonitorGui extends ContainerGuiProvider<CoreMonitorMenu> {
    private static final int W = 352, H = 252, PYLON_ROWS = 7;
    private static final String[] TABS = {"Display", "Control", "Pylons", "Options", "Stats"};
    private static final String[] RANGE_LONG = {"5 minutes", "1 hour", "24 hours"}, RANGE_SHORT = {"5m", "1h", "24h"};
    private static int tab = 0; // remembered while the game runs
    private static int statRange = 0;

    private TileCoreMonitor tile;
    private ModularGui gui;
    private GuiElement<?> root;
    private int previewTier = -1, ticks;
    private Slider warnSlider, alarmSlider;
    private GuiTextField nameField, targetField;

    public static class Screen extends ModularGuiContainer<CoreMonitorMenu> {
        public Screen(CoreMonitorMenu menu, Inventory inv, Component title) {
            super(menu, inv, new CoreMonitorGui());
        }
    }

    @Override
    public GuiElement<?> createRootElement(ModularGui gui) {
        GuiManipulable root = new GuiManipulable(gui).addMoveHandle(3).enableCursors(true);
        GuiRectangle bg = new GuiRectangle(root.getContentElement()) {
            @Override
            public void renderBackground(GuiRender r, double mx, double my, float pt) {
                panel(r, xMin(), yMin(), xSize(), ySize());
            }
        };
        Constraints.bind(bg, root.getContentElement());
        return root;
    }

    @Override
    public void buildGui(ModularGui gui, ContainerScreenAccess<CoreMonitorMenu> access) {
        tile = access.getMenu().tile;
        this.gui = gui;
        gui.initStandardGui(W, H);
        gui.setGuiTitle(Component.literal("Core Monitor"));
        gui.renderScreenBackground(true);
        root = gui.getRoot();

        int[] size = screenSize();
        paint(8, 6, 250, 10, -1, (r, x, y, w, h, hov) -> r.drawString(Component.literal("Core Monitor (" + size[0] + "\u00D7" + size[1] + ")").withStyle(s -> s.withBold(true)), x, y, head(), false));
        click(W - 46, 4, 38, 14, -1, (r, x, y, w, h, hov) -> button(r, x, y, w, h, "Done", btn(), hov, false), () -> gui.getScreen().onClose());
        paint(8, 22, W - 16, 16, -1, this::statusStrip);
        buildTabs();

        if (!DEConfig.extra("coreMonitor")) {
            paint(8, 64, W - 16, 30, -1, (r, x, y, w, h, hov) -> r.drawWordWrap(Component.literal("The Core Monitor is switched off in the server config (Extras > coreMonitor)."), x, y, (int) w, 0xFFFF5555));
            return;
        }
        buildDisplay();
        buildControl();
        buildPylons();
        buildOptions();
        buildStats();

        gui.onTick(() -> {
            if (warnSlider != null) warnSlider.settle();
            if (alarmSlider != null) alarmSlider.settle();
            if (++ticks % 20 != 0) return;
            if (tab == 2) requestPylons();
            if (tab == 4) requestStats();
        });
        if (tab == 2) requestPylons();
        if (tab == 4) requestStats();
    }

    // ### Frame: status strip and tabs

    private void statusStrip(GuiRender r, double x, double y, double w, double h, boolean hov) {
        boolean linked = tile.isLinked(), live = tile.loaded.get();
        int border = !linked ? 0xFFFF5555 : !live ? 0xFF6A6A78 : !tile.coreActive.get() || !tile.stabilizersOk.get() ? 0xFFFF5555 : 0xFF8800FF;
        r.fill(x, y, x + w, y + h, border);
        r.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF0B0710);
        if (!linked) {
            str(r, fit("Not linked: Shift-click a core with a Crystal Binder, then this monitor", w - 10), x + 5, y + 4, 0xFFFF5555);
            return;
        }
        BlockPos p = tile.linkPos.get();
        String dim = tile.linkDim.get();
        dim = dim.substring(dim.indexOf(':') + 1).replace("the_", "");
        String right = dim + " \u00B7 " + (live ? "loaded" : "not loaded");
        int rw = font().width(right);
        str(r, right, x + w - 5 - rw, y + 4, 0xFFAAAAAA);
        str(r, fit("Linked: Tier " + tile.tier.get() + " Energy Core at " + p.getX() + " " + p.getY() + " " + p.getZ(), w - rw - 16), x + 5, y + 4, live ? 0xFF55FF55 : 0xFFAAAAAA);
    }

    private void buildTabs() {
        int x = 8;
        for (int i = 0; i < TABS.length; i++) {
            int t = i, tw = font().width(TABS[i]) + 14, tx = x;
            click(tx, 42, tw, 14, -1, (r, bx, by, w, h, hov) -> {
                boolean on = tab == t;
                if (on) {
                    r.fill(bx, by, bx + w, by + h, outline());
                    r.fill(bx + 1, by + 1, bx + w - 1, by + h + 1, panelBg());
                } else {
                    r.fill(bx, by + 1, bx + w, by + h, outline());
                    r.fill(bx + 1, by + 2, bx + w - 1, by + h - 1, hov ? lighten(btn()) : btn());
                }
                str(r, TABS[t], bx + 7, by + (on ? 3 : 4), on ? head() : 0xFFE8E8E8);
            }, () -> {
                tab = t;
                if (t == 2) requestPylons();
                if (t == 4) requestStats();
            });
            x += tw + 2;
        }
        // the line under the tabs, broken under the active one
        paint(8, 55, W - 16, 1, -1, (r, lx, ly, w, h, hov) -> {
            int cx = 8;
            for (int i = 0; i < TABS.length; i++) {
                int tw = font().width(TABS[i]) + 14;
                if (i == tab) {
                    r.fill(lx, ly, root.xMin() + cx, ly + 1, outline());
                    r.fill(root.xMin() + cx + tw, ly, lx + w, ly + 1, outline());
                    return;
                }
                cx += tw + 2;
            }
        });
        paint(8, H - 14, W - 16, 10, -1, (r, fx, fy, fw, fh, hov) -> str(r, fit(footer(), fw), fx, fy, muted()));
    }

    private String footer() {
        return switch (tab) {
            case 0 -> "Link: Crystal Binder, Shift-click the core, then this monitor.";
            case 1 -> "The tier upgrade itself is still done at the core.";
            case 2 -> "Pin: box through walls for 30 s + chat waypoint. Click again to stop.";
            case 3 -> "Alerts: below the alarm %, stabilizer broken, core turned off.";
            default -> tile.owner.isNull() ? "No owner: anyone can change settings" : "Owner: " + tile.ownerName.get() + " (and operators)";
        };
    }

    // ### Display (mockup board "Settings")

    private void buildDisplay() {
        paint(8, 60, 170, 150, 0, (r, x, y, w, h, hov) -> fieldset(r, x, y, w, h, "Show on screen"));
        String[] names = {"Tier header", "Status word", "Percent", "Fill bar", "Stored / capacity", "In and out", "Net per tick",
                "Time to full / empty", "Graph (3\u00D72 and up)", "Stat cards (3\u00D73, 4\u00D74)", "Core picture", "Frame"};
        int[] flags = {TileCoreMonitor.SHOW_HEADER, TileCoreMonitor.SHOW_STATUS, TileCoreMonitor.SHOW_PERCENT, TileCoreMonitor.SHOW_BAR,
                TileCoreMonitor.SHOW_STORED, TileCoreMonitor.SHOW_INOUT, TileCoreMonitor.SHOW_NET, TileCoreMonitor.SHOW_ETA,
                TileCoreMonitor.SHOW_GRAPH, TileCoreMonitor.SHOW_STATS, TileCoreMonitor.SHOW_RENDER, TileCoreMonitor.SHOW_FRAME};
        for (int i = 0; i < names.length; i++) {
            int flag = flags[i];
            String name = names[i];
            click(14, 72 + i * 11, 160, 10, 0, (r, x, y, w, h, hov) -> check(r, x, y, w, h, name, tile.isShown(flag), hov, false),
                    () -> tile.sendPacketToServer(o -> o.writeInt(tile.show.get() ^ flag), TileCoreMonitor.MSG_SHOW));
        }

        paint(186, 60, 158, 28, 0, (r, x, y, w, h, hov) -> fieldset(r, x, y, w, h, "Unit"));
        radios(192, 72, 0, new String[]{"Mine", "OP", "RF", "FE", "J"}, () -> clamp(tile.unit.get(), 4), i -> sendLook(null, i, -1, -1, -1, -1));

        paint(186, 92, 158, 58, 0, (r, x, y, w, h, hov) -> {
            fieldset(r, x, y, w, h, "Alert colours");
            str(r, "Warn (yellow) below " + warnSlider.value() + "%", x + 6, y + 12, text());
            str(r, "Alarm (red) below " + alarmSlider.value() + "%", x + 6, y + 34, text());
        });
        warnSlider = slider(192, 114, 146, 0, 0xFFFFAA00, () -> tile.warnAt.get(), v -> sendLook(null, -1, v, -1, -1, -1));
        alarmSlider = slider(192, 136, 146, 0, 0xFFFF5555, () -> tile.alarmAt.get(), v -> sendLook(null, -1, -1, v, -1, -1));

        paint(186, 154, 158, 28, 0, (r, x, y, w, h, hov) -> fieldset(r, x, y, w, h, "Screen graph"));
        radios(192, 166, 0, new String[]{"5 min", "1 hour", "24 h"}, () -> clamp(tile.screenRange.get(), 2),
                i -> tile.sendPacketToServer(o -> o.writeByte(i), TileCoreMonitor.MSG_SCREEN_RANGE));

        paint(186, 186, 158, 28, 0, (r, x, y, w, h, hov) -> fieldset(r, x, y, w, h, "Background"));
        radios(192, 198, 0, new String[]{"Solid", "75%", "50%"}, () -> clamp(tile.bgAlpha.get(), 2), i -> sendLook(null, -1, -1, -1, -1, i));
    }

    /** Sends the look settings; -1 / null = keep the saved value. */
    private void sendLook(String name, int unit, int warn, int alarm, int target, int bg) {
        String n = name != null ? name : tile.name.get();
        int u = unit >= 0 ? unit : tile.unit.get(), w = warn >= 0 ? warn : tile.warnAt.get(), a = alarm >= 0 ? alarm : tile.alarmAt.get();
        int t = target >= 0 ? target : tile.target.get(), b = bg >= 0 ? bg : tile.bgAlpha.get();
        tile.sendPacketToServer(o -> { o.writeString(n); o.writeByte(u); o.writeByte(w); o.writeByte(a); o.writeByte(t); o.writeByte(b); }, TileCoreMonitor.MSG_LOOK);
    }

    // ### Control (mockup board "Controls")

    private void buildControl() {
        paint(8, 60, 168, 10, 1, (r, x, y, w, h, hov) -> str(r, "Core power", x, y, head()));
        paint(8, 72, 168, 30, 1, (r, x, y, w, h, hov) -> {
            boolean on = tile.coreActive.get(), live = tile.loaded.get();
            r.fill(x, y, x + w, y + h, !tile.isLinked() || !live ? 0xFF6A6A78 : on ? 0xFF8800FF : 0xFFFF5555);
            r.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF0B0710);
            String word = !tile.isLinked() ? "NOT LINKED" : !live ? "NOT LOADED" : on ? "ACTIVE" : "INACTIVE";
            int ww = font().width(word);
            str(r, word, x + w - 5 - ww, y + 5, !tile.isLinked() || !on ? 0xFFFF5555 : !live ? 0xFFAAAAAA : 0xFF55FF55);
            str(r, fit("TIER " + tile.tier.get() + " ENERGY CORE", w - ww - 14), x + 5, y + 5, 0xFF55FFFF);
            long net = tile.input.get() - tile.output.get();
            String line = !tile.isLinked() ? "Link a core first" : !live ? "Last values shown" : on
                    ? "Running \u00B7 Net " + signed(net) + " " + tile.unitName() + "/t"
                    : "Stopped \u00B7 " + EnergyCoreGui.shortNumber(tile.toUnit(tile.stored.get())) + " " + tile.unitName() + " kept";
            str(r, fit(line, w - 10), x + 5, y + 17, 0xFFAAAAAA);
        });
        click(8, 106, 168, 20, 1, (r, x, y, w, h, hov) -> button(r, x, y, w, h, tile.coreActive.get() ? "Deactivate core" : "Activate core",
                        tile.coreActive.get() ? 0xFF8B3A3A : 0xFF3A7A3A, hov, !tile.isLinked() || !tile.loaded.get()),
                () -> { if (tile.isLinked() && tile.loaded.get()) tile.sendPacketToServer(o -> {}, TileCoreMonitor.MSG_TOGGLE_CORE); });
        paint(8, 130, 168, 40, 1, (r, x, y, w, h, hov) -> r.drawWordWrap(Component.literal(
                "Deactivating stops the pylons; stored energy is kept. Activating checks the structure and stabilizers first, like the core's own button."), x, y, (int) w, muted()));
        paint(8, 182, 168, 10, 1, (r, x, y, w, h, hov) -> str(r, "Who can use this", x, y, head()));
        paint(8, 194, 168, 30, 1, (r, x, y, w, h, hov) -> r.drawWordWrap(Component.literal(tile.owner.isNull()
                ? "No owner yet: anyone can change it. Linking with the binder makes you the owner."
                : "Only " + tile.ownerName.get() + " (who linked it) and server operators."), x, y, (int) w, text()));

        // next tier preview
        paint(184, 60, 100, 10, 1, (r, x, y, w, h, hov) -> str(r, "Next tier preview", x, y, head()));
        click(290, 57, 13, 13, 1, (r, x, y, w, h, hov) -> button(r, x, y, w, h, "<", btn(), hov, preview() <= 1),
                () -> previewTier = Math.max(1, preview() - 1));
        paint(303, 60, 28, 10, 1, (r, x, y, w, h, hov) -> { String s = "T" + preview(); str(r, s, x + (w - font().width(s)) / 2, y, text()); });
        click(331, 57, 13, 13, 1, (r, x, y, w, h, hov) -> button(r, x, y, w, h, ">", btn(), hov, preview() >= TileEnergyCore.MAX_TIER),
                () -> previewTier = Math.min(TileEnergyCore.MAX_TIER, preview() + 1));
        paint(184, 74, 78, 36, 1, (r, x, y, w, h, hov) -> {
            int cur = Math.max(1, tile.tier.get());
            card(r, x, y, w, h, 0xFF8800FF);
            str(r, "NOW \u00B7 TIER " + cur, x + 4, y + 4, 0xFFAAAAAA);
            str(r, fit(capText(cur), w - 8), x + 4, y + 15, 0xFFFFAA00);
            str(r, fit(total(blockCounts(cur)) + " blocks", w - 8), x + 4, y + 26, 0xFFAAAAAA);
        });
        paint(266, 74, 78, 36, 1, (r, x, y, w, h, hov) -> {
            int t = preview(), cur = Math.max(1, tile.tier.get());
            card(r, x, y, w, h, t == cur ? 0xFF8800FF : 0xFF55FF55);
            str(r, "TIER " + t, x + 4, y + 4, 0xFFAAAAAA);
            str(r, fit(capText(t), w - 8), x + 4, y + 15, 0xFF55FF55);
            long cap = capacity(t), now = capacity(cur);
            String ratio = cap < 0 ? "No real limit" : now <= 0 || t == cur ? "current tier" : cap >= now
                    ? String.format(Locale.ROOT, "\u00D7%.1f of now", cap / (double) now) : String.format(Locale.ROOT, "%.0f%% of now", cap * 100D / now);
            str(r, fit(ratio, w - 8), x + 4, y + 26, 0xFFAAAAAA);
        });
        paint(184, 114, 160, 48, 1, (r, x, y, w, h, hov) -> {
            r.fill(x, y, x + w, y + h, fieldCol());
            r.fill(x + 1, y + 1, x + w - 1, y + h - 1, subBg());
            int t = preview(), cur = Math.max(1, tile.tier.get());
            str(r, "What tier " + t + " needs", x + 5, y + 4, head());
            Map<Block, Integer> need = blockCounts(t), have = blockCounts(cur);
            int row = 0;
            for (var e : need.entrySet()) {
                if (row == 2) break;
                int diff = e.getValue() - have.getOrDefault(e.getKey(), 0);
                String amount = e.getValue() + (t != cur && diff != 0 ? " (" + (diff > 0 ? "+" : "") + diff + ")" : "");
                int aw = font().width(amount);
                str(r, amount, x + w - 5 - aw, y + 15 + row * 10, darkMode ? 0xFF55FF55 : 0xFF2F6B2F);
                str(r, fit(e.getKey().getName().getString(), w - aw - 16), x + 5, y + 15 + row * 10, text());
                row++;
            }
            String stab = t >= TileEnergyCore.ADV_STABILIZER_TIER ? "4 advanced stabilizers" : "4 stabilizers";
            str(r, stab, x + 5, y + 15 + row * 10, text());
        });
        paint(184, 166, 160, 10, 1, (r, x, y, w, h, hov) -> str(r, "All tiers", x, y, head()));
        for (int i = 0; i < TileEnergyCore.MAX_TIER; i++) {
            int t = i + 1;
            click(184 + (i % 2) * 81, 178 + (i / 2) * 12, 79, 11, 1, (r, x, y, w, h, hov) -> {
                int cur = Math.max(1, tile.tier.get());
                int bg = t == cur ? 0xFF8800FF : t == preview() ? 0xFF2F6B2F : hov ? lighten(subBg()) : subBg();
                r.fill(x, y, x + w, y + h, bg);
                str(r, fit("T" + t + " \u00B7 " + shortCap(t), w - 6), x + 3, y + 2, t == cur || t == preview() ? 0xFFFFFFFF : text());
            }, () -> previewTier = t);
        }
    }

    private int preview() {
        if (previewTier < 1) previewTier = Math.min(TileEnergyCore.MAX_TIER, Math.max(1, tile.tier.get() + 1));
        return previewTier;
    }

    private static long capacity(int tier) {
        Long[] caps = DEConfig.coreCapacity;
        return tier < 1 || tier > caps.length ? 0 : caps[tier - 1];
    }

    private String capText(int tier) {
        long c = capacity(tier);
        return c < 0 ? "Unlimited" : EnergyCoreGui.shortNumber(tile.toUnit(c)) + " " + tile.unitName();
    }

    private String shortCap(int tier) {
        long c = capacity(tier);
        return c < 0 ? "Unlimited" : EnergyCoreGui.shortNumber(tile.toUnit(c));
    }

    private static int total(Map<Block, Integer> m) {
        int n = 0;
        for (int v : m.values()) n += v;
        return n;
    }

    private static final Map<Integer, Map<Block, Integer>> COUNTS = new java.util.HashMap<>();

    /** Blocks of each type in a tier's structure, not counting the core itself. */
    private static Map<Block, Integer> blockCounts(int tier) {
        return COUNTS.computeIfAbsent(tier, t -> {
            Map<Block, Integer> out = new LinkedHashMap<>();
            MultiBlockDefinition def = MultiBlockManager.getDefinition(Identifier.fromNamespaceAndPath(DraconicEvolution.MODID, "energy_core_" + t));
            if (def == null) return out;
            for (MultiBlockPart part : def.getBlocks().values()) {
                if (part instanceof EmptyPart) continue;
                Block b = part.getFirstValidBlock();
                if (b == DEContent.ENERGY_CORE.get() || b.defaultBlockState().isAir()) continue;
                out.merge(b, 1, Integer::sum);
            }
            return out;
        });
    }

    // ### Pylons (mockup board "Pylons")

    private void requestPylons() {
        tile.sendPacketToServer(out -> {}, TileCoreMonitor.MSG_PYLONS);
    }

    private TileCoreMonitor.PylonRow pylon(int row) {
        var list = tile.clientPylons;
        return row < list.size() ? list.get(row) : null;
    }

    private void buildPylons() {
        paint(8, 60, 214, 168, 2, (r, x, y, w, h, hov) -> {
            fieldset(r, x, y, w, h, "Pylons on this core");
            if (tile.clientPylons.isEmpty()) {
                str(r, tile.loaded.get() ? "No pylons linked to this core." : "Core not loaded.", x + 6, y + 14, muted());
            } else if (tile.clientPylons.size() > PYLON_ROWS) {
                str(r, "...and " + (tile.clientPylons.size() - PYLON_ROWS) + " more", x + 6, y + h - 11, muted());
            }
        });
        for (int i = 0; i < PYLON_ROWS; i++) {
            int row = i, ry = 72 + i * 21;
            paint(14, ry, 202, 19, 2, (r, x, y, w, h, hov) -> {
                var p = pylon(row);
                if (p == null) return;
                card(r, x, y, w, h, p.input() ? 0xFF3A7A3A : 0xFF8B3A3A);
                String rate = (p.input() ? "+" : "-") + EnergyCoreGui.shortNumber(tile.toUnit(p.rate())) + "/t";
                int rw = font().width(rate);
                str(r, rate, x + w - 66 - rw, y + 6, p.rate() == 0 ? 0xFFAAAAAA : p.input() ? 0xFF55FF55 : 0xFFFF5555);
                str(r, fit(p.pos().getX() + " " + p.pos().getY() + " " + p.pos().getZ(), w - 76 - rw), x + 5, y + 6, 0xFFE8E6F0);
            });
            click(14 + 202 - 62, ry + 2, 14, 15, 2, (r, x, y, w, h, hov) -> {
                var p = pylon(row);
                if (p == null) return;
                button(r, x, y, w, h, "", MonitorHighlight.isOn(tile.linkDim.get(), p.pos()) ? 0xFF6A2FA8 : btn(), hov, false);
                pin(r, x, y);
            }, () -> {
                var p = pylon(row);
                if (p == null) return;
                // Through-walls box (30 s, click again to stop) plus the server's chat waypoint and particle column.
                if (MonitorHighlight.toggle(tile.linkDim.get(), p.pos(), p.input() ? 0x55FF55 : 0xFF5555)) {
                    tile.sendPacketToServer(o -> o.writePos(p.pos()), TileCoreMonitor.MSG_PYLON_LOCATE);
                }
            });
            click(14 + 202 - 46, ry + 2, 44, 15, 2, (r, x, y, w, h, hov) -> {
                var p = pylon(row);
                if (p == null) return;
                button(r, x, y, w, h, p.input() ? "INPUT" : "OUTPUT", p.input() ? 0xFF3A7A3A : 0xFF8B3A3A, hov, false);
            }, () -> {
                var p = pylon(row);
                if (p != null) tile.sendPacketToServer(o -> o.writePos(p.pos()), TileCoreMonitor.MSG_PYLON_FLIP);
            });
        }

        paint(230, 60, 114, 168, 2, (r, x, y, w, h, hov) -> {
            fieldset(r, x, y, w, h, "Rate limits");
            str(r, "Max input / tick", x + 6, y + 12, text());
            str(r, "Max output / tick", x + 6, y + 40, text());
            str(r, "Reserve %", x + 6, y + 68, text());
            r.drawWordWrap(Component.literal("Whole core, shared by its pylons. Reserve: no output below this fill. Empty = no limit."),
                    x + 6, y + 116, (int) w - 12, muted());
        });
        GuiTextField in = input(236, 82, 102, 2, 0xFF55FF55, tile.limIn.get() > 0 ? Long.toString(tile.limIn.get()) : "", 19);
        GuiTextField out = input(236, 110, 102, 2, 0xFFFF5555, tile.limOut.get() > 0 ? Long.toString(tile.limOut.get()) : "", 19);
        GuiTextField res = input(236, 138, 102, 2, 0xFFFFAA00, tile.limReserve.get() > 0 ? Integer.toString(tile.limReserve.get()) : "", 3);
        click(236, 156, 102, 15, 2, (r, x, y, w, h, hov) -> button(r, x, y, w, h, "Save limits", btn(), hov, !tile.isLinked() || !tile.loaded.get()), () -> {
            if (!tile.isLinked() || !tile.loaded.get()) return;
            long a = parse(in.getValue()), b = parse(out.getValue());
            int c = (int) Math.min(100, parse(res.getValue()));
            tile.sendPacketToServer(o -> { o.writeLong(a); o.writeLong(b); o.writeByte(c); }, TileCoreMonitor.MSG_LIMITS);
        });
    }

    // ### Options (mockup board "Pylons", bottom row)

    private void buildOptions() {
        paint(8, 60, 168, 80, 3, (r, x, y, w, h, hov) -> {
            fieldset(r, x, y, w, h, "Name and look");
            str(r, "Monitor name", x + 6, y + 12, text());
            str(r, "ETA target %", x + 6, y + 41, text());
            str(r, "(empty = full/empty)", x + 6, y + 51, muted());
        });
        nameField = input(14, 82, 156, 3, 0xFFE8E6F0, tile.name.get(), 32);
        targetField = input(120, 99, 50, 3, 0xFFFFFF55, tile.target.get() > 0 ? Integer.toString(tile.target.get()) : "", 3);
        click(14, 120, 156, 15, 3, (r, x, y, w, h, hov) -> button(r, x, y, w, h, "Save name and target", btn(), hov, false),
                () -> sendLook(nameField.getValue().trim(), -1, -1, -1, (int) Math.min(100, parse(targetField.getValue())), -1));

        paint(184, 60, 160, 80, 3, (r, x, y, w, h, hov) -> {
            fieldset(r, x, y, w, h, "Alerts");
            str(r, "Switching one on sends a TEST.", x + 6, y + 66, muted());
        });
        String[] names = {"Chat message", "Toast", "Sound", "Also to my team"};
        int[] flags = {TileCoreMonitor.ALERT_CHAT, TileCoreMonitor.ALERT_TOAST, TileCoreMonitor.ALERT_SOUND, TileCoreMonitor.ALERT_TEAM};
        for (int i = 0; i < 4; i++) {
            int flag = flags[i];
            String name = names[i];
            click(190, 72 + i * 11, 150, 10, 3, (r, x, y, w, h, hov) -> check(r, x, y, w, h, name, (tile.alerts.get() & flag) != 0, hov, false), () -> {
                boolean turningOn = (tile.alerts.get() & flag) == 0;
                tile.sendPacketToServer(o -> { o.writeInt(tile.alerts.get() ^ flag); o.writeByte(tile.redstoneMode.get()); }, TileCoreMonitor.MSG_ALERTS);
                if (turningOn && flag != TileCoreMonitor.ALERT_TEAM) tile.sendPacketToServer(o -> o.writeByte(flag), TileCoreMonitor.MSG_ALERT_TEST);
            });
        }

        paint(8, 144, 168, 36, 3, (r, x, y, w, h, hov) -> fieldset(r, x, y, w, h, "Redstone output"));
        click(14, 157, 156, 15, 3, (r, x, y, w, h, hov) -> {
            button(r, x, y, w, h, "", btn(), hov, false);
            str(r, fit(TileCoreMonitor.REDSTONE_MODES[clamp(tile.redstoneMode.get(), TileCoreMonitor.REDSTONE_MODES.length - 1)], w - 18), x + 5, y + 4, 0xFFFFFFFF);
            str(r, "\u25BC", x + w - 11, y + 4, 0xFFFFFFFF);
        }, () -> tile.sendPacketToServer(o -> { o.writeInt(tile.alerts.get()); o.writeByte((tile.redstoneMode.get() + 1) % TileCoreMonitor.REDSTONE_MODES.length); },
                TileCoreMonitor.MSG_ALERTS));

        paint(184, 144, 160, 84, 3, (r, x, y, w, h, hov) -> {
            fieldset(r, x, y, w, h, "Computer and locate");
            r.drawWordWrap(Component.literal("CC: Tweaked peripheral \"draconic_monitor\": getStored, getNet, getEta, setActive and more."), x + 6, y + 12, (int) w - 12, text());
        });
        click(190, 208, 148, 15, 3, (r, x, y, w, h, hov) -> button(r, x, y, w, h,
                        tile.isLinked() && MonitorHighlight.isOn(tile.linkDim.get(), tile.linkPos.get()) ? "Stop highlighting" : "Locate core",
                        tile.isLinked() && MonitorHighlight.isOn(tile.linkDim.get(), tile.linkPos.get()) ? 0xFF6A2FA8 : btn(), hov, !tile.isLinked()),
                () -> {
                    if (!tile.isLinked()) return;
                    if (MonitorHighlight.toggle(tile.linkDim.get(), tile.linkPos.get(), 0xFFAA00)) tile.sendPacketToServer(o -> {}, TileCoreMonitor.MSG_LOCATE);
                });
    }

    // ### Stats (styled after Logistic Networks' I/O Monitor)

    private void requestStats() {
        tile.sendPacketToServer(out -> {}, TileCoreMonitor.MSG_STATS);
    }

    private void buildStats() {
        paint(8, 60, W - 16, 176, 4, this::statsPanel);
        for (int i = 0; i < 3; i++) {
            int rg = i;
            click(W - 16 - 4 - (3 - i) * 26, 80, 24, 12, 4, (r, x, y, w, h, hov) -> {
                boolean on = statRange == rg;
                r.fill(x, y, x + w, y + h, on ? 0xFF3A3A48 : hov ? 0xFF2A2A34 : 0xFF1C1C22);
                if (on) r.fill(x, y + h - 1, x + w, y + h, 0xFF8800FF);
                String s = RANGE_SHORT[rg];
                str(r, s, x + (w - font().width(s)) / 2, y + 2, on ? 0xFFFFFFFF : 0xFF9A9AA8);
            }, () -> statRange = rg);
        }
    }

    private void statsPanel(GuiRender r, double x, double y, double w, double h, boolean hov) {
        r.fill(x, y, x + w, y + h, 0xFF2B2B33);
        r.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF17171C);
        boolean live = tile.loaded.get();
        long net = tile.input.get() - tile.output.get();
        String unit = tile.unitName();
        str(r, "Net flow", x + 8, y + 7, 0xFFFFFFFF);
        str(r, " \u00B7 Tier " + tile.tier.get() + " core", x + 8 + font().width("Net flow"), y + 7, 0xFF9A9AA8);
        String badge = live ? "\u25CF LIVE" : "\u25CF OFFLINE";
        int bw = font().width(badge) + 8;
        r.fill(x + w - 8 - bw, y + 5, x + w - 8, y + 17, live ? 0xFF16331F : 0xFF33161A);
        str(r, badge, x + w - 4 - bw, y + 7, live ? 0xFF55FF77 : 0xFFFF5555);
        String big = signed(net) + " " + unit + "/t";
        str(r, big, x + 8, y + 22, net > 0 ? 0xFF55FF55 : net < 0 ? 0xFFFF5555 : 0xFFAAAAAA);
        str(r, "Throughput / tick", x + 8 + font().width(big) + 8, y + 22, 0xFF6A6A78);

        // chart
        double cx = x + 46, cy = y + 38, cw = w - 54, ch = 86;
        r.fill(cx - 1, cy - 1, cx + cw + 1, cy + ch + 1, 0xFF2B2B33);
        r.fill(cx, cy, cx + cw, cy + ch, 0xFF101014);
        long[] d = tile.statSeries[statRange];
        long maxPos = 0, maxNeg = 0;
        for (long v : d) { maxPos = Math.max(maxPos, v); maxNeg = Math.max(maxNeg, -v); }
        long top = Math.max(maxPos, maxNeg == 0 ? 1 : 0), bottom = maxNeg;
        if (top == 0 && bottom == 0) top = 1;
        double zero = cy + ch * top / (double) (top + bottom);
        for (int g = 0; g <= 4; g++) {
            double gy = cy + ch * g / 4D;
            r.fill(cx, gy, cx + cw, gy + 1, 0xFF22222A);
            long v = top - (top + bottom) * g / 4;
            String lab = compact(tile.toUnit(v));
            str(r, lab, cx - 4 - font().width(lab), gy - 4, 0xFF6A6A78);
        }
        r.fill(cx, zero, cx + cw, zero + 1, 0xFF4A4A58);
        if (d.length == 0) {
            String msg = statRange == 0 ? "Needs the core's rate history switch" : "Collecting... (first point after a minute)";
            str(r, msg, cx + (cw - font().width(msg)) / 2, cy + ch / 2 - 12, 0xFF9A9AA8);
        } else {
            int slots = 60, n = Math.min(slots, d.length);
            double bwid = cw / slots;
            for (int i = 0; i < n; i++) {
                long v = d[d.length - n + i];
                double bx = cx + (slots - n + i) * bwid;
                double bh = Math.abs(v) / (double) (top + bottom) * ch;
                if (v > 0) r.fill(bx + 0.5, zero - bh, bx + bwid - 0.5, zero, 0xFF4CD964);
                else if (v < 0) r.fill(bx + 0.5, zero + 1, bx + bwid - 0.5, zero + 1 + bh, 0xFFFF4D4D);
            }
        }
        str(r, "-" + RANGE_SHORT[statRange], cx, cy + ch + 3, 0xFF6A6A78);
        str(r, "now", cx + cw - font().width("now"), cy + ch + 3, 0xFF6A6A78);

        // cards
        boolean none = tile.statMinutes == 0;
        String[] labels = {"Average 1 h", "Peak in", "Peak out", "Low 24 h"};
        String[] values = {none ? "-" : signed(tile.statAvg) + "/t", none ? "-" : "+" + EnergyCoreGui.shortNumber(tile.toUnit(tile.statPeakIn)) + "/t",
                none ? "-" : "-" + EnergyCoreGui.shortNumber(tile.toUnit(tile.statPeakOut)) + "/t",
                tile.statLow < 0 ? "-" : String.format(Locale.ROOT, "%.1f%%", tile.statLow)};
        double cardW = (w - 16 - 3 * 6) / 4, cardY = y + h - 34;
        for (int i = 0; i < 4; i++) {
            double kx = x + 8 + i * (cardW + 6);
            r.fill(kx, cardY, kx + cardW, cardY + 26, 0xFF2B2B33);
            r.fill(kx + 1, cardY + 1, kx + cardW - 1, cardY + 25, 0xFF1F1F26);
            str(r, labels[i], kx + 5, cardY + 4, 0xFF9A9AA8);
            str(r, fit(values[i], cardW - 10), kx + 5, cardY + 15, 0xFFFFFFFF);
        }
    }

    // ### Building blocks

    @FunctionalInterface
    private interface Painter {
        void paint(GuiRender r, double x, double y, double w, double h, boolean hover);
    }

    /** A drawn element at (x, y) relative to the window, shown on one tab (-1 = always). */
    private GuiRectangle paint(int x, int y, int w, int h, int onTab, Painter p) {
        GuiRectangle e = new GuiRectangle(root) {
            @Override
            public void renderBackground(GuiRender r, double mx, double my, float pt) {
                p.paint(r, xMin(), yMin(), xSize(), ySize(), isMouseOver());
            }
        };
        if (onTab >= 0) e.setEnabled(() -> tab == onTab);
        Constraints.size(e, w, h);
        Constraints.placeInside(e, root, Constraints.LayoutPos.TOP_LEFT, x, y);
        return e;
    }

    /** A clickable drawn element. */
    private GuiButton click(int x, int y, int w, int h, int onTab, Painter p, Runnable onPress) {
        GuiButton b = new GuiButton(root).onPress(onPress);
        if (onTab >= 0) b.setEnabled(() -> tab == onTab);
        Constraints.size(b, w, h);
        Constraints.placeInside(b, root, Constraints.LayoutPos.TOP_LEFT, x, y);
        GuiRectangle look = new GuiRectangle(b) {
            @Override
            public void renderBackground(GuiRender r, double mx, double my, float pt) {
                p.paint(r, xMin(), yMin(), xSize(), ySize(), b.isMouseOver());
            }
        };
        Constraints.bind(look, b);
        return b;
    }

    private void radios(int x, int y, int onTab, String[] labels, IntSupplier selected, IntConsumer onPick) {
        int cx = x;
        for (int i = 0; i < labels.length; i++) {
            int idx = i, w = font().width(labels[i]) + 13;
            click(cx, y, w, 10, onTab, (r, bx, by, bw, bh, hov) -> check(r, bx, by, bw, bh, labels[idx], selected.getAsInt() == idx, hov, true),
                    () -> onPick.accept(idx));
            cx += w + 6;
        }
    }

    private GuiTextField input(int x, int y, int w, int onTab, int colour, String value, int maxLength) {
        var f = GuiTextField.create(root, 0xFF0B0710, 0xFF1E1E1E, colour | 0xFF000000); // alpha byte required, or the text is invisible
        f.container().setEnabled(() -> tab == onTab);
        f.field().setMaxLength(maxLength).setTextState(TextState.simpleState(value));
        Constraints.size(f.container(), w, 12);
        Constraints.placeInside(f.container(), root, Constraints.LayoutPos.TOP_LEFT, x, y);
        return f.field();
    }

    private Slider slider(int x, int y, int w, int onTab, int accent, IntSupplier saved, IntConsumer onSet) {
        Slider s = new Slider(root, saved, accent, onSet);
        s.setEnabled(() -> tab == onTab);
        Constraints.size(s, w, 10);
        Constraints.placeInside(s, root, Constraints.LayoutPos.TOP_LEFT, x, y);
        return s;
    }

    /** 0-100 slider; sends once on release and shows the dragged value until the server confirms it. */
    private static class Slider extends GuiRectangle {
        final IntSupplier saved;
        final IntConsumer onSet;
        final int accent;
        int local = -1;
        boolean drag;

        Slider(GuiElement<?> parent, IntSupplier saved, int accent, IntConsumer onSet) {
            super(parent);
            this.saved = saved;
            this.accent = accent;
            this.onSet = onSet;
        }

        int value() {
            return local >= 0 ? local : saved.getAsInt();
        }

        void settle() {
            if (!drag && local >= 0 && saved.getAsInt() == local) local = -1;
        }

        void set(double mx) {
            local = (int) Math.round(Math.max(0, Math.min(1, (mx - xMin()) / xSize())) * 100);
        }

        @Override
        public void renderBackground(GuiRender r, double mx, double my, float pt) {
            double x = xMin(), y = yMin(), w = xSize(), h = ySize(), mid = y + h / 2;
            r.fill(x, mid - 1, x + w, mid + 1, darkMode ? 0xFF55555F : 0xFF8B8B8B);
            double fx = x + w * value() / 100D;
            r.fill(x, mid - 1, fx, mid + 1, accent);
            r.fill(fx - 3, y, fx + 3, y + h, 0xFF1E1E1E);
            r.fill(fx - 2, y + 1, fx + 2, y + h - 1, isMouseOver() || drag ? 0xFFFFFFFF : accent);
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (button == 0 && isMouseOver()) {
                drag = true;
                set(mx);
                return true;
            }
            return super.mouseClicked(mx, my, button);
        }

        @Override
        public void mouseMoved(double mx, double my) {
            if (drag) set(mx);
            super.mouseMoved(mx, my);
        }

        @Override
        public boolean mouseReleased(double mx, double my, int button, boolean consumed) {
            if (drag) {
                drag = false;
                onSet.accept(local);
            }
            return super.mouseReleased(mx, my, button, consumed);
        }
    }

    // ### Drawing helpers (mockup colours; dark variants follow BC's theme)

    private static int panelBg() { return darkMode ? 0xFF2B2B33 : 0xFFC6C6C6; }
    private static int outline() { return 0xFF1E1E1E; }
    private static int text() { return darkMode ? 0xFFE0E0E0 : 0xFF303030; }
    private static int head() { return darkMode ? 0xFFEDE6FF : 0xFF3F3F3F; }
    private static int muted() { return darkMode ? 0xFF9A9AA8 : 0xFF4A4A4A; }
    private static int fieldCol() { return darkMode ? 0xFF55555F : 0xFF8B8B8B; }
    private static int btn() { return darkMode ? 0xFF4A4A56 : 0xFF8B8B8B; }
    private static int subBg() { return darkMode ? 0xFF22222A : 0xFFD6D6D6; }

    private static Font font() {
        return Minecraft.getInstance().font;
    }

    private static void str(GuiRender r, String s, double x, double y, int colour) {
        r.drawString(s, x, y, colour, false);
    }

    private static String fit(String s, double w) {
        if (font().width(s) <= w) return s;
        return font().plainSubstrByWidth(s, (int) Math.max(0, w - font().width("..."))) + "...";
    }

    private static int clamp(int v, int max) {
        return Math.max(0, Math.min(max, v));
    }

    private String signed(long op) {
        return (op > 0 ? "+" : "") + EnergyCoreGui.shortNumber(tile.toUnit(op));
    }

    private static int mixCol(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return 0xFF000000 | r << 16 | g << 8 | bl;
    }

    private static int lighten(int c) { return mixCol(c, 0xFFFFFF, 0.22F); }
    private static int darken(int c) { return mixCol(c, 0x000000, 0.35F); }

    /** The window: dark outline, grey body, light top-left and dark bottom-right bevel. */
    private static void panel(GuiRender r, double x, double y, double w, double h) {
        r.fill(x, y, x + w, y + h, outline());
        r.fill(x + 1, y + 1, x + w - 1, y + h - 1, panelBg());
        int hi = darkMode ? 0xFF45454F : 0xFFFFFFFF, lo = darkMode ? 0xFF17171C : 0xFF555555;
        r.fill(x + 1, y + 1, x + w - 1, y + 3, hi);
        r.fill(x + 1, y + 3, x + 3, y + h - 1, hi);
        r.fill(x + 3, y + h - 3, x + w - 1, y + h - 1, lo);
        r.fill(x + w - 3, y + 3, x + w - 1, y + h - 3, lo);
    }

    /** A grey-bordered box with its title sitting in the top line, like an HTML fieldset. */
    private static void fieldset(GuiRender r, double x, double y, double w, double h, String legend) {
        double top = y + 4;
        int c = fieldCol();
        r.fill(x, top, x + w, top + 1, c);
        r.fill(x, y + h - 1, x + w, y + h, c);
        r.fill(x, top, x + 1, y + h, c);
        r.fill(x + w - 1, top, x + w, y + h, c);
        int tw = font().width(legend);
        r.fill(x + 5, y, x + 11 + tw, y + 9, panelBg());
        str(r, legend, x + 8, y, head());
    }

    private static void button(GuiRender r, double x, double y, double w, double h, String label, int bg, boolean hover, boolean disabled) {
        int base = disabled ? (darkMode ? 0xFF33333A : 0xFFA6A6A6) : hover ? lighten(bg) : bg;
        r.fill(x, y, x + w, y + h, outline());
        r.fill(x + 1, y + 1, x + w - 1, y + h - 1, base);
        r.fill(x + 1, y + 1, x + w - 1, y + 2, lighten(base));
        r.fill(x + 1, y + 2, x + 2, y + h - 1, lighten(base));
        r.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, darken(base));
        r.fill(x + w - 2, y + 2, x + w - 1, y + h - 2, darken(base));
        if (label.isEmpty()) return;
        String l = fit(label, w - 6);
        str(r, l, x + (w - font().width(l)) / 2, y + (h - 8) / 2, disabled ? 0xFF7A7A7A : 0xFFFFFFFF);
    }

    /** Checkbox (or radio button) with its label; purple when on, like the mockup's accent colour. */
    private static void check(GuiRender r, double x, double y, double w, double h, String label, boolean on, boolean hover, boolean radio) {
        double by = y + (h - 8) / 2;
        r.fill(x, by, x + 8, by + 8, hover ? 0xFF8800FF : outline());
        r.fill(x + 1, by + 1, x + 7, by + 7, darkMode ? 0xFF15151A : 0xFFFFFFFF);
        if (on) r.fill(x + 2, by + 2, x + 6, by + 6, 0xFF8800FF);
        if (radio) { // round it off a little
            int bg = panelBg();
            r.fill(x, by, x + 1, by + 1, bg);
            r.fill(x + 7, by, x + 8, by + 1, bg);
            r.fill(x, by + 7, x + 1, by + 8, bg);
            r.fill(x + 7, by + 7, x + 8, by + 8, bg);
        }
        str(r, fit(label, w - 12), x + 11, y + (h - 8) / 2, text());
    }

    /** Dark card with a coloured border (status boxes, pylon rows, tier cards). */
    private static void card(GuiRender r, double x, double y, double w, double h, int border) {
        r.fill(x, y, x + w, y + h, border);
        r.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF0B0710);
    }

    /** Red teardrop map pin with a white dot, drawn in a 14x15 button. */
    private static void pin(GuiRender r, double x, double y) {
        int c = 0xFFFF5555, d = 0xFF9A2A2A;
        r.fill(x + 5, y + 2, x + 9, y + 3, c);
        r.fill(x + 4, y + 3, x + 10, y + 8, c);
        r.fill(x + 5, y + 8, x + 9, y + 9, c);
        r.fill(x + 6, y + 9, x + 8, y + 11, c);
        r.fill(x + 6.5, y + 11, x + 7.5, y + 12, c);
        r.fill(x + 9, y + 4, x + 10, y + 8, d); // shade on the right
        r.fill(x + 6, y + 4, x + 8, y + 6, 0xFFFFFFFF);
    }

    /** -1000 -> "-1k", 1500 -> "1.5k"; millions and up like elsewhere (271.781M). */
    private static String compact(long v) {
        long a = Math.abs(v);
        if (a < 1000) return Long.toString(v);
        if (a < 1_000_000) return String.format(Locale.ROOT, "%.1fk", v / 1000D).replace(".0k", "k");
        return EnergyCoreGui.shortNumber(v);
    }

    private static long parse(String s) {
        try {
            String d = s.replace(",", "").trim();
            return d.isEmpty() ? 0 : Math.max(0, Long.parseLong(d));
        } catch (NumberFormatException e) {
            return Long.MAX_VALUE;
        }
    }

    /** Width x height of the screen this panel draws (same scan as the renderer). */
    private int[] screenSize() {
        Level level = tile.getLevel();
        if (level == null) return new int[]{1, 1};
        Direction facing = tile.getBlockState().getValue(CoreMonitor.FACING), right = facing.getCounterClockWise();
        BlockPos pos = tile.getBlockPos();
        int w = 1, h = 1;
        while (w < TileCoreMonitor.MAX_SIZE && isPanel(level, pos.relative(right, w), facing)) w++;
        while (h < TileCoreMonitor.MAX_SIZE && isPanel(level, pos.above(h), facing)) h++;
        return new int[]{w, h};
    }

    private static boolean isPanel(Level level, BlockPos pos, Direction facing) {
        var s = level.getBlockState(pos);
        return s.getBlock() instanceof CoreMonitor && s.getValue(CoreMonitor.FACING) == facing;
    }

    @SuppressWarnings("unused")
    private static String limitText(long v) {
        return v <= 0 ? "no limit" : Utils.addCommas(v) + "/t";
    }
}
