# Implementation Log

Use this format for every implementation or validation step:

```text
date/time:
files changed:
reason:
validation command:
result:
risks/next step:
```

## Entries

### 2026-05-18T07:52:08-03:00

date/time: 2026-05-18T07:52:08-03:00

files changed:

- `docs/findings.md`
- `docs/compat-map.md`
- `docs/plan.md`
- `docs/implementation-log.md`

reason: Record docs touchups before Milestone 1 implementation, including active Fresh/FA evidence, dependency-resolution caution, and the no-render-mixin boundary for Milestone 1.

validation command:

```powershell
Get-Content "$Profile\options.txt" -Raw | Select-String -Pattern "resourcePacks|incompatibleResourcePacks"
Select-String -Path "$Profile\logs\latest.log" -Pattern "FreshAnimations|FA\+Player|FA\+All|Reloading ResourceManager"
```

result: Local files show Fresh Animations and FA All Extensions in `options.txt`; latest resource reload log also includes `FA+Player-v1.0.zip`.

risks/next step: Continue with Milestone 1 only: Fabric client mod load, config, VPB gun detection, debug logging, and debug overlay.

### 2026-05-18T08:02:42-03:00 Milestone 1 Bootstrap

date/time: 2026-05-18T08:02:42-03:00

files changed:

- `settings.gradle`
- `build.gradle`
- `gradle.properties`
- `gradle/wrapper/gradle-wrapper.properties`
- `gradle/wrapper/gradle-wrapper.jar`
- `gradlew`
- `gradlew.bat`
- `.gitignore`
- `src/main/resources/fabric.mod.json`
- `src/main/java/com/joao2/vpbfpa/**`
- `README.md`
- `docs/plan.md`

reason: Create the smallest Fabric client mod for Milestone 1: load, config defaults, reflected VPB gun detection, conservative fallback detection, throttled debug logging, and optional HUD overlay. No mixins or arm-pose changes were added.

validation command:

```powershell
.\gradlew build
```

result: Initial wrapper `9.2.1` was incompatible with resolved Fabric Loom Remap `1.16.2`, which requested Gradle plugin API `9.4.0`. Wrapper was updated to Gradle `9.4.0`. Settings repository policy was changed to prefer project repositories so Loom could provide Mojang/Fabric repositories. Final build succeeded.

risks/next step: Copy the built jar into GoodCraft only after review, then validate mod load, overlay text, Point Blank detection, and Blue Archive `pointblank:ba_*` detection in-game.

### 2026-05-18T08:02:42-03:00 Jar Validation

date/time: 2026-05-18T08:02:42-03:00

files changed: none after validation.

reason: Confirm the built jar contains mod metadata/classes and no redistributed third-party assets.

validation command:

```powershell
Get-ChildItem "build\libs" -Filter "*.jar" | Sort-Object LastWriteTime -Descending
jar tf "build\libs\vpb-fpa-compat-0.1.0.jar" | Select-String -Pattern "fabric.mod.json|vpb_fpa_compat|com/joao2/vpbfpa"
jar tf "build\libs\vpb-fpa-compat-0.1.0.jar" | Select-String -Pattern "pointblank|Fresh|FA\+|entity_model_features|entity_texture_features|bluearchive|blue-archive"
```

result: `build\libs\vpb-fpa-compat-0.1.0.jar` exists, contains `fabric.mod.json` and `com/joao2/vpbfpa` classes, and does not contain VPB/Fresh/EMF/ETF/Blue Archive asset paths. Extracted `fabric.mod.json` confirms id `vpb_fpa_compat`, version `0.1.0`, and environment `client`.

risks/next step: GoodCraft runtime validation has not been performed yet; the jar has not been copied into the profile.

### 2026-05-18T08:02:42-03:00 Milestone 1.1 Cleanup Prep

date/time: 2026-05-18T08:02:42-03:00

files changed:

- `src/main/java/com/joao2/vpbfpa/config/CompatConfig.java`
- `src/main/java/com/joao2/vpbfpa/VpbFpaCompatClient.java`
- `scripts/install-goodcraft-test-jar.ps1`
- `README.md`
- `docs/plan.md`
- `docs/compat-map.md`
- `docs/test-matrix.md`
- `docs/implementation-log.md`
- `.agents/agents/client-render-mixin-engineer.md`
- `.agents/skills/minecraft-client-compat-release.md`

reason: Add snake_case config aliases while keeping camelCase preferred, clarify that Milestone 1 has no mixins, document `fresh: unknown` as acceptable before runtime validation, and add a narrow GoodCraft install helper that is not run automatically.

validation command:

```powershell
git status --short --ignored
```

result: The command failed because the workspace is not a git repository. `.gitignore` still ignores `.gradle/`, `build/`, `run/`, `out/`, and `*.class`; no generated files were removed.

risks/next step: Run `.\gradlew clean build` and validate the jar before any GoodCraft copy step.

### 2026-05-18T08:16:31-03:00 Milestone 1.1 Build Validation

date/time: 2026-05-18T08:16:31-03:00

files changed:

- `docs/implementation-log.md`

reason: Record cleanup validation results after Milestone 1.1.

validation command:

```powershell
.\gradlew clean build
jar tf "build\libs\vpb-fpa-compat-0.1.0.jar" | Select-String -Pattern "fabric.mod.json|com/joao2/vpbfpa"
jar tf "build\libs\vpb-fpa-compat-0.1.0.jar" | Select-String -Pattern "pointblank|Fresh|FA\+|entity_model_features|entity_texture_features|bluearchive|blue-archive"
jar tf "build\libs\vpb-fpa-compat-0.1.0.jar" | Select-String -Pattern "mixin|mixins|vpb_fpa_compat.mixins"
```

result: Clean build succeeded. The jar contains `fabric.mod.json` and `com/joao2/vpbfpa` classes. No third-party asset paths or mixin metadata were found.

risks/next step: GoodCraft runtime validation is next. The install helper has not been run and no GoodCraft files were changed.

### 2026-05-18T08:22:29-03:00 Install Helper Backup Path Fix

date/time: 2026-05-18T08:22:29-03:00

files changed:

- `scripts/install-goodcraft-test-jar.ps1`
- `README.md`
- `docs/implementation-log.md`

reason: Move previous test-jar backups outside the GoodCraft `mods` folder and into `vpb-fpa-compat-backups\<timestamp>` under the profile root.

validation command:

```powershell
.\gradlew clean build
$null = [scriptblock]::Create((Get-Content ".\scripts\install-goodcraft-test-jar.ps1" -Raw))
```

result: Clean build succeeded. Install script syntax parsed successfully. Built jar: `build\libs\vpb-fpa-compat-0.1.0.jar`.

risks/next step: Runtime test in GoodCraft. The install helper has not been run by this validation.

### 2026-05-18T08:47:55-03:00 Milestone 1.2 Runtime Validation

date/time: 2026-05-18T08:47:55-03:00

files changed:

- `docs/implementation-log.md`
- `docs/test-matrix.md`
- `docs/compat-map.md`

reason: Record GoodCraft runtime validation from the installed Milestone 1 jar.

validation command:

```powershell
GoodCraft runtime test with vpb-fpa-compat-0.1.0.jar installed.
```

result: Game launched normally. Fresh Player Animations was visibly active. `vpb_fpa_compat 0.1.0` loaded and used defaults because config was absent. Init logged `enabled=true`, `debugLogging=true`, `debugOverlay=true`, `detectionMode=AUTO`, `armMode=OFF`, `pointblankMod=true`, `gunItemClass=true`, `emf=true`, `etf=true`, and `nea=true`. Base VPB guns `pointblank:aughbar` and `pointblank:m134minigun` were detected. Blue Archive gun `pointblank:ba_whitefang465` was detected. Detection method was `class`, fallback was `false`. HUD overlay was not visible. Arm compatibility is not implemented yet and remained unchanged.

risks/next step: Improve overlay visibility/reliability only; do not implement arm/render/model fixes.

### 2026-05-18T08:47:55-03:00 Milestone 1.2 Overlay Reliability

date/time: 2026-05-18T08:47:55-03:00

files changed:

- `src/main/java/com/joao2/vpbfpa/config/CompatConfig.java`
- `src/main/java/com/joao2/vpbfpa/debug/DebugOverlay.java`
- `src/main/java/com/joao2/vpbfpa/VpbFpaCompatClient.java`
- `README.md`
- `docs/plan.md`
- `docs/implementation-log.md`

reason: Make the debug overlay more reliable and visible without adding mixins or touching player model/render pose logic.

validation command:

```powershell
.\gradlew build
```

result: Compile/build succeeded after moving the overlay from deprecated `HudRenderCallback` to Fabric `HudElementRegistry.addLast`, adding top-left configurable coordinates, scale, and a dark translucent background. The likely cause of the invisible overlay was the deprecated callback/layer ordering and low-contrast no-background text competing with a heavily modded HUD.

risks/next step: Run clean build and jar validation, then reinstall and retest overlay visibility in GoodCraft.

### 2026-05-18T08:49:12-03:00 Milestone 1.2 Build Validation

date/time: 2026-05-18T08:49:12-03:00

files changed:

- `docs/implementation-log.md`

reason: Record Milestone 1.2 clean build and jar validation.

validation command:

```powershell
.\gradlew clean build
jar tf "build\libs\vpb-fpa-compat-0.1.0.jar" | Select-String -Pattern "fabric.mod.json|com/joao2/vpbfpa"
jar tf "build\libs\vpb-fpa-compat-0.1.0.jar" | Select-String -Pattern "mixin|mixins|vpb_fpa_compat.mixins"
jar tf "build\libs\vpb-fpa-compat-0.1.0.jar" | Select-String -Pattern "pointblank|Fresh|FA\+|entity_model_features|entity_texture_features|bluearchive|blue-archive"
```

result: Clean build succeeded. Jar contains `fabric.mod.json` and `com/joao2/vpbfpa` classes. No mixin metadata and no third-party asset paths were found. Built jar: `build\libs\vpb-fpa-compat-0.1.0.jar`.

