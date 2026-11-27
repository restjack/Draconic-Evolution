package com.brandon3055.draconicevolution.client.gui;

import com.brandon3055.brandonscore.client.BCGuiTextures;
import com.brandon3055.brandonscore.client.gui.GuiToolkit;
import com.brandon3055.brandonscore.client.gui.GuiToolkit.Palette;
import com.brandon3055.brandonscore.client.gui.modular.ModularGui;
import com.brandon3055.brandonscore.client.gui.modular.ModularGuiContainer;
import com.brandon3055.brandonscore.client.gui.modular.elements.*;
import com.brandon3055.brandonscore.client.gui.modular.lib.Constraints;
import com.brandon3055.brandonscore.client.gui.modular.lib.GuiRender;
import com.brandon3055.brandonscore.client.gui.modular.lib.container.ContainerGuiProvider;
import com.brandon3055.brandonscore.client.gui.modular.lib.container.ContainerScreenAccess;
import com.brandon3055.brandonscore.client.gui.modular.lib.geometry.Align;
import com.brandon3055.brandonscore.client.gui.modular.lib.geometry.Direction;
import com.brandon3055.brandonscore.client.gui.modular.lib.geometry.GuiParent;
import com.brandon3055.brandonscore.client.gui.modulargui.templates.ButtonRow;
import com.brandon3055.draconicevolution.DEConfig;
import com.brandon3055.draconicevolution.api.crafting.IFusionRecipe;
import com.brandon3055.draconicevolution.blocks.tileentity.TileFusionConductor;
import com.brandon3055.draconicevolution.blocks.tileentity.TileFusionCraftingCore;
import com.brandon3055.draconicevolution.blocks.tileentity.TileFusionCraftingInjector;
import com.brandon3055.draconicevolution.inventory.FusionConductorMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.brandon3055.brandonscore.client.gui.modular.lib.geometry.Constraint.*;
import static com.brandon3055.brandonscore.client.gui.modular.lib.geometry.GeoParam.*;

/**
 * Fusion Conductor screen: tall energy bar + capacitor (left), input 6x3, output 3x3, recipe lock + batch row, one row of
 * machine toggles (auto-craft, push output, power injectors, redstone mode, injector mode, show injectors), status box
 * with a settings summary and progress, live view of the injectors. Theme + info stay top right.
 * Layout v2 (Eli 2026-11-26): the old right-hand button column ran into the live view and clipped "64".
 */
public class FusionConductorGui extends ContainerGuiProvider<FusionConductorMenu> {
    private static final GuiToolkit TOOLKIT = new GuiToolkit("gui.draconicevolution.fusion_conductor");
    public static final int GUI_WIDTH = 226;
    public static final int GUI_HEIGHT = 276;
    private static final Item[] RS_ICONS = {Items.GUNPOWDER, Items.REDSTONE_TORCH, Items.LEVER, Items.STONE_BUTTON};
    private static final String[] RS_KEYS = {"ignore", "high", "low", "pulse"};

    @Override
    public GuiElement<?> createRootElement(ModularGui gui) {
        GuiManipulable root = new GuiManipulable(gui).addMoveHandle(3).enableCursors(true);
        GuiTexture bg = new GuiTexture(root.getContentElement(), BCGuiTextures.themedGetter("background_dynamic")).dynamicTexture();
        Constraints.bind(bg, root.getContentElement());
        return root;
    }

