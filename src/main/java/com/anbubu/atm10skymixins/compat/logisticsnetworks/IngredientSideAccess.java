package com.anbubu.atm10skymixins.compat.logisticsnetworks;

/** Mixin-added side metadata on LogisticsNetworks recipe ingredients. */
public interface IngredientSideAccess {
    String atm10skymixins$getReceivingSide();
    void atm10skymixins$setReceivingSide(String side);
}
