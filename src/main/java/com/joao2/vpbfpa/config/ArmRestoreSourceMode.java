package com.joao2.vpbfpa.config;

import java.util.Locale;

public enum ArmRestoreSourceMode {
    VANILLA_ARM_SOURCE("vanilla_arm_source"),
    EMF_CUSTOM_ARM_SOURCE("emf_custom_arm_source"),
    EMF_CUSTOM_SLEEVE_SOURCE("emf_custom_sleeve_source"),
    VANILLA_SLEEVE_SOURCE("vanilla_sleeve_source");

    private final String id;

    ArmRestoreSourceMode(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static ArmRestoreSourceMode fromConfig(String value) {
        if (value == null) {
            return VANILLA_ARM_SOURCE;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (ArmRestoreSourceMode sourceMode : values()) {
            if (sourceMode.id.equals(normalized)) {
                return sourceMode;
            }
        }

        return VANILLA_ARM_SOURCE;
    }
}
