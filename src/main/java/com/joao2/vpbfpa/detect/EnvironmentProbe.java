package com.joao2.vpbfpa.detect;

import net.fabricmc.loader.api.FabricLoader;

public record EnvironmentProbe(
        boolean pointBlankModLoaded,
        boolean gunItemClassFound,
        boolean emfModLoaded,
        boolean etfModLoaded,
        boolean notEnoughAnimationsModLoaded,
        Class<?> gunItemClass
) {
    private static final String GUN_ITEM_CLASS = "com.vicmatskiv.pointblank.item.GunItem";

    public static EnvironmentProbe create() {
        FabricLoader loader = FabricLoader.getInstance();
        Class<?> gunItemClass = findClass(GUN_ITEM_CLASS);

        return new EnvironmentProbe(
                loader.isModLoaded("pointblank"),
                gunItemClass != null,
                loader.isModLoaded("entity_model_features"),
                loader.isModLoaded("entity_texture_features"),
                loader.isModLoaded("notenoughanimations"),
                gunItemClass
        );
    }

    public String pointBlankStatus() {
        return pointBlankModLoaded ? "detected" : "not detected";
    }

    public String freshStatus() {
        return "unknown";
    }

    public String emfStatus() {
        return emfModLoaded ? "detected" : "not detected";
    }

    public String etfStatus() {
        return etfModLoaded ? "detected" : "not detected";
    }

    private static Class<?> findClass(String className) {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            return Class.forName(className, false, classLoader);
        } catch (ClassNotFoundException | LinkageError ignored) {
            return null;
        }
    }
}
