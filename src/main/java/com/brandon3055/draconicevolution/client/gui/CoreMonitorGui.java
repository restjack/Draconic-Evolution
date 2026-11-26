package com.brandon3055.draconicevolution.client.gui;

import com.brandon3055.brandonscore.client.BCGuiTextures;
import com.brandon3055.brandonscore.client.gui.GuiToolkit;
import com.brandon3055.brandonscore.client.gui.modular.ModularGui;
import com.brandon3055.brandonscore.client.gui.modular.ModularGuiContainer;
import com.brandon3055.brandonscore.client.gui.modular.lib.container.ContainerGuiProvider;
import com.brandon3055.brandonscore.client.gui.modular.lib.container.ContainerScreenAccess;
import com.brandon3055.draconicevolution.inventory.CoreMonitorMenu;
import com.brandon3055.brandonscore.client.gui.modular.elements.GuiButton;
import com.brandon3055.brandonscore.client.gui.modular.elements.GuiElement;
import com.brandon3055.brandonscore.client.gui.modular.elements.GuiManipulable;
import com.brandon3055.brandonscore.client.gui.modular.elements.GuiText;
import com.brandon3055.brandonscore.client.gui.modular.elements.GuiTextField;
import com.brandon3055.brandonscore.client.gui.modular.elements.GuiTexture;
import com.brandon3055.brandonscore.client.gui.modular.lib.Constraints;
import com.brandon3055.brandonscore.client.gui.modular.lib.geometry.Align;
import com.brandon3055.brandonscore.multiblock.EmptyPart;
import com.brandon3055.brandonscore.multiblock.MultiBlockDefinition;
import com.brandon3055.brandonscore.multiblock.MultiBlockManager;
import com.brandon3055.brandonscore.multiblock.MultiBlockPart;
import com.brandon3055.brandonscore.utils.Utils;
import com.brandon3055.draconicevolution.DEConfig;
import com.brandon3055.draconicevolution.DraconicEvolution;
import com.brandon3055.draconicevolution.blocks.tileentity.TileCoreMonitor;
import com.brandon3055.draconicevolution.blocks.tileentity.TileEnergyCore;
import com.brandon3055.draconicevolution.init.DEContent;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static net.minecraft.ChatFormatting.*;

/**
 * eli.8 Core Monitor settings (Round B). Tabs: Display (which lines the screen shows), Control (activate/deactivate,
 * next-tier preview), Pylons (list + remote mode switch), Limits (max in/out per tick, reserve %).
 * Everything is sent to the server, which checks the player is the monitor's owner or an operator.
 */
public class CoreMonitorGui extends ContainerGuiProvider<CoreMonitorMenu> {
    private static final GuiToolkit TOOLKIT = new GuiToolkit("gui.draconicevolution.core_monitor");
    private static final int W = 236, H = 200, PYLON_ROWS = 8;
    private static final String[] TABS = {"Display", "Control", "Pylons", "Limits"};
    private static int tab = 0; // remembered while the game runs

    private TileCoreMonitor tile;
    private int previewTier = -1, ticks;

    public static class Screen extends ModularGuiContainer<CoreMonitorMenu> {
        public Screen(CoreMonitorMenu menu, net.minecraft.world.entity.player.Inventory inv, Component title) {
            super(menu, inv, new CoreMonitorGui());
        }
    }

    @Override
    public GuiElement<?> createRootElement(ModularGui gui) {
        GuiManipulable root = new GuiManipulable(gui).addMoveHandle(3).enableCursors(true);
        GuiTexture bg = new GuiTexture(root.getContentElement(), BCGuiTextures.themedGetter("background_dynamic")).dynamicTexture();
        Constraints.bind(bg, root.getContentElement());
        return root;
    }

