package com.joao2.vpbfpa.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.joao2.vpbfpa.VpbFpaCompatClient;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

public final class CompatConfig {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    public boolean enabled = true;

    @SerializedName(value = "debugLogging", alternate = {"debug_logging"})
    public boolean debugLogging = false;

    @SerializedName(value = "debugOverlay", alternate = {"debug_overlay"})
    public boolean debugOverlay = false;

    @SerializedName(value = "detectionMode", alternate = {"detection_mode"})
    public String detectionMode = DetectionMode.AUTO.id();

    @SerializedName(value = "armMode", alternate = {"arm_mode"})
    public String armMode = ArmMode.OFF.id();

    @SerializedName(value = "includeOffhand", alternate = {"include_offhand"})
    public boolean includeOffhand = false;

    @SerializedName(value = "allowBodyFreshAnimation", alternate = {"allow_body_fresh_animation"})
    public boolean allowBodyFreshAnimation = true;

    @SerializedName(value = "logIntervalTicks", alternate = {"log_interval_ticks"})
    public int logIntervalTicks = 40;

    @SerializedName(value = "overlayX", alternate = {"overlay_x"})
    public int overlayX = 8;

    @SerializedName(value = "overlayY", alternate = {"overlay_y"})
    public int overlayY = 8;

    @SerializedName(value = "overlayScale", alternate = {"overlay_scale"})
    public float overlayScale = 1.0F;

    @SerializedName(value = "overlayBackground", alternate = {"overlay_background"})
    public boolean overlayBackground = true;

    @SerializedName(value = "poseTracing", alternate = {"pose_tracing"})
    public boolean poseTracing = false;

    @SerializedName(value = "poseTraceLogging", alternate = {"pose_trace_logging"})
    public boolean poseTraceLogging = false;

    @SerializedName(value = "poseTraceIntervalTicks", alternate = {"pose_trace_interval_ticks"})
    public int poseTraceIntervalTicks = 40;

    @SerializedName(value = "poseTraceLocalPlayerOnly", alternate = {"pose_trace_local_player_only"})
    public boolean poseTraceLocalPlayerOnly = true;

    @SerializedName(value = "poseTraceVpbOnly", alternate = {"pose_trace_vpb_only"})
    public boolean poseTraceVpbOnly = true;

    @SerializedName(value = "poseTraceIncludeInactive", alternate = {"pose_trace_include_inactive"})
    public boolean poseTraceIncludeInactive = false;

    @SerializedName(value = "armRestoreLocalPlayerOnly", alternate = {"arm_restore_local_player_only"})
    public boolean armRestoreLocalPlayerOnly = true;

    @SerializedName(value = "armRestoreDebugCompare", alternate = {"arm_restore_debug_compare"})
    public boolean armRestoreDebugCompare = false;

    @SerializedName(value = "armRestoreLayerMode", alternate = {"arm_restore_layer_mode"})
    public String armRestoreLayerMode = ArmRestoreLayerMode.ARMS_AND_SLEEVES.id();

    @SerializedName(value = "armRestoreStrategy", alternate = {"arm_restore_strategy"})
    public String armRestoreStrategy = ArmRestoreStrategy.SOURCE_POSE_TO_SELECTED_PARTS.id();

    @SerializedName(value = "armRestoreSourceMode", alternate = {"arm_restore_source_mode"})
    public String armRestoreSourceMode = ArmRestoreSourceMode.VANILLA_ARM_SOURCE.id();

    @SerializedName(value = "dumpModelParts", alternate = {"dump_model_parts"})
    public boolean dumpModelParts = false;

    @SerializedName(value = "dumpModelPartsMaxDepth", alternate = {"dump_model_parts_max_depth"})
    public int dumpModelPartsMaxDepth = 6;

    @SerializedName(value = "dumpModelPartsMaxParts", alternate = {"dump_model_parts_max_parts"})
    public int dumpModelPartsMaxParts = 128;

    @SerializedName(value = "visualProbePartNameFilter", alternate = {"visual_probe_part_name_filter"})
    public String visualProbePartNameFilter = "";

    @SerializedName(value = "armLockFreezeWhileHoldingGun", alternate = {"arm_lock_freeze_while_holding_gun"})
    public boolean armLockFreezeWhileHoldingGun = false;

    @SerializedName(value = "armLockAimOnly", alternate = {"arm_lock_aim_only"})
    public boolean armLockAimOnly = false;

    @SerializedName(value = "armLockHideSleeves", alternate = {"arm_lock_hide_sleeves"})
    public boolean armLockHideSleeves = false;

    @SerializedName(value = "armLockRightPitchOffset", alternate = {"arm_lock_right_pitch_offset"})
    public float armLockRightPitchOffset = 0.0F;

    @SerializedName(value = "armLockRightYawOffset", alternate = {"arm_lock_right_yaw_offset"})
    public float armLockRightYawOffset = 0.0F;

