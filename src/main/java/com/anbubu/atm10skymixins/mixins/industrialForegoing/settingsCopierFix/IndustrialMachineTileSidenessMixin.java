package com.anbubu.atm10skymixins.mixins.industrialForegoing.settingsCopierFix;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.hrznstudio.titanium.block.tile.ActiveTile;
import com.hrznstudio.titanium.component.fluid.FluidTankComponent;
import com.hrznstudio.titanium.component.fluid.MultiTankComponent;
import com.hrznstudio.titanium.component.inventory.InventoryComponent;
import com.hrznstudio.titanium.component.inventory.MultiInventoryComponent;
import com.hrznstudio.titanium.component.sideness.ICapabilityHolder;
import com.hrznstudio.titanium.component.sideness.IFacingComponent;
import com.hrznstudio.titanium.util.FacingUtil;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
    targets = "com.buuz135.industrial.block.tile.IndustrialMachineTile",
    remap = false
)
public abstract class IndustrialMachineTileSidenessMixin {

    @Inject(method = "loadSettings", at = @At("TAIL"), remap = false)
    private void atm10skymixins$rebuildSidedCapabilities(
        Player player,
        CompoundTag tag,
        CallbackInfo ci
    ) {
        final ActiveTile<?> self = (ActiveTile<?>) (Object) this;

        final MultiInventoryComponent<?> inventories = self.getMultiInventoryComponent();
        if (inventories != null) {
            for (InventoryComponent<?> inventory : List.copyOf(inventories.getInventoryHandlers())) {
                if (inventory instanceof IFacingComponent facing)
                    atm10skymixins$reapply(inventories, facing);
            }
        }

        final MultiTankComponent<?> tanks = self.getMultiTankComponent();
        if (tanks != null) {
            for (FluidTankComponent<?> tank : List.copyOf(tanks.getTanks())) {
                if (tank instanceof IFacingComponent facing)
                    atm10skymixins$reapply(tanks, facing);
            }
        }

        self.invalidateCapabilities();
    }

    @Unique
    private static void atm10skymixins$reapply(
        ICapabilityHolder<?> holder,
        IFacingComponent facing
    ) {
        final Map<FacingUtil.Sideness, IFacingComponent.FaceMode> modes = facing.getFacingModes();
        final List<IFacingComponent.FaceMode> valid = Arrays.asList(facing.getValidFacingModes());

        for (FacingUtil.Sideness side : List.copyOf(modes.keySet())) {
            final int index = valid.indexOf(modes.get(side));
            if (index >= 0)
                holder.handleFacingChange(facing.getName(), side, index);
        }
    }
}