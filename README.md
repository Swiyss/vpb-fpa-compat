# VPB Fresh Player Animations Compat

Client-side Fabric compatibility mod for the GoodCraft test profile. Milestone 1 is intentionally limited to safe detection and debug tooling for Vic's Point Blank guns, including Blue Archive Point Blank content-pack guns.

## Target

- Minecraft `1.21.11`
- Fabric Loader `0.19.2`
- Fabric API `0.141.4+1.21.11`
- Fabric Loom Remap `1.16.2`
- Gradle wrapper `9.4.0`
- Java 21

## Milestone 1 Scope

- Client-only mod load.
- Config kill switch.
- VPB gun detection through reflected `com.vicmatskiv.pointblank.item.GunItem`.
- Conservative `pointblank` namespace fallback only when reflection is unavailable.
- Debug logging with interval throttling.
- Optional HUD debug overlay.

Milestone 1 does not alter player model poses, first-person rendering, gameplay, networking, recipes, sounds, projectiles, or damage.

## Config

The mod reads `config/vpb-fpa-compat.json` if present. It does not auto-create the file. CamelCase is the preferred JSON format; matching snake_case aliases are accepted for compatibility with earlier docs.

Supported fields:

```json
{
  "enabled": true,
  "debugLogging": true,
  "debugOverlay": true,
  "detectionMode": "auto",
  "armMode": "off",
  "includeOffhand": false,
  "allowBodyFreshAnimation": true,
  "logIntervalTicks": 40,
  "overlayX": 8,
  "overlayY": 8,
  "overlayScale": 1.0,
  "overlayBackground": true,
  "poseTracing": false,
  "poseTraceLogging": true,
  "poseTraceIntervalTicks": 40,
  "poseTraceLocalPlayerOnly": true,
  "poseTraceVpbOnly": true,
  "poseTraceIncludeInactive": false,
  "armRestoreLocalPlayerOnly": true,
  "armRestoreDebugCompare": false,
  "armRestoreLayerMode": "arms_and_sleeves",
  "armRestoreStrategy": "source_pose_to_selected_parts",
  "armRestoreSourceMode": "vanilla_arm_source",
  "dumpModelParts": false,
  "dumpModelPartsMaxDepth": 6,
  "dumpModelPartsMaxParts": 128,
  "visualProbePartNameFilter": "",
  "armLockFreezeWhileHoldingGun": false,
  "armLockRightPitchOffset": 0.0,
  "armLockRightYawOffset": 0.0,
  "armLockRightRollOffset": 0.0,
  "armLockLeftPitchOffset": 0.0,
  "armLockLeftYawOffset": 0.0,
  "armLockLeftRollOffset": 0.0,
  "armLockRightSleevePitchOffset": 0.0,
  "armLockRightSleeveYawOffset": 0.0,
  "armLockRightSleeveRollOffset": 0.0,
  "armLockLeftSleevePitchOffset": 0.0,
  "armLockLeftSleeveYawOffset": 0.0,
  "armLockLeftSleeveRollOffset": 0.0,
  "armLockCustomArmMode": "neutral",
  "armLockSleeveSyncMode": "source_pose"
}
```

For Milestone 1, `armMode` is always effectively no-op.

Equivalent snake_case keys such as `debug_logging`, `debug_overlay`, `detection_mode`, `arm_mode`, `include_offhand`, `allow_body_fresh_animation`, `log_interval_ticks`, `overlay_x`, `overlay_y`, `overlay_scale`, `overlay_background`, `pose_tracing`, `pose_trace_logging`, `pose_trace_interval_ticks`, `pose_trace_local_player_only`, `pose_trace_vpb_only`, `pose_trace_include_inactive`, `arm_restore_local_player_only`, `arm_restore_debug_compare`, `arm_restore_layer_mode`, `arm_restore_strategy`, `arm_restore_source_mode`, `dump_model_parts`, `dump_model_parts_max_depth`, `dump_model_parts_max_parts`, `visual_probe_part_name_filter`, `arm_lock_freeze_while_holding_gun`, `arm_lock_right_pitch_offset`, `arm_lock_right_yaw_offset`, `arm_lock_right_roll_offset`, `arm_lock_left_pitch_offset`, `arm_lock_left_yaw_offset`, `arm_lock_left_roll_offset`, `arm_lock_right_sleeve_pitch_offset`, `arm_lock_right_sleeve_yaw_offset`, `arm_lock_right_sleeve_roll_offset`, `arm_lock_left_sleeve_pitch_offset`, `arm_lock_left_sleeve_yaw_offset`, `arm_lock_left_sleeve_roll_offset`, `arm_lock_custom_arm_mode`, and `arm_lock_sleeve_sync_mode` are also accepted.

