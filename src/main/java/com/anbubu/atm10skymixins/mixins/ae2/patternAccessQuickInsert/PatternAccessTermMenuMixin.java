package com.anbubu.atm10skymixins.mixins.ae2.patternAccessQuickInsert;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import appeng.api.stacks.AEItemKey;
import appeng.menu.implementations.PatternAccessTermMenu;

@Mixin(PatternAccessTermMenu.class)
public abstract class PatternAccessTermMenuMixin {

    @Unique
    private static final ResourceLocation atm10skymixins$ASSEMBLER_MATRIX_PATTERN_ID =
        ResourceLocation.fromNamespaceAndPath("extendedae", "assembler_matrix_pattern");

    @WrapOperation(
        method = "quickMovePattern",
        at = @At(
            value = "INVOKE",
            target = "Lappeng/api/stacks/AEItemKey;is(Lnet/minecraft/world/level/ItemLike;)Z"
        )
    )
    private boolean atm10skymixins$allowAssemblerMatrixQuickInsert(
        AEItemKey providerIcon,
        ItemLike item,
        Operation<Boolean> original
    ) {
        return original.call(providerIcon, item)
            || providerIcon.getId().equals(atm10skymixins$ASSEMBLER_MATRIX_PATTERN_ID);
    }
}