risks/next step: Reinstall the jar into GoodCraft and retest overlay visibility while empty-handed, with a base VPB gun, and with a Blue Archive VPB gun.

### 2026-05-18T09:07:47-03:00 Milestone 1.2.1 Overlay Text Rendering

date/time: 2026-05-18T09:07:47-03:00

files changed:

- `src/main/java/com/joao2/vpbfpa/debug/DebugOverlay.java`
- `docs/implementation-log.md`

reason: Fix invisible overlay text while keeping the existing HUD element/background path and avoiding mixins, player model changes, and gameplay changes.

validation command:

```powershell
javap -classpath <minecraft-merged-1.21.11-yarn-jar> net.minecraft.client.gui.DrawContext | Select-String -Pattern "drawTextWithShadow|drawText\("
.\gradlew clean build
jar tf "build\libs\vpb-fpa-compat-0.1.0.jar" | Select-String -Pattern "fabric.mod.json|com/joao2/vpbfpa"
jar tf "build\libs\vpb-fpa-compat-0.1.0.jar" | Select-String -Pattern "mixin|mixins|vpb_fpa_compat.mixins"
```

result: Clean build succeeded. Jar contains `fabric.mod.json` and `com/joao2/vpbfpa` classes. No mixin metadata and no third-party asset paths were found. Investigation confirmed Yarn 1.21.11 has `DrawContext.drawTextWithShadow(TextRenderer, String, int, int, int)`. The likely text invisibility cause was color alpha: previous text colors were `0xFFFFFF` and `0xFFFF55`, while the background used ARGB. The overlay now uses opaque ARGB colors `0xFFFFFFFF` and `0xFFFFFF55`, keeps background-first/text-after draw order, guarantees non-empty core lines, and logs a throttled overlay render summary when debug logging is enabled. Built jar: `build\libs\vpb-fpa-compat-0.1.0.jar`.

risks/next step: Reinstall the jar and retest overlay text in GoodCraft.

### 2026-05-18T09:10:00-03:00 Milestone 1 Runtime Completion

date/time: 2026-05-18T09:10:00-03:00

files changed:

- `src/main/java/com/joao2/vpbfpa/config/CompatConfig.java`
- `src/main/java/com/joao2/vpbfpa/arm/ArmRestoreManager.java`
- `src/main/java/com/joao2/vpbfpa/trace/ModelPartDumper.java`
- `README.md`
- `docs/implementation-log.md`
- `docs/test-matrix.md`
- `docs/compat-map.md`
- `docs/plan.md`

reason: Record validated Milestone 1 runtime results before starting Milestone 2 tracing.

validation command:

```powershell
GoodCraft runtime test with vpb-fpa-compat-0.1.0.jar installed.
```

result: Game launches normally. Fresh Player Animations is visibly active. Debug overlay works. Empty hand and random/non-gun items are inactive. Base Vic's Point Blank guns are active. Blue Archive Point Blank guns are active. All tested VPB and Blue Archive guns detected correctly. No gameplay or first-person issues were observed. Arm compatibility is still not implemented.

risks/next step: Begin Milestone 2 as diagnostics only: pose tracing with config kill switches, no arm fix, no pose writes, and no gameplay or first-person changes.

### 2026-05-18T09:10:00-03:00 Milestone 2 Trace Target Inspection

date/time: 2026-05-18T09:10:00-03:00

files changed:

- `docs/implementation-log.md`
- `docs/compat-map.md`

reason: Record confirmed trace targets from local Yarn and installed mod jars before adding tracing mixins.

validation command:

```powershell
javap -classpath <minecraft-merged-1.21.11-yarn-jar> -private net.minecraft.client.render.entity.model.PlayerEntityModel
javap -classpath <minecraft-merged-1.21.11-yarn-jar> -private net.minecraft.client.render.entity.LivingEntityRenderer
javap -classpath <GoodCraft EMF jar> -private traben.entity_model_features.models.animation.state.EMFBipedPose
javap -classpath <GoodCraft NEA jar> -private dev.tr7zw.notenoughanimations.logic.PlayerTransformer
```

result: Confirmed vanilla trace targets `PlayerEntityModel#setAngles(PlayerEntityRenderState)` and `LivingEntityRenderer#render(LivingEntityRenderState, MatrixStack, OrderedRenderCommandQueue, CameraRenderState)`. Confirmed EMF target `traben.entity_model_features.models.animation.state.EMFBipedPose#applyTo(BipedEntityModel<?>)`. Confirmed NEA target `dev.tr7zw.notenoughanimations.logic.PlayerTransformer#updateModel(ClientPlayerEntity, PlayerEntityModel, float, CallbackInfo)`. Point Blank's direct player model pose hook is in its optional PlayerAnimator compatibility path; standalone PlayerAnimator is not loaded in GoodCraft, so no Point Blank-specific model trace hook is added in this pass.

risks/next step: Add read-only trace mixins for confirmed targets, with optional-safe plugin gating for EMF and NEA.

### 2026-05-18T09:26:16-03:00 Milestone 2 Trace Implementation And Build

date/time: 2026-05-18T09:26:16-03:00

files changed:

- `src/main/java/com/joao2/vpbfpa/config/CompatConfig.java`
- `src/main/java/com/joao2/vpbfpa/VpbFpaCompatClient.java`
- `src/main/java/com/joao2/vpbfpa/detect/VpbWeaponDetector.java`
- `src/main/java/com/joao2/vpbfpa/trace/PoseTraceLogger.java`
- `src/main/java/com/joao2/vpbfpa/mixin/VpbFpaMixinPlugin.java`
- `src/main/java/com/joao2/vpbfpa/mixin/PlayerEntityModelTraceMixin.java`
- `src/main/java/com/joao2/vpbfpa/mixin/LivingEntityRendererTraceMixin.java`
- `src/main/java/com/joao2/vpbfpa/mixin/compat/EmfBipedPoseTraceMixin.java`
- `src/main/java/com/joao2/vpbfpa/mixin/compat/NeaPlayerTransformerTraceMixin.java`
- `src/main/resources/vpb_fpa_compat.mixins.json`
- `src/main/resources/fabric.mod.json`
- `README.md`
- `docs/plan.md`
- `docs/compat-map.md`
- `docs/test-matrix.md`
- `docs/implementation-log.md`

reason: Add configurable, read-only third-person player pose tracing. The trace system snapshots model part rotations and logs at throttled intervals without altering model state.

validation command:

```powershell
.\gradlew clean build
jar tf "build\libs\vpb-fpa-compat-0.1.0.jar" | Select-String -Pattern "fabric.mod.json|vpb_fpa_compat|mixins|com/joao2/vpbfpa"
jar tf "build\libs\vpb-fpa-compat-0.1.0.jar" | Select-String -Pattern "pointblank|Fresh|FA\+|entity_model_features|entity_texture_features|bluearchive|blue-archive"
```

result: Clean build succeeded. The jar contains `fabric.mod.json`, `vpb_fpa_compat.mixins.json`, and `com/joao2/vpbfpa` classes. Mixin metadata is present only for tracing. No third-party asset paths were found. Built jar: `build\libs\vpb-fpa-compat-0.1.0.jar`.

risks/next step: Install in GoodCraft, enable `poseTracing`, collect logs for empty hand/random item if desired, base VPB guns, and Blue Archive guns, then compare arm snapshots across trace stages before implementing any arm fix.

### 2026-05-18T09:35:00-03:00 Milestone 2 Runtime Trace Analysis

date/time: 2026-05-18T09:35:00-03:00

files changed:

- `docs/implementation-log.md`
- `docs/compat-map.md`
- `docs/plan.md`
- `docs/test-matrix.md`

reason: Record GoodCraft runtime trace results before adding the first experimental arm restore mode.

validation command:

```powershell
GoodCraft runtime test with poseTracing=true and armMode=off.
```

result: Game launched normally. Debug overlay worked. VPB and Blue Archive guns were detected. Pose trace logs were produced. Config initialized with `poseTracing=true`, `poseTraceLogging=true`, `armMode=OFF`, `pointblankMod=true`, `gunItemClass=true`, `emf=true`, `etf=true`, and `nea=true`. Optional trace mixins reported `EmfBipedPoseTraceMixin apply=true` and `NeaPlayerTransformerTraceMixin apply=true`. For VPB and Blue Archive guns, arms were gun-like at `vanilla_or_model_setup_tail`, with right/left arm pitch around `-1.5` to `-1.7`. By `final_render_after`, arms were overwritten to Fresh/normal-ish values, with right arm around `-0.3` to `-0.4` pitch and left arm around `0.0`. Body/head/legs also changed later, which is desired for Fresh. EMF/NEA candidate mixins applied, but `emf_candidate_before/after` and `nea_candidate_before/after` did not appear in filtered logs.

risks/next step: This strongly supports a capture/restore strategy. Direct EMF/NEA suppression is not proven yet because their candidate trace stages did not appear.

### 2026-05-18T09:40:00-03:00 Milestone 2.1 Restore Experiment

date/time: 2026-05-18T09:40:00-03:00

files changed:

- `src/main/java/com/joao2/vpbfpa/config/ArmMode.java`
- `src/main/java/com/joao2/vpbfpa/config/CompatConfig.java`
- `src/main/java/com/joao2/vpbfpa/arm/ArmRestoreManager.java`
- `src/main/java/com/joao2/vpbfpa/mixin/PlayerEntityModelTraceMixin.java`
- `src/main/java/com/joao2/vpbfpa/mixin/LivingEntityRendererTraceMixin.java`
- `src/main/java/com/joao2/vpbfpa/mixin/compat/EmfBipedPoseTraceMixin.java`
- `src/main/java/com/joao2/vpbfpa/mixin/compat/NeaPlayerTransformerTraceMixin.java`
- `src/main/java/com/joao2/vpbfpa/trace/PoseTraceLogger.java`
- `src/main/java/com/joao2/vpbfpa/debug/DebugOverlay.java`
- `README.md`
- `docs/plan.md`
- `docs/compat-map.md`
- `docs/test-matrix.md`
- `docs/implementation-log.md`

