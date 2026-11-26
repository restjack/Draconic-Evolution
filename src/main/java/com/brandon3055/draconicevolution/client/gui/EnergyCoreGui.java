package com.brandon3055.draconicevolution.client.gui;

import codechicken.lib.math.MathHelper;

import com.brandon3055.brandonscore.api.power.IOInfo;

import com.brandon3055.brandonscore.client.BCGuiTextures;

import com.brandon3055.brandonscore.client.gui.GuiToolkit;

import com.brandon3055.brandonscore.client.gui.modular.ModularGui;

import com.brandon3055.brandonscore.client.gui.modular.ModularGuiContainer;

import com.brandon3055.brandonscore.client.gui.modular.elements.*;

import com.brandon3055.brandonscore.client.gui.modular.lib.ColourState;

import com.brandon3055.brandonscore.client.gui.modular.lib.Constraints;

import com.brandon3055.brandonscore.client.gui.modular.lib.GuiRender;

import com.brandon3055.brandonscore.client.gui.modular.lib.TextState;

import com.brandon3055.brandonscore.client.gui.modular.lib.container.ContainerGuiProvider;

import com.brandon3055.brandonscore.client.gui.modular.lib.container.ContainerScreenAccess;

import com.brandon3055.brandonscore.client.gui.modular.lib.geometry.Direction;

import com.brandon3055.brandonscore.client.gui.modular.lib.geometry.GuiParent;

import com.brandon3055.brandonscore.client.gui.modulargui.ShaderEnergyBar;

import com.brandon3055.brandonscore.client.gui.modulargui.templates.ButtonRow;

import com.brandon3055.brandonscore.utils.Utils;

import com.brandon3055.draconicevolution.blocks.tileentity.TileEnergyCore;

import com.brandon3055.draconicevolution.client.DEGuiTextures;

import com.brandon3055.draconicevolution.inventory.EnergyCoreMenu;

import java.math.BigDecimal;

import java.math.RoundingMode;

import java.util.Locale;

import java.util.concurrent.atomic.AtomicBoolean;

import java.util.function.Supplier;

import net.minecraft.client.Minecraft;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import net.minecraft.world.entity.player.Inventory;

import org.jetbrains.annotations.NotNull;

import static com.brandon3055.brandonscore.BCConfig.darkMode;

import static com.brandon3055.brandonscore.client.gui.modular.lib.geometry.Constraint.*;

import static com.brandon3055.brandonscore.client.gui.modular.lib.geometry.GeoParam.*;

import static net.minecraft.ChatFormatting.*;





/**
 * Created by brandon3055 on 7/4/2016.
 */
public class EnergyCoreGui extends ContainerGuiProvider<EnergyCoreMenu> {
    private static final GuiToolkit TOOLKIT = new GuiToolkit("gui.draconicevolution.energy_core");
    private final AtomicBoolean colourSelectMode;
    public static final int GUI_WIDTH = 180;
    public static final int GUI_HEIGHT = 200;

    public EnergyCoreGui(AtomicBoolean hideJEI) {
        this.colourSelectMode = hideJEI;
    }

    @Override
    public GuiElement<?> createRootElement(ModularGui gui) {
        GuiManipulable root = new GuiManipulable(gui).addMoveHandle(3).enableCursors(true);
        GuiTexture bg = new GuiTexture(root.getContentElement(), DEGuiTextures.themedGetter("energy_core"))
                .setEnabled(() -> !colourSelectMode.get());
        Constraints.bind(bg, root.getContentElement());
        return root;
    }

