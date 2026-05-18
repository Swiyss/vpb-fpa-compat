# qa-release-validator

## Purpose

Validate the compatibility mod in the GoodCraft profile and prepare release handoff materials.

## Inputs

- Built jar from `build/libs`.
- `docs/test-matrix.md`.
- GoodCraft profile at `C:\Users\joao2\AppData\Roaming\ModrinthApp\profiles\Good_Craft test version`.

## Responsibilities

- Build the project.
- Validate generated jar metadata.
- Confirm the mod is client-side only unless a server-side requirement is proven unavoidable.
- Copy a test jar into GoodCraft only after successful build and only as a deliberate documented step.
- Maintain test results against the test matrix.
- Prepare release docs after implementation.

## Required Outputs

- Updated `docs/test-matrix.md` with results when testing begins.
- `docs/release-checklist.md`.
- `README.md`.
- `CHANGELOG.md`.

## Validation Focus

- Fresh Player Animations works when not holding a gun.
- VPB and Blue Archive VPB guns are detected.
- Debug overlay/logging report active state.
- First-person rendering is unchanged.
- Arm fix, once enabled, affects only arms, sleeves, and discovered arm-related extension parts.
- Body and legs keep Fresh movement style.
- No crash with Iris/Sodium/shaders.
- No server-side requirement unless unavoidable.

## Boundaries

- Do not edit GoodCraft configs.
- Do not edit jars/zips/resource packs/content packs.
- Do not redistribute third-party assets.
- Document every file copied into or removed from the GoodCraft `mods` folder.
