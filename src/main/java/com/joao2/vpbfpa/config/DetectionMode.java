package com.joao2.vpbfpa.config;

import java.util.Locale;

public enum DetectionMode {
    AUTO("auto"),
    CLASS("class"),
    REGISTRY("registry");

    private final String id;

    DetectionMode(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static DetectionMode fromConfig(String value) {
        if (value == null) {
            return AUTO;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (DetectionMode mode : values()) {
            if (mode.id.equals(normalized)) {
                return mode;
            }
        }

        return AUTO;
    }
}
