# LogisticsNetworks recipe receiving sides

This integration adds the per-ingredient receiving-side feature from the supplied
`recipe-receiving-side.patch` to ATM10SkyMixins, without modifying LogisticsNetworks
itself.

## Compatibility target

The supplied LogisticsNetworks source is version `1.21.1-1.18.0`. The patch was
reversed cleanly to reconstruct that unpatched source before creating these mixins.
The integration targets that class layout and method signatures; a different
LogisticsNetworks release may need updated injection targets.

## Behaviour

- Each recipe ingredient can select Down, Up, North, South, West, East, All, None,
  or the node's configured default side.
- Left-click the `=`/side marker in a recipe card to cycle through the sides.
  Right-click resets the ingredient to the node default.
- The side is part of the ingredient codec and the LogisticsNetworks recipe text
  representation (`side <key>`), so it is retained through saving and networking.
- Item, fluid, and Mekanism chemical transfers use the selected receiving face.
  Rollback is routed to the exact handler that received each moved resource.
- Existing target channel-type checks remain in force.

## Configuration

The integration is enabled by default and can be disabled in
`config/atm10skymixins-client.toml`:

```toml
[mixins]
logisticsnetworksRecipeReceivingSides = false
```

A restart is required after changing it. The mixin config is only registered when
`logisticsnetworks` is installed.

## Verification status

The source was checked against the unpatched `1.21.1-1.18.0` source tree, including
all injected method names and the item/fluid/chemical lane signatures. The Gradle
build could not be run in this environment because the Gradle 9.2.1 distribution
was not cached and network access to `services.gradle.org` was unavailable.
Therefore, compilation and in-game behaviour remain unverified.
