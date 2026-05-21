# Implementation Plan

## Baseline

This plan is approved as the planning baseline. It includes the later corrections that make the first implementation milestone intentionally small.

Target:

- Fabric client-only compatibility mod.
- Minecraft `1.21.11`.
- Fabric Loader `0.19.2`.
- Fabric API `0.141.4+1.21.11`.
- Yarn `1.21.11+build.5`.
- Gradle wrapper `9.4.0`.
- Fabric Loom Remap `1.16.2`.
- Java 21.

These dependency versions are targets based on the GoodCraft profile and current Fabric tooling. During bootstrap, dependency resolution must verify them. If a declared Loom, Yarn, Fabric Loader, Fabric API, or Minecraft dependency cannot resolve, stop and report the exact issue instead of changing versions opportunistically. The Gradle wrapper may be updated only when the resolved plugin reports an exact Gradle API requirement.

Do not implement a NeoForge or multi-loader project unless future profile evidence contradicts the current findings.

## Milestone 0: Persist Repo Source Of Truth

Create and maintain:

- `docs/findings.md`
- `docs/compat-map.md`
- `docs/plan.md`
- `docs/test-matrix.md`
- Local workflow notes for modpack inspection, render mixins, QA validation, GoodCraft probing, animation tracing, and release checks.

## Milestone 1: Small Client Mod, Detection, Debug

Build the smallest useful Fabric client mod:

- Mod loads in GoodCraft.
- Config kill switch exists.
- Detects base Point Blank guns.
- Detects Blue Archive Point Blank guns through dynamic Point Blank gun detection.
- Logs active detection state.
- Shows optional debug overlay.
- Makes no gameplay changes.
- Makes no first-person rendering changes.
- Does not attempt an arm fix yet except through disabled/no-op `armMode`.
- Avoids render/animation mixins.
- Avoids EMF/Fresh/ETF/Not Enough Animations mixins.

Config shape:

- `enabled`
- `debugLogging`
- `debugOverlay`
- `detectionMode`
- `armMode`
- `includeOffhand`
- `allowBodyFreshAnimation`
- `logIntervalTicks`
- `overlayX`
- `overlayY`
- `overlayScale`
- `overlayBackground`

CamelCase is the preferred JSON format. Matching snake_case aliases are accepted for compatibility with earlier docs: `debug_logging`, `debug_overlay`, `detection_mode`, `arm_mode`, `include_offhand`, `allow_body_fresh_animation`, `log_interval_ticks`, `overlay_x`, `overlay_y`, `overlay_scale`, and `overlay_background`.

Default `armMode` should be `off` for the first detection build, or an explicit no-op mode if the config enum requires one.

Debug output should include:

- `VPB-FPA Compat: active`
- `weapon: <registry id>`
- `mode: <armMode>`
- `pointblank: detected/not detected`
- `GunItem: found/missing`
- `EMF: detected/not detected`
- `ETF: detected/not detected`
- `NEA: detected/not detected`
- `arms: off/restored/suppressed/fallback`

For Milestone 1.2, the overlay uses Fabric's HUD element registry, a top-left default position, and a dark translucent backing rectangle. It remains visible even when detection is inactive if `debugOverlay=true`.

## Milestone 2: Arm Fix Behind `armMode`

Milestone 2 begins with diagnostics only. Before any arm correction, add configurable read-only pose tracing to determine where arms change in the current GoodCraft render stack.

Trace config:

- `poseTracing`
- `poseTraceLogging`
- `poseTraceIntervalTicks`
- `poseTraceLocalPlayerOnly`
- `poseTraceVpbOnly`
- `poseTraceIncludeInactive`
- `armRestoreLocalPlayerOnly`

Snake_case aliases are also accepted for these fields.

Trace stages may include only confirmed hooks:

- `player_model_set_angles_head`
- `vanilla_or_model_setup_tail`
- `nea_candidate_before`
- `nea_candidate_after`
- `emf_candidate_before`
- `emf_candidate_after`
- `final_render_before`
- `final_render_after`

Tracing must be read-only, throttled, client-only, and guarded by config. Optional EMF/NEA trace mixins must be skipped safely when the optional mod or target class is absent.

After tracing identifies the render order, implement arm correction behind config.

Milestone 2.1 experimental arm mode:

- Add `restore_model_setup_arms_after_render_setup`.
- Default remains `off`.
- Prefer local-player restore only by default with `armRestoreLocalPlayerOnly=true`.
- Capture right/left arm rotations at `vanilla_or_model_setup_tail` when a VPB gun is active.
- Restore only right arm, left arm, right sleeve, and left sleeve at the latest confirmed pre-render point.
- Do not restore body, head, legs, root, item transforms, first-person rendering, gameplay, or networking.
- Keep debug logs throttled.

