package com.joao2.vpbfpa.config;

import java.util.Locale;

public enum ArmLockSleeveSyncMode {
    SOURCE_POSE("source_pose"),
    LOCKED_PARENT_POSE("locked_parent_pose");

    private final String id;

    ArmLockSleeveSyncMode(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static ArmLockSleeveSyncMode fromConfig(String value) {
        if (value == null) {
            return SOURCE_POSE;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (ArmLockSleeveSyncMode mode : values()) {
            if (mode.id.equals(normalized)) {
                return mode;
            }
        }

        return SOURCE_POSE;
    }
}