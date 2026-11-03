package com.anbubu.atm10skymixins.mixins.logisticsnetworks.recipeReceivingSides;

import com.anbubu.atm10skymixins.compat.logisticsnetworks.ReceivingSideRuntime;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(targets = "me.almana.logisticsnetworks.logic.ItemLane", remap = false)
public abstract class ItemLaneMixin {

    @Inject(method = "placements", at = @At("HEAD"), remap = false)
    private static void atm10skymixins$selectIngredientSide(
            @Coerce Object handler,
            @Coerce Object ingredient,
            CallbackInfoReturnable<List<?>> cir) {
        ReceivingSideRuntime.setForIngredient(ingredient);
    }

    @Inject(method = "moveFromSlot", at = @At("RETURN"), remap = false)
    private void atm10skymixins$rememberMovedStackSide(
            @Coerce Object target,
            int ingredient,
            int slot,
            int limit,
            boolean[] mask,
            CallbackInfoReturnable<Integer> cir) {
        if (cir.getReturnValue() <= 0) return;
        try {
            List<?> pending = (List<?>) ReceivingSideRuntime.field(this, "pending");
            if (pending.isEmpty()) return;
            Object moved = pending.get(pending.size() - 1);
            Object stack = ReceivingSideRuntime.call(moved, "stack");
            ReceivingSideRuntime.rememberPending(stack);
        } catch (Exception error) {
            throw new IllegalStateException("Could not remember an item recipe's receiving side", error);
        }
    }

    @Redirect(method = "insert", at = @At(value = "INVOKE",
            target = "Lme/almana/logisticsnetworks/logic/TransferEngine;insertItemWithAllowedSlots(Lnet/neoforged/neoforge/items/IItemHandler;Lnet/neoforged/neoforge/items/IItemHandler;Lnet/minecraft/world/item/ItemStack;Z[Z)Lnet/minecraft/world/item/ItemStack;"),
            remap = false)
    private ItemStack atm10skymixins$unwrapSideSpecificItemTargets(
            IItemHandler handler,
            @Coerce Object bulkHandler,
            ItemStack stack,
            boolean simulate,
            boolean[] allowedSlots) {
        try {
            Object selectedHandler = ReceivingSideRuntime.delegateForCurrent(handler);
            Object selectedBulkHandler = ReceivingSideRuntime.delegateForCurrent(bulkHandler);
            return (ItemStack) ReceivingSideRuntime.callStatic(
                    "me.almana.logisticsnetworks.logic.TransferEngine", "insertItemWithAllowedSlots",
                    selectedHandler, selectedBulkHandler, stack, simulate, allowedSlots);
        } catch (Exception error) {
            throw new IllegalStateException("Could not route a recipe item insertion through its receiving side", error);
        }
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
    private void atm10skymixins$rollbackFromTheOriginalReceivingSides(int target, CallbackInfo ci) {
        try {
            @SuppressWarnings("unchecked")
            List<Object> pending = (List<Object>) ReceivingSideRuntime.field(this, "pending");
            @SuppressWarnings("unchecked")
            List<Object> endpoints = (List<Object>) ReceivingSideRuntime.field(this, "endpoints");
            IItemHandler source = (IItemHandler) ReceivingSideRuntime.field(this, "source");
            Object transferTarget = ReceivingSideRuntime.call(endpoints.get(target), "items");
            IItemHandler receiver = (IItemHandler) ReceivingSideRuntime.call(transferTarget, "handler");

            for (Object moved : List.copyOf(pending)) {
                ItemStack stack = (ItemStack) ReceivingSideRuntime.call(moved, "stack");
                int sourceSlot = (int) ReceivingSideRuntime.call(moved, "sourceSlot");
                ReceivingSideRuntime.setForPending(stack);
                int left = stack.getCount();
                for (int slot = 0; slot < receiver.getSlots() && left > 0; slot++) {
                    if (!ItemStack.isSameItemSameComponents(receiver.getStackInSlot(slot), stack)) continue;
                    ItemStack back = receiver.extractItem(slot, left, false);
                    left -= back.getCount();
                    ItemStack stuck = back.isEmpty() ? back
                            : (ItemStack) ReceivingSideRuntime.call(this, "giveBack", sourceSlot, back);
                    if (!stuck.isEmpty()) stuck = receiver.insertItem(slot, stuck, false);
                    if (!stuck.isEmpty()) {
                        stuck = (ItemStack) ReceivingSideRuntime.call(this, "insert", transferTarget,
                                stuck, false, null);
                    }
                }
            }
            ReceivingSideRuntime.clearPending(this);
            pending.clear();
            ci.cancel();
        } catch (Throwable error) {
            throw new IllegalStateException("Could not roll back side-specific item transfers", error);
        }
    }

    @Inject(method = "commit", at = @At("HEAD"), remap = false)
    private void atm10skymixins$clearReceivingSideState(int target, @Coerce Object listener, CallbackInfo ci) {
        ReceivingSideRuntime.clearPending(this);
    }
}
