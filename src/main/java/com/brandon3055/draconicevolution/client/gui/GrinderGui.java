package com.brandon3055.draconicevolution.client.gui;

import com.brandon3055.brandonscore.client.BCGuiTextures;

import com.brandon3055.brandonscore.client.gui.GuiToolkit.Palette;

import com.brandon3055.brandonscore.client.gui.GuiToolkit;

import com.brandon3055.brandonscore.client.gui.InfoPanel;

import com.brandon3055.brandonscore.client.gui.modular.ModularGui;

import com.brandon3055.brandonscore.client.gui.modular.ModularGuiContainer;

import com.brandon3055.brandonscore.client.gui.modular.elements.*;

import com.brandon3055.brandonscore.client.gui.modular.lib.Constraints;

import com.brandon3055.brandonscore.client.gui.modular.lib.container.ContainerGuiProvider;

import com.brandon3055.brandonscore.client.gui.modular.lib.container.ContainerScreenAccess;

import com.brandon3055.brandonscore.client.gui.modular.lib.geometry.Direction;

import com.brandon3055.brandonscore.client.gui.modulargui.GuiEntityFilter;

import com.brandon3055.brandonscore.client.gui.modulargui.templates.ButtonRow;

import com.brandon3055.draconicevolution.blocks.tileentity.TileGrinder;

import com.brandon3055.draconicevolution.client.DEGuiTextures;

import com.brandon3055.draconicevolution.inventory.GrinderMenu;

import net.minecraft.client.Minecraft;

import net.minecraft.network.chat.Component;

import net.minecraft.world.entity.player.Inventory;

import static com.brandon3055.brandonscore.client.gui.modular.lib.geometry.Constraint.*;

import static com.brandon3055.brandonscore.client.gui.modular.lib.geometry.GeoParam.*;

import static net.minecraft.ChatFormatting.GOLD;

import static net.minecraft.ChatFormatting.GRAY;




/**
 * Created by brandon3055 on 30/3/2016.
 */
public class GrinderGui extends ContainerGuiProvider<GrinderMenu> {
    private static GuiToolkit TOOLKIT = new GuiToolkit("gui.draconicevolution.grinder");
    public static final int GUI_WIDTH = 270;
    public static final int GUI_HEIGHT = 200;
    private boolean largeView = false;
    private TileGrinder tile;

    @Override
    public GuiElement<?> createRootElement(ModularGui gui) {
        GuiManipulable root = new GuiManipulable(gui).addMoveHandle(3).enableCursors(true);
        GuiTexture bg = new GuiTexture(root.getContentElement(), DEGuiTextures.themedGetter("grinder"));
        Constraints.bind(bg, root.getContentElement());
        return root;
    }

