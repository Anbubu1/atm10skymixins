package com.anbubu.atm10skymixins.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Atm10SkyMixinsConfig {

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue AE2_SMITHING_FIX;
    public static final ModConfigSpec.BooleanValue MEGACELLS_OPTIMISATION;
    public static final ModConfigSpec.BooleanValue TITANIUM_OPTIMISATION;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("mixins");

        AE2_SMITHING_FIX = builder
                .comment("Fixes AE2 Smithing Patterns to support non-vanilla smithing recipes, such as Forbidden Arcanus's Eternal Stella. Requires a client restart.")
                .gameRestart()
                .define("ae2SmithingFix", false);

        MEGACELLS_OPTIMISATION = builder
                .comment("Optimises Mega Cells, reducing server TPS lag caused from decompressing items in mega storage cells. Requires a client restart.")
                .gameRestart()
                .define("megacellsOptimisation", true);

        TITANIUM_OPTIMISATION = builder
                .comment("Prevents unnecessary JEI search and bookmark list recalculations caused by Titanium's UI exclusion area updates. Requires a client restart.")
                .gameRestart()
                .define("titaniumOptimisation", true);

        builder.pop();

        SPEC = builder.build();
    }
}