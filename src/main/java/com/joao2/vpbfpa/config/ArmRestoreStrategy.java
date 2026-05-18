package com.joao2.vpbfpa.config;

import java.util.Locale;

public enum ArmRestoreStrategy {
    SOURCE_POSE_TO_SELECTED_PARTS("source_pose_to_selected_parts"),
    COPY_SLEEVES_FROM_FINAL_ARMS("copy_sleeves_from_final_arms"),
    RESTORE_BEFORE_HELD_ITEM_TOO("restore_before_held_item_too"),
    RESTORE_ARMS_THEN_SYNC_SLEEVES_LATE("restore_arms_then_sync_sleeves_late"),
    RESTORE_AT_ARM_PART_RENDER("restore_at_arm_part_render"),
    RESTORE_BASE_ARMS_AT_PART_RENDER_ONLY("restore_base_arms_at_part_render_only"),
    COPY_FULL_TRANSFORM_TO_ARMS_AND_SLEEVES("copy_full_transform_to_arms_and_sleeves"),
    COPY_SLEEVE_TRANSFORM_TO_BASE_ARM("copy_sleeve_transform_to_base_arm"),
    COPY_EMF_SLEEVE_TRANSFORM_TO_EMF_ARM("copy_emf_sleeve_transform_to_emf_arm"),
    HIDE_BASE_ARMS_KEEP_SLEEVES("hide_base_arms_keep_sleeves"),
    HIDE_SLEEVES_KEEP_BASE_ARMS("hide_sleeves_keep_base_arms"),
    SOURCE_POSE_SLEEVES_ONLY_HIDE_BASE_ARMS("source_pose_sleeves_only_hide_base_arms"),
    SOURCE_POSE_SLEEVES_AND_CUSTOM_ONLY("source_pose_sleeves_and_custom_only"),
    SOURCE_POSE_SLEEVES_HIDE_EMF_CUSTOM_ARMS("source_pose_sleeves_hide_emf_custom_arms"),
    PARENT_ARM_SOURCE_CUSTOM_ARM_NEUTRAL("parent_arm_source_custom_arm_neutral"),
    PARENT_ARM_SOURCE_CUSTOM_ARM_DELTA("parent_arm_source_custom_arm_delta"),
    SOURCE_POSE_SLEEVES_CUSTOM_ARM_PARENT_DELTA("source_pose_sleeves_custom_arm_parent_delta"),
    SOURCE_POSE_SLEEVES_CUSTOM_ARM_PARENT_DELTA_PITCH_ONLY("source_pose_sleeves_custom_arm_parent_delta_pitch_only"),
    SOURCE_POSE_SLEEVES_CUSTOM_ARM_PARENT_DELTA_NO_ROLL("source_pose_sleeves_custom_arm_parent_delta_no_roll"),
    LOCK_PARENT_ARMS_AFTER_EMF_ANIMATE("lock_parent_arms_after_emf_animate"),
    LOCK_PARENT_ARMS_AND_HELD_ITEM("lock_parent_arms_and_held_item"),
    OBSERVE_FULL_TRANSFORM_ONLY("observe_full_transform_only"),
    OBSERVE_ONLY("observe_only");

    private final String id;

    ArmRestoreStrategy(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static ArmRestoreStrategy fromConfig(String value) {
        if (value == null) {
            return SOURCE_POSE_TO_SELECTED_PARTS;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (ArmRestoreStrategy strategy : values()) {
            if (strategy.id.equals(normalized)) {
                return strategy;
            }
        }

        return SOURCE_POSE_TO_SELECTED_PARTS;
    }
}
