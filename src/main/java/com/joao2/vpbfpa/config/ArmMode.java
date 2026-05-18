package com.joao2.vpbfpa.config;

import java.util.Locale;

public enum ArmMode {
    OFF("off"),
    RESTORE_MODEL_SETUP_ARMS_AFTER_RENDER_SETUP("restore_model_setup_arms_after_render_setup"),
    VISUAL_PROBE_EXAGGERATED_ARMS("visual_probe_exaggerated_arms"),
    VISUAL_PROBE_ALL_ARM_LIKE_PARTS("visual_probe_all_arm_like_parts"),
    VISUAL_PROBE_AFTER_SET_ANGLES_TAIL("visual_probe_after_set_angles_tail"),
    VISUAL_PROBE_AT_ACTUAL_MODEL_RENDER("visual_probe_at_actual_model_render"),
    RESTORE_VPB_ARMS_AFTER_SET_ANGLES_TAIL("restore_vpb_arms_after_set_angles_tail"),
    RESTORE_VPB_ARMS_AFTER_FRESH("restore_vpb_arms_after_fresh"),
    SUPPRESS_FRESH_ARMS_WHEN_VPB_WEAPON("suppress_fresh_arms_when_vpb_weapon"),
    MANUAL_VPB_LIKE_ARM_POSE("manual_vpb_like_arm_pose");

    private final String id;

    ArmMode(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static ArmMode fromConfig(String value) {
        if (value == null) {
            return OFF;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (ArmMode mode : values()) {
            if (mode.id.equals(normalized)) {
                return mode;
            }
        }

        return OFF;
    }
}
