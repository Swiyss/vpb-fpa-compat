# Test Matrix

## Validation Rules

- Build before deployment.
- Validate generated jar metadata before deployment.
- Copy the test jar into GoodCraft only after build success.
- Do not edit GoodCraft configs, mod jars, resource packs, content packs, or zip files.
- Do not redistribute VPB, Fresh, EMF, ETF, or Blue Archive assets.

GoodCraft profile:

```text
<path-to-local-goodcraft-test-profile>
```

## Build And Jar Checks

Run from repo root:

```powershell
.\gradlew build
jar tf "build\libs\<built-jar>.jar" | Select-String -Pattern "fabric.mod.json|vpb_fpa_compat|com/joao2/vpbfpa"
```

Expected:

- Build succeeds.
- Jar contains Fabric metadata.
- Milestone 1 jar contains no mixin metadata.
- Jar does not contain copied VPB/Fresh/EMF/ETF/Blue Archive assets.
- Metadata declares client-only behavior.

## Deployment Checks

Only after successful build:

```powershell
$Profile = "<path-to-local-goodcraft-test-profile>"
Get-ChildItem "$Profile\mods" -Filter "vpb-fpa-compat-*.jar"
Copy-Item "build\libs\<built-jar>.jar" "$Profile\mods\"
```

If replacing a previous test jar, move or remove only prior `vpb-fpa-compat-*.jar` files, and document the exact filenames changed.

## Milestone 1 Tests

| Case | Expected Result |
| --- | --- |
| Game launch | PASS: no crash; mod logs client initialization. |
| Empty hand | PASS: overlay visible and inactive. |
| Base VPB gun in main hand | PASS in logs for `pointblank:aughbar` and `pointblank:m134minigun` via `GunItem` class detection. |
| Blue Archive VPB gun in main hand | PASS in logs for `pointblank:ba_whitefang465` via `GunItem` class detection. |
| Random/non-gun item | PASS: inactive. |
| Non-gun Point Blank item | Not detected as active gun unless it is a real `GunItem`. |
| Offhand gun with `includeOffhand = false` | No offhand activation. |
| Offhand gun with `includeOffhand = true` | Offhand activation is reported. |
| Debug overlay enabled | PASS: overlay visible after Milestone 1.2.1 text fix. |
| Debug overlay disabled | No overlay text, logs still follow config. |
| Config `enabled = false` | All compat behavior disabled. |
| First person | PASS: no visible behavior change reported. |
| Third person/F5 | PASS for no crash/regression; arm compatibility remains unimplemented and unchanged. |

## Milestone 2 Trace Tests

| Case | Expected Result |
| --- | --- |
| `poseTracing = false` | No pose trace logs. |
| `poseTracing = true`, empty hand, default trace filters | No trace logs because `poseTraceVpbOnly = true`. |
| `poseTracing = true`, `poseTraceIncludeInactive = true`, empty hand | Trace logs include inactive state for comparison. |
| `poseTracing = true`, base VPB gun idle | Trace logs include implemented player-model/final-render stages. |
| `poseTracing = true`, Blue Archive gun idle | Trace logs include `pointblank:ba_*` item id. |
| Aiming/firing/reload while tracing | Logs remain throttled and no gameplay or first-person behavior changes. |
| Optional EMF absent in a later test profile | Game still starts; EMF trace mixin is skipped. |
| Optional NEA absent in a later test profile | Game still starts; NEA trace mixin is skipped. |

## Milestone 2.1 Experimental Restore Tests

| Case | Expected Result |
| --- | --- |
| Default `armMode = off` | No arm restore behavior. |
| `armMode = restore_model_setup_arms_after_render_setup`, empty hand | No snapshot/restore; overlay reports inactive/no snapshot. |
| Experimental mode, random item | No snapshot/restore. |
| Experimental mode, base VPB gun | Captures arm pose at `vanilla_or_model_setup_tail` and restores only arms/sleeves before model render. |
| Experimental mode, Blue Archive gun | Same restore path with `pointblank:ba_*` detection. |
| Experimental mode, first person | No first-person behavior change. |
| Experimental mode, walk/sprint/crouch | Body/head/legs remain Fresh-controlled where possible. |
| Experimental mode, logs | Throttled `[VPB-FPA ArmFix] captured/restored` lines appear for detected VPB guns. |
| EMF/NEA hook clarity | If candidate stages still do not appear, throttled `hook=<stage> reached` lines show whether hooks are reached and whether trace context exists. |

## Milestone 2.2 Visual Probe Tests

