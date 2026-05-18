# Compatibility Map

## Project Target

Create a separate Fabric client mod for the GoodCraft profile that keeps Fresh Player Animations active while preventing broken third-person arm poses when the player holds Vic's Point Blank guns, including Blue Archive Point Blank content-pack guns.

## Environment Map

| Area | Finding | Compatibility Impact |
| --- | --- | --- |
| Minecraft | `1.21.11` | Use Minecraft 1.21.11 mappings and runtime assumptions. |
| Loader | Fabric Loader `0.19.2` | Build a Fabric client mod only. |
| Fabric API | `0.141.4+1.21.11` | Use this as the profile-matching API version. |
| Point Blank | `2.0.1` | Detect guns through Point Blank item type/API where possible. |
| Blue Archive VPB | `pointblank\bluearchive-ext v0.6.zip` | Treat as Point Blank content pack, not a separate mod namespace. |
| Fresh Animations | `FreshAnimations_v1.10.4.zip` active in `options.txt` | Fresh base resource pack is active. |
| FA Player | `FA+Player-v1.0.zip` seen in resource reload log | Player extension appears active from latest reload evidence; re-confirm if logs change. |
| FA All Extensions | `FA+All_Extensions-v1.8.1.zip` active in `options.txt` and reload log | Extension pack is active. |
| EMF | `3.2.4` | Likely Fresh player model animation executor. Optional-safe only. |
| ETF | `7.1` | Related visual mod; do not assume it is needed for logic. |
| Not Enough Animations | `1.12.3` | May also touch player model setup. Trace before arm fix. |
| Iris/Sodium | Installed | Validate no shader/render crash, but do not integrate directly unless needed. |

## Detection Map

Preferred VPB weapon detection order:

1. Reflectively load `com.vicmatskiv.pointblank.item.GunItem`.
2. Check whether the main-hand item is an instance of that class.
3. If `includeOffhand` is enabled, also check offhand.
4. Log the registry id of the detected item.
5. If class detection fails, fall back to registry/content-pack heuristics.

Fallback heuristics:

- Registry namespace `pointblank`.
- Item ids from Point Blank item definitions where available.
- Content-pack item JSON with `"type": "Gun"` where available.

Avoid hardcoding every Blue Archive weapon unless all dynamic detection paths fail.

Runtime validation from GoodCraft confirmed the primary class-based path:

- `vpb_fpa_compat 0.1.0` loaded.
- Point Blank, `GunItem`, EMF, ETF, and Not Enough Animations were detected.
- Base VPB guns detected by class method: `pointblank:aughbar`, `pointblank:m134minigun`.
- Blue Archive VPB gun detected by class method: `pointblank:ba_whitefang465`.
- Detection method was `class`; fallback was not used.
- Fresh Player Animations was visibly active in-game.
- HUD overlay was not visible in the first runtime test, so Milestone 1.2 moves it to Fabric's HUD element registry with a visible background.
- Milestone 1.2.1 completed overlay text visibility; the overlay is visible in GoodCraft.

## Milestone 2 Trace Target Map

Confirmed trace targets:

- Vanilla/model setup: `net.minecraft.client.render.entity.model.PlayerEntityModel#setAngles(PlayerEntityRenderState)`.
- Final player render: `net.minecraft.client.render.entity.LivingEntityRenderer#render(LivingEntityRenderState, MatrixStack, OrderedRenderCommandQueue, CameraRenderState)`.
- EMF candidate: `traben.entity_model_features.models.animation.state.EMFBipedPose#applyTo(BipedEntityModel<?>)`.
- NEA candidate: `dev.tr7zw.notenoughanimations.logic.PlayerTransformer#updateModel(ClientPlayerEntity, PlayerEntityModel, float, CallbackInfo)`.

Implemented trace stages:

- `player_model_set_angles_head`
- `vanilla_or_model_setup_tail`
- `nea_candidate_before`
- `nea_candidate_after`
- `emf_candidate_before`
- `emf_candidate_after`
- `final_render_before`
- `final_render_after`

