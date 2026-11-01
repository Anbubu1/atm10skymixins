package com.anbubu.atm10skymixins;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

import com.anbubu.atm10skymixins.config.Atm10SkyMixinsConfig;

@Mod(Atm10SkyMixins.MOD_ID)
public class Atm10SkyMixins {
    public static final String MOD_ID = "atm10skymixins";

    public Atm10SkyMixins(ModContainer container) {
        container.registerConfig(
            ModConfig.Type.CLIENT,
            Atm10SkyMixinsConfig.SPEC
        );
    }
}