| Case | Expected Result |
| --- | --- |
| Default `armMode = off` | No arm changes. |
| `armMode = visual_probe_exaggerated_arms`, empty hand | No visual probe applied. |
| Visual probe, random item | No visual probe applied. |
| Visual probe, base VPB gun | Vanilla arms/sleeves are set to an exaggerated pose; if visible, vanilla arms affect F5 rendering. |
| Visual probe, Blue Archive gun | Same proof test with `pointblank:ba_*` detection. |
| Visual probe, first person | No first-person behavior change. |
| `dumpModelParts = true` with tracing | One low-spam model/part structure dump appears in logs. |

## Milestone 2.3 EMF/Fresh Part Discovery Tests

| Case | Expected Result |
| --- | --- |
| Default `armMode = off` | No arm changes. |
| `armMode = visual_probe_all_arm_like_parts`, empty hand | No probe applied. |
| All-arm-like probe, base VPB gun | Applies exaggerated pose to vanilla arms/sleeves and discovered arm-like EMF/Fresh candidates. |
| All-arm-like probe, Blue Archive gun | Same with `pointblank:ba_*` detection. |
| `dumpModelParts = true` | Logs EMF part names/classes and highlights arm candidates. |
| `visualProbePartNameFilter = right` | Only candidates with `right` in name/path are probed. |
| First person | No first-person behavior change. |

## Milestone 2.3.1 Dumper Safety Tests

| Case | Expected Result |
| --- | --- |
| `dumpModelParts = false` | No model-part dumping; no traversal risk. |
| `dumpModelParts = true` in F5 | No crash; traversal is bounded by identity tracking, max depth, and max part count. |
| EMF shared/cyclic part references | Traversal stops and logs `traversal truncated reason=cycle`. |
| Large EMF part registry | Traversal stops and logs `traversal truncated reason=maxParts` after the configured limit. |
| Deep custom child chain | Traversal stops and logs `traversal truncated reason=maxDepth`. |
| `visual_probe_all_arm_like_parts` with no safe candidates | Probe is skipped and logs `visual_probe skipped reason=no_safe_candidates`. |

## Milestone 2.4 Render Timing Tests

| Case | Expected Result |
| --- | --- |
| Default `armMode = off` | No arm changes. |
| `visual_probe_after_set_angles_tail`, empty hand | No probe applied. |
| `visual_probe_after_set_angles_tail`, VPB gun | Probe applies immediately after confirmed `PlayerEntityModel#setAngles` tail. |
| `visual_probe_at_actual_model_render`, VPB gun | Probe applies at confirmed model render/submission timing if a safe hook exists. |
| Held item trace, VPB gun | Logs held-item feature stage without changing item rendering. |
| First person | No first-person behavior change. |
| Trace logs | New timing stages are throttled and grep-friendly. |
| `actual_model_render_head` with VPB gun | Trace occurs after deferred `Model#setAngles` and immediately before first visible `Model#render`. |
| `visual_probe_at_actual_model_render` visible | Confirms timing was the blocking issue for model arm parts. |
| `visual_probe_at_actual_model_render` not visible | Suggests held-item transform or a different renderer/feature path may be responsible. |

## Milestone 2.5 First Restore Tests

| Case | Expected Result |
| --- | --- |
| Default `armMode = off` | No arm changes. |
| Visual probe, F5, then first-person item switch | No persistent first-person hand/item break after cleanup. |
| `restore_vpb_arms_after_set_angles_tail`, empty hand | No restore applied; status/log shows inactive or off. |
| `restore_vpb_arms_after_set_angles_tail`, base VPB gun | Copies post-`setAngles` gun arm pose to vanilla and EMF arm/sleeve candidates. |
| `restore_vpb_arms_after_set_angles_tail`, Blue Archive gun | Same with `pointblank:ba_*` detection. |
| Restore source not gun-like | Skip rather than forcing bad values. |
| First person with VPB gun | No arm mutation is applied. |
| After actual model render | Original touched rotations are restored and cleanup is logged. |
| Body/head/legs | Remain Fresh-controlled. |

## Milestone 2.6 Layer Alignment Tests

