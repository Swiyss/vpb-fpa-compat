# VPB/FPA Compatibility Findings

## Source Of Truth

This document captures the approved planning baseline for the VPB + Fresh Player Animations compatibility project. Future implementation work should treat these repo files as the source of truth instead of relying on chat context.

Workspace:

```text
C:\Projects\vpb-fpa-compat
```

GoodCraft profile:

```text
C:\Users\joao2\AppData\Roaming\ModrinthApp\profiles\Good_Craft test version
```

No GoodCraft files should be modified during implementation, except for copying a successfully built test jar into the profile `mods` folder as a deliberate and documented validation step.

## Confirmed Environment

- Loader: Fabric.
- Minecraft: `1.21.11`.
- Fabric Loader: `0.19.2`.
- Fabric API in profile: `0.141.4+1.21.11`.
- Confirmation source: `logs/latest.log`, which reported loading Minecraft `1.21.11` with Fabric Loader `0.19.2`.
- `modrinth.index.json` and `profile.json` were not present in the profile root during inspection, so logs and installed files are the reliable local evidence.

The compatibility project should therefore be a Fabric client mod, not NeoForge and not multi-loader.

## Relevant Installed Mods And Packs

Observed relevant components:

- Vic's Point Blank: `pointblank-fabric-1.21.11-2.0.1.jar`.
- GeckoLib: `5.4.5`.
- EMF / Entity Model Features: `3.2.4`.
- ETF / Entity Texture Features: `7.1`.
- Not Enough Animations: `1.12.3`.
- Iris: `1.10.7+mc1.21.11`.
- Sodium: `0.8.12-beta.3+mc1.21.11`.
- Sodium Extra: `0.8.3+mc1.21.11`.
- Existing first-person compatibility mod: `vpb-hmi-compat-0.3.0.jar`.
- Other animation/render-adjacent mods include CameraOverhaul, PaperDoll, WaveyCapes, SpawnAnimations, 3D armor, and HMI.

No standalone KosmX PlayerAnimator jar was detected, though Point Blank contains optional PlayerAnimator compatibility classes.

## Point Blank Findings

Important Point Blank config values from `config/pointblank-common.toml`:

- `thirdPersonArmPoseAlwaysOn = true`
- `thirdPersonAnimationsEnabled = true`
- `firstPersonAnimationsEnabled = true`
- `pipScopesEnabled = false`

Important Point Blank APIs/classes observed in the installed jar:

- `com.vicmatskiv.pointblank.item.GunItem`
- `com.vicmatskiv.pointblank.util.MiscUtil#getMainHeldGun`
- `GunItem` methods for resolving held gun context and operable gun stacks.

The safest first detection path is to reflectively test whether the held item is an instance of `com.vicmatskiv.pointblank.item.GunItem`. This supports base Point Blank guns and content-pack guns without hardcoding every weapon id.

## Blue Archive VPB Pack Findings

Blue Archive VPB content was found as a Point Blank content pack:

```text
pointblank\bluearchive-ext v0.6.zip
```

The pack contains:

- `ext.json` with name `pointblank-bluearchiveofficial-pack`.
- `pack.mcmeta` with description `Blue Archive Pack`.
- Assets under `assets/pointblank/...`.
- Gun item JSON files under `assets/pointblank/items/`.
- Gun entries use `"type": "Gun"` and names such as `ba_ariusar`, `ba_identity`, and related `ba_*` ids.

Because Blue Archive guns are registered through Point Blank and use the `pointblank` namespace, the implementation should not hardcode every Blue Archive weapon unless dynamic detection fails.

## Fresh / EMF Player Animation Findings

Fresh Animations and extension packs are not merely present; local evidence shows they are active.

- `options.txt` includes `file/FreshAnimations_v1.10.4.zip` and `file/FA+All_Extensions-v1.8.1.zip` in the active `resourcePacks` list.
- `logs/latest.log` resource reload output includes `file/FA+Player-v1.0.zip` and `file/FA+All_Extensions-v1.8.1.zip`.
- If future logs/options differ, active state must be re-confirmed in-game before diagnosing arm animation behavior.

`FA+Player-v1.0.zip` contains EMF/CEM player assets including:

- `assets/minecraft/emf/cem/player.jem`
- `assets/minecraft/emf/cem/player_slim.jem`
- `a_player_variables.jpm`
- `a_player_equipment.jpm`
- `a_player_firstperson.jpm`
- `a_player_idle.jpm`
- `a_player_movement.jpm`

Important player model parts:

- `head`
- `body`
- `right_arm`
- `left_arm`
- `right_leg`
- `left_leg`
- `right_sleeve`
- `left_sleeve`
- `right_item`
- `left_item`

Fresh/EMF animation files explicitly modify arm rotations/translations and copy sleeve pose values from the corresponding arms. The compatibility mod must preserve or restore only arms and arm-related extension parts while leaving body, legs, and head Fresh animation intact where possible.

## Render-Order Hypothesis

The likely conflict is that Fresh/EMF re-applies player model arm transforms after Point Blank has prepared its third-person gun pose, causing the arms to lose the VPB gun-holding stance.

Initial evidence:

- Point Blank has optional PlayerAnimator compatibility mixins that apply model part poses around player model setup.
- Not Enough Animations also injects around `PlayerModel` setup.
- EMF includes player model pose reapplication logic at the tail of player setup.
- EMF pose reapplication appears capable of overwriting vanilla `rightArm` and `leftArm` model part transforms.

Do not rely blindly on a direct EMF internal mixin. Before any EMF-targeted mixin is implemented, re-identify the exact installed EMF class, method, descriptor, field names, and call order from the installed jar.

Milestone 1 must not depend on this render-order hypothesis. It should implement only mod loading, config, detection, logging, and overlay.

## Risks And Unknowns

- The exact final render order must be revalidated in code before implementing arm fixes.
- Direct EMF internals may change between versions, so EMF-specific mixins must be optional-safe and guarded.
- If Fresh/EMF custom player rendering bypasses vanilla player arms more deeply than expected, the first arm strategy may need to fall back to a later final restore or manual VPB-like pose.
- First milestone must avoid gameplay, networking, item logic, first-person rendering, and profile config changes.
- No VPB, Fresh, EMF, ETF, or Blue Archive assets may be redistributed.
