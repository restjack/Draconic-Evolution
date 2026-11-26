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

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(FUSION, ClientEventHandler.fusionRecipes);
        registration.addIngredientInfo(DEContent.INFO_TABLET.get(), InfoTablet.JEI_INFO);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(FUSION, DEContent.CRAFTING_CORE.get(), DEContent.BASIC_CRAFTING_INJECTOR.get(),
                DEContent.WYVERN_CRAFTING_INJECTOR.get(), DEContent.AWAKENED_CRAFTING_INJECTOR.get(), DEContent.CHAOTIC_CRAFTING_INJECTOR.get());
    }

    /** Layout follows upstream's 1.21 category: catalyst above the output in the middle, injector items in side columns. */
    static class FusionCategory implements IRecipeCategory<RecipeHolder<IFusionRecipe>> {
        private static final int W = 164, H = 111;
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
            builder.addSlot(RecipeIngredientRole.INPUT, W / 2 - 8, H / 2 - 8 - 23).setStandardSlotBackground().addIngredients(recipe.getCatalyst());
            builder.addSlot(RecipeIngredientRole.OUTPUT, W / 2 - 8, H / 2 - 8 + 15).setOutputSlotBackground()
                    .addItemStack(recipe.getResultItem(Minecraft.getInstance().level.registryAccess()));
            List<Ingredient> ingredients = recipe.fusionIngredients().stream().map(i -> i.get()).toList();
            int columns = ingredients.size() > 16 ? 4 : 2;
            int rows = (int) Math.ceil(ingredients.size() / (double) columns);
            for (int i = 0; i < ingredients.size(); i++) {
                int side = (i % columns) >= columns / 2 ? 1 : -1;
                int offset = columns == 2 ? 0 : i % 2 == 0 ? -1 : 1;
                int x = W / 2 - 8 + side * (60 + offset * 10);
                int y = rows > 1 ? (H / 2 - 42) + (84 / (rows - 1)) * (i / columns) : H / 2;
                builder.addSlot(RecipeIngredientRole.INPUT, x, y - 8).setStandardSlotBackground().addIngredients(ingredients.get(i));
            }
        }

        @Override
        public void createRecipeExtras(IRecipeExtrasBuilder builder, RecipeHolder<IFusionRecipe> holder, IFocusGroup focuses) {
            IFusionRecipe recipe = holder.value();
            builder.addText(Component.translatable("gui.draconicevolution.fusion_craft.tier." + recipe.getRecipeTier().name().toLowerCase(Locale.ROOT)), W, 10)
                    .setPosition(0, 0, W, 10, HorizontalAlignment.CENTER, VerticalAlignment.TOP).setColor(0xFF8000FF);
            builder.addText(Component.literal(String.format(Locale.ROOT, "%,d OP", recipe.getEnergyCost())), W, 10)
                    .setPosition(0, H - 10, W, 10, HorizontalAlignment.CENTER, VerticalAlignment.TOP).setColor(0xFF4466FF);
        }
    }
}
