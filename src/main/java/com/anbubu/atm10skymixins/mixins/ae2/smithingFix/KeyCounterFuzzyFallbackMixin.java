package com.anbubu.atm10skymixins.mixins.ae2.smithingFix;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;

import appeng.api.config.FuzzyMode;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;

@Mixin(KeyCounter.class)
public abstract class KeyCounterFuzzyFallbackMixin {

    @Shadow @Final
    private Reference2ObjectMap<Object, ?> lists;

    @Inject(
        method = "findFuzzy",
        at = @At("HEAD"),
        cancellable = true
    )
    private void atm10skymixins$fallbackForNonDurableVariants(
        AEKey key,
        FuzzyMode fuzzy,
        CallbackInfoReturnable<
            Collection<Object2LongMap.Entry<AEKey>>
        > cir
    ) {
        if (key.getFuzzySearchMaxValue() > 0)
            return;

        Object subIndex = lists.get(key.getPrimaryKey());

        if (!(subIndex instanceof Iterable<?> iterable)) {
            cir.setReturnValue(List.of());
            return;
        }

        var matches = new ArrayList<Object2LongMap.Entry<AEKey>>();

        for (Object value : iterable) {
            if (!(value instanceof Object2LongMap.Entry<?> entry))
                continue;

            if (!(entry.getKey() instanceof AEKey candidate))
                continue;

            if (!key.fuzzyEquals(candidate, fuzzy))
                continue;

            @SuppressWarnings("unchecked")
            Object2LongMap.Entry<AEKey> typedEntry = (Object2LongMap.Entry<AEKey>) entry;

            matches.add(typedEntry);
        }

        cir.setReturnValue(matches);
    }
}