    @SerializedName(value = "armLockRightRollOffset", alternate = {"arm_lock_right_roll_offset"})
    public float armLockRightRollOffset = 0.0F;

    @SerializedName(value = "armLockLeftPitchOffset", alternate = {"arm_lock_left_pitch_offset"})
    public float armLockLeftPitchOffset = 0.0F;

    @SerializedName(value = "armLockLeftYawOffset", alternate = {"arm_lock_left_yaw_offset"})
    public float armLockLeftYawOffset = 0.0F;

    @SerializedName(value = "armLockLeftRollOffset", alternate = {"arm_lock_left_roll_offset"})
    public float armLockLeftRollOffset = 0.0F;

    @SerializedName(value = "armLockRightSleevePitchOffset", alternate = {"arm_lock_right_sleeve_pitch_offset"})
    public float armLockRightSleevePitchOffset = 0.0F;

    @SerializedName(value = "armLockRightSleeveYawOffset", alternate = {"arm_lock_right_sleeve_yaw_offset"})
    public float armLockRightSleeveYawOffset = 0.0F;

    @SerializedName(value = "armLockRightSleeveRollOffset", alternate = {"arm_lock_right_sleeve_roll_offset"})
    public float armLockRightSleeveRollOffset = 0.0F;

    @SerializedName(value = "armLockLeftSleevePitchOffset", alternate = {"arm_lock_left_sleeve_pitch_offset"})
    public float armLockLeftSleevePitchOffset = 0.0F;

    @SerializedName(value = "armLockLeftSleeveYawOffset", alternate = {"arm_lock_left_sleeve_yaw_offset"})
    public float armLockLeftSleeveYawOffset = 0.0F;

    @SerializedName(value = "armLockLeftSleeveRollOffset", alternate = {"arm_lock_left_sleeve_roll_offset"})
    public float armLockLeftSleeveRollOffset = 0.0F;

    @SerializedName(value = "armLockCustomArmMode", alternate = {"arm_lock_custom_arm_mode"})
    public String armLockCustomArmMode = ArmLockCustomArmMode.NEUTRAL.id();

    @SerializedName(value = "armLockSleeveSyncMode", alternate = {"arm_lock_sleeve_sync_mode"})
    public String armLockSleeveSyncMode = ArmLockSleeveSyncMode.SOURCE_POSE.id();

    public static CompatConfig load(Path path) {
        if (!Files.exists(path)) {
            VpbFpaCompatClient.LOGGER.info("No config found at {}; using in-memory defaults and not writing a file.", path);
            return new CompatConfig();
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            CompatConfig config = GSON.fromJson(reader, CompatConfig.class);
            if (config == null) {
                VpbFpaCompatClient.LOGGER.warn("Config at {} was empty; using defaults.", path);
                return new CompatConfig();
            }
            return config;
        } catch (IOException | RuntimeException exception) {
            VpbFpaCompatClient.LOGGER.warn("Could not read config at {}; using defaults.", path, exception);
            return new CompatConfig();
        }
    }

    public DetectionMode detectionMode() {
        return DetectionMode.fromConfig(detectionMode);
    }

    public ArmMode armMode() {
        return ArmMode.fromConfig(armMode);
    }

    public ArmRestoreLayerMode armRestoreLayerMode() {
        return ArmRestoreLayerMode.fromConfig(armRestoreLayerMode);
    }

    public ArmRestoreStrategy armRestoreStrategy() {
        return ArmRestoreStrategy.fromConfig(armRestoreStrategy);
    }

    public ArmRestoreSourceMode armRestoreSourceMode() {
        return ArmRestoreSourceMode.fromConfig(armRestoreSourceMode);
    }

    public ArmLockCustomArmMode armLockCustomArmMode() {
        return ArmLockCustomArmMode.fromConfig(armLockCustomArmMode);
    }

    public ArmLockSleeveSyncMode armLockSleeveSyncMode() {
        return ArmLockSleeveSyncMode.fromConfig(armLockSleeveSyncMode);
    }

    public int normalizedLogIntervalTicks() {
        return Math.max(1, logIntervalTicks);
    }

    public int normalizedOverlayX() {
        return Math.max(0, overlayX);
    }

    public int normalizedOverlayY() {
        return Math.max(0, overlayY);
    }

    public float normalizedOverlayScale() {
        if (!Float.isFinite(overlayScale)) {
            return 1.0F;
        }
        return Math.max(0.5F, Math.min(4.0F, overlayScale));
    }

    public int normalizedPoseTraceIntervalTicks() {
        return Math.max(1, poseTraceIntervalTicks);
    }

    public int normalizedDumpModelPartsMaxDepth() {
        return Math.max(0, Math.min(16, dumpModelPartsMaxDepth));
    }

    public int normalizedDumpModelPartsMaxParts() {
        return Math.max(1, Math.min(512, dumpModelPartsMaxParts));
    }
}