    @Override
    public void buildGui(ModularGui gui, ContainerScreenAccess<FusionConductorMenu> screenAccess) {
        gui.initStandardGui(GUI_WIDTH, GUI_HEIGHT);
        FusionConductorMenu menu = screenAccess.getMenu();
        TileFusionConductor tile = menu.tile;
        GuiElement<?> root = gui.getRoot();
        GuiText heading = TOOLKIT.createHeading(root, gui.getGuiTitle(), true);

        // Right column toggles
        com.brandon3055.brandonscore.client.gui.InfoPanel info = com.brandon3055.brandonscore.client.gui.InfoPanel.create(root);
        info.labeledValue(TOOLKIT.translate("stats.crafts").withStyle(ChatFormatting.GOLD), () -> Component.literal(String.valueOf(tile.crafts.get())).withStyle(ChatFormatting.GRAY));
        info.labeledValue(TOOLKIT.translate("stats.energy").withStyle(ChatFormatting.GOLD), () -> Component.literal(com.brandon3055.brandonscore.utils.Utils.formatNumber(tile.energyUsed.get()) + " OP").withStyle(ChatFormatting.GRAY));
        info.labeledValue(TOOLKIT.translate("stats.average").withStyle(ChatFormatting.GOLD), () -> Component.literal(tile.crafts.get() == 0 ? "-" : com.brandon3055.brandonscore.utils.Utils.formatNumber(tile.energyUsed.get() / tile.crafts.get()) + " OP").withStyle(ChatFormatting.GRAY));
        info.labeledValue(TOOLKIT.translate("stats.owner").withStyle(ChatFormatting.GOLD), () -> Component.literal(tile.ownerName.get().isEmpty() ? "-" : tile.ownerName.get()).withStyle(ChatFormatting.GRAY));

        ButtonRow buttons = ButtonRow.topRightInside(root, Direction.DOWN, 3, 3).setSpacing(1);
        buttons.addButton(TOOLKIT::createThemeButton);
        buttons.addButton(e -> TOOLKIT.createInfoButton(e, info));
        Flash flash = new Flash();

        // Player inventory
        var playInv = GuiSlots.player(root, screenAccess, menu.main, menu.hotBar);
        playInv.stream().forEach(e -> e.setSlotTexture(slot -> BCGuiTextures.getThemed("slot")));
        Constraints.placeInside(playInv.container(), root, Constraints.LayoutPos.BOTTOM_CENTER, 0, -7);
        TOOLKIT.playerInvTitle(playInv.container());

        // Input 6x3, arrow, output 3x3
        GuiSlots input = new GuiSlots(root, screenAccess, menu.input, 6)
                .setSlotTexture(slot -> BCGuiTextures.getThemed("slot"))
                .constrain(LEFT, relative(root.get(LEFT), 28))
                .constrain(TOP, relative(heading.get(BOTTOM), 4));
        GuiSlots output = new GuiSlots(root, screenAccess, menu.output, 3)
                .setSlotTexture(slot -> BCGuiTextures.getThemed("slot"))
                .constrain(LEFT, relative(input.get(RIGHT), 18))
                .constrain(TOP, match(input.get(TOP)));
        new GuiText(root, Component.literal("→")).setTextColour(Palette.BG::text).setShadow(false)
                .constrain(LEFT, relative(input.get(RIGHT), 2)).constrain(RIGHT, relative(output.get(LEFT), -2))
                .constrain(TOP, relative(input.get(TOP), 22)).constrain(HEIGHT, literal(10));


        // Row: recipe lock ghost slot + clear, batch - N +, left N
        GuiButton lock = TOOLKIT.createFlat3DButton(root, null)
                .setTooltip(() -> lockTooltip(tile))
                .onPress(() -> setLock(tile, menu.getCarried()), GuiButton.LEFT_CLICK)
                .onPress(() -> tile.sendPacketToServer(out -> out.writeString(""), 1), GuiButton.RIGHT_CLICK)
                .constrain(LEFT, match(input.get(LEFT)))
                .constrain(TOP, relative(input.get(BOTTOM), 3))
                .constrain(WIDTH, literal(18)).constrain(HEIGHT, literal(18));
        GuiItemStack lockIcon = new GuiItemStack(lock, ItemStack.EMPTY).enableStackToolTip(false);
        Constraints.bind(lockIcon, lock, 1);
        gui.onTick(() -> {
            RecipeHolder<IFusionRecipe> r = tile.lockedRecipe();
            lockIcon.setStack(r == null ? ItemStack.EMPTY : r.value().getResultItem(tile.getLevel().registryAccess()));
        });
        new GuiText(root, () -> tile.recipeLock.get() == null ? TOOLKIT.translate("lock.none") : TOOLKIT.translate("lock.set"))
                .setTextColour(Palette.BG::text).setShadow(false).setAlignment(Align.LEFT)
                .constrain(LEFT, relative(lock.get(RIGHT), 3)).constrain(TOP, relative(lock.get(TOP), 5))
                .constrain(WIDTH, literal(40)).constrain(HEIGHT, literal(9));

        GuiButton minus = TOOLKIT.createFlat3DButton(root, () -> Component.literal("-"))
                .setTooltipSingle(TOOLKIT.translate("batch.change"))
                .onPress(() -> batch(tile, -1))
                .constrain(LEFT, relative(lock.get(RIGHT), 46)).constrain(TOP, relative(lock.get(TOP), 3))
                .constrain(WIDTH, literal(12)).constrain(HEIGHT, literal(12));
        GuiText batchText = new GuiText(root, () -> tile.batch.get() == 0 ? TOOLKIT.translate("batch.off") : TOOLKIT.translate("batch.n", tile.batch.get()))
                .setTextColour(Palette.BG::text).setShadow(false)
                .setTooltipSingle(TOOLKIT.translate("batch.info"))
                .constrain(LEFT, relative(minus.get(RIGHT), 1)).constrain(TOP, relative(minus.get(TOP), 2))
                .constrain(WIDTH, literal(54)).constrain(HEIGHT, literal(9));
        GuiButton plus = TOOLKIT.createFlat3DButton(root, () -> Component.literal("+"))
                .setTooltipSingle(TOOLKIT.translate("batch.change"))
                .onPress(() -> batch(tile, 1))
                .constrain(LEFT, relative(batchText.get(RIGHT), 1)).constrain(TOP, match(minus.get(TOP)))
                .constrain(WIDTH, literal(12)).constrain(HEIGHT, literal(12));
        new GuiText(root, () -> TOOLKIT.translate("batch.left", tile.batchLeft.get()))
                .setTextColour(Palette.BG::text).setShadow(false).setAlignment(Align.LEFT)
                .setEnabled(() -> tile.batch.get() > 0)
                .constrain(LEFT, relative(plus.get(RIGHT), 3)).constrain(TOP, relative(plus.get(TOP), 2))
                .constrain(WIDTH, literal(40)).constrain(HEIGHT, literal(9));

        // Machine toggles: one row of 16 px buttons under the lock/batch row, all inside the panel at any GUI scale.
        GuiButton[] row = {
                toggle(root, new ItemStack(Items.REDSTONE_TORCH), () -> flash.set(tile, TileFusionConductor.SET_AUTO, !tile.autoCraft.get(), "auto_craft"), tile.autoCraft::get, "gui.draconicevolution.fusion_conductor.auto_craft"),
                toggle(root, new ItemStack(Items.HOPPER), () -> flash.set(tile, TileFusionConductor.SET_PUSH, !tile.pushOutput.get(), "push_output"), tile.pushOutput::get, "gui.draconicevolution.fusion_conductor.push_output"),
                toggle(root, new ItemStack(Items.LIGHTNING_ROD), () -> flash.set(tile, TileFusionConductor.SET_POWER, !tile.powerInjectors.get(), "power_injectors"), tile.powerInjectors::get, "gui.draconicevolution.fusion_conductor.power_injectors"),
                redstoneButton(root, gui, tile, flash),
                injectorButton(root, tile, flash),
                toggle(root, new ItemStack(Items.ENDER_EYE), () -> {
                    TileFusionCraftingCore c = tile.getCore();
                    if (c == null) return;
                    FusionCraftingCoreGui.toggleHighlight(c);
                    flash.show(onOff("gui.draconicevolution.fusion_craft.show_injectors", FusionCraftingCoreGui.highlightOn(c)));
                }, () -> tile.getCore() != null && FusionCraftingCoreGui.highlightOn(tile.getCore()), "gui.draconicevolution.fusion_craft.show_injectors"),
        };
        for (int i = 0; i < row.length; i++) {
            row[i].constrain(LEFT, relative(input.get(LEFT), i * 18)).constrain(TOP, relative(lock.get(BOTTOM), 3));
        }

        // Live view (right) + status box (left of it)
        LiveView live = new LiveView(root, tile);
        live.constrain(RIGHT, relative(root.get(RIGHT), -5))
                .constrain(TOP, relative(lock.get(BOTTOM), 22))
                .constrain(BOTTOM, relative(playInv.container().get(TOP), -24))
                .constrain(WIDTH, dynamic(() -> live.ySize()));
        GuiRectangle statusBG = new GuiRectangle(root)
                .shadedRect(Palette.SubItem::accentLight, Palette.SubItem::accentDark, Palette.SubItem::fill)
                .constrain(LEFT, match(input.get(LEFT)))
                .constrain(RIGHT, relative(live.get(LEFT), -3))
                .constrain(TOP, match(live.get(TOP)))
                .constrain(BOTTOM, match(live.get(BOTTOM)));
        GuiText modes = new GuiText(statusBG, () -> summaryModes(tile))
                .setShadow(false).setTextColour(Palette.SubItem::text).setAlignment(Align.LEFT)
                .constrain(LEFT, relative(statusBG.get(LEFT), 3))
                .constrain(RIGHT, relative(statusBG.get(RIGHT), -3))
                .constrain(BOTTOM, relative(statusBG.get(BOTTOM), -6))
                .constrain(HEIGHT, literal(9));
        GuiText toggles = new GuiText(statusBG, () -> summaryToggles(tile))
                .setShadow(false).setTextColour(Palette.SubItem::text).setAlignment(Align.LEFT)
                .constrain(LEFT, relative(statusBG.get(LEFT), 3))
                .constrain(RIGHT, relative(statusBG.get(RIGHT), -3))
                .constrain(BOTTOM, match(modes.get(TOP)))
                .constrain(HEIGHT, literal(9));
        // A click shows what it changed for 2 s, then the machine status comes back.
        new GuiText(statusBG, () -> flash.get() != null ? flash.get() : tile.status.get() == null ? Component.empty() : tile.status.get())
                .setWrap(true).setShadow(false).setTextColour(Palette.SubItem::text)
                .setAlignment(Align.LEFT)
                .constrain(LEFT, relative(statusBG.get(LEFT), 3))
                .constrain(RIGHT, relative(statusBG.get(RIGHT), -3))
                .constrain(TOP, relative(statusBG.get(TOP), 3))
                .constrain(BOTTOM, relative(toggles.get(TOP), -1));
        GuiRectangle barBG = new GuiRectangle(statusBG).fill(0xFF202020)
                .constrain(LEFT, relative(statusBG.get(LEFT), 3))
                .constrain(RIGHT, relative(statusBG.get(RIGHT), -3))
                .constrain(BOTTOM, relative(statusBG.get(BOTTOM), -2))
                .constrain(HEIGHT, literal(3))
                .setEnabled(() -> tile.progress.get() >= 0);
        new GuiRectangle(barBG).fill(0xFF9B6BFF)
                .constrain(LEFT, match(barBG.get(LEFT)))
                .constrain(TOP, match(barBG.get(TOP)))
                .constrain(BOTTOM, match(barBG.get(BOTTOM)))
                .constrain(WIDTH, dynamic(() -> barBG.xSize() * Math.max(0, Math.min(1, tile.progress.get()))));

        // Legend for the live view, one row under the status box and the view.
        new Legend(root)
                .constrain(LEFT, relative(root.get(LEFT), 5))
                .constrain(RIGHT, relative(root.get(RIGHT), -5))
                .constrain(TOP, relative(statusBG.get(BOTTOM), 2))
                .constrain(HEIGHT, literal(9));

        // Energy bar runs the full left side, capacitor slot under it (Eli: the old 34 px bar was too small)
        GuiSlots cap = GuiSlots.singleSlot(root, screenAccess, menu.capacitor, 0)
                .setSlotTexture(slot -> BCGuiTextures.getThemed("slot"))
                .setEmptyIcon(BCGuiTextures.get("slots/energy"))
                .setTooltip(() -> List.of(TOOLKIT.translate("capacitor"), speedLine(tile).copy().withStyle(ChatFormatting.DARK_PURPLE), TOOLKIT.translate("capacitor.info").withStyle(ChatFormatting.GRAY)))
                .constrain(LEFT, relative(root.get(LEFT), 5))
                .constrain(BOTTOM, match(statusBG.get(BOTTOM)));
        var energyBar = TOOLKIT.createEnergyBar(root, tile.opStorage);
        energyBar.container()
                .constrain(TOP, relative(heading.get(BOTTOM), 4))
                .constrain(LEFT, relative(root.get(LEFT), 5))
                .constrain(BOTTOM, relative(cap.get(TOP), -2))
                .constrain(WIDTH, literal(18));
    }

