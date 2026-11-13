package com.anbubu.atm10skymixins.mixins.logisticsNetworks.optimisation;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

import me.almana.logisticsnetworks.integration.storage.StorageEndpoint;
import net.minecraft.world.item.ItemStack;

@Mixin(targets = "me.almana.logisticsnetworks.integration.storage.DirectItemHandler", remap = false)
public abstract class DirectItemHandlerMixin {

    @Unique
    private boolean atm10skymixins$extractingKnownExport;

    @WrapOperation(
        remap = false,
        require = 1,
        method = "extractItem",
        at = @At(
            value = "INVOKE",
            target = "Lme/almana/logisticsnetworks/integration/storage/DirectItemHandler;extract(Lnet/minecraft/world/item/ItemStack;IZ)Lnet/minecraft/world/item/ItemStack;",
            remap = false
        )
    )
    private ItemStack atm10skymixins$withKnownExport(
        @Coerce Object instance,
        ItemStack stack,
        int amount,
        boolean simulate,
        Operation<ItemStack> original
    ) {
        this.atm10skymixins$extractingKnownExport = true;
        try {
            return original.call(instance, stack, amount, simulate);
        } finally {
            this.atm10skymixins$extractingKnownExport = false;
        }
    }

    @WrapOperation(
        remap = false,
        require = 1,
        method = "extract",
        at = @At(
            value = "INVOKE",
            target = "Lme/almana/logisticsnetworks/integration/storage/StorageEndpoint;canExportItem(Lnet/minecraft/world/item/ItemStack;)Z",
            remap = false
        )
    )
    private boolean atm10skymixins$skipKnownExportCheck(
        StorageEndpoint endpoint,
        ItemStack stack,
        Operation<Boolean> original
    ) {
        return this.atm10skymixins$extractingKnownExport || original.call(endpoint, stack);
    }
}
