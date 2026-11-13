package com.anbubu.atm10skymixins.mixins.logisticsNetworks.optimisation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(targets = "me.almana.logisticsnetworks.logic.async.TransferCommitter", remap = false)
public abstract class TransferCommitterMixin {

    @WrapOperation(
        remap = false,
        require = 1,
        method = "commitChannel",
        at = @At(
            value = "INVOKE",
            target = "Lme/almana/logisticsnetworks/integration/storage/DirectStorageHandlers;isDirect(Ljava/lang/Object;)Z",
            remap = false
        )
    )
    private static boolean atm10skymixins$disableDirectRecoveryExpansion(
        Object handler,
        Operation<Boolean> original
    ) {
        return false;
    }
}