Milestone 2.2 diagnostic mode:

- Add `visual_probe_exaggerated_arms`.
- Default remains `off`.
- Only runs when compat is enabled, a VPB gun is detected, and the local-player/default scope permits it.
- Set vanilla right arm, left arm, right sleeve, and left sleeve to a deliberately obvious pose.
- Do not touch body, head, legs, root, first-person rendering, held-item logic, gameplay, or networking.
- Use this only to prove whether vanilla arm parts affect visible F5 rendering.
- Add optional model part dump config to inspect model class names and accessible child part names.
- Improve hook logs to use `[VPB-FPA Hook] hook=emf_applyTo_*` and `[VPB-FPA Hook] hook=nea_updateModel_*` when `poseTracing=true`.

Milestone 2.3 visible arm discovery:

- Inspect installed EMF classes before relying on field names.
- Improve `ModelPartDumper` to list EMF root part registry/tree entries using defensive reflection.
- Add `visual_probe_all_arm_like_parts`.
- Add `visualProbePartNameFilter`.
- Default remains `off`.
- Probe only detected VPB/Blue Archive guns and local player by default.
- Exaggerate vanilla arms/sleeves and discovered arm-like EMF/Fresh candidates.
- Do not touch body/head/legs unless the part name clearly indicates arm, sleeve, or hand.
- Add an optional `visualProbePartNameFilter` for targeted part probing.

Milestone 2.3.1 dumper crash fix:

- Record that the first 2.3 runtime test crashed on F5 with `StackOverflowError` in `ModelPartDumper.collectEmfParts`.
- Treat EMF parts as a graph with shared/global references, not as a simple tree.
- Add identity-based visited tracking to recursive walks.
- Add `dumpModelPartsMaxDepth` and `dumpModelPartsMaxParts`, defaulting to `6` and `128`.
- Avoid repeatedly calling root-global `getAllVanillaPartsEMF()` from nested parts.
- Cache reflection lookups where practical.
- Skip `visual_probe_all_arm_like_parts` if safe candidate discovery fails.
- Do not implement the final arm compatibility fix in this milestone.

Milestone 2.4 visible render timing:

- Record that the safe all-arm visual probe no longer crashes and mutates eight vanilla/EMF arm-like parts internally.
- Investigate actual Minecraft 1.21.11 render flow around `LivingEntityRenderer#render`, `PlayerEntityModel#setAngles`, `OrderedRenderCommandQueue#submitModel`, model render calls, player feature renderers, held item rendering, and EMF model part rendering.
- Add read-only trace stages only for confirmed hooks.
- Add temporary diagnostic visual-probe timing modes only when a hook is confirmed:
  - `visual_probe_after_set_angles_tail`
  - `visual_probe_at_actual_model_render`
- Keep default `armMode=off`.
- Keep all timing probes local-player/VPB-only by default and avoid first-person/gameplay changes.
- Add held-item feature trace logging if a safe confirmed hook exists.

Confirmed render-flow answer for Milestone 2.4:

- `LivingEntityRenderer#render` submits a model command before feature rendering.
- `BatchingRenderCommandQueue#submitModel` stores the mutable model and state; it does not immediately render or bake the current part rotations.
- `ModelCommandRenderer#render` later calls `Model#setAngles(state)` and then `Model#render(...)`.
- A confirmed pre-visible-render hook exists after `Model#setAngles(Object)` and before the first `Model#render(...)` in `ModelCommandRenderer`.
- The prior `before_submit_model` stage is now known to be a command-submission stage, not the final visible render stage.

Milestone 2.4 implementation scope:

- Add trace stages around `ModelCommandRenderer` command execution.
- Add `visual_probe_after_set_angles_tail`.
- Add `visual_probe_at_actual_model_render`.
- Add held-item feature trace logging around `HeldItemFeatureRenderer#renderItem`.
- Do not implement the final arm compatibility fix.

Milestone 2.5 first real restore mode:

- Record that both timing probes were visible in GoodCraft.
- Fix first-person/model state leaks before treating any arm mode as usable.
- Bracket every diagnostic and restore mutation:
  - save original rotations for every touched part
  - apply probe/restore
  - restore originals immediately after the actual `Model#render(...)` call
