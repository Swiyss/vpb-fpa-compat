# modpack-animation-inspector

## Purpose

Inspect the GoodCraft profile and produce the technical compatibility map for VPB, Blue Archive VPB content, Fresh Player Animations, EMF/ETF, and other animation/render participants.

## Inputs

- Repo root: `C:\Projects\vpb-fpa-compat`
- GoodCraft profile: `C:\Users\joao2\AppData\Roaming\ModrinthApp\profiles\Good_Craft test version`
- Existing docs in `docs/`

## Responsibilities

- Confirm Minecraft version, loader, loader version, and relevant mod versions from local evidence.
- Identify VPB jars, Point Blank dependencies, Blue Archive VPB content packs, Fresh/FA packs, EMF, ETF, Not Enough Animations, PlayerAnimator if present, GeckoLib, Iris, Sodium, and related render mods.
- Inspect Point Blank configs for third-person animation and arm-pose settings.
- Inspect Fresh/FA player resource-pack structure for `.jem`, `.jpm`, animation expressions, and player model part names.
- Identify whether Blue Archive guns are supplied by content pack, resource pack, datapack, mod jar, config, or another path.
- Revalidate EMF internals before any plan depends on direct EMF mixins.

## Outputs

- `docs/findings.md`
- `docs/compat-map.md`
- Recommended implementation path.
- Risks, unknowns, and follow-up traces needed before arm fixes.

## Boundaries

- Do not edit GoodCraft files.
- Do not edit jars/zips in place.
- Copy files into the repo for analysis only if needed and document provenance.
- Keep exact filenames and versions in findings.
- Do not implement code.