    private static void set(TileFusionConductor tile, int which, boolean on) {
        set(tile, which, on ? 1 : 0);
    }

    private static void set(TileFusionConductor tile, int which, int value) {
        tile.sendPacketToServer(out -> out.writeByte(which).writeByte(value), 4);
    }

    private static int mode(TileFusionConductor tile) {
        return Math.floorMod(tile.redstoneMode.get(), 4);
    }

    private static boolean allSingle(TileFusionConductor tile) {
        TileFusionCraftingCore core = tile.getCore();
        return core != null && FusionCraftingCoreGui.allSingle(core);
    }

    private static void batch(TileFusionConductor tile, int sign) {
        Minecraft mc = Minecraft.getInstance();
        int step = mc.hasControlDown() ? 64 : mc.hasShiftDown() ? 10 : 1;
        tile.sendPacketToServer(out -> out.writeInt(sign * step), 2);
    }

    /** Click the ghost slot holding the item you want to make (or a recipe output from JEI's "+"). */
    private static void setLock(TileFusionConductor tile, ItemStack carried) {
        if (carried.isEmpty()) return;
        RecipeHolder<IFusionRecipe> r = TileFusionConductor.recipeFor(tile.getLevel(), carried);
        if (r != null) tile.sendPacketToServer(out -> out.writeString(r.id().identifier().toString()), 1);
    }

