package com.brandon3055.draconicevolution.integration.jei;

import com.brandon3055.draconicevolution.DraconicEvolution;
import com.brandon3055.draconicevolution.api.crafting.IFusionRecipe;
import com.brandon3055.draconicevolution.client.handler.ClientEventHandler;
import com.brandon3055.draconicevolution.init.DEContent;
import com.brandon3055.draconicevolution.items.InfoTablet;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.placement.HorizontalAlignment;
import mezz.jei.api.gui.placement.VerticalAlignment;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;
import java.util.Locale;

/**
 * JEI support (eli.7, not upstream code). The port only shipped EMI support, so JEI showed no fusion recipes.
 * Recipes come from {@link ClientEventHandler#fusionRecipes}: since MC 1.21.2 the server only sends recipe types it is asked to.
 * JEI only loads this class when it is installed, so the dependency stays optional.
 */
@JeiPlugin
public class DEJeiPlugin implements IModPlugin {
    static final IRecipeHolderType<IFusionRecipe> FUSION = IRecipeHolderType.create(Identifier.fromNamespaceAndPath(DraconicEvolution.MODID, "fusion_crafting"));

    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath(DraconicEvolution.MODID, "info");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new FusionCategory(registration.getJeiHelpers().getGuiHelper().createDrawableItemLike(DEContent.CRAFTING_CORE.get())));
    }

    /** Fusion Conductor (unofficial-1.16): JEI's "+" on a fusion recipe sets the conductor's recipe lock (moves no items). */
    @Override
    public void registerRecipeTransferHandlers(mezz.jei.api.registration.IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(new mezz.jei.api.recipe.transfer.IRecipeTransferHandler<com.brandon3055.draconicevolution.inventory.FusionConductorMenu, net.minecraft.world.item.crafting.RecipeHolder<IFusionRecipe>>() {
            @Override
            public Class<? extends com.brandon3055.draconicevolution.inventory.FusionConductorMenu> getContainerClass() {
                return com.brandon3055.draconicevolution.inventory.FusionConductorMenu.class;
            }

            @Override
            public java.util.Optional<net.minecraft.world.inventory.MenuType<com.brandon3055.draconicevolution.inventory.FusionConductorMenu>> getMenuType() {
                return java.util.Optional.of(DEContent.MENU_FUSION_CONDUCTOR.get());
            }

            @Override
            public mezz.jei.api.recipe.types.IRecipeType<net.minecraft.world.item.crafting.RecipeHolder<IFusionRecipe>> getRecipeType() {
                return FUSION;
            }

            @Override
            public mezz.jei.api.recipe.transfer.IRecipeTransferError transferRecipe(com.brandon3055.draconicevolution.inventory.FusionConductorMenu menu, net.minecraft.world.item.crafting.RecipeHolder<IFusionRecipe> recipe,
                                                                                   mezz.jei.api.gui.ingredient.IRecipeSlotsView slots, net.minecraft.world.entity.player.Player player, boolean maxTransfer, boolean doTransfer) {
                if (doTransfer) menu.tile.sendPacketToServer(out -> out.writeString(recipe.id().identifier().toString()), 1);
                return null;
            }
        }, FUSION);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(FUSION, ClientEventHandler.fusionRecipes);
        registration.addIngredientInfo(DEContent.INFO_TABLET.get(), InfoTablet.JEI_INFO);
        registration.addIngredientInfo(java.util.List.of(DEContent.BASIC_CRAFTING_INJECTOR.get().asItem().getDefaultInstance(),
                        DEContent.WYVERN_CRAFTING_INJECTOR.get().asItem().getDefaultInstance(),
                        DEContent.AWAKENED_CRAFTING_INJECTOR.get().asItem().getDefaultInstance(),
                        DEContent.CHAOTIC_CRAFTING_INJECTOR.get().asItem().getDefaultInstance()),
                mezz.jei.api.constants.VanillaTypes.ITEM_STACK,
                com.brandon3055.draconicevolution.blocks.machines.CraftingInjector.MODE_INFO);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(FUSION, DEContent.CRAFTING_CORE.get(), DEContent.BASIC_CRAFTING_INJECTOR.get(),
                DEContent.WYVERN_CRAFTING_INJECTOR.get(), DEContent.AWAKENED_CRAFTING_INJECTOR.get(), DEContent.CHAOTIC_CRAFTING_INJECTOR.get());
    }

    /** Layout follows upstream's 1.21 category: catalyst above the output in the middle, injector items in side columns. */
    static class FusionCategory implements IRecipeCategory<RecipeHolder<IFusionRecipe>> {
        private static final int W = 164, H = 136; // eli.8: room for a text line above and below the ingredient columns
        private final IDrawable icon;

        FusionCategory(IDrawable icon) {
            this.icon = icon;
        }

        @Override
        public IRecipeHolderType<IFusionRecipe> getRecipeType() {
            return FUSION;
        }

        @Override
        public Component getTitle() {
            return Component.translatable(DEContent.CRAFTING_CORE.get().getDescriptionId());
        }

        @Override
        public int getWidth() {
            return W;
        }

        @Override
        public int getHeight() {
            return H;
        }

        @Override
        public IDrawable getIcon() {
            return icon;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<IFusionRecipe> holder, IFocusGroup focuses) {
            IFusionRecipe recipe = holder.value();
            // Text lines take y 0-10 and H-11..H; ingredients and the centre slots stay between them.
            int top = 14, bottom = H - 14 - 18;
            builder.addSlot(RecipeIngredientRole.INPUT, W / 2 - 8, H / 2 - 8 - 16).setStandardSlotBackground().addIngredients(recipe.getCatalyst());
            builder.addSlot(RecipeIngredientRole.OUTPUT, W / 2 - 8, H / 2 - 8 + 16).setOutputSlotBackground()
                    .addItemStack(recipe.getResultItem(Minecraft.getInstance().level.registryAccess()));
            List<Ingredient> ingredients = recipe.fusionIngredients().stream().map(i -> i.get()).toList();
            int columns = ingredients.size() > 12 ? 4 : 2;
            int rows = (int) Math.ceil(ingredients.size() / (double) columns);
            for (int i = 0; i < ingredients.size(); i++) {
                int side = (i % columns) >= columns / 2 ? 1 : -1;
                int offset = columns == 2 ? 0 : i % 2 == 0 ? -1 : 1;
                int x = W / 2 - 8 + side * (60 + offset * 10);
                int row = i / columns;
                int y = rows > 1 ? top + (bottom - top) * row / (rows - 1) : H / 2 - 8;
                builder.addSlot(RecipeIngredientRole.INPUT, x, y).setStandardSlotBackground().addIngredients(ingredients.get(i));
            }
        }

        @Override
        public void createRecipeExtras(IRecipeExtrasBuilder builder, RecipeHolder<IFusionRecipe> holder, IFocusGroup focuses) {
            IFusionRecipe recipe = holder.value();
            builder.addText(Component.translatable("gui.draconicevolution.fusion_craft.tier." + recipe.getRecipeTier().name().toLowerCase(Locale.ROOT)), W, 10)
                    .setTextAlignment(HorizontalAlignment.CENTER).setPosition(0, 0).setColor(0xFF8000FF);
            builder.addText(Component.literal(String.format(Locale.ROOT, "%,d %s", recipe.getEnergyCost(), com.brandon3055.draconicevolution.DEConfig.energyUnit())), W, 10)
                    .setTextAlignment(HorizontalAlignment.CENTER).setPosition(0, H - 10).setColor(0xFF4466FF);
        }
    }
}
