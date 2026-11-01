package com.anbubu.atm10skymixins.mixins.titanium.optimisation;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
    targets = "com.hrznstudio.titanium.jei.BasicContainerScreenHandler",
    remap = false
)
public abstract class TitaniumJeiExclusionAreasMixin {

    @Inject(
        method = "getGuiExtraAreas",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private void atm10skymixins$skipExclusionAreas(
        CallbackInfoReturnable<List<?>> cir
    ) {
        cir.setReturnValue(List.of());
    }
}