reason: Add experimental `restore_model_setup_arms_after_render_setup` mode behind config and improve EMF/NEA trace clarity with throttled hook-reached logs.

validation command:

```powershell
.\gradlew clean build
jar tf "build\libs\vpb-fpa-compat-0.1.0.jar" | Select-String -Pattern "fabric.mod.json|vpb_fpa_compat|mixins|com/joao2/vpbfpa"
jar tf "build\libs\vpb-fpa-compat-0.1.0.jar" | Select-String -Pattern "pointblank|Fresh|FA\+|entity_model_features|entity_texture_features|bluearchive|blue-archive"
```

result: Clean build succeeded. The jar contains `fabric.mod.json`, `vpb_fpa_compat.mixins.json`, and `com/joao2/vpbfpa` classes including the experimental `ArmRestoreManager`. No third-party asset paths were found. Built jar: `build\libs\vpb-fpa-compat-0.1.0.jar`.

risks/next step: Test in GoodCraft with `armMode=restore_model_setup_arms_after_render_setup`; compare visual arm pose and `[VPB-FPA ArmFix]` logs before adding any broader/manual fallback.

### 2026-05-18T10:10:00-03:00 Milestone 2.1 Runtime Visual Mismatch

date/time: 2026-05-18T10:10:00-03:00

files changed:

- `docs/implementation-log.md`
- `docs/compat-map.md`
- `docs/plan.md`
- `docs/test-matrix.md`

reason: Record Milestone 2.1 runtime result before adding Milestone 2.2 proof diagnostics.

validation command:

```powershell
GoodCraft runtime test with armMode=restore_model_setup_arms_after_render_setup.
```

result: Game launched. Overlay was active on guns. Fresh body/legs still animated. Sleeves were aligned. First person stayed normal. No crashes or gameplay issues were observed. Visual arm compatibility remained unchanged for base VPB and Blue Archive guns. Logs proved the restore mode was active: `[VPB-FPA ArmFix] captured stage=vanilla_or_model_setup_tail`, `[VPB-FPA ArmFix] restored stage=before_submit_model`, and `final_render_after` showed right/left arms and sleeves restored to gun-like values. Therefore the traced vanilla model parts are changing internally, but the visible player arms are not matching that restored pose.

risks/next step: Investigate whether visible Fresh/EMF arms are not vanilla `rightArm`/`leftArm`, whether command timing still defeats the restore, whether custom child parts render independently, or whether held-item/gun transforms are the real visual mismatch.

### 2026-05-18T10:15:00-03:00 Milestone 2.2 Visual Probe

date/time: 2026-05-18T10:15:00-03:00

files changed:

- `src/main/java/com/joao2/vpbfpa/config/ArmMode.java`
- `src/main/java/com/joao2/vpbfpa/config/CompatConfig.java`
- `src/main/java/com/joao2/vpbfpa/arm/ArmRestoreManager.java`
- `src/main/java/com/joao2/vpbfpa/trace/ModelPartDumper.java`
- `src/main/java/com/joao2/vpbfpa/trace/PoseTraceLogger.java`
- `src/main/java/com/joao2/vpbfpa/mixin/LivingEntityRendererTraceMixin.java`
- `src/main/java/com/joao2/vpbfpa/mixin/compat/EmfBipedPoseTraceMixin.java`
- `src/main/java/com/joao2/vpbfpa/mixin/compat/NeaPlayerTransformerTraceMixin.java`
- `src/main/java/com/joao2/vpbfpa/debug/DebugOverlay.java`
- `README.md`
- `docs/plan.md`
- `docs/compat-map.md`
- `docs/test-matrix.md`
- `docs/implementation-log.md`

reason: Add diagnostic `visual_probe_exaggerated_arms`, model part probing, and clearer EMF/NEA hook logs without implementing the final fix.

validation command:

```powershell
.\gradlew clean build
jar tf "build\libs\vpb-fpa-compat-0.1.0.jar" | Select-String -Pattern "fabric.mod.json|vpb_fpa_compat|mixins|com/joao2/vpbfpa"
jar tf "build\libs\vpb-fpa-compat-0.1.0.jar" | Select-String -Pattern "pointblank|Fresh|FA\+|entity_model_features|entity_texture_features|bluearchive|blue-archive"
```

result: Clean build succeeded. The jar contains `fabric.mod.json`, `vpb_fpa_compat.mixins.json`, and `com/joao2/vpbfpa` classes including `ModelPartDumper`. No third-party asset paths were found. Built jar: `build\libs\vpb-fpa-compat-0.1.0.jar`.

risks/next step: Test whether exaggerated vanilla arms visibly affect F5 rendering. If not visible, focus next on EMF/Fresh custom parts or held-item transform rather than vanilla arm rotations.

### 2026-05-18T10:40:00-03:00 Milestone 2.2 Runtime Visual Probe Result

date/time: 2026-05-18T10:40:00-03:00

files changed:

- `docs/implementation-log.md`
- `docs/compat-map.md`
- `docs/plan.md`
- `docs/test-matrix.md`

reason: Record GoodCraft visual probe results before adding EMF/Fresh visible-arm discovery.

validation command:

```powershell
GoodCraft runtime test with armMode=visual_probe_exaggerated_arms.
```

result: Game launched normally. Overlay was active on guns. Base VPB and Blue Archive visual probe was not visible. Fresh body/legs still animated. First person stayed normal. No crashes or issues were observed. Config loaded `armMode=VISUAL_PROBE_EXAGGERATED_ARMS` with Point Blank, GunItem, EMF, ETF, and NEA detected. Model probe reported `rootClass=traben.entity_model_features.models.parts.EMFModelPartRoot`, `partCount=34`, and vanilla arm/sleeve fields as `EMFModelPartVanilla`. The probed vanilla root/rightArm/leftArm/rightSleeve/leftSleeve were `empty=true` and `children=unavailable`. No `[VPB-FPA ArmFix] visual_probe applied...` line appeared in the filtered output.

risks/next step: Discover the EMF/Fresh visible arm-like parts inside the EMF root part registry/tree, and verify visual probe logging is reachable.

### 2026-05-18T10:55:00-03:00 Milestone 2.3 EMF Part Discovery

date/time: 2026-05-18T10:55:00-03:00

files changed:

- `src/main/java/com/joao2/vpbfpa/config/ArmMode.java`
- `src/main/java/com/joao2/vpbfpa/config/CompatConfig.java`
- `src/main/java/com/joao2/vpbfpa/arm/ArmRestoreManager.java`
- `src/main/java/com/joao2/vpbfpa/trace/ModelPartDumper.java`
- `src/main/java/com/joao2/vpbfpa/trace/PoseTraceLogger.java`
- `src/main/java/com/joao2/vpbfpa/mixin/compat/EmfBipedPoseTraceMixin.java`
- `src/main/java/com/joao2/vpbfpa/mixin/compat/NeaPlayerTransformerTraceMixin.java`
- `README.md`
- `docs/plan.md`
- `docs/compat-map.md`
- `docs/implementation-log.md`

reason: Discover EMF/Fresh visible arm-like parts and fix visual probe applicability/logging.

validation command:

```powershell
javap -classpath <GoodCraft EMF jar> -private traben.entity_model_features.models.parts.EMFModelPartRoot
javap -classpath <GoodCraft EMF jar> -private traben.entity_model_features.models.parts.EMFModelPartVanilla
javap -classpath <GoodCraft EMF jar> -private traben.entity_model_features.models.parts.EMFModelPartCustom
.\gradlew clean build
jar tf "build\libs\vpb-fpa-compat-0.1.0.jar" | Select-String -Pattern "fabric.mod.json|vpb_fpa_compat|mixins|com/joao2/vpbfpa"
jar tf "build\libs\vpb-fpa-compat-0.1.0.jar" | Select-String -Pattern "pointblank|Fresh|FA\+|entity_model_features|entity_texture_features|bluearchive|blue-archive"
```

result: Clean build succeeded. The jar contains `fabric.mod.json`, `vpb_fpa_compat.mixins.json`, and `com/joao2/vpbfpa` classes. No third-party asset paths were found. EMF inspection confirmed `EMFModelPartRoot` stores parts in private `allVanillaParts` and exposes `getAllVanillaPartsEMF()`. `EMFModelPartVanilla` has `name`, `isPlayerArm`, and `getAllEMFCustomChildren()`. `EMFModelPartCustom` has `id` and `partToBeAttached`. The previous visual probe mode was not actually applying because it required a restore snapshot before checking the visual-probe branch; that is fixed. Built jar: `build\libs\vpb-fpa-compat-0.1.0.jar`.

risks/next step: Validate build, then test `visual_probe_all_arm_like_parts` in GoodCraft with model part dumping enabled.

### 2026-05-18T11:05:00-03:00 Milestone 2.3.1 StackOverflow Crash Record

date/time: 2026-05-18T11:05:00-03:00

files changed:

- `src/main/java/com/joao2/vpbfpa/config/CompatConfig.java`
- `src/main/java/com/joao2/vpbfpa/arm/ArmRestoreManager.java`
- `src/main/java/com/joao2/vpbfpa/trace/ModelPartDumper.java`
- `README.md`
- `docs/implementation-log.md`
- `docs/test-matrix.md`
- `docs/compat-map.md`
- `docs/plan.md`

reason: Record the first Milestone 2.3 GoodCraft runtime failure before changing the dumper.

validation command:

```powershell
GoodCraft runtime test with armMode=visual_probe_all_arm_like_parts, poseTracing=true, dumpModelParts=true.
.\gradlew clean build
jar tf "build\libs\vpb-fpa-compat-0.1.0.jar" | Select-String -Pattern "fabric.mod.json|vpb_fpa_compat|mixins|com/joao2/vpbfpa"
jar tf "build\libs\vpb-fpa-compat-0.1.0.jar" | Select-String -Pattern "pointblank|Fresh|FA\+|entity_model_features|entity_texture_features|bluearchive|blue-archive"
```

