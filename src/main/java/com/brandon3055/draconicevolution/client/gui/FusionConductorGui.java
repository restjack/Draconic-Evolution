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
 * Fusion Conductor screen: energy + capacitor, input 6x3, output 3x3, recipe lock + batch row, status box with progress,
 * live view of the injectors, and the toggles (theme, auto-craft, push output, show injectors, injector mode, power
 * injectors, redstone mode).
 */
public class FusionConductorGui extends ContainerGuiProvider<FusionConductorMenu> {
    private static final GuiToolkit TOOLKIT = new GuiToolkit("gui.draconicevolution.fusion_conductor");
    public static final int GUI_WIDTH = 226;
    public static final int GUI_HEIGHT = 246;
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
        buttons.addButton(e -> toggle(e, new ItemStack(Items.REDSTONE_TORCH), () -> set(tile, TileFusionConductor.SET_AUTO, !tile.autoCraft.get()), tile.autoCraft::get, "gui.draconicevolution.fusion_conductor.auto_craft"));
        buttons.addButton(e -> toggle(e, new ItemStack(Items.HOPPER), () -> set(tile, TileFusionConductor.SET_PUSH, !tile.pushOutput.get()), tile.pushOutput::get, "gui.draconicevolution.fusion_conductor.push_output"));
        buttons.addButton(e -> toggle(e, new ItemStack(Items.ENDER_EYE), () -> { TileFusionCraftingCore c = tile.getCore(); if (c != null) FusionCraftingCoreGui.toggleHighlight(c); },
                () -> tile.getCore() != null && FusionCraftingCoreGui.highlightOn(tile.getCore()), "gui.draconicevolution.fusion_craft.show_injectors"));
        buttons.addButton(e -> {
            GuiButton b = TOOLKIT.createFlat3DButton(e, () -> Component.literal(allSingle(tile) ? "1" : "64"))
                    .setTooltip(() -> List.of(Component.translatable("gui.draconicevolution.fusion_craft.injector_mode"),
                            com.brandon3055.draconicevolution.blocks.machines.CraftingInjector.modeText(allSingle(tile)),
                            Component.translatable("gui.draconicevolution.fusion_craft.injector_mode.info").withStyle(ChatFormatting.GRAY)))
                    .onPress(() -> {
                        boolean single = !allSingle(tile);
                        tile.sendPacketToServer(out -> out.writeBoolean(single), 3);
                    });
            Constraints.size(b, 12, 12);
            return b;
        });
        buttons.addButton(e -> toggle(e, new ItemStack(Items.LIGHTNING_ROD), () -> set(tile, TileFusionConductor.SET_POWER, !tile.powerInjectors.get()), tile.powerInjectors::get,
                "gui.draconicevolution.fusion_conductor.power_injectors"));
        buttons.addButton(e -> {
            GuiButton b = TOOLKIT.createFlat3DButton(e, null)
                    .setTooltip(() -> List.of(TOOLKIT.translate("redstone"),
                            TOOLKIT.translate("redstone." + RS_KEYS[mode(tile)]).withStyle(ChatFormatting.GOLD),
                            TOOLKIT.translate("redstone.info").withStyle(ChatFormatting.GRAY)))
                    .onPress(() -> set(tile, TileFusionConductor.SET_RS, (mode(tile) + 1) % 4), GuiButton.LEFT_CLICK)
                    .onPress(() -> set(tile, TileFusionConductor.SET_RS, (mode(tile) + 3) % 4), GuiButton.RIGHT_CLICK);
            Constraints.size(b, 12, 12);
            GuiItemStack icon = new GuiItemStack(b, new ItemStack(RS_ICONS[0])).enableStackToolTip(false);
            Constraints.bind(icon, b, 1);
            gui.onTick(() -> icon.setStack(new ItemStack(RS_ICONS[mode(tile)])));
            return b;
        });

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