    @Override
    public void buildGui(ModularGui gui, ContainerScreenAccess<EnergyCoreMenu> screenAccess) {
        gui.initStandardGui(GUI_WIDTH, GUI_HEIGHT);
        EnergyCoreMenu menu = screenAccess.getMenu();
        TileEnergyCore tile = menu.tile;
        GuiElement<?> actualRoot = gui.getRoot();
        GuiElement<?> root = new GuiElement<>(actualRoot)
                .setEnabled(() -> !colourSelectMode.get());
        Constraints.bind(root, actualRoot);

        GuiText title = TOOLKIT.createHeading(root, gui.getGuiTitle(), true).setTextSupplier(() -> TOOLKIT.translate("title", tile.tier.get()));
        gui.renderScreenBackground(!colourSelectMode.get());

        ButtonRow buttonRow = ButtonRow.topRightInside(root, Direction.DOWN, 3, 3).setSpacing(1);
        buttonRow.addButton(TOOLKIT::createThemeButton);

        var playInv = GuiSlots.player(root, screenAccess, menu.main, menu.hotBar);
        playInv.stream().forEach(e -> e.setSlotTexture(slot -> BCGuiTextures.getThemed("slot")));
        Constraints.placeInside(playInv.container(), root, Constraints.LayoutPos.BOTTOM_CENTER, 0, -7);
        GuiText slotsTitle = TOOLKIT.playerInvTitle(playInv.container());

        //Buttons
        GuiButton activate = TOOLKIT.createFlat3DButton(root, () -> TOOLKIT.translate(tile.active.get() ? "deactivate" : "activate"))
                .setEnabled(() -> !tile.active.get() && tile.isStructureValid())
                .onPress(() -> tile.sendPacketToServer(e -> {}, TileEnergyCore.MSG_TOGGLE_ACTIVATION));
        Constraints.size(activate, 18 * 9, 14);
        Constraints.placeOutside(activate, playInv.container(), Constraints.LayoutPos.TOP_CENTER, 0, -14);

        GuiButton tierDown = TOOLKIT.createFlat3DButton(root, () -> TOOLKIT.translate("tier_down"))
                .setEnabled(() -> !tile.active.get())
                .setDisabled(() -> tile.tier.get() <= 1)
                .onPress(tile.tier::dec);
        Constraints.size(tierDown, ((18 * 9) / 2D) - 1, 14);
        Constraints.placeInside(tierDown, activate, Constraints.LayoutPos.TOP_LEFT, 0, -15);

        GuiButton tierUp = TOOLKIT.createFlat3DButton(root, () -> TOOLKIT.translate("tier_up"))
                .setEnabled(() -> !tile.active.get())
                .setDisabled(() -> tile.tier.get() >= TileEnergyCore.MAX_TIER)
                .onPress(tile.tier::inc);
        Constraints.size(tierUp, ((18 * 9) / 2D) - 1, 14);
        Constraints.placeInside(tierUp, activate, Constraints.LayoutPos.TOP_RIGHT, 0, -15);

        GuiButton buildGuide = TOOLKIT.createFlat3DButton(root, () -> TOOLKIT.translate("build_guide"))
                .setToggleMode(tile.buildGuide::get)
                .onPress(tile.buildGuide::invert)
                .setEnabled(() -> !tile.active.get());
        Constraints.size(buildGuide, 18 * 9, 14);
        Constraints.placeOutside(buildGuide, activate, Constraints.LayoutPos.TOP_CENTER, 0, -16);

        GuiButton assemble = TOOLKIT.createFlat3DButton(root, () -> TOOLKIT.translate("assemble"))
                .onPress(() -> tile.sendPacketToServer(e -> {}, TileEnergyCore.MSG_BUILD_CORE))
                .setEnabled(() -> !tile.isStructureValid());
        Constraints.bind(assemble, activate);

        GuiButton disable = TOOLKIT.createThemedIconButton(root, "pwr_btn")
                .setEnabled(tile.active::get)
                .onPress(() -> tile.sendPacketToServer(e -> {}, TileEnergyCore.MSG_TOGGLE_ACTIVATION))
                .setTooltip(TOOLKIT.translate("deactivate"));
        Constraints.placeInside(disable, root, Constraints.LayoutPos.TOP_LEFT, 3, 3);

        //Labels
        GuiText coreInvalidLabel = TOOLKIT.createHeading(root, TOOLKIT.translate("core_invalid").withStyle(RED))
                .setEnabled(() -> !tile.active.get() && !tile.coreValid.get());
        Constraints.size(coreInvalidLabel, playInv.container().xSize(), 8);
        Constraints.placeOutside(coreInvalidLabel, title, Constraints.LayoutPos.BOTTOM_CENTER, 0, 5);

        // eli.8 (upstream TODO in TileEnergyCore.isCoreValidForTier): say which blocks are wrong; details on hover.
        GuiText badCount = new GuiText(root).setTextSupplier(() -> structureCount(tile))
                .setTooltip(() -> structureDetails(tile), 0)
                .setEnabled(() -> !tile.active.get() && !tile.coreValid.get());
        Constraints.size(badCount, playInv.container().xSize(), 8);
        Constraints.placeOutside(badCount, coreInvalidLabel, Constraints.LayoutPos.BOTTOM_CENTER, 0, 5);

        GuiText stabInvalidLabel = TOOLKIT.createHeading(root, TOOLKIT.translate("stabilizers_invalid").withStyle(RED))
                .setEnabled(() -> !tile.active.get() && !tile.stabilizersValid.get());
        Constraints.size(stabInvalidLabel, playInv.container().xSize(), 8);
        Constraints.placeOutside(stabInvalidLabel, badCount, Constraints.LayoutPos.BOTTOM_CENTER, 0, 5);

        GuiText stabAdvLabel = TOOLKIT.createHeading(root, TOOLKIT.translate("stabilizers_advanced"))
                .setEnabled(() -> !tile.active.get() && !tile.stabilizersValid.get() && tile.reqAdvStabilizers());
        Constraints.size(stabAdvLabel, playInv.container().xSize(), 8);
        Constraints.placeOutside(stabAdvLabel, stabInvalidLabel, Constraints.LayoutPos.BOTTOM_CENTER, 0, 5);


        //Display
        GuiRectangle display = toolTipBackground(root, 0xF0100010, () -> tile.tier.get() == 8 ? 0xFFFF5500 : 0xFF8800FF)
                .setEnabled(tile.active::get)
                .constrain(LEFT, match(playInv.container().get(LEFT)))
                .constrain(RIGHT, match(playInv.container().get(RIGHT)))
                .constrain(TOP, relative(title.get(BOTTOM), 3))
                .constrain(BOTTOM, relative(slotsTitle.get(TOP), -2));


        GuiRectangle barSlot = new GuiRectangle(display)
                .shadedRect(() -> darkMode ? 0xFF808080 : 0xFF505050, () -> 0xFFFFFFFF, () -> 0);
        Constraints.size(barSlot, display.xSize() - 6, 14);
        Constraints.placeInside(barSlot, display, Constraints.LayoutPos.BOTTOM_CENTER, 0, -3);

        GuiEnergyBar energyBar = new ShaderEnergyBar(display)
                .setCapacity(() -> 1000000L)
                .setEnergy(() -> (long) (getEnergyDouble(tile) * 1000000))
                .setTooltipSingle(() -> {
                    int p1000 = (int) (getEnergyDouble(tile) * 100000);
                    return Component.translatable("mod_gui.brandonscore.energy_bar.stored").withStyle(GOLD).append(": ").append(Component.literal(tile.energy.getScientific() + " (" + (p1000 / 1000D) + "%)").withStyle(GRAY));
                });
        Constraints.bind(energyBar, barSlot, 1);

        GuiText opLabel = new GuiText(display).setTextSupplier(() -> com.brandon3055.draconicevolution.DEConfig.showEnergyAsRF ? Component.literal("Stored Energy").withStyle(DARK_AQUA) : Component.translatable("mod_gui.brandonscore.energy_bar.operational_potential").withStyle(DARK_AQUA));
        Constraints.size(opLabel, display.xSize(), 9);
        Constraints.placeInside(opLabel, display, Constraints.LayoutPos.TOP_CENTER, 0, 5);

        // eli.7: six lines, same spacing as upstream: stored / capacity, then In and Out, Net, and time to full/empty.
        GuiText energy = new GuiText(display)
                .setTextSupplier(() -> {
                    // eli.8: short prefixes (271.781M / 273M) like other mods; tier 8 keeps DE's own formatter (beyond a long).
                    MutableComponent c = Component.literal(tile.energy.isUnlimited() ? tile.energy.getReadable().getString().trim()
                            : shortNumber(tile.energy.getOPStored())).withStyle(GOLD);
                    if (!tile.energy.isUnlimited()) c.append(Component.literal(" / " + shortNumber(tile.energy.getMaxOPStored())).withStyle(GRAY));
                    return c.append(Component.literal(" " + com.brandon3055.draconicevolution.DEConfig.energyUnit()).withStyle(tile.energy.isUnlimited() ? GOLD : GRAY));
                });
        Constraints.size(energy, display.xSize(), 9);
        Constraints.placeOutside(energy, opLabel, Constraints.LayoutPos.BOTTOM_CENTER, 0, 2);

        GuiText ioLabel = new GuiText(display, Component.translatable("mod_gui.brandonscore.energy_bar.io").withStyle(DARK_AQUA));
        Constraints.size(ioLabel, display.xSize(), 9);
        Constraints.placeOutside(ioLabel, energy, Constraints.LayoutPos.BOTTOM_CENTER, 0, 3);

        GuiText inOut = new GuiText(display)
                .setTextSupplier(() -> tile.energy.isUnlimited() ? genNetText(tile) : genInOutText(tile));
        Constraints.size(inOut, display.xSize(), 9);
        Constraints.placeOutside(inOut, ioLabel, Constraints.LayoutPos.BOTTOM_CENTER, 0, 2);

        GuiText net = new GuiText(display)
                .setTextSupplier(() -> genNetText(tile))
                .setEnabled(() -> !tile.energy.isUnlimited());
        Constraints.size(net, display.xSize(), 9);
        Constraints.placeOutside(net, inOut, Constraints.LayoutPos.BOTTOM_CENTER, 0, 3);

        GuiText eta = new GuiText(display)
                .setTextSupplier(() -> genEtaText(tile))
                .setEnabled(() -> !tile.energy.isUnlimited());
        Constraints.size(eta, display.xSize(), 9);
        Constraints.placeOutside(eta, net, Constraints.LayoutPos.BOTTOM_CENTER, 0, 2);

        var target = GuiTextField.create(display, 0xF0300000, 0xFFa8a8a8, 0xe1e3e5);
        target.container().setEnabled(() -> tile.energy.isUnlimited());
        target.field()
                .setSuggestion(TOOLKIT.translate("energy_target"))
                .setMaxLength(64)
                .setTooltipSingle(TOOLKIT.translate("energy_target_info"))
                .setFilter(s -> validBigInt(sanitizeNumStr(s)))
                .setTextState(TextState.simpleState(tile.energyTarget.get(), tile.energyTarget::set));
        Constraints.size(target.container(), barSlot.xSize(), 12);
        Constraints.placeOutside(target.container(), energyBar, Constraints.LayoutPos.TOP_CENTER, 0, -4);

        //Render Config
        GuiButton legacy = TOOLKIT.createThemedIconButton(root, 12, BCGuiTextures.getter("legacy"))
                .onPress(tile.legacyRender::invert)
                .setToggleMode(tile.legacyRender::get)
                .setTooltipSingle(() -> tile.customColour.get() ? TOOLKIT.translate("legacy_true") : TOOLKIT.translate("legacy_false"));
        Constraints.placeOutside(legacy, display, Constraints.LayoutPos.BOTTOM_RIGHT, -12, 0);

        GuiButton customColour = TOOLKIT.createThemedIconButton(root, 12, BCGuiTextures.getter("rgb_checker"))
                .setEnabled(() -> !legacy.toggleState())
                .onPress(tile.customColour::invert)
                .setToggleMode(tile.customColour::get)
                .setTooltipSingle(() -> tile.customColour.get() ? TOOLKIT.translate("custom_colour_true") : TOOLKIT.translate("custom_colour_false"));
        Constraints.placeOutside(customColour, legacy, Constraints.LayoutPos.MIDDLE_LEFT, 0, 0);

        GuiButton setColour = TOOLKIT.createThemedIconButton(root, 12, BCGuiTextures.getter("color_picker"))
                .setEnabled(() -> customColour.toggleState() && !legacy.toggleState())
                .onPress(() -> setColourSelectMode(root, true))
                .setTooltipSingle(() -> TOOLKIT.translate("config_colour"));
        Constraints.placeOutside(setColour, customColour, Constraints.LayoutPos.MIDDLE_LEFT, 0, 0);

        // eli.6: per-player client toggle for DE block effects (core, stabilizers, pylons, reactor). Same setting as
        // "blockEffectRendering" in config/brandon3055/DraconicEvolution.cfg; saved immediately, applies next frame.
        GuiButton effects = TOOLKIT.createThemedIconButton(root, 12, DEGuiTextures.getter("effects_toggle"))
                .onPress(() -> com.brandon3055.draconicevolution.DEConfig.modifyClientProperty("blockEffectRendering",
                        v -> v.setBoolean(!com.brandon3055.draconicevolution.DEConfig.blockEffectRendering)))
                .setToggleMode(() -> !com.brandon3055.draconicevolution.DEConfig.blockEffectRendering)
                .setTooltipSingle(() -> Component.literal(com.brandon3055.draconicevolution.DEConfig.blockEffectRendering
                        ? "Block effects ON (core, stabilizers, pylons, reactor). Click to hide them - only for you."
                        : "Block effects OFF - only for you. Click to show them again."));
        Constraints.placeOutside(effects, setColour, Constraints.LayoutPos.MIDDLE_LEFT, 0, 0);

        // eli.8: rate history graph (net OP/t, one bar per 5 s, last 5 minutes) in a panel right of the screen.
        GuiButton graphButton = TOOLKIT.createThemedIconButton(root, 12, DEGuiTextures.getter("rate_graph"))
                .setEnabled(tile.active::get)
                .onPress(() -> showGraph = !showGraph)
                .setToggleMode(() -> showGraph)
                .setTooltipSingle(() -> Component.literal(showGraph ? "Hide the rate history" : "Show the rate history (last 5 minutes)"));
        Constraints.placeOutside(graphButton, effects, Constraints.LayoutPos.MIDDLE_LEFT, 0, 0);

        // eli.8: show energy as RF instead of OP (display only, 1:1), per player like the eye button.
        GuiButton unitButton = TOOLKIT.createThemedIconButton(root, 12, DEGuiTextures.getter("energy_unit"))
                .setEnabled(tile.active::get)
                .onPress(() -> com.brandon3055.draconicevolution.DEConfig.modifyClientProperty("showEnergyAsRF",
                        v -> v.setBoolean(!com.brandon3055.draconicevolution.DEConfig.showEnergyAsRF)))
                .setToggleMode(() -> com.brandon3055.draconicevolution.DEConfig.showEnergyAsRF)
                .setTooltipSingle(() -> Component.literal(com.brandon3055.draconicevolution.DEConfig.showEnergyAsRF
                        ? "Showing RF. Click to show OP (1 OP = 1 RF)." : "Showing OP. Click to show RF (1 OP = 1 RF)."));
        Constraints.placeOutside(unitButton, graphButton, Constraints.LayoutPos.MIDDLE_LEFT, 0, 0);

        GuiRectangle graph = new GuiRectangle(root) {
            @Override
            public void renderBackground(GuiRender render, double mouseX, double mouseY, float partialTicks) {
                renderRateGraph(render, tile, xMin(), yMin(), xSize(), ySize());
            }
        }.setEnabled(() -> showGraph && tile.active.get());
        Constraints.size(graph, 124, 84);
        Constraints.placeOutside(graph, display, Constraints.LayoutPos.MIDDLE_RIGHT, 6, 0);

        setupColourPickers(gui.getDirectRoot(), tile);
    }