    @Override
    public void buildGui(ModularGui gui, ContainerScreenAccess<GrinderMenu> screenAccess) {
        gui.initStandardGui(GUI_WIDTH, GUI_HEIGHT);
        GrinderMenu menu = screenAccess.getMenu();
        tile = menu.tile;
        GuiElement<?> root = gui.getRoot();
        TOOLKIT.createHeading(root, gui.getGuiTitle(), true);

        InfoPanel infoPanel = InfoPanel.create(root);
        infoPanel.labeledValue(TOOLKIT.translate("stored_xp").withStyle(GOLD), () -> TOOLKIT.translate("xp_value", tile.storedXP.get()).withStyle(GRAY));

        ButtonRow buttonRow = ButtonRow.topRightInside(root, Direction.DOWN, 3, 3).setSpacing(1);
        buttonRow.addButton(e -> TOOLKIT.createThemeButton(e));
        buttonRow.addButton(e -> TOOLKIT.createInfoButton(e, infoPanel));
        buttonRow.addButton(e -> TOOLKIT.createRSSwitch(e, screenAccess.getMenu().tile));

        //Inventory
        GuiRectangle invBG = new GuiRectangle(root)
                .shadedRect(Palette.SubItem::accentLight, Palette.SubItem::accentDark, Palette.SubItem::fill);
        var playInv = GuiSlots.player(invBG, screenAccess, menu.main, menu.hotBar);
        playInv.stream().forEach(e -> e.setSlotTexture(slot -> BCGuiTextures.getThemed("slot")));
        Constraints.placeInside(playInv.container(), root, Constraints.LayoutPos.BOTTOM_RIGHT, -7, -7);
        Constraints.bind(invBG, playInv.container(), -13, -2, -2, -2);
        TOOLKIT.playerInvTitle(playInv.container());

        //Power
        GuiSlots capInv = GuiSlots.singleSlot(root, screenAccess, menu.capacitor, 0)
                .setSlotTexture(slot -> BCGuiTextures.getThemed("slot"))
                .setEmptyIcon(BCGuiTextures.get("slots/energy"))
                .constrain(LEFT, relative(root.get(LEFT), 5))
                .constrain(BOTTOM, relative(invBG.get(TOP),-2));
        var energyBar = TOOLKIT.createEnergyBar(root, tile.opStorage);
        energyBar.container()
                .constrain(TOP, relative(root.get(TOP), 14))
                .constrain(LEFT, relative(root.get(LEFT), 7))
                .constrain(BOTTOM, relative(capInv.get(TOP), -12))
                .constrain(WIDTH, literal(14));
        Constraints.placeOutside(TOOLKIT.energySlotArrow(root, false, true), capInv, Constraints.LayoutPos.TOP_CENTER, 0, -1);

        //Settings row: weapon slot, AOE size, three icon toggles
        GuiSlots weaponInv = GuiSlots.singleSlot(root, screenAccess, menu.weapon, 0)
                .setSlotTexture(slot -> BCGuiTextures.getThemed("slot"))
                .setTooltipSingle(TOOLKIT.translate("weapon_slot"))
                .setEmptyIcon(BCGuiTextures.get("slots/sword"))
                .constrain(LEFT, relative(root.get(LEFT), 4))
                .constrain(TOP, relative(invBG.get(TOP), 1));

        GuiButton aoeSize = TOOLKIT.createFlat3DButton(root, () -> Component.literal(getAOEString()))
                .setTooltip(() -> java.util.List.of(TOOLKIT.translate("aoe").append(" " + getAOEString()), TOOLKIT.translate("aoe.info")))
                .onPress(() -> modifyAOE(Minecraft.getInstance().hasShiftDown()), GuiButton.LEFT_CLICK)
                .onPress(() -> modifyAOE(true), GuiButton.RIGHT_CLICK)
                .constrain(WIDTH, literal(23))
                .constrain(HEIGHT, literal(16))
                .constrain(TOP, relative(invBG.get(TOP), 2))
                .constrain(LEFT, relative(root.get(LEFT), 24));

        GuiButton showAOE = iconToggle(root, net.minecraft.world.item.Items.ENDER_EYE, tile.showAOE::invert, tile.showAOE::get,
                TOOLKIT.translate("show_aoe"))
                .constrain(LEFT, relative(aoeSize.get(RIGHT), 2))
                .constrain(TOP, match(aoeSize.get(TOP)));
        GuiButton collectItems = iconToggle(root, net.minecraft.world.item.Items.HOPPER, tile.collectItems::invert, tile.collectItems::get,
                TOOLKIT.translate("collect.items"), TOOLKIT.translate("collect.items.info"))
                .constrain(LEFT, relative(showAOE.get(RIGHT), 1))
                .constrain(TOP, match(aoeSize.get(TOP)));
        iconToggle(root, net.minecraft.world.item.Items.EXPERIENCE_BOTTLE, tile.collectXP::invert, tile.collectXP::get,
                TOOLKIT.translate("collect.xp"), TOOLKIT.translate("collect.xp.info"))
                .constrain(LEFT, relative(collectItems.get(RIGHT), 1))
                .constrain(TOP, match(aoeSize.get(TOP)));

        //XP panel: level number + bar like the player's, take / store buttons (Shift 10, Ctrl all)
        GuiRectangle xpBG = new GuiRectangle(root)
                .shadedRect(Palette.SubItem::accentLight, Palette.SubItem::accentDark, Palette.SubItem::fill)
                .constrain(LEFT, relative(root.get(LEFT), 4))
                .constrain(RIGHT, relative(invBG.get(LEFT), -2))
                .constrain(TOP, relative(invBG.get(TOP), 22))
                .constrain(BOTTOM, match(invBG.get(BOTTOM)));
        new GuiText(xpBG, TOOLKIT.translate("xp_stored"))
                .setTextColour(Palette.BG::text).setShadow(false)
                .constrain(TOP, relative(xpBG.get(TOP), 3))
                .constrain(LEFT, relative(xpBG.get(LEFT), 2))
                .constrain(RIGHT, relative(xpBG.get(RIGHT), -2))
                .constrain(HEIGHT, literal(9));
        new GuiText(xpBG, () -> Component.literal(String.valueOf((int) TileGrinder.levelOf(tile.storedXP.get()))))
                .setTextColour(0xFF80FF20).setShadow(true)
                .setTooltip(() -> java.util.List.of(TOOLKIT.translate("xp_value", tile.storedXP.get())))
                .constrain(TOP, relative(xpBG.get(TOP), 14))
                .constrain(LEFT, relative(xpBG.get(LEFT), 2))
                .constrain(RIGHT, relative(xpBG.get(RIGHT), -2))
                .constrain(HEIGHT, literal(9));
        GuiRectangle barBG = new GuiRectangle(xpBG).fill(0xFF000000)
                .constrain(TOP, relative(xpBG.get(TOP), 25))
                .constrain(LEFT, relative(xpBG.get(LEFT), 5))
                .constrain(RIGHT, relative(xpBG.get(RIGHT), -5))
                .constrain(HEIGHT, literal(5));
        new GuiRectangle(barBG).fill(0xFF80FF20)
                .constrain(TOP, relative(barBG.get(TOP), 1))
                .constrain(BOTTOM, relative(barBG.get(BOTTOM), -1))
                .constrain(LEFT, relative(barBG.get(LEFT), 1))
                .constrain(WIDTH, dynamic(() -> {
                    double l = TileGrinder.levelOf(tile.storedXP.get());
                    return (barBG.xSize() - 2) * (l - (int) l);
                }));

        GuiButton take = TOOLKIT.createFlat3DButton(xpBG, () -> TOOLKIT.translate("xp_take"))
                .setTooltip(TOOLKIT.translate("xp_take.info"), TOOLKIT.translate("xp_modifiers"))
                .onPress(() -> sendXP(4))
                .constrain(HEIGHT, literal(14))
                .constrain(TOP, relative(barBG.get(BOTTOM), 4))
                .constrain(LEFT, relative(xpBG.get(LEFT), 3))
                .constrain(WIDTH, dynamic(() -> (xpBG.xSize() - 7) / 2D));
        TOOLKIT.createFlat3DButton(xpBG, () -> TOOLKIT.translate("xp_store"))
                .setTooltip(TOOLKIT.translate("xp_store.info"), TOOLKIT.translate("xp_modifiers"))
                .onPress(() -> sendXP(5))
                .constrain(HEIGHT, literal(14))
                .constrain(TOP, match(take.get(TOP)))
                .constrain(LEFT, relative(take.get(RIGHT), 1))
                .constrain(RIGHT, relative(xpBG.get(RIGHT), -3));

        //Entity Filter
        GuiButton largeViewButton = TOOLKIT.createResizeButton(root)
                .onPress(() -> largeView = !largeView);
        Constraints.placeOutside(largeViewButton, invBG, Constraints.LayoutPos.TOP_RIGHT, -10, -2);

        GuiButton closeLarge = TOOLKIT.createFlat3DButton(root, () -> TOOLKIT.translate("close_large_view"))
                .setEnabled(() -> largeView)
                .onPress(() -> largeView = !largeView)
                .jeiExclude();

        Constraints.size(closeLarge, 140, 14);
        Constraints.placeOutside(closeLarge, root, Constraints.LayoutPos.BOTTOM_CENTER, 0, 1);

        GuiRectangle filterBG = new GuiRectangle(root)
                .setOpaque(true)
                .shadedRect(Palette.Slot::accentDark, Palette.Slot::accentLight, Palette.Slot::fill)
                .constrain(TOP, relative(root.get(TOP), () -> largeView ? 4 : 14D))
                .constrain(LEFT, relative(root.get(LEFT), () -> largeView ? 4 : 25D))
                .constrain(RIGHT, relative(root.get(RIGHT), () -> largeView ? -4 : -16D))
                .constrain(BOTTOM, dynamic(() -> largeView ? (root.getValue(BOTTOM) -4) : (invBG.getValue(TOP) -2)));

        GuiEntityFilter filterUI = new GuiEntityFilter(filterBG, tile.entityFilter)
                .setNodeBgBuilder(e -> new GuiRectangle(e).shadedRect(() -> Palette.Ctrl.accentLight(false), () -> Palette.Ctrl.accentDark(false), () -> Palette.Ctrl.fill(false)))
                .setTitleTextColour(Palette.Slot::text)
                .setScrollBarCustomizer(guiSlider -> Constraints.bind(new GuiRectangle(guiSlider).fill(Palette.SubItem::accentDark), guiSlider.getSlider()))
                .initFilter();
        Constraints.bind(filterUI, filterBG, 2);
    }