- Do not mutate when the effective camera perspective is first person.
- Add `restore_vpb_arms_after_set_angles_tail`, default off.
- In this mode, copy the post-`setAngles` vanilla gun arm pose to vanilla arms/sleeves and discovered EMF/Fresh arm candidates.
- Skip if the vanilla source pose does not look like a gun/forward arm pose for that frame.
- Keep body, head, legs, root, item transforms, first-person rendering, and gameplay untouched.

Milestone 2.6 arm/sleeve layer alignment:

- Record that the real restore mode partially fixed the visible F5 gun pose and fixed the first-person leak.
- Treat arm/sleeve separation as the current release blocker.
- Keep `armMode=off` as the default and keep `restore_vpb_arms_after_set_angles_tail` config-only.
- Add `armRestoreLayerMode`, default `arms_and_sleeves`, to isolate which visible layer family should receive the restored VPB arm pose.
- Supported diagnostic values: `arms_and_sleeves`, `arms_only`, `sleeves_only`, `vanilla_only`, `emf_custom_only`, `emf_custom_arms_only`, and `emf_custom_sleeves_only`.
- Log touched parts and a render-tail layer comparison when `armRestoreDebugCompare=true`.
- Preserve scoped cleanup after `Model#render(...)` and keep first-person mutation skipped.

Milestone 2.7 main model / sleeve / held item synchronization:

- Record that all 2.6 layer filters were partial or worse; `arms_and_sleeves` remains the best baseline but is not release-ready.
- Add compact sync tracing around final `setAngles`, model render head/tail, cleanup, and held item feature rendering.
- Include vanilla arm/sleeve rotations, EMF custom arm/sleeve rotations, held item arm pose, and configured layer/strategy in sync logs.
- Reflectively inspect EMF custom part attachment metadata so `EMF_right_sleeve` can be classified as attached/parented/independent where local fields expose that information.
- Add `armRestoreStrategy`, default `source_pose_to_selected_parts`, with temporary values `source_pose_to_selected_parts`, `copy_sleeves_from_final_arms`, `restore_before_held_item_too`, `restore_arms_then_sync_sleeves_late`, and `observe_only`.
- Keep all strategies opt-in, third-person/local-player gated, and cleaned up after actual model render.

Milestone 2.8 in-render arm drift and part-render restore point:

- Record that `restore_before_held_item_too` is only partial and still has no active held-item model context.
- Treat in-render base-arm drift as the current main blocker: model render head is synchronized, but render tail shows vanilla/base arms drifting while sleeves/custom parts remain restored.
- Inspect and target only confirmed render methods:
  - `Model#render(...)`
  - `ModelPart#render(...)`
  - `PlayerEntityModel#render` behavior through inherited `Model#render(...)`
  - EMF `EMFModelPartVanilla#method_22699(...)`
  - EMF `EMFModelPartCustom#method_22699(...)`
  - EMF `EMFModelPartWithState#method_22699(...)`
- Add part-render trace stages for relevant arm/sleeve parts only.
- Add opt-in strategies `restore_at_arm_part_render` and `restore_base_arms_at_part_render_only`.
- Keep first-person skipped, local-player/VPB gating, render-command cleanup, and no gameplay/item logic changes.

Milestone 2.8.1 boot-time mixin safety:

- Record that the first 2.8 runtime test crashed before launch with `ReEntrantTransformerError`.
- Fix boot safety before any further arm work.
- `VpbFpaMixinPlugin` must not call `Class.forName(...)`, reflect into target classes, access MinecraftClient, or otherwise load Minecraft/EMF/NEA/Point Blank classes in `shouldApplyMixin`.
- Optional EMF/NEA mixins may be gated by Fabric mod id only.
- If part-render mixins still crash after plugin safety is fixed, disable the new 2.8 part-render mixins and leave the project launchable.

Milestone 2.8.2 NEA descriptor cleanup:

- Record that 2.8.1 fixed the re-entrance crash and GoodCraft launches.
- Inspect the installed NEA jar before changing the mixin descriptor.
- Update `NeaPlayerTransformerTraceMixin` to match the installed `PlayerTransformer#updateModel(AbstractClientPlayerEntity, PlayerEntityModel, float, CallbackInfo)` signature.
- Keep optional mixin selection classloading-safe; do not reintroduce `Class.forName(...)`.
- If NEA descriptor support becomes unstable, disable the diagnostic NEA trace mixin and continue the EMF/model-part render investigation.

Milestone 2.9 full transform / inner arm vs sleeve authority:

- Record that part-render restore strategies synchronized tracked rotations but made visuals worse.
- Treat full transform authority as the next blocker: pivot/origin, scale, local basis, attachment metadata, and layer visibility matter.
- Extend transform traces beyond rotation for vanilla arms/sleeves and EMF custom arm/sleeve candidates.
- Add `armRestoreSourceMode`, default `vanilla_arm_source`, with source candidates `emf_custom_arm_source`, `emf_custom_sleeve_source`, and `vanilla_sleeve_source`.
- Add opt-in strategies `copy_full_transform_to_arms_and_sleeves`, `copy_sleeve_transform_to_base_arm`, `copy_emf_sleeve_transform_to_emf_arm`, `hide_base_arms_keep_sleeves`, `hide_sleeves_keep_base_arms`, and `observe_full_transform_only`.
- Prioritize runtime tests: hide base arms, hide sleeves, then copy sleeve transform to base arm.
- Preserve first-person skip and render cleanup for all transform/visibility changes.
- Cleanup must restore full mutable part state, not only rotation: rotation, pivot/origin, scale, visible, and hidden state.
- Full-transform logs should identify whether base arms and sleeve/custom parts use different pivots or coordinate bases before any final fix is chosen.

Milestone 2.10 sleeve-authority diagnostics:

- Record that `source_pose_to_selected_parts` reproduced the old 13:35 baseline on the current jar.
- Treat sleeves/outer layer as the current strongest authority candidate.
- Add opt-in strategies `source_pose_sleeves_only_hide_base_arms`, `source_pose_sleeves_and_custom_only`, `source_pose_sleeves_hide_emf_custom_arms`, `parent_arm_source_custom_arm_neutral`, and `parent_arm_source_custom_arm_delta`.
- `source_pose_sleeves_only_hide_base_arms` must explicitly apply the VPB source pose to vanilla/EMF sleeves and hide vanilla/custom arm geometry, unlike the older generic hide-base strategy.
- Keep `parent_arm_source_custom_arm_delta` diagnostic-only; if conservative delta math is too risky, stub/log as skipped.
- Add compact `[VPB-FPA AuthorityTrace]` logs for base/custom/sleeve visibility and rotations.
- Preserve first-person skip and render cleanup for all authority strategies.

Milestone 2.11 EMF custom inner-arm delta:

- Record that sleeve authority is confirmed and EMF custom arms are likely the visible inner arm geometry.
- Add opt-in strategies:
  - `source_pose_sleeves_custom_arm_parent_delta`
  - `source_pose_sleeves_custom_arm_parent_delta_pitch_only`
  - `source_pose_sleeves_custom_arm_parent_delta_no_roll`
- At set-angles tail, keep restoring vanilla/EMF sleeves to the VPB source pose.
- Do not directly force source pose onto EMF custom arms at set-angles tail.
- At EMF custom arm render head, read the current mutated vanilla/anchor parent arm and set custom local rotation to a conservative delta from source to parent.
- Keep cleanup full-state and scoped to third-person/local-player/VPB render context.
- Keep held-item alignment as a later milestone; do not rewrite held-item rendering here.

Milestone 2.12 VPB arm lock:

- Record that all Euler delta variants failed and stop expanding delta guesses.
- Add `lock_parent_arms_after_emf_animate`: restore sleeves to the VPB source pose, then at EMF vanilla arm render head reapply the VPB source pose to the vanilla/anchor arm after EMF has mutated it, while setting the corresponding EMF custom arm local rotation neutral.
- Add `lock_parent_arms_and_held_item`: include the parent-arm lock and expose a scoped source-pose bridge to held-item feature rendering for diagnostics.
- Add `armLockFreezeWhileHoldingGun`, default false, to optionally reuse the first stable pose for the same held gun until item/perspective changes.
- Add lock/held-item trace logs; do not alter first-person, body/head/legs, gameplay, or item transform logic.

Milestone 2.13 lock pose calibration:

- Record that `lock_parent_arms_and_held_item` is the best current baseline.
- Preserve that strategy's synchronization behavior: held-item bridge works, gun alignment is improved, and arms/sleeves/gun no longer jiggle while walking/running.
- Treat final arm/sleeve orientation as the remaining blocker.
- Add zero-default calibration offsets, in radians, applied only to lock strategies:
  - `armLockRightPitchOffset`
  - `armLockRightYawOffset`
  - `armLockRightRollOffset`
  - `armLockLeftPitchOffset`
  - `armLockLeftYawOffset`
  - `armLockLeftRollOffset`
- Add `armLockCustomArmMode`, default `neutral`, with diagnostic values:
  - `neutral`: current winning behavior; EMF custom arm local rotation is zeroed under the locked parent.
  - `source`: EMF custom arm local rotation receives the calibrated source pose.
  - `hidden`: EMF custom arm geometry is hidden for the scoped render.