    private static List<Component> lockTooltip(TileFusionConductor tile) {
        List<Component> list = new ArrayList<>();
        list.add(TOOLKIT.translate("lock"));
        RecipeHolder<IFusionRecipe> r = tile.lockedRecipe();
        if (r != null) list.add(r.value().getResultItem(tile.getLevel().registryAccess()).getHoverName().copy().withStyle(ChatFormatting.LIGHT_PURPLE));
        list.add(TOOLKIT.translate("lock.info").withStyle(ChatFormatting.GRAY));
        return list;
    }

    private static Component speedLine(TileFusionConductor tile) {
        int tier = tile.tier();
        return TOOLKIT.translate("speed", TileFusionConductor.speed(tier), TileFusionConductor.trim(TileFusionConductor.energyFactor(tier)));
    }

    static Component onOff(String key, boolean on) {
        return Component.translatable(key).append(": ").append(Component.translatable(on ? "gui.draconicevolution.toggle.on" : "gui.draconicevolution.toggle.off")
                .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED));
    }

    /** "Auto  Push  Power": each word green when on, red when off. */
    private static Component summaryToggles(TileFusionConductor tile) {
        return Component.empty().append(word("auto", tile.autoCraft.get())).append("  ")
                .append(word("push", tile.pushOutput.get())).append("  ").append(word("power", tile.powerInjectors.get()));
    }

    private static Component word(String key, boolean on) {
        return TOOLKIT.translate("summary." + key).withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    /** "RS: Ignore  Inj: 64" */
    private static Component summaryModes(TileFusionConductor tile) {
        return TOOLKIT.translate("summary.rs", TOOLKIT.translate("summary.rs." + RS_KEYS[mode(tile)])).append("  ")
                .append(TOOLKIT.translate("summary.inj", allSingle(tile) ? "1" : "64"));
    }

    /** Short "Auto-craft: Off" line in the status box after a click, so the change shows at once. */
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

        void set(TileFusionConductor tile, int which, boolean on, String key) {
            FusionConductorGui.set(tile, which, on);
            show(onOff("gui.draconicevolution.fusion_conductor." + key, on));
        }
    }

    private static GuiButton redstoneButton(GuiElement<?> parent, ModularGui gui, TileFusionConductor tile, Flash flash) {
        GuiButton b = TOOLKIT.createFlat3DButton(parent, null)
                .setTooltip(() -> List.of(TOOLKIT.translate("redstone"),
                        TOOLKIT.translate("redstone." + RS_KEYS[mode(tile)]).withStyle(ChatFormatting.GOLD),
                        TOOLKIT.translate("redstone.info").withStyle(ChatFormatting.GRAY)))
                .onPress(() -> cycleRs(tile, flash, 1), GuiButton.LEFT_CLICK)
                .onPress(() -> cycleRs(tile, flash, 3), GuiButton.RIGHT_CLICK);
        Constraints.size(b, 16, 16);
        GuiItemStack icon = new GuiItemStack(b, new ItemStack(RS_ICONS[0])).enableStackToolTip(false);
        Constraints.bind(icon, b, 1);
        gui.onTick(() -> icon.setStack(new ItemStack(RS_ICONS[mode(tile)])));
        return b;
    }

    private static void cycleRs(TileFusionConductor tile, Flash flash, int step) {
        int next = (mode(tile) + step) % 4;
        set(tile, TileFusionConductor.SET_RS, next);
        flash.show(TOOLKIT.translate("redstone").append(": ").append(TOOLKIT.translate("redstone." + RS_KEYS[next]).withStyle(ChatFormatting.GOLD)));
    }

    private static GuiButton injectorButton(GuiElement<?> parent, TileFusionConductor tile, Flash flash) {
        GuiButton b = TOOLKIT.createFlat3DButton(parent, () -> Component.literal(allSingle(tile) ? "1" : "64"))
                .setTooltip(() -> List.of(Component.translatable("gui.draconicevolution.fusion_craft.injector_mode"),
                        com.brandon3055.draconicevolution.blocks.machines.CraftingInjector.modeText(allSingle(tile)),
                        Component.translatable("gui.draconicevolution.fusion_craft.injector_mode.info").withStyle(ChatFormatting.GRAY)))
                .onPress(() -> {
                    boolean single = !allSingle(tile);
                    tile.sendPacketToServer(out -> out.writeBoolean(single), 3);
                    flash.show(com.brandon3055.draconicevolution.blocks.machines.CraftingInjector.modeText(single));
                });
        Constraints.size(b, 16, 16);
        return b;
    }

    /** 16 px toggle: green with the icon when on; dimmed icon plus a red corner when off. */
    static GuiButton toggle(GuiElement<?> parent, ItemStack icon, Runnable press, java.util.function.Supplier<Boolean> state, String key) {
        return toggle(parent, b -> Constraints.bind(new GuiItemStack(b, icon).enableStackToolTip(false), b, 1), press, state, key);
    }

    /** Same toggle with a 16 px gui-atlas icon (e.g. "celestial_manipulator/quiet") instead of an item. */
    static GuiButton toggle(GuiElement<?> parent, String texture, Runnable press, java.util.function.Supplier<Boolean> state, String key) {
        return toggle(parent, b -> Constraints.bind(new GuiTexture(b, com.brandon3055.draconicevolution.client.DEGuiTextures.getter(texture)), b, 0), press, state, key);
    }

    private static GuiButton toggle(GuiElement<?> parent, java.util.function.Consumer<GuiButton> icon, Runnable press, java.util.function.Supplier<Boolean> state, String key) {
        GuiButton button = TOOLKIT.createFlat3DButton(parent, null)
                .setTooltip(() -> List.of(Component.translatable(key),
                        Component.translatable(state.get() ? "gui.draconicevolution.toggle.on" : "gui.draconicevolution.toggle.off")
                                .withStyle(state.get() ? ChatFormatting.GREEN : ChatFormatting.RED),
                        Component.translatable(key + ".info").withStyle(ChatFormatting.GRAY)))
                .onPress(press)
                .setToggleMode(state);
        Constraints.size(button, 16, 16);
        new GuiRectangle(button).fill(0xB040C040).setEnabled(state)
                .constrain(LEFT, relative(button.get(LEFT), 1)).constrain(TOP, relative(button.get(TOP), 1))
                .constrain(RIGHT, relative(button.get(RIGHT), -1)).constrain(BOTTOM, relative(button.get(BOTTOM), -1));
        icon.accept(button);
        new GuiRectangle(button).fill(0x90202020).setEnabled(() -> !state.get())
                .constrain(LEFT, relative(button.get(LEFT), 1)).constrain(TOP, relative(button.get(TOP), 1))
                .constrain(RIGHT, relative(button.get(RIGHT), -1)).constrain(BOTTOM, relative(button.get(BOTTOM), -1));
        new GuiRectangle(button).fill(0xFFD03030).setEnabled(() -> !state.get())
                .constrain(RIGHT, relative(button.get(RIGHT), -1)).constrain(BOTTOM, relative(button.get(BOTTOM), -1))
                .constrain(WIDTH, literal(4)).constrain(HEIGHT, literal(4));
        return button;
    }

    static final int COL_EMPTY = 0xFF808080, COL_LOADED = 0xFF40D040, COL_FOREIGN = 0xFFE04040;
    static final int COL_CORE_IDLE = 0xFF58C8FF, COL_CORE_CRAFTING = 0xFF9B6BFF, COL_CHARGE = 0xFF9B6BFF;

    /**
     * Top-down map of the core (centre) and its injectors, turned so screen-up is the way the player faced when the
     * screen opened (snapped to N/E/S/W); "N" marks north. Zooms to fit the injectors actually used. Injectors stacked in
     * one column (same x/z) share a split marker, highest on top; hover lists them all. Injectors directly above or
     * below the core sit on a small ring around it. Colours: see {@link Legend}.
     */
    private static class LiveView extends GuiElement<LiveView> implements com.brandon3055.brandonscore.client.gui.modular.lib.BackgroundRender {
        private final TileFusionConductor tile;
        /** Player's forward (fx, fz) and right (rx, rz) as world x/z steps. */
        private final int fx, fz, rx, rz;
        private double mx, my;

        LiveView(@NotNull GuiParent<?> parent, TileFusionConductor tile) {
            super(parent);
            this.tile = tile;
            var player = Minecraft.getInstance().player;
            net.minecraft.core.Direction d = player == null ? net.minecraft.core.Direction.NORTH : player.getDirection();
            fx = d.getStepX();
            fz = d.getStepZ();
            rx = -fz;
            rz = fx;
            setTooltip(this::hoverText);
        }

        private record Inj(BlockPos pos, TileFusionCraftingInjector inj) {}

        private record Marker(double x, double y, double w, double h, List<Inj> list) {
            boolean hit(double px, double py) {
                return Math.abs(px - x) <= w / 2 + 1 && Math.abs(py - y) <= h / 2 + 1;
            }
        }

        /** World offset from the core (x, z) to screen units (right, down). */
        private double[] screen(int dx, int dz) {
            return new double[]{dx * rx + dz * rz, -(dx * fx + dz * fz)};
        }

        private double cx() { return xMin() + xSize() / 2; }

        private double cy() { return yMin() + ySize() / 2; }

        private List<Marker> markers() {
            List<Marker> out = new ArrayList<>();
            TileFusionCraftingCore core = tile.getCore();
            if (core == null) return out;
            BlockPos c = core.getBlockPos();
            // key: x/z column plus 0 = beside the core, 1 = above, 2 = below (ring)
            java.util.Map<List<Integer>, List<Inj>> cols = new java.util.LinkedHashMap<>();
            for (BlockPos p : core.getInjectorPositions()) {
                if (!(tile.getLevel().getBlockEntity(p) instanceof TileFusionCraftingInjector inj)) continue;
                int dx = p.getX() - c.getX(), dy = p.getY() - c.getY(), dz = p.getZ() - c.getZ();
                int ring = Math.abs(dy) > Math.max(Math.abs(dx), Math.abs(dz)) ? (dy > 0 ? 1 : 2) : 0;
                cols.computeIfAbsent(List.of(dx, dz, ring), k -> new ArrayList<>()).add(new Inj(p, inj));
            }
            double spread = 1;
            for (List<Integer> k : cols.keySet()) {
                if (k.get(2) != 0) continue;
                double[] sp = screen(k.get(0), k.get(1));
                spread = Math.max(spread, Math.max(Math.abs(sp[0]), Math.abs(sp[1])));
            }
            // Zoom to the real spread; capped so a 1-block layout does not fly apart, floor keeps markers inside the box.
            double scale = Math.min(12, (Math.min(xSize(), ySize()) / 2 - 6) / spread);
            for (var e : cols.entrySet()) {
                List<Inj> list = e.getValue();
                list.sort((a, b) -> b.pos().getY() - a.pos().getY());
                double h = Math.min(12, Math.max(5, 3 * list.size()));
                int dx = e.getKey().get(0), dz = e.getKey().get(1), ring = e.getKey().get(2);
                double[] sp = screen(dx, dz);
                double x, y;
                if (ring == 0) {
                    x = cx() + sp[0] * scale;
                    y = cy() + sp[1] * scale;
                } else {
                    double a = Math.atan2(sp[1], sp[0]) + (ring == 1 ? 0 : Math.PI / 4);
                    x = cx() + Math.cos(a) * 9;
                    y = cy() + Math.sin(a) * 9;
                }
                out.add(new Marker(x, y, 5, h, list));
            }
            return out;
        }

        private int colour(Inj i) {
            if (i.inj().getInjectorStack().isEmpty()) return COL_EMPTY;
            return tile.isLoaded(i.pos()) ? COL_LOADED : COL_FOREIGN;
        }

        @Override
        public void renderBackground(GuiRender render, double mouseX, double mouseY, float partialTicks) {
            mx = mouseX;
            my = mouseY;
            render.borderRect(xMin(), yMin(), xSize(), ySize(), 1, 0xFF1A1A1A, 0xFF555555);
            double cx = cx(), cy = cy();
            double[] n = screen(0, -1);
            double r = Math.min(xSize(), ySize()) / 2 - 5;
            render.drawString("N", cx + n[0] * r - 2, cy + n[1] * r - 3, 0xFF8A8A8A, false);
            TileFusionCraftingCore core = tile.getCore();
            render.rect(cx - 3.5, cy - 3.5, 7, 7, core != null && core.isCrafting() ? COL_CORE_CRAFTING : COL_CORE_IDLE);
            for (Marker m : markers()) {
                int count = m.list().size();
                double top = m.y() - m.h() / 2, sh = m.h() / count;
                for (int i = 0; i < count; i++) {
                    render.rect(m.x() - m.w() / 2, top + i * sh, m.w(), sh, colour(m.list().get(i)));
                    if (i > 0) render.rect(m.x() - m.w() / 2, top + i * sh - 0.5, m.w(), 1, 0xFF1A1A1A);
                }
                long need = 0, have = 0;
                for (Inj i : m.list()) {
                    need += i.inj().getEnergyRequirement();
                    have += Math.min(i.inj().getInjectorEnergy(), i.inj().getEnergyRequirement());
                }
                if (need > 0) render.rect(m.x() - m.w() / 2, top + m.h() + 1, m.w() * Math.min(1, have / (double) need), 1, COL_CHARGE);
            }
        }

        private List<Component> hoverText() {
            for (Marker m : markers()) {
                if (!m.hit(mx, my)) continue;
                List<Component> list = new ArrayList<>();
                if (m.list().size() > 1) list.add(TOOLKIT.translate("live.column", m.list().size()).withStyle(ChatFormatting.GOLD));
                for (Inj i : m.list()) {
                    BlockPos p = i.pos();
                    list.add(Component.translatable("tech_level.draconicevolution." + i.inj().getInjectorTier().name().toLowerCase())
                            .append(" " + p.getX() + " " + p.getY() + " " + p.getZ()));
                    ItemStack s = i.inj().getInjectorStack();
                    {
                        net.minecraft.network.chat.MutableComponent line = Component.literal("  ").append(s.isEmpty() ? TOOLKIT.translate("live.empty").withStyle(ChatFormatting.GRAY)
                                : s.getHoverName().copy().withStyle(tile.isLoaded(p) ? ChatFormatting.GREEN : ChatFormatting.RED));
                        if (i.inj().getEnergyRequirement() > 0)
                            line.append(Component.literal("  " + Math.round(100D * Math.min(1, i.inj().getInjectorEnergy() / (double) i.inj().getEnergyRequirement())) + "%").withStyle(ChatFormatting.LIGHT_PURPLE));
                        list.add(line);
                    }
                    if (!s.isEmpty() && !tile.isLoaded(p)) list.add(Component.literal("  ").append(TOOLKIT.translate("live.foreign").withStyle(ChatFormatting.RED)));
                }
                return list;
            }
            TileFusionCraftingCore core = tile.getCore();
            if (core != null && Math.abs(mx - cx()) <= 4.5 && Math.abs(my - cy()) <= 4.5) {
                return List.of(core.getBlockState().getBlock().getName(),
                        TOOLKIT.translate(core.isCrafting() ? "live.core.crafting" : "live.core.idle").withStyle(core.isCrafting() ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.AQUA));
            }
            return List.of(TOOLKIT.translate("live"), TOOLKIT.translate("live.info").withStyle(ChatFormatting.GRAY));
        }
    }

    /** One-row key for the live view: core, empty, loaded, hand item, charge. Entries that do not fit are left off. */
    private static class Legend extends GuiElement<Legend> implements com.brandon3055.brandonscore.client.gui.modular.lib.BackgroundRender {
        Legend(@NotNull GuiParent<?> parent) {
            super(parent);
        }

        @Override
        public void renderBackground(GuiRender render, double mouseX, double mouseY, float partialTicks) {
            String[] keys = {"core", "empty", "loaded", "foreign", "charge"};
            int[] cols = {COL_CORE_IDLE, COL_EMPTY, COL_LOADED, COL_FOREIGN, COL_CHARGE};
            double x = xMin(), y = yMin(), right = xMax();
            int text = Palette.BG.text();
            for (int i = 0; i < keys.length; i++) {
                Component label = TOOLKIT.translate("legend." + keys[i]);
                double w = 7 + render.font().width(label);
                if (x + w > right) break;
                if (i == 0) {
                    render.rect(x, y + 1, 3, 6, COL_CORE_IDLE);
                    render.rect(x + 3, y + 1, 3, 6, COL_CORE_CRAFTING);
                } else if (keys[i].equals("charge")) {
                    render.rect(x, y + 5, 5, 1, cols[i]);
                } else {
                    render.rect(x, y + 2, 5, 5, cols[i]);
                }
                render.drawString(label, x + 7, y + 1, text, false);
                x += w + 4;
            }
        }
    }

    public static class Screen extends ModularGuiContainer<FusionConductorMenu> {
        public Screen(FusionConductorMenu menu, Inventory inv, Component title) {
            super(menu, inv, new FusionConductorGui());
            getModularGui().setGuiTitle(title);
        }
    }
}
