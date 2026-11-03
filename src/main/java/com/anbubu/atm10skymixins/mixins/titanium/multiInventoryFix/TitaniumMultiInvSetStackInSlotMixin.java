package com.anbubu.atm10skymixins.mixins.titanium.multiInventoryFix;

import com.hrznstudio.titanium.component.inventory.InventoryComponent;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
    targets = "com.hrznstudio.titanium.component.inventory.MultiInventoryComponent$MultiInvCapabilityHandler",
    remap = false
)
public abstract class TitaniumMultiInvSetStackInSlotMixin {

    @Shadow(remap = false)
    public abstract InventoryComponent<?> getFromSlot(int slot);

    @Shadow(remap = false)
    public abstract int getRelativeSlot(InventoryComponent<?> handler, int slot);

    @Inject(
        method = "setStackInSlot",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private void atm10skymixins$writeOnlyToMappedInventory(
        int slot,
        ItemStack stack,
        CallbackInfo ci
    ) {
        InventoryComponent<?> handler = getFromSlot(slot);

        if (handler == null) {
            return;
        }

        handler.setStackInSlot(
            getRelativeSlot(handler, slot),
            stack
        );

        ci.cancel();
    }
}