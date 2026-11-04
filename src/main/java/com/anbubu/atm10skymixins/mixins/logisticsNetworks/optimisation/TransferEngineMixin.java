package com.anbubu.atm10skymixins.mixins.logisticsNetworks.optimisation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import me.almana.logisticsnetworks.data.FilterMode;
import me.almana.logisticsnetworks.filter.FilterItemData;
import me.almana.logisticsnetworks.integration.storage.DirectItemAccess;
import me.almana.logisticsnetworks.integration.storage.ItemResource;
import me.almana.logisticsnetworks.logic.FilterLogic;
import me.almana.logisticsnetworks.logic.TransferEngine;
import me.almana.logisticsnetworks.registration.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

@Mixin(TransferEngine.class)
public abstract class TransferEngineMixin {

    @WrapOperation(
        method = "executeMove",
        at = @At(
            value = "INVOKE",
            target = "Lnet/neoforged/neoforge/items/IItemHandler;extractItem(IIZ)Lnet/minecraft/world/item/ItemStack;",
            ordinal = 0
        )
    )
    private static ItemStack interceptSimulatedExtract(
        IItemHandler source,
        int slot,
        int amount,
        boolean simulate,
        Operation<ItemStack> original,

        @Local(argsOnly = true) ItemStack[] exportFilters,
        @Local(argsOnly = true) FilterMode exportFilterMode,
        @Local(argsOnly = true) HolderLookup.Provider provider,
        @Local(argsOnly = true) FilterItemData.ReadCache filterReadCache,
        @Local(argsOnly = true) ItemResource resource
    ) {
        if (provider == null || !(source instanceof DirectItemAccess)) return original.call(source, slot, amount, simulate);

        ItemStack peek = source.getStackInSlot(slot);
        if (peek.isEmpty()) return ItemStack.EMPTY;

        var tag = ModTags.RESOURCE_BLACKLIST_ITEMS;
        if (tag != null && peek.is(tag)) return ItemStack.EMPTY;
        if (resource != null && !resource.equals(new ItemResource(peek))) return ItemStack.EMPTY;
        if (!FilterLogic.matchesItemInSlot(
            exportFilters,
            exportFilterMode,
            peek,
            provider,
            null,
            filterReadCache,
            slot
        )) return ItemStack.EMPTY;

        return original.call(source, slot, amount, simulate);
    }
}