Trace snapshots include active detection state, hand, held item id, local/remote marker, player name/id, camera perspective, tick marker, and model part pitch/yaw/roll for arms, sleeves, body, head, and legs.

Skipped trace targets:

- Point Blank direct model trace is skipped for Milestone 2 because the concrete model-writing path found locally is Point Blank's optional PlayerAnimator compatibility mixin, and standalone PlayerAnimator is not loaded in GoodCraft.
- ETF is skipped because no ETF player-pose writer was identified; ETF remains environment context only.

Milestone 2 trace mixins are diagnostics-only. They must not write model parts or change render behavior.

Likely next implementation strategy after collecting trace logs:

- If arms are correct before EMF/NEA and broken after one candidate stage, prefer a targeted suppress/restore strategy around that stage.
- If arms are already broken by `vanilla_or_model_setup_tail`, investigate Point Blank/PlayerAnimator availability and whether VPB is failing to apply a third-person arm pose in this stack.
- If arms are correct at `final_render_before`, avoid model-pose fixes and look for held-item transform or feature-renderer issues instead.
- Keep any future fix behind `armMode`; do not change first-person rendering or gameplay.

Milestone 2 runtime trace result:

- Arms are gun-like at `vanilla_or_model_setup_tail` for VPB and Blue Archive guns.
- Arms are overwritten by `final_render_after` while body/head/legs also receive desired Fresh-style movement.
- This supports restoring only arms/sleeves from the model-setup snapshot after later animation systems run.
- EMF/NEA optional trace mixins reported `apply=true`, but their candidate stage logs did not appear, so direct EMF/NEA suppression is not yet proven.

Milestone 2.1 trace clarity:

- EMF/NEA candidate hooks now emit throttled `hook=<stage> reached` logs while `poseTracing=true`.
- If hook logs do not appear, the target method is not reached for this runtime path.
- If hook logs appear with `context=false`, the hook is reached outside the current player-model trace context.
- If hook logs appear with `context=true` but no candidate snapshot follows, trace filters or model type checks are suppressing the snapshot.

Experimental restore placement:

- Capture: `vanilla_or_model_setup_tail`, where runtime traces showed VPB gun arms are still gun-like.
- Restore: immediately before `OrderedRenderCommandQueue.submitModel(...)`, which is the confirmed pre-main-model submission point.
- Restore again: `LivingEntityRenderer#render` return, because the renderer calls `model.setAngles(...)` again for feature rendering after main model submission; the command queue may render deferred commands from the final mutable model state.

Milestone 2.1 runtime result:

- Restore mode is internally successful; trace logs show vanilla arms and sleeves restored to gun-like values.
- The visible F5 result remains unchanged.
- This means either the visible Fresh/EMF arm geometry is not governed by vanilla `rightArm`/`leftArm`, command submission timing still does not affect visible geometry, custom arm children override rendering, or the visible mismatch is held-item/gun transform rather than arm rotations.

Milestone 2.2 proof diagnostics:

- `visual_probe_exaggerated_arms` will set vanilla arms/sleeves to an intentionally obvious pose for detected VPB guns.
- If this is visible in F5, vanilla arm parts affect rendering and the issue is timing/pose choice.
- If this is not visible in F5, visible Fresh/EMF arms are likely custom or separately controlled parts.
- Model probing will dump model/arm class names and recursively accessible child part names around arms/sleeves.
- Hook diagnostics now log as `[VPB-FPA Hook] hook=emf_applyTo_* ...` and `[VPB-FPA Hook] hook=nea_updateModel_* ...` while tracing is enabled.

Milestone 2.2 runtime result:

- `visual_probe_exaggerated_arms` loaded but was not visible for base VPB or Blue Archive guns.
- Model root is `traben.entity_model_features.models.parts.EMFModelPartRoot`.
- Vanilla arm/sleeve fields are `traben.entity_model_features.models.parts.EMFModelPartVanilla`.
- Root reports `partCount=34`, but the current dumper could not list EMF root children/parts.
- Vanilla root/arms/sleeves appeared `empty=true`, supporting the theory that visible Fresh arms are custom EMF/Fresh parts rather than the vanilla fields currently restored.

