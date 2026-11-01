package com.anbubu.atm10skymixins.mixins;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.objectweb.asm.tree.ClassNode;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public class Atm10SkyMixinsPlugin implements IMixinConfigPlugin {

    private boolean enabled = true;

    @Override
    public void onLoad(String mixinPackage) {
        String configKey = toConfigKey(mixinPackage);

        Path configPath = Path.of(
            "config",
            "atm10skymixins-client.toml"
        );

        try (CommentedFileConfig config = CommentedFileConfig.builder(configPath).sync().build()) {
            config.load();

            enabled = config.getOrElse(
                "mixins." + configKey,
                true
            );
        } catch (Exception e) {
            enabled = true;
        }
    }

    @Override
    public boolean shouldApplyMixin(
        String targetClassName,
        String mixinClassName
    ) {
        return enabled;
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(
        Set<String> myTargets,
        Set<String> otherTargets
    ) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(
        String targetClassName,
        ClassNode targetClass,
        String mixinClassName,
        IMixinInfo mixinInfo
    ) {}

    @Override
    public void postApply(
        String targetClassName,
        ClassNode targetClass,
        String mixinClassName,
        IMixinInfo mixinInfo
    ) {}

    private static String toConfigKey(String mixinPackage) {
        String suffix = mixinPackage.substring(
                mixinPackage.indexOf(".mixins.") + ".mixins.".length()
        );

        String[] parts = suffix.split("\\.");

        StringBuilder key = new StringBuilder(parts[0]);

        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];

            key.append(Character.toUpperCase(part.charAt(0)))
            .append(part.substring(1));
        }

        return key.toString();
    }
}