result: GoodCraft crashed when switching to F5 with `java.lang.StackOverflowError: Rendering entity in world`. The stack points to `ModelPartDumper.allVanillaParts` and repeated `ModelPartDumper.collectEmfParts` calls. The likely cause is recursive EMF part traversal without cycle protection, combined with repeatedly calling root-wide EMF part listing methods from nested parts. The visual probe result is inconclusive because the crash happened before testing. The dumper was changed to use identity visited tracking, conservative `dumpModelPartsMaxDepth=6` and `dumpModelPartsMaxParts=128` defaults, cached reflection lookups, and a flat root-level EMF part scan. Clean build succeeded. The jar contains `fabric.mod.json`, `vpb_fpa_compat.mixins.json`, and `com/joao2/vpbfpa` classes. No third-party asset paths were found. Built jar: `build\libs\vpb-fpa-compat-0.1.0.jar`.

risks/next step: Retest `visual_probe_all_arm_like_parts` in GoodCraft with bounded dumping. If traversal truncates, use the logged reason and optional part-name filter to narrow the next probe.

### 2026-05-18T11:30:00-03:00 Milestone 2.4 Timing Investigation Record

date/time: 2026-05-18T11:30:00-03:00

files changed:

- `src/main/java/com/joao2/vpbfpa/config/ArmMode.java`
- `src/main/java/com/joao2/vpbfpa/arm/ArmRestoreManager.java`
- `src/main/java/com/joao2/vpbfpa/mixin/PlayerEntityModelTraceMixin.java`
- `src/main/java/com/joao2/vpbfpa/mixin/ModelCommandRendererTraceMixin.java`
- `src/main/java/com/joao2/vpbfpa/mixin/HeldItemFeatureRendererTraceMixin.java`
- `src/main/java/com/joao2/vpbfpa/trace/PoseTraceLogger.java`
- `src/main/resources/vpb_fpa_compat.mixins.json`
- `README.md`
- `docs/implementation-log.md`
- `docs/test-matrix.md`
- `docs/compat-map.md`
- `docs/plan.md`

reason: Record Milestone 2.3.1 safe visual probe runtime result before adding render timing diagnostics.

validation command:

```powershell
GoodCraft runtime test with armMode=visual_probe_all_arm_like_parts, poseTracing=true, dumpModelParts=true.
javap -classpath <minecraft-merged-1.21.11-yarn-jar> -private -c net.minecraft.client.render.entity.LivingEntityRenderer
javap -classpath <minecraft-merged-1.21.11-yarn-jar> -private -c net.minecraft.client.render.command.BatchingRenderCommandQueue
javap -classpath <minecraft-merged-1.21.11-yarn-jar> -private -c net.minecraft.client.render.command.ModelCommandRenderer
javap -classpath <minecraft-merged-1.21.11-yarn-jar> -private -c net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer
.\gradlew clean build
jar tf "build\libs\vpb-fpa-compat-0.1.0.jar" | Select-String -Pattern "fabric.mod.json|vpb_fpa_compat|mixins|com/joao2/vpbfpa"
jar tf "build\libs\vpb-fpa-compat-0.1.0.jar" | Select-String -Pattern "pointblank|Fresh|FA\+|entity_model_features|entity_texture_features|bluearchive|blue-archive"
```

result: Game launched normally. F5 no longer crashed. Overlay was active on guns. Base VPB and Blue Archive all-arm visual probes remained visually unchanged. Fresh body/legs still animated. First person stayed normal. No crash/issues were observed. `ModelProbe` discovered EMF root `traben.entity_model_features.models.parts.EMFModelPartRoot`, `partCount=34`, and `discoveredParts=31`. It found non-empty EMF custom candidates `EMF_right_arm`, `EMF_left_arm`, `EMF_right_sleeve`, and `EMF_left_sleeve`, plus empty vanilla proxy parts `right_arm`, `left_arm`, `right_sleeve`, and `left_sleeve`. The all-arm probe applied internally at `before_submit_model` with `target=all parts=8`, and traces showed exaggerated arm/sleeve values, but F5 output was unchanged. The suspicious log order was `final_render_before`, `visual_probe applied stage=before_submit_model`, `player_model_set_angles_head`, `vanilla_or_model_setup_tail`, `final_render_after`, which suggests the existing `before_submit_model` injection can run before the final `PlayerEntityModel#setAngles` call. Bytecode inspection confirmed `submitModel` stores a mutable model/state command; `ModelCommandRenderer#render` later calls `Model#setAngles(state)` and then `Model#render(...)`. Added timing trace stages and diagnostic modes around those confirmed points, plus held-item feature trace logging. Clean build succeeded. The jar contains the new trace mixins and no third-party asset paths. Built jar: `build\libs\vpb-fpa-compat-0.1.0.jar`.

risks/next step: Test `visual_probe_after_set_angles_tail` first, then `visual_probe_at_actual_model_render` if needed. If neither is visible, inspect held-item trace output and consider that the visible issue may be item transform or a different render path.

### 2026-05-18T12:05:00-03:00 Milestone 2.5 Runtime Result And Restore Plan

date/time: 2026-05-18T12:05:00-03:00

files changed:

- `src/main/java/com/joao2/vpbfpa/config/ArmMode.java`
- `src/main/java/com/joao2/vpbfpa/config/CompatConfig.java`
- `src/main/java/com/joao2/vpbfpa/arm/ArmRestoreManager.java`
- `src/main/java/com/joao2/vpbfpa/mixin/PlayerEntityModelTraceMixin.java`
- `src/main/java/com/joao2/vpbfpa/mixin/ModelCommandRendererTraceMixin.java`
- `README.md`
- `docs/implementation-log.md`
- `docs/test-matrix.md`
- `docs/compat-map.md`
- `docs/plan.md`

reason: Record decisive Milestone 2.4 runtime results before adding the first real restore mode.

validation command:

```powershell
GoodCraft runtime test with armMode=visual_probe_after_set_angles_tail.
GoodCraft runtime test with armMode=visual_probe_at_actual_model_render.
.\gradlew clean build
jar tf "build\libs\vpb-fpa-compat-0.1.0.jar" | Select-String -Pattern "fabric.mod.json|vpb_fpa_compat|mixins|com/joao2/vpbfpa"
jar tf "build\libs\vpb-fpa-compat-0.1.0.jar" | Select-String -Pattern "pointblank|Fresh|FA\+|entity_model_features|entity_texture_features|bluearchive|blue-archive"
```

result: Both timing probes were visible for base VPB and Blue Archive guns, proving the mod can reach the actual visible F5 player model. `visual_probe_after_set_angles_tail` applied at `set_angles_tail_after_probe_candidate`, and actual render stages saw the modified values at `actual_model_set_angles_after` and `actual_model_render_head`. Held item trace showed VPB gun item rendering was active with `armPose=bow_and_arrow` and `itemStateEmpty=false`. Both visual probe modes also exposed a first-person state leak after cycling F5 and switching from a VPB gun to empty hand or a regular item. The leak can be cleared by actions that force a pose reset, so the fix must bracket mutations and restore original model-part rotations after render. Added scoped render mutation handling, first-person skip checks, original-rotation cleanup after `Model#render`, optional compare logging, and `restore_vpb_arms_after_set_angles_tail`. Clean build succeeded. The jar contains `fabric.mod.json`, `vpb_fpa_compat.mixins.json`, and `com/joao2/vpbfpa` classes. No third-party asset paths were found. Built jar: `build\libs\vpb-fpa-compat-0.1.0.jar`.

risks/next step: Runtime-test first-person leak cleanup and the new restore mode in GoodCraft. Do not release until the restore visual result and item-switch cleanup are validated.

### 2026-05-18T13:05:00-03:00 Milestone 2.6 Arm Layer Alignment Plan

date/time: 2026-05-18T13:05:00-03:00

files changed:

- `docs/implementation-log.md`
- `docs/test-matrix.md`
- `docs/compat-map.md`
- `docs/plan.md`

reason: Record Milestone 2.5 runtime result before adding targeted layer diagnostics.

validation command:

```powershell
GoodCraft runtime test with armMode=restore_vpb_arms_after_set_angles_tail.
```

result: Game launched with no crash. The overlay showed `restore_vpb_arms_after_set_angles_tail`. Base VPB and Blue Archive F5 arms were partially fixed, Fresh body/legs still animated, first-person guns stayed normal, and the previous first-person leak sequence was fixed. The remaining blocker is arm skin/outer sleeve layer alignment: the outer/second layer can appear separated from the base arm. Logs showed restore and cleanup working, but at render tail base arms had moved back toward Fresh/normal-ish values while sleeves remained in the restored gun pose, indicating base arms and sleeve/custom layer parts can diverge during or after `Model#render`.

risks/next step: Release remains blocked until arm layers align. Add a diagnostic `armRestoreLayerMode` filter so GoodCraft can compare arms-only, EMF-custom-arms-only, and EMF-custom-only restore targets without changing gameplay or first-person rendering.

### 2026-05-18T13:45:00-03:00 Milestone 2.6 Layer Diagnostics Build

date/time: 2026-05-18T13:45:00-03:00

files changed:

- `src/main/java/com/joao2/vpbfpa/config/ArmRestoreLayerMode.java`
- `src/main/java/com/joao2/vpbfpa/config/CompatConfig.java`
- `src/main/java/com/joao2/vpbfpa/debug/DebugOverlay.java`
- `src/main/java/com/joao2/vpbfpa/arm/ArmRestoreManager.java`
- `README.md`
- `docs/implementation-log.md`
- `docs/test-matrix.md`
- `docs/compat-map.md`
- `docs/plan.md`