Milestone 2.3 investigation:

- Inspect installed EMF part classes to locate the part registry/list and names.
- Improve model probing to list EMF/Fresh parts and arm-like candidates.
- Add `visual_probe_all_arm_like_parts` to exaggerate every discovered arm/hand/sleeve candidate for proof testing.

EMF class inspection:

- `EMFModelPartRoot` extends `EMFModelPartVanilla`.
- `EMFModelPartRoot` stores the 34 visible/known vanilla-format parts in private `Map<String, EMFModelPartVanilla> allVanillaParts`.
- `EMFModelPartRoot#getAllVanillaPartsEMF()` exposes those parts as a collection.
- `EMFModelPartVanilla` has final `String name`, boolean `isPlayerArm`, and `getAllEMFCustomChildren()`.
- `EMFModelPartCustom` has final `String id` and `String partToBeAttached`.
- `EMFModelPartWithState` has `vanillaChildren` and state variant data.
- The improved dumper uses reflection defensively for names/children and calls public EMF methods reflectively when present; failure only logs unavailable data.

Milestone 2.3 visual probe fix:

- `visual_probe_exaggerated_arms` was not actually being applied in 2.2 because the restore path returned early when no restore snapshot existed.
- The visual probe branch now runs before the snapshot requirement.
- `visual_probe_all_arm_like_parts` applies exaggerated rotations to vanilla arms/sleeves plus discovered EMF/Fresh candidates whose path/name contains arm, sleeve, or hand, optionally narrowed by `visualProbePartNameFilter`.

Milestone 2.3 first runtime result:

- GoodCraft crashed when switching to F5 with `armMode=visual_probe_all_arm_like_parts`, `poseTracing=true`, and `dumpModelParts=true`.
- Crash was `StackOverflowError` in `ModelPartDumper.collectEmfParts`, reached through `allVanillaParts`.
- The likely cause is unbounded recursive traversal of EMF parts where root/global part collections reference shared parts or are repeatedly exposed from nested parts.
- The visual probe result is inconclusive because the crash happened before a useful F5 observation.

Milestone 2.3.1 safety requirement:

- Use object identity tracking for every recursive part walk.
- Add conservative traversal bounds: `dumpModelPartsMaxDepth=6` and `dumpModelPartsMaxParts=128`.
- Call root-level `getAllVanillaPartsEMF()` once per scan and iterate it flatly.
- Inspect direct custom children only within the same bounds.
- Make `visual_probe_all_arm_like_parts` operate only on the safe discovered candidate list.

Milestone 2.3.1 safe runtime result:

- F5 no longer crashes with bounded model dumping.
- `ModelProbe` discovered the EMF root as `traben.entity_model_features.models.parts.EMFModelPartRoot`, with `partCount=34` and `discoveredParts=31`.
- Non-empty visible-looking EMF arm/sleeve candidates were found: `EMF_right_arm`, `EMF_left_arm`, `EMF_right_sleeve`, and `EMF_left_sleeve`.
- Empty vanilla proxy parts were also present: `right_arm`, `left_arm`, `right_sleeve`, and `left_sleeve`.
- `visual_probe_all_arm_like_parts` did apply internally at `before_submit_model`, changing eight parts.
- F5 visuals remained unchanged, so the next target is render timing, command submission, and whether the gun visual mismatch is related to held-item transform instead of model arm rotations.
- Logged order indicates the current `before_submit_model` injection may run before a later `PlayerEntityModel#setAngles` call.

Milestone 2.4 render-flow inspection:

- `LivingEntityRenderer#render` calls `OrderedRenderCommandQueue#submitModel(...)` before feature rendering.
- `submitModel(...)` does not bake the player model pose immediately. `BatchingRenderCommandQueue#submitModel(...)` stores a `ModelCommand` containing a copied matrix entry, the mutable `Model` reference, and the render state.
- Later, `ModelCommandRenderer#render(ModelCommand, ...)` executes the visible model command. It calls `model.setAngles(state)` and then immediately calls `model.render(...)`.
- Therefore the current `before_submit_model` hook can be overwritten by the deferred `model.setAngles(state)` inside `ModelCommandRenderer`.
- The better confirmed pre-visible-render hook is inside `ModelCommandRenderer`, after `Model#setAngles(Object)` and before the first `Model#render(...)`.
- `HeldItemFeatureRenderer#renderItem(...)` calls `ModelWithArms#setArmAngle(...)`, then applies fixed item rotations/translations, then renders `ItemRenderState`. This is a plausible place to trace whether the gun/held-item transform contributes to the visual mismatch.
- `ItemRenderState` has no public display-context getter in this mapping, so held-item trace logs `mode=unknown` plus item id, arm, hand, arm pose, and whether the item state is empty.

Milestone 2.4 new trace stages:

- `set_angles_tail_after_probe_candidate`
- `actual_model_command_head`
- `actual_model_set_angles_after`
- `actual_model_render_head`
- `actual_model_render_tail`
- `held_item_feature_head`

Milestone 2.4 diagnostic arm modes:

- `visual_probe_after_set_angles_tail`
- `visual_probe_at_actual_model_render`

Both modes are VPB-only, local-player by default, and mutate only the safe arm-like candidate list.

Milestone 2.4 runtime result:

- `visual_probe_after_set_angles_tail` was visible for base VPB and Blue Archive guns.
- `visual_probe_at_actual_model_render` was also visible for base VPB and Blue Archive guns.
- `after_set_angles_tail` is the preferred real hook because it runs after the final visible `Model#setAngles(...)` and before actual `Model#render(...)`.
- Held item trace confirmed VPB guns render as held items with `armPose=bow_and_arrow` and `itemStateEmpty=false`.
- Both visual probes caused a first-person state leak after F5/item switching, so every mutation must be bracketed and restored after render.

Milestone 2.5 first real restore target:

- New mode: `restore_vpb_arms_after_set_angles_tail`.
- Hook: `set_angles_tail_after_probe_candidate`, but only while inside the confirmed `ModelCommandRenderer` player model command.
- Source pose: vanilla `rightArm` and `leftArm` immediately after final visible `setAngles`.
- Targets: vanilla arms/sleeves and discovered arm-like EMF/Fresh candidates.
- Cleanup: restore every touched part's original rotation after `Model#render(...)` returns.

Milestone 2.6 layer-alignment finding:

- Runtime confirmed the 2.5 restore path partially fixes the visible F5 gun pose.
- The first-person leak exposed by visual probes is fixed by scoped mutation cleanup.
- Remaining blocker: outer arm/sleeve layers can separate from the base arms.
- Logs show base arms and sleeves can diverge during or after `Model#render`: base arms move back toward Fresh/normal-ish values, while sleeve parts remain in the restored gun pose.
- Working hypothesis: applying the same absolute pose to both parent/base arms and sleeve/custom parts may double-transform or mis-target relative sleeve layers. The next diagnostic pass filters restore targets by layer family.

Milestone 2.6 layer modes:

- `arms_and_sleeves`: current behavior, restore vanilla and EMF arm/sleeve candidates.
- `arms_only`: restore arm candidates, leave sleeves untouched.
- `sleeves_only`: restore sleeve candidates only.
- `vanilla_only`: restore only vanilla/proxy arm and sleeve parts.
- `emf_custom_only`: restore only EMF custom arm and sleeve candidates.
- `emf_custom_arms_only`: restore only `EMF_right_arm` and `EMF_left_arm`-style custom arm candidates.
- `emf_custom_sleeves_only`: restore only EMF custom sleeve candidates.

Milestone 2.6 runtime result:

- `arms_and_sleeves` is still the best baseline, but it remains partial and arm/sleeve layers separate.
- `arms_only`, `emf_custom_arms_only`, `emf_custom_only`, and `vanilla_only` were worse.
- `vanilla_only` showed one layer closer to the expected VPB pose while another stayed offset/unchanged, confirming the visible result is split across more than one transform/render path.
- First-person leak cleanup stayed fixed and must not regress.

Milestone 2.7 synchronization target:

- Identify the authoritative transform path for visible base arms, visible sleeve/outer layers, and held gun placement.
- Trace vanilla arms/sleeves, EMF custom arm/sleeve candidates, and held item state at confirmed render points.
- Inspect EMF attachment metadata for whether sleeve custom parts are attached to, parented by, or independent from arm custom parts.
- Add opt-in `armRestoreStrategy` diagnostics instead of more layer combinations.
- Keep `armMode=off` by default and preserve the 2.5/2.6 cleanup behavior.

EMF part metadata confirmed locally:

- `EMFModelPartCustom` fields: `partToBeAttached`, `id`, `attach`, `attachments`.
- `EMFModelPartVanilla` fields/methods: `name`, `isPlayerArm`, `getAllEMFCustomChildren()`.
- Runtime sync logs now report `attachedTo`, `attach`, attachment count, vanilla child count, and `inherits=unknown` because no direct parent pointer was exposed by `javap`.

Milestone 2.7 strategy diagnostics:

- `source_pose_to_selected_parts`: current 2.6 behavior.
- `copy_sleeves_from_final_arms`: copy sleeve rotations from the matching final arm candidate rather than directly from the VPB source pose.
- `restore_before_held_item_too`: attempt a scoped reapply before held-item feature rendering only if the render model context is active; otherwise log the missing context.
- `restore_arms_then_sync_sleeves_late`: restore arms at set-angles tail, then sync sleeves from final arms at render head.
- `observe_only`: no mutation, sync trace only.

Milestone 2.7 runtime result:

- `observe_only` confirmed held item detection but `HeldItemSync` reported `context=none`.
- `restore_arms_then_sync_sleeves_late` was worse.
- `restore_before_held_item_too` was partial and made the gun appear better aligned, but still logged `held_item_no_active_model_context`.
- Held item bridge is therefore not active yet and is not the primary 2.8 fix target.
- The decisive trace is in-render arm drift: at model render head, vanilla arms, sleeves, and EMF candidates are synchronized; by render tail, vanilla/base arms drift back while sleeves/custom parts remain restored.

Milestone 2.8 render method inspection:

- `Model#render(MatrixStack, VertexConsumer, int, int, int)` calls `getRootPart().render(...)`.
- `ModelPart#render(...)` applies the part transform, renders cuboids, then renders children.
- `PlayerEntityModel` sleeves are children of `leftArm` and `rightArm` in vanilla model data.
- EMF overrides the render path as `method_22699(...)` on `EMFModelPartVanilla` and `EMFModelPartCustom`.
- `EMFModelPartWithState#method_22699(...)` calls `root.oneTimeRunnable()`, `root.animate()`, then delegates to `EMFModelPart#method_22699(...)`; this is the likely in-render place where base-arm drift is introduced.

Milestone 2.8 strategy diagnostics:

- `restore_at_arm_part_render`: reapply the captured VPB pose immediately before relevant arm/sleeve part render.
- `restore_base_arms_at_part_render_only`: preserve current baseline but reapply only base/non-custom arms immediately before those arm parts render.

Milestone 2.8.1 boot safety finding:

- The Milestone 2.8 jar crashed before launch with `ReEntrantTransformerError` while transforming `net.minecraft.client.main.Main`.
- The unsafe path was `VpbFpaMixinPlugin` using `Class.forName(...)` to check optional target class existence during mixin selection.
- Mixin plugin selection must not load Minecraft, EMF, NEA, Point Blank, or mapped client classes. Optional mixins are gated by `FabricLoader.isModLoaded(...)` only.
- Runtime config cannot prevent this class of crash because mixin transformation happens before config logic matters.

Milestone 2.8.2 NEA trace descriptor:

- 2.8.1 restored launch safety: main menu and world load worked, overlay appeared, and no re-entrance crash was present.
- Installed NEA `PlayerTransformer#updateModel` signature is `(AbstractClientPlayerEntity, PlayerEntityModel, float, CallbackInfo)`.
- The previous mixin handler used `ClientPlayerEntity`, causing a non-fatal `InvalidInjectionException`.
- NEA trace is diagnostic-only; if future descriptor drift is unstable, disable the NEA trace mixin instead of blocking the part-render investigation.