`armLockSleeveSyncMode` supports `source_pose` and `locked_parent_pose`. `source_pose` is the existing/default behavior. `locked_parent_pose` is an opt-in diagnostic mode for `lock_parent_arms_after_emf_animate` and `lock_parent_arms_and_held_item`; it reapplies sleeves at the late parent-arm lock stage from the final locked parent pose, then applies sleeve-only offsets without moving the inner arms or held item.

The debug overlay is registered through Fabric's HUD element registry and draws a dark translucent background by default. It should remain visible while inactive so empty-hand testing is obvious.

## Build

```powershell
.\gradlew build
```

Do not copy the jar into GoodCraft until the build succeeds and jar contents are validated.

## GoodCraft Test Install

Manual copy after a successful build:

```powershell
$Profile = "C:\Users\joao2\AppData\Roaming\ModrinthApp\profiles\Good_Craft test version"
Copy-Item "build\libs\vpb-fpa-compat-0.1.0.jar" "$Profile\mods\"
```

Optional helper script:

```powershell
.\scripts\install-goodcraft-test-jar.ps1
```

The helper only moves previous `vpb-fpa-compat-*.jar` files from `mods` into `vpb-fpa-compat-backups\<timestamp>` under the GoodCraft profile root, then copies the latest built compatibility jar into `mods`. It does not touch GoodCraft configs, resource packs, content packs, or unrelated mods.

## Mixins

Milestone 2 adds read-only trace mixins. They snapshot/log third-person player model poses only when `poseTracing=true`; they do not alter arm poses or gameplay.

To collect trace logs in GoodCraft, create or edit `config/vpb-fpa-compat.json` locally with:

```json
{
  "poseTracing": true,
  "poseTraceLogging": true,
  "poseTraceIntervalTicks": 40,
  "poseTraceLocalPlayerOnly": true,
  "poseTraceVpbOnly": true,
  "poseTraceIncludeInactive": false
}
```

Experimental arm restore mode for Milestone 2.1:

```json
{
  "armMode": "restore_model_setup_arms_after_render_setup",
  "armRestoreLocalPlayerOnly": true
}
```

This mode restores only arms and sleeves for detected VPB guns. It does not alter body, head, legs, first-person rendering, or gameplay.

Visual proof mode for Milestone 2.2:

```json
{
  "armMode": "visual_probe_exaggerated_arms",
  "armRestoreLocalPlayerOnly": true,
  "poseTracing": true,
  "dumpModelParts": true,
  "visualProbePartNameFilter": ""
}
```

This intentionally exaggerates vanilla arms/sleeves for detected VPB guns. It is diagnostic only.

To probe every discovered arm-like EMF/Fresh candidate:

```json
{
  "armMode": "visual_probe_all_arm_like_parts",
  "armRestoreLocalPlayerOnly": true,
  "poseTracing": true,
  "dumpModelParts": true,
  "dumpModelPartsMaxDepth": 6,
  "dumpModelPartsMaxParts": 128,
  "visualProbePartNameFilter": ""
}
```

Model-part dumping is opt-in and bounded. The dumper uses object identity tracking, max depth, and max part count limits so EMF shared/global part references cannot recurse indefinitely.

Timing probes for Milestone 2.4:

```json
{
  "armMode": "visual_probe_after_set_angles_tail",
  "armRestoreLocalPlayerOnly": true,
  "poseTracing": true,
  "dumpModelParts": true,
  "dumpModelPartsMaxDepth": 6,
  "dumpModelPartsMaxParts": 128,
  "visualProbePartNameFilter": ""
}
```