    private void setColourSelectMode(GuiElement<?> access, boolean enabled) {
        colourSelectMode.set(enabled);
        access.getModularGui().renderScreenBackground(!enabled);
    }

    private void setupColourPickers(GuiElement<?> root, TileEnergyCore tile) {
        GuiColourPicker frameColourDialog = GuiColourPicker.create(root, ColourState.create(tile.frameColour::get, tile.frameColour::set), false);
        frameColourDialog.constrain(TOP, literal(34));
        labelPicker(frameColourDialog, "Frame"); // eli.7: upstream left the three pickers unlabeled; short so it fits
        frameColourDialog.constrain(LEFT, literal(6)); // eli.8: off the screen edge, below other mods' corner icons
        frameColourDialog.addMoveHandle((int) frameColourDialog.ySize());
        frameColourDialog.setEnabled(colourSelectMode::get);
        frameColourDialog.getCancelButton().setEnabled(false);
        frameColourDialog.getOkButton().onPress(() -> setColourSelectMode(root, false));

        GuiColourPicker triangleColourDialog = GuiColourPicker.create(root, ColourState.create(tile.innerColour::get, tile.innerColour::set), false);
        triangleColourDialog.constrain(TOP, literal(120));
        labelPicker(triangleColourDialog, "Panels");
        triangleColourDialog.constrain(LEFT, literal(6));
        triangleColourDialog.addMoveHandle((int) triangleColourDialog.ySize());
        triangleColourDialog.setEnabled(colourSelectMode::get);
        triangleColourDialog.getCancelButton().setEnabled(false);
        triangleColourDialog.getOkButton().onPress(() -> setColourSelectMode(root, false));

        GuiColourPicker effectColourDialog = GuiColourPicker.create(root, ColourState.create(tile.effectColour::get, tile.effectColour::set), false);
        effectColourDialog.constrain(TOP, literal(206));
        labelPicker(effectColourDialog, "Glow");
        effectColourDialog.constrain(LEFT, literal(6));
        effectColourDialog.addMoveHandle((int) effectColourDialog.ySize());
        effectColourDialog.setEnabled(colourSelectMode::get);
        effectColourDialog.getCancelButton().setEnabled(false);
        effectColourDialog.getOkButton().onPress(() -> setColourSelectMode(root, false));
    }

