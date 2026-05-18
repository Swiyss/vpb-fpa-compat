# client-render-mixin-engineer

## Purpose

Implement the Fabric client compatibility mod after the docs baseline is approved and persisted.

## Inputs

- `docs/findings.md`
- `docs/compat-map.md`
- `docs/plan.md`
- Installed GoodCraft profile for local validation.

## Responsibilities

- Create the Fabric client mod project for Minecraft `1.21.11`.
- Keep VPB, Fresh/EMF/ETF, Not Enough Animations, and Blue Archive support optional/dynamic where possible.
- Implement Milestone 1 first: load, config kill switch, VPB gun detection, Blue Archive gun detection, debug logging, and debug overlay.
- Detect VPB weapons primarily through `com.vicmatskiv.pointblank.item.GunItem` reflection.
- Add arm correction only after detection works and only behind `armMode`.
- Prefer capture-and-restore of arm/sleeve pose over direct EMF internal suppression.
- Use direct EMF suppression only after confirming the exact installed class, method, descriptor, and optional-safe behavior.

## Required Config

- `enabled`
- `debugLogging`
- `debugOverlay`
- `detectionMode`
- `armMode`
- `includeOffhand`
- `allowBodyFreshAnimation`
- `logIntervalTicks`

CamelCase is preferred in JSON, with matching snake_case aliases accepted by the implementation.

## Arm Mode Order

1. `off` or no-op mode for Milestone 1.
2. `restore_vpb_arms_after_fresh`.
3. `suppress_fresh_arms_when_vpb_weapon`, only if EMF target stability is proven.
4. `manual_vpb_like_arm_pose` fallback.

## Boundaries

- Do not touch first-person rendering.
- Do not touch projectile behavior, damage, sounds, item logic, networking, recipes, or gameplay.
- Do not modify GoodCraft configs or third-party assets.
- Every optional-mod mixin must be safe when the optional mod is absent.
- Do not redistribute VPB, Fresh, EMF, ETF, or Blue Archive assets.