```json
{
  "armMode": "visual_probe_at_actual_model_render",
  "armRestoreLocalPlayerOnly": true,
  "poseTracing": true,
  "dumpModelParts": true,
  "dumpModelPartsMaxDepth": 6,
  "dumpModelPartsMaxParts": 128,
  "visualProbePartNameFilter": ""
}
```

These modes are diagnostic only. They do not change gameplay or first-person rendering.

First real restore experiment for Milestone 2.5:

```json
{
  "armMode": "restore_vpb_arms_after_set_angles_tail",
  "armRestoreLocalPlayerOnly": true,
  "armRestoreDebugCompare": true,
  "poseTracing": true,
  "dumpModelParts": true,
  "dumpModelPartsMaxDepth": 6,
  "dumpModelPartsMaxParts": 128,
  "visualProbePartNameFilter": ""
}
```

This copies the post-`setAngles` VPB gun arm pose to vanilla and EMF arm/sleeve parts during third-person player rendering, then restores the original mutable model-part rotations after the render call returns.

Milestone 2.6 layer diagnostics keep the same restore mode and vary only `armRestoreLayerMode`:

```json
{
  "armMode": "restore_vpb_arms_after_set_angles_tail",
  "armRestoreLayerMode": "arms_only",
  "armRestoreLocalPlayerOnly": true,
  "armRestoreDebugCompare": true,
  "poseTracing": true,
  "dumpModelParts": true,
  "dumpModelPartsMaxDepth": 6,
  "dumpModelPartsMaxParts": 128,
  "visualProbePartNameFilter": ""
}
```

Useful comparison values are `arms_only`, `emf_custom_arms_only`, and `emf_custom_only`. The default remains `arms_and_sleeves`, which preserves the Milestone 2.5 behavior for comparison.

Milestone 2.7 synchronization diagnostics keep `armMode` and `armRestoreLayerMode` explicit, then vary `armRestoreStrategy`:

```json
{
  "armMode": "restore_vpb_arms_after_set_angles_tail",
  "armRestoreLayerMode": "arms_and_sleeves",
  "armRestoreStrategy": "observe_only",
  "armRestoreLocalPlayerOnly": true,
  "armRestoreDebugCompare": true,
  "poseTracing": true,
  "poseTraceLogging": true,
  "dumpModelParts": true,
  "dumpModelPartsMaxDepth": 6,
  "dumpModelPartsMaxParts": 128,
  "visualProbePartNameFilter": ""
}
```

Strategy values are `source_pose_to_selected_parts`, `copy_sleeves_from_final_arms`, `restore_before_held_item_too`, `restore_arms_then_sync_sleeves_late`, `restore_at_arm_part_render`, `restore_base_arms_at_part_render_only`, and `observe_only`. They are diagnostic and opt-in; `source_pose_to_selected_parts` preserves the current baseline behavior.

Milestone 2.8 part-render restore probe:

```json
{
  "armMode": "restore_vpb_arms_after_set_angles_tail",
  "armRestoreLayerMode": "arms_and_sleeves",
  "armRestoreStrategy": "restore_base_arms_at_part_render_only",
  "armRestoreLocalPlayerOnly": true,
  "armRestoreDebugCompare": true,
  "poseTracing": true,
  "poseTraceLogging": true,
  "dumpModelParts": true,
  "dumpModelPartsMaxDepth": 6,
  "dumpModelPartsMaxParts": 128,
  "visualProbePartNameFilter": ""
}
```

This keeps the current best baseline restore, then reapplies only base/non-custom arm parts immediately before those parts render. Compare with `restore_at_arm_part_render`, which reapplies the source pose before every selected arm/sleeve part render.

Milestone 2.9 full-transform diagnostics keep the same restore mode and use full part state instead of rotation-only state. Transform logs include rotation, pivot/origin, scale, visibility, hidden state, emptiness, cuboid count, child count, and EMF attachment metadata where reflection exposes it.