reason: Add targeted layer diagnostic filtering for the real restore mode without changing the default safe behavior.

validation command:

```powershell
.\gradlew clean build
jar tf "build\libs\vpb-fpa-compat-0.1.0.jar" | Select-String -Pattern "fabric.mod.json|vpb_fpa_compat|mixins|com/joao2/vpbfpa"
jar tf "build\libs\vpb-fpa-compat-0.1.0.jar" | Select-String -Pattern "pointblank|Fresh|FA\+|entity_model_features|entity_texture_features|bluearchive|blue-archive"
```

result: Clean build succeeded. The jar contains `fabric.mod.json`, `vpb_fpa_compat.mixins.json`, and `com/joao2/vpbfpa` classes including `ArmRestoreLayerMode`. The third-party asset namespace scan returned no matches. `armMode` still defaults to `off`; `armRestoreLayerMode` defaults to `arms_and_sleeves`, preserving Milestone 2.5 behavior until a test config changes it. Added overlay `layers: ...`, throttled touched-part logs, and render-tail `layer_compare` logging when `armRestoreDebugCompare=true`. Built jar: `build\libs\vpb-fpa-compat-0.1.0.jar`.

risks/next step: Test `arms_only`, `emf_custom_arms_only`, and `emf_custom_only` in GoodCraft. The best-looking mode will identify whether sleeves should inherit naturally, whether only EMF custom arms should be restored, or whether custom sleeves also need direct handling.

### 2026-05-18T14:20:00-03:00 Milestone 2.7 Sync Investigation Plan

date/time: 2026-05-18T14:20:00-03:00

files changed:

- `docs/implementation-log.md`
- `docs/test-matrix.md`
- `docs/compat-map.md`
- `docs/plan.md`

reason: Record Milestone 2.6 runtime results before adding synchronization tracing and strategy diagnostics.

validation command:

```powershell
GoodCraft runtime tests with armRestoreLayerMode=arms_and_sleeves, arms_only, emf_custom_arms_only, emf_custom_only, and vanilla_only.
```

result: `arms_and_sleeves` remains the best baseline, but it is still only partially fixed and not release-ready because arm/sleeve layers separate. `arms_only`, `emf_custom_arms_only`, `emf_custom_only`, and `vanilla_only` were all worse; screenshots for `vanilla_only` showed one layer closer to the expected VPB pose while another layer stayed offset or unchanged. Fresh body/legs still animated, the first-person leak remained fixed, and no crashes or gameplay issues were observed.

risks/next step: Simple target filtering is not enough. The remaining blocker is third-person synchronization across visible base arms, sleeve/outer layer, EMF custom parts, and held gun rendering. Add sync tracing and opt-in restore strategies without changing first-person rendering or gameplay.

### 2026-05-18T14:25:00-03:00 Milestone 2.7 Sync Diagnostics Build

date/time: 2026-05-18T14:25:00-03:00

files changed:

- `src/main/java/com/joao2/vpbfpa/config/ArmRestoreStrategy.java`
- `src/main/java/com/joao2/vpbfpa/config/CompatConfig.java`
- `src/main/java/com/joao2/vpbfpa/debug/DebugOverlay.java`
- `src/main/java/com/joao2/vpbfpa/trace/ModelPartDumper.java`
- `src/main/java/com/joao2/vpbfpa/arm/ArmRestoreManager.java`
- `src/main/java/com/joao2/vpbfpa/mixin/HeldItemFeatureRendererTraceMixin.java`
- `README.md`
- `docs/implementation-log.md`
- `docs/test-matrix.md`
- `docs/compat-map.md`
- `docs/plan.md`

reason: Add synchronization trace and opt-in restore strategies while preserving default `armMode=off`.

validation command:

```powershell
javap -classpath "$Profile\mods\entity_model_features-3.2.4-1.21.11-fabric.jar" -private traben.entity_model_features.models.parts.EMFModelPartCustom
javap -classpath "$Profile\mods\entity_model_features-3.2.4-1.21.11-fabric.jar" -private traben.entity_model_features.models.parts.EMFModelPartVanilla
.\gradlew clean build
jar tf "build\libs\vpb-fpa-compat-0.1.0.jar" | Select-String -Pattern "fabric.mod.json|vpb_fpa_compat|mixins|com/joao2/vpbfpa"
jar tf "build\libs\vpb-fpa-compat-0.1.0.jar" | Select-String -Pattern "pointblank|Fresh|FA\+|entity_model_features|entity_texture_features|bluearchive|blue-archive"
```

result: EMF inspection confirmed `EMFModelPartCustom` exposes `partToBeAttached`, `id`, `attach`, and `attachments`; `EMFModelPartVanilla` exposes `name`, `isPlayerArm`, and `getAllEMFCustomChildren()`. Added reflected attachment metadata logging for EMF custom arm/sleeve candidates. Added `armRestoreStrategy` with `source_pose_to_selected_parts`, `copy_sleeves_from_final_arms`, `restore_before_held_item_too`, `restore_arms_then_sync_sleeves_late`, and `observe_only`. Added `[VPB-FPA SyncTrace]` around set-angles, render-head, render-tail/cleanup, and `[VPB-FPA HeldItemSync]` at held-item feature rendering. Clean build succeeded. The jar contains `fabric.mod.json`, `vpb_fpa_compat.mixins.json`, and `com/joao2/vpbfpa` classes. The third-party asset namespace scan returned no matches. Built jar: `build\libs\vpb-fpa-compat-0.1.0.jar`.

risks/next step: Runtime logs must confirm whether held item feature rendering has an active model context. Local render-flow inspection suggests held-item feature rendering is likely earlier than the deferred model restore, so `restore_before_held_item_too` may log `held_item_no_active_model_context`; that would be useful evidence rather than a failure.

### 2026-05-18T15:15:00-03:00 Milestone 2.8 In-Render Drift Plan

date/time: 2026-05-18T15:15:00-03:00

files changed:

- `docs/implementation-log.md`
- `docs/test-matrix.md`
- `docs/compat-map.md`
- `docs/plan.md`

reason: Record Milestone 2.7 runtime result before adding in-render arm drift tracing and part-render restore diagnostics.

validation command:

```powershell
GoodCraft runtime tests with armRestoreStrategy=observe_only, restore_arms_then_sync_sleeves_late, and restore_before_held_item_too.
javap -classpath <minecraft merged yarn jar> -private -c net.minecraft.client.model.Model
javap -classpath <minecraft merged yarn jar> -private -c net.minecraft.client.model.ModelPart
javap -classpath <minecraft merged yarn jar> -private -c net.minecraft.client.render.entity.model.PlayerEntityModel
javap -classpath <GoodCraft EMF jar> -private -c traben.entity_model_features.models.parts.EMFModelPart
javap -classpath <GoodCraft EMF jar> -private -c traben.entity_model_features.models.parts.EMFModelPartVanilla
javap -classpath <GoodCraft EMF jar> -private -c traben.entity_model_features.models.parts.EMFModelPartCustom
javap -classpath <GoodCraft EMF jar> -private -c traben.entity_model_features.models.parts.EMFModelPartWithState
```

result: `observe_only` left visuals unchanged and confirmed held item detection, but `HeldItemSync` reported `context=none`. `restore_arms_then_sync_sleeves_late` was worse. `restore_before_held_item_too` was partial and made the gun appear better aligned with hands/arms, but `HeldItemSync` still reported `context=none` and the restore probe logged `skipped reason=held_item_no_active_model_context`, so there is no active model bridge at held-item feature time. The decisive trace pattern is in-render drift: at `actual_model_render_head`, vanilla arms, sleeves, and EMF arm/sleeve candidates are synchronized to the VPB pose; by `actual_model_render_tail`, vanilla/base arms have drifted back toward Fresh/normal values while sleeves/custom parts remain restored. Local inspection confirmed `Model#render(...)` calls root `ModelPart#render(...)`; vanilla `ModelPart#render(...)` applies the current part transform, renders cuboids, then renders children. EMF part classes override the render path as `method_22699(...)`; `EMFModelPartWithState#method_22699` calls `root.oneTimeRunnable()`, `root.animate()`, then `EMFModelPart#method_22699(...)`, which supports the hypothesis that EMF animation/render logic can mutate model parts during render.

risks/next step: Add part-render tracing for relevant arm/sleeve parts and opt-in strategies that reapply the captured VPB source pose immediately before arm part rendering. Held-item bridge remains diagnostic, not the primary focus.

### 2026-05-18T15:35:00-03:00 Milestone 2.8 Part Render Diagnostics Build

date/time: 2026-05-18T15:35:00-03:00

files changed:

- `src/main/java/com/joao2/vpbfpa/config/ArmRestoreStrategy.java`
- `src/main/java/com/joao2/vpbfpa/arm/ArmRestoreManager.java`
- `src/main/java/com/joao2/vpbfpa/mixin/ModelPartRenderTraceMixin.java`
- `src/main/java/com/joao2/vpbfpa/mixin/compat/EmfModelPartVanillaRenderTraceMixin.java`
- `src/main/java/com/joao2/vpbfpa/mixin/compat/EmfModelPartCustomRenderTraceMixin.java`
- `src/main/java/com/joao2/vpbfpa/mixin/VpbFpaMixinPlugin.java`
- `src/main/resources/vpb_fpa_compat.mixins.json`
- `README.md`
- `docs/implementation-log.md`
- `docs/test-matrix.md`
- `docs/compat-map.md`
- `docs/plan.md`

reason: Add part-render arm drift tracing and two opt-in part-render restore strategies.

validation command:

```powershell
.\gradlew clean build
jar tf "build\libs\vpb-fpa-compat-0.1.0.jar" | Select-String -Pattern "fabric.mod.json|vpb_fpa_compat|mixins|com/joao2/vpbfpa"
jar tf "build\libs\vpb-fpa-compat-0.1.0.jar" | Select-String -Pattern "pointblank|Fresh|FA\+|entity_model_features|entity_texture_features|bluearchive|blue-archive"
```

