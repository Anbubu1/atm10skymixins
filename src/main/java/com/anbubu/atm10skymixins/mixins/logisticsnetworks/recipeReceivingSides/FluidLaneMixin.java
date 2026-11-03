package com.anbubu.atm10skymixins.mixins.logisticsnetworks.recipeReceivingSides;

import com.anbubu.atm10skymixins.compat.logisticsnetworks.ReceivingSideRuntime;
import net.minecraft.core.HolderLookup;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(targets = "me.almana.logisticsnetworks.logic.FluidLane", remap = false)
public abstract class FluidLaneMixin {

    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    private void atm10skymixins$associateFluidRequirementsWithSides(
            IFluidHandler source,
            List<?> ingredients,
            List<?> endpoints,
            HolderLookup.Provider provider,
            @Coerce Object cache,
            CallbackInfo ci) {
        ReceivingSideRuntime.registerWanted(this, ingredients);
    }

    @Inject(method = "scaled", at = @At("HEAD"), remap = false)
    private static void atm10skymixins$selectFluidReceivingSide(FluidStack want, int sets, CallbackInfoReturnable<FluidStack> cir) {
        ReceivingSideRuntime.setForWanted(want);
    }

    @Redirect(method = "move", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"),
            remap = false)
    private boolean atm10skymixins$rememberFluidRollbackSide(List<Object> pending, Object moved) {
        ReceivingSideRuntime.rememberPending(moved);
        return pending.add(moved);
    }

    @Inject(method = "available", at = @At("HEAD"), cancellable = true, remap = false)
    private void atm10skymixins$aggregateFluidAvailability(int cap, CallbackInfoReturnable<Integer> cir) {
        try {
            cir.setReturnValue(ReceivingSideRuntime.fluidAvailable(this, cap));
        } finally {
            ReceivingSideRuntime.setCurrent(null);
        }
    }

    @Inject(method = "fit", at = @At("HEAD"), cancellable = true, remap = false)
    private void atm10skymixins$fitFluidByReceivingSide(int target, int sets, CallbackInfoReturnable<Integer> cir) {
        try {
            cir.setReturnValue(ReceivingSideRuntime.fluidFit(this, target, sets));
        } finally {
            ReceivingSideRuntime.setCurrent(null);
        }
    }

    @Inject(method = "available", at = @At("RETURN"), remap = false)
    private void atm10skymixins$clearAvailableSide(int cap, CallbackInfoReturnable<Integer> cir) {
        ReceivingSideRuntime.setCurrent(null);
    }

    @Inject(method = "fit", at = @At("RETURN"), remap = false)
    private void atm10skymixins$clearFitSide(int target, int sets, CallbackInfoReturnable<Integer> cir) {
        ReceivingSideRuntime.setCurrent(null);
    }

    @Inject(method = "move", at = @At("RETURN"), remap = false)
    private void atm10skymixins$clearMoveSide(int target, int sets, CallbackInfoReturnable<Boolean> cir) {
        ReceivingSideRuntime.setCurrent(null);
    }

    @Inject(method = "rollback", at = @At("HEAD"), cancellable = true, remap = false)
    private void atm10skymixins$rollbackFromOriginalReceivingSides(int target, CallbackInfo ci) {
        try {
            IFluidHandler source = (IFluidHandler) ReceivingSideRuntime.field(this, "source");
            @SuppressWarnings("unchecked")
            List<Object> endpoints = (List<Object>) ReceivingSideRuntime.field(this, "endpoints");
            @SuppressWarnings("unchecked")
            List<FluidStack> pending = (List<FluidStack>) ReceivingSideRuntime.field(this, "pending");
            Object transferTarget = ReceivingSideRuntime.call(endpoints.get(target), "fluids");
            IFluidHandler receiver = (IFluidHandler) ReceivingSideRuntime.call(transferTarget, "handler");
            for (FluidStack moved : List.copyOf(pending)) {
                ReceivingSideRuntime.setForPending(moved);
                FluidStack back = receiver.drain(moved, IFluidHandler.FluidAction.EXECUTE);
                int kept = back.isEmpty() ? 0 : source.fill(back, IFluidHandler.FluidAction.EXECUTE);
                if (kept < back.getAmount()) {
                    receiver.fill(back.copyWithAmount(back.getAmount() - kept), IFluidHandler.FluidAction.EXECUTE);
                }
            }
            ReceivingSideRuntime.clearLane(this);
            pending.clear();
            ci.cancel();
        } catch (Throwable error) {
            throw new IllegalStateException("Could not roll back side-specific fluid transfers", error);
        }
    }

    @Inject(method = "commit", at = @At("HEAD"), remap = false)
    private void atm10skymixins$clearReceivingSideState(int target, @Coerce Object listener, CallbackInfo ci) {
        ReceivingSideRuntime.clearLane(this);
    }
}
