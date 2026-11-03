package com.anbubu.atm10skymixins.mixins.logisticsnetworks.recipeReceivingSides;

import com.anbubu.atm10skymixins.compat.logisticsnetworks.IngredientSideAccess;
import com.anbubu.atm10skymixins.compat.logisticsnetworks.ReceivingSide;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(targets = "me.almana.logisticsnetworks.client.screen.RecipeCardCells", remap = false)
public abstract class RecipeCardCellsMixin {

    @ModifyConstant(method = "cellX", constant = @Constant(intValue = 88), remap = false)
    private static int atm10skymixins$widenGridCell(int original) {
        return 96;
    }

    @ModifyConstant(method = "cell", constant = @Constant(intValue = 52), remap = false)
    private static int atm10skymixins$moveTagButton(int original) {
        return 50;
    }

    @ModifyConstant(method = "cell", constant = @Constant(intValue = 70), remap = false)
    private static int atm10skymixins$moveSlotsButton(int original) {
        return 66;
    }

    @Inject(method = "cell", at = @At("RETURN"), remap = false)
    private static void atm10skymixins$drawReceivingSideButton(GuiGraphics graphics, Font font, int x, int y,
            @Coerce Object ingredient, boolean editing, CallbackInfo ci) {
        if (ingredient == null || font == null) return;
        String side = ingredient instanceof IngredientSideAccess access
                ? access.atm10skymixins$getReceivingSide() : null;
        int border = 0xFF444444;
        int fill = side == null ? 0xFF2E2E2E : 0xFF2E5E2E;
        String markString = side == null ? "=" : ReceivingSide.parse(side).mark();
        graphics.fill(x + 82, y + 1, x + 96, y + 15, border);
        graphics.fill(x + 83, y + 2, x + 95, y + 14, fill);
        graphics.drawCenteredString(font, markString == null ? "=" : markString,
                      x + 89, y + 4, 0xFFE0E0E0);
    }

    @Inject(method = "tooltip", at = @At("RETURN"), cancellable = true, remap = false)
    private static void atm10skymixins$includeReceivingSideInCellTooltip(@Coerce Object ingredient,
            CallbackInfoReturnable<List<Component>> cir) {
        if (ingredient == null) return;
        String side = ingredient instanceof IngredientSideAccess access
                ? access.atm10skymixins$getReceivingSide() : null;
        MutableComponent value = side == null
                ? Component.translatable("tooltip.atm10skymixins.recipe_card.side_default")
                : Component.translatable("tooltip.atm10skymixins.recipe_card.side",
                        Component.translatable("gui.logisticsnetworks.direction." + side));
        List<Component> lines = new ArrayList<>(cir.getReturnValue());
        lines.add(value.withStyle(ChatFormatting.GRAY));
        cir.setReturnValue(lines);
    }
}
