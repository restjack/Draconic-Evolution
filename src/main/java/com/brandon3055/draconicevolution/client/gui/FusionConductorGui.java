package com.brandon3055.draconicevolution.client.gui;

import com.brandon3055.brandonscore.client.BCGuiTextures;
import com.brandon3055.brandonscore.client.gui.GuiToolkit;
import com.brandon3055.brandonscore.client.gui.GuiToolkit.Palette;
import com.brandon3055.brandonscore.client.gui.modular.ModularGui;
import com.brandon3055.brandonscore.client.gui.modular.ModularGuiContainer;
import com.brandon3055.brandonscore.client.gui.modular.elements.*;
import com.brandon3055.brandonscore.client.gui.modular.lib.Constraints;
import com.brandon3055.brandonscore.client.gui.modular.lib.container.ContainerGuiProvider;
import com.brandon3055.brandonscore.client.gui.modular.lib.container.ContainerScreenAccess;
import com.brandon3055.brandonscore.client.gui.modular.lib.geometry.Direction;
import com.brandon3055.brandonscore.client.gui.modulargui.templates.ButtonRow;
import com.brandon3055.draconicevolution.blocks.tileentity.TileFusionConductor;
import com.brandon3055.draconicevolution.inventory.FusionConductorMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static com.brandon3055.brandonscore.client.gui.modular.lib.geometry.Constraint.*;
import static com.brandon3055.brandonscore.client.gui.modular.lib.geometry.GeoParam.*;

/** Fusion Conductor screen (unofficial-1.15): energy + capacitor, input 6x3, output 3x3, status, toggles. */
public class FusionConductorGui extends ContainerGuiProvider<FusionConductorMenu> {
    private static final GuiToolkit TOOLKIT = new GuiToolkit("gui.draconicevolution.fusion_conductor");
    public static final int GUI_WIDTH = 226;
    public static final int GUI_HEIGHT = 222;

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

        ButtonRow buttons = ButtonRow.topRightInside(root, Direction.DOWN, 3, 3).setSpacing(1);
        buttons.addButton(TOOLKIT::createThemeButton);
        buttons.addButton(e -> toggle(e, new ItemStack(net.minecraft.world.item.Items.LIGHTNING_ROD), tile.powerInjectors::invert, tile.powerInjectors::get,
                "gui.draconicevolution.fusion_conductor.power_injectors"));

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

        // Status box
        GuiRectangle statusBG = new GuiRectangle(root)
                .shadedRect(Palette.SubItem::accentLight, Palette.SubItem::accentDark, Palette.SubItem::fill)
                .constrain(LEFT, relative(root.get(LEFT), 5))
                .constrain(RIGHT, relative(root.get(RIGHT), -5))
                .constrain(TOP, relative(input.get(BOTTOM), 4))
                .constrain(BOTTOM, relative(playInv.container().get(TOP), -12));
        new GuiText(statusBG, () -> tile.status.get() == null ? Component.empty() : tile.status.get())
                .setWrap(true).setShadow(false).setTextColour(Palette.SubItem::text)
                .setAlignment(com.brandon3055.brandonscore.client.gui.modular.lib.geometry.Align.LEFT)
                .constrain(LEFT, relative(statusBG.get(LEFT), 3))
                .constrain(RIGHT, relative(statusBG.get(RIGHT), -3))
                .constrain(TOP, relative(statusBG.get(TOP), 3))
                .constrain(BOTTOM, relative(statusBG.get(BOTTOM), -7));
        // Progress bar along the bottom of the status box
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

    public static class Screen extends ModularGuiContainer<FusionConductorMenu> {
        public Screen(FusionConductorMenu menu, Inventory inv, Component title) {
            super(menu, inv, new FusionConductorGui());
            getModularGui().setGuiTitle(title);
        }
    }
}