    @Override
    public void buildGui(ModularGui gui, ContainerScreenAccess<CoreMonitorMenu> access) {
        tile = access.getMenu().tile;
        gui.initStandardGui(W, H);
        gui.setGuiTitle(Component.literal("Core Monitor"));
        GuiElement<?> root = gui.getRoot();
        gui.renderScreenBackground(true);

        GuiText title = TOOLKIT.createHeading(root, Component.empty(), true).setTextSupplier(this::titleText);

        for (int i = 0; i < TABS.length; i++) {
            int t = i;
            GuiButton b = TOOLKIT.createFlat3DButton(root, () -> Component.literal(TABS[t]))
                    .setToggleMode(() -> tab == t)
                    .onPress(() -> { tab = t; if (t == 2) requestPylons(); });
            Constraints.size(b, 54, 14);
            Constraints.placeInside(b, root, Constraints.LayoutPos.TOP_LEFT, 6 + i * 56, 16);
        }

        if (!DEConfig.extra("coreMonitor")) {
            text(root, 6, 40, -1, () -> Component.literal("The Core Monitor is switched off in the server config.").withStyle(RED));
            return;
        }
        buildDisplay(root);
        buildControl(root);
        buildPylons(root);
        buildLimits(root);

        text(root, 6, H - 12, -1, () -> Component.literal(tile.owner.isNull() ? "No owner: anyone can change settings"
                : "Owner: " + tile.ownerName.get() + " (and operators)").withStyle(DARK_GRAY));

        gui.onTick(() -> {
            if (tab == 2 && ++ticks % 20 == 0) requestPylons();
        });
        if (tab == 2) requestPylons();
    }

    private Component titleText() {
        if (!tile.isLinked()) return Component.literal("Core Monitor - not linked").withStyle(RED);
        var p = tile.linkPos.get();
        return Component.literal("Core Monitor - core at " + p.getX() + " " + p.getY() + " " + p.getZ());
    }

    // ### Display

    private static final String[] SHOW_NAMES = {"Tier header", "Status word", "Percent", "Fill bar", "Stored / capacity", "In and out", "Net rate", "Time to full/empty"};

    private void buildDisplay(GuiElement<?> root) {
        for (int i = 0; i < SHOW_NAMES.length; i++) {
            int flag = 1 << i;
            String name = SHOW_NAMES[i];
            GuiButton b = TOOLKIT.createFlat3DButton(root, () -> Component.literal((tile.isShown(flag) ? "[x] " : "[ ] ") + name))
                    .setToggleMode(() -> tile.isShown(flag))
                    .onPress(() -> tile.sendPacketToServer(out -> out.writeInt(tile.show.get() ^ flag), TileCoreMonitor.MSG_SHOW))
                    .setEnabled(() -> tab == 0);
            Constraints.size(b, 110, 14);
            Constraints.placeInside(b, root, Constraints.LayoutPos.TOP_LEFT, 6 + (i % 2) * 114, 38 + (i / 2) * 17);
        }
        text(root, 6, 110, 0, () -> Component.literal("Applies to every panel of this screen.").withStyle(GRAY));
        text(root, 6, 122, 0, () -> Component.literal("A 1x1 panel only shows status, percent, bar and net.").withStyle(GRAY));
    }

    // ### Control

    private void buildControl(GuiElement<?> root) {
        text(root, 6, 38, 1, this::coreStatus);
        GuiButton toggle = TOOLKIT.createFlat3DButton(root, () -> Component.literal(tile.coreActive.get() ? "Deactivate core" : "Activate core"))
                .setDisabled(() -> !tile.isLinked() || !tile.loaded.get())
                .onPress(() -> tile.sendPacketToServer(out -> {}, TileCoreMonitor.MSG_TOGGLE_CORE))
                .setTooltipSingle(() -> Component.literal("Same checks as the core's own button. Stored energy is kept."))
                .setEnabled(() -> tab == 1);
        Constraints.size(toggle, W - 12, 14);
        Constraints.placeInside(toggle, root, Constraints.LayoutPos.TOP_LEFT, 6, 50);

        text(root, 6, 72, 1, () -> Component.literal("Next tier preview (upgrade at the core)").withStyle(DARK_AQUA));
        GuiButton prev = TOOLKIT.createFlat3DButton(root, () -> Component.literal("<"))
                .onPress(() -> previewTier = Math.max(1, preview() - 1)).setEnabled(() -> tab == 1);
        Constraints.size(prev, 14, 12);
        Constraints.placeInside(prev, root, Constraints.LayoutPos.TOP_LEFT, W - 38, 70);
        GuiButton next = TOOLKIT.createFlat3DButton(root, () -> Component.literal(">"))
                .onPress(() -> previewTier = Math.min(TileEnergyCore.MAX_TIER, preview() + 1)).setEnabled(() -> tab == 1);
        Constraints.size(next, 14, 12);
        Constraints.placeInside(next, root, Constraints.LayoutPos.TOP_LEFT, W - 22, 70);

        for (int i = 0; i < 8; i++) {
            int line = i;
            text(root, 6, 86 + i * 11, 1, () -> previewLine(line));
        }
    }

