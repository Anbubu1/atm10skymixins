package com.anbubu.atm10skymixins.mixins.ae2.craftingRemainderCache;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;

@Mixin(targets = "appeng.crafting.pattern.AECraftingPattern$Input", remap = false)
public abstract class AECraftingPatternRemainderCacheMixin {

    @Unique
    private final Map<AEItemKey, Optional<AEKey>> atm10skymixins$remainingKeyCache =
        new LinkedHashMap<>(16, 0.75f, true);

    @Inject(
        method = "getRemainingKey",
        at = @At("HEAD"),
        cancellable = true
    )
    private void atm10skymixins$useCachedRemainder(
        AEKey template,
        CallbackInfoReturnable<AEKey> cir
    ) {
        if (!(template instanceof AEItemKey itemKey))
            return;

        Optional<AEKey> cached;
        synchronized (this.atm10skymixins$remainingKeyCache) {
            cached = this.atm10skymixins$remainingKeyCache.get(itemKey);
        }

        if (cached != null)
            cir.setReturnValue(cached.orElse(null));
    }

    @Inject(
        method = "getRemainingKey",
        at = @At("RETURN")
    )
    private void atm10skymixins$cacheRemainder(
        AEKey template,
        CallbackInfoReturnable<AEKey> cir
    ) {
        if (!(template instanceof AEItemKey itemKey))
            return;

        synchronized (this.atm10skymixins$remainingKeyCache) {
            this.atm10skymixins$remainingKeyCache.put(
                itemKey,
                Optional.ofNullable(cir.getReturnValue())
            );

            if (this.atm10skymixins$remainingKeyCache.size() > 128) {
                Iterator<AEItemKey> oldest = this.atm10skymixins$remainingKeyCache.keySet().iterator();
                oldest.next();
                oldest.remove();
            }
        }
    }
}