result: Clean build succeeded. Added vanilla `ModelPart#render(...)` part tracing plus optional-safe EMF `EMFModelPartVanilla#method_22699(...)` and `EMFModelPartCustom#method_22699(...)` hooks. Added `restore_at_arm_part_render` and `restore_base_arms_at_part_render_only`. Part-render restore is active only inside the existing scoped player model render context, only after VPB detection/source pose capture, and still relies on render-command cleanup. Built jar: `build\libs\vpb-fpa-compat-0.1.0.jar`.

risks/next step: Runtime-test `restore_base_arms_at_part_render_only` first. If it improves layer alignment, the base arm drift happens inside EMF/ModelPart rendering and the final fix should be a narrow base-arm render-time reapply. If it does not, compare with `restore_at_arm_part_render` and inspect `[VPB-FPA PartTrace]` ordering.

### 2026-05-18T15:50:00-03:00 Milestone 2.8.1 Boot Crash Record

date/time: 2026-05-18T15:50:00-03:00

files changed:

- `docs/implementation-log.md`
- `docs/test-matrix.md`
- `docs/compat-map.md`
- `docs/plan.md`

reason: Record the Milestone 2.8 runtime boot crash before fixing mixin plugin safety.

validation command:

```powershell
GoodCraft launch with Milestone 2.8 jar.
```

result: The Milestone 2.8 build succeeded, but GoodCraft crashed before Minecraft launched. Crash type was `org.spongepowered.asm.mixin.transformer.throwables.ReEntrantTransformerError: Re-entrance error` during transformation of `net.minecraft.client.main.Main`. The likely cause is unsafe optional mixin selection: `VpbFpaMixinPlugin` called `Class.forName(...)` on optional target classes during `shouldApplyMixin`, which can load Minecraft/EMF/NEA classes while Mixin is already transforming early client classes. Milestone 2.8 runtime testing is blocked until boot safety is restored.

risks/next step: Remove classloading/reflection from `VpbFpaMixinPlugin.shouldApplyMixin`. Optional mixins should be gated by Fabric mod id only, with no target-class loading in the plugin.

### 2026-05-18T15:58:00-03:00 Milestone 2.8.1 Boot Safety Build

date/time: 2026-05-18T15:58:00-03:00

files changed:

- `src/main/java/com/joao2/vpbfpa/mixin/VpbFpaMixinPlugin.java`
- `docs/implementation-log.md`
- `docs/test-matrix.md`
- `docs/compat-map.md`
- `docs/plan.md`

reason: Remove boot-time optional target class loading from mixin selection.

validation command:

```powershell
.\gradlew clean build
jar tf "build\libs\vpb-fpa-compat-0.1.0.jar" | Select-String -Pattern "fabric.mod.json|vpb_fpa_compat|mixins|com/joao2/vpbfpa"
jar tf "build\libs\vpb-fpa-compat-0.1.0.jar" | Select-String -Pattern "pointblank|Fresh|FA\+|entity_model_features|entity_texture_features|bluearchive|blue-archive"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\scripts\install-goodcraft-test-jar.ps1"
```

result: Clean build succeeded. The jar contains `fabric.mod.json`, `vpb_fpa_compat.mixins.json`, and `com/joao2/vpbfpa` classes. The third-party asset namespace scan returned no matches. `VpbFpaMixinPlugin` no longer calls `Class.forName(...)` or otherwise checks optional target classes. Optional EMF/NEA mixins remain enabled only by `FabricLoader.getInstance().isModLoaded(...)`. The GoodCraft install helper copied `build\libs\vpb-fpa-compat-0.1.0.jar` into the profile `mods` folder and moved no previous compat jars.

risks/next step: Launch GoodCraft to confirm main menu and world load. If a re-entrance crash persists, temporarily disable the new 2.8 part-render mixins and keep the project launchable before continuing arm work.

### 2026-05-18T16:10:00-03:00 Milestone 2.8.2 NEA Descriptor Record

date/time: 2026-05-18T16:10:00-03:00

files changed:

- `docs/implementation-log.md`
- `docs/test-matrix.md`
- `docs/compat-map.md`
- `docs/plan.md`

reason: Record successful 2.8.1 boot result and the new NEA descriptor warning before fixing the diagnostic mixin.

validation command:

```powershell
GoodCraft launch with Milestone 2.8.1 jar.
javap -classpath "$Profile\mods\notenoughanimations-fabric-1.12.3-mc1.21.11.jar" -private -s dev.tr7zw.notenoughanimations.logic.PlayerTransformer
```

result: GoodCraft launched successfully: main menu loaded, world loaded, overlay appeared, and the `ReEntrantTransformerError` was gone. A non-fatal diagnostic mixin warning remained: `NeaPlayerTransformerTraceMixin` had an invalid descriptor because it used `ClientPlayerEntity` (`class_746`) while the installed NEA target expects `AbstractClientPlayerEntity` (`class_742`). `javap` confirmed `PlayerTransformer#updateModel(net.minecraft.class_742, net.minecraft.class_591, float, CallbackInfo)`.

risks/next step: Update `NeaPlayerTransformerTraceMixin` and the trace helper to accept `AbstractClientPlayerEntity`. If descriptor drift recurs later, disable this diagnostic-only mixin rather than risking runtime noise.

### 2026-05-18T16:25:00-03:00 Milestone 2.8.2 NEA Descriptor Build

date/time: 2026-05-18T16:25:00-03:00

files changed:

- `src/main/java/com/joao2/vpbfpa/mixin/compat/NeaPlayerTransformerTraceMixin.java`
- `src/main/java/com/joao2/vpbfpa/trace/PoseTraceLogger.java`
- `docs/implementation-log.md`
- `docs/test-matrix.md`
- `docs/compat-map.md`
- `docs/plan.md`

reason: Match the installed NEA `PlayerTransformer#updateModel` descriptor.

validation command:

```powershell
.\gradlew clean build
jar tf "build\libs\vpb-fpa-compat-0.1.0.jar" | Select-String -Pattern "fabric.mod.json|vpb_fpa_compat|mixins|com/joao2/vpbfpa"
jar tf "build\libs\vpb-fpa-compat-0.1.0.jar" | Select-String -Pattern "pointblank|Fresh|FA\+|entity_model_features|entity_texture_features|bluearchive|blue-archive"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\scripts\install-goodcraft-test-jar.ps1"
```

result: Updated the NEA trace mixin handler from `ClientPlayerEntity` to `AbstractClientPlayerEntity`, matching the installed descriptor `(Lnet/minecraft/class_742;Lnet/minecraft/class_591;FLorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V`. Updated `PoseTraceLogger` to accept `AbstractClientPlayerEntity`. Clean build succeeded. Jar validation found our metadata/classes and no third-party asset namespace matches. Installed the jar into GoodCraft; the helper backed up the previous compat jar under the profile root.

risks/next step: Launch GoodCraft and verify no `InvalidInjectionException` for `NeaPlayerTransformerTraceMixin`. If NEA still drifts in a future version, disable this diagnostic mixin rather than blocking the part-render investigation.

### 2026-05-18T16:45:00-03:00 Milestone 2.9 Full Transform Investigation Plan

date/time: 2026-05-18T16:45:00-03:00

files changed:

- `docs/implementation-log.md`
- `docs/test-matrix.md`
- `docs/compat-map.md`
- `docs/plan.md`

reason: Record Milestone 2.8 runtime results before adding full transform and layer-authority diagnostics.

validation command:

```powershell
GoodCraft runtime tests with armRestoreStrategy=restore_base_arms_at_part_render_only and restore_at_arm_part_render.
```

result: Both part-render restore strategies were worse for base VPB and Blue Archive guns. Sleeves/outer layer did not align, guns did not align with hands/arms, Fresh body/legs still animated, the first-person leak stayed fixed, and there were no crashes. Logs showed tracked rotations could be synchronized at render time, but the visual result was still wrong. This suggests the blocker is no longer simple pitch/yaw/roll timing. Remaining suspects are full transforms, pivots/origins, attachment semantics, EMF custom local transforms, and whether the inner/base arm or sleeve/outer layer should be authoritative. Older screenshots suggest the sleeve/outer layer may have been closer to the correct VPB gun pose while the base/inner arm was wrong.

risks/next step: Extend diagnostics from rotations to full model-part transforms and add opt-in visibility/source-copy strategies. Test hiding base arms vs hiding sleeves first to identify which layer is visually closer to the desired pose.

### 2026-05-18T17:10:00-03:00 Milestone 2.9 Full Transform Build

date/time: 2026-05-18T17:10:00-03:00

files changed:

- `src/main/java/com/joao2/vpbfpa/arm/ArmRestoreManager.java`
- `src/main/java/com/joao2/vpbfpa/config/ArmRestoreStrategy.java`
- `src/main/java/com/joao2/vpbfpa/config/ArmRestoreSourceMode.java`
- `src/main/java/com/joao2/vpbfpa/config/CompatConfig.java`
- `src/main/java/com/joao2/vpbfpa/debug/DebugOverlay.java`
- `src/main/java/com/joao2/vpbfpa/trace/ModelPartDumper.java`
- `src/main/java/com/joao2/vpbfpa/VpbFpaCompatClient.java`
- `README.md`
- `docs/implementation-log.md`
- `docs/test-matrix.md`
- `docs/compat-map.md`
- `docs/plan.md`

reason: Add full-transform diagnostics and opt-in inner-arm/sleeve authority strategies.

validation command:

```powershell
.\gradlew clean build
jar tf "build\libs\vpb-fpa-compat-0.1.0.jar" | Select-String -Pattern "fabric.mod.json|vpb_fpa_compat|mixins|com/joao2/vpbfpa"
jar tf "build\libs\vpb-fpa-compat-0.1.0.jar" | Select-String -Pattern "pointblank|Fresh|FA\+|entity_model_features|entity_texture_features|bluearchive|blue-archive"
```