Priority test 1, hide base/inner arms and leave sleeves visible:

```json
{
  "armMode": "restore_vpb_arms_after_set_angles_tail",
  "armRestoreLayerMode": "arms_and_sleeves",
  "armRestoreStrategy": "hide_base_arms_keep_sleeves",
  "armRestoreSourceMode": "vanilla_arm_source",
  "armRestoreLocalPlayerOnly": true,
  "armRestoreDebugCompare": true,
  "poseTracing": true,
  "poseTraceLogging": true,
  "dumpModelParts": true,
  "dumpModelPartsMaxDepth": 6,
  "dumpModelPartsMaxParts": 128,
  "visualProbePartNameFilter": ""
}
```

Priority test 2, hide sleeves/outer layer and leave base arms visible:

```json
{
  "armMode": "restore_vpb_arms_after_set_angles_tail",
  "armRestoreLayerMode": "arms_and_sleeves",
  "armRestoreStrategy": "hide_sleeves_keep_base_arms",
  "armRestoreSourceMode": "vanilla_arm_source",
  "armRestoreLocalPlayerOnly": true,
  "armRestoreDebugCompare": true,
  "poseTracing": true,
  "poseTraceLogging": true,
  "dumpModelParts": true,
  "dumpModelPartsMaxDepth": 6,
  "dumpModelPartsMaxParts": 128,
  "visualProbePartNameFilter": ""
}
```

Priority test 3, treat sleeve/outer transform as authoritative and copy it to matching base arms:

```json
{
  "armMode": "restore_vpb_arms_after_set_angles_tail",
  "armRestoreLayerMode": "arms_and_sleeves",
  "armRestoreStrategy": "copy_sleeve_transform_to_base_arm",
  "armRestoreSourceMode": "vanilla_arm_source",
  "armRestoreLocalPlayerOnly": true,
  "armRestoreDebugCompare": true,
  "poseTracing": true,
  "poseTraceLogging": true,
  "dumpModelParts": true,
  "dumpModelPartsMaxDepth": 6,
  "dumpModelPartsMaxParts": 128,
  "visualProbePartNameFilter": ""
}
```

Additional 2.9 strategy values are `copy_full_transform_to_arms_and_sleeves`, `copy_emf_sleeve_transform_to_emf_arm`, and `observe_full_transform_only`. Source values are `vanilla_arm_source`, `vanilla_sleeve_source`, `emf_custom_arm_source`, and `emf_custom_sleeve_source`.

Milestone 2.10 sleeve-authority diagnostics start from the reproduced baseline where sleeves/outer arm layers look closest to the VPB pose and base/inner arms look wrong. These are still diagnostic and opt-in.

Test 1, explicitly restore sleeves to the VPB source pose and hide base/custom arm geometry:

```json
{
  "armMode": "restore_vpb_arms_after_set_angles_tail",
  "armRestoreLayerMode": "arms_and_sleeves",
  "armRestoreStrategy": "source_pose_sleeves_only_hide_base_arms",
  "armRestoreSourceMode": "vanilla_arm_source",
  "armRestoreLocalPlayerOnly": true,
  "armRestoreDebugCompare": true,
  "poseTracing": true,
  "poseTraceLogging": true,
  "dumpModelParts": true
}
```

Test 2, restore sleeves and hide only EMF custom arms:

```json
{
  "armMode": "restore_vpb_arms_after_set_angles_tail",
  "armRestoreLayerMode": "arms_and_sleeves",
  "armRestoreStrategy": "source_pose_sleeves_hide_emf_custom_arms",
  "armRestoreSourceMode": "vanilla_arm_source",
  "armRestoreLocalPlayerOnly": true,
  "armRestoreDebugCompare": true,
  "poseTracing": true,
  "poseTraceLogging": true,
  "dumpModelParts": true
}
```

Test 3, source the vanilla parent/proxy arm and neutralize EMF custom arm local rotation:

```json
{
  "armMode": "restore_vpb_arms_after_set_angles_tail",
  "armRestoreLayerMode": "arms_and_sleeves",
  "armRestoreStrategy": "parent_arm_source_custom_arm_neutral",
  "armRestoreSourceMode": "vanilla_arm_source",
  "armRestoreLocalPlayerOnly": true,
  "armRestoreDebugCompare": true,
  "poseTracing": true,
  "poseTraceLogging": true,
  "dumpModelParts": true
}
```

Other 2.10 values are `source_pose_sleeves_and_custom_only` and `parent_arm_source_custom_arm_delta`. The old `parent_arm_source_custom_arm_delta` value is intentionally conservative and logs skipped.

Milestone 2.11 EMF custom inner-arm delta diagnostics keep sleeves restored to the VPB source pose, then compute EMF custom arm local rotation at `emf_custom_part_render_head` from `source - current mutated vanilla parent/anchor`.

Full delta test:

```json
{
  "armMode": "restore_vpb_arms_after_set_angles_tail",
  "armRestoreLayerMode": "arms_and_sleeves",
  "armRestoreStrategy": "source_pose_sleeves_custom_arm_parent_delta",
  "armRestoreSourceMode": "vanilla_arm_source",
  "armRestoreLocalPlayerOnly": true,
  "armRestoreDebugCompare": true,
  "poseTracing": true,
  "poseTraceLogging": true,
  "dumpModelParts": true
}
```

Pitch-only delta test:

```json
{
  "armMode": "restore_vpb_arms_after_set_angles_tail",
  "armRestoreLayerMode": "arms_and_sleeves",
  "armRestoreStrategy": "source_pose_sleeves_custom_arm_parent_delta_pitch_only",
  "armRestoreSourceMode": "vanilla_arm_source",
  "armRestoreLocalPlayerOnly": true,
  "armRestoreDebugCompare": true,
  "poseTracing": true,
  "poseTraceLogging": true,
  "dumpModelParts": true
}
```

No-roll delta test:

```json
{
  "armMode": "restore_vpb_arms_after_set_angles_tail",
  "armRestoreLayerMode": "arms_and_sleeves",
  "armRestoreStrategy": "source_pose_sleeves_custom_arm_parent_delta_no_roll",
  "armRestoreSourceMode": "vanilla_arm_source",
  "armRestoreLocalPlayerOnly": true,
  "armRestoreDebugCompare": true,
  "poseTracing": true,
  "poseTraceLogging": true,
  "dumpModelParts": true
}
```

Milestone 2.12 VPB arm-lock diagnostics stop adding Euler delta variants. They restore sleeves to the VPB source pose, then re-lock the vanilla/anchor parent arms after EMF has mutated them. EMF custom arms are neutralized under that locked parent. Held-item support here is diagnostic only and does not rewrite item transforms.

Parent-arm lock:

```json
{
  "armMode": "restore_vpb_arms_after_set_angles_tail",
  "armRestoreLayerMode": "arms_and_sleeves",
  "armRestoreStrategy": "lock_parent_arms_after_emf_animate",
  "armRestoreSourceMode": "vanilla_arm_source",
  "armRestoreDebugCompare": true,
  "poseTracing": true,
  "poseTraceLogging": true,
  "dumpModelParts": true,
  "armLockFreezeWhileHoldingGun": false
}
```

Parent-arm lock with held-item bridge diagnostics:

```json
{
  "armMode": "restore_vpb_arms_after_set_angles_tail",
  "armRestoreLayerMode": "arms_and_sleeves",
  "armRestoreStrategy": "lock_parent_arms_and_held_item",
  "armRestoreSourceMode": "vanilla_arm_source",
  "armRestoreDebugCompare": true,
  "poseTracing": true,
  "poseTraceLogging": true,
  "dumpModelParts": true,
  "armLockFreezeWhileHoldingGun": false
}
```

To check whether walking/running jiggle comes from the captured source pose, set `armLockFreezeWhileHoldingGun` to `true` for either lock strategy.

Milestone 2.13 lock calibration starts from the winning `lock_parent_arms_and_held_item` baseline. Offsets are in radians, default to zero, and apply only to `lock_parent_arms_after_emf_animate` and `lock_parent_arms_and_held_item`.

