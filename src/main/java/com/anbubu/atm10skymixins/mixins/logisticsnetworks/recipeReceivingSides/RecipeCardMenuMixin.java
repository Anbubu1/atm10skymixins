package com.anbubu.atm10skymixins.mixins.logisticsnetworks.recipeReceivingSides;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(targets = "me.almana.logisticsnetworks.menu.RecipeCardMenu", remap = false)
public abstract class RecipeCardMenuMixin {

    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 13), remap = false)
    private int atm10skymixins$shiftInventorySlotsRight(int original) {
        return 23;
    }
}
