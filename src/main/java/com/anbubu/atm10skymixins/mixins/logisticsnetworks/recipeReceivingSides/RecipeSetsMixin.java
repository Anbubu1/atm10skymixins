package com.anbubu.atm10skymixins.mixins.logisticsnetworks.recipeReceivingSides;

import com.anbubu.atm10skymixins.compat.logisticsnetworks.ReceivingSideRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(targets = "me.almana.logisticsnetworks.logic.RecipeSets", remap = false)
public abstract class RecipeSetsMixin {

    @Inject(method = "usable", at = @At("HEAD"), cancellable = true, remap = false)
    private static void atm10skymixins$selectEndpointsForIngredientSides(
            @Coerce Object card,
            List<?> endpoints,
            CallbackInfoReturnable<List<Integer>> cir) {
        cir.setReturnValue(ReceivingSideRuntime.usableForCard(card, endpoints));
    }

    @Inject(method = "holds", at = @At("HEAD"), cancellable = true, remap = false)
    private static void atm10skymixins$checkBlockersOnEveryReceivingSide(
            @Coerce Object endpoint,
            List<?> blockers,
            CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(ReceivingSideRuntime.holdsOnAnySide(endpoint, blockers));
    }
}
