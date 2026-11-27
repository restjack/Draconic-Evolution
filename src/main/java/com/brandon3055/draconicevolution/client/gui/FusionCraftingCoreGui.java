package com.brandon3055.draconicevolution.client.gui;

import com.brandon3055.brandonscore.client.BCGuiTextures;

import com.brandon3055.brandonscore.client.gui.GuiToolkit;

import com.brandon3055.brandonscore.client.gui.modular.ModularGui;

import com.brandon3055.brandonscore.client.gui.modular.ModularGuiContainer;

import com.brandon3055.brandonscore.client.gui.modular.elements.*;

import com.brandon3055.brandonscore.client.gui.modular.lib.Constraints;

import com.brandon3055.brandonscore.client.gui.modular.lib.container.ContainerGuiProvider;

import com.brandon3055.brandonscore.client.gui.modular.lib.container.ContainerScreenAccess;

import com.brandon3055.brandonscore.client.gui.modular.lib.geometry.Direction;

import com.brandon3055.brandonscore.client.gui.modular.lib.geometry.GuiParent;


import com.brandon3055.draconicevolution.api.DraconicAPI;

import com.brandon3055.draconicevolution.api.crafting.IFusionInjector;

import com.brandon3055.draconicevolution.api.crafting.IFusionRecipe;

import com.brandon3055.draconicevolution.blocks.tileentity.TileFusionCraftingCore;

import com.brandon3055.draconicevolution.client.DEGuiTextures;

import com.brandon3055.draconicevolution.inventory.FusionCraftingCoreMenu;

import java.util.ArrayList;

import java.util.List;

import java.util.function.Supplier;

import java.util.stream.Collectors;

import net.minecraft.network.chat.Component;

import net.minecraft.world.entity.player.Inventory;

import net.minecraft.world.item.ItemStack;

import net.minecraft.world.item.crafting.RecipeHolder;

import org.jetbrains.annotations.NotNull;

import static com.brandon3055.brandonscore.client.gui.modular.lib.geometry.Constraint.literal;

import static com.brandon3055.brandonscore.client.gui.modular.lib.geometry.Constraint.relative;

import static com.brandon3055.brandonscore.client.gui.modular.lib.geometry.GeoParam.*;





public class FusionCraftingCoreGui extends ContainerGuiProvider<FusionCraftingCoreMenu> {
    private static final GuiToolkit TOOLKIT = new GuiToolkit("gui.draconicevolution.fusion_craft");
    public static final int GUI_WIDTH = 218;
    public static final int GUI_HEIGHT = 220;

    private RecipeHolder<IFusionRecipe> currentRecipe = null;
    public GuiItemStack stackIcon;
    public Supplier<Boolean> hideRecipes;

    @Override
    public GuiElement<?> createRootElement(ModularGui gui) {
        GuiManipulable root = new GuiManipulable(gui).addMoveHandle(3).enableCursors(true);
        GuiTexture bg = new GuiTexture(root.getContentElement(), DEGuiTextures.themedGetter("fusion_craft"));
        Constraints.bind(bg, root.getContentElement());
        return root;
    }