Milestone 2.9 transform-authority finding:

- Part-render restore strategies made tracked rotations match internally but visuals were worse.
- The remaining blocker is likely full transform authority, not rotation timing alone.
- Suspects: pivot/origin mismatch, scale/position mismatch, parent/attachment transform mismatch, EMF custom local basis, or wrong visible layer authority.
- Older screenshots suggest the sleeve/outer layer may be closer to correct while the inner/base arm is wrong.

Milestone 2.9 diagnostics:

- Log full transform fields for vanilla arms/sleeves and EMF custom arm/sleeve candidates: rotation, origin/pivot, scale, visibility, hidden state, empty/cuboid count, attachment metadata, and class name.
- Add `armRestoreSourceMode`: `vanilla_arm_source`, `emf_custom_arm_source`, `emf_custom_sleeve_source`, `vanilla_sleeve_source`.
- Add strategies: `copy_full_transform_to_arms_and_sleeves`, `copy_sleeve_transform_to_base_arm`, `copy_emf_sleeve_transform_to_emf_arm`, `hide_base_arms_keep_sleeves`, `hide_sleeves_keep_base_arms`, and `observe_full_transform_only`.
- Implemented transform cleanup stores and restores `ModelPart` rotation, `originX/Y/Z`, `xScale/yScale/zScale`, `visible`, and `hidden`.
- Runtime logs are needed to determine whether pivots/origins differ between the base arm and sleeve/EMF layers; the build now exposes those values but does not assume an authority.
- The first authority tests should hide base arms, hide sleeves, then copy sleeve transform to base arm.

Milestone 2.10 sleeve-authority finding:

- The old `13:35:58` visual baseline was reproduced with the current jar using `source_pose_to_selected_parts`.
- Sleeves/outer layer appear to be the closest correct visual authority for base VPB and Blue Archive guns.
- Base/inner arms appear to be the wrong visual layer after EMF/part rendering drifts them back toward Fresh/normal values.
- The earlier `hide_base_arms_keep_sleeves` strategy did not prove authority because it did not explicitly restore sleeves to the VPB source pose.
- Next diagnostics should explicitly restore sleeves/custom sleeve layers while hiding or neutralizing base/custom inner arm layers.

Milestone 2.11 custom inner-arm delta finding:

- Restored sleeves/outer layers are confirmed as the correct VPB-looking authority.
- Hiding EMF custom arms makes visible inner arms disappear, so `EMF_right_arm` / `EMF_left_arm` are likely the visible inner arm geometry.
- Vanilla/base arm fields behave like empty mutable anchors/proxies and drift during EMF render.
- Applying the source pose directly to EMF custom arms is wrong, and neutralizing EMF custom arms is also wrong.
- The next diagnostic strategy computes EMF custom arm local rotation from `desired VPB source pose - current mutated vanilla/anchor parent arm pose` at EMF custom part render head.

Milestone 2.12 arm-lock correction:

- The earlier sleeve/arm interpretation is corrected: sleeves/outer layer are correct-looking, inner arms are wrong, and held gun alignment is separate.
- Full, pitch-only, and no-roll custom-arm delta strategies all failed despite applying deltas, so further Euler guessing is paused.
- The next diagnostic is a lock strategy: after EMF/Fresh mutates the vanilla arm anchor, reapply the VPB source pose to that parent/anchor, keep EMF custom arm local rotation neutral under the locked parent, and keep sleeves restored to the source pose.
- Held item context remains diagnostic-only; the bridge should expose source pose availability during held-item rendering without rewriting item transforms.

Milestone 2.13 calibration target:

- `lock_parent_arms_and_held_item` is the new best baseline.
- The held-item bridge works after initial frames and the gun aligns with the locked arm basis.
- Arms, sleeves, and gun no longer jiggle while walking/running in that strategy.
- Fresh body/legs continue animating and the first-person cleanup remains fixed.
- Remaining blocker is no longer synchronization; it is the final locked pose orientation/calibration.
- Delta strategies are deprecated as failed diagnostics.
- Add zero-default lock pose offsets for right/left pitch, yaw, and roll, applied only to `lock_parent_arms_after_emf_animate` and `lock_parent_arms_and_held_item`.
- Add `armLockCustomArmMode` with `neutral`, `source`, and `hidden` to determine whether the final visual issue is parent/sleeve calibration or EMF custom arm local behavior.

Milestone 2.14 sleeve-only calibration target:

- Runtime testing showed `armLockCustomArmMode=neutral` is the best mode; `source` and `hidden` are worse.
- Global arm-lock pitch offsets move the sleeve layer in the useful direction, but they also move inner arms and the held gun.
- Inner arms and the held gun are already aligned in the winning `lock_parent_arms_and_held_item` baseline.
- Remaining calibration target is sleeve orientation only.
- Add zero-default right/left sleeve pitch/yaw/roll offsets.
- Sleeve offsets apply only to vanilla sleeves and EMF custom sleeves in the lock strategies.
- Sleeve offsets must not affect parent/inner arms, EMF custom arms, held-item bridge source, body/head/legs, or first-person rendering.

## Render And Pose Map

Known pose participants to trace:

- Vanilla `PlayerModel` pose setup.
- Point Blank third-person gun pose logic.
- Fresh/EMF player model animation expression application.
- Not Enough Animations `PlayerModel` injections.
- Final player model render call.

Important vanilla/Fresh model parts:

- `rightArm`
- `leftArm`
- `rightSleeve`
- `leftSleeve`
- `body`
- `head`
- `rightLeg`
- `leftLeg`

The compatibility mod should only capture, restore, suppress, or manually adjust arms, sleeves, and discovered arm-extension parts. It should leave body, legs, and head to Fresh unless a specific aiming breakage is proven.

## EMF Investigation Gate

Direct EMF suppression is not the first arm fix. It is allowed only after confirming the exact installed target in the local EMF jar.

Candidate from inspection, to revalidate before implementation:

- Class: `traben.entity_model_features.models.animation.state.EMFBipedPose`
- Method candidate: `applyTo`
- Behavior candidate: applies stored pose parts to `HumanoidModel`, including head, body, left arm, right arm, legs, and root.

Before any direct EMF mixin:

- Confirm the class exists in the installed EMF jar.
- Confirm method name and descriptor after remapping/compilation context.
- Confirm field names or access strategy for left/right arm pose parts.
- Confirm the mixin can be skipped safely when EMF is absent.
- Confirm the mod still starts without EMF/Fresh installed.

## Arm Fix Strategy Map

Arm modes:

- `off`: detection/debug only; no pose changes.
- `restore_vpb_arms_after_fresh`: capture arm pose before Fresh/EMF overwrite, then restore only arms and sleeves after Fresh/EMF.
- `suppress_fresh_arms_when_vpb_weapon`: optional direct Fresh/EMF suppression for arm pose only, if the installed target proves stable.
- `manual_vpb_like_arm_pose`: fallback pose approximation if exact capture/restore is not technically viable.

First implementation must ship detection, debug, config, and `armMode = off` or no-op behavior. It should avoid render/animation mixins entirely unless detection proves impossible without them.

## Non-Goals

- No first-person rendering changes.
- No projectile, damage, sound, networking, item, recipe, or gameplay changes.
- No edits to GoodCraft configs, jars, zips, or resource packs.
- No redistribution of VPB, Fresh, EMF, ETF, or Blue Archive assets.

## Milestone 2.15 note

`armLockSleeveSyncMode=locked_parent_pose` is an opt-in diagnostic path for EMF/Fresh timing. It does not add dependencies or bundle third-party assets. It only changes late sleeve writes for detected VPB/Blue Archive guns under the lock strategies.

## Manual Milestone 2.16 Compat Notes

- `armLockAimOnly` uses local client aim/use state as a conservative activation gate for lock strategies.
- `armLockHideSleeves` only hides sleeve parts while a lock strategy is applying; it is not a global skin/resource-pack modification.