    private GuiButton iconToggle(GuiElement<?> parent, net.minecraft.world.item.Item icon, Runnable invert, java.util.function.Supplier<Boolean> state, Component... tooltip) {
        GuiButton button = TOOLKIT.createFlat3DButton(parent, null)
                .setTooltip(tooltip)
                .onPress(invert)
                .setToggleMode(state)
                .constrain(WIDTH, literal(16))
                .constrain(HEIGHT, literal(16));
        // Green while on, plus an On/Off line, so the state is obvious (the pressed shading alone was not).
        new GuiRectangle(button).fill(0xB040C040).setEnabled(state)
                .constrain(LEFT, relative(button.get(LEFT), 1)).constrain(TOP, relative(button.get(TOP), 1))
                .constrain(RIGHT, relative(button.get(RIGHT), -1)).constrain(BOTTOM, relative(button.get(BOTTOM), -1));
        Constraints.bind(new GuiItemStack(button, new net.minecraft.world.item.ItemStack(icon)).enableStackToolTip(false), button);
        return button;
    }

    /** id 4 = take, 5 = store: 1 level, Shift 10, Ctrl all (0). */
    private void sendXP(int id) {
        Minecraft mc = Minecraft.getInstance();
        int levels = mc.hasControlDown() ? 0 : mc.hasShiftDown() ? 10 : 1;
        tile.sendPacketToServer(output -> output.writeByte(id).writeInt(levels), 1);
    }

    private String getAOEString() {
        int aoe = 1 + (tile.aoe.get() - 1) * 2;
        return aoe + "x" + aoe;
    }

    private void modifyAOE(boolean dec) {
        int aoe = tile.aoe.get();
        tile.aoe.set((byte) (dec ? aoe == 1 ? tile.getMaxAOE() : aoe - 1 : aoe == tile.getMaxAOE() ? 1 : aoe + 1));
    }

    public static class Screen extends ModularGuiContainer<GrinderMenu> {
        public Screen(GrinderMenu menu, Inventory inv, Component title) {
            super(menu, inv, new GrinderGui());
            getModularGui().setGuiTitle(title);
        }
    }
}