    /** Name tag above a colour picker; a child of the dialog so it moves with it. */
    private static void labelPicker(GuiColourPicker dialog, String text) {
        GuiRectangle tag = toolTipBackground(dialog, 0xF0100010, () -> 0xFF8800FF);
        Constraints.size(tag, dialog.xSize(), 11);
        Constraints.placeOutside(tag, dialog, Constraints.LayoutPos.TOP_CENTER, 0, -1);
        Constraints.bind(new GuiText(tag, Component.literal(text).withStyle(GOLD)), tag, 1);
    }

    private static boolean showGraph = false;

    /** 271781000 -> "271.781M", 273000000 -> "273M", 999999 -> "999,999". Up to three decimals, trailing zeros dropped. */
    public static String shortNumber(long v) {
        if (Math.abs(v) < 1_000_000L) return Utils.addCommas(v);
        String[] units = {"k", "M", "G", "T", "P", "E"};
        java.math.BigDecimal d = java.math.BigDecimal.valueOf(v); // exact: 1640000000 must read 1.64G, not 1.639G
        int u = -1;
        while (d.abs().compareTo(java.math.BigDecimal.valueOf(1000)) >= 0 && u < units.length - 1) { d = d.movePointLeft(3); u++; }
        return d.setScale(3, java.math.RoundingMode.DOWN).stripTrailingZeros().toPlainString() + units[u];
    }
    private static long problemCheckedAt = -1;
    private static java.util.List<com.brandon3055.brandonscore.multiblock.InvalidPart> problems = java.util.List.of();