| Case | Expected Result |
| --- | --- |
| Default `armMode = off` | No arm changes. |
| `restore_vpb_arms_after_set_angles_tail` with `armRestoreLayerMode = arms_and_sleeves` | Preserves Milestone 2.5 behavior for comparison. |
| `armRestoreLayerMode = arms_only` | Restores arm candidates only; sleeve/outer layer should either inherit correctly or reveal it needs direct handling. |
| `armRestoreLayerMode = emf_custom_arms_only` | Restores only EMF custom arm candidates, testing whether visible Fresh arms are the sole required target. |
| `armRestoreLayerMode = emf_custom_only` | Restores EMF custom arms and sleeves only, testing whether vanilla/proxy parts should be left alone. |
| `armRestoreLayerMode = vanilla_only` | Restores vanilla/proxy arm and sleeve fields only. |
| `armRestoreLayerMode = sleeves_only` | Diagnostic only; should reveal whether sleeve parts are independently visible or relative children. |
| `armRestoreDebugCompare = true` | Logs touched parts and render-tail `layer_compare` summaries at throttled intervals. |
| F5 to first person to empty/random item | First-person hand/item remains normal; cleanup still prevents state leaks. |
| Body/head/legs | Remain Fresh-controlled for every layer mode. |

## Milestone 2.7 Synchronization Tests

| Case | Expected Result |
| --- | --- |
| Default `armMode = off` | No arm changes. |
| `armRestoreStrategy = observe_only` | No mutation; sync logs show vanilla, EMF custom, sleeve, and held-item state at throttled intervals. |
| `source_pose_to_selected_parts` | Preserves the 2.6 baseline for comparison. |
| `copy_sleeves_from_final_arms` | Arms use the VPB source pose; sleeves copy from the final effective arm rotations instead of direct source pose. |
| `restore_arms_then_sync_sleeves_late` | Arms restore first, then sleeve parts sync at latest confirmed pre-render point. |
| `restore_before_held_item_too` | Reapplies the restored pose before held item rendering only as an opt-in diagnostic, without first-person item changes. |
| Held item sync trace | Logs whether held item feature rendering observes restored or non-restored arm state. |
| EMF attachment trace | Logs whether EMF sleeve candidates expose attachment/parent metadata. |
| F5 to first person to empty/random item | First-person hand/item remains normal; scoped cleanup is preserved. |
| Body/head/legs | Remain Fresh-controlled for every strategy. |

## Milestone 2.8 Part Render Drift Tests

| Case | Expected Result |
| --- | --- |
| Default `armMode = off` | No arm changes. |
| `observe_only` with part tracing | Logs relevant arm/sleeve part render head/tail without mutation. |
| `restore_at_arm_part_render` | Reapplies VPB source pose immediately before relevant vanilla/EMF arm and sleeve part rendering. |
| `restore_base_arms_at_part_render_only` | Reapplies only base/non-custom arm parts at part-render time while preserving the current baseline for sleeves/custom parts. |
| Part trace for right arm | Shows whether right arm changes before/after its actual render call. |
| Part trace for right sleeve | Shows whether sleeve remains restored or inherits from a changed parent. |
| Held item sync | Remains diagnostic; no first-person or item-render rewrite. |
| F5 to first person to empty/random item | First-person hand/item remains normal; scoped cleanup is preserved. |
| Body/head/legs | Remain Fresh-controlled for every strategy. |

## Milestone 2.8.1 Boot Safety Tests

| Case | Expected Result |
| --- | --- |
| GoodCraft launch with compat jar | No `ReEntrantTransformerError`; Minecraft reaches main menu. |
| World load | No boot-time mixin crash; overlay can appear normally. |
| EMF optional mixin selection | Uses only Fabric mod-id checks, not `Class.forName(...)`. |
| EMF absent future check | Optional EMF mixins are skipped by mod id and should not crash. |
| Part-render mixins enabled | Launch remains stable; if not, disable 2.8 part-render mixins before further testing. |

## Milestone 2.8.2 NEA Descriptor Tests

| Case | Expected Result |
| --- | --- |
| GoodCraft launch | Main menu loads with no `ReEntrantTransformerError`. |
| World load | Overlay appears. |
| NEA trace mixin apply | No `InvalidInjectionException` from `NeaPlayerTransformerTraceMixin`. |
| NEA absent future check | Optional NEA mixin is skipped by mod id and should not crash. |

## Milestone 2.9 Full Transform Tests