- Use calibrated source for parent/anchor arms, sleeves, EMF sleeves, and held-item bridge.
- Keep all calibration third-person/local-player/VPB gated, opt-in through the existing lock strategies, and cleaned up after render.

Milestone 2.14 sleeve-only lock calibration:

- Record that `armLockCustomArmMode=neutral` is the winning custom-arm mode; `source` and `hidden` are worse.
- Record that global arm-lock pitch offsets move sleeves in the correct direction but also move inner arms and the held gun, which are already correct/aligned.
- Keep global lock offsets for parent/inner arms and held-item bridge, but stop using them for sleeve-only calibration.
- Add zero-default sleeve offsets, in radians:
  - `armLockRightSleevePitchOffset`
  - `armLockRightSleeveYawOffset`
  - `armLockRightSleeveRollOffset`
  - `armLockLeftSleevePitchOffset`
  - `armLockLeftSleeveYawOffset`
  - `armLockLeftSleeveRollOffset`
- For `lock_parent_arms_after_emf_animate` and `lock_parent_arms_and_held_item`, compute the normal calibrated arm source first, then compute a sleeve-calibrated source by adding sleeve-only offsets.
- Apply sleeve-calibrated source only to vanilla sleeves and EMF custom sleeves.
- Do not apply sleeve-only offsets to parent/inner arms, EMF custom arms, held-item bridge source, body/head/legs, or first-person rendering.

Preferred first arm fix:

- Capture the arm pose before Fresh/EMF overwrites it.
- Restore only right arm, left arm, right sleeve, and left sleeve after Fresh/EMF runs.
- Leave Fresh body, legs, and head animation intact.

Only use direct EMF suppression if:

- The exact target class, method, descriptor, and field access are identified from the installed EMF jar.
- The mixin is optional-safe.
- The mod starts cleanly when EMF/Fresh is absent.

Fallback:

- `manual_vpb_like_arm_pose`, using conservative VPB-style rifle/pistol arm placement only if exact capture/restore is not possible.

## Implementation Rules

- Use optional-safe mixins for any EMF, ETF, Fresh, or Not Enough Animations related target.
- A missing optional mod must never crash the compatibility mod.
- Do not modify GoodCraft configs or mod/resourcepack files.
- Do not redistribute third-party assets.
- Do not touch first-person rendering.
- Do not touch gameplay systems.
- Keep the mod client-side unless a future blocker proves a server-side component is unavoidable.

## Expected Project Files

Expected implementation files after the docs phase:

- `settings.gradle`
- `build.gradle`
- `gradle.properties`
- `src/main/resources/fabric.mod.json`
- Client initializer, config, detector, and debug overlay classes under `src/main/java`.

Milestone 1 had no mixins. Milestone 2 adds `src/main/resources/vpb_fpa_compat.mixins.json` for trace-only mixins. These mixins must not alter model state.

No source files should embed VPB, Fresh, EMF, ETF, or Blue Archive assets.

## GoodCraft Test Deployment

Deployment happens only after a successful build:

1. Build with `.\gradlew build`.
2. Validate the jar contents.
3. Remove or move any previous `vpb-fpa-compat-*.jar` from the GoodCraft `mods` folder.
4. Copy the new built jar into:

```text
<path-to-local-goodcraft-test-profile>\mods
```

Do not edit GoodCraft configs, jars, zips, resource packs, or content packs.

## Acceptance Criteria

Milestone 1:

- Project builds.
- Mod loads in GoodCraft.
- Base VPB guns are detected.
- Blue Archive VPB guns are detected.
- Debug overlay/log confirms active state in F5.
- Fresh Player Animations remains active normally.
- No gameplay or first-person regression is introduced.

Milestone 2:

- When holding a VPB gun, arms no longer use the broken Fresh arm pose.
- Body and legs still animate with Fresh.
- Sleeves remain aligned with arms.
- Aim, fire, reload, sprint, and crouch do not produce broken poses.

## Milestone 2.15 next validation

Test `locked_parent_pose` against the previous `source_pose` sleeve sync behavior. If sleeves now move with the same timing as the inner arms/gun, continue tuning sleeve-only pitch. If not, inspect EMF sleeve render timing again instead of using freeze.

## Manual Milestone 2.16 Direction

The sleeve calibration branch is paused. The current plan is to simplify the player-facing behavior:

1. Normal gun holding stance should pass through untouched.
2. Aim/use input should enable the lock strategy.
3. Sleeves should be hidden during the aim lock to avoid outer-layer mismatch.
4. Inner arms and held gun should retain the no-jiggle lock behavior.
