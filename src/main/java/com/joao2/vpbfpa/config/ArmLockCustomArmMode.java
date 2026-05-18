package com.joao2.vpbfpa.config;

import java.util.Locale;

public enum ArmLockCustomArmMode {
    NEUTRAL("neutral"),
    SOURCE("source"),
    HIDDEN("hidden");

    private final String id;

    ArmLockCustomArmMode(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static ArmLockCustomArmMode fromConfig(String value) {
        if (value == null) {
            return NEUTRAL;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (ArmLockCustomArmMode mode : values()) {
            if (mode.id.equals(normalized)) {
                return mode;
            }
        }

        return NEUTRAL;
    }
}