    private static java.util.List<com.brandon3055.brandonscore.multiblock.InvalidPart> structureProblems(TileEnergyCore tile) {
        var level = tile.getLevel();
        var def = tile.getMultiBlockDef();
        if (level == null || def == null) return java.util.List.of();
        if (level.getGameTime() - problemCheckedAt >= 20 || level.getGameTime() < problemCheckedAt) { // re-test once a second
            problemCheckedAt = level.getGameTime();
            problems = def.test(level, tile.getBlockPos());
        }
        return problems;
    }

    private static Component structureCount(TileEnergyCore tile) {
        int n = structureProblems(tile).size();
        return n == 0 ? Component.empty() : Component.literal(n + (n == 1 ? " block" : " blocks") + " wrong (hover)").withStyle(GOLD);
    }

    /** "X Y Z: needs Draconium Block (has Stone)" for the first few problems. */
    private static java.util.List<Component> structureDetails(TileEnergyCore tile) {
        var list = structureProblems(tile);
        var out = new java.util.ArrayList<Component>();
        for (var part : list.subList(0, Math.min(6, list.size()))) {
            var pos = part.pos();
            var have = tile.getLevel().getBlockState(pos);
            var need = part.expected().validBlocks().stream().findFirst().map(b -> (Component) b.getName()).orElse(Component.literal("air"));
            out.add(Component.literal(pos.getX() + " " + pos.getY() + " " + pos.getZ() + ": needs ").withStyle(GRAY)
                    .append(need.copy().withStyle(YELLOW))
                    .append(Component.literal(have.isAir() ? " (empty)" : " (has ").withStyle(GRAY))
                    .append(have.isAir() ? Component.empty() : have.getBlock().getName().copy().withStyle(RED))
                    .append(Component.literal(have.isAir() ? "" : ")").withStyle(GRAY)));
        }
        if (list.size() > 6) out.add(Component.literal("...and " + (list.size() - 6) + " more").withStyle(GRAY));
        out.add(Component.literal(tile.buildGuide.get() ? "Wrong blocks have red boxes." : "Turn on Build Guide to see them in the world.").withStyle(DARK_AQUA));
        return out;
    }

