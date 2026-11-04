package com.anbubu.atm10skymixins.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Atm10SkyMixinsConfig {

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue AE2_SMITHING_FIX;
    public static final ModConfigSpec.BooleanValue MEGACELLS_OPTIMISATION;
    public static final ModConfigSpec.BooleanValue TITANIUM_OPTIMISATION;
    public static final ModConfigSpec.BooleanValue RFTU_ENVIRONMENT_FIX;
    public static final ModConfigSpec.BooleanValue IF_SETTINGS_COPIER_FIX;
    public static final ModConfigSpec.BooleanValue TITANIUM_MULTI_INVENTORY_FIX;
    public static final ModConfigSpec.BooleanValue LOGISTICSNETWORKS_OPTIMISATION;

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

        RFTU_ENVIRONMENT_FIX = builder
                .comment("Fixes the Environment Controller from RFToolsUtility. Requires a client restart.")
                .gameRestart()
                .define("rftoolsutilityEnvironmentalFix", true);

        IF_SETTINGS_COPIER_FIX = builder
                .comment("Fixes Industrial Foregoing's Machine Settings Copier not applying side modes to the machine's item/fluid capabilities. Requires a client restart.")
                .gameRestart()
                .define("industrialforegoingSettingsCopierFix", true);

        TITANIUM_MULTI_INVENTORY_FIX = builder
                .comment("Fixes Titanium's composite inventory handler backing inventory issue, solving automation errors regarding Industrial Foregoing machines. Requires a restart.")
                .gameRestart()
                .define("titaniumMultiInventoryFix", true);

        LOGISTICSNETWORKS_OPTIMISATION = builder
                .comment("Optimises LogisticsNetworks' TransferEngine class by early rejection. Requires a restart.")
                .gameRestart()
                .define("logisticsnetworksOptimisation", true);

        builder.pop();

        SPEC = builder.build();
    }
}