package com.anbubu.atm10skymixins.mixins.logisticsnetworks.recipeReceivingSides;

import com.anbubu.atm10skymixins.compat.logisticsnetworks.ReceivingSideRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Pseudo
@Mixin(targets = "me.almana.logisticsnetworks.integration.mekanism.ChemicalLane", remap = false)
public abstract class ChemicalLaneMixin {

    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    private void atm10skymixins$associateChemicalRequirementsWithSides(
            @Coerce Object source,
            List<?> ingredients,
            List<?> endpoints,
            @Coerce Object cache,
            CallbackInfo ci) {
        ReceivingSideRuntime.registerWanted(this, ingredients);
    }

    @Inject(method = "scaled", at = @At("HEAD"), remap = false)
    private static void atm10skymixins$selectChemicalReceivingSide(
            @Coerce Object want,
            long sets,
            CallbackInfoReturnable<Object> cir) {
        ReceivingSideRuntime.setForWanted(want);
    }

    @Redirect(method = "move", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"),
            remap = false)
    private boolean atm10skymixins$rememberChemicalRollbackSide(List<Object> pending, Object moved) {
        ReceivingSideRuntime.rememberPending(moved);
        return pending.add(moved);
    }

    @Inject(method = "available", at = @At("HEAD"), cancellable = true, remap = false)
    private void atm10skymixins$aggregateChemicalAvailability(int cap, CallbackInfoReturnable<Integer> cir) {
        try {
            cir.setReturnValue(ReceivingSideRuntime.chemicalAvailable(this, cap));
        } finally {
            ReceivingSideRuntime.setCurrent(null);
        }
    }

    @Inject(method = "fit", at = @At("HEAD"), cancellable = true, remap = false)
    private void atm10skymixins$fitChemicalByReceivingSide(int target, int sets,
            CallbackInfoReturnable<Integer> cir) {
        try {
            cir.setReturnValue(ReceivingSideRuntime.chemicalFit(this, target, sets));
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
            Object source = ReceivingSideRuntime.field(this, "source");
            @SuppressWarnings("unchecked")
            List<Object> endpoints = (List<Object>) ReceivingSideRuntime.field(this, "endpoints");
            @SuppressWarnings("unchecked")
            List<Object> pending = (List<Object>) ReceivingSideRuntime.field(this, "pending");
            Object transferTarget = ReceivingSideRuntime.call(endpoints.get(target), "chemicals");
            Object receiver = ReceivingSideRuntime.call(transferTarget, "handler");
            Class<?> actionClass = Class.forName("mekanism.api.Action");
            Object execute = actionClass.getField("EXECUTE").get(null);
            for (Object moved : List.copyOf(pending)) {
                ReceivingSideRuntime.setForPending(moved);
                Object back = ReceivingSideRuntime.call(receiver, "extractChemical", moved, execute);
                boolean empty = (boolean) ReceivingSideRuntime.call(back, "isEmpty");
                Object stuck = empty ? back : ReceivingSideRuntime.call(source, "insertChemical", back, execute);
                if (!(boolean) ReceivingSideRuntime.call(stuck, "isEmpty")) {
                    ReceivingSideRuntime.call(receiver, "insertChemical", stuck, execute);
                }
            }
            ReceivingSideRuntime.clearLane(this);
            pending.clear();
            ci.cancel();
        } catch (Throwable error) {
            throw new IllegalStateException("Could not roll back side-specific chemical transfers", error);
        }
    }

    @Inject(method = "commit", at = @At("HEAD"), remap = false)
    private void atm10skymixins$clearReceivingSideState(int target, @Coerce Object listener, CallbackInfo ci) {
        ReceivingSideRuntime.clearLane(this);
    }
}