| Case | Expected Result |
| --- | --- |
| Default `armMode = off` | No arm changes. |
| `observe_full_transform_only` | Logs full transform comparisons without mutation. |
| `hide_base_arms_keep_sleeves` | Hides inner/base arms only for detected VPB guns to reveal whether sleeve/custom layer is visually authoritative. |
| `hide_sleeves_keep_base_arms` | Hides sleeve/outer parts only for detected VPB guns to reveal whether base arms are visually authoritative. |
| `copy_sleeve_transform_to_base_arm` | Copies full sleeve transform to the matching base arm for testing. |
| `copy_emf_sleeve_transform_to_emf_arm` | Copies full EMF sleeve transform to the matching EMF arm for testing. |
| `copy_full_transform_to_arms_and_sleeves` | Copies full source transform to selected arm/sleeve candidates. |
| `armRestoreSourceMode = vanilla_sleeve_source` | Uses vanilla sleeve transforms as the full-transform source for copy diagnostics. |
| `armRestoreSourceMode = emf_custom_sleeve_source` | Uses EMF custom sleeve transforms as the full-transform source if available. |
| Transform trace | Logs rotation, pivot/origin, scale, visible/hidden, empty/cuboid count, child count, class, and attachment metadata. |
| F5 to first person to empty/random item | First-person hand/item remains normal; transform and visibility cleanup is preserved. |
| Body/head/legs | Remain Fresh-controlled for every strategy. |

## Milestone 2.10 Sleeve Authority Tests

| Case | Expected Result |
| --- | --- |
| `source_pose_to_selected_parts` baseline | Reproduces the old 13:35 behavior: sleeves/outer layer look closest to correct while base/inner arms are wrong. |
| `source_pose_sleeves_only_hide_base_arms` | Applies VPB source pose to sleeves and hides base/custom arm geometry to test whether sleeve-only authority is visually usable. |
| `source_pose_sleeves_and_custom_only` | Applies VPB source pose to vanilla sleeves, EMF custom sleeves, and EMF custom arms, without writing source pose to vanilla/base arms. |
| `source_pose_sleeves_hide_emf_custom_arms` | Applies VPB source pose to sleeves and hides only EMF custom arm geometry. |
| `parent_arm_source_custom_arm_neutral` | Sets vanilla/base arm parent/proxy to source pose, sets EMF custom arm local rotation neutral, and keeps sleeves in source pose. |
| `parent_arm_source_custom_arm_delta` | Diagnostic delta strategy; may log skipped if conservative correction data is insufficient. |
| Authority trace | Logs base arm, EMF custom arm, sleeve visibility and rotations plus source rotation. |
| F5 to first person to empty/random item | First-person hand/item remains normal; transform and visibility cleanup is preserved. |

## Milestone 2.11 Custom Arm Delta Tests

| Case | Expected Result |
| --- | --- |
| `source_pose_sleeves_custom_arm_parent_delta` | Sleeves stay restored to source pose; EMF custom arms receive full local delta at custom part render head. |
| `source_pose_sleeves_custom_arm_parent_delta_pitch_only` | Same, but only pitch delta is applied to identify whether yaw/roll cause twist. |
| `source_pose_sleeves_custom_arm_parent_delta_no_roll` | Same, but roll delta is suppressed. |
| Delta trace | Logs source, current parent/anchor, computed delta, custom arm before, and custom arm after. |
| Held item | Remains unchanged; held-item alignment is observed but not fixed in this milestone. |
| F5 to first person to empty/random item | First-person hand/item remains normal; transform and visibility cleanup is preserved. |

## Milestone 2.12 Arm Lock Tests

| Case | Expected Result |
| --- | --- |
| `lock_parent_arms_after_emf_animate` | Sleeves remain in VPB source pose; vanilla/anchor arms are re-locked after EMF mutation; EMF custom arms are neutral under the locked parent. |
| `lock_parent_arms_and_held_item` | Same lock plus held-item source-pose context bridge diagnostics. |
| `armLockFreezeWhileHoldingGun = true` | First captured pose for the same held gun is reused to check whether walking/running jiggle comes from source pose changes. |
| Lock trace | Logs source and applied pose at lock stages. |
| Held item sync | Logs whether a source pose bridge is available; no item transform rewrite yet. |
| F5 to first person to empty/random item | First-person hand/item remains normal; transform and visibility cleanup is preserved. |

## Milestone 2.13 Lock Calibration Tests

