package com.anbubu.atm10skymixins.mixins.ae2.smithingFix;

import java.util.Optional;

import javax.annotation.Nonnull;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.anbubu.atm10skymixins.compat.ae2.DynamicSmithingRecipeBridge;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.item.crafting.SmithingTrimRecipe;

import appeng.api.stacks.AEItemKey;
import appeng.crafting.pattern.AESmithingTablePattern;

@Mixin(AESmithingTablePattern.class)
public abstract class AESmithingTablePatternMixin {

    @Shadow @Final
    private AEItemKey template;

    @Shadow @Final
    private AEItemKey base;

    @Shadow @Final
    private AEItemKey addition;

    @Redirect(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/crafting/RecipeManager;byKey(Lnet/minecraft/resources/ResourceLocation;)Ljava/util/Optional;"
        )
    )
    private Optional<?> atm10skymixins$bridgeCustomSmithingRecipe(
        RecipeManager manager,
        @Nonnull ResourceLocation recipeId
    ) {
        var found = manager.byKey(recipeId);

        if (found.isEmpty())
            return found;

        var holder = found.get();

        if (!(holder.value() instanceof SmithingRecipe recipe))
            return found;

        if (recipe instanceof SmithingTransformRecipe
         || recipe instanceof SmithingTrimRecipe)
            return found;

        var id = holder.id();
        if (id == null)
            return found;

        return Optional.of(
            new RecipeHolder<>(
                id,
                new DynamicSmithingRecipeBridge(
                    recipe,
                    template.toStack(),
                    base.toStack(),
                    addition.toStack()
                )
            )
        );
    }
}