    private Component coreStatus() {
        if (!tile.isLinked()) return Component.literal("Not linked. Shift-click a core with a Crystal Binder, then this monitor.").withStyle(RED);
        if (!tile.loaded.get()) return Component.literal("Core not loaded (its chunk is unloaded).").withStyle(GRAY);
        MutableComponent c = Component.literal("Tier " + tile.tier.get() + " core: ").withStyle(GRAY);
        c.append(tile.coreActive.get() ? Component.literal("ACTIVE").withStyle(GREEN) : Component.literal("INACTIVE").withStyle(RED));
        if (!tile.stabilizersOk.get()) c.append(Component.literal(", stabilizers not valid").withStyle(RED));
        return c;
    }

    private int preview() {
        if (previewTier < 1) previewTier = Math.min(TileEnergyCore.MAX_TIER, Math.max(1, tile.tier.get() + 1));
        return previewTier;
    }

    private Component previewLine(int line) {
        int cur = Math.max(1, tile.tier.get()), t = preview();
        if (line == 0) {
            long cap = capacity(t), now = capacity(cur);
            String capText = cap < 0 ? "unlimited" : EnergyCoreGui.shortNumber(cap) + " " + DEConfig.energyUnit();
            String ratio = cap < 0 || now <= 0 || t == cur ? "" : String.format(java.util.Locale.ROOT, " (x%.1f vs tier %d)", cap / (double) now, cur);
            return Component.literal("Tier " + t + (t == cur ? " (current)" : "") + ": ").withStyle(GOLD)
                    .append(Component.literal(capText + ratio).withStyle(WHITE));
        }
        if (line == 1) {
            boolean adv = t >= TileEnergyCore.ADV_STABILIZER_TIER;
            return Component.literal("Stabilizers: " + (adv ? "4 advanced (3x3)" : "4 basic")).withStyle(GRAY);
        }
        List<Map.Entry<Block, Integer>> need = new ArrayList<>(blockCounts(t).entrySet());
        int idx = line - 2;
        if (idx >= need.size()) return Component.empty();
        Map<Block, Integer> have = blockCounts(cur);
        var e = need.get(idx);
        int diff = e.getValue() - have.getOrDefault(e.getKey(), 0);
        MutableComponent c = e.getKey().getName().copy().withStyle(GRAY).append(Component.literal(": " + e.getValue()).withStyle(WHITE));
        if (t != cur && diff != 0) c.append(Component.literal(" (" + (diff > 0 ? "+" : "") + diff + ")").withStyle(diff > 0 ? YELLOW : GREEN));
        return c;
    }