    @Override
    public void buildGui(ModularGui gui, ContainerScreenAccess<FusionCraftingCoreMenu> screenAccess) {
        gui.initStandardGui(GUI_WIDTH, GUI_HEIGHT);
        FusionCraftingCoreMenu menu = screenAccess.getMenu();
        TileFusionCraftingCore tile = menu.tile;
        GuiElement<?> root = gui.getRoot();
        GuiText heading = TOOLKIT.createHeading(root, gui.getGuiTitle(), true);

        hideRecipes = () -> tile.getActiveRecipe() != null;

        // Auto-craft, push output, show injectors, injector mode and theme live on the Fusion Conductor screen (Eli 2026-11-26).

        var playInv = GuiSlots.playerAllSlots(root, screenAccess, menu.main, menu.hotBar, menu.armor, menu.offhand);
        playInv.stream().forEach(e -> e.setSlotTexture(slot -> BCGuiTextures.getThemed("slot")));
        Constraints.placeInside(playInv.container(), root, Constraints.LayoutPos.BOTTOM_CENTER, 0, -7);
        TOOLKIT.playerInvTitle(playInv.container());

        //Setup Craft display area
        GuiElement<?> craftArea = new GuiElement<>(root)
                .constrain(LEFT, relative(root.get(LEFT), 17))
                .constrain(RIGHT, relative(root.get(RIGHT), -17))
                .constrain(TOP, relative(heading.get(BOTTOM), 3))
                .constrain(BOTTOM, relative(playInv.container().get(TOP), -3));


        //Input/Output Slots
        GuiSlots catalyst = GuiSlots.singleSlot(craftArea, screenAccess, menu.catalyst);
        Constraints.center(catalyst, craftArea, 0, -22);

        GuiSlots output = GuiSlots.singleSlot(craftArea, screenAccess, menu.output);
        Constraints.center(output, craftArea, 0, 22);

        GuiText statusLabel = new GuiText(catalyst, tile.userStatus::get)
                .constrain(TOP, relative(heading.get(BOTTOM), 4))
                .constrain(BOTTOM, relative(catalyst.get(TOP), -4))
                .constrain(LEFT, relative(catalyst.get(LEFT), -64))
                .constrain(RIGHT, relative(catalyst.get(RIGHT), 64))
                .setWrap(true)
                .setEnabled(() -> tile.userStatus.get() != null);

        //Craft Button
        GuiButton craft = TOOLKIT.createFlat3DButton(root, () -> Component.translatable("gui.draconicevolution.fusion_craft.craft"))
                .setEnabled(() -> tile.getActiveRecipe() != null && !tile.isCrafting())
                .onPress(() -> tile.sendPacketToServer(e -> {}, 0));
        Constraints.size(craft, 80, 14);
        Constraints.placeOutside(craft, playInv.container(), Constraints.LayoutPos.TOP_CENTER, 0, -12);

        //Result Display
        stackIcon = new GuiItemStack(craftArea, ItemStack.EMPTY);
        Constraints.size(stackIcon, 20, 20);
        Constraints.center(stackIcon, craftArea);

        Constraints.bind(new IngredRenderer(craftArea, tile), craftArea);

        gui.onTick(() -> {
            currentRecipe = tile.getActiveRecipe();
            if (currentRecipe == null) {
                stackIcon.setStack(ItemStack.EMPTY);
            } else {
                stackIcon.setStack(currentRecipe.value().getResultItem(tile.getLevel().registryAccess()));
            }
        });
    }


    /** True when every linked injector is in single item mode (the client has their synced mode). */
    public static boolean allSingle(TileFusionCraftingCore tile) {
        boolean any = false;
        for (net.minecraft.core.BlockPos p : tile.getInjectorPositions()) {
            if (tile.getLevel().getBlockEntity(p) instanceof com.brandon3055.draconicevolution.blocks.tileentity.TileFusionCraftingInjector inj) {
                if (!inj.singleItem.get()) return false;
                any = true;
            }
        }
        return any;
    }

    private static String dim(TileFusionCraftingCore tile) {
        return tile.getLevel().dimension().identifier().toString();
    }

    public static boolean highlightOn(TileFusionCraftingCore tile) {
        return tile.getInjectorPositions().stream().anyMatch(p -> com.brandon3055.draconicevolution.client.render.MonitorHighlight.isOn(dim(tile), p));
    }

