package com.anbubu.atm10skymixins.compat.ae2;

import javax.annotation.Nonnull;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.level.Level;

public final class DynamicSmithingRecipeBridge
        extends SmithingTransformRecipe {

    private final SmithingRecipe delegate;

    public DynamicSmithingRecipeBridge(
        SmithingRecipe delegate,
        ItemStack template,
        ItemStack base,
        ItemStack addition
    ) {
        super(
            Ingredient.of(template),
            Ingredient.of(base),
            Ingredient.of(addition),
            ItemStack.EMPTY
        );

        this.delegate = delegate;
    }

    @Override
    public boolean matches(
        @Nonnull SmithingRecipeInput input,
        @Nonnull Level level
    ) {
        return delegate.matches(input, level);
    }

    @Override
    public ItemStack assemble(
        @Nonnull SmithingRecipeInput input,
        @Nonnull HolderLookup.Provider registries
    ) {
        return delegate.assemble(input, registries);
    }

    @Override
    public ItemStack getResultItem(
        @Nonnull HolderLookup.Provider registries
    ) {
        return delegate.getResultItem(registries);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return delegate.getSerializer();
    }

    @Override
    public boolean isTemplateIngredient(@Nonnull ItemStack stack) {
        return delegate.isTemplateIngredient(stack);
    }

    @Override
    public boolean isBaseIngredient(@Nonnull ItemStack stack) {
        return delegate.isBaseIngredient(stack);
    }

    @Override
    public boolean isAdditionIngredient(@Nonnull ItemStack stack) {
        return delegate.isAdditionIngredient(stack);
    }

    @Override
    public boolean isIncomplete() {
        return delegate.isIncomplete();
    }
}