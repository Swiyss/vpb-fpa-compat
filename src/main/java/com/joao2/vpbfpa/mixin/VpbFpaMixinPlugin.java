package com.joao2.vpbfpa.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class VpbFpaMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger("vpb_fpa_compat/mixin");

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith(".compat.EmfBipedPoseTraceMixin")) {
            return optionalMod("entity_model_features", mixinClassName);
        }

        if (mixinClassName.endsWith(".compat.NeaPlayerTransformerTraceMixin")) {
            return optionalMod("notenoughanimations", mixinClassName);
        }

        if (mixinClassName.endsWith(".compat.EmfModelPartVanillaRenderTraceMixin")) {
            return optionalMod("entity_model_features", mixinClassName);
        }

        if (mixinClassName.endsWith(".compat.EmfModelPartCustomRenderTraceMixin")) {
            return optionalMod("entity_model_features", mixinClassName);
        }

        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    private static boolean optionalMod(String modId, String mixinClassName) {
        boolean apply = FabricLoader.getInstance().isModLoaded(modId);
        LOGGER.info("VPB-FPA optional trace mixin {} apply={}", mixinClassName, apply);
        return apply;
    }
}
