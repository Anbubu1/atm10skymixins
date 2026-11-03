package com.anbubu.atm10skymixins.mixins.logisticsnetworks.recipeReceivingSides;

import com.anbubu.atm10skymixins.compat.logisticsnetworks.IngredientSideAccess;
import com.anbubu.atm10skymixins.compat.logisticsnetworks.ReceivingSide;
import net.minecraft.core.HolderLookup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;

@Mixin(targets = "me.almana.logisticsnetworks.client.lnet.LnetRecipeText", remap = false)
public abstract class LnetRecipeTextMixin {

    @Unique
    private static final ThreadLocal<String> atm10skymixins$parsedSide = new ThreadLocal<>();

    @ModifyVariable(method = "readIngredient", at = @At("HEAD"), argsOnly = true, ordinal = 0, remap = false)
    private static String atm10skymixins$readReceivingSide(String text) throws IOException {
        atm10skymixins$parsedSide.remove();
        int sideAt = text.lastIndexOf(" side ");
        if (sideAt < 0) return text;
        String key = text.substring(sideAt + " side ".length()).trim();
        if (ReceivingSide.parse(key) == null) {
            throw new IOException("unknown receiving side '" + key + "'");
        }
        atm10skymixins$parsedSide.set(key);
        return text.substring(0, sideAt).trim();
    }

    @Inject(method = "readIngredient", at = @At("RETURN"), remap = false)
    private static void atm10skymixins$applyParsedReceivingSide(String text, HolderLookup.Provider registries,
            CallbackInfoReturnable<Object> cir) {
        try {
            Object ingredient = cir.getReturnValue();
            if (ingredient instanceof IngredientSideAccess access) {
                access.atm10skymixins$setReceivingSide(atm10skymixins$parsedSide.get());
            }
        } finally {
            atm10skymixins$parsedSide.remove();
        }
    }

    @Inject(method = "writeIngredient", at = @At("RETURN"), cancellable = true, remap = false)
    private static void atm10skymixins$writeReceivingSide(@Coerce Object ingredient,
            HolderLookup.Provider registries, CallbackInfoReturnable<String> cir) {
        String side = ingredient instanceof IngredientSideAccess access
                ? access.atm10skymixins$getReceivingSide() : null;
        if (side != null) cir.setReturnValue(cir.getReturnValue() + " side " + side);
    }
}
