package com.anbubu.atm10skymixins.mixins.logisticsnetworks.recipeReceivingSides;

import com.anbubu.atm10skymixins.compat.logisticsnetworks.ReceivingSideRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Set;

@Mixin(targets = "me.almana.logisticsnetworks.logic.RecipeChannel", remap = false)
public abstract class RecipeChannelMixin {

    @Inject(method = "transfer", at = @At("HEAD"), remap = false)
    private static void atm10skymixins$clearPreviousRecipeContext(CallbackInfoReturnable<Integer> cir) {
        ReceivingSideRuntime.clearCards();
    }

    @Redirect(method = "transfer", at = @At(value = "INVOKE",
            target = "Lme/almana/logisticsnetworks/logic/RecipeSets;kinds(Ljava/util/List;)Ljava/util/Set;"),
            remap = false)
    private static Set<?> atm10skymixins$captureRecipeCards(List<?> cards) {
        ReceivingSideRuntime.setCards(cards);
        try {
            @SuppressWarnings("unchecked")
            Set<?> kinds = (Set<?>) ReceivingSideRuntime.callStatic(
                    "me.almana.logisticsnetworks.logic.RecipeSets", "kinds", cards);
            return kinds;
        } catch (Exception error) {
            throw new IllegalStateException("Could not capture LogisticsNetworks recipe-card sides", error);
        }
    }

    @Inject(method = "transfer", at = @At("RETURN"), remap = false)
    private static void atm10skymixins$clearRecipeContext(CallbackInfoReturnable<Integer> cir) {
        ReceivingSideRuntime.clearCards();
    }

    @Inject(method = "endpoint", at = @At("RETURN"), cancellable = true, remap = false)
    private static void atm10skymixins$createReceivingSideTargets(
            @Coerce Object target,
            @Coerce Object source,
            Set<?> kinds,
            @Coerce Object caps,
            @Coerce Object cache,
            CallbackInfoReturnable<Object> cir) {
        cir.setReturnValue(ReceivingSideRuntime.createSideAwareEndpoint(
                target, source, caps, cache, cir.getReturnValue()));
    }
}