    private static void renderRateGraph(GuiRender render, TileEnergyCore tile, double x, double y, double w, double h) {
        render.toolTipBackground(x, y, w, h, 0xF0100010, 0xF0100010, 0xFF8800FF, 0xFF440080, false);
        long[] hist = tile.clientRateHistory;
        render.drawString("Net " + com.brandon3055.draconicevolution.DEConfig.energyUnit() + "/t, last 5 min", x + 4, y + 4, 0xFF55FFFF, false);
        double gx = x + 4, gy = y + 16, gw = w - 8, gh = h - 30, mid = gy + gh / 2;
        render.fill(gx, mid, gx + gw, mid + 1, 0x60FFFFFF);
        if (hist.length == 0) {
            render.drawString("Collecting... (one bar / 5 s)", x + 4, mid + 4, 0xFFAAAAAA, false);
            return;
        }
        long max = 1;
        for (long v : hist) max = Math.max(max, Math.abs(v));
        double bar = gw / TileEnergyCore.HISTORY_SAMPLES;
        int offset = TileEnergyCore.HISTORY_SAMPLES - hist.length; // newest sample on the right
        for (int i = 0; i < hist.length; i++) {
            double bh = (gh / 2) * Math.abs(hist[i]) / (double) max;
            double bx = gx + (offset + i) * bar;
            if (hist[i] > 0) render.fill(bx, mid - bh, bx + Math.max(1, bar - 0.5), mid, 0xFF40E060);
            else if (hist[i] < 0) render.fill(bx, mid + 1, bx + Math.max(1, bar - 0.5), mid + 1 + bh, 0xFFE04040);
        }
        render.drawString("±" + Utils.formatNumber(max), x + 4, y + h - 12, 0xFFAAAAAA, false);
        long last = hist[hist.length - 1];
        String now = "now " + (last > 0 ? "+" : "") + Utils.formatNumber(last);
        render.drawString(now, x + w - 4 - render.font().width(now), y + h - 12, last > 0 ? 0xFF40E060 : last < 0 ? 0xFFE04040 : 0xFFAAAAAA, false);
    }