        // Energy bar + capacitor slot (left)
        GuiSlots cap = GuiSlots.singleSlot(root, screenAccess, menu.capacitor, 0)
                .setSlotTexture(slot -> BCGuiTextures.getThemed("slot"))
                .setEmptyIcon(BCGuiTextures.get("slots/energy"))
                .setTooltip(() -> List.of(TOOLKIT.translate("capacitor"), speedLine(tile).copy().withStyle(ChatFormatting.DARK_PURPLE), TOOLKIT.translate("capacitor.info").withStyle(ChatFormatting.GRAY)))
                .constrain(LEFT, relative(root.get(LEFT), 5))
                .constrain(BOTTOM, match(input.get(BOTTOM)));
        var energyBar = TOOLKIT.createEnergyBar(root, tile.opStorage);
        energyBar.container()
                .constrain(TOP, relative(heading.get(BOTTOM), 4))
                .constrain(LEFT, relative(root.get(LEFT), 7))
                .constrain(BOTTOM, relative(cap.get(TOP), -2))
                .constrain(WIDTH, literal(14));

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

        // Live view (right) + status box (left of it)
        LiveView live = new LiveView(root, tile);
        live.constrain(RIGHT, relative(root.get(RIGHT), -5))
                .constrain(TOP, relative(lock.get(BOTTOM), 3))
                .constrain(BOTTOM, relative(playInv.container().get(TOP), -12))
                .constrain(WIDTH, dynamic(() -> live.ySize()));
        GuiRectangle statusBG = new GuiRectangle(root)
                .shadedRect(Palette.SubItem::accentLight, Palette.SubItem::accentDark, Palette.SubItem::fill)
                .constrain(LEFT, relative(root.get(LEFT), 5))
                .constrain(RIGHT, relative(live.get(LEFT), -3))
                .constrain(TOP, match(live.get(TOP)))
                .constrain(BOTTOM, match(live.get(BOTTOM)));
        new GuiText(statusBG, () -> tile.status.get() == null ? Component.empty() : tile.status.get())
                .setWrap(true).setShadow(false).setTextColour(Palette.SubItem::text)
                .setAlignment(Align.LEFT)
                .constrain(LEFT, relative(statusBG.get(LEFT), 3))
                .constrain(RIGHT, relative(statusBG.get(RIGHT), -3))
                .constrain(TOP, relative(statusBG.get(TOP), 3))
                .constrain(BOTTOM, relative(statusBG.get(BOTTOM), -7));
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

    static GuiButton toggle(GuiElement<?> parent, ItemStack icon, Runnable press, java.util.function.Supplier<Boolean> state, String key) {
        GuiButton button = TOOLKIT.createFlat3DButton(parent, null)
                .setTooltip(() -> List.of(Component.translatable(key),
                        Component.translatable(state.get() ? "gui.draconicevolution.toggle.on" : "gui.draconicevolution.toggle.off")
                                .withStyle(state.get() ? ChatFormatting.GREEN : ChatFormatting.RED),
                        Component.translatable(key + ".info").withStyle(ChatFormatting.GRAY)))
                .onPress(press)
                .setToggleMode(state);
        Constraints.size(button, 12, 12);
        new GuiRectangle(button).fill(0xB040C040).setEnabled(state)
                .constrain(LEFT, relative(button.get(LEFT), 1)).constrain(TOP, relative(button.get(TOP), 1))
                .constrain(RIGHT, relative(button.get(RIGHT), -1)).constrain(BOTTOM, relative(button.get(BOTTOM), -1));
        Constraints.bind(new GuiItemStack(button, icon).enableStackToolTip(false), button, 1);
        return button;
    }

    /**
     * Top-down map of the core (centre) and its injectors. Green: loaded by the conductor. Red: holds an item placed by
     * hand (blocks the conductor). Grey: empty. Injectors above or below the core sit on a small ring around the centre.
     */
    private static class LiveView extends GuiElement<LiveView> implements com.brandon3055.brandonscore.client.gui.modular.lib.BackgroundRender {
        private final TileFusionConductor tile;
        private double mx, my;

        LiveView(@NotNull GuiParent<?> parent, TileFusionConductor tile) {
            super(parent);
            this.tile = tile;
            setTooltip(this::hoverText);
        }

        private record Dot(BlockPos pos, double x, double y, TileFusionCraftingInjector inj) {}

