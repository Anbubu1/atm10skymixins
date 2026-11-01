package com.anbubu.atm10skymixins.mixins.rftoolsutility.environmentalFix;

import java.util.HashSet;
import java.util.Set;

import mcjty.rftoolsutility.modules.environmental.EnvironmentalModule;
import mcjty.rftoolsutility.modules.environmental.blocks.EnvironmentalControllerTileEntity;
import mcjty.rftoolsutility.modules.environmental.data.EnvironmentalData;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EnvironmentalControllerTileEntity.class, remap = false)
public abstract class EnvironmentalControllerPlayersMixin {

    @Shadow
    public abstract java.util.List<?> getEnvironmentModules();

    @Inject(method = "addPlayer", at = @At("HEAD"), cancellable = true, remap = false)
    private void atm10skymixins$addPlayer(String player, CallbackInfo ci) {
        final BlockEntity self = (BlockEntity) (Object) this;
        final EnvironmentalData data = self.getData(EnvironmentalModule.ENVIRONMENTAL_DATA);
        if (!data.players().contains(player)) {
            final Set<String> next = new HashSet<>(data.players());
            next.add(player);
            self.setData(EnvironmentalModule.ENVIRONMENTAL_DATA, data.withPlayers(Set.copyOf(next)));
        }
        ci.cancel();
    }

    @Inject(method = "delPlayer", at = @At("HEAD"), cancellable = true, remap = false)
    private void atm10skymixins$delPlayer(String player, CallbackInfo ci) {
        final BlockEntity self = (BlockEntity) (Object) this;
        final EnvironmentalData data = self.getData(EnvironmentalModule.ENVIRONMENTAL_DATA);
        if (data.players().contains(player)) {
            final Set<String> next = new HashSet<>(data.players());
            next.remove(player);
            self.setData(EnvironmentalModule.ENVIRONMENTAL_DATA, data.withPlayers(Set.copyOf(next)));
        }
        ci.cancel();
    }

    @Inject(method = "deactivate", at = @At("HEAD"), remap = false)
    private void atm10skymixins$initModules(CallbackInfo ci) {
        getEnvironmentModules();
    }
}