    /** Green = injectors the core uses. Red = injectors in range it ignores (wrong facing, blocked or too close). */
    public static void toggleHighlight(TileFusionCraftingCore tile) {
        boolean on = highlightOn(tile);
        java.util.Set<net.minecraft.core.BlockPos> linked = new java.util.HashSet<>(tile.getInjectorPositions());
        int range = com.brandon3055.draconicevolution.DEConfig.fusionInjectorRange;
        net.minecraft.core.BlockPos c = tile.getBlockPos();
        java.util.Set<net.minecraft.core.BlockPos> all = new java.util.HashSet<>(linked);
        for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) {
            for (int i = 1; i <= range; i++) {
                for (int a = -1; a <= 1; a++) for (int b = -1; b <= 1; b++) {
                    net.minecraft.core.BlockPos p = c.relative(d, i);
                    p = d.getAxis() == net.minecraft.core.Direction.Axis.X ? p.offset(0, a, b) : d.getAxis() == net.minecraft.core.Direction.Axis.Y ? p.offset(a, 0, b) : p.offset(a, b, 0);
                    if (tile.getLevel().getBlockEntity(p) instanceof com.brandon3055.draconicevolution.blocks.tileentity.TileFusionCraftingInjector) all.add(p.immutable());
                }
            }
        }
        for (net.minecraft.core.BlockPos p : all) {
            boolean lit = com.brandon3055.draconicevolution.client.render.MonitorHighlight.isOn(dim(tile), p);
            if (lit == on) com.brandon3055.draconicevolution.client.render.MonitorHighlight.toggle(dim(tile), p, linked.contains(p) ? 0x55FF55 : 0xFF5555);
        }
    }

    private static class IngredRenderer extends GuiElement<IngredRenderer> {
        private TileFusionCraftingCore core;
        private List<ItemStack> lastStacks = new ArrayList<>();

        public IngredRenderer(@NotNull GuiParent<?> parent, TileFusionCraftingCore core) {
            super(parent);
            this.core = core;
        }

        private void setInjectors() {
            double centerX = xMin() + (xSize() / 2);
            ArrayList<ArrayList<ItemStack>> columns = new ArrayList<>();
            int colCount = Math.min((int)Math.ceil(lastStacks.size() / 12D), 3) * 2;
            for (int i = 0; i < colCount; i++) {
                columns.add(new ArrayList<>());
            }
            for (int i = 0; i < lastStacks.size(); i++) {
                columns.get(i % colCount).add(lastStacks.get(i));
            }

            int innerOffset = colCount == 6 ? 44 : colCount == 4 ? 53 : 65;

            for (int column = 0; column < columns.size(); column++) {
                List<ItemStack> stacks = columns.get(column);
                int offset = (column / 2) * 20;
                double xPos = (column % 2 == 0) ? (centerX + innerOffset + offset) : (centerX - innerOffset - offset - 18);
                double yHeight = Math.min(stacks.size() * 20, ySize());
                for (int i = 0; i < stacks.size(); i++) {
                    double yPos = ((yMin() + (ySize() / 2)) - (yHeight / 2)) + (i * (yHeight / stacks.size()));
                    new GuiItemStack(this, stacks.get(i))
                            .constrain(WIDTH, literal(16))
                            .constrain(HEIGHT, literal(16))
                            .constrain(LEFT, literal(xPos))
                            .constrain(TOP, literal(yPos));
                }
            }
        }

        @Override
        public void tick(double mouseX, double mouseY) {
            List<ItemStack> stacks = core.getInjectors()
                    .stream()
                    .map(IFusionInjector::getInjectorStack)
                    .filter(injectorStack -> !injectorStack.isEmpty())
                    .collect(Collectors.toList());
            if (!stacks.equals(lastStacks)) {
                getChildren().forEach(this::removeChild);
                lastStacks = stacks;
                if (!lastStacks.isEmpty()) {
                    setInjectors();
                }
            }
            super.tick(mouseX, mouseY);
        }
    }

    public static class Screen extends ModularGuiContainer<FusionCraftingCoreMenu> {
        public Screen(FusionCraftingCoreMenu menu, Inventory inv, Component title) {
            super(menu, inv, new FusionCraftingCoreGui());
            getModularGui().setGuiTitle(title);
        }
    }
}
