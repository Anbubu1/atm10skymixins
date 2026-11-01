package com.anbubu.atm10skymixins.compat.ae2;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.item.crafting.SmithingTrimRecipe;
import net.minecraft.world.level.Level;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEItemKey;
import appeng.core.definitions.AEItems;
import appeng.menu.slot.RestrictedInputSlot;
import appeng.util.ConfigInventory;

public final class SmithingPatternBatchEncoder {

    private SmithingPatternBatchEncoder() {}

    public static boolean encodeIfCustom(
        Player player,
        ConfigInventory encodedInputs,
        RestrictedInputSlot blankPatternSlot,
        RestrictedInputSlot encodedPatternSlot
    ) {
        if (!(encodedInputs.getKey(0) instanceof AEItemKey template)
         || !(encodedInputs.getKey(1) instanceof AEItemKey selectedBase)
         || !(encodedInputs.getKey(2) instanceof AEItemKey addition)) {
            return false;
        }

        Level level = player.level();

        SmithingRecipeInput selectedInput = new SmithingRecipeInput(
            template.toStack(),
            selectedBase.toStack(),
            addition.toStack()
        );

        RecipeHolder<SmithingRecipe> recipeHolder =
            level.getRecipeManager()
                 .getRecipeFor(RecipeType.SMITHING, selectedInput, level)
                 .orElse(null);

        if (recipeHolder == null)
            return false;

        SmithingRecipe recipe = recipeHolder.value();

        if (recipe instanceof SmithingTransformRecipe
         || recipe instanceof SmithingTrimRecipe)
            return false;

        ItemStack existingOutput = encodedPatternSlot.getItem();

        boolean reusableOutput = !existingOutput.isEmpty()
                              && (AEItems.BLANK_PATTERN.is(existingOutput)
                              || PatternDetailsHelper.isEncodedPattern(existingOutput));

        if (!existingOutput.isEmpty() && !reusableOutput)
            return true;

        ItemStack blanks = blankPatternSlot.getItem();

        int availableBlanks = AEItems.BLANK_PATTERN.is(blanks)
                            ? blanks.getCount()
                            : 0;

        int maxPatterns = availableBlanks + (reusableOutput ? 1 : 0);

        if (maxPatterns <= 0)
            return true;

        List<ItemStack> patterns = createPatterns(
            recipeHolder,
            recipe,
            level,
            template,
            selectedBase,
            addition,
            maxPatterns
        );

        if (patterns.isEmpty())
            return true;

        int index = 0;

        if (!reusableOutput)
            if (!consumeBlank(blankPatternSlot))
                return true;

        encodedPatternSlot.set(patterns.get(index++));

        for (; index < patterns.size(); index++) {
            if (!consumeBlank(blankPatternSlot))
                break;

            ItemStack extra = patterns.get(index).copy();

            if (extra != null && !player.getInventory().add(extra))
                player.drop(extra, false);
        }

        return true;
    }

    private static List<ItemStack> createPatterns(
        RecipeHolder<SmithingRecipe> recipeHolder,
        SmithingRecipe recipe,
        @Nonnull Level level,
        AEItemKey template,
        AEItemKey selectedBase,
        AEItemKey addition,
        int limit
    ) {
        List<ItemStack> patterns = new ArrayList<>();

        Map<Item, AEItemKey> candidates = new LinkedHashMap<>();
        candidates.put(selectedBase.getItem(), selectedBase);

        for (Item item : BuiltInRegistries.ITEM) {
            AEItemKey key = AEItemKey.of(item.getDefaultInstance());

            if (key != null)
                candidates.putIfAbsent(item, key);
        }

        for (AEItemKey candidateBase : candidates.values()) {
            if (patterns.size() >= limit)
                break;

            SmithingRecipeInput input = new SmithingRecipeInput(
                template.toStack(),
                candidateBase.toStack(),
                addition.toStack()
            );

            if (!recipe.matches(input, level))
                continue;

            ItemStack output = recipe.assemble(
                input,
                level.registryAccess()
            );

            if (output.isEmpty())
                continue;

            AEItemKey outputKey = AEItemKey.of(output);

            if (outputKey == null)
                continue;

            patterns.add(
                PatternDetailsHelper.encodeSmithingTablePattern(
                    recipeHolder,
                    template,
                    candidateBase,
                    addition,
                    outputKey,
                    true
                )
            );
        }

        return patterns;
    }

    private static boolean consumeBlank(
        RestrictedInputSlot blankPatternSlot
    ) {
        ItemStack blank = blankPatternSlot.getItem();

        if (!AEItems.BLANK_PATTERN.is(blank) || blank.isEmpty())
            return false;

        ItemStack remaining = blank.copy();
        remaining.shrink(1);

        blankPatternSlot.set(remaining);

        return true;
    }
}