result: Clean build succeeded. Added `armRestoreSourceMode` with `vanilla_arm_source`, `vanilla_sleeve_source`, `emf_custom_arm_source`, and `emf_custom_sleeve_source`. Added full-transform strategies `copy_full_transform_to_arms_and_sleeves`, `copy_sleeve_transform_to_base_arm`, `copy_emf_sleeve_transform_to_emf_arm`, `hide_base_arms_keep_sleeves`, `hide_sleeves_keep_base_arms`, and `observe_full_transform_only`. Transform cleanup now restores rotation, pivot/origin, scale, visible, and hidden state for every touched part. Transform trace logs now include rotation, pivot/origin, scale, visibility, hidden state, empty/cuboid/child counts, class name, and EMF attachment metadata. Jar validation found our metadata/classes and no third-party asset namespace matches. Built jar: `build\libs\vpb-fpa-compat-0.1.0.jar`.

risks/next step: Runtime-test `hide_base_arms_keep_sleeves`, then `hide_sleeves_keep_base_arms`, then `copy_sleeve_transform_to_base_arm`. These tests should identify whether the sleeve/outer layer or base/inner arm is the visual authority before trying another final restore approach.

### 2026-05-18T16:15:00-03:00 Milestone 2.10 Sleeve Authority Record

date/time: 2026-05-18T16:15:00-03:00

files changed:

- `docs/implementation-log.md`
- `docs/test-matrix.md`
- `docs/compat-map.md`
- `docs/plan.md`

reason: Record the reproduced 13:35 baseline and refine the next diagnostic target.

validation command:

```powershell
GoodCraft runtime test with armMode=restore_vpb_arms_after_set_angles_tail, armRestoreLayerMode=arms_and_sleeves, armRestoreStrategy=source_pose_to_selected_parts, armRestoreSourceMode=vanilla_arm_source.
```

result: The current jar reproduced the old `2026-05-18_13.35.58` screenshot behavior. Base VPB and Blue Archive guns both show sleeves/outer arm layers close to the correct VPB pose while base/inner arms remain wrong. The gun itself is still in the unchanged/wrong position. Fresh body/legs still animate, the first-person leak remains fixed, and there were no crashes. Logs show the VPB source pose is applied to vanilla arms, vanilla sleeves, EMF custom arms, and EMF custom sleeves at `set_angles_tail_after_probe_candidate` / `actual_model_render_head`; later EMF/part rendering drifts vanilla/base arms back toward Fresh/normal values while sleeve and EMF candidates retain the source pose.

risks/next step: Previous `hide_base_arms_keep_sleeves` was inconclusive because it hid arms without first forcing sleeves into the VPB source pose. Add true sleeve-authority strategies that explicitly keep/restores sleeve pose while removing or neutralizing the wrong inner/base arm contribution.

### 2026-05-18T16:20:00-03:00 Milestone 2.10 Sleeve Authority Build

date/time: 2026-05-18T16:20:00-03:00

files changed:

- `src/main/java/com/joao2/vpbfpa/arm/ArmRestoreManager.java`
- `src/main/java/com/joao2/vpbfpa/config/ArmRestoreStrategy.java`
- `README.md`
- `docs/implementation-log.md`
- `docs/test-matrix.md`
- `docs/compat-map.md`
- `docs/plan.md`

reason: Add sleeve-authority diagnostic strategies based on the reproduced baseline.

validation command:

```powershell
.\gradlew clean build
jar tf "build\libs\vpb-fpa-compat-0.1.0.jar" | Select-String -Pattern "fabric.mod.json|vpb_fpa_compat|mixins|com/joao2/vpbfpa"
jar tf "build\libs\vpb-fpa-compat-0.1.0.jar" | Select-String -Pattern "pointblank|Fresh|FA\+|entity_model_features|entity_texture_features|bluearchive|blue-archive"
```

result: Clean build succeeded. Added `source_pose_sleeves_only_hide_base_arms`, `source_pose_sleeves_and_custom_only`, `source_pose_sleeves_hide_emf_custom_arms`, `parent_arm_source_custom_arm_neutral`, and a conservative `parent_arm_source_custom_arm_delta` stub that logs skipped until a safe delta basis is proven. Added `[VPB-FPA AuthorityTrace]` and `[VPB-FPA ArmFix] authority_hide ... reason=wrong_inner_arm_layer` logs. Jar validation found our metadata/classes and no third-party asset namespace matches. Built jar: `build\libs\vpb-fpa-compat-0.1.0.jar`.

risks/next step: Runtime-test the three priority configs in order: sleeve-only with base/custom arms hidden, sleeves with only EMF custom arms hidden, then parent-source with custom arm neutral. The result should tell whether the sleeve/outer layer alone is viable or whether the custom arm local transform must be neutralized instead.

### 2026-05-18T16:35:00-03:00 Milestone 2.11 Custom Arm Delta Record

date/time: 2026-05-18T16:35:00-03:00

files changed:

- `docs/implementation-log.md`
- `docs/test-matrix.md`
- `docs/compat-map.md`
- `docs/plan.md`

reason: Record Milestone 2.10 runtime results before adding the EMF custom inner-arm delta diagnostic.

validation command:

```powershell
GoodCraft runtime tests with source_pose_sleeves_only_hide_base_arms, source_pose_sleeves_hide_emf_custom_arms, parent_arm_source_custom_arm_neutral, and source_pose_sleeves_and_custom_only.
```

result: Sleeve authority is now confirmed. Hiding base/custom arms made visible arms disappear while restored sleeves remained visible and correct. Hiding only EMF custom arms also made base arms invisible in F5, which strongly suggests `EMF_right_arm` / `EMF_left_arm` are the visible inner arm geometry. Direct source pose on EMF custom arms was wrong, and neutral EMF custom arms were also wrong. Vanilla/base arms appear to be empty mutable anchors/proxies that EMF/Fresh mutates during render. Gun alignment is still wrong and remains out of scope for this milestone.

risks/next step: Add a render-time local delta strategy for EMF custom arms: keep sleeves restored to the VPB source pose, then at EMF custom arm render head compute `source - current parent/anchor arm` and apply that as the custom arm local rotation. Add pitch-only and no-roll variants to isolate safe axes if full Euler delta twists badly.

### 2026-05-18T16:45:00-03:00 Milestone 2.11 Custom Arm Delta Build

date/time: 2026-05-18T16:45:00-03:00

files changed:

- `src/main/java/com/joao2/vpbfpa/arm/ArmRestoreManager.java`
- `src/main/java/com/joao2/vpbfpa/config/ArmRestoreStrategy.java`
- `README.md`
- `docs/implementation-log.md`
- `docs/test-matrix.md`
- `docs/compat-map.md`
- `docs/plan.md`

reason: Add render-time EMF custom inner-arm local delta diagnostics.

validation command:

```powershell
.\gradlew clean build
jar tf "build\libs\vpb-fpa-compat-0.1.0.jar" | Select-String -Pattern "fabric.mod.json|vpb_fpa_compat|mixins|com/joao2/vpbfpa"
jar tf "build\libs\vpb-fpa-compat-0.1.0.jar" | Select-String -Pattern "pointblank|Fresh|FA\+|entity_model_features|entity_texture_features|bluearchive|blue-archive"
```

result: Clean build succeeded. Added `source_pose_sleeves_custom_arm_parent_delta`, `source_pose_sleeves_custom_arm_parent_delta_pitch_only`, and `source_pose_sleeves_custom_arm_parent_delta_no_roll`. These strategies restore vanilla/EMF sleeves to the VPB source pose at set-angles tail and do not directly force source pose onto EMF custom arms there. At `emf_custom_part_render_head`, EMF custom arm local rotation is computed from the current mutated vanilla parent/anchor arm and the desired source pose. Added throttled `[VPB-FPA DeltaTrace]` logs with part, source, parent, delta, before, and after rotations. Jar validation found our metadata/classes and no third-party asset namespace matches. Built jar: `build\libs\vpb-fpa-compat-0.1.0.jar`.

risks/next step: Runtime-test full delta first, then pitch-only, then no-roll. If arms improve while gun remains wrong, held-item synchronization should be the next separate milestone.

### 2026-05-18T17:00:00-03:00 Milestone 2.12 Arm Lock Record

date/time: 2026-05-18T17:00:00-03:00

files changed:

- `docs/implementation-log.md`
- `docs/test-matrix.md`
- `docs/compat-map.md`
- `docs/plan.md`

reason: Record failed delta runtime tests and switch to a VPB arm/gun lock diagnostic.

validation command:

```powershell
GoodCraft runtime tests with source_pose_sleeves_custom_arm_parent_delta, source_pose_sleeves_custom_arm_parent_delta_pitch_only, and source_pose_sleeves_custom_arm_parent_delta_no_roll.
```

result: All three delta variants were worse for base VPB and Blue Archive guns. Sleeves remained correct, but inner arms did not align with sleeves and the held gun remained wrong. Fresh body/legs still animated, first-person cleanup remained fixed, and there were no crashes. The important correction is that sleeves/outer layer are the correct VPB-looking pose authority, inner arms are wrong, and the gun/held item is a separate remaining alignment problem. Logs prove deltas were calculated and applied, so the failure means EMF custom arm attachment is not a simple Euler `source - parent` relation, or the effective transform includes attachment/pivot/matrix behavior this diagnostic does not model.

risks/next step: Stop adding Euler delta variants. Add an opt-in lock strategy that treats the vanilla/anchor arm as the locked authority after EMF mutates it, keeps custom inner arms neutral relative to that locked parent, restores sleeves to source pose, and adds a scoped held-item source-pose bridge for diagnostics only.

### 2026-05-18T17:15:00-03:00 Milestone 2.12 Arm Lock Build

date/time: 2026-05-18T17:15:00-03:00

files changed:

