package com.anbubu.atm10skymixins.mixins.logisticsnetworks.recipeReceivingSides;

import com.anbubu.atm10skymixins.compat.logisticsnetworks.IngredientSideAccess;
import com.anbubu.atm10skymixins.compat.logisticsnetworks.ReceivingSide;
import com.anbubu.atm10skymixins.compat.logisticsnetworks.ReceivingSideRuntime;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

@Mixin(targets = "me.almana.logisticsnetworks.client.screen.RecipeCardScreen", remap = false)
public abstract class RecipeCardScreenMixin {

    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 188), remap = false)
    private int atm10skymixins$widenRecipeCardScreen(int original) {
        return 208;
    }

    @ModifyConstant(method = "clickCard", constant = @Constant(intValue = 52), remap = false)
    private int atm10skymixins$moveTagButton(int original) {
        return 50;
    }

    @ModifyConstant(method = "clickCard", constant = @Constant(intValue = 70), remap = false)
    private int atm10skymixins$moveSlotsButton(int original) {
        return 66;
    }

    @ModifyConstant(method = "renderCardTooltip", constant = @Constant(intValue = 52), remap = false)
    private int atm10skymixins$moveTagTooltip(int original) {
        return 50;
    }

    @ModifyConstant(method = "renderCardTooltip", constant = @Constant(intValue = 70), remap = false)
    private int atm10skymixins$moveSlotsTooltip(int original) {
        return 66;
    }

    @ModifyConstant(method = "renderBg", constant = @Constant(intValue = 13), remap = false)
    private int atm10skymixins$shiftInventoryLabel(int original) {
        return 23;
    }

    @Inject(method = "clickCard", at = @At("HEAD"), cancellable = true, remap = false)
    private void atm10skymixins$editIngredientReceivingSide(int x, int y, int action,
            CallbackInfoReturnable<Boolean> cir) {
        try {
            Object menu = ReceivingSideRuntime.field(this, "menu");
            Object data = ReceivingSideRuntime.call(menu, "data");
            @SuppressWarnings("unchecked")
            List<Object> ingredients = (List<Object>) ReceivingSideRuntime.call(data, "ingredients");
            int index = atm10skymixins$sideButtonAt(x, y, ingredients.size());
            if (index < 0) return;

            Object ingredient = ingredients.get(index);
            String currentKey = ReceivingSideRuntime.sideOf(ingredient);
            ReceivingSide next = action == 1 ? null : ReceivingSide.next(ReceivingSide.parse(currentKey));
            Object copy = ReceivingSideRuntime.call(ingredient, "withCount",
                    ReceivingSideRuntime.call(ingredient, "count"));
            ((IngredientSideAccess) copy).atm10skymixins$setReceivingSide(next == null ? null : next.key());
            Object updated = ReceivingSideRuntime.call(data, "withIngredient", index, copy);
            UnaryOperator<Object> replacement = ignored -> updated;
            ReceivingSideRuntime.call(menu, "edit", replacement);
            try {
                ReceivingSideRuntime.callStatic("me.almana.logisticsnetworks.client.screen.ClickSound", "play");
            } catch (Throwable ignored) {
                // Sound is cosmetic; editing the card is the functional part.
            }
            cir.setReturnValue(true);
        } catch (Throwable error) {
            throw new IllegalStateException("Could not edit the LogisticsNetworks recipe receiving side", error);
        }
    }

    @Inject(method = "renderCardTooltip", at = @At("HEAD"), cancellable = true, remap = false)
    private void atm10skymixins$showReceivingSideTooltip(GuiGraphics graphics, int mouseX, int mouseY,
            CallbackInfo ci) {
        try {
            int left = (int) ReceivingSideRuntime.field(this, "leftPos");
            int top = (int) ReceivingSideRuntime.field(this, "topPos");
            int x = mouseX - left;
            int y = mouseY - top;
            Object menu = ReceivingSideRuntime.field(this, "menu");
            Object data = ReceivingSideRuntime.call(menu, "data");
            @SuppressWarnings("unchecked")
            List<Object> ingredients = (List<Object>) ReceivingSideRuntime.call(data, "ingredients");
            int index = atm10skymixins$sideButtonAt(x, y, ingredients.size());
            if (index < 0) return;

            String key = ReceivingSideRuntime.sideOf(ingredients.get(index));
            Component side = key == null
                    ? Component.translatable("tooltip.atm10skymixins.recipe_card.side_default")
                    : Component.translatable("gui.logisticsnetworks.direction." + key);
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("tooltip.atm10skymixins.recipe_card.side", side));
            lines.add(Component.translatable("tooltip.atm10skymixins.recipe_card.side_edit_hint"));
            Font font = (Font) ReceivingSideRuntime.field(this, "font");
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
            ci.cancel();
        } catch (Throwable error) {
            throw new IllegalStateException("Could not render the LogisticsNetworks receiving-side tooltip", error);
        }
    }

    @Unique
    private static int atm10skymixins$sideButtonAt(int x, int y, int ingredientCount) {
        for (int i = 0; i < ingredientCount; i++) {
            int cellX = 8 + (i % 2) * 96;
            int cellY = 40 + (i / 2) * 20;
            if (x >= cellX + 82 && x < cellX + 96 && y >= cellY + 2 && y < cellY + 16) return i;
        }
        return -1;
    }
}