        private List<Dot> dots() {
            List<Dot> out = new ArrayList<>();
            TileFusionCraftingCore core = tile.getCore();
            if (core == null) return out;
            double cx = xMin() + xSize() / 2, cy = yMin() + ySize() / 2;
            double scale = (xSize() / 2 - 4) / Math.max(1, DEConfig.fusionInjectorRange);
            for (BlockPos p : core.getInjectorPositions()) {
                if (!(tile.getLevel().getBlockEntity(p) instanceof TileFusionCraftingInjector inj)) continue;
                int dx = p.getX() - core.getBlockPos().getX(), dy = p.getY() - core.getBlockPos().getY(), dz = p.getZ() - core.getBlockPos().getZ();
                double x, y;
                if (Math.abs(dy) > Math.max(Math.abs(dx), Math.abs(dz))) {
                    double a = Math.atan2(dz, dx) + (dy > 0 ? 0 : Math.PI / 4);
                    x = cx + Math.cos(a) * 6;
                    y = cy + Math.sin(a) * 6;
                } else {
                    x = cx + dx * scale;
                    y = cy + dz * scale;
                }
                out.add(new Dot(p, x, y, inj));
            }
            return out;
        }

        private int colour(Dot d) {
            if (d.inj().getInjectorStack().isEmpty()) return 0xFF808080;
            return tile.isLoaded(d.pos()) ? 0xFF40D040 : 0xFFE04040;
        }

        @Override
        public void renderBackground(GuiRender render, double mouseX, double mouseY, float partialTicks) {
            mx = mouseX;
            my = mouseY;
            render.borderRect(xMin(), yMin(), xSize(), ySize(), 1, 0xFF1A1A1A, 0xFF555555);
            double cx = xMin() + xSize() / 2, cy = yMin() + ySize() / 2;
            TileFusionCraftingCore core = tile.getCore();
            render.rect(cx - 2.5, cy - 2.5, 5, 5, core != null && core.isCrafting() ? 0xFF9B6BFF : 0xFF58C8FF);
            for (Dot d : dots()) {
                render.rect(d.x() - 1.5, d.y() - 1.5, 3, 3, colour(d));
                if (d.inj().getEnergyRequirement() > 0) {
                    double f = Math.min(1, d.inj().getInjectorEnergy() / (double) d.inj().getEnergyRequirement());
                    render.rect(d.x() - 1.5, d.y() + 2, 3 * f, 1, 0xFF9B6BFF);
                }
            }
        }

        private List<Component> hoverText() {
            for (Dot d : dots()) {
                if (Math.abs(mx - d.x()) <= 2.5 && Math.abs(my - d.y()) <= 2.5) {
                    List<Component> list = new ArrayList<>();
                    BlockPos p = d.pos();
                    list.add(Component.translatable("tech_level.draconicevolution." + d.inj().getInjectorTier().name().toLowerCase())
                            .append(" " + p.getX() + " " + p.getY() + " " + p.getZ()));
                    ItemStack s = d.inj().getInjectorStack();
                    list.add(s.isEmpty() ? TOOLKIT.translate("live.empty").withStyle(ChatFormatting.GRAY)
                            : s.getHoverName().copy().withStyle(tile.isLoaded(p) ? ChatFormatting.GREEN : ChatFormatting.RED));
                    if (!s.isEmpty() && !tile.isLoaded(p)) list.add(TOOLKIT.translate("live.foreign").withStyle(ChatFormatting.RED));
                    if (d.inj().getEnergyRequirement() > 0) list.add(Component.literal(Math.round(100D * d.inj().getInjectorEnergy() / d.inj().getEnergyRequirement()) + "%").withStyle(ChatFormatting.LIGHT_PURPLE));
                    return list;
                }
            }
            return List.of(TOOLKIT.translate("live"), TOOLKIT.translate("live.info").withStyle(ChatFormatting.GRAY));
        }
    }

    public static class Screen extends ModularGuiContainer<FusionConductorMenu> {
        public Screen(FusionConductorMenu menu, Inventory inv, Component title) {
            super(menu, inv, new FusionConductorGui());
            getModularGui().setGuiTitle(title);
        }
    }
}