Test 1, current winning behavior made explicit:

```json
{
  "armMode": "restore_vpb_arms_after_set_angles_tail",
  "armRestoreLayerMode": "arms_and_sleeves",
  "armRestoreStrategy": "lock_parent_arms_and_held_item",
  "armRestoreSourceMode": "vanilla_arm_source",
  "armLockCustomArmMode": "neutral",
  "armLockFreezeWhileHoldingGun": false,
  "armLockRightPitchOffset": 0.0,
  "armLockRightYawOffset": 0.0,
  "armLockRightRollOffset": 0.0,
  "armLockLeftPitchOffset": 0.0,
  "armLockLeftYawOffset": 0.0,
  "armLockLeftRollOffset": 0.0,
  "armRestoreDebugCompare": true,
  "poseTracing": true,
  "poseTraceLogging": true,
  "dumpModelParts": true
}
```

Test 2, custom inner arms use the calibrated source pose:

```json
{
  "armMode": "restore_vpb_arms_after_set_angles_tail",
  "armRestoreLayerMode": "arms_and_sleeves",
  "armRestoreStrategy": "lock_parent_arms_and_held_item",
  "armRestoreSourceMode": "vanilla_arm_source",
  "armLockCustomArmMode": "source",
  "armLockFreezeWhileHoldingGun": false,
  "armRestoreDebugCompare": true,
  "poseTracing": true,
  "poseTraceLogging": true,
  "dumpModelParts": true
}
```

Test 3, custom inner arms are hidden for the scoped render:

```json
{
  "armMode": "restore_vpb_arms_after_set_angles_tail",
  "armRestoreLayerMode": "arms_and_sleeves",
  "armRestoreStrategy": "lock_parent_arms_and_held_item",
  "armRestoreSourceMode": "vanilla_arm_source",
  "armLockCustomArmMode": "hidden",
  "armLockFreezeWhileHoldingGun": false,
  "armRestoreDebugCompare": true,
  "poseTracing": true,
  "poseTraceLogging": true,
  "dumpModelParts": true
}
```

Milestone 2.14 adds sleeve-only lock calibration. These offsets are also radians and apply only to vanilla/EMF sleeve parts in the two lock strategies. They do not affect parent/inner arms or the held-item bridge.

First sleeve-only pitch test:

```json
{
  "armMode": "restore_vpb_arms_after_set_angles_tail",
  "armRestoreLayerMode": "arms_and_sleeves",
  "armRestoreStrategy": "lock_parent_arms_and_held_item",
  "armRestoreSourceMode": "vanilla_arm_source",
  "armLockCustomArmMode": "neutral",
  "armLockFreezeWhileHoldingGun": false,
  "armLockRightPitchOffset": 0.0,
  "armLockRightYawOffset": 0.0,
  "armLockRightRollOffset": 0.0,
  "armLockLeftPitchOffset": 0.0,
  "armLockLeftYawOffset": 0.0,
  "armLockLeftRollOffset": 0.0,
  "armLockRightSleevePitchOffset": 0.25,
  "armLockRightSleeveYawOffset": 0.0,
  "armLockRightSleeveRollOffset": 0.0,
  "armLockLeftSleevePitchOffset": 0.25,
  "armLockLeftSleeveYawOffset": 0.0,
  "armLockLeftSleeveRollOffset": 0.0,
  "armRestoreDebugCompare": true,
  "poseTracing": true,
  "poseTraceLogging": true,
  "dumpModelParts": true
}
```

## Manual Milestone 2.16 Aim-only Simplified Test

For the simplified behavior where normal stance passes through and only aiming locks the arm/gun pose, use:

```json
{
  "armRestoreStrategy": "lock_parent_arms_and_held_item",
  "armLockCustomArmMode": "neutral",
  "armLockAimOnly": true,
  "armLockHideSleeves": true,
  "armLockFreezeWhileHoldingGun": false,
  "armLockSleeveSyncMode": "source_pose"
}
```

Keep global offsets and sleeve offsets at `0.0` for this test.
