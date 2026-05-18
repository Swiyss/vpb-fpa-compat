package com.joao2.vpbfpa.config;

import java.util.Locale;

public enum ArmRestoreLayerMode {
    ARMS_AND_SLEEVES("arms_and_sleeves"),
    ARMS_ONLY("arms_only"),
    SLEEVES_ONLY("sleeves_only"),
    VANILLA_ONLY("vanilla_only"),
    EMF_CUSTOM_ONLY("emf_custom_only"),
    EMF_CUSTOM_ARMS_ONLY("emf_custom_arms_only"),
    EMF_CUSTOM_SLEEVES_ONLY("emf_custom_sleeves_only");

    private final String id;

    ArmRestoreLayerMode(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static ArmRestoreLayerMode fromConfig(String value) {
        if (value == null) {
            return ARMS_AND_SLEEVES;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (ArmRestoreLayerMode mode : values()) {
            if (mode.id.equals(normalized)) {
                return mode;
            }
        }

        return ARMS_AND_SLEEVES;
    }
}