    private static long capacity(int tier) {
        Long[] caps = DEConfig.coreCapacity;
        return tier < 1 || tier > caps.length ? 0 : caps[tier - 1];
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

    // ### Pylons

    private void requestPylons() {
        tile.sendPacketToServer(out -> {}, TileCoreMonitor.MSG_PYLONS);
    }

    private void buildPylons(GuiElement<?> root) {
        text(root, 6, 38, 2, () -> tile.clientPylons.isEmpty()
                ? Component.literal(tile.loaded.get() ? "No pylons linked to this core." : "Core not loaded.").withStyle(GRAY)
                : Component.literal(tile.clientPylons.size() + " pylon(s). Click a mode to switch it.").withStyle(DARK_AQUA));
        for (int i = 0; i < PYLON_ROWS; i++) {
            int row = i;
            text(root, 6, 54 + i * 15, 2, () -> {
                var r = pylon(row);
                if (r == null) return Component.empty();
                String rate = (r.input() ? "+" : "-") + EnergyCoreGui.shortNumber(r.rate()) + " " + DEConfig.energyUnit() + "/t";
                return Component.literal(r.pos().getX() + " " + r.pos().getY() + " " + r.pos().getZ() + "  ").withStyle(GRAY)
                        .append(Component.literal(rate).withStyle(r.rate() == 0 ? GRAY : r.input() ? GREEN : RED));
            });
            GuiButton b = TOOLKIT.createFlat3DButton(root, () -> {
                        var r = pylon(row);
                        return r == null ? Component.empty() : Component.literal(r.input() ? "INPUT" : "OUTPUT").withStyle(r.input() ? GREEN : RED);
                    })
                    .onPress(() -> {
                        var r = pylon(row);
                        if (r != null) tile.sendPacketToServer(out -> out.writePos(r.pos()), TileCoreMonitor.MSG_PYLON_FLIP);
                    })
                    .setTooltipSingle(() -> Component.literal("Input = charging the core, output = draining it"))
                    .setEnabled(() -> tab == 2 && pylon(row) != null);
            Constraints.size(b, 54, 13);
            Constraints.placeInside(b, root, Constraints.LayoutPos.TOP_LEFT, W - 60, 51 + i * 15);
        }
        text(root, 6, 54 + PYLON_ROWS * 15, 2, () -> tile.clientPylons.size() > PYLON_ROWS
                ? Component.literal("...and " + (tile.clientPylons.size() - PYLON_ROWS) + " more").withStyle(GRAY) : Component.empty());
    }

    private TileCoreMonitor.PylonRow pylon(int row) {
        var list = tile.clientPylons;
        return row < list.size() ? list.get(row) : null;
    }

    // ### Limits

    private void buildLimits(GuiElement<?> root) {
        text(root, 6, 38, 3, () -> Component.literal("Limits for the whole core, shared by all its pylons.").withStyle(DARK_AQUA));
        text(root, 6, 50, 3, () -> Component.literal("Empty or 0 = no limit.").withStyle(GRAY));
        GuiTextField in = field(root, "Max input per tick (" + DEConfig.energyUnit() + ")", 66, tile.limIn.get());
        GuiTextField out = field(root, "Max output per tick (" + DEConfig.energyUnit() + ")", 86, tile.limOut.get());
        GuiTextField res = field(root, "Reserve % (no output below it)", 106, tile.limReserve.get());
        res.setMaxLength(3);
        GuiButton save = TOOLKIT.createFlat3DButton(root, () -> Component.literal("Save limits"))
                .setDisabled(() -> !tile.isLinked() || !tile.loaded.get())
                .onPress(() -> {
                    long a = parse(in.getValue()), b = parse(out.getValue());
                    int r = (int) Math.min(100, parse(res.getValue()));
                    tile.sendPacketToServer(o -> { o.writeLong(a); o.writeLong(b); o.writeByte(r); }, TileCoreMonitor.MSG_LIMITS);
                })
                .setEnabled(() -> tab == 3);
        Constraints.size(save, W - 12, 14);
        Constraints.placeInside(save, root, Constraints.LayoutPos.TOP_LEFT, 6, 126);
        text(root, 6, 146, 3, () -> Component.literal("Now: in " + limitText(tile.limIn.get()) + ", out " + limitText(tile.limOut.get())
                + ", reserve " + tile.limReserve.get() + "%").withStyle(GRAY));
        text(root, 6, 158, 3, () -> Component.literal("Off if the server turns the Core Monitor off.").withStyle(DARK_GRAY));
    }

    private GuiTextField field(GuiElement<?> root, String label, int y, long value) {
        text(root, 6, y + 2, 3, () -> Component.literal(label).withStyle(GRAY));
        var f = GuiTextField.create(root, 0xFF000000, 0xFFA0A0A0, 0xE0E0E0);
        f.container().setEnabled(() -> tab == 3);
        f.field().setMaxLength(19).setFilter(s -> s.chars().allMatch(c -> Character.isDigit(c) || c == ','))
                .setValue(value > 0 ? Long.toString(value) : "");
        Constraints.size(f.container(), 70, 12);
        Constraints.placeInside(f.container(), root, Constraints.LayoutPos.TOP_LEFT, W - 76, y);
        return f.field();
    }

    private static long parse(String s) {
        try {
            String d = s.replace(",", "").trim();
            return d.isEmpty() ? 0 : Math.max(0, Long.parseLong(d));
        } catch (NumberFormatException e) {
            return Long.MAX_VALUE; // more digits than a long: same as no practical limit
        }
    }

    private static String limitText(long v) {
        return v <= 0 ? "no limit" : Utils.addCommas(v) + "/t";
    }

    /** A left-aligned line of text shown on one tab (tab -1 = always). */
    private static GuiText text(GuiElement<?> root, int x, int y, int onTab, java.util.function.Supplier<Component> supplier) {
        GuiText t = new GuiText(root, supplier).setAlignment(Align.MIN).setShadow(false);
        if (onTab >= 0) t.setEnabled(() -> tab == onTab);
        Constraints.size(t, W - x - 6, 9);
        Constraints.placeInside(t, root, Constraints.LayoutPos.TOP_LEFT, x, y);
        return t;
    }
}