- `src/main/java/com/joao2/vpbfpa/arm/ArmRestoreManager.java`
- `src/main/java/com/joao2/vpbfpa/config/ArmRestoreStrategy.java`
- `src/main/java/com/joao2/vpbfpa/config/CompatConfig.java`
- `src/main/java/com/joao2/vpbfpa/mixin/HeldItemFeatureRendererTraceMixin.java`
- `README.md`
- `docs/implementation-log.md`
- `docs/test-matrix.md`
- `docs/compat-map.md`
- `docs/plan.md`

reason: Add opt-in VPB arm-lock strategies and held-item context bridge diagnostics.

validation command:

```powershell
.\gradlew clean build
jar tf "build\libs\vpb-fpa-compat-0.1.0.jar" | Select-String -Pattern "fabric.mod.json|vpb_fpa_compat|mixins|com/joao2/vpbfpa"
jar tf "build\libs\vpb-fpa-compat-0.1.0.jar" | Select-String -Pattern "pointblank|Fresh|FA\+|entity_model_features|entity_texture_features|bluearchive|blue-archive"
```

result: Clean build succeeded. Added `lock_parent_arms_after_emf_animate` and `lock_parent_arms_and_held_item`. Both strategies keep sleeves restored to source pose at set-angles tail. At `emf_vanilla_part_render_head`, the vanilla/anchor arm is re-locked to the VPB source pose after EMF mutation and the matching EMF custom arm is neutralized under the locked parent. Added `armLockFreezeWhileHoldingGun`, default `false`, to optionally reuse the first captured pose for the same held gun. Added a scoped held-item source-pose bridge and held-item tail cleanup; the bridge is diagnostic and does not rewrite item transforms. Added `[VPB-FPA LockTrace]` and held-item bridge availability logging. Built jar: `build\libs\vpb-fpa-compat-0.1.0.jar`.

risks/next step: Runtime-test `lock_parent_arms_after_emf_animate` first. If arms/sleeves lock but gun remains wrong, test `lock_parent_arms_and_held_item` to confirm whether held-item rendering can see the source bridge. If walking/running jiggle remains, repeat with `armLockFreezeWhileHoldingGun=true`.

### 2026-05-18T17:24:51-03:00 Milestone 2.13 Calibration Record

date/time: 2026-05-18T17:24:51-03:00

files changed:

- `docs/implementation-log.md`
- `docs/test-matrix.md`
- `docs/compat-map.md`
- `docs/plan.md`

reason: Record Milestone 2.12 runtime results before adding arm-lock pose calibration.

validation command:

```powershell
GoodCraft runtime tests with lock_parent_arms_after_emf_animate and lock_parent_arms_and_held_item.
```

result: `lock_parent_arms_and_held_item` is the new best baseline. The held-item bridge becomes available after initial frames, `held_item_lock` applies and cleans up, the gun aligns, and arms/sleeves/gun no longer jiggle while walking or running. Fresh body/legs continue animating, first-person cleanup remains fixed, and there were no crashes. Remaining blocker is final third-person arm/sleeve pose orientation: the synchronized pose is stable but still visually up/wrong enough to need calibration. Delta strategies are deprecated as failed diagnostics.

risks/next step: Add zero-default lock pose calibration offsets and a small `armLockCustomArmMode` diagnostic. Keep the winning lock strategy opt-in, preserve first-person cleanup, and do not add new render hooks or gameplay changes.

### 2026-05-18T17:28:35-03:00 Milestone 2.13 Calibration Build

date/time: 2026-05-18T17:28:35-03:00

files changed:

- `src/main/java/com/joao2/vpbfpa/config/ArmLockCustomArmMode.java`
- `src/main/java/com/joao2/vpbfpa/config/CompatConfig.java`
- `src/main/java/com/joao2/vpbfpa/arm/ArmRestoreManager.java`
- `src/main/java/com/joao2/vpbfpa/VpbFpaCompatClient.java`
- `README.md`
- `docs/implementation-log.md`
- `docs/test-matrix.md`
- `docs/compat-map.md`
- `docs/plan.md`

reason: Add zero-default lock pose calibration offsets and a small EMF custom-arm mode diagnostic for the winning lock strategies.

validation command:

```powershell
.\gradlew clean build
jar tf "build\libs\vpb-fpa-compat-0.1.0.jar" | Select-String -Pattern "fabric.mod.json|vpb_fpa_compat|mixins|com/joao2/vpbfpa"
jar tf "build\libs\vpb-fpa-compat-0.1.0.jar" | Select-String -Pattern "pointblank|Fresh|FA\+|entity_model_features|entity_texture_features|bluearchive|blue-archive"
```

result: Clean build succeeded. Added right/left pitch/yaw/roll lock offsets, defaulting to zero, and `armLockCustomArmMode` with `neutral`, `source`, and `hidden`. Offsets apply only to `lock_parent_arms_after_emf_animate` and `lock_parent_arms_and_held_item`; the calibrated source feeds parent/anchor arms, sleeves, EMF sleeves, and the held-item bridge. `neutral` preserves the current winning custom-arm behavior, `source` applies the calibrated source to EMF custom arms, and `hidden` hides EMF custom arms during the scoped render. Jar validation found our metadata/classes and no third-party asset namespace matches. Built jar: `build\libs\vpb-fpa-compat-0.1.0.jar`.

risks/next step: Runtime-test zero-offset `neutral` first to confirm the baseline is unchanged, then compare `source` and `hidden`. If one mode is best, tune the six lock offsets in small radian increments.

### 2026-05-18T18:00:03-03:00 Milestone 2.14 Sleeve-Only Calibration Record

date/time: 2026-05-18T18:00:03-03:00

files changed:

- `docs/implementation-log.md`
- `docs/test-matrix.md`
- `docs/compat-map.md`
- `docs/plan.md`

reason: Record Milestone 2.13 runtime calibration results before adding sleeve-only offsets.

validation command:

```powershell
GoodCraft runtime tests with lock_parent_arms_and_held_item, armLockCustomArmMode variants, and global pitch offsets.
```

result: `armLockCustomArmMode=neutral` remains the best custom-arm mode; `source` and `hidden` are worse. A positive global pitch offset, tested with `armLockRightPitchOffset=0.25` and `armLockLeftPitchOffset=0.25`, moved sleeves down in the correct direction. However, the same global offset also moved the inner arms and attached gun down. Inner arms and the gun were already visually correct/aligned in the winning baseline, so global offsets are too broad for the current remaining issue. No-jiggle behavior, Fresh body/legs, and first-person cleanup remained fixed.

risks/next step: Stop tuning global offsets for sleeve alignment. Add zero-default sleeve-only lock offsets applied only to vanilla/EMF sleeve parts, leaving parent/inner arms and the held-item bridge on the unmodified arm lock source.

### 2026-05-18T18:02:32-03:00 Milestone 2.14 Sleeve-Only Calibration Build

date/time: 2026-05-18T18:02:32-03:00

files changed:

- `src/main/java/com/joao2/vpbfpa/config/CompatConfig.java`
- `src/main/java/com/joao2/vpbfpa/arm/ArmRestoreManager.java`
- `README.md`
- `docs/implementation-log.md`
- `docs/test-matrix.md`
- `docs/compat-map.md`
- `docs/plan.md`

reason: Add zero-default sleeve-only lock calibration offsets and keep arm/gun calibration separate from sleeve calibration.

validation command:

```powershell
.\gradlew clean build
jar tf "build\libs\vpb-fpa-compat-0.1.0.jar" | Select-String -Pattern "fabric.mod.json|vpb_fpa_compat|mixins|com/joao2/vpbfpa"
jar tf "build\libs\vpb-fpa-compat-0.1.0.jar" | Select-String -Pattern "pointblank|Fresh|FA\+|entity_model_features|entity_texture_features|bluearchive|blue-archive"
```

result: Clean build succeeded. Added right/left sleeve pitch/yaw/roll offsets, defaulting to zero. In lock strategies, the normal calibrated arm source still drives parent/inner arms and the held-item bridge. A second sleeve-calibrated source is computed after arm calibration and is used only when writing vanilla sleeves or EMF custom sleeve parts. The lock calibration log now reports separate arm-calibrated and sleeve-calibrated sources and offsets. Jar validation found our metadata/classes and no third-party asset namespace matches. Built jar: `build\libs\vpb-fpa-compat-0.1.0.jar`.

risks/next step: Runtime-test the winning baseline with global offsets zero and `armLockRightSleevePitchOffset=0.25`, `armLockLeftSleevePitchOffset=0.25`. Confirm sleeves move without moving inner arms or the held gun.

## Milestone 2.15 manual patch: late sleeve sync mode

Added `armLockSleeveSyncMode` with `source_pose` and `locked_parent_pose` values. The default remains `source_pose` for compatibility. The new `locked_parent_pose` mode is scoped to the current lock strategies and reapplies sleeves during the late `emf_vanilla_part_render_head` parent-arm lock, deriving the sleeve pose from the already locked parent arm and then applying sleeve-only offsets. This is intended to address the runtime observation that sleeve-only offsets moved in the right direction but sleeves followed a slightly different/faster timing than the inner arms and held gun.

No gameplay, first-person rendering, VPB logic, asset, projectile, sound, item, networking, or release changes were made.

## Manual Milestone 2.16 - Aim-only lock and sleeve hide diagnostic

- Added `armLockAimOnly` so lock strategies can pass through normal holding stance and only activate while the local player is actively aiming/using the weapon input.
- Added `armLockHideSleeves` so sleeves can be hidden only while the lock strategy is active.
- Intended test baseline: `lock_parent_arms_and_held_item`, `armLockCustomArmMode=neutral`, `armLockAimOnly=true`, `armLockHideSleeves=true`, `armLockFreezeWhileHoldingGun=false`, all arm/sleeve offsets `0.0`.
- Purpose: abandon sleeve alignment tuning for now; keep normal non-aim stance untouched, hide problematic sleeves during aim, and lock only inner arms + held gun while aiming.
- No gameplay, projectile, damage, sound, recipe, networking, or first-person rewrite is intended.