    private double getEnergyDouble(TileEnergyCore tile) {
        if (tile.tier.get() < 8) {
            return tile.energy.getOPStored() / (double) tile.energy.getMaxOPStored();
        }
        if (!validBigInt(sanitizeNumStr(tile.energyTarget.get()))) {
            return 0;
        }
        BigDecimal target = new BigDecimal(sanitizeNumStr(tile.energyTarget.get()));
        if (target.compareTo(BigDecimal.ONE) < 0) {
            return 0;
        }
        double val = new BigDecimal(tile.energy.getStoredBig()).divide(target, 6, RoundingMode.HALF_EVEN).doubleValue();
        return MathHelper.clip(val, 0, 1);
    }

    private static final String OP = "mod_gui.brandonscore.energy_bar.op";

    /** "+1,234 / -567 OP/t" with input green and output red. */
    private Component genInOutText(TileEnergyCore tile) {
        IOInfo io = tile.energy.getIOInfo();
        if (io == null) return Component.literal("[Not Available]");
        return Component.empty()
                .append(Component.literal("+" + Utils.formatNumber(io.currentInput())).withStyle(GREEN))
                .append(Component.literal(" / ").withStyle(GRAY))
                .append(Component.literal("-" + Utils.formatNumber(io.currentOutput())).withStyle(RED))
                .append(Component.literal(" " + com.brandon3055.draconicevolution.DEConfig.energyUnit() + "/t").withStyle(GRAY));
    }

