package com.anbubu.atm10skymixins.mixins.logisticsNetworks.configSave;

import me.almana.logisticsnetworks.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
    targets = "me.almana.logisticsnetworks.client.screen.ModConfigScreen",
    remap = false
)
public abstract class LogisticsNetworksConfigSaveMixin {

    @Shadow
    private boolean canEditServerConfig;

    @Inject(
        method = "save",
        at = @At("TAIL"),
        remap = false
    )
    private void atm10skymixins$persistCommonConfig(CallbackInfo ci) {
        if (canEditServerConfig)
            Config.backoffMaxTicksSpec.save();
    }
}