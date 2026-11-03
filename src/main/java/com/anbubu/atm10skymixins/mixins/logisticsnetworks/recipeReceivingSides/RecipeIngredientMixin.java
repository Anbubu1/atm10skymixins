package com.anbubu.atm10skymixins.mixins.logisticsnetworks.recipeReceivingSides;

import com.anbubu.atm10skymixins.compat.logisticsnetworks.IngredientSideAccess;
import com.anbubu.atm10skymixins.compat.logisticsnetworks.ReceivingSide;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Mixin(targets = "me.almana.logisticsnetworks.component.RecipeCardData$Ingredient", remap = false)
public abstract class RecipeIngredientMixin implements IngredientSideAccess {

    @Shadow @Final @Mutable public static Codec<?> CODEC;

    @Unique
    private String atm10skymixins$receivingSide;

    @Override
    @Unique
    public String atm10skymixins$getReceivingSide() {
        return atm10skymixins$receivingSide;
    }

    @Override
    @Unique
    public void atm10skymixins$setReceivingSide(String side) {
        if (side != null && ReceivingSide.parse(side) == null) {
            throw new IllegalArgumentException("Unknown LogisticsNetworks receiving side: " + side);
        }
        atm10skymixins$receivingSide = side;
    }

    @Inject(method = "<clinit>", at = @At("TAIL"), remap = false)
    private static void atm10skymixins$extendIngredientCodec(CallbackInfo ci) {
        @SuppressWarnings("unchecked")
        Codec<Object> original = (Codec<Object>) CODEC;
        CODEC = new Codec<Object>() {
            @Override
            public <T> DataResult<T> encode(
                    Object ingredient,
                    DynamicOps<T> ops,
                    T prefix
            ) {
                return original.encode(ingredient, ops, prefix).flatMap(encoded -> {
                    String side = ((IngredientSideAccess) ingredient)
                            .atm10skymixins$getReceivingSide();

                    if (side == null) {
                        return DataResult.success(encoded);
                    }

                    return ops.mergeToMap(
                            encoded,
                            ops.createString("side"),
                            ops.createString(side)
                    );
                });
            }

            @Override
            public <T> DataResult<Pair<Object, T>> decode(
                    DynamicOps<T> ops,
                    T input
            ) {
                return original.decode(ops, input).flatMap(pair ->
                        atm10skymixins$decodeSide(ops, input, pair)
                );
            }
        };
    }

    @Unique
    private static <T> DataResult<Pair<Object, T>> atm10skymixins$decodeSide(
            DynamicOps<T> ops, T input, Pair<Object, T> decoded) {
        Optional<MapLike<T>> fields = ops.getMap(input).result();
        if (fields.isEmpty()) return DataResult.success(decoded);
        T encodedSide = fields.get().get("side");
        if (encodedSide == null) return DataResult.success(decoded);
        return ops.getStringValue(encodedSide).flatMap(key -> {
            if (ReceivingSide.parse(key) == null) {
                return DataResult.error(() -> "Unknown recipe receiving side: " + key);
            }
            ((IngredientSideAccess) decoded.getFirst()).atm10skymixins$setReceivingSide(key);
            return DataResult.success(decoded);
        });
    }

    @Inject(method = "withCount", at = @At("RETURN"), remap = false)
    private void atm10skymixins$copySideFromCount(int value, CallbackInfoReturnable<Object> cir) {
        atm10skymixins$copySide(cir.getReturnValue());
    }

    @Inject(method = "withSlots", at = @At("RETURN"), remap = false)
    private void atm10skymixins$copySideFromSlots(List<?> value, CallbackInfoReturnable<Object> cir) {
        atm10skymixins$copySide(cir.getReturnValue());
    }

    @Inject(method = "withSplit", at = @At("RETURN"), remap = false)
    private void atm10skymixins$copySideFromSplit(boolean value, CallbackInfoReturnable<Object> cir) {
        atm10skymixins$copySide(cir.getReturnValue());
    }

    @Inject(method = "nextTag", at = @At("RETURN"), remap = false)
    private void atm10skymixins$copySideFromNextTag(CallbackInfoReturnable<Object> cir) {
        atm10skymixins$copySide(cir.getReturnValue());
    }

    @Inject(method = "stripped", at = @At("RETURN"), remap = false)
    private void atm10skymixins$copySideFromStripped(CallbackInfoReturnable<Object> cir) {
        atm10skymixins$copySide(cir.getReturnValue());
    }

    @Unique
    private void atm10skymixins$copySide(Object copy) {
        if (copy instanceof IngredientSideAccess access) {
            access.atm10skymixins$setReceivingSide(atm10skymixins$receivingSide);
        }
    }

    @Inject(method = "sameKey", at = @At("RETURN"), cancellable = true, remap = false)
    private void atm10skymixins$includeSideInSameKey(@Coerce Object other,
            CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() && other instanceof IngredientSideAccess access
                && !Objects.equals(atm10skymixins$receivingSide, access.atm10skymixins$getReceivingSide())) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "equals", at = @At("RETURN"), cancellable = true, remap = false)
    private void atm10skymixins$includeSideInEquals(Object other, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() && other instanceof IngredientSideAccess access
                && !Objects.equals(atm10skymixins$receivingSide, access.atm10skymixins$getReceivingSide())) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "hashCode", at = @At("RETURN"), cancellable = true, remap = false)
    private void atm10skymixins$includeSideInHashCode(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(31 * cir.getReturnValue() + Objects.hashCode(atm10skymixins$receivingSide));
    }
}
