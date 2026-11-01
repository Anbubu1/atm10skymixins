package com.anbubu.atm10skymixins.mixins.ae2.smithingFix;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.anbubu.atm10skymixins.compat.ae2.SmithingPatternBatchEncoder;

import net.minecraft.world.entity.player.Player;

import appeng.menu.AEBaseMenu;
import appeng.menu.me.items.PatternEncodingTermMenu;
import appeng.menu.slot.RestrictedInputSlot;
import appeng.parts.encoding.EncodingMode;
import appeng.util.ConfigInventory;

@Mixin(PatternEncodingTermMenu.class)
public abstract class PatternEncodingTermMenuMixin {

    @Shadow @Final
    private ConfigInventory encodedInputsInv;

    @Shadow @Final
    private RestrictedInputSlot blankPatternSlot;

    @Shadow @Final
    private RestrictedInputSlot encodedPatternSlot;

    @Shadow
    public EncodingMode mode;

    @Inject(
        method = "encode",
        at = @At("HEAD"),
        cancellable = true
    )
    private void atm10skymixins$batchEncodeDynamicSmithing(
        CallbackInfo ci
    ) {
        if (mode != EncodingMode.SMITHING_TABLE)
            return;

        Player player = ((AEBaseMenu) (Object) this).getPlayer();

        if (player.level().isClientSide())
            return;

        boolean handled = SmithingPatternBatchEncoder.encodeIfCustom(
            player,
            encodedInputsInv,
            blankPatternSlot,
            encodedPatternSlot
        );

        if (handled)
            ci.cancel();
    }
}