| Case | Expected Result |
| --- | --- |
| Default `armMode = off` | No arm changes. |
| `lock_parent_arms_and_held_item` with zero offsets and `armLockCustomArmMode = neutral` | Preserves the current best baseline: gun aligned, no walking/running jiggle, Fresh body/legs animate, first-person cleanup remains fixed. |
| `armLockCustomArmMode = source` | Uses the calibrated source pose for EMF custom arm local rotation to test whether custom inner arms should follow the lock source. |
| `armLockCustomArmMode = hidden` | Hides EMF custom inner arms during the scoped render to test whether remaining visual error comes from custom arm geometry. |
| Non-zero lock offsets | Offsets apply only to `lock_parent_arms_after_emf_animate` and `lock_parent_arms_and_held_item`, and only in third-person VPB-gun render scope. |
| Held item bridge | Uses the calibrated source pose and still cleans up at held-item render tail. |
| Lock calibration trace | Logs raw source, calibrated source, offsets, custom-arm mode, and held-item bridge availability at throttled intervals. |
| F5 to first person to empty/random item | First-person hand/item remains normal; scoped cleanup is preserved. |

## Milestone 2.14 Sleeve-Only Calibration Tests

| Case | Expected Result |
| --- | --- |
| Default `armMode = off` | No arm changes. |
| `lock_parent_arms_and_held_item`, `armLockCustomArmMode = neutral`, global offsets zero | Preserves the winning baseline: inner arms and gun aligned, no jiggle, Fresh body/legs animate. |
| `armLockRightSleevePitchOffset = 0.25`, `armLockLeftSleevePitchOffset = 0.25` | Sleeves move in the tested useful direction while parent/inner arms and held gun stay on the non-sleeve calibrated source. |
| Non-zero sleeve yaw/roll offsets | Affect only vanilla sleeves and EMF custom sleeves in lock strategies. |
| Held item bridge | Uses global arm lock source, not sleeve-only source. |
| Lock calibration trace | Shows separate arm-calibrated and sleeve-calibrated sources/offsets. |
| F5 to first person to empty/random item | First-person hand/item remains normal; full-state cleanup is preserved. |

## Milestone 2 Base VPB Gun Tests

| Case | Expected Result |
| --- | --- |
| Third person front | Arms hold gun without broken Fresh pose. |
| Third person back | Arms and sleeves remain aligned. |
| Idle | VPB arm stance preserved/restored. |
| Walk | Fresh body/legs animate; arms remain usable. |
| Sprint | No broken arm offsets. |
| Crouch | Fresh crouch/body style remains where possible. |
| Aim | Gun aiming does not break head/body alignment. |
| Fire | No pose snap causing invisible or offset arms. |
| Reload | Reload animation does not leave arms stuck. |
| Swap weapons | Active state updates promptly. |
| Holster if supported | No stale arm restore after holster. |
| Dual-wield if supported | Behavior matches configured hand detection. |

## Milestone 2 Blue Archive VPB Gun Tests

Repeat the base VPB gun tests with representative Blue Archive guns from the content pack, including at least one `pointblank:ba_*` rifle-like weapon and one non-rifle-like weapon if available.

## Conflict Checks

| Area | Expected Result |
| --- | --- |
| Fresh without gun | Fresh Player Animations still works normally. |
| Fresh body/legs with gun | Body and legs keep Fresh movement style. |
| Sleeves | Sleeves follow restored/suppressed arms. |
| Held item transform | Gun remains in hands. |
| Iris/Sodium/shaders | No launch or render crash. |
| Server join | No server-side mod requirement unless a future blocker proves unavoidable. |
| Logs | No repeated spam beyond configured interval. |

## Milestone 2.15 manual test

Use the winning baseline with `lock_parent_arms_and_held_item`, `armLockCustomArmMode=neutral`, `armLockFreezeWhileHoldingGun=false`, global arm offsets at `0.0`, sleeve pitch offsets at `0.25`, and `armLockSleeveSyncMode=locked_parent_pose`.

Expected result: inner arms and held gun keep the existing alignment/no-jiggle behavior, while sleeves derive from the same late locked parent pose before receiving sleeve-only offsets.

## Manual Milestone 2.16 Test

Config focus:

```json
{
  "armRestoreStrategy": "lock_parent_arms_and_held_item",
  "armLockCustomArmMode": "neutral",
  "armLockAimOnly": true,
  "armLockHideSleeves": true,
  "armLockFreezeWhileHoldingGun": false,
  "armLockSleeveSyncMode": "source_pose",
  "armLockRightPitchOffset": 0.0,
  "armLockLeftPitchOffset": 0.0,
  "armLockRightSleevePitchOffset": 0.0,
  "armLockLeftSleevePitchOffset": 0.0
}
```

Expected:

- Holding a VPB/Blue Archive gun without aiming should look like normal FPA + VPB behavior.
- Aiming should lock inner arms and held gun using the previous winning no-jiggle behavior.
- Sleeves should be hidden while the aim lock is active.
- Fresh body/legs should continue animating.
- First-person behavior should remain normal.
