# vpb-fpa-animation-trace

Use this repo-local skill when tracing third-person player animation/render order for VPB, Fresh Player Animations, EMF, ETF, Not Enough Animations, and related client rendering mods.

## Goals

- Identify when VPB applies third-person gun arm poses.
- Identify when Fresh/EMF applies player model animation expressions.
- Determine whether Fresh/EMF overwrites VPB arms, VPB applies too early, or another mod changes final pose.
- Preserve Fresh body and legs while touching only arms and arm-related extension parts.

## Trace Targets

Inspect and trace:

- `PlayerEntityRenderer`
- `LivingEntityRenderer`
- `PlayerModel`
- player model pose setup / `setupAnim`
- Point Blank player animation classes and mixins
- Not Enough Animations player model mixins
- EMF player model pose application

Before adding any direct EMF mixin, identify the exact installed class, method, descriptor, and field access from the local EMF jar.

## Pose Snapshots

Compare model part rotations/translations before and after each suspected animation system:

- `rightArm`
- `leftArm`
- `rightSleeve`
- `leftSleeve`
- `body`
- `head`
- `rightLeg`
- `leftLeg`

Log only at a configured interval to avoid spam.

## Required Comparisons

Run or reason through these states:

- Fresh disabled, VPB enabled.
- Fresh enabled, VPB enabled.
- Fresh enabled, no gun.
- Base VPB gun in main hand.
- Blue Archive VPB gun in main hand.

## Choosing An Arm Strategy

Prefer strategies in this order:

1. `restore_vpb_arms_after_fresh`: capture VPB/vanilla arm pose before Fresh/EMF overwrite and restore only arms/sleeves afterward.
2. `suppress_fresh_arms_when_vpb_weapon`: skip Fresh/EMF arm transforms only when the exact optional-safe target is known.
3. `manual_vpb_like_arm_pose`: fallback if exact capture/suppression is not viable.

## Hard Rules

- Keep Fresh body and legs.
- Only touch arms, sleeves, and discovered arm-related extension parts.
- Never alter first-person gun rendering.
- Always leave a config kill switch.
- Every optional-mod mixin must be safe if that mod is removed.
- Do not modify VPB, Fresh, EMF, ETF, or Blue Archive assets.