    /** "Net: +766 OP/t", green when filling, red when draining. */
    private Component genNetText(TileEnergyCore tile) {
        IOInfo io = tile.energy.getIOInfo();
        if (io == null) return Component.literal("[Not Available]");
        long net = io.currentInput() - io.currentOutput();
        return Component.literal("Net: ").withStyle(GRAY)
                .append(Component.literal((net > 0 ? "+" : "") + Utils.formatNumber(net) + " ")
                        .append(com.brandon3055.draconicevolution.DEConfig.energyUnit()).append("/t")
                        .withStyle(net > 0 ? GREEN : net < 0 ? RED : GRAY));
    }

    /** "Full in 3h 12m" / "Empty in 5m 2s" / "Full" / "Empty" / "Steady", from the 1 s average rate. */
    private Component genEtaText(TileEnergyCore tile) {
        IOInfo io = tile.energy.getIOInfo();
        if (io == null) return Component.empty();
        long net = io.currentInput() - io.currentOutput();
        long stored = tile.energy.getOPStored();
        long max = tile.energy.getMaxOPStored();
        if (net > 0) {
            return stored >= max ? Component.literal("Full").withStyle(GREEN)
                    : Component.literal("Full in " + formatDuration((max - stored) / (double) net / 20D)).withStyle(GRAY);
        }
        if (net < 0) {
            return stored <= 0 ? Component.literal("Empty").withStyle(RED)
                    : Component.literal("Empty in " + formatDuration(stored / (double) -net / 20D)).withStyle(GRAY);
        }
        return Component.literal(stored >= max ? "Full" : stored <= 0 ? "Empty" : "Steady").withStyle(GRAY);
    }

    /** Two largest units: "2y 41d", "3d 4h", "3h 12m", "12m 5s", "45s". */
    public static String formatDuration(double seconds) {
        if (seconds < 1) return "<1s";
        if (seconds > 999L * 365 * 86400) return "999y+";
        long s = (long) Math.ceil(seconds);
        long y = s / (365L * 86400), d = s / 86400 % 365, h = s / 3600 % 24, m = s / 60 % 60, sec = s % 60;
        if (y > 0) return y + "y " + d + "d";
        if (d > 0) return d + "d " + h + "h";
        if (h > 0) return h + "h " + m + "m";
        if (m > 0) return m + "m " + sec + "s";
        return sec + "s";
    }

    /**
     * This is just limits the target value the user can specify.
     * Things get incredibly slow when using numbers bigger then this.
     * <p>
     * This does not limit the core's capacity.
     * I think i'm just going to have to accept that after around 10^10^5 years at an input rate of 2^64 * 2^32
     * things may start to get a bit laggy....
     */
    private static final BigDecimal MAX_BIGINT = new BigDecimal("9999e9999");

    public static boolean validBigInt(String value) {
        try {
            BigDecimal val = new BigDecimal(value);
            return val.compareTo(BigDecimal.ZERO) >= 0 && val.compareTo(MAX_BIGINT) <= 0;
        } catch (Throwable e) {
            return false;
        }
    }

    public static String sanitizeNumStr(String value) {
        if (value.isEmpty() || value.toLowerCase(Locale.ENGLISH).endsWith("e")) {
            value += "0";
        }
        return value;
    }

    public static GuiRectangle toolTipBackground(@NotNull GuiParent<?> parent, int backgroundColour, Supplier<Integer> borderColour) {
        Supplier<Integer> borderEndColor = () -> (borderColour.get() & 0xFEFEFE) >> 1 | borderColour.get() & 0xFF000000;
        return new GuiRectangle(parent) {
            @Override
            public void renderBackground(GuiRender render, double mouseX, double mouseY, float partialTicks) {
                render.toolTipBackground(xMin(), yMin(), xSize(), ySize(), backgroundColour, backgroundColour, borderColour.get(), borderEndColor.get(), false);
            }
        };
    }

    public static class Screen extends ModularGuiContainer<EnergyCoreMenu> {
        public static AtomicBoolean hideJEI = new AtomicBoolean(false);

        public Screen(EnergyCoreMenu menu, Inventory inv, Component title) {
            super(menu, inv, new EnergyCoreGui(hideJEI));
            getModularGui().setGuiTitle(title);
        }
    }
}
